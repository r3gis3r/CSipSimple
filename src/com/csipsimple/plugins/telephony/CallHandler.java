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
package com.csipsimple.plugins.telephony;

import java.lang.reflect.Field;
import java.util.List;

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
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

public class CallHandler extends BroadcastReceiver {

	

	private static final String THIS_FILE = "CallHandlerTelephony";

	@Override
	public void onReceive(Context context, Intent intent) {
		if(SipManager.ACTION_GET_PHONE_HANDLERS.equals(intent.getAction())) {
			
			PendingIntent pendingIntent = null;
			String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			// We must handle that clean way cause when call just to 
			// get the row in account list expect this to reply correctly
			if(number != null && Compatibility.canMakeGSMCall(context)) {
				// Build pending intent
				Intent i = new Intent(Intent.ACTION_CALL);
				i.setData(Uri.fromParts("tel", number, null));
				pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
			}
			
			// Build icon
			Bitmap bmp = null;
			List<ResolveInfo> callers = Compatibility.getIntentsForCall(context);
			if(callers != null) {
				for(final ResolveInfo caller : callers) {
					if(caller.activityInfo.packageName.startsWith("com.android")) {
						PackageManager pm = context.getPackageManager();
						
						Resources remoteRes;
						try {
							//ComponentName cmp = new ComponentName(caller.activityInfo.packageName, caller.activityInfo.name);
							// To be sure, also try to resolve resovePackage for android api-4 and upper
							if(Compatibility.isCompatible(4)) {
								try {
									Field f = ResolveInfo.class.getDeclaredField("resolvePackageName");
									String resPackage = (String) f.get(caller);
									Log.d(THIS_FILE, "Load from " + resPackage);
									if(resPackage != null) {
										//cmp = new ComponentName(resPackage, caller.activityInfo.name);
									}
								} catch (Exception e) {
									Log.e(THIS_FILE, "Impossible to use 4 api ", e);
								}
								
							}
							remoteRes = pm.getResourcesForApplication(caller.activityInfo.applicationInfo);
							//remoteRes = pm.getResourcesForActivity(cmp);
							bmp = BitmapFactory.decodeResource(remoteRes, caller.getIconResource());
						} catch (NameNotFoundException e) {
							Log.e(THIS_FILE, "Impossible to load ", e);
						}
						
					}
				}
			}
			
			
			//Build the result for the row (label, icon, pending intent, and excluded phone number)
			Bundle results = getResultExtras(true);
			if(pendingIntent != null) {
				results.putParcelable(com.csipsimple.utils.CallHandler.EXTRA_REMOTE_INTENT_TOKEN, pendingIntent);
			}
			results.putString(Intent.EXTRA_TITLE, context.getResources().getString(R.string.use_pstn));
			if(bmp != null) {
				results.putParcelable(Intent.EXTRA_SHORTCUT_ICON, bmp);
			}
			
			// This will exclude next time tel:xxx is raised from csipsimple treatment which is wanted
			results.putString(Intent.EXTRA_PHONE_NUMBER, number);
			
		}
	}

}
