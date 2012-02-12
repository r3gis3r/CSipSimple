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
/**
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2010 The Android Open Source Project
 */


package com.csipsimple.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.Uri.Builder;
//import android.net.sip.SipManager;
import android.telephony.TelephonyManager;

import com.csipsimple.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides static functions to quickly test the capabilities of this device. The static
 * members are not safe for threading
 */
public final class PhoneCapabilityTester {
    private static boolean sIsInitialized;
    private static boolean sIsPhone;
   // private static boolean sIsSipPhone;

    /**
     * Tests whether the Intent has a receiver registered. This can be used to show/hide
     * functionality (like Phone, SMS)
     */
    public static boolean isIntentRegistered(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return receiverList.size() > 0;
    }
    
    /**
     * Resolve the intent as a activity receiver and return all activities related
     * @param ctxt The application content
     * @param i the intent to resolve
     * @return List of resolved info
     */
    public static List<ResolveInfo> getPossibleActivities(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        try {
            return pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY
                    | PackageManager.GET_RESOLVED_FILTER);
        } catch (NullPointerException e) {
            return new ArrayList<ResolveInfo>();
        }
    }


    /**
     * Returns true if this device can be used to make phone calls
     */
    public static boolean isPhone(Context context) {
        if (!sIsInitialized) initialize(context);
        // Is the device physically capabable of making phone calls?
        return sIsPhone;
    }

    private static void initialize(Context context) {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        
        sIsPhone = (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE);
        //sIsSipPhone = sIsPhone && SipManager.isVoipSupported(context);
        callIntents = getPossibleActivities(context, getPriviledgedIntent("123"));
        sIsInitialized = true;
    }

    /**
     * Returns true if this device can be used to make sip calls
     */
    /*
    public static boolean isSipPhone(Context context) {
        if (!sIsInitialized) initialize(context);
        return sIsSipPhone;
    }
    */


    public static Intent getPriviledgedIntent(String number) {
        Intent i = new Intent("android.intent.action.CALL_PRIVILEGED");
        Builder b = new Uri.Builder();
        b.scheme("tel").appendPath(number);
        i.setData(b.build());
        return i;
    }

    private static List<ResolveInfo> callIntents = null;

    /**
     * Find packages registered for priviledged call management
     * @param ctxt the application context
     * @return list of package resolved info for a priviledged call intent
     */
    public final static List<ResolveInfo> resolvePackageForPriviledgedCall(Context ctxt) {
        if (!sIsInitialized) {
            initialize(ctxt);
        }
        return callIntents;
    }
    
    /**
     * Returns true if the device has an SMS application installed.
     */
    public static boolean isSmsIntentRegistered(Context context) {
        // Don't cache the result as the user might install third party apps to send SMS
        final Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("smsto", "", null));
        return isIntentRegistered(context, intent);
    }

    /**
     * True if we are using two-pane layouts ("tablet mode"), false if we are using single views
     * ("phone mode")
     */
    public static boolean isUsingTwoPanes(Context context) {
        return context.getResources().getBoolean(R.bool.use_dual_panes);
    }
}
