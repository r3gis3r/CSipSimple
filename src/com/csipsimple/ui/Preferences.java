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
package com.csipsimple.ui;

import com.csipsimple.service.SipService;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import com.csipsimple.R;
import com.csipsimple.service.ISipService;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	
	ServiceConnection restart_srv_conn;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Use our custom wizard view
		//setContentView(R.layout.custom_prefs);
		
		addPreferencesFromResource(R.xml.preferences);
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
		//Bind buttons to their actions
		/*
		Button bt = (Button) findViewById(R.id.save_bt);
		bt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		*/
		
		fillSummaries();
	}

	/*
	private String getDefaultFieldSummary(String field_name){
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
	private void setStringFieldSummary(String field_name){
		PreferenceScreen pfs = getPreferenceScreen();
		SharedPreferences sp = pfs.getSharedPreferences();
		Preference pref = pfs.findPreference(field_name);
		
		String val = sp.getString(field_name, "");
		if(val.equals("")){
			val = getDefaultFieldSummary(field_name);
		}
		pref.setSummary(val);
		
	}*/
	
	
	private void fillSummaries() {
		//Nothing to do yet
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		
		restart_srv_conn = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				ISipService sipservice = ISipService.Stub.asInterface(service);
				try {
					sipservice.sipStop();
					sipservice.sipStart();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				unbindService(restart_srv_conn);
			}
		};
		Intent serviceIntent =  new Intent(this, SipService.class);
		bindService(serviceIntent, restart_srv_conn, 0);
		startService(serviceIntent);
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		fillSummaries();
		
	}
	
	
}
