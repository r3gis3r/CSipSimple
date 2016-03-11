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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@TargetApi(8)
public class SipSharedPreferencesHelper implements BackupHelper {
    private static final String THIS_FILE = "SipSharedPreferencesHelper";
    private static final String SETTINGS_BACKUP_KEY = "settings";
    private final Context mContext;
    private final File prefsFiles;
    /**
     * @param context
     * @param prefGroups
     */
    public SipSharedPreferencesHelper(Context context)  {
        mContext = context;
        String sharedPrefsName = context.getPackageName() + "_preferences";
        prefsFiles = getPreferenceFile(context, sharedPrefsName);
    }
    /* (non-Javadoc)
     * @see android.app.backup.BackupHelper#performBackup(android.os.ParcelFileDescriptor, android.app.backup.BackupDataOutput, android.os.ParcelFileDescriptor)
     */
    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        boolean forceBackup = (oldState == null);

        long fileModified = 1;
        if(prefsFiles != null) {
            fileModified = prefsFiles.lastModified();
        }
        try {
            if (!forceBackup) {
                FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
                DataInputStream in = new DataInputStream(instream);
                long lastModified = in.readLong();
                in.close();

                if (lastModified < fileModified) {
                    forceBackup = true;
                }
            }
        } catch (IOException e) {
            Log.e(THIS_FILE, "Cannot manage previous local backup state", e);
            forceBackup = true;
        }

        Log.d(THIS_FILE, "Will backup profiles ? " + forceBackup);
        if (forceBackup) {
            JSONObject settings = SipProfileJson.serializeSipSettings(mContext);
            try {
                writeData(data, settings.toString());
            } catch (IOException e) {
                Log.e(THIS_FILE, "Cannot manage remote backup", e);
            }
        }

        try {
            FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
            DataOutputStream out = new DataOutputStream(outstream);
            out.writeLong(fileModified);
            out.close();
        } catch (IOException e) {
            Log.e(THIS_FILE, "Cannot manage final local backup state", e);
        }
        
    }
    /* (non-Javadoc)
     * @see android.app.backup.BackupHelper#restoreEntity(android.app.backup.BackupDataInputStream)
     */
    @Override
    public void restoreEntity(BackupDataInputStream data) {
        if (SETTINGS_BACKUP_KEY.equalsIgnoreCase(data.getKey())) {
            try {
                String settingsStr = readData(data);
                JSONObject settings = new JSONObject(settingsStr);
                if (settings != null) {
                    SipProfileJson.restoreSipSettings(mContext, settings);
                }
                PreferencesWrapper pw = new PreferencesWrapper(mContext);
                pw.setPreferenceBooleanValue(PreferencesProviderWrapper.HAS_ALREADY_SETUP_SERVICE, true);
                pw.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP, true);
            } catch (IOException e) {
                Log.e(THIS_FILE, "Cannot restore backup entry", e);
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Cannot parse backup entry", e);
            }
        }
    }
    /* (non-Javadoc)
     * @see android.app.backup.BackupHelper#writeNewStateDescription(android.os.ParcelFileDescriptor)
     */
    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        long fileModified = 0;
        if(prefsFiles != null) {
            prefsFiles.lastModified();
        }
        try {
            FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
            DataOutputStream out = new DataOutputStream(outstream);
            out.writeLong(fileModified);
            out.close();
        } catch (IOException e) {
            Log.e(THIS_FILE, "Cannot manage final local backup state", e);
        }
    }
    
    
    @SuppressLint("SdCardPath")
    private File getPreferenceFile(Context context, String prefName) {
        String finalPath = "shared_prefs/" + prefName + ".xml";
        File f = new File(context.getFilesDir(), "../" + finalPath);
        if (f.exists()) {
            return f;
        }
        f = new File("/data/data/" + context.getPackageName() + "/" + finalPath);
        if (f.exists()) {
            return f;
        }
        f = new File("/dbdata/databases/" + context.getPackageName() + "/" + finalPath);
        if (f.exists()) {
            return f;
        }
        return null;
    }

    private void writeData(BackupDataOutput data, String value) throws IOException {
        // Create buffer stream and data output stream for our data
        ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
        DataOutputStream outWriter = new DataOutputStream(bufStream);
        // Write structured data
        outWriter.writeUTF(value);
        // Send the data to the Backup Manager via the BackupDataOutput
        byte[] buffer = bufStream.toByteArray();
        int len = buffer.length;
        data.writeEntityHeader(SETTINGS_BACKUP_KEY, len);
        data.writeEntityData(buffer, len);
    }

    /**
     * Read data from the input stream
     * 
     * @param data the input stream
     * @return the data
     * @throws IOException I/O error
     */
    private String readData(BackupDataInputStream data) throws IOException {
        String dataS;
        byte[] buf = new byte[data.size()];
        data.read(buf, 0, buf.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        dataS = dis.readUTF();
        dis.close();
        bais.close();
        return dataS;
    }
}
