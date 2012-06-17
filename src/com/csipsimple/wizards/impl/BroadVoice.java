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

import android.preference.EditTextPreference;
import android.text.InputType;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;

public class BroadVoice extends SimpleImplementation {
	

	private static final String SUFFIX_KEY = "suffix";
	private EditTextPreference accountSuffix;

	@Override
	protected String getDomain() {
		return "sip.broadvoice.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "BroadVoice";
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_common_phone_number);
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		 
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
		// Allow to add suffix x11
		boolean recycle = true;
		accountSuffix = (EditTextPreference) findPreference(SUFFIX_KEY);
		if(accountSuffix == null) {
			accountSuffix = new EditTextPreference(parent);
			accountSuffix.setKey(SUFFIX_KEY);
			accountSuffix.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
			accountSuffix.setTitle("Suffix for account id");
			accountSuffix.setSummary("For multipresence usage (leave blank if not want)");
			recycle = false;
		}
		

        if(!recycle) {
            addPreference(accountSuffix);
        }
        
		String uName = account.getSipUserName();
		String[] uNames = uName.split("x");
		
		accountUsername.setText(uNames[0]);
		if(uNames.length > 1) {
			accountSuffix.setText(uNames[1]);
		}

	}

	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.proxies = null;
		account.reg_timeout = 3600;
		account.contact_rewrite_method = 1;
		
		String finalUsername = accountUsername.getText().trim();
		if(accountSuffix != null) {
			String suffix = accountSuffix.getText();
			if(!TextUtils.isEmpty(suffix)) {
				finalUsername += "x"+suffix.trim();
			}
		}
		
		account.acc_id = "<sip:" + SipUri.encodeUser(finalUsername) + "@"+getDomain()+">";
		
		return account;
	}
	
	@Override
	protected boolean canTcp() {
		return false;
	}
}
