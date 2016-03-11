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

import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class IiNet extends SimpleImplementation {
	
	ListPreference accountState;
	
	@Override
	public void fillLayout(final SipProfile account) {
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
        addPreference(accountState);
        
        String domain = account.reg_uri;
        if( domain != null ) {
	        for(CharSequence state : states) {
	        	String currentComp = "sip:sip."+state+".iinet.net.au";
	        	if( currentComp.equalsIgnoreCase(domain) ) {
	        		accountState.setValue(state.toString());
	        		break;
	        	}
	        }
        }
        
        accountUsername.setTitle(R.string.w_iinet_username);
		accountUsername.setDialogTitle(R.string.w_iinet_username);
		accountPassword.setTitle(R.string.w_iinet_password);
		accountPassword.setDialogTitle(R.string.w_iinet_password);
	}

	@Override
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		
		String regUri = "sip:sip."+accountState.getValue()+".iinet.net.au";
		
		account.reg_uri = regUri;
		account.proxies = new String[] { regUri } ;
		account.mwi_enabled = false;
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

	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
}
