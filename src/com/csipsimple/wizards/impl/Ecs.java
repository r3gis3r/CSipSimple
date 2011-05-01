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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.preference.EditTextPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;

public class Ecs extends BaseImplementation {
	
	protected static String DISPLAY_NAME = "display_name";
	protected static String PHONE_NUMBER = "phone_number";
	protected static String USER_NAME = "user_name";
	protected static String SERVER =  "server_ip";
	protected static String DOMAIN = "server_domain";
	protected static String PASSWORD = "password";

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountPhoneNumber;
	private EditTextPreference accountUsername;
	private EditTextPreference accountPassword;
	private EditTextPreference accountServerDomain;
	private EditTextPreference accountServerIp;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference(DISPLAY_NAME);
		accountPhoneNumber = (EditTextPreference) parent.findPreference(PHONE_NUMBER);
		accountUsername = (EditTextPreference) parent.findPreference(USER_NAME);
		accountPassword = (EditTextPreference) parent.findPreference(PASSWORD);
		accountServerDomain = (EditTextPreference) parent.findPreference(DOMAIN);
		accountServerIp = (EditTextPreference) parent.findPreference(SERVER);
		
	}

	@Override
	public void fillLayout(final SipProfile account) {
		bindFields();

		accountDisplayName.setText(account.display_name);

		String domain_name = "";
		String account_cfgid = account.acc_id;

		if (account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@([^@]*)>");
		Matcher m = p.matcher(account_cfgid);
		if (m.matches()) {
			account_cfgid = m.group(1);
			domain_name = m.group(2);
		}

		String server_ip = account.reg_uri;
		if (server_ip == null) {
			server_ip = "";
		}
		p = Pattern.compile("sip:([^@]*)");
		m = p.matcher(server_ip);
		if (m.matches()) {
			server_ip = m.group(1);
		}

		if (server_ip.equalsIgnoreCase(domain_name)) {
			server_ip = "";
		}

		accountPhoneNumber.setText(account_cfgid);
		accountServerDomain.setText(domain_name);

		accountUsername.setText(account.username);
		accountPassword.setText(account.data);
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = 3055562364235868653L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(PHONE_NUMBER, R.string.w_ecs_phone_number_desc);
		put(USER_NAME, R.string.w_ecs_user_name_desc);
		put(SERVER, R.string.w_ecs_server_ip_desc);
		put(DOMAIN, R.string.w_ecs_server_domain_desc);
		put(PASSWORD, R.string.w_ecs_password_desc);
	}};

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}


	@Override
	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(PHONE_NUMBER);
		setStringFieldSummary(USER_NAME);
		setStringFieldSummary(SERVER);
		setStringFieldSummary(DOMAIN);
		setPasswordFieldSummary(PASSWORD);
	}

	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		isValid &= checkField(accountPhoneNumber, isEmpty(accountPhoneNumber));
		isValid &= checkField(accountServerDomain, isEmpty(accountServerDomain));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		
		account.display_name = accountDisplayName.getText();

		// TODO add an user display name
		account.acc_id = "<sip:" + accountPhoneNumber.getText() + "@" + accountServerDomain.getText() + ">";

		String server_ip = accountServerIp.getText();
		if (TextUtils.isEmpty(server_ip)) {
			server_ip = accountServerDomain.getText();
		}
		String regUri = "sip:" + server_ip;
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri } ;
		
		
		account.realm = "*";
		account.username = getText(accountUsername);
		account.data = getText(accountPassword);
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		
		return account;
	}

	

	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_ecs_preferences;
	}

	@Override
	public boolean needRestart() {
		return false;
	}

}
