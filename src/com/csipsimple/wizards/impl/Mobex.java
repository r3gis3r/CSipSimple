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

import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mobex extends SimpleImplementation {
    protected static final int DID_SUCCEED = 0;
    protected static final int DID_ERROR = 1;
    private String THIS_FILE = "MobexW";


    private LinearLayout customWizard;
    private TextView customWizardText;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDomain() {
        return "200.152.124.172";
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected String getDefaultName() {
		return "Mobex";
	}
	
	private final static String USUAL_PREFIX = "12";
	
	/**
	 * {@inheritDoc}
	 */
    @Override
    public void fillLayout(SipProfile account) {
        super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
        if(TextUtils.isEmpty(account.username)){
            accountUsername.setText(USUAL_PREFIX);
        }

        //Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
        

        updateAccountInfos(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canSave() {
        boolean ok = super.canSave();
        ok &= checkField(accountUsername, accountUsername.getText().trim().equalsIgnoreCase(USUAL_PREFIX));
        return ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        acc.proxies = null;
        String encodedUser = SipUri.encodeUser(accountUsername.getText().trim());
        account.acc_id = "0"+encodedUser+" <sip:"
                + encodedUser + "@"+getDomain()+">";
        return acc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {

        prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
        
        // G729 8 KHz, PCMA 8 KHz, PCMU 8 KHz and GSM 8 KHz
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"220");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"240");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "210");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_WB, "0");

        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"220");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"240");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "210");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_NB, "0");
        
    }
    
    // Account balance
    /**
     * Handle credit account message updates.
     */
    private Handler creditHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
            case DID_SUCCEED: {
                //Here we get the credit info, now add a row in the interface
                String response = (String) message.obj;
                try{
                    float value = Float.parseFloat(response.trim());
                    if(value >= 0) {
                        customWizardText.setText("Credito : " + Math.round(value * 100.0)/100.0 + " euros");
                        customWizard.setVisibility(View.VISIBLE);
                    }
                }catch(NumberFormatException e) {
                    Log.e(THIS_FILE, "Impossible to parse result", e);
                }catch (NullPointerException e) {
                    Log.e(THIS_FILE, "Null result");
                }
                
                break;
            }
            case DID_ERROR: {
                Exception e = (Exception) message.obj;
                e.printStackTrace();
                break;
            }
            }
        }
    };
    
    
    /**
     * Update view regarding account information.
     * For now only get the account balance.
     * @param acc The current SipProfile of this account.
     */
    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            Thread t = new Thread() {
                public void run() {
                    try {
                        HttpClient httpClient = new DefaultHttpClient();
                        
                        String requestURL = "http://200.152.124.172/billing/webservice/Server.php";
                        
                        HttpPost httpPost = new HttpPost(requestURL);
                        httpPost.addHeader("SOAPAction", "\"mostra_creditos\"");
                        httpPost.addHeader("Content-Type", "text/xml");

                        // prepare POST body
                        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope " +
                        		"SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                        		"xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                        		"xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
                        		"xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"" +
                        		"><SOAP-ENV:Body><mostra_creditos SOAP-ENC:root=\"1\">" +
                        		"<chave xsi:type=\"xsd:string\">" +
                        		acc.data +
                        		"</chave><username xsi:type=\"xsd:string\">" +
                        		acc.username +
                        		"</username></mostra_creditos></SOAP-ENV:Body></SOAP-ENV:Envelope>";

                        // set POST body
                        HttpEntity entity = new StringEntity(body);
                        httpPost.setEntity(entity);

                        // Create a response handler
                        HttpResponse httpResponse = httpClient.execute(httpPost);
                        if(httpResponse.getStatusLine().getStatusCode() == 200) {
                            InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
                            BufferedReader br = new BufferedReader(isr);
                            Pattern p = Pattern.compile("^.*<return xsi:type=\"xsd:string\">(.*)</return>.*$");
                            
                            String line = null;
                            while( (line = br.readLine() ) != null ) {
                                Matcher matcher = p.matcher(line);
                                if(matcher.matches()) {
                                    creditHandler.sendMessage(creditHandler.obtainMessage(DID_SUCCEED, matcher.group(1)));
                                    break;
                                }
                            }
                        }else {
                            creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
                        }
                    } catch (Exception e) {
                        creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
                    }
                }
            };
            t.start();
        } else {
            // add a row to link 
            customWizardText.setText(R.string.create_account);
            customWizard.setVisibility(View.GONE);
            // Not yet...
        }
    }
}
