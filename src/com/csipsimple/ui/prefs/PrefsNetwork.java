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

import android.telephony.TelephonyManager;

import com.csipsimple.R;


public class PrefsNetwork extends GenericPrefs {
	
	@Override
	protected int getXmlPreferences() {
		TelephonyManager TM = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
		if (TM.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
			return R.xml.prefs_network_cdma;
		} else {
			return R.xml.prefs_network;
		}
	}

	@Override
	protected void updateDescriptions() {
		// TODO Auto-generated method stub
		
	}
	
}
