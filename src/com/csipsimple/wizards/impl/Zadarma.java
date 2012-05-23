/**
 * Copyright (C) 2011 Dmytro Tokar
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;
import com.csipsimple.wizards.impl.AccountCreationWebview.OnAccountCreationDoneListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Zadarma extends SimpleImplementation implements OnAccountCreationDoneListener {


	protected static final String THIS_FILE = "ZadarmaW";
	protected static final int DID_SUCCEED = 0;
	protected static final int DID_ERROR = 1;
	
	private static final String webCreationPage = "https://ss.zadarma.com/android/registration/";

	private LinearLayout customWizard;
	private TextView customWizardText;
    private AccountCreationWebview extAccCreator;
	
	@Override
	protected String getDomain() {
		return "Zadarma.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "Zadarma";
	}
	
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

		//Get wizard specific row
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
		
		
		updateAccountInfos(account);

        extAccCreator = new AccountCreationWebview(parent, webCreationPage, this);
	}
	



	private Handler creditHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case DID_SUCCEED: {
				//Here we get the credit info, now add a row in the interface
				String response = (String) message.obj;
				try{
					float value = Float.parseFloat(response.trim());
					if(value >= 0) {
						customWizardText.setText("Balance : " + Math.round(value * 100.0)/100.0 + " USD");
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
	
	private void updateAccountInfos(final SipProfile acc) {
		if (acc != null && acc.id != SipProfile.INVALID_ID) {
			customWizard.setVisibility(View.GONE);
			Thread t = new Thread() {

				public void run() {
					try {
						HttpClient httpClient = new DefaultHttpClient();
						
						String requestURL = "https://ss.zadarma.com/android/getbalance/?"
							+ "login=" + acc.username
							+ "&code=" + MD5.MD5Hash(acc.data);
						
						HttpGet httpGet = new HttpGet(requestURL);

						// Create a response handler
						HttpResponse httpResponse = httpClient.execute(httpGet);
						if(httpResponse.getStatusLine().getStatusCode() == 200) {
							InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
							BufferedReader br = new BufferedReader(isr);
							String line = br.readLine();
							creditHandler.sendMessage(creditHandler.obtainMessage(DID_SUCCEED, line));
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
			customWizardText.setText("Create account");
			customWizard.setVisibility(View.VISIBLE);
			customWizard.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
				    extAccCreator.show();
				}
			});
		}
	}
	
	
	
	@Override
	public boolean needRestart() {
		return true;
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		return account;
	}
	

    @Override
    public void onAccountCreationDone(String username, String password) {
        setUsername(username);
        setPassword(password);
    }
    

    @Override
    public boolean saveAndQuit() {
        if(canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }
}
