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

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsip_cred_data_type;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.utils.Log;

public class Expert extends BaseImplementation {

	private static final String THIS_FILE = "Expert";


	private EditTextPreference accountDisplayName;
	private EditTextPreference accountAccId;
	private EditTextPreference accountRegUri;
	private EditTextPreference accountUserName;
	private EditTextPreference accountData;
	private ListPreference accountDataType;
	private EditTextPreference accountRealm;
	private ListPreference accountScheme;
	private CheckBoxPreference accountUseTcp;
	private CheckBoxPreference accountPublishEnabled;
	private EditTextPreference accountRegTimeout;
	private EditTextPreference accountKaInterval;
	private EditTextPreference accountForceContact;
	private EditTextPreference accountProxy;
	private ListPreference accountUseSrtp;
	private CheckBoxPreference accountPreventTcp;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference("display_name");
		accountAccId = (EditTextPreference) parent.findPreference("acc_id");
		accountRegUri = (EditTextPreference) parent.findPreference("reg_uri");
		accountRealm = (EditTextPreference) parent.findPreference("realm");
		accountUserName = (EditTextPreference) parent.findPreference("username");
		accountData = (EditTextPreference) parent.findPreference("data");
		accountDataType = (ListPreference) parent.findPreference("data_type");
		accountScheme = (ListPreference) parent.findPreference("scheme");
		accountUseTcp = (CheckBoxPreference) parent.findPreference("use_tcp");
		accountPreventTcp = (CheckBoxPreference) parent.findPreference("prevent_tcp");
		accountUseSrtp = (ListPreference) parent.findPreference("use_srtp");
		accountPublishEnabled = (CheckBoxPreference) parent.findPreference("publish_enabled");
		accountRegTimeout = (EditTextPreference) parent.findPreference("reg_timeout");
		accountKaInterval = (EditTextPreference) parent.findPreference("ka_interval");
		accountForceContact = (EditTextPreference) parent.findPreference("force_contact");
		accountProxy = (EditTextPreference) parent.findPreference("proxy");
	}

	public void fillLayout(Account account) {
		bindFields();

		pjsip_cred_info ci = account.cfg.getCred_info();

		accountDisplayName.setText(account.display_name);
		accountAccId.setText(account.cfg.getId().getPtr());
		accountRegUri.setText(account.cfg.getReg_uri().getPtr());
		accountRealm.setText(ci.getRealm().getPtr());
		accountUserName.setText(ci.getUsername().getPtr());
		accountData.setText(ci.getData().getPtr());

		{
			String scheme = ci.getScheme().getPtr();
			if (scheme != null && !scheme.equals("")) {
				accountScheme.setValue(ci.getScheme().getPtr());
			} else {
				accountScheme.setValue("Digest");
			}
		}
		{
			int ctype = ci.getData_type();
			if (ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue()) {
				accountDataType.setValueIndex(0);
			} else if (ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_DIGEST.swigValue()) {
				accountDataType.setValueIndex(1);
			}
			//DISABLED SINCE NOT SUPPORTED YET
			/*
			else if (ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_EXT_AKA.swigValue()) {
				accountDataType.setValueIndex(2);
			} */else {
				accountDataType.setValueIndex(0);
			}
		}

		accountUseTcp.setChecked((account.use_tcp));
		accountPreventTcp.setChecked((account.prevent_tcp));
		accountPublishEnabled.setChecked((account.cfg.getPublish_enabled() == 1));
		accountRegTimeout.setText(Long.toString(account.cfg.getReg_timeout()));
		accountKaInterval.setText(Long.toString(account.cfg.getKa_interval()));
		
		accountForceContact.setText(account.cfg.getForce_contact().getPtr());
		accountProxy.setText(account.cfg.getProxy()[0].getPtr());
		
		accountUseSrtp.setValueIndex(account.cfg.getUse_srtp().swigValue());
	}
	

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("acc_id");
		setStringFieldSummary("reg_uri");
		setStringFieldSummary("realm");
		setStringFieldSummary("username");
		setStringFieldSummary("proxy");
		setPasswordFieldSummary("data");
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5469900404720631144L;

	{
		put("display_name", R.string.w_common_display_name_desc);
		put("acc_id", R.string.w_expert_acc_id_desc);
		put("reg_uri", R.string.w_expert_reg_uri_desc);
		put("realm", R.string.w_expert_realm_desc);
		put("username", R.string.w_expert_username_desc);
		put("proxy", R.string.w_expert_proxy_desc);
		put("data", R.string.w_expert_data_desc);
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
		isValid &= checkField(accountAccId, isEmpty(accountAccId) || !isMatching(accountAccId, "[^<]*<sip(s)?:[^@]*@[^@]*>"));
		isValid &= checkField(accountRegUri, isEmpty(accountRegUri) || !isMatching(accountRegUri, "sip(s)?:.*"));
		isValid &= checkField(accountProxy, !isEmpty(accountProxy) && !isMatching(accountProxy, "sip(s)?:.*"));

		return isValid;
	}

	public Account buildAccount(Account account) {
		account.display_name = accountDisplayName.getText();
		account.use_tcp = accountUseTcp.isChecked();
		account.prevent_tcp = accountPreventTcp.isChecked();
		account.cfg.setId(getPjText(accountAccId));
		account.cfg.setReg_uri(getPjText(accountRegUri));
		try {
			account.cfg.setUse_srtp(pjmedia_srtp_use.swigToEnum(Integer.parseInt(accountUseSrtp.getValue())));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Use srtp is not a number");
		}
		pjsip_cred_info ci = account.cfg.getCred_info();

		if (!isEmpty(accountUserName)) {
			account.cfg.setCred_count(1);
			ci.setRealm(getPjText(accountRealm));
			ci.setUsername(getPjText(accountUserName));
			ci.setData(getPjText(accountData));
			ci.setScheme(pjsua.pj_str_copy(accountScheme.getValue()));
			
			
			String dataType = accountDataType.getValue();
			if(dataType.equalsIgnoreCase("0")) {
				ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
			}else if(dataType.equalsIgnoreCase("1")){
				ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_DIGEST.swigValue());
			}
			//DISABLED SINCE NOT SUPPORTED YET
			/*else if(dataType.equalsIgnoreCase("16")){
				ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_EXT_AKA.swigValue());
			} */else {
				ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
			}
		} else {
			account.cfg.setCred_count(0);
			ci.setRealm(pjsua.pj_str_copy(""));
			ci.setUsername(pjsua.pj_str_copy(""));
			ci.setData(pjsua.pj_str_copy(""));
			ci.setScheme(pjsua.pj_str_copy("Digest"));
			ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
		}

		account.cfg.setPublish_enabled((accountPublishEnabled.isChecked()) ? 1 : 0);
		try {
			account.cfg.setReg_timeout(Integer.parseInt(accountRegTimeout.getText()));
		} catch (NumberFormatException e) {
			account.cfg.setReg_timeout(0);
		}
		try {
			account.cfg.setKa_interval(Integer.parseInt(accountKaInterval.getText()));
		} catch (NumberFormatException e) {
			account.cfg.setKa_interval(0);
		}
		
		/*
		 * account.cfg.setForce_contact(getPjText(accountForceContact));
		 */
		if (!isEmpty(accountProxy)) {
			account.cfg.setProxy_cnt(1);
			pj_str_t[] proxies = account.cfg.getProxy();
			proxies[0] = getPjText(accountProxy);
			account.cfg.setProxy(proxies);
		} else {
			account.cfg.setProxy_cnt(0);
		}
		
		return account;
	}


	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_expert_preferences;
	}
}
