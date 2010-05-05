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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public abstract class GenericPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	protected abstract int getXmlPreferences();
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		beforeBuildPrefs();
		addPreferencesFromResource(getXmlPreferences());
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		updateDescriptions();
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updateDescriptions();
	}

	protected abstract void updateDescriptions();
	protected void beforeBuildPrefs() {};
	
	//Utilities for update Descriptions
	
	protected String getDefaultFieldSummary(String field_name){
		String val = "";
		try {
			String keyid = R.string.class.getField(field_name+"_summary").get(null).toString();
			val = getString( Integer.parseInt(keyid) );
		} catch (SecurityException e) {
			//Nothing to do : desc is null
		} catch (NoSuchFieldException e) {
			//Nothing to do : desc is null
		} catch (IllegalArgumentException e) {
			//Nothing to do : desc is null
		} catch (IllegalAccessException e) {
			//Nothing to do : desc is null
		}
		
		return val;
	}
	
	protected void setStringFieldSummary(String field_name){
		PreferenceScreen pfs = getPreferenceScreen();
		SharedPreferences sp = pfs.getSharedPreferences();
		Preference pref = pfs.findPreference(field_name);
		
		String val = sp.getString(field_name, "");
		if(val.equals("")){
			val = getDefaultFieldSummary(field_name);
		}
		pref.setSummary(val);
		
	}
	
	protected void setPasswordFieldSummary(String field_name){
		PreferenceScreen pfs = getPreferenceScreen();
		SharedPreferences sp = pfs.getSharedPreferences();
		Preference pref = pfs.findPreference(field_name);
		
		String val = sp.getString(field_name, "");
		
		if(val.equals("")){
			val = getDefaultFieldSummary(field_name);
		}else{
			val = val.replaceAll(".", "*");
		}
		pref.setSummary(val);
	}

	
	


}
