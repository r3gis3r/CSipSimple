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

package com.csipsimple.service;

import com.csipsimple.pjsip.UAStateReceiver;
import com.csipsimple.utils.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class HeadsetButtonReceiver extends BroadcastReceiver {

	private static final String THIS_FILE = "HeadsetButtonReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(THIS_FILE, "onReceive");
		if(uaReceiver == null) {
			return;
		}
		//	abortBroadcast();
	    	//
			// Headset button has been pressed by user. Normally when 
			// the UI is active this event will never be generated instead
			// a headset button press will be handled as a regular key
			// press event.
			//
	        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
				KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				Log.d(THIS_FILE, "Key : "+event.getKeyCode());
				if (event != null && 
						event.getAction() == KeyEvent.ACTION_DOWN && 
						event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
					
		        	if (uaReceiver.handleHeadsetButton()) {
			        	//
						// After processing the event we will prevent other applications
						// from receiving the button press since we have handled it ourself
						// and do not want any media player to start playing for example.
						//
		        		/*
		        		 * TODO : enable this test if api > 5
		        		if (isOrderedBroadcast()) {
		        		*/
		        			abortBroadcast();
		        			/*
		        		}
		        		*/
		        	}
				}
			}

	}
	
	private static UAStateReceiver uaReceiver = null;
	public static void setService(UAStateReceiver aUAReceiver) {
		uaReceiver = aUAReceiver;
	}

}
