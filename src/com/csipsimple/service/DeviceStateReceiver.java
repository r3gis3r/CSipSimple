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
import android.net.ConnectivityManager;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class DeviceStateReceiver extends BroadcastReceiver {

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
		//
		// ACTION_CONNECTIVITY_CHANGED
		// Connectivity change is used to detect changes in the overall
		// data network status as well as a switch between wifi and mobile
		// networks.
		//
		if (intentAction.equals(ACTION_DATA_STATE_CHANGED) ||
				intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
				intentAction.equals(Intent.ACTION_BOOT_COMPLETED)
		) {
			Log.d(THIS_FILE, ">>> Data device change detected" + intentAction);
			
			
			if (prefWrapper.isValidConnectionForIncoming() && !prefWrapper.hasBeenQuit()) {
				Log.d(THIS_FILE, "Try to start service if not already started");
				Intent sip_service_intent = new Intent(context, SipService.class);
				sip_service_intent.putExtra(SipService.EXTRA_DIRECT_CONNECT, false);
				context.startService(sip_service_intent);
			}
			Log.d(THIS_FILE, "<<< Data device change detected");
			
		}else if(intentAction.equals(SipManager.INTENT_SIP_ACCOUNT_ACTIVATE)) {
			long accId;
			accId = intent.getLongExtra(SipManager.EXTRA_ACCOUNT_ID, SipProfile.INVALID_ID);
			
			if(accId == SipProfile.INVALID_ID) {
				// allow remote side to send us integers.
				// previous call will warn, but that's fine, no worries
				accId = intent.getIntExtra(SipManager.EXTRA_ACCOUNT_ID, SipProfile.INVALID_ID);
			}
			
			if(accId != SipProfile.INVALID_ID) {
	    		DBAdapter database = new DBAdapter(context);
				database.open();
				boolean active = intent.getBooleanExtra(SipManager.EXTRA_ACTIVATE, true);
				boolean done = database.setAccountActive(accId, active);
				Log.d(THIS_FILE, "Set account active : " + active);
				database.close();
				if(done) {
					if (prefWrapper.isValidConnectionForIncoming()) {
						Intent sip_service_intent = new Intent(context, SipService.class);
						context.startService(sip_service_intent);
					}
				}
			}
		}
	}
	
}
