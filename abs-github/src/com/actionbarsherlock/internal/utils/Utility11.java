/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of ActionBarSherlock2.
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
 *  along with ActionBarSherlock2.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.actionbarsherlock.internal.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

// 11 is HONEYCOMB
@TargetApi(11)
public class Utility11 extends Utility9 {

    @Override
    public void viewSetActivated(View view, boolean activated) {
        view.setActivated(activated);
    }
    

    @Override
    public boolean hasPermanentMenuKey(ViewConfiguration vcfg) {
        return true;
    }
    
    @Override
    public void jumpDrawablesToCurrentState(View v) {
        v.jumpDrawablesToCurrentState();
    }
    
    @Override
    public void jumpToCurrentState(Drawable indeterminateDrawable) {
        indeterminateDrawable.jumpToCurrentState();
    }
    
    @Override
    public Drawable getActivityLogo(Context context) {
        Drawable mLogo = null;
        ApplicationInfo appInfo = context.getApplicationInfo();
        PackageManager pm = context.getPackageManager();
        if (context instanceof Activity) {
            try {
                mLogo = pm.getActivityLogo(((Activity) context).getComponentName());
            } catch (NameNotFoundException e) {
                Log.e("Utility", "Activity component name not found!", e);
            }
        }
        if (mLogo == null) {
            mLogo = appInfo.loadLogo(pm);
        }
        return mLogo;
    }
    
    @Override
    public PopupWindow buildPopupWindow(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        return new PopupWindow(context, attrs, defStyleAttr, defStyleRes);
    }
    
    @Override
    public int resolveSizeAndState(int size, int measureSpec, int state) {
        return View.resolveSizeAndState(size, measureSpec, state);
    }

    @Override
    public int getMeasuredState(View child) {
        return child.getMeasuredState();
    }
    
    @Override
    public int combineMeasuredStates(int curState, int newState) {
        return View.combineMeasuredStates(curState, newState);
    }

    @Override
    public void setLinearLayoutDividerDrawable(LinearLayout l, Drawable d) {
        l.setDividerDrawable(d);
        super.setLinearLayoutDividerDrawable(l, d);
    }
}
