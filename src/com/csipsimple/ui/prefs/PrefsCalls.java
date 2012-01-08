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

import android.view.MenuItem;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;


public class PrefsCalls extends GenericPrefs {


	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_calls;
	}
	
	/*
	@Override
	protected void beforeBuildPrefs() {
		super.beforeBuildPrefs();
		
		ActionBar ab = getActionBar();
		if(ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}
	}
	*/
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int selId = item.getItemId();
		if(selId == Compatibility.getHomeMenuId()) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		/*
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		if(!pfw.isAdvancedUser()) {
			
		}
		*/
		
		if(CustomDistribution.forceNoMultipleCalls()) {
			hidePreference(null, SipConfigManager.SUPPORT_MULTIPLE_CALLS);
		}
	}
	
	
	@Override
	protected void updateDescriptions() {
		// TODO : update description here
	}


	
}
