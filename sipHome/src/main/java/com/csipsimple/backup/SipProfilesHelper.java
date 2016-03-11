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
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@TargetApi(8)
public class SipProfilesHelper implements BackupHelper {

    private static final String THIS_FILE = "SipProfileHelper";

    private static final String ACCOUNTS_BACKUP_KEY = "accounts";

    private final Context mContext;

    private File databaseFile;

    SipProfilesHelper(Context ctxt) {
        mContext = ctxt;
        databaseFile = ctxt.getDatabasePath(SipManager.AUTHORITY);
    }

    /*
     * (non-Javadoc)
     * @see
     * android.app.backup.BackupHelper#performBackup(android.os.ParcelFileDescriptor
     * , android.app.backup.BackupDataOutput, android.os.ParcelFileDescriptor)
     */
    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) {
        boolean forceBackup = (oldState == null);

        long fileModified = databaseFile.lastModified();
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
            JSONArray accountsSaved = SipProfileJson.serializeSipProfiles(mContext);
            try {
                writeData(data, accountsSaved.toString());
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

    /*
     * (non-Javadoc)
     * @see android.app.backup.BackupHelper#restoreEntity(android.app.backup.
     * BackupDataInputStream)
     */
    @Override
    public void restoreEntity(BackupDataInputStream data) {
        if (ACCOUNTS_BACKUP_KEY.equalsIgnoreCase(data.getKey())) {
            try {
                String profilesStr = readData(data);
                JSONArray accounts = new JSONArray(profilesStr);
                if (accounts != null && accounts.length() > 0) {
                    SipProfileJson.restoreSipAccounts(mContext, accounts);
                }
            } catch (IOException e) {
                Log.e(THIS_FILE, "Cannot restore backup entry", e);
            } catch (JSONException e) {
                Log.e(THIS_FILE, "Cannot parse backup entry", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.backup.BackupHelper#writeNewStateDescription(android.os.
     * ParcelFileDescriptor)
     */
    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        long fileModified = databaseFile.lastModified();
        try {
            FileOutputStream outstream = new FileOutputStream(newState.getFileDescriptor());
            DataOutputStream out = new DataOutputStream(outstream);
            out.writeLong(fileModified);
            out.close();
        } catch (IOException e) {
            Log.e(THIS_FILE, "Cannot manage final local backup state", e);
        }
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
        data.writeEntityHeader(ACCOUNTS_BACKUP_KEY, len);
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
