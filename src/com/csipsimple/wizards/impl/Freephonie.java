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
import com.csipsimple.utils.Log;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;

public class Freephonie extends BasePrefsWizard {
	public static String label = "Freephonie";
	public static String id = "FREEPHONIE";
	public static int icon = R.drawable.ic_wizard_freephonie;
	public static int priority = 0;
	protected static final String THIS_FILE = "Freephonie W";

	private EditTextPreference mAccountDisplayName;
	private EditTextPreference mAccountPhoneNumber;
	private EditTextPreference mAccountPassword;

	
	protected void fillLayout() {
		mAccountDisplayName = (EditTextPreference) findPreference("display_name");
		mAccountPhoneNumber = (EditTextPreference) findPreference("phone_number");
		mAccountPassword = (EditTextPreference) findPreference("password");

		
		
		String display_name = mAccount.display_name;
		if(display_name.equalsIgnoreCase("")) {
			display_name = "Freephonie";
		}
		mAccountDisplayName.setText(display_name);
		String account_cfgid = mAccount.cfg.getId().getPtr();
		if(account_cfgid == null) {
			account_cfgid = "";
		}
		Pattern p = Pattern.compile("[^<]*<sip:([^@]*)@.*");
		Matcher m = p.matcher(account_cfgid);

		if (m.matches()) {
			account_cfgid = m.group(1);
		}

		mAccountPhoneNumber.setText(account_cfgid);
		
		pjsip_cred_info ci = mAccount.cfg.getCred_info();
		mAccountPassword.setText(ci.getData().getPtr());
	}

	protected void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("phone_number");
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
		if( isEmpty(mAccountPhoneNumber)) {
			return false;
		}

		return true;
	}

	protected void buildAccount() {
		Log.d(THIS_FILE, "begin of save ....");
		mAccount.display_name = mAccountDisplayName.getText();
		// TODO add an user display name
		mAccount.cfg.setId(pjsua.pj_str_copy("<sip:"
				+ mAccountPhoneNumber.getText() + "@freephonie.net>"));
		mAccount.cfg.setReg_uri(pjsua.pj_str_copy("sip:freephonie.net"));

		pjsip_cred_info ci = mAccount.cfg.getCred_info();

		mAccount.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("freephonie.net"));
		ci.setUsername(getPjText(mAccountPhoneNumber));
		ci.setData(getPjText(mAccountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD
				.swigValue());

		mAccount.cfg.setReg_timeout(1800); // Minimum value for freephonie
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
