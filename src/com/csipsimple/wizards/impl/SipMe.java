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
import com.csipsimple.api.SipProfile;

public class SipMe extends SimpleImplementation {
	
    @Override
    protected String getDomain() {
        return "sipme.me";
    }
    
	
	@Override
	protected String getDefaultName() {
		return "sipme";
	}
	
	private final static String USUAL_PREFIX = "078555"; 
	
    @Override
    public void fillLayout(SipProfile account) {
        super.fillLayout(account);
        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
        accountUsername.setDialogMessage(R.string.w_sipme_phone_number_desc);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
        if(TextUtils.isEmpty(account.username)){
            accountUsername.setText(USUAL_PREFIX);
        }
    }
    
    @Override
    public boolean canSave() {
    
        boolean ok = super.canSave();
        ok &= checkField(accountUsername, !accountUsername.getText().trim().equalsIgnoreCase(USUAL_PREFIX));
        return ok;
    }

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(USER_NAME)) {
            return parent.getString(R.string.w_sipme_phone_number_desc);
        }
        return super.getDefaultFieldSummary(fieldName);
    }
    
    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        acc.vm_nbr = "*900";
        return acc;
    }
}
