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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Telphin extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "voice.telphin.com";
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	}


	@Override
	protected String getDefaultName() {
		return "Telphin";
	}
	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.proxies = new String[] {"sip:voice.telphin.com:5068"};
		account.reg_timeout = 60;
		account.sip_stun_use = 0;
		account.media_stun_use = 0;
		account.try_clean_registers = 0;
		account.publish_enabled = 0;
		account.mwi_enabled = false;
		account.transport = SipProfile.TRANSPORT_UDP;
		
		return account;
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.BaseImplementation#setDefaultParams(com.csipsimple.utils.PreferencesWrapper)
	 */
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
	    super.setDefaultParams(prefs);
        prefs.setPreferenceStringValue(SipConfigManager.UDP_TRANSPORT_PORT, "6000");
        prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, Integer.toString(SipConfigManager.DTMF_MODE_INBAND));
	}
	
}
