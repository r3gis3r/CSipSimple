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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateFormat;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public final class SipProfileJson {

    private final static String THIS_FILE = "SipProfileJson";
    private final static String FILTER_KEY = "filters";

    private SipProfileJson() {
    }

    private static JSONObject serializeBaseSipProfile(SipProfile profile) {

        ContentValues cv = profile.getDbContentValues();
        Columns cols = new Columns(DBProvider.ACCOUNT_FULL_PROJECTION,
                DBProvider.ACCOUNT_FULL_PROJECTION_TYPES);
        return cols.contentValueToJSON(cv);
    }

    private static JSONObject serializeBaseFilter(Filter filter) {

        ContentValues cv = filter.getDbContentValues();
        Columns cols = new Columns(Filter.FULL_PROJ, Filter.FULL_PROJ_TYPES);
        return cols.contentValueToJSON(cv);
    }

    public static JSONObject serializeSipProfile(Context context, SipProfile profile) {
        JSONObject jsonProfile = serializeBaseSipProfile(profile);
        JSONArray jsonFilters = new JSONArray();

        Cursor c = Filter.getFiltersCursorForAccount(context, profile.id);
        int numRows = c.getCount();
        c.moveToFirst();
        for (int i = 0; i < numRows; ++i) {
            Filter f = new Filter(c);
            try {
                jsonFilters.put(i, serializeBaseFilter(f));
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Impossible to add fitler", e);
            }
            c.moveToNext();
        }
        c.close();

        try {
            jsonProfile.put(FILTER_KEY, jsonFilters);
        } catch (JSONException e) {
            Log.e(THIS_FILE, "Impossible to add fitlers", e);
        }

        return jsonProfile;
    }

    private static JSONArray serializeSipProfiles(Context ctxt) {

        JSONArray jsonSipProfiles = new JSONArray();
        Cursor c = ctxt.getContentResolver().query(SipProfile.ACCOUNT_URI,
                DBProvider.ACCOUNT_FULL_PROJECTION, null, null, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    SipProfile account = new SipProfile(c);
                    JSONObject p = serializeSipProfile(ctxt, account);
                    try {
                        jsonSipProfiles.put(jsonSipProfiles.length(), p);
                    } catch (JSONException e) {
                        Log.e(THIS_FILE, "Impossible to add profile", e);
                    }
                }

            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }

        // Add negative fake accounts

        Map<String, String> callHandlers = CallHandlerPlugin.getAvailableCallHandlers(ctxt);
        for (String packageName : callHandlers.keySet()) {
            final Long externalAccountId = CallHandlerPlugin
                    .getAccountIdForCallHandler(ctxt, packageName);
            SipProfile gsmProfile = new SipProfile();
            gsmProfile.id = externalAccountId;
            JSONObject p = serializeSipProfile(ctxt, gsmProfile);
            try {
                jsonSipProfiles.put(jsonSipProfiles.length(), p);
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Impossible to add profile", e);
            }
        }

        return jsonSipProfiles;
    }

    private static JSONObject serializeSipSettings(Context ctxt) {
        PreferencesWrapper prefs = new PreferencesWrapper(ctxt);
        return prefs.serializeSipSettings();
    }

    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_SETTINGS = "settings";

    /**
     * Save current sip configuration
     * 
     * @param ctxt
     * @return
     */
    public static boolean saveSipConfiguration(Context ctxt) {
        File dir = PreferencesWrapper.getConfigFolder(ctxt);
        if (dir != null) {
            Date d = new Date();
            File file = new File(dir.getAbsoluteFile() + File.separator + "backup_"
                    + DateFormat.format("yy-MM-dd_kkmmss", d) + ".json");
            Log.d(THIS_FILE, "Out dir " + file.getAbsolutePath());

            JSONObject configChain = new JSONObject();
            try {
                configChain.put(KEY_ACCOUNTS, serializeSipProfiles(ctxt));
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Impossible to add profiles", e);
            }
            try {
                configChain.put(KEY_SETTINGS, serializeSipSettings(ctxt));
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Impossible to add profiles", e);
            }

            try {
                // Create file
                FileWriter fstream = new FileWriter(file.getAbsoluteFile());
                BufferedWriter out = new BufferedWriter(fstream);
                out.write(configChain.toString(2));
                // Close the output stream
                out.close();
                return true;
            } catch (Exception e) {
                // Catch exception if any
                Log.e(THIS_FILE, "Impossible to save config to disk", e);
                return false;
            }
        }
        return false;
    }

    // --- RESTORE PART --- //
    private static boolean restoreSipProfile(JSONObject jsonObj, ContentResolver cr) {
        // Restore accounts
        Columns cols;
        ContentValues cv;

        cols = new Columns(DBProvider.ACCOUNT_FULL_PROJECTION,
                DBProvider.ACCOUNT_FULL_PROJECTION_TYPES);
        cv = cols.jsonToContentValues(jsonObj);

        long profileId = cv.getAsLong(SipProfile.FIELD_ID);
        if(profileId >= 0) {
            Uri insertedUri = cr.insert(SipProfile.ACCOUNT_URI, cv);
            profileId = ContentUris.parseId(insertedUri);
        }
        // TODO : else restore call handler in private db
        
        
        // Restore filters
        cols = new Columns(Filter.FULL_PROJ, Filter.FULL_PROJ_TYPES);
        try {
            JSONArray filtersObj = jsonObj.getJSONArray(FILTER_KEY);
            Log.d(THIS_FILE, "We have filters for " + profileId + " > " + filtersObj.length());
            for (int i = 0; i < filtersObj.length(); i++) {
                JSONObject filterObj = filtersObj.getJSONObject(i);
                // Log.d(THIS_FILE, "restoring "+filterObj.toString(4));
                cv = cols.jsonToContentValues(filterObj);
                cv.put(Filter.FIELD_ACCOUNT, profileId);
                cr.insert(SipManager.FILTER_URI, cv);
            }
        } catch (JSONException e) {
            Log.e(THIS_FILE, "Error while restoring filters", e);
        }

        return false;
    }

    private static void restoreSipSettings(Context ctxt, JSONObject settingsObj) {
        PreferencesWrapper prefs = new PreferencesWrapper(ctxt);
        prefs.restoreSipSettings(settingsObj);
    }

    /**
     * Restore a sip configuration
     * 
     * @param ctxt
     * @param fileToRestore
     * @return
     */
    public static boolean restoreSipConfiguration(Context ctxt, File fileToRestore) {
        if (fileToRestore == null || !fileToRestore.isFile()) {
            return false;
        }

        StringBuffer contentBuf = new StringBuffer();
        
        try {
            BufferedReader buf;
            String line;
            FileReader fr = new FileReader(fileToRestore);
            buf = new BufferedReader(fr);
            while ((line = buf.readLine()) != null) {
                contentBuf.append(line);
            }
            fr.close();
        } catch (FileNotFoundException e) {
            Log.e(THIS_FILE, "Error while restoring", e);
        } catch (IOException e) {
            Log.e(THIS_FILE, "Error while restoring", e);
        }

        JSONArray accounts = null;
        JSONObject settings = null;
        // Parse json if some string here
        if (contentBuf.length() > 0) {
            try {
                JSONObject mainJSONObject = new JSONObject(contentBuf.toString());
                // Retrieve accounts
                accounts = mainJSONObject.getJSONArray(KEY_ACCOUNTS);
                // Retrieve settings
                settings = mainJSONObject.getJSONObject(KEY_SETTINGS);

            } catch (JSONException e) {
                Log.e(THIS_FILE, "Error while parsing saved file", e);
            }
        } else {
            return false;
        }

        if (accounts != null && accounts.length() > 0) {
            ContentResolver cr = ctxt.getContentResolver();
            // Clear old existing accounts
            cr.delete(SipProfile.ACCOUNT_URI, "1", null);
            cr.delete(SipManager.FILTER_URI, "1", null);

            // Add each accounts
            for (int i = 0; i < accounts.length(); i++) {
                try {
                    JSONObject account = accounts.getJSONObject(i);
                    restoreSipProfile(account, cr);
                } catch (JSONException e) {
                    Log.e(THIS_FILE, "Unable to parse item " + i, e);
                }
            }
        }

        if (settings != null) {
            restoreSipSettings(ctxt, settings);
            return true;
        }

        return false;
    }
}
