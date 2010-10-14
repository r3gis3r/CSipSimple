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

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class Sipgate extends BasePrefsWizard {
	
	public static WizardInfo getWizardInfo() {
		WizardInfo result = new WizardInfo();
		result.id =  "SIPGATE";
		result.label = "Sipgate";
		result.icon = R.drawable.ic_wizard_sipgate;
		result.priority = 20;
		result.countries = new Locale[]{
				Locale.US,
				Locale.UK,
				Locale.GERMANY
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
			account.display_name = "Sipgate";
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

		//Override titles
		accountDisplayName.setTitle(R.string.w_sipgate_display_name);
		accountDisplayName.setDialogTitle(R.string.w_sipgate_display_name);
		accountServer.setTitle(R.string.w_sipgate_server);
		accountServer.setDialogTitle(R.string.w_sipgate_server);
		accountUserName.setTitle(R.string.w_sipgate_username);
		accountUserName.setDialogTitle(R.string.w_sipgate_username);
		accountPassword.setTitle(R.string.w_sipgate_password);
		accountPassword.setDialogTitle(R.string.w_sipgate_password);
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
		
		account.display_name = accountDisplayName.getText();
		account.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ accountUserName.getText() + "@"+accountServer.getText()+">"));
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:"+accountServer.getText()));

		pjsip_cred_info ci = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUserName));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));

		account.cfg.setProxy_cnt(1);
		pj_str_t[] proxies = account.cfg.getProxy();
		proxies[0] = pjsua.pj_str_copy("sip:"+accountServer.getText());
		account.cfg.setProxy(proxies);
		
		// Add stun server
		PreferencesWrapper prefs = new PreferencesWrapper((Context) this);
		if( ! (prefs.getStunEnabled()==1) || TextUtils.isEmpty(prefs.getStunServer())) {
			prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_STUN, true);
			prefs.setPreferenceStringValue(PreferencesWrapper.STUN_SERVER, "stun.sipgate.net:10000");
		}
		
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
		return "sipgate";
	}

	
}
