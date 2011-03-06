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

import android.preference.EditTextPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;

public class Local extends BaseImplementation {
	protected static final String THIS_FILE = "Advanced W";
	
	private EditTextPreference accountDisplayName;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference("display_name");
		hidePreference(null, "caller_id");
		hidePreference(null, "server");
		hidePreference(null, "username");
		hidePreference(null, "password");
		hidePreference(null, "use_tcp");
		hidePreference(null, "proxy");
	}

	public void fillLayout(final SipProfile account) {
		bindFields();
		
		accountDisplayName.setText(account.display_name);
		
	}

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = 3055562364235868653L;

	{
		put("display_name", R.string.w_common_display_name_desc);
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

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		Log.d(THIS_FILE, "begin of save ....");
		
		account.display_name = accountDisplayName.getText();
		account.reg_uri = "";
		account.acc_id = "";
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
