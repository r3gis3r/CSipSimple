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

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_cred_data_type;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.preference.EditTextPreference;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.utils.Log;

public class Basic extends BaseImplementation {
	protected static final String THIS_FILE = "Basic W";

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountUserName;
	private EditTextPreference accountServer;
	private EditTextPreference accountPassword;

	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference("display_name");
		accountUserName = (EditTextPreference) parent.findPreference("username");
		accountServer = (EditTextPreference) parent.findPreference("server");
		accountPassword = (EditTextPreference) parent.findPreference("password");
	}
	
	public void fillLayout(Account account) {
		bindFields();
		
		accountDisplayName.setText(account.display_name);
		String server = "";
		String account_cfgid = account.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@([^>]*)>");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			account_cfgid = m.group(1);
			server = m.group(2);
		}
		

		accountUserName.setText(account_cfgid);
		accountServer.setText(server);
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		accountPassword.setText(ci.getData().getPtr());
	}

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("username");
		setStringFieldSummary("server");
		setPasswordFieldSummary("password");
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203615L;

	{
		put("display_name", R.string.w_common_display_name_desc);
		put("username", R.string.w_basic_username_desc);
		put("server", R.string.w_common_server_desc);
		put("password", R.string.w_basic_password_desc);
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
		isValid &= checkField(accountServer, isEmpty(accountServer));
		isValid &= checkField(accountUserName, isEmpty(accountUserName));

		return isValid;
	}

	public Account buildAccount(Account account) {
		Log.d(THIS_FILE, "begin of save ....");
		account.display_name = accountDisplayName.getText();
		// TODO add an user display name
		account.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ accountUserName.getText() + "@"+accountServer.getText()+">"));
		
		pj_str_t regUri = pjsua.pj_str_copy("sip:"+accountServer.getText());
		account.cfg.setReg_uri(regUri);
		account.cfg.setProxy_cnt(1);
		pj_str_t[] proxies = account.cfg.getProxy();
		proxies[0] = regUri;
		account.cfg.setProxy(proxies);

		pjsip_cred_info ci = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUserName));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD
				.swigValue());
		//By default auto transport
		account.transport = Account.TRANSPORT_AUTO;
		return account;
	}

	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_basic_preferences;
	}

	@Override
	public boolean needRestart() {
		return false;
	}
}
