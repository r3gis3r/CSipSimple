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

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;

public class Fritzbox extends AlternateServerImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "Fritz!Box";
	}

	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_fritz_extension);
		accountUsername.setDialogTitle(R.string.w_fritz_extension);
		accountUsername.setDialogMessage(R.string.w_fritz_extension_advise);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
		accountServer.setTitle(R.string.w_fritz_proxy);
		accountServer.setDialogTitle(R.string.w_fritz_proxy);
		
		if(account != null && account.proxies != null && account.proxies.length > 0) {
		    accountServer.setText(account.proxies[0].replace("sip:", ""));
		}else {
		    accountServer.setText("fritz.box");
		}
		
	}
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_fritz_extension_desc);
		}else if(fieldName.equals(SERVER)) {
		    return parent.getString(R.string.w_fritz_proxy_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		//Ensure registration timeout value
		account.reg_uri = "sip:fritz.box";
        account.acc_id = "<sip:"
                + SipUri.encodeUser(accountUsername.getText().trim()) + "@fritz.box>";
		account.contact_rewrite_method = 1;
        account.try_clean_registers = 0;
		account.allow_contact_rewrite = false;
		return account;
	}
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#canTcp()
	 */
	@Override
	protected boolean canTcp() {
	    return true;
	}
	
}
