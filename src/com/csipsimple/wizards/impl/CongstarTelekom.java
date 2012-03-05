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

import android.net.Uri;
import android.text.InputType;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;


public class CongstarTelekom extends AuthorizationImplementation {

	@Override
	protected String getDefaultName() {
		return "Congstar";
	}
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_common_phone_number);
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		accountUsername.setDialogMessage("030xxxx");
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
		
		accountAuthorization.setTitle(R.string.w_common_username);
		accountAuthorization.setDialogMessage("cVertragsnummer");

		hidePreference(null, SERVER);
	}
	

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return "030xxxx";
		}else if(fieldName.equals(AUTH_NAME)) {
		    return "cVertragsnummer";
		}else if(fieldName.equals(PASSWORD)) {
		    return "DSL-Vertragskennwort";
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	
	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));
		isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));
	//	isValid &= checkField(accountServer, isEmpty(accountServer));

		return isValid;
	}
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account =  super.buildAccount(account);
		account.proxies = null;
		account.transport = SipProfile.TRANSPORT_UDP;
		String uname = Uri.encode(accountUsername.getText().trim());
        account.acc_id = uname + " <sip:" + uname + "@" + getDomain() + ">";
		return account;
	}

	protected String getDomain() {
		return "tel.congstar.de";
	}
	
	
	
}
