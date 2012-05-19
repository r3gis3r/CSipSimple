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

import android.text.InputType;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.PreferencesWrapper;



public class TOnline extends AuthorizationImplementation {
	

	@Override
	protected String getDomain() {
		return "tel.t-online.de";
	}
	
	@Override
	protected String getDefaultName() {
		return "t-online";
	}
	
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);
	    
	    if(!TextUtils.isEmpty(account.username)) {
	        String[] parts = account.username.split("@");
	        accountAuthorization.setText(parts[0]);
	    }
	    
	    accountAuthorization.setTitle("Zugangsnummer");
	    accountAuthorization.setDialogTitle("Zugangsnummer");
	    accountAuthorization.setDialogMessage("The id which is used for online access");
	    accountAuthorization.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	    
	    accountUsername.setTitle("Phone Number");
        accountUsername.setDialogTitle("Phone Number");
        accountUsername.setDialogMessage("Your phone number provieded by Telekom");
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	    
        accountPassword.setTitle("Zugangspassword");
        accountPassword.setDialogTitle("Zugangspassword");
        accountPassword.setDialogMessage("The password which is used for online access");

        hidePreference(null, SERVER);
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
	    if(AUTH_NAME.equalsIgnoreCase(fieldName)) {
	        return "The id which is used for online access";
	    }else if(USER_NAME.equalsIgnoreCase(fieldName)) {
	        return "Your phone number provieded by Telekom";
	    }else if(PASSWORD.equalsIgnoreCase(fieldName)) {
	        return "The password which is used for online access";
	    }
	    return super.getDefaultFieldSummary(fieldName);
	}
	

    public boolean canSave() {
        boolean isValid = true;
        
        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountUsername, isEmpty(accountUsername));
        isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
        isValid &= checkField(accountPassword, isEmpty(accountPassword));
        
        return isValid;
    }

	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		String domain = "t-online.de";
        account.acc_id = "<sip:" + SipUri.encodeUser(accountUsername.getText().trim()) + "@"
                + domain + ">";
		
		acc.username = getText(accountAuthorization).trim() + "@" + domain;
		
		return acc;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.addStunServer("stun.t-online.de");
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
}
