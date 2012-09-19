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

import java.util.regex.Pattern;

public class OSTN extends AlternateServerImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "OSTN";
	}
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		String domain = getDomain();
		String[] domainPart = domain.split(":");
		boolean shouldAddPort = true;
		if(domainPart.length > 1) {
		    // If latest part is digit we should not add port
		    shouldAddPort = !Pattern.matches("^[0-9]+$", domainPart[1]);
		}
		if(shouldAddPort) {
		    domain = domain + ":5061";
		}
		account.proxies = new String[] { "sips:" + domain };
		account.transport = SipProfile.TRANSPORT_TLS;
		account.use_zrtp = 1;
    account.vm_nbr = "*98";
		return account;
	}
	
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
            super.setDefaultParams(prefs);
            prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
    }
   

    @Override
    public boolean needRestart() {
            return true;
    }
}
