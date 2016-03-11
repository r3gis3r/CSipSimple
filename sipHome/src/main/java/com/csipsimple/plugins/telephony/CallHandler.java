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
package com.csipsimple.plugins.telephony;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PhoneCapabilityTester;

import java.util.List;

public class CallHandler extends BroadcastReceiver {

	

	private static final String THIS_FILE = "CallHandlerTelephony";

	private static Bitmap sPhoneAppBmp = null;
	private static boolean sPhoneAppInfoLoaded = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(SipManager.ACTION_GET_PHONE_HANDLERS.equals(intent.getAction())) {
			
			PendingIntent pendingIntent = null;
			String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			// We must handle that clean way cause when call just to 
			// get the row in account list expect this to reply correctly
			if(number != null && PhoneCapabilityTester.isPhone(context)) {
				// Build pending intent
				Intent i = new Intent(Intent.ACTION_CALL);
				i.setData(Uri.fromParts("tel", number, null));
				pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
			}
			
			// Retrieve and cache infos from the phone app 
			if(!sPhoneAppInfoLoaded) {
    			List<ResolveInfo> callers = PhoneCapabilityTester.resolveActivitiesForPriviledgedCall(context);
    			if(callers != null) {
    				for(final ResolveInfo caller : callers) {
    					if(caller.activityInfo.packageName.startsWith("com.android")) {
    						PackageManager pm = context.getPackageManager();
    						Resources remoteRes;
    						try {
    							// We load the resource in the context of the remote app to have a bitmap to return.
    						    remoteRes = pm.getResourcesForApplication(caller.activityInfo.applicationInfo);
    						    sPhoneAppBmp = BitmapFactory.decodeResource(remoteRes, caller.getIconResource());
    						} catch (NameNotFoundException e) {
    							Log.e(THIS_FILE, "Impossible to load ", e);
    						}
    						break;
    					}
    				}
    			}
    			sPhoneAppInfoLoaded = true;
			}
			
			
			//Build the result for the row (label, icon, pending intent, and excluded phone number)
			Bundle results = getResultExtras(true);
			if(pendingIntent != null) {
				results.putParcelable(CallHandlerPlugin.EXTRA_REMOTE_INTENT_TOKEN, pendingIntent);
			}
			results.putString(Intent.EXTRA_TITLE, context.getResources().getString(R.string.use_pstn));
			if(sPhoneAppBmp != null) {
				results.putParcelable(Intent.EXTRA_SHORTCUT_ICON, sPhoneAppBmp);
			}
			
			// This will exclude next time tel:xxx is raised from csipsimple treatment which is wanted
			results.putString(Intent.EXTRA_PHONE_NUMBER, number);
			
		}
	}

}
