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

public class CongstarQSC extends SimpleImplementation {
    
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
        accountUsername.setDialogMessage("Beispiel: 022112345678");
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
    }
    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(USER_NAME)) {
            return "Rufnummer inkl. Vorwahl Beispiel: 022112345678";
        }
        return super.getDefaultFieldSummary(fieldName);
    }
    
    
    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        account.proxies = new String[] {"sip:farm2.tel2.congstar.qsc.de"};
        account.reg_uri = "sip:tel2.congstar.de";
        account.transport = SipProfile.TRANSPORT_UDP;
        String uname = Uri.encode(accountUsername.getText().trim());
        account.acc_id = uname + " <sip:" + uname + "@" + getDomain() + ">";
        return account;
    }

    @Override
    protected String getDomain() {
        return "congstar.de";
    }
    
    
}
