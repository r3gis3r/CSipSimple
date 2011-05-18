/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.ui.prefs;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.PreferencesWrapper;

public class PrefsSecurity extends GenericPrefs {

	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_security;
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		boolean canUseTLS = pfw.getLibCapability(PreferencesWrapper.LIB_CAP_TLS);
		if(!canUseTLS) {
			hidePreference(null, "tls");
			hidePreference("secure_media", SipConfigManager.USE_ZRTP);
		}
		
	}

	@Override
	protected void updateDescriptions() {
		// TODO Auto-generated method stub

	}

}
