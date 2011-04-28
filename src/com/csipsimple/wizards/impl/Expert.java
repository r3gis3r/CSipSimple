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

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
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
	private ListPreference accountTransport;
	private CheckBoxPreference accountPublishEnabled;
	private EditTextPreference accountRegTimeout;
//	private EditTextPreference accountKaInterval;
	private EditTextPreference accountForceContact;
	private CheckBoxPreference accountAllowContactRewrite;
	private ListPreference accountContactRewriteMethod;
	private EditTextPreference accountProxy;
	private ListPreference accountUseSrtp;
	private EditTextPreference accountRegDelayRefresh;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference("display_name");
		accountAccId = (EditTextPreference) parent.findPreference("acc_id");
		accountRegUri = (EditTextPreference) parent.findPreference("reg_uri");
		accountRealm = (EditTextPreference) parent.findPreference("realm");
		accountUserName = (EditTextPreference) parent.findPreference("username");
		accountData = (EditTextPreference) parent.findPreference("data");
		accountDataType = (ListPreference) parent.findPreference("data_type");
		accountScheme = (ListPreference) parent.findPreference("scheme");
		accountTransport = (ListPreference) parent.findPreference("transport");
		accountUseSrtp = (ListPreference) parent.findPreference("use_srtp");
		accountPublishEnabled = (CheckBoxPreference) parent.findPreference("publish_enabled");
		accountRegTimeout = (EditTextPreference) parent.findPreference("reg_timeout");
		accountRegDelayRefresh = (EditTextPreference) parent.findPreference("reg_delay_before_refresh");
		accountForceContact = (EditTextPreference) parent.findPreference("force_contact");
		accountAllowContactRewrite = (CheckBoxPreference) parent.findPreference("allow_contact_rewrite");
		accountContactRewriteMethod = (ListPreference) parent.findPreference("contact_rewrite_method");
		accountProxy = (EditTextPreference) parent.findPreference("proxy");
	}

	public void fillLayout(final SipProfile account) {
		bindFields();


		accountDisplayName.setText(account.display_name);
		accountAccId.setText(account.acc_id);
		accountRegUri.setText(account.reg_uri);
		accountRealm.setText(account.realm);
		accountUserName.setText(account.username);
		accountData.setText(account.data);

		{
			String scheme = account.scheme;
			if (scheme != null && !scheme.equals("")) {
				accountScheme.setValue(scheme);
			} else {
				accountScheme.setValue("Digest");
			}
		}
		{
			int ctype = account.datatype;
			if (ctype == SipProfile.CRED_DATA_PLAIN_PASSWD) {
				accountDataType.setValueIndex(0);
			} else if (ctype == SipProfile.CRED_DATA_DIGEST) {
				accountDataType.setValueIndex(1);
			}
			//DISABLED SINCE NOT SUPPORTED YET
			/*
			else if (ctype ==SipProfile.CRED_CRED_DATA_EXT_AKA) {
				accountDataType.setValueIndex(2);
			} */else {
				accountDataType.setValueIndex(0);
			}
		}

		accountTransport.setValue(account.transport.toString());
		accountPublishEnabled.setChecked((account.publish_enabled == 1));
		accountRegTimeout.setText(Long.toString(account.reg_timeout));
		accountRegDelayRefresh.setText(Long.toString(account.reg_delay_before_refresh));
		
		accountForceContact.setText(account.force_contact);
		accountAllowContactRewrite.setChecked(account.allow_contact_rewrite);
		accountContactRewriteMethod.setValue(Integer.toString(account.contact_rewrite_method));
		if(account.proxies != null) {
			accountProxy.setText(TextUtils.join(SipProfile.PROXIES_SEPARATOR, account.proxies));
		}else {
			accountProxy.setText("");
		}
		Log.d(THIS_FILE, "use srtp : "+account.use_srtp);
		if(account.use_srtp >= 0) {
			accountUseSrtp.setValueIndex(account.use_srtp);
		}
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
		isValid &= checkField(accountRegUri, !isEmpty(accountRegUri) && !isMatching(accountRegUri, "sip(s)?:.*"));
		isValid &= checkField(accountProxy, !isEmpty(accountProxy) && !isMatching(accountProxy, "sip(s)?:.*"));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		account.display_name = accountDisplayName.getText();
		try {
			account.transport = Integer.parseInt(accountTransport.getValue());
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport is not a number");
		}
		account.acc_id = getText(accountAccId);
		account.reg_uri = getText(accountRegUri);
		try {
			account.use_srtp = Integer.parseInt(accountUseSrtp.getValue());
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Use srtp is not a number");
		}

		if (!isEmpty(accountUserName)) {
			account.realm = getText(accountRealm);
			account.username = getText(accountUserName);
			account.data = getText(accountData);
			account.scheme = accountScheme.getValue();
			
			
			String dataType = accountDataType.getValue();
			if(dataType.equalsIgnoreCase("0")) {
				account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
			}else if(dataType.equalsIgnoreCase("1")){
				account.datatype = SipProfile.CRED_DATA_DIGEST;
			}
			//DISABLED SINCE NOT SUPPORTED YET
			/*else if(dataType.equalsIgnoreCase("16")){
				ci.setData_type(SipProfile.CRED_DATA_EXT_AKA);
			} */else {
				account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
			}
		} else {
			account.realm = "";
			account.username = "";
			account.data = "";
			account.scheme = "Digest";
			account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		}

		account.publish_enabled = accountPublishEnabled.isChecked() ? 1 : 0;
		try {
			account.reg_timeout = Integer.parseInt(accountRegTimeout.getText());
		} catch (NumberFormatException e) {
			//Leave default
			//account.reg_timeout = 900;
		}
		try {
			int reg_delay =  Integer.parseInt(accountRegDelayRefresh.getText());
			if(reg_delay > 0) {
				account.reg_delay_before_refresh = reg_delay;
			}
		}catch (NumberFormatException e) {
			//Leave default
			//account.reg_timeout = 900;
		}
		
		try {
			account.contact_rewrite_method = Integer.parseInt(accountContactRewriteMethod.getValue());
		} catch (NumberFormatException e) {
			//DO nothing
		}
		account.allow_contact_rewrite = accountAllowContactRewrite.isChecked();
		String forceContact = accountForceContact.getText();
		if(!TextUtils.isEmpty(forceContact)) {
			account.force_contact = getText(accountForceContact);
		}else {
			account.force_contact = "";
		}
		
		if (!isEmpty(accountProxy)) {
			account.proxies = new String[] { accountProxy.getText() };
		} else {
			account.proxies = null;
		}
		
		return account;
	}


	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_expert_preferences;
	}
	
	@Override
	public boolean needRestart() {
		return false;
	}
}
