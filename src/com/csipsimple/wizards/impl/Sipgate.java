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
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Base64;
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

public class Sipgate extends AlternateServerImplementation {
	
	private static final String PROXY_KEY = "proxy_server";

    private EditTextPreference accountProxy;


    @Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		//Override titles
		accountDisplayName.setTitle(R.string.w_sipgate_display_name);
		accountDisplayName.setDialogTitle(R.string.w_sipgate_display_name);
		accountServer.setTitle(R.string.w_common_server);
		accountServer.setDialogTitle(R.string.w_common_server);
		accountUsername.setTitle(R.string.w_sipgate_username);
		accountUsername.setDialogTitle(R.string.w_sipgate_username);
		accountPassword.setTitle(R.string.w_sipgate_password);
		accountPassword.setDialogTitle(R.string.w_sipgate_password);
		
		// Add optional proxy
        boolean recycle = true;
        accountProxy = (EditTextPreference) findPreference(PROXY_KEY);
        if(accountProxy == null) {
            accountProxy = new EditTextPreference(parent);
            accountProxy.setKey(PROXY_KEY);
            accountProxy.setTitle(R.string.w_advanced_proxy);
            accountProxy.setSummary(R.string.w_advanced_proxy_desc);
            accountProxy.setDialogMessage(R.string.w_advanced_proxy_desc);
            recycle = false;
        }
        
        String currentProxy = account.getProxyAddress();
        String currentServer = account.getSipDomain();
        if(!TextUtils.isEmpty(currentProxy) && !TextUtils.isEmpty(currentServer)
                && !currentProxy.equalsIgnoreCase(currentProxy)) {
            accountProxy.setText(currentProxy);
        }

        if(!recycle) {
            addPreference(accountProxy);
        }
        
		
        if(TextUtils.isEmpty(account.getSipDomain())) {
            accountServer.setText("sipgate.de");
        }

		//Get wizard specific row for balance
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
		updateAccountInfos(account);
	}
	

	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		String nproxy = getText(accountProxy);
		if(!TextUtils.isEmpty(nproxy)) {
		    account.proxies = new String[] {"sip:"+nproxy};
		}
		account.transport = SipProfile.TRANSPORT_UDP;
		account.allow_contact_rewrite = false;
		return account;
	}

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.addStunServer("stun.sipgate.net:10000");
	}
	

	@Override
	protected String getDefaultName() {
		return "Sipgate";
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
	

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(PROXY_KEY.equals(fieldName)) {
            return parent.getString(R.string.w_advanced_proxy_desc);
        }
        return super.getDefaultFieldSummary(fieldName);
    }
	
    @Override
    public void updateDescriptions() {
        super.updateDescriptions();
        setStringFieldSummary(PROXY_KEY);
    }

	
	// Balance consulting

	private static final String URL_BALANCE = "samurai.sipgate.net/RPC2";
	
	private LinearLayout customWizard;
	private TextView customWizardText;
	protected static final int DID_SUCCEED = 0;
	protected static final int DID_ERROR = 1;

	protected static final String THIS_FILE = "Sipgatewizard";
	
	private Handler creditHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case DID_SUCCEED: {
				//Here we get the credit info, now add a row in the interface
				String response = (String) message.obj;
				try{
					float value = Float.parseFloat(response.trim());
					if(value >= 0) {
						customWizardText.setText("Credit : " + Math.round(value * 100.0)/100.0 + " euros");
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
				Log.e(THIS_FILE, "Error here", e);
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
						
						String requestURL = "https://";
						String provider = acc.getSipDomain();
						if(!TextUtils.isEmpty(provider)) {
							requestURL += URL_BALANCE;
							
							String username = ""; String password = "";
							

							HttpPost httpPost = new HttpPost(requestURL);
							String userpassword = username + ":" + password;
							String encodedAuthorization = Base64.encodeBytes( userpassword.getBytes() );
							httpPost.addHeader("Authorization", "Basic " + encodedAuthorization);
							httpPost.addHeader("Content-Type", "text/xml");
							
							// prepare POST body
	                        String body = "<?xml version='1.0'?><methodCall><methodName>samurai.BalanceGet</methodName></methodCall>";

	                        // set POST body
	                        HttpEntity entity = new StringEntity(body);
	                        httpPost.setEntity(entity);
							
							// Create a response handler
							HttpResponse httpResponse = httpClient.execute(httpPost);
							if(httpResponse.getStatusLine().getStatusCode() == 200) {
								InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
								BufferedReader br = new BufferedReader(isr);
								String line = null;
								Pattern p = Pattern.compile("^.*TotalIncludingVat</name><value><double>(.*)</double>.*$");
								
								while( (line = br.readLine() ) != null ) {
									Matcher matcher = p.matcher(line);
									if(matcher.matches()) {
										creditHandler.sendMessage(creditHandler.obtainMessage(DID_SUCCEED, matcher.group(1)));
										break;
									}
								}
								creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
							}else {
								creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
							}
							
						}
						
						
					} catch (Exception e) {
						creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR, e));
					}
				}
			};
			t.start();
		} else {
			// add a row to link 
			customWizard.setVisibility(View.GONE);
			
		}
	}
}
