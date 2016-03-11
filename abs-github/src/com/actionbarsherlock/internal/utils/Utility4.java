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

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.internal.widget.IcsLinearLayout;

import org.xmlpull.v1.XmlPullParser;

public class Utility4 extends UtilityWrapper {

    private static final boolean DEBUG = false;
    private static final String TAG = "Utility4";



    @Override
    public void viewSetActivated(View view, boolean activated) {
        // Not valid for this target -- maybe we should throw something
    }

    @Override
    public boolean hasPermanentMenuKey(ViewConfiguration vcfg) {
        return false;
    }

    @Override
    public void jumpDrawablesToCurrentState(View v) {
        // Nothing to do
    }

    @Override
    public Drawable getActivityLogo(Context context) {
        if (context instanceof Activity) {
            //Even though native methods existed in API 9 and 10 they don't work
            //so just parse the manifest to look for the logo pre-Honeycomb
            final int resId = loadLogoFromManifest((Activity) context);
            if (resId != 0) {
                return context.getResources().getDrawable(resId);
            }
        }
        return null;
    }

    

    /**
     * Attempt to programmatically load the logo from the manifest file of an
     * activity by using an XML pull parser. This should allow us to read the
     * logo attribute regardless of the platform it is being run on.
     *
     * @param activity Activity instance.
     * @return Logo resource ID.
     */
    private static int loadLogoFromManifest(Activity activity) {
        int logo = 0;
        try {
            final String thisPackage = activity.getClass().getName();
            if (DEBUG) Log.i(TAG, "Parsing AndroidManifest.xml for " + thisPackage);

            final String packageName = activity.getApplicationInfo().packageName;
            final AssetManager am = activity.createPackageContext(packageName, 0).getAssets();
            final XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");

            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = xml.getName();

                    if ("application".equals(name)) {
                        //Check if the <application> has the attribute
                        if (DEBUG) Log.d(TAG, "Got <application>");

                        for (int i = xml.getAttributeCount() - 1; i >= 0; i--) {
                            if (DEBUG) Log.d(TAG, xml.getAttributeName(i) + ": " + xml.getAttributeValue(i));

                            if ("logo".equals(xml.getAttributeName(i))) {
                                logo = xml.getAttributeResourceValue(i, 0);
                                break; //out of for loop
                            }
                        }
                    } else if ("activity".equals(name)) {
                        //Check if the <activity> is us and has the attribute
                        if (DEBUG) Log.d(TAG, "Got <activity>");
                        Integer activityLogo = null;
                        String activityPackage = null;
                        boolean isOurActivity = false;

                        for (int i = xml.getAttributeCount() - 1; i >= 0; i--) {
                            if (DEBUG) Log.d(TAG, xml.getAttributeName(i) + ": " + xml.getAttributeValue(i));

                            //We need both uiOptions and name attributes
                            String attrName = xml.getAttributeName(i);
                            if ("logo".equals(attrName)) {
                                activityLogo = xml.getAttributeResourceValue(i, 0);
                            } else if ("name".equals(attrName)) {
                                activityPackage = ActionBarSherlockCompat.cleanActivityName(packageName, xml.getAttributeValue(i));
                                if (!thisPackage.equals(activityPackage)) {
                                    break; //on to the next
                                }
                                isOurActivity = true;
                            }

                            //Make sure we have both attributes before processing
                            if ((activityLogo != null) && (activityPackage != null)) {
                                //Our activity, logo specified, override with our value
                                logo = activityLogo.intValue();
                            }
                        }
                        if (isOurActivity) {
                            //If we matched our activity but it had no logo don't
                            //do any more processing of the manifest
                            break;
                        }
                    }
                }
                eventType = xml.nextToken();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DEBUG) Log.i(TAG, "Returning " + Integer.toHexString(logo));
        return logo;
    }

    @Override
    public CharSequence stringToUpper(CharSequence text) {
        if(text != null) {
            return text.toString().toUpperCase();
        }
        return null;
    }

    @Override
    public PopupWindow buildPopupWindow(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        Context wrapped = new ContextThemeWrapper(context, defStyleRes);
        return new PopupWindow(wrapped, attrs, defStyleAttr);
    }

    @Override
    public void jumpToCurrentState(Drawable indeterminateDrawable) {
        // Need to be implemented ?
        
    }

    @Override
    public int resolveSizeAndState(int size, int measureSpec, int state) {
        return View.resolveSize(size, measureSpec);
    }
    
    @Override
    public int getMeasuredState(View child) {
        return 0;
    }

    @Override
    public int combineMeasuredStates(int curState, int newState) {
        return newState;
    }

    @Override
    public boolean isLongPressEvent(KeyEvent evt) {
        return false;
    }
    
    @SuppressWarnings("deprecation")
    public void setBackgroundDrawable(View v, Drawable d) {
        v.setBackgroundDrawable(d);
    }

    @Override
    public void setLinearLayoutDividerPadding(LinearLayout l, int padding) {
        if(l instanceof IcsLinearLayout) {
            ((IcsLinearLayout)l).supportSetDividerPadding(padding);
        }
    }

    @Override
    public void setLinearLayoutDividerDrawable(LinearLayout l, Drawable drawable) {
        if(l instanceof IcsLinearLayout) {
            ((IcsLinearLayout)l).supportSetDividerDrawable(drawable);
        }
    }
}
