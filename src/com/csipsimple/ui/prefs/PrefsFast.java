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

package com.csipsimple.ui.prefs;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.actionbarsherlock.app.SherlockActivity;
import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.PreferencesWrapper;

public class PrefsFast extends SherlockActivity implements OnClickListener {
	
	private CheckBox globIntegrate;
	private RadioButton globProfileAlways;
	private RadioButton globProfileWifi;
	private RadioButton globProfileNever;
	private CheckBox globGsm;
	
	
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
		
		
		//Init checkboxes objects
		globIntegrate = (CheckBox) findViewById(R.id.glob_integrate);
		globProfileAlways = (RadioButton) findViewById(R.id.glob_profile_always);
		globProfileWifi = (RadioButton) findViewById(R.id.glob_profile_wifi);
		globProfileNever = (RadioButton) findViewById(R.id.glob_profile_never);
		globGsm = (CheckBox) findViewById(R.id.glob_tg);
		
		Button saveBtn = (Button) findViewById(R.id.save_bt);
		
		saveBtn.setOnClickListener(this);
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
		//applyPrefs();
	}
	
	private void updateFromPrefs() {
		globIntegrate.setChecked(SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_WITH_DIALER));
		boolean tgIn = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_3G_IN, false);
		boolean tgOut = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_3G_OUT, false);
		boolean gprsIn = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_IN, false);
		boolean gprsOut = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_OUT, false);
		boolean edgeIn = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_IN, false);
		boolean edgeOut = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_OUT, false);
		boolean wifiIn = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_IN, true);
		boolean wifiOut = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_OUT, true);
		
		boolean useGsmIn = (tgIn || gprsIn || edgeIn);
		boolean useGsmOut = (tgOut || gprsOut || edgeOut);
		
		boolean useGsm = useGsmIn || useGsmOut ;
		boolean lockWifi = SipConfigManager.getPreferenceBooleanValue(this, SipConfigManager.LOCK_WIFI, true);
		
		globGsm.setChecked( useGsm );
		
		Profile mode = Profile.UNKOWN;
		
		if( ( !useGsm && wifiIn && wifiOut && lockWifi) ||
			(  useGsm && wifiIn && wifiOut && tgIn && tgOut && gprsIn && gprsOut && edgeIn && edgeOut )) {
			mode = Profile.ALWAYS;
		} else if (wifiIn && wifiOut ) {
			mode = Profile.WIFI;
		} else if (!wifiIn && !useGsmIn) {
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
			globGsm.toggle();
		}else if (id == R.id.save_bt) {
			if(!SipConfigManager.getPreferenceBooleanValue(this, PreferencesWrapper.HAS_ALREADY_SETUP, false) ) {
			    SipConfigManager.setPreferenceBooleanValue(this, PreferencesWrapper.HAS_ALREADY_SETUP, true);
			}
			applyPrefs();
			finish();
		}
	}
	
	private void applyPrefs() {
		boolean integrate = globIntegrate.isChecked();
		boolean useGsm = globGsm.isChecked();
		Profile mode = Profile.UNKOWN;
		if(globProfileAlways.isChecked()) {
			mode = Profile.ALWAYS;
		}else if (globProfileWifi.isChecked()) {
			mode = Profile.WIFI;
		}else if(globProfileNever.isChecked()) {
			mode = Profile.NEVER;
		}
		
		// About integration
		SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_WITH_DIALER, integrate);
		SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_WITH_CALLLOGS, integrate);
		
		// About out/in mode
		if(mode != Profile.UNKOWN) {
			
		    SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_3G_IN, (useGsm && mode == Profile.ALWAYS));
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_3G_OUT, useGsm);
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_IN, (useGsm && mode == Profile.ALWAYS));
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_OUT, useGsm);
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_IN, (useGsm && mode == Profile.ALWAYS));
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_OUT, useGsm);
			
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_IN, mode != Profile.NEVER);
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_OUT, true);
			
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_OTHER_IN, mode != Profile.NEVER);
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_OTHER_OUT, true);
			
			SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.LOCK_WIFI, (mode == Profile.ALWAYS) && !useGsm);
		}
		
	}

}
