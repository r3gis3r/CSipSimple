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

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class MTel extends AuthorizationImplementation {
	
	@Override
	protected String getDomain() {
		return "mtel.ba";
	}
	
	@Override
	protected String getDefaultName() {
		return "m:tel";
	}

	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle("IMPU");
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		
		accountAuthorization.setTitle("IMPI");
		accountAuthorization.setDialogTitle(R.string.w_authorization_auth_name);
		
		hidePreference(null, SERVER);
	}
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_common_phone_number_desc);
		}else if(fieldName.equals(AUTH_NAME)) {
            return parent.getString(R.string.w_authorization_auth_name);
        }
		return super.getDefaultFieldSummary(fieldName);
	}
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.proxies = new String[] {"sip:89.111.231.49"};
		// Manage IMPU
		String user = getText(accountUsername).trim();
		if(!user.contains("@")) {
		    user =  user + "@" + getDomain();
		}
        account.acc_id = "<sip:" + user + ">";
		account.use_rfc5626 = true;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.AuthorizationImplementation#canSave()
	 */
	@Override
	public boolean canSave() {
        boolean isValid = true;
        
        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountUsername, isEmpty(accountUsername));
        isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
        isValid &= checkField(accountPassword, isEmpty(accountPassword));

        return isValid;
	}
}
