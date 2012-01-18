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

import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.CallHandler;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;

public class OutgoingCall extends BroadcastReceiver {

	private static final String THIS_FILE = "Outgoing RCV";
	private Context context;
	private PreferencesProviderWrapper prefsWrapper;
	
	public static String ignoreNext = "";
	

	@Override
	public void onReceive(Context aContext, Intent intent) {
		String action = intent.getAction();
		String number = getResultData();
		//String full_number = intent.getStringExtra("android.phone.extra.ORIGINAL_URI");

		if (number == null) {
			return;
		}
		
		if(PhoneNumberUtils.isEmergencyNumber(number)) {
			Log.d(THIS_FILE, "It's an emergency number ignore that");
			ignoreNext = "";
			setResultData(number);
			return;
		}
		
		
		context = aContext;
		prefsWrapper = new PreferencesProviderWrapper(context);
		
		//Log.d(THIS_FILE, "act=" + action + " num=" + number + " fnum=" + full_number + " ignx=" + ignoreNext);	
		
		if ( !prefsWrapper.getPreferenceBooleanValue(SipConfigManager.INTEGRATE_WITH_DIALER, true) || 
				ignoreNext.equalsIgnoreCase(number) || 
				action == null) {
			
			Log.d(THIS_FILE, "Our selector disabled, or Mobile chosen in our selector, send to tel");
			ignoreNext = "";
			setResultData(number);
			return;
		}
		
		//Compute remote apps that could receive the outgoing call itnent through our api
		Map<String, String> potentialHandlers = CallHandler.getAvailableCallHandlers(context);
        Log.d(THIS_FILE, "We have " + potentialHandlers.size() + " potential handlers");
		
		// If this is an outgoing call with a valid number
		if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) ) {
			// If sip is there or there is at least 2 call handlers (if only one we assume that's the embed gsm one !
			if(prefsWrapper.isValidConnectionForOutgoing() || potentialHandlers.size() > 1) {
				// Just to be sure of what is incoming : sanitize phone number (in case of it was not properly done by dialer
				// Or by a third party app
				number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
	            number = PhoneNumberUtils.stripSeparators(number);
				
				// Launch activity to choose what to do with this call
				Intent outgoingCallChooserIntent = new Intent(Intent.ACTION_CALL);
				// Add csipsimple protocol :)
				outgoingCallChooserIntent.setData(Uri.fromParts("csip", number, null));
				outgoingCallChooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Log.d(THIS_FILE, "Start outgoing call chooser for CSipSimple");
				context.startActivity(outgoingCallChooserIntent);
				// We will treat this by ourselves
				setResultData(null);
				return;
			}
		}
		
		
		
		Log.d(THIS_FILE, "Can't use SIP, pass number along");
		// Pass the call to pstn handle
		setResultData(number);
		return;
	}


}
