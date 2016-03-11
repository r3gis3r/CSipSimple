/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.wizards.impl;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MondotalkCreate extends Activity implements OnClickListener, TextWatcher {
    
    private final static String API_KEY = "{7EB11554-7BAD-D25D-1D17-B070D4AC459F}";

    private static final String THIS_FILE = "MondotalkCreate";
    
    private ImageView captchaImg;
    private View captchaProgress;
    private EditText firstName;
    private EditText lastName;
    private EditText emailAddr;
    private EditText countryCode;
    private EditText phone;
    private EditText captcha;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.w_mondotalk_create);
        
        findViewById(R.id.cancel_bt).setOnClickListener(this);
        findViewById(R.id.save_bt).setOnClickListener(this);
        
        captchaImg = (ImageView) findViewById(R.id.cw_captcha_img);
        captchaProgress = findViewById(R.id.cw_captcha_progress);
        
        firstName = (EditText) findViewById(R.id.cw_first_name); 
        lastName = (EditText) findViewById(R.id.cw_last_name);
        emailAddr = (EditText) findViewById(R.id.cw_email);
        countryCode = (EditText) findViewById(R.id.cw_country_code);
        phone = (EditText) findViewById(R.id.cw_phone);
        captcha = (EditText) findViewById(R.id.cw_captcha);
        
        
        firstName.addTextChangedListener(this);
        emailAddr.addTextChangedListener(this);
        captcha.addTextChangedListener(this);
        
        // Ok, crappy thread creation to retrieve captcha in bg
        Thread t = new Thread() {
            public void run() {
                retrieveCaptcha();
            };
        };
        t.start();
        
        canSave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(captchaBitmap != null) {
            captchaBitmap.recycle();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.cancel_bt) {
            setResult(RESULT_CANCELED);
            finish();
        }else if(id == R.id.save_bt) {
            if(canSave()) {
                progressDialog = ProgressDialog.show(this, getResources().getText(R.string.create_account), 
                        getResources().getText(R.string.loading), true);
                Thread t = new Thread() {
                    public void run() {
                        saveOnline(getEditTextValue(firstName), getEditTextValue(lastName),
                                getEditTextValue(emailAddr), getEditTextValue(phone),
                                getEditTextValue(countryCode), getEditTextValue(captcha));

                    };
                };
                t.start();
            }
        }
    }
    
    private final static int MSG_CAPTCHA_LOADED = 0;
    private final static int MSG_SAVE_DONE = 1;
    private final static int MSG_SAVE_ERROR = 2;

    private Handler mHander = new MondotalkHandler(this);
    
    private static class MondotalkHandler extends Handler {
        
        WeakReference<MondotalkCreate> w;
        
        MondotalkHandler(MondotalkCreate wizard){
            w = new WeakReference<MondotalkCreate>(wizard);
        }
        
        public void dispatchMessage(Message msg) {
            MondotalkCreate wizard = w.get();
            if(wizard == null) {
                return;
            }
            switch (msg.what) {
                case MSG_CAPTCHA_LOADED:
                    if(wizard.captchaBitmap != null && wizard.captchaImg != null) {
                        wizard.captchaImg.setImageBitmap(wizard.captchaBitmap);
                        wizard.captchaImg.setVisibility(View.VISIBLE);
                        wizard.captchaProgress.setVisibility(View.GONE);
                    }
                    break;
                case MSG_SAVE_DONE:
                    if(wizard.progressDialog != null) {
                        wizard.progressDialog.dismiss();
                    }
                    AccountCreationResult res = (AccountCreationResult) msg.obj;
                    Intent it = wizard.getIntent();
                    it.putExtra(SipProfile.FIELD_USERNAME, res.username);
                    it.putExtra(SipProfile.FIELD_DATA, res.password);
                    wizard.setResult(RESULT_OK, it);
                    wizard.finish();
                    break;
                case MSG_SAVE_ERROR:
                    if(wizard.progressDialog != null) {
                        wizard.progressDialog.dismiss();
                    }
                    Toast toast = Toast.makeText(wizard, (String)msg.obj, Toast.LENGTH_LONG);
                    toast.show();
                    break;
            }
        };
    };
    
    
    private String captchaKey = "";
    private String captchaUrl = "";
    private Bitmap captchaBitmap = null;

    private Pattern soapResultPattern = Pattern.compile("^.*<result xsi:type=\"xsd:string\">(.*)</result>.*$");
    private boolean retrieveCaptcha() {
        String ip;
        try {
            Socket socket = new Socket("api001.mondotalk.com", 80);
            ip = socket.getLocalAddress().toString();
        } catch (Exception e) {
            Log.e(THIS_FILE,"Can't get local ip address", e);
            ip = "127.0.0.1";
        }
        
        try {
            // No SOAP lib.... bourrinage !
            String requestURL = "https://api001.mondotalk.com/webservices/captcha.php";

            HttpPost httpPost = new HttpPost(requestURL);
            httpPost.addHeader("SOAPAction", "\"Captcha\"");
            httpPost.addHeader("Content-Type", "text/xml");

            // prepare POST body
            String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<SOAP-ENV:Envelope " +
                    " SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                    " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"" +
                    " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
                    " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                    " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" >" +
                    "<SOAP-ENV:Body>" +
                    "<Captcha SOAP-ENC:root=\"1\">" +
                    "<v1 xsi:type=\"xsd:string\">" + API_KEY + "</v1>" +
                    "<v2 xsi:type=\"xsd:string\">" + ip + "</v2>" +
                    "<v3 xsi:type=\"xsd:string\">520</v3>" +
                    "<v4 xsi:type=\"xsd:string\">200</v4>" +
                    "</Captcha>" +
                    "</SOAP-ENV:Body>" +
                    "</SOAP-ENV:Envelope>";

            // set POST body
            HttpEntity entity;
            entity = new StringEntity(body);
            httpPost.setEntity(entity);
            

            HttpClient httpClient = new DefaultHttpClient();
            // Create a response handler
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if(httpResponse.getStatusLine().getStatusCode() == 200) {
                InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
                BufferedReader br = new BufferedReader(isr);

                String line = null;
                while( (line = br.readLine() ) != null ) {
                    if(!TextUtils.isEmpty(line)) {

                        Matcher matcher = soapResultPattern.matcher(line);
                        if(matcher.matches()) {
                            String strValue = matcher.group(1).trim();
                            if(!TextUtils.isEmpty(strValue)) {
                                String[] strValues = strValue.split("\\|");
                                if(strValues.length > 1) {
                                    captchaUrl = strValues[0];
                                    captchaKey = strValues[1];
                                }
                            }
                            break;
                        }
                    }
                }
                
                if(TextUtils.isEmpty(captchaKey)) {
                    return false;
                }
                
                Log.d(THIS_FILE, "Captcha retrieved " + captchaKey + " and " + captchaUrl);
                
                captchaBitmap = BitmapFactory.decodeStream((new URL(captchaUrl)).openConnection() .getInputStream()); 
                
                mHander.sendEmptyMessage(MSG_CAPTCHA_LOADED);
            }else {
                Log.e(THIS_FILE, "Something went wrong while retrieving the captcha webservice ");
            }

        } catch (Exception e) {
            Log.e(THIS_FILE, "Something went wrong while retrieving the captcha", e);
        }
        
        return false;
    }
    
    /**
     * Client side form validation.
     * 
     * @return true if the form is valid for save
     */
    private boolean canSave() {
        boolean isValid = true;
        
        isValid &= checkEmptyText(firstName);
        isValid &= checkEmptyText(emailAddr);
        isValid &= checkEmptyText(captcha);
        
        
        // Update the status of the save button
        findViewById(R.id.save_bt).setEnabled(isValid);
        
        return isValid;
    }
    
    /**
     * Get the value of an edit text a safe way
     * @param txt The edit text to get value of
     * @return the Text value of the edit text
     */
    private String getEditTextValue(EditText txt) {
        if(txt == null) {
            return "";
        }
        String res = txt.getText().toString();
        if(TextUtils.isEmpty(res)) {
            return "";
        }
        return res;
    }
    private boolean checkEmptyText(EditText txt) {
        if(TextUtils.isEmpty(txt.getText().toString())) {
            txt.setError("Empty");
            return false;
        }
        txt.setError(null);
        return true;
    }
    
    
    private class AccountCreationResult {
        final String username;
        final String password;
        AccountCreationResult(String uname, String pwd){
            username = uname;
            password = pwd;
        }
    }
    
    private boolean saveOnline(String firstName, String lastName, String contactEmail, String contactPhone, String countryCode, String captcha) {

        try {
            // No SOAP lib.... bourrinage !
            String requestURL = "https://api001.mondotalk.com/webservices/createaccount.php";

            HttpPost httpPost = new HttpPost(requestURL);
            httpPost.addHeader("SOAPAction", "\"CreateAccount\"");
            httpPost.addHeader("Content-Type", "text/xml");

            // prepare POST body
            String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<SOAP-ENV:Envelope " +
                    " SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                    " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\"" +
                    " xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
                    " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                    " xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\" >" +
                    "<SOAP-ENV:Body>" +
                    "<CreateAccount SOAP-ENC:root=\"1\">" +
                    "<v1 xsi:type=\"xsd:string\">" + API_KEY + "</v1>" +
                    "<v2 xsi:type=\"xsd:string\">" + firstName + "</v2>" +
                    "<v3 xsi:type=\"xsd:string\">" + lastName + "</v3>" +
                    "<v4 xsi:type=\"xsd:string\">" + contactEmail + "</v4>" +
                    "<v5 xsi:type=\"xsd:string\">" + contactPhone + "</v5>" +
                    "<v6 xsi:type=\"xsd:string\">" + countryCode + "</v6>" +
                    "<v7 xsi:type=\"xsd:string\">" + captcha + "</v7>" +
                    "<v8 xsi:type=\"xsd:string\">" + captchaKey + "</v8>" +
                    "</CreateAccount>" +
                    "</SOAP-ENV:Body>" +
                    "</SOAP-ENV:Envelope>";

            // set POST body
            HttpEntity entity;
            entity = new StringEntity(body);
            httpPost.setEntity(entity);
            

            HttpClient httpClient = new DefaultHttpClient();
            // Create a response handler
            HttpResponse httpResponse = httpClient.execute(httpPost);
            if(httpResponse.getStatusLine().getStatusCode() == 200) {
                InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
                BufferedReader br = new BufferedReader(isr);

                String line = null;
                
                String username = "";
                String password = "";
                while( (line = br.readLine() ) != null ) {
                    if(!TextUtils.isEmpty(line)) {

                        Matcher matcher = soapResultPattern.matcher(line);
                        if(matcher.matches()) {
                            String strValue = matcher.group(1).trim();
                            if(!TextUtils.isEmpty(strValue)) {
                                
                                String[] strValues = strValue.split("\\|");
                                if(strValues.length > 1) {
                                    username = strValues[0];
                                    password = strValues[1];
                                }
                            }
                            break;
                        }
                    }
                }
                
                Log.d(THIS_FILE, "Account created " + username);
                if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                    mHander.sendMessage(mHander.obtainMessage(MSG_SAVE_ERROR, "Invalid datas to create the account"));
                    return false;
                }
                
                AccountCreationResult res = new AccountCreationResult(username, password);
                mHander.sendMessage(mHander.obtainMessage(MSG_SAVE_DONE, res));
                return true;
            }else {
                Log.e(THIS_FILE, "Something went wrong while retrieving the captcha webservice ");
            }

        } catch (Exception e) {
            Log.e(THIS_FILE, "Can't create account", e);
        }
        mHander.sendMessage(mHander.obtainMessage(MSG_SAVE_ERROR, "Error while creating the account"));
        
        return false;
    }

    @Override
    public void afterTextChanged(Editable s) {
        canSave();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Nothing to do
    }
    
    
}
