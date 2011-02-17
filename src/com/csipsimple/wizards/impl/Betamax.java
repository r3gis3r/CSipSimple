/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;


public class Betamax extends AuthorizationImplementation {

	
	static String PROVIDER = "provider";

	protected static final String THIS_FILE = "BetamaxW";
	
	private static final String URL_BALANCE = "/myaccount/getbalance.php";
	
	private LinearLayout customWizard;
	private TextView customWizardText;
	protected static final int DID_SUCCEED = 0;
	protected static final int DID_ERROR = 1;
	
	
	ListPreference providerListPref;
	
	static HashMap<String, String[]> providers = new HashMap<String, String[]>(){
		private static final long serialVersionUID = 4984940975243241784L;
	{
		put("FreeCall", new String[] {"sip.voiparound.com", "stun.voiparound.com"});
		put("InternetCalls", new String[] {"sip.internetcalls.com", "stun.internetcalls.com"});
		put("Low Rate VoIP", new String[] {"sip.lowratevoip.com", "stun.lowratevoip.com"});
		put("NetAppel", new String[] {"sip.netappel.fr", "stun.netappel.fr"});
		put("Poivy", new String[] {"sip.poivy.com", "stun.poivy.com"});
		put("SIP Discount", new String[] {"sip.sipdiscount.com", "stun.sipdiscount.com"});
		put("SMS Discount", new String[] {"sip.smsdiscount.com", "stun.smsdiscount.com"});
		put("SparVoIP", new String[] {"sip.sparvoip.com", "stun.sparvoip.com"});
		put("VoIP Buster", new String[] {"sip.voipbuster.com", "stun.voipbuster.com"});
		put("VoIP Buster Pro", new String[] {"sip.voipbusterpro.com", "stun.voipbusterpro.com"});
		put("VoIP Cheap", new String[] {"sip.voipcheap.com", "stun.voipcheap.com"});
		put("VoIP Discount", new String[] {"sip.voipdiscount.com", "stun.voipdiscount.com"});
		put("12VoIP", new String[] {"sip.12voip.com", "stun.12voip.com"});
		put("VoIP Stunt", new String[] {"sip.voipstunt.com", "stun.voipstunt.com"});
		put("WebCall Direct", new String[] {"sip.webcalldirect.com", "stun.webcalldirect.com"});
		put("Just VoIP", new String[] {"sip.justvoip.com", "stun.justvoip.com"});
		put("Nonoh", new String[] {"sip.nonoh.net", "stun.nonoh.net"});
		put("VoIPWise", new String[] {"sip.voipwise.com", "stun.voipwise.com"});
		put("VoIPRaider", new String[] {"sip.voipraider.com", "stun.voipraider.com"});
		put("BudgetSIP", new String[] {"sip.budgetsip.com", "stun.budgetsip.com"});
		put("InterVoIP", new String[] {"sip.intervoip.com", "stun.intervoip.com"});
		put("VoIPHit", new String[] {"sip.voiphit.com", "stun.voiphit.com"});
		put("SmartVoIP", new String[] {"sip.smartvoip.com", "stun.smartvoip.com"});
		put("ActionVoIP", new String[] {"sip.actionvoip.com", "stun.actionvoip.com"});
		put("Jumblo", new String[] {"sip.jumblo.com", "stun.jumblo.com"});
		put("Rynga", new String[] {"sip.rynga.com", "stun.rynga.com"});
		put("PowerVoIP", new String[] {"sip.powervoip.com", "stun.powervoip.com"});
		/*
		put("InternetCalls", new String[] {"", ""});
		*/
	}};
	
	@Override
	protected String getDefaultName() {
		return "Betamax";
	}
	
	
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);

		accountUsername.setTitle(R.string.w_advanced_caller_id);
		accountUsername.setDialogTitle(R.string.w_advanced_caller_id_desc);
		
		
        providerListPref = new ListPreference(parent);
        
        CharSequence[] v = new CharSequence[providers.size()];
        int i = 0;
        for(String pv : providers.keySet()) {
        	v[i] = pv;
        	i++;
        }
        
        providerListPref.setEntries(v);
        providerListPref.setEntryValues(v);
        providerListPref.setKey(PROVIDER);
        providerListPref.setDialogTitle("Provider");
        providerListPref.setTitle("Provider");
        providerListPref.setSummary("Betamax clone provider");
        providerListPref.setDefaultValue("FreeCall");
        String domain = account.getDefaultDomain();
        for(Entry<String, String[]> entry : providers.entrySet()) {
        	String[] val = entry.getValue();
        	if(val[0] == domain) {
        		providerListPref.setValue(entry.getKey());
        	}
        }
        
        parent.getPreferenceScreen().addPreference(providerListPref);
        
        hidePreference(null, SERVER);
        

		//Get wizard specific row
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
		
		updateAccountInfos(account);
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		return account;
	}

	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203615L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(USER_NAME, R.string.w_advanced_caller_id_desc);
		put(AUTH_NAME, R.string.w_authorization_auth_name_desc);
		put(PASSWORD, R.string.w_common_password_desc);
		put(SERVER, R.string.w_common_server_desc);
	}};
	
	public void updateDescriptions() {
		super.updateDescriptions();
		setStringFieldSummary(PROVIDER);
		
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(fieldName == PROVIDER) {
			if(providerListPref != null) {
				return providerListPref.getValue();
			}
		}
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}
	
	
	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));
		isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		return isValid;
	}
	
	protected String getDomain() {
		String provider = providerListPref.getValue();
		if(provider != null) {
			String[] set = providers.get(provider);
			return set[0];
		}
		return "";
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}

	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		//Disable ICE and turn on STUN!!! 
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		String provider = providerListPref.getValue();
		if(provider != null) {
			String[] set = providers.get(provider);
			if(set[1] != null) {
				prefs.addStunServer(set[1]);
			}
		}
		
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, false);
	}
	
	
	// Balance consulting

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
						String provider = providerListPref.getValue();
						if(provider != null) {
							String[] set = providers.get(provider);
							requestURL += set[0].replace("sip.", "www.")+URL_BALANCE;
							requestURL += "?username=" + acc.username;
							requestURL += "&password=" + acc.data;
							
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
