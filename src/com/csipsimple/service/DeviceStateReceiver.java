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

package com.csipsimple.service;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.ExtraPlugins;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.NightlyUpdater;
import com.csipsimple.utils.PhoneCapabilityTester;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.RewriterPlugin;

public class DeviceStateReceiver extends BroadcastReceiver {

    //private static final String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
    private static final String THIS_FILE = "Device State";
    public static final String APPLY_NIGHTLY_UPLOAD = "com.csipsimple.action.APPLY_NIGHTLY";

    @Override
    public void onReceive(Context context, Intent intent) {

        PreferencesProviderWrapper prefWrapper = new PreferencesProviderWrapper(context);
        String intentAction = intent.getAction();

        //
        // ACTION_DATA_STATE_CHANGED
        // Data state change is used to detect changes in the mobile
        // network such as a switch of network type (GPRS, EDGE, 3G)
        // which are not detected by the Connectivity changed broadcast.
        //
        //
        // ACTION_CONNECTIVITY_CHANGED
        // Connectivity change is used to detect changes in the overall
        // data network status as well as a switch between wifi and mobile
        // networks.
        //
        if (/*intentAction.equals(ACTION_DATA_STATE_CHANGED) ||*/
                intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                intentAction.equals(Intent.ACTION_BOOT_COMPLETED)) {
            
            if (prefWrapper.isValidConnectionForIncoming()
                    &&
                    !prefWrapper
                            .getPreferenceBooleanValue(PreferencesProviderWrapper.HAS_BEEN_QUIT)) {
                Log.d(THIS_FILE, "Try to start service if not already started");
                Intent sip_service_intent = new Intent(context, SipService.class);
                context.startService(sip_service_intent);
            }

        } else if (intentAction.equals(SipManager.INTENT_SIP_ACCOUNT_ACTIVATE)) {
            context.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);

            long accId;
            accId = intent.getLongExtra(SipProfile.FIELD_ID, SipProfile.INVALID_ID);

            if (accId == SipProfile.INVALID_ID) {
                // allow remote side to send us integers.
                // previous call will warn, but that's fine, no worries
                accId = intent.getLongExtra(SipProfile.FIELD_ID, SipProfile.INVALID_ID);
            }

            if (accId != SipProfile.INVALID_ID) {
                boolean active = intent.getBooleanExtra(SipProfile.FIELD_ACTIVE, true);
                ContentValues cv = new ContentValues();
                cv.put(SipProfile.FIELD_ACTIVE, active);
                int done = context.getContentResolver().update(
                        ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, accId), cv,
                        null, null);
                if (done > 0) {
                    if (prefWrapper.isValidConnectionForIncoming()) {
                        Intent sipServiceIntent = new Intent(context, SipService.class);
                        context.startService(sipServiceIntent);
                    }
                }
            }
        } else if (Intent.ACTION_PACKAGE_ADDED.equalsIgnoreCase(intentAction) ||
                Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(intentAction)) {
            CallHandlerPlugin.clearAvailableCallHandlers();
            RewriterPlugin.clearAvailableRewriters();
            ExtraPlugins.clearDynPlugins();
            PhoneCapabilityTester.deinit();
        } else if (APPLY_NIGHTLY_UPLOAD.equals(intentAction)) {
            NightlyUpdater nu = new NightlyUpdater(context);
            nu.applyUpdate(intent);
        }
    }

}
