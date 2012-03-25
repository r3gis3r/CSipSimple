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
import android.text.InputType;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;


public class BelCentrale extends SimpleImplementation {

	ListPreference accountState;
	private static String STATE_KEY = "state";

	@Override
	protected String getDomain() {
		String prefix = "login";
		if(accountState != null && !TextUtils.isEmpty(accountState.getValue())){
			prefix = accountState.getValue();
		}
		return prefix+".belcentrale.nl";
	}
	
	@Override
	protected String getDefaultName() {
		return "Belcentrale";
	}
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);
		

		CharSequence[] states = new CharSequence[] {"login", "pbx2", "pbx3", "pbx4", 
				"pbx6", "pbx7", "pbx8", "pbx9", "pbx10", "pbx11", "pbx12", "pbx13", "pbx15"};

		boolean recycle = true;
		accountState = (ListPreference) findPreference(STATE_KEY);
		if(accountState == null) {
			accountState = new ListPreference(parent);
			accountState.setKey(STATE_KEY);
			recycle = false;
		}
		
        accountState.setEntries(states);
        accountState.setEntryValues(states);
        accountState.setDialogTitle(R.string.w_common_server);
        accountState.setTitle(R.string.w_common_server);
        accountState.setSummary(R.string.w_common_server_desc);
        accountState.setDefaultValue("login");
        
        if(!recycle) {
        	addPreference(accountState);
        }
        

        String domain = account.reg_uri;
        if( domain != null ) {
	        for(CharSequence state : states) {
	        	String currentComp = "sip:"+state+".belcentrale.nl";
	        	if( domain.startsWith(currentComp) ) {
	        		accountState.setValue((String) state);
	        		break;
	        	}
	        }
        }
        

		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	}
	

	public void updateDescriptions() {
		super.updateDescriptions();
		setStringFieldSummary(STATE_KEY);
		
	}
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		acc.reg_timeout = 60;
		acc.vm_nbr = "*95";
		return acc;
	}
	
	
}
