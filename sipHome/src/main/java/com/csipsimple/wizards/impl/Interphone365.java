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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Interphone365 extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "voip.interphone365.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "INTERPHONE365";
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_UDP;
		account.vm_nbr = "2111";
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
}
