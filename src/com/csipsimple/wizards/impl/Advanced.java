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

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import com.csipsimple.utils.Log;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class Advanced extends BasePrefsWizard {
	protected static final String THIS_FILE = "Advanced W";
	
	public static WizardInfo getWizardInfo() {
		WizardInfo result = new WizardInfo();
		result.id = "ADVANCED";
		result.label = "Advanced";
		result.icon = R.drawable.ic_wizard_advanced;
		result.priority = 2;
		result.countries = new Locale[] {};
		result.isGeneric = true;
		return result;
	}

	private EditTextPreference accountDisplayName;
	private EditTextPreference accountUserName;
	private EditTextPreference accountServer;
	private EditTextPreference accountPassword;
	private EditTextPreference accountCallerId;
	private CheckBoxPreference accountUseTcp;
	private EditTextPreference accountProxy;

	protected void fillLayout() {
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountCallerId = (EditTextPreference) findPreference("caller_id");
		accountServer = (EditTextPreference) findPreference("server");
		accountUserName = (EditTextPreference) findPreference("username");
		accountPassword = (EditTextPreference) findPreference("password");
		accountUseTcp = (CheckBoxPreference) findPreference("use_tcp");
		accountProxy = (EditTextPreference) findPreference("proxy");
		
		accountDisplayName.setText(account.display_name);
		
		String server = "";
		String caller_id = "";
		String account_cfgid = account.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("([^<]*)<sip:([^@]*)@([^>]*)>");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			caller_id = m.group(1);
			account_cfgid = m.group(2);
			server = m.group(3);
		}
		
		accountServer.setText(server);
		accountCallerId.setText(caller_id);
		accountUserName.setText(account_cfgid);
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		accountPassword.setText(ci.getData().getPtr());

		accountUseTcp.setChecked((account.use_tcp));
		accountProxy.setText(account.cfg.getProxy().getPtr());

	}

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("caller_id");
		setStringFieldSummary("server");
		setStringFieldSummary("username");
		setPasswordFieldSummary("password");
		setStringFieldSummary("proxy");
	}

	protected boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountCallerId, isEmpty(accountCallerId));
		isValid &= checkField(accountServer, isEmpty(accountServer));
		isValid &= checkField(accountUserName, isEmpty(accountUserName));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));

		return isValid;
	}

	protected void buildAccount() {
		Log.d(THIS_FILE, "begin of save ....");
		account.display_name = accountDisplayName.getText();
		account.cfg.setId(pjsua.pj_str_copy(accountCallerId.getText() + " <sip:"
				+ accountUserName.getText() + "@"+accountServer.getText()+">"));
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:"+accountServer.getText()));

		pjsip_cred_info ci = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUserName));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD
				.swigValue());

		account.use_tcp = accountUseTcp.isChecked();

		if (!isEmpty(accountProxy)) {
			account.cfg.setProxy_cnt(1);
			account.cfg.setProxy(getPjText(accountProxy));
		} else {
			account.cfg.setProxy_cnt(0);
		}
	}

	@Override
	protected String getWizardId() {
		return getWizardInfo().id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_advanced_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "advanced";
	}
}
