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

import android.preference.EditTextPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.utils.Log;

import java.util.HashMap;

public class Basic extends BaseImplementation {
	protected static final String THIS_FILE = "Basic W";

	private EditTextPreference etAccountDisplayName;
	private EditTextPreference etAccountUserName;
	private EditTextPreference etAccountServer;
	private EditTextPreference etAccountPassword;

	private void bindFields() {
		etAccountDisplayName = (EditTextPreference) findPreference("display_name");
		etAccountUserName = (EditTextPreference) findPreference("username");
		etAccountServer = (EditTextPreference) findPreference("server");
		etAccountPassword = (EditTextPreference) findPreference("password");
	}
	
	public void fillLayout(final SipProfile account) {
		bindFields();

		String serverFull = account.reg_uri;
		if (serverFull == null) {
			serverFull = "";
		}else {
			serverFull = serverFull.replaceFirst("sip:", "");
		}
		
		ParsedSipContactInfos parsedInfo = SipUri.parseSipContact(account.acc_id);

		etAccountDisplayName.setText(account.display_name);
		etAccountUserName.setText(parsedInfo.userName);
		etAccountServer.setText(serverFull);
		etAccountPassword.setText(account.data);
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
		
		isValid &= checkField(etAccountDisplayName, isEmpty(etAccountDisplayName));
		isValid &= checkField(etAccountPassword, isEmpty(etAccountPassword));
		isValid &= checkField(etAccountServer, isEmpty(etAccountServer));
		isValid &= checkField(etAccountUserName, isEmpty(etAccountUserName));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		Log.d(THIS_FILE, "begin of save ....");
		account.display_name = etAccountDisplayName.getText().trim();
		
		String[] serverParts = etAccountServer.getText().split(":");
		account.acc_id = "<sip:" + SipUri.encodeUser(etAccountUserName.getText().trim()) + "@"+serverParts[0].trim()+">";
		
		String regUri = "sip:" + etAccountServer.getText();
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri } ;


		account.realm = "*";
		account.username = getText(etAccountUserName).trim();
		account.data = getText(etAccountPassword);
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		//By default auto transport
		account.transport = SipProfile.TRANSPORT_UDP;
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
