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

import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.csipsimple.ui.OutgoingCallChooser;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class OutgoingCall extends BroadcastReceiver {

	private static final String THIS_FILE = "Outgoing RCV";
	private Context context;
	private PreferencesWrapper prefsWrapper;
	

	@Override
	public void onReceive(Context aContext, Intent intent) {
		context = aContext;
		prefsWrapper = new PreferencesWrapper(context);
		String action = intent.getAction();
		String number = getResultData();
		String full_number = intent.getStringExtra("android.phone.extra.ORIGINAL_URI");

		Log.d(THIS_FILE, "We are trying to call " + full_number);
		if (Pattern.matches("^.*#PSTN$", full_number)) {
			Log.d(THIS_FILE, "we will force it ");
			setResultData(number);
			return;
		}

		// If this is an outgoing call with a valid number
		if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL) && number != null) {
			Log.d(THIS_FILE, "This is a work for super outgoing call handler....");
			if (isCallableNumber(number) && prefsWrapper.isValidConnectionForOutgoing()) {
				// Launch activity to choose what to do with this call
				Intent outgoingCallChooserIntent = new Intent(context, OutgoingCallChooser.class);
				outgoingCallChooserIntent.putExtra("number", number);
				outgoingCallChooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				context.startActivity(outgoingCallChooserIntent);
				// We will treat this by ourselves
				setResultData(null);
				return;
			} else {
				// Pass the call to pstn handle
				setResultData(number);
				return;
			}
		}
	}

	/**
	 * Check whether a number can be call using sip
	 * Should check if not matches preferences of excluded patterns
	 * @param number the number to test
	 * @return true if we should handle this number using SIP
	 */
	private boolean isCallableNumber(String number) {
		return true;
	}

}
