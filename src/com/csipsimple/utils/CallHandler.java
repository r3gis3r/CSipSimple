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
package com.csipsimple.utils;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.csipsimple.api.SipManager;


public class CallHandler {

	private static final String THIS_FILE = "CallHandler";
	private static onLoadListener listener;

	public CallHandler(Intent intent, Bundle resolvedInfos) {
		
	}

	public static void getCallHandler(Context ctxt, String packageName, String number, onLoadListener l) {
		listener = l;
		
		String[] splitPackage = packageName.split("/");
		ComponentName cn = new ComponentName(splitPackage[0], splitPackage[1]);
		
		Intent it = new Intent(SipManager.ACTION_GET_PHONE_HANDLERS);
		it.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
		it.setComponent(cn);
		
		ctxt.sendOrderedBroadcast(it, null, new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle resolvedInfos = getResultExtras(true);
				
				if(listener != null) {
					listener.onLoad(new CallHandler(intent, resolvedInfos));
				}
			}
		}, null, Activity.RESULT_OK, null, null);

		Log.d(THIS_FILE, "After broadcast");
	}
	
	public static HashMap<String, String> getAvailableCallHandlers(Context ctxt){
		HashMap<String, String> result = new HashMap<String, String>();
		
		PackageManager packageManager = ctxt.getPackageManager();
		Intent it = new Intent(SipManager.ACTION_GET_PHONE_HANDLERS);
		
		List<ResolveInfo> availables = packageManager.queryBroadcastReceivers(it, 0);
		for(ResolveInfo resInfo : availables) {
			ActivityInfo actInfos = resInfo.activityInfo;
			String packagedActivityName = actInfos.packageName + "/" + actInfos.name;
			result.put((String) resInfo.loadLabel(packageManager), packagedActivityName);
		}
		
		return result;
	}

	public interface onLoadListener {
		void onLoad(CallHandler ch);
	}
}
