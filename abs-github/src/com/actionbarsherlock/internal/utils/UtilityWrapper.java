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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class UtilityWrapper {

    private static UtilityWrapper instance;

    public static UtilityWrapper getInstance() {
        if (instance == null) {
            if (Build.VERSION.SDK_INT >= 16) {
                instance = new com.actionbarsherlock.internal.utils.Utility16();
            } else if (Build.VERSION.SDK_INT >= 14) {
                instance = new com.actionbarsherlock.internal.utils.Utility14();
            } else if (Build.VERSION.SDK_INT >= 11) {
                instance = new com.actionbarsherlock.internal.utils.Utility11();
            } else if (Build.VERSION.SDK_INT >= 8) {
                instance = new com.actionbarsherlock.internal.utils.Utility8();
            } else if (Build.VERSION.SDK_INT >= 7) {
                instance = new com.actionbarsherlock.internal.utils.Utility7();
            } else {
                instance = new com.actionbarsherlock.internal.utils.Utility4();
            }
        }

        return instance;
    }
    
    public abstract void viewSetActivated(View view, boolean activated);
    
    public abstract boolean hasPermanentMenuKey(ViewConfiguration vcfg);
    
    public abstract void jumpDrawablesToCurrentState(View v);
    
    public abstract Drawable getActivityLogo(Context context);
    
    public abstract CharSequence stringToUpper(CharSequence text);
    
    public abstract PopupWindow buildPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes);

    public abstract void jumpToCurrentState(Drawable indeterminateDrawable);
    
    public abstract int resolveSizeAndState(int size, int measureSpec, int state);
    
    public abstract int getMeasuredState(View child);
    
    public abstract int combineMeasuredStates(int curState, int newState);
    
    public abstract boolean isLongPressEvent(KeyEvent evt);
    
    public abstract void setBackgroundDrawable(View v, Drawable d);
    
    public abstract void setLinearLayoutDividerPadding(LinearLayout l, int padding);
    
    public abstract void setLinearLayoutDividerDrawable(LinearLayout l, Drawable drawable);
    
    public static Method safelyGetSuperclassMethod(Class<?> cls, String methodName, Class<?>... parametersType) {
        Class<?> sCls = cls.getSuperclass();
        while(sCls != Object.class) {
            try {
                return sCls.getDeclaredMethod(methodName, parametersType);
            } catch (NoSuchMethodException e) {
                // Just super it again
            }
            sCls = sCls.getSuperclass();
        }
        throw new RuntimeException("Method not found " + methodName);
    }
    
    public static Object safelyInvokeMethod(Method method, Object receiver, Object... args) {
        try {
            return method.invoke(receiver, args);
        } catch (IllegalArgumentException e) {
            Log.e("Safe invoke fail", "Invalid args", e);
        } catch (IllegalAccessException e) {
            Log.e("Safe invoke fail", "Invalid access", e);
        } catch (InvocationTargetException e) {
            Log.e("Safe invoke fail", "Invalid target", e);
        }
        
        return null;
    }
    
}
