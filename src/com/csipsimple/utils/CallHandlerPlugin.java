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

import android.Manifest.permission;
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
import android.os.Handler;
import android.text.TextUtils;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallHandlerPlugin {

    private static final String THIS_FILE = "CallHandlerPlugin";
    public static final String EXTRA_REMOTE_INTENT_TOKEN = "android.intent.extra.remote_intent_token";

    private OnLoadListener listener;
    private PendingIntent pendingIntent = null;
    private Bitmap icon = null;
    private String nextExclude = null;
    private String label = null;
    private long accountId = SipProfile.INVALID_ID;
    private final Context context;

    private static Map<String, String> AVAILABLE_HANDLERS = null;

    private final static String VIRTUAL_ACC_MAX_ENTRIES = "maxVirtualAcc";
    private final static String VIRTUAL_ACC_PREFIX = "vAcc_";

    public CallHandlerPlugin(Context ctxt) {
        context = ctxt;
    }
    private static Handler mHandler = null;

    /**
     * Load plugin from a given plugin component name 
     * @param componentName Fully qualified component name to call
     * @param number Optional number to call
     * @param l Listener to fire on load completion
     */
    public void loadFrom(final String componentName, String number, OnLoadListener l) {
        listener = l;
        ComponentName cn = ComponentName.unflattenFromString(componentName);

        Intent it = new Intent(SipManager.ACTION_GET_PHONE_HANDLERS);
        it.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        it.setComponent(cn);

        context.sendOrderedBroadcast(it, permission.PROCESS_OUTGOING_CALLS,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle resolvedInfos = getResultExtras(true);
                        fillWith(componentName, resolvedInfos);
                        if (listener != null) {
                            listener.onLoad(CallHandlerPlugin.this);
                        }
                    }
                }, mHandler, Activity.RESULT_OK, null, null);

    }

    /**
     * Load plugin from a given account id.
     * @param accountId Fake (< -1) account id to load plugin from
     * @param number Optional number to call
     * @param l Listener to fire on load completion
     */
    public void loadFrom(final Long accountId, String number, OnLoadListener l) {
        Map<String, String> callHandlers = getAvailableCallHandlers(context);
        for (String packageName : callHandlers.keySet()) {
            if (accountId == getAccountIdForCallHandler(context, packageName)) {
                loadFrom(packageName, number, l);
                return;
            }
        }

    }

    public void fillWith(String packageName, Bundle resolvedInfos) {

        pendingIntent = (PendingIntent) resolvedInfos.getParcelable(EXTRA_REMOTE_INTENT_TOKEN);
        icon = (Bitmap) resolvedInfos.getParcelable(Intent.EXTRA_SHORTCUT_ICON);
        nextExclude = resolvedInfos.getString(Intent.EXTRA_PHONE_NUMBER);
        label = resolvedInfos.getString(Intent.EXTRA_TITLE);
        if(TextUtils.isEmpty(label)) {
            if(AVAILABLE_HANDLERS != null && AVAILABLE_HANDLERS.containsKey(packageName)) {
                label = AVAILABLE_HANDLERS.get(packageName);
            }
        }

        accountId = getAccountIdForCallHandler(context, packageName);
    }

    /**
     * Retrieve internal id of call handler as saved in databases It should be
     * some negative < SipProfile.INVALID_ID number
     * 
     * @param ctxt Application context
     * @param packageName name of the call handler package
     * @return the id of this call handler in databases
     */
    public static Long getAccountIdForCallHandler(Context ctxt, String packageName) {
        SharedPreferences prefs = ctxt.getSharedPreferences("handlerCache", Context.MODE_PRIVATE);

        long accountId = SipProfile.INVALID_ID;
        try {
            accountId = prefs.getLong(VIRTUAL_ACC_PREFIX + packageName, SipProfile.INVALID_ID);
        } catch (Exception e) {
            Log.e(THIS_FILE, "Can't retrieve call handler cache id - reset");
        }
        if (accountId == SipProfile.INVALID_ID) {
            // We never seen this one, add a new entry for account id
            int maxAcc = prefs.getInt(VIRTUAL_ACC_MAX_ENTRIES, 0x0);
            int currentEntry = maxAcc + 1;
            accountId = SipProfile.INVALID_ID - (long) currentEntry;
            Editor edt = prefs.edit();
            edt.putLong(VIRTUAL_ACC_PREFIX + packageName, accountId);
            edt.putInt(VIRTUAL_ACC_MAX_ENTRIES, currentEntry);
            edt.commit();
        }
        return accountId;
    }

    /**
     * Retrieve outgoing call handlers available as plugin for csipsimple Also
     * contains stock call handler if available
     * 
     * @param ctxt context of application
     * @return A map of package name => Fancy name of call handler
     */
    public static Map<String, String> getAvailableCallHandlers(Context ctxt) {

        if (AVAILABLE_HANDLERS == null) {
            AVAILABLE_HANDLERS = new HashMap<String, String>();

            PackageManager packageManager = ctxt.getPackageManager();
            Intent it = new Intent(SipManager.ACTION_GET_PHONE_HANDLERS);

            List<ResolveInfo> availables = packageManager.queryBroadcastReceivers(it, 0);
            for (ResolveInfo resInfo : availables) {
                ActivityInfo actInfos = resInfo.activityInfo;
                Log.d(THIS_FILE, "Found call handler " + actInfos.packageName + " " + actInfos.name);
                if (packageManager.checkPermission(permission.PROCESS_OUTGOING_CALLS,
                        actInfos.packageName) == PackageManager.PERMISSION_GRANTED) {
                    String packagedActivityName = (new ComponentName(actInfos.packageName,
                            actInfos.name)).flattenToString();
                    AVAILABLE_HANDLERS.put(packagedActivityName,
                            (String) resInfo.loadLabel(packageManager));
                }
            }
        }

        return AVAILABLE_HANDLERS;
    }

    /**
     * Reset cache of outgoing call handlers
     */
    public static void clearAvailableCallHandlers() {
        AVAILABLE_HANDLERS = null;
    }

    /**
     * Interface for listener about load state of remote call handler plugin
     */
    public interface OnLoadListener {
        /**
         * Fired when call handler has been loaded
         * 
         * @param ch the call handler object that has been loaded
         */
        void onLoad(CallHandlerPlugin ch);
    }

    /**
     * Get the display label name for this call handler
     * 
     * @return A string to display to represent this call handler
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the icon bitmap for this call handler
     * 
     * @return the bitmap icon representing the call handler
     */
    public Bitmap getIcon() {
        return icon;
    }

    /**
     * Get the icon drawable for this call handler
     * 
     * @return the drawable icon representing the call handler
     */
    public Drawable getIconDrawable() {
        if (icon != null) {
            //return new BitmapDrawable(icon);
            return new BitmapDrawable(context.getResources(), icon);
        }
        return null;
    }

    /**
     * The pending intent to fire when user select this call handler This is
     * only populated once loadFromXXX has been launched and called back
     * 
     * @return the intent to fire
     */
    public PendingIntent getIntent() {
        return pendingIntent;
    }

    /**
     * The number to exclude from treatment in next run of outgoing call if any.
     * This is useful if the call handler also launch a make call intent to
     * ignore this intent from processing This is only populated once
     * loadFromXXX has been launched and called back
     * 
     * @return The phone number to ignore or null if none to ignore
     */
    public String getNextExcludeTelNumber() {
        return nextExclude;
    }

    /**
     * Get the call id as stored in db for this call handler Should be <
     * SipProfile.INVALID_ID
     * 
     * @return the sip account id
     */
    public long getAccountId() {
        return accountId;
    }

    /**
     * Build a fake sip profile object for this plugin call handler It will
     * contain id for this callhandler, display name and icon
     * 
     * @return the SipProfile equivalent object for this CallHandler
     */
    public SipProfile getFakeProfile() {
        SipProfile profile = new SipProfile();
        profile.id = accountId;
        profile.display_name = label;
        profile.icon = icon;
        return profile;

    }

    public static void initHandler() {
        if(mHandler == null) {
            mHandler = new Handler();
        }
    }

}
