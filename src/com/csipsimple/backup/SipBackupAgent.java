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
 * 
 */

package com.csipsimple.backup;

import android.annotation.TargetApi;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.os.ParcelFileDescriptor;

import com.csipsimple.utils.Log;

import java.io.IOException;

@TargetApi(8)
public class SipBackupAgent extends BackupAgentHelper {

    private static final String THIS_FILE = "SipBackupAgent";
    private static final String KEY_SHARED_PREFS = "shared_prefs";
    private static final String KEY_DATABASES = "databases";

    /*
     * (non-Javadoc)
     * @see android.app.backup.BackupAgent#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(THIS_FILE, "Create backup agent");
        SipSharedPreferencesHelper sharedPrefsHelper = new SipSharedPreferencesHelper(this);
        addHelper(KEY_SHARED_PREFS, sharedPrefsHelper);
        
        SipProfilesHelper profilesHelper = new SipProfilesHelper(this);
        addHelper(KEY_DATABASES, profilesHelper);
    }

    /*
     * (non-Javadoc)
     * @see
     * android.app.backup.BackupAgent#onRestore(android.app.backup.BackupDataInput
     * , int, android.os.ParcelFileDescriptor)
     */
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {
        Log.d(THIS_FILE, "App version code : " + appVersionCode);
        super.onRestore(data, appVersionCode, newState);


    }
}
