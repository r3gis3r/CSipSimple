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
	
	
	private EditTextPreference mAccountDisplayName;
	private EditTextPreference mAccountAccId;
	private EditTextPreference mAccountRegUri;
	private EditTextPreference mAccountUserName;
	private EditTextPreference mAccountData;
	private ListPreference mAccountDataType;
	private EditTextPreference mAccountRealm;
	private ListPreference mAccountScheme;
	private CheckBoxPreference mAccountPublishEnabled;
	private EditTextPreference mAccountRegTimeout;
	private EditTextPreference mAccountForceContact;
	private EditTextPreference mAccountProxy;
	
	
	
	
	protected void fillLayout(){
		mAccountDisplayName = (EditTextPreference) findPreference("display_name");
		mAccountAccId = (EditTextPreference) findPreference("acc_id");
		mAccountRegUri = (EditTextPreference) findPreference("reg_uri");
		mAccountRealm = (EditTextPreference) findPreference("realm");
		mAccountUserName = (EditTextPreference) findPreference("username");
		mAccountData = (EditTextPreference) findPreference("data");
		mAccountDataType = (ListPreference) findPreference("data_type");
		mAccountScheme = (ListPreference) findPreference("scheme");
		mAccountPublishEnabled = (CheckBoxPreference) findPreference("publish_enabled");
		mAccountRegTimeout = (EditTextPreference) findPreference("reg_timeout");
		mAccountForceContact = (EditTextPreference) findPreference("force_contact");
		mAccountProxy = (EditTextPreference) findPreference("proxy");
		
		
		pjsip_cred_info ci = mAccount.cfg.getCred_info();
		
		mAccountDisplayName.setText(mAccount.display_name);
		mAccountAccId.setText(mAccount.cfg.getId().getPtr());
		mAccountRegUri.setText(mAccount.cfg.getReg_uri().getPtr());
		mAccountRealm.setText(ci.getRealm().getPtr());
		mAccountUserName.setText(ci.getUsername().getPtr());
		mAccountData.setText(ci.getData().getPtr());
		
		{
			String scheme = ci.getScheme().getPtr();
			if(scheme != null && !scheme.equals("")){
				mAccountScheme.setValue(ci.getScheme().getPtr());
			}else{
				mAccountScheme.setValue("Digest");
			}
		}
		{
			int ctype=ci.getData_type();
			if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue()){
				mAccountDataType.setValueIndex(0);
			}else if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_DIGEST.swigValue()){
				mAccountDataType.setValueIndex(1);
			}else if(ctype == pjsip_cred_data_type.PJSIP_CRED_DATA_EXT_AKA.swigValue()){
				mAccountDataType.setValueIndex(2);
			}else{
				mAccountDataType.setValueIndex(0);
			}
		}
		
		mAccountPublishEnabled.setChecked((mAccount.cfg.getPublish_enabled() == 1));
		if(mAccount.cfg.getReg_timeout() > 0){
			mAccountRegTimeout.setText(""+mAccount.cfg.getReg_timeout());
		}else{
			mAccountRegTimeout.setText("");
		}
		mAccountForceContact.setText(mAccount.cfg.getForce_contact().getPtr());
		mAccountProxy.setText(mAccount.cfg.getProxy().getPtr());
	}
	
	protected void updateDescriptions(){
		setStringFieldSummary("display_name");
		setStringFieldSummary("acc_id");
		setStringFieldSummary("reg_uri");
		setStringFieldSummary("realm");
		setStringFieldSummary("username");
		setPasswordFieldSummary("data");
	}
	
	protected boolean canSave(){
		if(isEmpty(mAccountDisplayName) ||
				isEmpty(mAccountAccId) || 
				isEmpty(mAccountRegUri)
		){
			return false;
		}
		
		return true;
	}
	

	protected void buildAccount(){
		Log.d(TAG, "begin of build ....");
		mAccount.display_name = mAccountDisplayName.getText();
		mAccount.cfg.setId(getPjText(mAccountAccId));
		mAccount.cfg.setReg_uri(getPjText(mAccountRegUri));
		
		pjsip_cred_info ci = mAccount.cfg.getCred_info();
		
		if( !isEmpty(mAccountUserName) ){
			mAccount.cfg.setCred_count(1);
			ci.setRealm(getPjText(mAccountRealm));
			ci.setUsername(getPjText(mAccountUserName));
			ci.setData(getPjText(mAccountData));
			ci.setScheme(pjsua.pj_str_copy(mAccountScheme.getValue()));
			// FIXME this is not the good value !
			ci.setData_type(pjsip_cred_data_type.PJSIP_CRED_DATA_PLAIN_PASSWD.swigValue());
			
		}else{
			mAccount.cfg.setCred_count(0);
		}
		
		
		mAccount.cfg.setPublish_enabled((mAccountPublishEnabled.isChecked())?1:0);
		try{
			mAccount.cfg.setReg_timeout(Integer.parseInt(mAccountRegTimeout.getText()));
		}catch(NumberFormatException e){
			mAccount.cfg.setReg_timeout(0);
		}
		/*
		mAccount.cfg.setForce_contact(getPjText(mAccountForceContact));
		
		if( !isEmpty(mAccountProxy) ){
			mAccount.cfg.setProxy_cnt(1);
			mAccount.cfg.setProxy(getPjText(mAccountProxy));
		}else{
			mAccount.cfg.setProxy_cnt(0);
		}
		*/
		
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
