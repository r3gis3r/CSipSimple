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

public class Advancefone extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "sip.advancefone.com:5061";
	}
	
	@Override
	protected String getDefaultName() {
		return "Advancefone";
	}

    private final static String USUAL_PREFIX = "79";
    
    /**
     * {@inheritDoc}
     */
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);

        if(TextUtils.isEmpty(account.username)){
            accountUsername.setText(USUAL_PREFIX);
        }
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canSave() {
        boolean ok = super.canSave();
        ok &= checkField(accountUsername, accountUsername.getText().trim().equalsIgnoreCase(USUAL_PREFIX));
        return ok;
    }
	
}
