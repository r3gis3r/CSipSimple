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
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Freespeech extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "freespeech.ie";
	}
	

    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);

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
	protected String getDefaultName() {
		return "Freespeech.ie";
	}
	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		// Contact rewrite not needed for them.
		// Besides stun will be enabled.
		account.contact_rewrite_method = 1;
		account.allow_contact_rewrite = false;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

        // Add stun server
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        

        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"240");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"239");
        
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"239");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"240");
	}
	
	
	@Override
	public boolean needRestart() {
	    return true;
	}
}
