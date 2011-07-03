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

import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Compatibility;

public class CallHandler extends BroadcastReceiver {

	

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
						Drawable icon = caller.loadIcon(pm);
						BitmapDrawable bd = ((BitmapDrawable) icon);
						bmp = bd.getBitmap();
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
