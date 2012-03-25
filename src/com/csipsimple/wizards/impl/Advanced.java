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

import java.util.HashMap;

import android.net.Uri;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.utils.Log;

public class Advanced extends BaseImplementation {
	protected static final String THIS_FILE = "Advanced W";
	
	protected EditTextPreference accountDisplayName;
	protected EditTextPreference accountUserName;
	protected EditTextPreference accountServer;
	protected EditTextPreference accountPassword;
	protected EditTextPreference accountCallerId;
	protected CheckBoxPreference accountUseTcp;
	protected EditTextPreference accountProxy;
	protected EditTextPreference accountAuthId;
	
	protected final static String FIELD_DISPLAY_NAME = "display_name";
    protected final static String FIELD_CALLER_ID = "caller_id";
    protected final static String FIELD_SERVER = "server";
    protected final static String FIELD_USERNAME = "username";
    protected final static String FIELD_AUTH_ID = "auth_id";
    protected final static String FIELD_PASSWORD = "password";
    protected final static String FIELD_TCP = "use_tcp";
    protected final static String FIELD_PROXY = "proxy";
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) findPreference(FIELD_DISPLAY_NAME);
		accountCallerId = (EditTextPreference) findPreference(FIELD_CALLER_ID);
		accountServer = (EditTextPreference) findPreference(FIELD_SERVER);
		accountUserName = (EditTextPreference) findPreference(FIELD_USERNAME);
        accountAuthId = (EditTextPreference) findPreference(FIELD_AUTH_ID);
		accountPassword = (EditTextPreference) findPreference(FIELD_PASSWORD);
		accountUseTcp = (CheckBoxPreference) findPreference(FIELD_TCP);
		accountProxy = (EditTextPreference) findPreference(FIELD_PROXY);
	}

	public void fillLayout(final SipProfile account) {
		bindFields();
		
		accountDisplayName.setText(account.display_name);
		
		
		ParsedSipContactInfos parsedInfo = SipUri.parseSipContact(account.acc_id);
		
		String serverFull = account.reg_uri;
		if (serverFull == null) {
			serverFull = "";
		}else {
			serverFull = serverFull.replaceFirst("sip:", "");
		}
		accountServer.setText(serverFull);
		accountCallerId.setText(parsedInfo.displayName);
		accountUserName.setText(parsedInfo.userName);
		
		if(!TextUtils.isEmpty(account.username)) {
		    if(!account.username.equals(parsedInfo.userName)) {
		        accountAuthId.setText(account.username);
		    }else {
		        accountAuthId.setText("");
		    }
		}else {
		    accountAuthId.setText("");
		}
		
		accountPassword.setText(account.data);

		accountUseTcp.setChecked(account.transport == SipProfile.TRANSPORT_TCP);
		
		if(account.proxies != null && account.proxies.length > 0) {
			accountProxy.setText(account.proxies[0].replaceFirst("sip:", ""));
		}else {
			accountProxy.setText("");
		}
	}

	public void updateDescriptions() {
		setStringFieldSummary(FIELD_DISPLAY_NAME);
		setStringFieldSummary(FIELD_CALLER_ID);
		setStringFieldSummary(FIELD_SERVER);
		setStringFieldSummary(FIELD_USERNAME);
		setStringFieldSummary(FIELD_AUTH_ID);
		setPasswordFieldSummary(FIELD_PASSWORD);
		setStringFieldSummary(FIELD_PROXY);
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = 3055562364235868653L;

	{
		put(FIELD_DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(FIELD_CALLER_ID, R.string.w_advanced_caller_id_desc);
		put(FIELD_SERVER, R.string.w_common_server_desc);
		put(FIELD_USERNAME, R.string.w_advanced_username_desc);
        put(FIELD_AUTH_ID, R.string.w_advanced_auth_id_desc);
		put(FIELD_PASSWORD, R.string.w_advanced_password_desc);
		put(FIELD_PROXY, R.string.w_advanced_proxy_desc);
	}};

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}

	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		//isValid &= checkField(accountCallerId, isEmpty(accountCallerId));
		isValid &= checkField(accountServer, isEmpty(accountServer));
		isValid &= checkField(accountUserName, isEmpty(accountUserName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		Log.d(THIS_FILE, "begin of save ....");
		account.display_name = accountDisplayName.getText().trim();
		String[] serverParts = accountServer.getText().split(":");
		account.acc_id = accountCallerId.getText().trim() + 
			" <sip:" + Uri.encode(accountUserName.getText().trim()) + "@" + serverParts[0].trim() + ">";
		
		account.reg_uri = "sip:" + accountServer.getText();

		account.realm = "*";
		
        account.username = getText(accountAuthId).trim();
        if (TextUtils.isEmpty(account.username)) {
            account.username = getText(accountUserName).trim();
        }
		account.data = getText(accountPassword);
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;

		account.transport = accountUseTcp.isChecked() ? SipProfile.TRANSPORT_TCP : SipProfile.TRANSPORT_AUTO;
		
		if (!isEmpty(accountProxy)) {
			account.proxies = new String[] { "sip:"+accountProxy.getText().trim() };
		} else {
			account.proxies = null;
		}
		return account;
	}

	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_advanced_preferences;
	}
	
	@Override
	public boolean needRestart() {
		return false;
	}

}
