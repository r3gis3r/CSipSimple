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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.preference.EditTextPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class MagicJack extends BasePrefsWizard {
	
	public static WizardInfo getWizardInfo() {
		WizardInfo result = new WizardInfo();
		result.id =  "MAGICJACK";
		result.label = "MagicJack";
		result.icon = R.drawable.ic_wizard_magicjack;
		result.priority = 50;
		result.countries = new Locale[]{
			Locale.US,
			Locale.CANADA
		};
		
		result.isWorld = false;
		return result;
	}
	

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountUserName;
	private EditTextPreference accountServer;
	private EditTextPreference accountPassword;

	protected void fillLayout() {
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountServer = (EditTextPreference) findPreference("server");
		accountUserName = (EditTextPreference) findPreference("username");
		accountPassword = (EditTextPreference) findPreference("password");
		
		if(TextUtils.isEmpty(account.display_name)) {
			account.display_name = "MagicJack";
		}
		accountDisplayName.setText(account.display_name);
		
		String server = "";
		String account_cfgid = account.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("<sip:([^@]*)@([^>]*)>");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			account_cfgid = m.group(1);
			server = m.group(2);
		}
		
		accountServer.setText(server);
		accountUserName.setText(account_cfgid);
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		accountPassword.setText(ci.getData().getPtr());


	}
	

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("server");
		setStringFieldSummary("username");
		setPasswordFieldSummary("password");
	}

	protected boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountServer, isEmpty(accountServer));
		isValid &= checkField(accountUserName, isEmpty(accountUserName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));

		return isValid;
	}

	protected void buildAccount() { 
		String port = "5070";
		
		account.display_name = accountDisplayName.getText();
		account.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ accountUserName.getText() + "@"+accountServer.getText()+">"));
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:"+accountServer.getText()+":"+port));

		pjsip_cred_info ci = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUserName));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(8); // 8 is MJ digest auth

		account.cfg.setProxy_cnt(1);
		account.cfg.setProxy(pjsua.pj_str_copy("sip:"+accountServer.getText()+":"+port));
		
	}

	@Override
	protected String getWizardId() {
		return getWizardInfo().id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_magicjack_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "magicjack";
	}
}
