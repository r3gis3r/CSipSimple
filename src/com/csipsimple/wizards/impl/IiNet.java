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

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsua;

import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.utils.PreferencesWrapper;

public class IiNet extends SimpleImplementation {
	
	ListPreference accountState;
	
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		
		CharSequence[] states = new CharSequence[] {"act", "nsw", "nt", "qld", "sa", "tas", "vic", "wa"};
		
        accountState = new ListPreference(parent);
        accountState.setEntries(states);
        accountState.setEntryValues(states);
        accountState.setKey("state");
        accountState.setDialogTitle(R.string.w_iinet_state);
        accountState.setTitle(R.string.w_iinet_state);
        accountState.setSummary(R.string.w_iinet_state_desc);
        accountState.setDefaultValue("act");
        parent.getPreferenceScreen().addPreference(accountState);
        
        accountUsername.setTitle(R.string.w_iinet_username);
		accountUsername.setDialogTitle(R.string.w_iinet_username);
		accountPassword.setTitle(R.string.w_iinet_password);
		accountPassword.setDialogTitle(R.string.w_iinet_password);
	}

	@Override
	public Account buildAccount(Account account) {
		account = super.buildAccount(account);
		
		pj_str_t regUri = pjsua.pj_str_copy("sip:sip."+accountState.getValue()+".iinet.net.au");
		account.cfg.setReg_uri(regUri);
		account.cfg.setProxy_cnt(1);
		pj_str_t[] proxies = account.cfg.getProxy();
		proxies[0] = regUri;
		account.cfg.setProxy(proxies);
		// Enable dns srv
		PreferencesWrapper prefs = new PreferencesWrapper(parent);
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_DNS_SRV, true);
		
		return account;
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString( R.string.w_iinet_username_desc );
		}else if(fieldName.equals(PASSWORD)) {
			return parent.getString( R.string.w_iinet_password_desc );
		}
		return super.getDefaultFieldSummary(fieldName);
	}

	@Override
	protected String getDomain() {
		return "iinetphone.iinet.net.au";
	}

	@Override
	protected String getDefaultName() {
		return "iinet";
	}

}
