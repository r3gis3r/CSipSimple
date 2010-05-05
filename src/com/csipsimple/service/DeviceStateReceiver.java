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
package com.csipsimple.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.csipsimple.utils.Log;

public class DeviceStateReceiver extends BroadcastReceiver {

	
	private static final String THIS_FILE = "Device State";
	private SharedPreferences prefs;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(THIS_FILE, ">>> Connectivity has changed");
		// New connection info :
		Bundle extras = intent.getExtras();
		NetworkInfo ni =  (NetworkInfo) extras.get(ConnectivityManager.EXTRA_NETWORK_INFO);
		
		//Preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		//Other case (ie update IP etc) are handled directly inside the service if started
		if( isValidConnectionForOutgoing(ni) ){
			Intent sip_service_intent = new Intent(context, SipService.class);
			context.startService(sip_service_intent);
		}
		Log.d(THIS_FILE, "<<< Connectivity has changed");
	}
	
	// Say whether the current connection is valid to register sip
	private boolean isValidConnectionForOutgoing(NetworkInfo ni){
		if(ni.getState() == NetworkInfo.State.CONNECTED ){
			if(ni.getType() == ConnectivityManager.TYPE_MOBILE){
				return prefs.getBoolean("use_3g_in", false);
			}
			if(ni.getType() == ConnectivityManager.TYPE_WIFI){
				return prefs.getBoolean("use_wifi_in", true);
			}
		}
		return false;
	}
	
}
