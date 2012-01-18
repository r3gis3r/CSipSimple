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
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;

public class EuroTelefon extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "sip.eurotelefon.eu";
	}
	
	
	@Override
	protected String getDefaultName() {
		return "EuroTELEFON";
	}
	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.reg_timeout = 90;
		account.vm_nbr = "*197";
		account.try_clean_registers = 1;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// NO restart needed for this setting
		prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, PreferencesProviderWrapper.DTMF_MODE_RTP);
	}
	
}
