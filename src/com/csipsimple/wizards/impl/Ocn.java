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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;


public class Ocn extends AuthorizationImplementation {

	@Override
	protected String getDefaultName() {
		return "OCN";
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
        accountUsername.setTitle("8 digits user account");
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        hidePreference(null, SERVER);
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(USER_NAME)) {
            return "8 digits without 050";
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

    protected String getDomain() {
        return "ocn.ne.jp";
    }
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account =  super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_UDP;
		String fourDigits = accountUsername.getText().substring(0, 4);
		String regUri = "sip:voip-ca" + fourDigits + ".ocn.ne.jp";
        account.reg_uri = regUri;
        account.proxies = new String[] { regUri };
		return account;
	}
	

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        // Add stun server
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.DISABLE_RPORT, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);

    }
    
    @Override
    public boolean needRestart() {
        return true;
    }
}
