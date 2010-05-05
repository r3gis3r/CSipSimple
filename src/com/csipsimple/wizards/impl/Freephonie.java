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

public class Freephonie extends BasePrefsWizard {
	public static String label = "Freephonie";
	public static String id = "FREEPHONIE";
	public static int icon = R.drawable.ic_wizard_freephonie;
	public static int priority = 10;
	protected static final String THIS_FILE = "Freephonie W";

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountPhoneNumber;
	private EditTextPreference accountPassword;

	
	protected void fillLayout() {
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountPhoneNumber = (EditTextPreference) findPreference("phone_number");
		accountPassword = (EditTextPreference) findPreference("password");

		
		
		String display_name = account.display_name;
		if(display_name.equalsIgnoreCase("")) {
			display_name = "Freephonie";
		}
		accountDisplayName.setText(display_name);
		String account_cfgid = account.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@.*");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			account_cfgid = m.group(1);
		}

		accountPhoneNumber.setText(account_cfgid);
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		accountPassword.setText(ci.getData().getPtr());
	}

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("phone_number");
		setPasswordFieldSummary("password");
	}

	protected boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		isValid &= checkField(accountPhoneNumber, isEmpty(accountPhoneNumber));

		return isValid;
	}

	protected void buildAccount() {
		Log.d(THIS_FILE, "begin of save ....");
		account.display_name = accountDisplayName.getText();
		// TODO add an user display name
		account.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ accountPhoneNumber.getText() + "@freephonie.net>"));
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:freephonie.net"));

		pjsip_cred_info credentials = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		credentials.setRealm(pjsua.pj_str_copy("freephonie.net"));
		credentials.setUsername(getPjText(accountPhoneNumber));
		credentials.setData(getPjText(accountPassword));
		credentials.setScheme(pjsua.pj_str_copy("Digest"));
		credentials.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD
				.swigValue());

		account.cfg.setReg_timeout(1800); // Minimum value for freephonie
											// server
	}

	@Override
	protected String getWizardId() {
		return id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_freephonie_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "freephonie";
	}
}
