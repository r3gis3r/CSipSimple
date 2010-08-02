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

import org.pjsip.pjsua.pjsip_cred_data_type;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.preference.EditTextPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class Ecs extends BasePrefsWizard {
	
	public static WizardInfo getWizardInfo() {
		WizardInfo result = new WizardInfo();
		result.id =  "ECS";
		result.label = "Alcatel-Lucent OmniPCX Office";
		result.icon = R.drawable.ic_wizard_ale;
		result.priority = 20;
		result.countries = new Locale[]{};
		result.isWorld = true;
		return result;
	}

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountPhoneNumber;
	private EditTextPreference accountUsername;
	private EditTextPreference accountPassword;
	private EditTextPreference accountServerDomain;
	private EditTextPreference accountServerIp;

	protected void fillLayout() {
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountPhoneNumber = (EditTextPreference) findPreference("phone_number");
		accountUsername = (EditTextPreference) findPreference("user_name");
		accountPassword = (EditTextPreference) findPreference("password");
		accountServerDomain = (EditTextPreference) findPreference("server_domain");
		accountServerIp = (EditTextPreference) findPreference("server_ip");

		pjsip_cred_info ci = account.cfg.getCred_info();

		
		accountDisplayName.setText(account.display_name);

		String domain_name = "";
		String account_cfgid = account.cfg.getId().getPtr();

		if (account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@([^@]*)>");
		Matcher m = p.matcher(account_cfgid);
		if (m.matches()) {
			account_cfgid = m.group(1);
			domain_name = m.group(2);
		}

		String server_ip = account.cfg.getReg_uri().getPtr();
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

		accountUsername.setText(ci.getUsername().getPtr());
		accountPassword.setText(ci.getData().getPtr());
	}

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("phone_number");
		setStringFieldSummary("user_name");
		setStringFieldSummary("server_ip");
		setStringFieldSummary("server_domain");
		setPasswordFieldSummary("password");
	}

	protected boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
		isValid &= checkField(accountPhoneNumber, isEmpty(accountPhoneNumber));
		isValid &= checkField(accountServerDomain, isEmpty(accountServerDomain));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));

		return isValid;
	}

	protected void buildAccount() {

		account.display_name = accountDisplayName.getText();

		// TODO add an user display name
		account.cfg.setId(pjsua.pj_str_copy("<sip:" + accountPhoneNumber.getText() + "@" + accountServerDomain.getText() + ">"));

		String server_ip = accountServerIp.getText();
		if (TextUtils.isEmpty(server_ip)) {
			server_ip = accountServerDomain.getText();
		}
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:" + server_ip));
		pjsip_cred_info ci = account.cfg.getCred_info();
		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUsername));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
	}

	
	@Override
	protected String getWizardId() {
		return getWizardInfo().id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_ecs_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "ecs";
	}

}
