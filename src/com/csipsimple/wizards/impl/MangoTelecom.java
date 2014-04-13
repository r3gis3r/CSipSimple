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

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class MangoTelecom extends AlternateServerImplementation {
    
    static final String DEFAULT_DOMAIN = "mangosip.ru";
	
	@Override
	protected String getDomain() {
	    String thirdDomain = accountServer.getText();
	    if(!TextUtils.isEmpty(thirdDomain)) {
	        return thirdDomain.trim() + "." + DEFAULT_DOMAIN;
	    }
		return DEFAULT_DOMAIN;
	}
	
	@Override
	protected String getDefaultName() {
		return "Mango Telecom";
	}
	
    public boolean canSave() {
        boolean isValid = true;
        
        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountPassword, isEmpty(accountPassword));
        isValid &= checkField(accountUsername, isEmpty(accountUsername));

        return isValid;
    }
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		String sipDomain = account.getSipDomain();
		if(!TextUtils.isEmpty(sipDomain)) {
		    if(!sipDomain.equals(DEFAULT_DOMAIN)) {
		        accountServer.setText(sipDomain.replace("."+DEFAULT_DOMAIN, ""));
		    }else {
		        accountServer.setText("");
		    }
		}
        accountServer.setTitle(R.string.user_personal_domain);
        accountServer.setDialogTitle(R.string.user_personal_domain);
	}
	
    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(fieldName.equals(SERVER)) {
            return parent.getString(R.string.user_personal_domain);
        }
        return super.getDefaultFieldSummary(fieldName);
    }
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#buildAccount(com.csipsimple.api.SipProfile)
	 */
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
        acc.sip_stun_use = 1;
        acc.media_stun_use = 1;
	    return acc;
	}
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, Integer.toString(SipConfigManager.DTMF_MODE_RTP));
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.addStunServer("mangosip.ru:3478");
    }
	
}
