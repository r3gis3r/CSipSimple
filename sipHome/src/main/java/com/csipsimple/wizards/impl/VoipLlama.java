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

import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;

public class VoipLlama extends Advanced {


    protected String getDefaultName() {
        return "VOIPLLAMA";
    }
    
    protected String getDomain() {
        return "sip.voipllama.com";
    }
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUserName.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        accountCallerId.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		hidePreference(null, FIELD_TCP);
		hidePreference(null, FIELD_PROXY);
		hidePreference(null, FIELD_SERVER);
		hidePreference(null, FIELD_AUTH_ID);
		if(TextUtils.isEmpty(account.display_name)) {
		    accountDisplayName.setText(getDefaultName());
		}
	}

	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		
        account.acc_id = accountCallerId.getText().trim() + 
            " <sip:" + SipUri.encodeUser(accountUserName.getText().trim()) + "@" + getDomain() + ">";
		
		String regUri = "sip:" + getDomain();
		account.reg_uri = regUri;
		account.proxies = new String[]{regUri};
		account.transport = SipProfile.TRANSPORT_UDP;
		return account;
	}
	
	@Override
	public boolean canSave() {
        boolean isValid = true;
        
        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountCallerId, isEmpty(accountCallerId));
        isValid &= checkField(accountUserName, isEmpty(accountUserName));
        isValid &= checkField(accountPassword, isEmpty(accountPassword));

        return isValid;
	}
}
