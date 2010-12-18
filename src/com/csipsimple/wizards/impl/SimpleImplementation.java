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

import java.util.HashMap;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;

public abstract class SimpleImplementation extends BaseImplementation {
	//private static final String THIS_FILE = "SimplePrefsWizard";
	protected EditTextPreference accountDisplayName;
	protected EditTextPreference accountUsername;
	protected EditTextPreference accountPassword;
	protected CheckBoxPreference accountUseTcp;
	
	protected static String DISPLAY_NAME = "display_name";
	protected static String USER_NAME = "username";
	protected static String PASSWORD = "password";
	protected static String USE_TCP = "use_tcp";

	protected void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference(DISPLAY_NAME);
		accountUsername = (EditTextPreference) parent.findPreference(USER_NAME);
		accountPassword = (EditTextPreference) parent.findPreference(PASSWORD);
		accountUseTcp = (CheckBoxPreference) parent.findPreference(USE_TCP);
	}
	
	public void fillLayout(final SipProfile account) {
		bindFields();
		
		String display_name = account.display_name;
		if(display_name.equalsIgnoreCase("")) {
			display_name = getDefaultName();
		}
		accountDisplayName.setText(display_name);
		ParsedSipContactInfos parsedInfo = SipUri.parseSipContact(account.acc_id);
		
		accountUsername.setText(parsedInfo.userName);
		accountPassword.setText(account.data);
		
		if(canTcp()) {
			accountUseTcp.setChecked(account.transport == SipProfile.TRANSPORT_TCP);
		}else {
			hidePreference(null, USE_TCP);
		}
	}

	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(USER_NAME);
		setPasswordFieldSummary(PASSWORD);
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203615L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(USER_NAME, R.string.w_common_username_desc);
		put(PASSWORD, R.string.w_common_password_desc);
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
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		account.display_name = accountDisplayName.getText();
		// TODO add an user display name
		account.acc_id = "<sip:"
				+ accountUsername.getText() + "@"+getDomain()+">";
		
		String regUri = "sip:"+getDomain();
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri } ;

		
		account.realm = "*";
		account.username = getText(accountUsername);
		account.data = getText(accountPassword);
		account.scheme = "Digest";
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;

		account.reg_timeout = 1800;
		
		if(canTcp()) {
			account.transport = accountUseTcp.isChecked() ? SipProfile.TRANSPORT_TCP : SipProfile.TRANSPORT_UDP;
		}else {
			account.transport = SipProfile.TRANSPORT_UDP;
		}
		
		return account;
	}

	protected abstract String getDomain();
	protected abstract String getDefaultName();
	
	//This method may be overriden by a implementation
	protected boolean canTcp() {
		return false;
	}

	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_simple_preferences;
	}

	public boolean needRestart() {
		return false;
	}
}
