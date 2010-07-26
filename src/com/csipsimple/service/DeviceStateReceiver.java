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
import android.view.KeyEvent;
import android.os.IBinder;
import android.os.RemoteException;

import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class DeviceStateReceiver extends BroadcastReceiver {

	private static final String ACTION_CONNECTIVITY_CHANGED = "android.net.conn.CONNECTIVITY_CHANGE";
	private static final String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
	private static final String THIS_FILE = "Device State";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		PreferencesWrapper prefWrapper = new PreferencesWrapper(context);
		String intentAction = intent.getAction();
		
		//
		// ACTION_DATA_STATE_CHANGED
		// Data state change is used to detect changes in the mobile
		// network such as a switch of network type (GPRS, EDGE, 3G) 
		// which are not detected by the Connectivity changed broadcast.
		//
		if (intentAction.equals(ACTION_DATA_STATE_CHANGED)) {
			Log.d(THIS_FILE, ">>> Data state change detected");
			
			if (prefWrapper.isValidConnectionForIncoming()) {
				Log.d(THIS_FILE, "Data state indicates connectivity now available");
				Intent sip_service_intent = new Intent(context, SipService.class);
				context.startService(sip_service_intent);
			}
			Log.d(THIS_FILE, "<<< Data state change detected");
			
		}
		//
		// ACTION_CONNECTIVITY_CHANGED
		// Connectivity change is used to detect changes in the overall
		// data network status as well as a switch between wifi and mobile
		// networks.
		//
		else if (intentAction.equals(ACTION_CONNECTIVITY_CHANGED)) {
			Log.d(THIS_FILE, ">>> Connectivity change detected");
			
			if (prefWrapper.isValidConnectionForIncoming()) {
				Log.d(THIS_FILE, "Connectivity now available");
				Intent sip_service_intent = new Intent(context, SipService.class);
				context.startService(sip_service_intent);
			}
			Log.d(THIS_FILE, "<<< Connectivity change detected");
			
			/* Perhaps use the possible upcomming network information
			 * when performing the connectivity check instead of the 
			 * current information, but it does not seem to be necessary.

			// New connection info
			Bundle extras = intent.getExtras();
			NetworkInfo ni = (NetworkInfo) extras.get(ConnectivityManager.EXTRA_NETWORK_INFO);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			// Other case (ie update IP etc) are handled directly inside the
			// service if started
			if (PreferencesWrapper.isValidConnectionFor(ni, prefs, "out")) {
				Log.d(THIS_FILE, "Connectivity now available");
				Intent sip_service_intent = new Intent(context, SipService.class);
				context.startService(sip_service_intent);
			}*/
		}
    	//
		// Headset button has been pressed by user. Normally when 
		// the UI is active this event will never be generated instead
		// a headset button press will be handled as a regular key
		// press event.
		//
		// This event should only be handled for Android versions higher
		// then v2.2 (API-Level 8). For older versions the event is
		// handled locally in the UAStateReceiver class.
		//
		else if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			
			//
			// Specific handling for Android versions higher then v2.2
			// previous versions use a separate receiver in UAStateReceiver
			// class.
			//
			if (Compatibility.isCompatible(8)) {
				if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
					//
					// Retrieve reference to the sip service to trigger the Headset
					// action button pressed event.
					//
					try {
						Log.d(THIS_FILE, ">>> Media button state change detected");
						
						Intent sip_service_intent = new Intent(context, SipService.class);
						if (sip_service_intent != null) {
							IBinder arg = peekService(context, sip_service_intent);
							if (arg != null) {
								ISipService service = ISipService.Stub.asInterface(arg);
								if (service != null) {
									service.performHeadsetAction();
								}
							}
						}
						Log.d(THIS_FILE, "<<< Media button state change detected");
						
					} catch (RemoteException e) {
						Log.d(THIS_FILE, "Failed to perform Headset button action");
					}
				}
			}
		}
	}
}
