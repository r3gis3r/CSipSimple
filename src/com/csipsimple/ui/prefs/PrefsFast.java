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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.csipsimple.R;
import com.csipsimple.utils.PreferencesWrapper;

public class PrefsFast extends Activity implements OnClickListener {
	
	private CheckBox globIntegrate;
	private RadioButton globProfileAlways;
	private RadioButton globProfileWifi;
	private RadioButton globProfileNever;
	private CheckBox globTg;
	private PreferencesWrapper prefsWrapper;
	private SharedPreferences prefs;
	
	enum Profile {
		UNKOWN,
		ALWAYS,
		WIFI,
		NEVER
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fast_settings);
		
		prefsWrapper = new PreferencesWrapper(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Init checkboxes objects
		globIntegrate = (CheckBox) findViewById(R.id.glob_integrate);
		globProfileAlways = (RadioButton) findViewById(R.id.glob_profile_always);
		globProfileWifi = (RadioButton) findViewById(R.id.glob_profile_wifi);
		globProfileNever = (RadioButton) findViewById(R.id.glob_profile_never);
		globTg = (CheckBox) findViewById(R.id.glob_tg);
		
		globProfileAlways.setOnClickListener(this);
		globProfileNever.setOnClickListener(this);
		globProfileWifi.setOnClickListener(this);
		
		findViewById(R.id.row_glob_integrate).setOnClickListener(this);
		findViewById(R.id.row_glob_profile_always).setOnClickListener(this);
		findViewById(R.id.row_glob_profile_wifi).setOnClickListener(this);
		findViewById(R.id.row_glob_profile_never).setOnClickListener(this);
		findViewById(R.id.row_glob_tg).setOnClickListener(this);
		
		updateFromPrefs();
		
	}
	

	@Override
	public void onDestroy(){
		super.onDestroy();
		applyPrefs();
	}
	
	private void updateFromPrefs() {
		globIntegrate.setChecked(prefsWrapper.useIntegrateDialer());
		boolean tgIn = prefs.getBoolean("use_3g_in", false);
		boolean tgOut = prefs.getBoolean("use_3g_out", false);
		boolean wifiIn = prefs.getBoolean("use_wifi_in", true);
		boolean wifiOut = prefs.getBoolean("use_wifi_out", true);
		boolean useTg = (tgIn || tgOut);
		boolean lockWifi = prefs.getBoolean("lock_wifi", true);
		
		globTg.setChecked( useTg);
		
		Profile mode = Profile.UNKOWN;
		if( ( !useTg && wifiIn && wifiOut && lockWifi) ||
			(  useTg && wifiIn && wifiOut && tgIn && tgOut && lockWifi)) {
			mode = Profile.ALWAYS;
		} else if(  wifiIn && wifiOut ) {
			mode = Profile.WIFI;
		} else if (!wifiIn && !tgIn) {
			mode = Profile.NEVER;
		}
		
		setProfile(mode);
	}

	private void setProfile(Profile mode) {
		globProfileAlways.setChecked(mode == Profile.ALWAYS);
		globProfileWifi.setChecked(mode == Profile.WIFI);
		globProfileNever.setChecked(mode == Profile.NEVER);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.glob_profile_always || id == R.id.row_glob_profile_always) {
			setProfile(Profile.ALWAYS);
		
		}else if(id == R.id.glob_profile_wifi || id == R.id.row_glob_profile_wifi) {
			setProfile(Profile.WIFI);
		
		}else if(id == R.id.glob_profile_never || id == R.id.row_glob_profile_never) {
			setProfile(Profile.NEVER);
			return;
		}else if( id == R.id.row_glob_integrate ) {
			globIntegrate.toggle();
		}else if( id == R.id.row_glob_tg ) {
			globTg.toggle();
		}
	}
	
	private void applyPrefs() {
		boolean integrate = globIntegrate.isChecked();
		boolean useTg = globTg.isChecked();
		Profile mode = Profile.UNKOWN;
		if(globProfileAlways.isChecked()) {
			mode = Profile.ALWAYS;
		}else if (globProfileWifi.isChecked()) {
			mode = Profile.WIFI;
		}else if(globProfileNever.isChecked()) {
			mode = Profile.NEVER;
		}
		
		Editor edt = prefs.edit();
		edt.putBoolean("integrate_with_native_dialer", integrate);
		if(mode != Profile.UNKOWN) {
			edt.putBoolean("use_3g_in", (useTg && mode == Profile.ALWAYS));
			edt.putBoolean("use_3g_out", useTg);
			edt.putBoolean("use_wifi_in", mode != Profile.NEVER);
			edt.putBoolean("use_wifi_out", true);
			edt.putBoolean("lock_wifi", mode == Profile.ALWAYS);
		}
		edt.commit();
		
	}

}
