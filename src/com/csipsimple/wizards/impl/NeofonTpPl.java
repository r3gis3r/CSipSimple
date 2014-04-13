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

import android.preference.ListPreference;
import android.text.InputType;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;

public class NeofonTpPl extends SimpleImplementation {
	private static final String PROVIDER_EXTRA_LETTER_KEY = "extra_letter";
    private ListPreference extensionLetter;
	
	@Override
	protected String getDomain() {
		return "neofon.tp.pl";
	}
	
	@Override
	protected String getDefaultName() {
		return "Neofon.tp.pl";
	}
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);

        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);

        boolean recycle = true;
        extensionLetter = (ListPreference) findPreference(PROVIDER_EXTRA_LETTER_KEY);
        if(extensionLetter == null) {
            extensionLetter = new ListPreference(parent);
            extensionLetter.setKey(PROVIDER_EXTRA_LETTER_KEY);
            recycle = false;
        }
        
        CharSequence[] e = new CharSequence[] {"b", "c", "a"};
        
        extensionLetter.setEntries(e);
        extensionLetter.setEntryValues(e);
        extensionLetter.setDialogTitle("uzupelnij numerem tel. kierunkowym");
        extensionLetter.setTitle("uzupelnij numerem tel. kierunkowym");
        extensionLetter.setDefaultValue("b");
        
        if(!recycle) {
            addPreference(extensionLetter);
        }
        
        String username = account.username;
        if( !TextUtils.isEmpty(username)) {
            if(username.endsWith("a@"+getDomain())) {
                extensionLetter.setValue("a");
            }else if(username.endsWith("c@"+getDomain())) {
                extensionLetter.setValue("c");
            }else if(username.endsWith("b@"+getDomain())) {
                extensionLetter.setValue("b");
            }
        }
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#buildAccount(com.csipsimple.api.SipProfile)
	 */
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
        acc.username = getText(accountUsername).trim() + extensionLetter.getValue() + "@" + getDomain();
	    return acc;
	}

}
