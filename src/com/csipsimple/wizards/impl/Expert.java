/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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
	private EditTextPreference accountVm;
	private CheckBoxPreference tryCleanRegisters;
    private CheckBoxPreference useRfc5626;
    private EditTextPreference rfc5626_regId;
    private EditTextPreference rfc5626_instanceId;
    private ListPreference vidOutAutoTransmit;
    private ListPreference vidInAutoShow;
    private ListPreference rtpEnableQos;
    private EditTextPreference rtpPort;
    private EditTextPreference rtpPublicAddr;
    private EditTextPreference rtpBoundAddr;
    private EditTextPreference rtpQosDscp;
	
	private void bindFields() {
		accountDisplayName = (EditTextPreference) parent.findPreference(SipProfile.FIELD_DISPLAY_NAME);
		accountAccId = (EditTextPreference) parent.findPreference(SipProfile.FIELD_ACC_ID);
		accountRegUri = (EditTextPreference) parent.findPreference(SipProfile.FIELD_REG_URI);
		accountRealm = (EditTextPreference) parent.findPreference(SipProfile.FIELD_REALM);
		accountUserName = (EditTextPreference) parent.findPreference(SipProfile.FIELD_USERNAME);
		accountData = (EditTextPreference) parent.findPreference(SipProfile.FIELD_DATA);
		accountDataType = (ListPreference) parent.findPreference(SipProfile.FIELD_DATATYPE);
		accountScheme = (ListPreference) parent.findPreference(SipProfile.FIELD_SCHEME);
		accountTransport = (ListPreference) parent.findPreference(SipProfile.FIELD_TRANSPORT);
		accountUseSrtp = (ListPreference) parent.findPreference(SipProfile.FIELD_USE_SRTP);
		accountPublishEnabled = (CheckBoxPreference) parent.findPreference(SipProfile.FIELD_PUBLISH_ENABLED);
		accountRegTimeout = (EditTextPreference) parent.findPreference(SipProfile.FIELD_REG_TIMEOUT);
		accountRegDelayRefresh = (EditTextPreference) parent.findPreference("reg_delay_before_refresh");
		accountForceContact = (EditTextPreference) parent.findPreference(SipProfile.FIELD_FORCE_CONTACT);
		accountAllowContactRewrite = (CheckBoxPreference) parent.findPreference(SipProfile.FIELD_ALLOW_CONTACT_REWRITE);
		accountContactRewriteMethod = (ListPreference) parent.findPreference(SipProfile.FIELD_CONTACT_REWRITE_METHOD);
		accountProxy = (EditTextPreference) parent.findPreference(SipProfile.FIELD_PROXY);
		accountVm = (EditTextPreference) parent.findPreference(SipProfile.FIELD_VOICE_MAIL_NBR);
		tryCleanRegisters = (CheckBoxPreference) parent.findPreference(SipProfile.FIELD_TRY_CLEAN_REGISTERS);
		useRfc5626 = (CheckBoxPreference) parent.findPreference(SipProfile.FIELD_USE_RFC5626);
		rfc5626_instanceId = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RFC5626_INSTANCE_ID);
		rfc5626_regId = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RFC5626_REG_ID);
		vidInAutoShow = (ListPreference) parent.findPreference(SipProfile.FIELD_VID_IN_AUTO_SHOW);
		vidOutAutoTransmit = (ListPreference) parent.findPreference(SipProfile.FIELD_VID_OUT_AUTO_TRANSMIT);
		rtpEnableQos = (ListPreference) parent.findPreference(SipProfile.FIELD_RTP_ENABLE_QOS);
		rtpQosDscp = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RTP_QOS_DSCP);
		rtpPort = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RTP_PORT);
        rtpBoundAddr = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RTP_BOUND_ADDR);
        rtpPublicAddr = (EditTextPreference) parent.findPreference(SipProfile.FIELD_RTP_PUBLIC_ADDR);
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
				accountScheme.setValue(SipProfile.CRED_SCHEME_DIGEST);
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
        Log.d(THIS_FILE, "use srtp : " + account.use_srtp);
		accountUseSrtp.setValueIndex(account.use_srtp + 1);
		
		
		accountVm.setText(account.vm_nbr);
		tryCleanRegisters.setChecked(account.try_clean_registers != 0);
	}
	

	public void updateDescriptions() {
		setStringFieldSummary(SipProfile.FIELD_DISPLAY_NAME);
		setStringFieldSummary(SipProfile.FIELD_ACC_ID);
		setStringFieldSummary(SipProfile.FIELD_REG_URI);
		setStringFieldSummary(SipProfile.FIELD_REALM);
		setStringFieldSummary(SipProfile.FIELD_USERNAME);
		setStringFieldSummary(SipProfile.FIELD_PROXY);
		setPasswordFieldSummary(SipProfile.FIELD_DATA);
		setListFieldSummary(SipProfile.FIELD_DATATYPE);
        setStringFieldSummary(SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH);
        setListFieldSummary(SipProfile.FIELD_USE_SRTP);
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5469900404720631144L;

	{
		put(SipProfile.FIELD_DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(SipProfile.FIELD_ACC_ID, R.string.w_expert_acc_id_desc);
		put(SipProfile.FIELD_REG_URI, R.string.w_expert_reg_uri_desc);
		put(SipProfile.FIELD_REALM, R.string.w_expert_realm_desc);
		put(SipProfile.FIELD_USERNAME, R.string.w_expert_username_desc);
		put(SipProfile.FIELD_PROXY, R.string.w_expert_proxy_desc);
		put(SipProfile.FIELD_DATA, R.string.w_expert_data_desc);
        put(SipProfile.FIELD_DATATYPE, R.string.w_expert_datatype_desc);
        put(SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH, R.string.w_expert_reg_delay_before_refresh_desc);
        put(SipProfile.FIELD_USE_SRTP, R.string.use_srtp_desc);
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
	
	private static int getIntValue(ListPreference pref, int defaultValue) {
	    try {
            return Integer.parseInt(pref.getValue());
        }catch(NumberFormatException e) {
            Log.e(THIS_FILE, "List item is not a number");
        }
	    return defaultValue;
	}
    private static int getIntValue(EditTextPreference pref, int defaultValue) {
        try {
            return Integer.parseInt(pref.getText());
        }catch(NumberFormatException e) {
            Log.e(THIS_FILE, "List item is not a number");
        }
        return defaultValue;
    }

	public SipProfile buildAccount(SipProfile account) {
		account.display_name = accountDisplayName.getText();
		account.transport = getIntValue(accountTransport, SipProfile.TRANSPORT_UDP);
		account.acc_id = getText(accountAccId);
		account.reg_uri = getText(accountRegUri);
		account.use_srtp = getIntValue(accountUseSrtp, -1);

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
			account.scheme = SipProfile.CRED_SCHEME_DIGEST;
			account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		}

		account.publish_enabled = accountPublishEnabled.isChecked() ? 1 : 0;
		int regTo = getIntValue(accountRegTimeout, -1);
		if(regTo > 0) {
			account.reg_timeout = regTo;
		}
		int regDelay = getIntValue(accountRegDelayRefresh, -1);
		if(regDelay > 0) {
			account.reg_delay_before_refresh = regDelay;
		}
		
		account.contact_rewrite_method = getIntValue(accountContactRewriteMethod, 2);
		
		account.allow_contact_rewrite = accountAllowContactRewrite.isChecked();
		String forceContact = getText(accountForceContact);
		if(!TextUtils.isEmpty(forceContact)) {
			account.force_contact = forceContact;
		}else {
			account.force_contact = "";
		}
		
		if (!isEmpty(accountProxy)) {
			account.proxies = new String[] { accountProxy.getText() };
		} else {
			account.proxies = null;
		}
		
		String vmNbr = getText(accountVm);
		if(!TextUtils.isEmpty(vmNbr)) {
			account.vm_nbr = vmNbr;
		}else {
			account.vm_nbr = "";
		}
		
        account.try_clean_registers = (tryCleanRegisters.isChecked()) ? 1 : 0;
		
        account.use_rfc5626 = useRfc5626.isChecked();
        account.rfc5626_instance_id = rfc5626_instanceId.getText();
        account.rfc5626_reg_id = rfc5626_regId.getText();
        
        account.vid_in_auto_show = getIntValue(vidInAutoShow, -1);
        account.vid_out_auto_transmit = getIntValue(vidOutAutoTransmit, -1);
        
        account.rtp_port = getIntValue(rtpPort, -1);
        account.rtp_bound_addr = rtpBoundAddr.getText();
        account.rtp_public_addr = rtpPublicAddr.getText();
        account.rtp_enable_qos = getIntValue(rtpEnableQos, -1);
        account.rtp_qos_dscp = getIntValue(rtpQosDscp, -1);
        
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
