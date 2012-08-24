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
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;

import java.util.HashMap;
import java.util.List;

public class Theme {

	private static final String THIS_FILE = "Theme";
	
	private final PackageManager pm;
	private Resources remoteRes = null;
    private PackageInfo pInfos = null;
	
	public Theme(Context ctxt, String packageName) {
		pm = ctxt.getPackageManager();
		
		ComponentName cn = ComponentName.unflattenFromString(packageName);
		
		try {
            pInfos = pm.getPackageInfo(cn.getPackageName(), 0);
            remoteRes = pm.getResourcesForApplication(cn.getPackageName());
        } catch (NameNotFoundException e) {
            Log.e(THIS_FILE, "Impossible to get resources from " + cn.toShortString());
            remoteRes = null;
            pInfos = null;
        }
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
		if(remoteRes != null && pInfos != null) {
			int id = remoteRes.getIdentifier(name, "drawable", pInfos.packageName);
            return pm.getDrawable(pInfos.packageName, id, pInfos.applicationInfo);
		}else {
			Log.d(THIS_FILE, "No results yet !! ");
		}
		return null;
	}

    public Integer getDimension(String name) {
        if(remoteRes != null && pInfos != null) {
            int id = remoteRes.getIdentifier(name, "dimen", pInfos.packageName);
            if(id > 0) {
                return remoteRes.getDimensionPixelSize(id);
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
	

    public void applyImageDrawable(ImageView subV, String res) {
        Drawable d = getDrawableResource(res);
        if(d != null) {
            subV.setImageDrawable(d);
        }
    }
	
    public void applyBackgroundStateListDrawable(View v, String prefix) {
        Drawable pressed = getDrawableResource(prefix+"_press");
        Drawable focused = getDrawableResource(prefix+"_focus");
        Drawable normal = getDrawableResource(prefix+"_normal");
        if(focused == null) {
            focused = pressed;
        }
        StateListDrawable std = null;
        if(pressed != null && focused != null && normal != null) {
            std = new StateListDrawable();
            std.addState(new int[] {android.R.attr.state_pressed}, pressed);
            std.addState(new int[] {android.R.attr.state_focused}, focused);
            std.addState(new int[] {}, normal);
        }
        
        if(std != null) {
            UtilityWrapper.getInstance().setBackgroundDrawable(v, std);
        }
    }
    

    public void applyBackgroundStateListSelectableDrawable(View v, String prefix) {
        Drawable pressed = getDrawableResource(prefix+"_press");
        Drawable focused = getDrawableResource(prefix+"_focus");
        Drawable selected = getDrawableResource(prefix+"_selected");
        Drawable unselected = getDrawableResource(prefix+"_unselected");
        if(focused == null) {
            focused = pressed;
        }
        StateListDrawable std = null;
        if(pressed != null && focused != null && selected != null && unselected != null) {
            std = new StateListDrawable();
            std.addState(new int[] {android.R.attr.state_pressed}, pressed);
            std.addState(new int[] {android.R.attr.state_focused}, focused);
            std.addState(new int[] {android.R.attr.state_selected}, selected);
            std.addState(new int[] {}, unselected);
        }
        
        if(std != null) {
            UtilityWrapper.getInstance().setBackgroundDrawable(v, std);
        }
    }
    
    public void applyLayoutMargin(View v, String prefix) {
        ViewGroup.MarginLayoutParams lp = null;
        try {
            lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
        }catch (ClassCastException e) {
            Log.e(THIS_FILE, "Trying to apply layout params to invalid layout " + v.getLayoutParams());
        }
        Integer marginTop = getDimension(prefix + "_top");
        Integer marginBottom = getDimension(prefix + "_bottom");
        Integer marginRight = getDimension(prefix + "_right");
        Integer marginLeft = getDimension(prefix + "_left");
        if(marginTop != null) {
            lp.topMargin = marginTop;
        }
        if(marginBottom != null) {
            lp.bottomMargin = marginBottom;
        }
        if(marginRight != null) {
            lp.rightMargin = marginRight;
        }
        if(marginLeft != null) {
            lp.leftMargin = marginLeft;
        }
        v.setLayoutParams(lp);
        
    }
    


    public void applyLayoutSize(View v, String prefix) {
        LayoutParams lp = v.getLayoutParams();
        Integer width = getDimension(prefix + "_width");
        Integer height = getDimension(prefix + "_height");
        if(width != null) {
            lp.width = width;
        }
        if(height != null) {
            lp.height = height;
        }
        v.setLayoutParams(lp);
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
