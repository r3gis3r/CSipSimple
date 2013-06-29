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


public class Pbxes extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "pbxes.org";
	}
	
	@Override
	protected String getDefaultName() {
		return "Pbxes.org";
	}

	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		acc.vm_nbr = "*97";
		return acc;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
	    super.setDefaultParams(prefs);
	    // We need to change T1 value because pbxes.org drop registrations when retransmition are made by SIP client
	    prefs.setPreferenceStringValue(SipConfigManager.TSX_T1_TIMEOUT, "1000");
	}
	
	@Override
	public boolean needRestart() {
	    return true;
	}
	
	@Override
	protected boolean canTcp() {
		return false; // Cause there is something really wrong on the pbxes.org server
	}
}
