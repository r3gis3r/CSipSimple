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
package com.csipsimple.wizards.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;


public class DvcNg extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "sip.dvc-ng.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "DVC'NG";
	}

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_QOS, true);
		
		
	}
	
	@Override
	public List<Filter> getDefaultFilters(SipProfile acc) {
		
		ArrayList<Filter> filters = new ArrayList<Filter>();
		
		Filter f = new Filter();
		f.account = (int) acc.id;
		f.action = Filter.ACTION_REPLACE;
		f.matchPattern = "^"+Pattern.quote("+")+"(.*)$";
		f.replacePattern = "$1";
		f.matchType = Filter.MATCHER_STARTS;
		filters.add(f);
		
		return filters;
	}
	
}
