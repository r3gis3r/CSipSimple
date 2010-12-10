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
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.pjsip.pjsua.pjsip_cred_info;

import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.format.DateFormat;

import com.csipsimple.models.Account;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;
import com.csipsimple.utils.PreferencesWrapper;

public class Ippi extends SimpleImplementation {


	protected static final String THIS_FILE = "IppiW";
	protected static final int DID_SUCCEED = 0;
	protected static final int DID_ERROR = 1;

	@Override
	protected String getDomain() {
		return "ippi.fr";
	}
	
	@Override
	protected String getDefaultName() {
		return "ippi";
	}

	
	//Customization
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);

		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

		if (account.id != null && account.id != Account.INVALID_ID) {
			final pjsip_cred_info ci = account.cfg.getCred_info();
			
			
			Thread t = new Thread() {

				public void run() {
					try {
						HttpClient httpClient = new DefaultHttpClient();
						
						String requestURL = "https://soap.ippi.fr/credit/check_credit.php?"
							+ "login=" + ci.getUsername().getPtr()
							+ "&code=" + MD5.MD5Hash(ci.getData().getPtr() + DateFormat.format("yyyyMMdd", new Date()));
						HttpGet httpGet = new HttpGet(requestURL);

						// Create a response handler
						HttpResponse httpResponse = httpClient.execute(httpGet);
						if(httpResponse.getStatusLine().getStatusCode() == 200) {
							InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
							BufferedReader br = new BufferedReader(isr);
							creditHandler.sendMessage(creditHandler.obtainMessage(DID_SUCCEED, br.readLine()));
						}else {
							creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
						}
					} catch (Exception e) {
						creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
					}
				}
			};
			t.start();
		}
	}

	private Handler creditHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case DID_SUCCEED: {
				String response = (String) message.obj;
				Log.d(THIS_FILE, "Response : " + response);
				
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
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_STUN, true);
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_ICE, true);
		prefs.addStunServer("stun.ippi.fr");
	}
	
	@Override
	protected boolean canTcp() {
		return true;
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
}
