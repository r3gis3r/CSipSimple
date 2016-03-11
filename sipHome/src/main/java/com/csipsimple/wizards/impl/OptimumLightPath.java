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

import android.text.TextUtils;

import com.csipsimple.api.SipProfile;

public class OptimumLightPath extends AlternateServerImplementation {
	
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        
        if(TextUtils.isEmpty(account.getSipDomain())) {
            accountServer.setText("registerh.optimumlightpathvoice.com");
        }
        
    }
    

	@Override
	protected String getDefaultName() {
		return "Optimum LightPath";
	}
	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.contact_rewrite_method = 1;
        account.try_clean_registers = 0;
		return account;
	}
	
}
