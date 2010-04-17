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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pjsip_cred_data_type;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.preference.EditTextPreference;
import com.csipsimple.utils.Log;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;

public class Basic extends BasePrefsWizard {
	public static String label = "Basic";
	public static String id = "BASIC";
	public static int icon = R.drawable.ic_wizard_basic;
	public static int priority = 100;
	protected static final String THIS_FILE = "Basic W";

	private EditTextPreference mAccountDisplayName;
	private EditTextPreference mAccountUserName;
	private EditTextPreference mAccountServer;
	private EditTextPreference mAccountPassword;

	
	protected void fillLayout() {
		mAccountDisplayName = (EditTextPreference) findPreference("display_name");
		mAccountUserName = (EditTextPreference) findPreference("username");
		mAccountServer = (EditTextPreference) findPreference("server");
		mAccountPassword = (EditTextPreference) findPreference("password");

		
		
		mAccountDisplayName.setText(mAccount.display_name);
		String server = "";
		String account_cfgid = mAccount.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@([^>]*)>");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			account_cfgid = m.group(1);
			server = m.group(2);
		}
		

		mAccountUserName.setText(account_cfgid);
		mAccountServer.setText(server);
		
		pjsip_cred_info ci = mAccount.cfg.getCred_info();
		mAccountPassword.setText(ci.getData().getPtr());
	}

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("username");
		setStringFieldSummary("server");
		setPasswordFieldSummary("password");
	}

	protected boolean canSave() {
		if( isEmpty(mAccountDisplayName)) {
//			View pref_view = (View) getPreferenceScreen().getRootAdapter().getItem(mAccountDisplayName.getOrder());
//			TextView tv = (TextView) pref_view.findViewById(R.id.title); 
//			tv.setTextColor(Color.RED);
			return false;
		}
		if( isEmpty(mAccountPassword) ) {
			return false;
		}
		if( isEmpty(mAccountServer)) {
			return false;
		}
		if( isEmpty(mAccountUserName)) {
			return false;
		}

		return true;
	}

	protected void buildAccount() {
		Log.d(THIS_FILE, "begin of save ....");
		mAccount.display_name = mAccountDisplayName.getText();
		// TODO add an user display name
		mAccount.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ mAccountUserName.getText() + "@"+mAccountServer.getText()+">"));
		mAccount.cfg.setReg_uri(pjsua.pj_str_copy("sip:"+mAccountServer.getText()));

		pjsip_cred_info ci = mAccount.cfg.getCred_info();

		mAccount.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(mAccountUserName));
		ci.setData(getPjText(mAccountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD
				.swigValue());

	}

	@Override
	protected String getWizardId() {
		return id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_basic_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "basic";
	}
}
