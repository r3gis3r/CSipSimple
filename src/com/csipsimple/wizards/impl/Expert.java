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


import org.pjsip.pjsua.pjsip_cred_data_type;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import com.csipsimple.wizards.BasePrefsWizard;

import com.csipsimple.R;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import com.csipsimple.utils.Log;

public class Expert extends BasePrefsWizard{

	private static final String TAG = "Expert W";
	public static String label = "Expert";
	public static String id = "EXPERT";
	public static int icon = R.drawable.ic_wizard_expert;
	public static int priority = -1;
	
	
	private EditTextPreference accountDisplayName;
	private EditTextPreference accountAccId;
	private EditTextPreference accountRegUri;
	private EditTextPreference accountUserName;
	private EditTextPreference accountData;
	private ListPreference accountDataType;
	private EditTextPreference accountRealm;
	private ListPreference accountScheme;
	private CheckBoxPreference accountPublishEnabled;
	private EditTextPreference accountRegTimeout;
	private EditTextPreference accountForceContact;
	private EditTextPreference accountProxy;
	
	
	
	
	protected void fillLayout(){
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountAccId = (EditTextPreference) findPreference("acc_id");
		accountRegUri = (EditTextPreference) findPreference("reg_uri");
		accountRealm = (EditTextPreference) findPreference("realm");
		accountUserName = (EditTextPreference) findPreference("username");
		accountData = (EditTextPreference) findPreference("data");
		accountDataType = (ListPreference) findPreference("data_type");
		accountScheme = (ListPreference) findPreference("scheme");
		accountPublishEnabled = (CheckBoxPreference) findPreference("publish_enabled");
		accountRegTimeout = (EditTextPreference) findPreference("reg_timeout");
		accountForceContact = (EditTextPreference) findPreference("force_contact");
		accountProxy = (EditTextPreference) findPreference("proxy");
		
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		
		accountDisplayName.setText(account.display_name);
		accountAccId.setText(account.cfg.getId().getPtr());
		accountRegUri.setText(account.cfg.getReg_uri().getPtr());
		accountRealm.setText(ci.getRealm().getPtr());
		accountUserName.setText(ci.getUsername().getPtr());
		accountData.setText(ci.getData().getPtr());
		
		{
			String scheme = ci.getScheme().getPtr();
			if(scheme != null && !scheme.equals("")){
				accountScheme.setValue(ci.getScheme().getPtr());
			}else{
				accountScheme.setValue("Digest");
			}
		}
		{
			int ctype=ci.getData_type();
			if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue()){
				accountDataType.setValueIndex(0);
			}else if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_DIGEST.swigValue()){
				accountDataType.setValueIndex(1);
			}else if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_EXT_AKA.swigValue()){
				accountDataType.setValueIndex(2);
			}else{
				accountDataType.setValueIndex(0);
			}
		}
		
		accountPublishEnabled.setChecked((account.cfg.getPublish_enabled() == 1));
		if(account.cfg.getReg_timeout() > 0){
			accountRegTimeout.setText(""+account.cfg.getReg_timeout());
		}else{
			accountRegTimeout.setText("");
		}
		accountForceContact.setText(account.cfg.getForce_contact().getPtr());
		accountProxy.setText(account.cfg.getProxy().getPtr());
	}
	
	protected void updateDescriptions(){
		setStringFieldSummary("display_name");
		setStringFieldSummary("acc_id");
		setStringFieldSummary("reg_uri");
		setStringFieldSummary("realm");
		setStringFieldSummary("username");
		setStringFieldSummary("proxy");
		setPasswordFieldSummary("data");
	}
	
	protected boolean canSave(){
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountAccId, isEmpty(accountAccId) ||  !isMatching(accountAccId, "[^<]*<sip(s)?:[^@]*@[^@]*>"));
		isValid &= checkField(accountRegUri, isEmpty(accountRegUri) || !isMatching(accountRegUri, "sip(s)?:.*" ));
		isValid &= checkField(accountProxy, ! isEmpty(accountProxy) && !isMatching(accountProxy, "sip(s)?:.*" ));
		
		return isValid;
	}
	

	protected void buildAccount(){
		Log.d(TAG, "begin of build ....");
		account.display_name = accountDisplayName.getText();
		account.cfg.setId(getPjText(accountAccId));
		account.cfg.setReg_uri(getPjText(accountRegUri));
		
		pjsip_cred_info ci = account.cfg.getCred_info();
		
		if( !isEmpty(accountUserName) ){
			account.cfg.setCred_count(1);
			ci.setRealm(getPjText(accountRealm));
			ci.setUsername(getPjText(accountUserName));
			ci.setData(getPjText(accountData));
			ci.setScheme(pjsua.pj_str_copy(accountScheme.getValue()));
			// FIXME this is not the good value !
			ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
			
		}else{
			account.cfg.setCred_count(0);
		}
		
		
		account.cfg.setPublish_enabled((accountPublishEnabled.isChecked())?1:0);
		try{
			account.cfg.setReg_timeout(Integer.parseInt(accountRegTimeout.getText()));
		}catch(NumberFormatException e){
			account.cfg.setReg_timeout(0);
		}
		/*
		account.cfg.setForce_contact(getPjText(accountForceContact));
		*/
		if( !isEmpty(accountProxy) ){
			account.cfg.setProxy_cnt(1);
			account.cfg.setProxy(getPjText(accountProxy));
		}else{
			account.cfg.setProxy_cnt(0);
		}
		
	}

	@Override
	protected String getWizardId() {
		return id;
	}

	@Override
	protected int getXmlPreferences() {
		return R.xml.w_expert_preferences;
	}

	@Override
	protected String getXmlPrefix() {
		return "expert";
	}
}
