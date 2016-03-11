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


public class EasyBell extends SimpleImplementation {

    /**
     * {@inheritDoc}
     */
	@Override
	protected String getDomain() {
		return "msp.easybell.de";
	}

    /**
     * {@inheritDoc}
     */
	@Override
	protected String getDefaultName() {
		return "easybell";
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
	    acc.transport = SipProfile.TRANSPORT_TCP;
	    return acc;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
        prefs.setPreferenceStringValue(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_MOBILE,  "90");
        prefs.setPreferenceStringValue(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_WIFI,  "90");
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean needRestart() {
	    return true;
	}
}
