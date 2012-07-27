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

package com.csipsimple.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;

import java.util.HashMap;
import java.util.List;

public class Theme {

	private static final String THIS_FILE = "Theme";
	
	
	PackageManager pm;
	//private String packageName;
	private Bundle resolvedInfos = null;
	private onLoadListener listener;
	
	public Theme(Context ctxt, String packageName, onLoadListener l) {
		listener = l;
		pm = ctxt.getPackageManager();
		
		String[] splitPackage = packageName.split("/");
		ComponentName cn = new ComponentName(splitPackage[0], splitPackage[1]);
		
		Intent it = new Intent(SipManager.ACTION_GET_DRAWABLES);
		it.setComponent(cn);
		
		ctxt.sendOrderedBroadcast(it, null, new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				resolvedInfos = getResultExtras(true);
				Log.d(THIS_FILE, "We have logs : " + resolvedInfos.getString("btn_dial_normal"));
				if(listener != null) {
					listener.onLoad(Theme.this);
				}
			}
		}, null, Activity.RESULT_OK, null, null);

		Log.d(THIS_FILE, "After broadcast" );
			
	}
	
	
	public static HashMap<String, String> getAvailableThemes(Context ctxt){
		HashMap<String, String> result = new HashMap<String, String>();
		result.put(ctxt.getResources().getString(R.string.app_name), "");
		
		PackageManager packageManager = ctxt.getPackageManager();
		Intent it = new Intent(SipManager.ACTION_GET_DRAWABLES);
		
		List<ResolveInfo> availables = packageManager.queryBroadcastReceivers(it, 0);
		Log.d(THIS_FILE, "We found " + availables.size() + "themes");
		for(ResolveInfo resInfo : availables) {
			Log.d(THIS_FILE, "We have -- "+resInfo);
			ActivityInfo actInfos = resInfo.activityInfo;
			String packagedActivityName = actInfos.packageName + "/" + actInfos.name;
			result.put((String) resInfo.loadLabel(packageManager), packagedActivityName);
		}
		
		return result;
	}
	
	public Drawable getDrawableResource(String name) {
		if(resolvedInfos != null) {
			String drawableName = resolvedInfos.getString(name);
			if(drawableName != null) {
				Log.d(THIS_FILE, "Theme package we search for " + drawableName);
				
				String packageName = drawableName.split(":")[0];
				
				//Log.d(THIS_FILE, "Theme package we search for " + packageName);
				
				PackageInfo pInfos;
				try {
					pInfos = pm.getPackageInfo(packageName, 0);
					Resources remoteRes = pm.getResourcesForApplication(pInfos.applicationInfo);
					int id = remoteRes.getIdentifier(drawableName, null, null);
					return pm.getDrawable(pInfos.packageName, id, pInfos.applicationInfo);
				} catch (NameNotFoundException e) {
					Log.e(THIS_FILE, "Unable to get resources for this theme package");
				}
			}else {
				Log.w(THIS_FILE, "Theme is not complete, not found : "+name);
			}
		}else {
			Log.d(THIS_FILE, "No results yet !! ");
		}
		return null;
	}


	public void applyBackgroundDrawable(View button, String res) {
		Drawable d = getDrawableResource(res);
		if(d != null) {
		    UtilityWrapper.getInstance().setBackgroundDrawable(button, d);
		}
	}
	
	
	public interface onLoadListener {
		void onLoad(Theme t);
	}
	
	
	private static boolean needRepeatableFix() {
        // In ICS and upper the problem is fixed, so no need to apply by code
	    return (!Compatibility.isCompatible(14));
	}
	
    /**
     * @param v The view to fix background of.
     * @see #fixRepeatableDrawable(Drawable)
     */
    public static void fixRepeatableBackground(View v) {
        if(!needRepeatableFix()) {
            return;
        }
        fixRepeatableDrawable(v.getBackground());
    }
    
    /**
     * Fix the repeatable background of a drawable.
     * This support both bitmap and layer drawables
     * @param d the drawable to fix.
     */
    public static void fixRepeatableDrawable(Drawable d) {
        if(!needRepeatableFix()) {
            return;
        }
        if (d instanceof LayerDrawable) {
            LayerDrawable layer = (LayerDrawable) d;
            for (int i = 0; i < layer.getNumberOfLayers(); i++) {
                fixRepeatableDrawable(layer.getDrawable(i));
            }
        } else if (d instanceof BitmapDrawable) {
            fixRepeatableBitmapDrawable((BitmapDrawable) d);
        }
    
    }
    
    /**
     * Fix the repeatable background of a bitmap drawable.
     * This only support a BitmapDrawable
     * @param d the BitmapDrawable to set repeatable.
     */
    public static void fixRepeatableBitmapDrawable(BitmapDrawable d) {
        if(!needRepeatableFix()) {
            return;
        }
        // I don't want to mutate because it's better to share the drawable fix for all that share this constant state
        //d.mutate();
        //Log.d(THIS_FILE, "Exisiting tile mode : " + d.getTileModeX() + ", "+ d.getTileModeY());
        d.setTileModeXY(d.getTileModeX(), d.getTileModeY());
        
    }
}
