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

import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Sonetel extends SimpleImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "Sonetel";
	}

	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_sonetel_email);
		accountUsername.setDialogTitle(R.string.w_sonetel_email);
		accountUsername.setDialogMessage(R.string.w_sonetel_email_desc);
		if( ! TextUtils.isEmpty(account.username) && !TextUtils.isEmpty(account.getSipDomain()) ){
			accountUsername.setText(account.username+"@"+account.getSipDomain());
		}
		
	}
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_sonetel_email_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		String[] emailParts = getText(accountUsername).trim().split("@");
		if(emailParts.length == 2) {
			account.username = emailParts[0];
			account.acc_id = "<sip:"+ getText(accountUsername).trim() +">";
			
			//account.reg_uri = "sip:"+ emailParts[1];
			// Already done by super, just to be sure and let that modifiable for future if needed re-add there
			// Actually sounds that it also work and that's also probably cleaner :
			account.reg_uri = "sip:"+getDomain();
			account.proxies = new String[] { "sip:"+getDomain() } ;
		}
		
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		

		//Only G711a/u  on WB & NB
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"245");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"244");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
		
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"245");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
	}
	
	public boolean canSave() {
		boolean canSave = super.canSave();
		
		String[] emailParts = getText(accountUsername).split("@");		
		canSave &= checkField(accountUsername, (emailParts.length != 2));
		
		return canSave;
		
	}

	@Override
	public boolean needRestart() {
		return true;
	}


	@Override
	protected String getDomain() {
		return "sonetel.net";
	}
}
