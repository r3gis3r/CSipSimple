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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;


public class CallHandler {

	private static final String THIS_FILE = "CallHandler";
	private onLoadListener listener;
	private PendingIntent pendingIntent = null;
	private Bitmap icon = null;
	private String nextExclude = null;
	private String label = null;
	private int accountId = SipProfile.INVALID_ID;
	private Context context = null;
	
	private final static String VIRTUAL_ACC_MAX_ENTRIES = "maxVirtualAcc";
	private final static String VIRTUAL_ACC_PREFIX = "vAcc_";
	
	public CallHandler(Context ctxt){
		context  = ctxt;
	}

	public void loadFrom(final String packageName, String number, onLoadListener l) {
		listener = l;
		
		String[] splitPackage = packageName.split("/");
		ComponentName cn = new ComponentName(splitPackage[0], splitPackage[1]);
		
		Intent it = new Intent(SipManager.ACTION_GET_PHONE_HANDLERS);
		it.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
		it.setComponent(cn);
		
		context.sendOrderedBroadcast(it, null, new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle resolvedInfos = getResultExtras(true);
				fillWith(packageName, resolvedInfos);
				if(listener != null) {
					listener.onLoad(CallHandler.this);
				}
			}
		}, null, Activity.RESULT_OK, null, null);

		Log.d(THIS_FILE, "After broadcast");
	}
	
	public void loadFrom( final Integer accountId, String number, onLoadListener l) {
		HashMap<String, String> callHandlers = getAvailableCallHandlers(context);
		for(String packageName : callHandlers.values()) {
			if(accountId == getAccountIdForCallHandler(context, packageName)) {
				loadFrom(packageName, number, l);
				return;
			}
		}
		
	}
	

	public void fillWith( String packageName, Bundle resolvedInfos) {
		
		pendingIntent = (PendingIntent) resolvedInfos.getParcelable(Intent.EXTRA_REMOTE_INTENT_TOKEN);
		icon = (Bitmap) resolvedInfos.getParcelable(Intent.EXTRA_SHORTCUT_ICON);
		nextExclude = resolvedInfos.getString(Intent.EXTRA_PHONE_NUMBER);
		label = resolvedInfos.getString(Intent.EXTRA_TITLE);
		
		accountId = getAccountIdForCallHandler(context, packageName);
	}

	
	public static Integer getAccountIdForCallHandler(Context ctxt, String packageName) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		
		int accountId = prefs.getInt(VIRTUAL_ACC_PREFIX + packageName, SipProfile.INVALID_ID);
		if(accountId == SipProfile.INVALID_ID) {
			// We never seen this one, add a new entry for account id
			int maxAcc = prefs.getInt(VIRTUAL_ACC_MAX_ENTRIES, 0x0);
			int currentEntry = maxAcc + 1;
			accountId = SipProfile.INVALID_ID - currentEntry;
			Editor edt = prefs.edit();
			edt.putInt(VIRTUAL_ACC_PREFIX + packageName, accountId);
			edt.putInt(VIRTUAL_ACC_MAX_ENTRIES, currentEntry);
			edt.commit();
		}
		return accountId;
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

	public CharSequence getLabel() {
		return label;
	}

	public Bitmap getIcon() {
		return icon;
	}
	
	public Drawable getIconDrawable() {
		if(icon != null) {
			return new BitmapDrawable(icon);
		}
		return null;
	}

	public PendingIntent getIntent() {
		return pendingIntent;
	}

	public String getNextExcludeTelNumber() {
		return nextExclude;
	}
	
	public int getAccountId() {
		return accountId;
	}
}
