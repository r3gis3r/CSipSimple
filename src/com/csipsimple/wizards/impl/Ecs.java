/**
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

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;

public class Ecs extends BasePrefsWizard {
	public static String label = "Alcatel-Lucent OmniPCX Office";
	public static String id = "ECS";
	public static int icon = R.drawable.ic_wizard_ale;
	public static int priority = 0;

	private EditTextPreference mAccountDisplayName;
	private EditTextPreference mAccountPhoneNumber;
	private EditTextPreference mAccountUsername;
	private EditTextPreference mAccountPassword;
	private EditTextPreference mAccountServerDomain;
	private EditTextPreference mAccountServerIp;

	protected void fillLayout() {
		mAccountDisplayName = (EditTextPreference) findPreference("display_name");
		mAccountPhoneNumber = (EditTextPreference) findPreference("phone_number");
		mAccountUsername = (EditTextPreference) findPreference("user_name");
		mAccountPassword = (EditTextPreference) findPreference("password");
		mAccountServerDomain = (EditTextPreference) findPreference("server_domain");
		mAccountServerIp = (EditTextPreference) findPreference("server_ip");

		pjsip_cred_info ci = mAccount.cfg.getCred_info();

		mAccountDisplayName.setText(mAccount.display_name);

		String domain_name = "";
		String account_cfgid = mAccount.cfg.getId().getPtr();

		if (account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@([^@]*)>");
		Matcher m = p.matcher(account_cfgid);
		if (m.matches()) {
			account_cfgid = m.group(1);
			domain_name = m.group(2);
		}

		String server_ip = mAccount.cfg.getReg_uri().getPtr();
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

		mAccountPhoneNumber.setText(account_cfgid);
		mAccountServerDomain.setText(domain_name);

		mAccountUsername.setText(ci.getUsername().getPtr());
		mAccountPassword.setText(ci.getData().getPtr());
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
		if (isEmpty(mAccountDisplayName) || isEmpty(mAccountPassword) || isEmpty(mAccountPhoneNumber) || isEmpty(mAccountServerDomain) || isEmpty(mAccountUsername)) {
			return false;
		}

		return true;
	}

	protected void buildAccount() {

		mAccount.display_name = mAccountDisplayName.getText();

		// TODO add an user display name
		mAccount.cfg.setId(pjsua.pj_str_copy("<sip:" + mAccountPhoneNumber.getText() + "@" + mAccountServerDomain.getText() + ">"));

		String server_ip = mAccountServerIp.getText();
		if (server_ip.equalsIgnoreCase("")) {
			server_ip = mAccountServerDomain.getText();
		}
		mAccount.cfg.setReg_uri(pjsua.pj_str_copy("sip:" + server_ip));
		pjsip_cred_info ci = mAccount.cfg.getCred_info();
		mAccount.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(mAccountUsername));
		ci.setData(getPjText(mAccountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
	}

	@Override
	protected String getWizardId() {
		return id;
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
