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

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.PreferencesWrapper;


public class LiberTalk extends SimpleImplementation {

	@Override
	protected String getDomain() {
		return "ims.mnc010.mcc208.3gppnetwork.org";
	}
	@Override
	protected String getDefaultName() {
		return "SFR LiberTalk";
	}
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
	    String phoneNumber = SipUri.encodeUser(accountUsername.getText().trim());
        acc.acc_id = phoneNumber + " <sip:+3399"+ phoneNumber + "@"+getDomain()+">";
        acc.reg_uri = "sip:ims.mnc010.mcc208.3gppnetwork.org";
        acc.username = "NDI"+phoneNumber+".LIBERTALK@sfr.fr";
	    
        acc.reg_timeout = 3600;
	    acc.proxies = new String[] {"sip:internet.p-cscf.sfr.net:5064"};
	    acc.vm_nbr = "147";
	    return acc;
	}

    //Customization
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        
        if(!TextUtils.isEmpty(account.username)) {
            String phoneNumber = account.username.replaceFirst("^NDI", "");
            phoneNumber = phoneNumber.replaceFirst("\\.LIBERTALK@sfr\\.fr$", "");
            accountUsername.setText(phoneNumber);
        }
        
        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
    }
    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(USER_NAME)) {
            return parent.getString(R.string.w_common_phone_number_desc);
        }
        return super.getDefaultFieldSummary(fieldName);
    }
    

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
    }
    
    @Override
    public boolean needRestart() {
        return true;
    }
}
