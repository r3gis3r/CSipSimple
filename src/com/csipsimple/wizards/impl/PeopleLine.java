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


public class PeopleLine extends AlternateServerImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "Peopleline.net";
	}

	private final static String USUAL_SERVER_SUFFIX = ".ucantalk.net";
    //Customization
	
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);
        
        if(TextUtils.isEmpty(account.getSipDomain())) {
            accountServer.setText(USUAL_SERVER_SUFFIX);
        }
	}
	
    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        acc.contact_rewrite_method = 1;
        return acc;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canSave() {
        boolean ok = super.canSave();
        ok &= checkField(accountServer, accountServer.getText().trim().equalsIgnoreCase(USUAL_SERVER_SUFFIX));
        return ok;
    }
    
}
