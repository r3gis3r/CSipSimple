/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.csipsimple.R;
import com.csipsimple.service.DeviceStateReceiver;
import com.csipsimple.service.Downloader;

public class NightlyUpdater {

	private static final String THIS_FILE = "NightlyUpdater";
	
	public static final String LAST_NIGHTLY_CHECK = "nightly_check_date";
	public static final String IGNORE_NIGHTLY_CHECK = "nightly_check_ignore";
	private static final String DOWNLOADED_VERSION = "dl_version";

	private Context context;
	private SharedPreferences prefs;
	private PackageInfo pinfo;

	public NightlyUpdater(Context ctxt) {
		prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		context = ctxt;
		pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(context);
	}
	
	private final static String BASE_UPDATE_VERSION = "http://nightlies.csipsimple.com/trunk/";
	
	private int getLastOnlineVersion() {
		try {
			URL url = new URL(BASE_UPDATE_VERSION + "CSipSimple-latest-trunk.version");
			InputStream content = (InputStream) url.getContent();
			if(content != null) {
				BufferedReader r = new BufferedReader(new InputStreamReader(content));
				String versionString = r.readLine();
				return Integer.parseInt(versionString);
			}
		} catch (MalformedURLException e) {
			Log.e(THIS_FILE, "Invalid nightly build url", e);
		} catch (IOException e) {
			Log.e(THIS_FILE, "Can't get nightly latest value", e);
		} catch (NumberFormatException e) {
			Log.e(THIS_FILE, "Invalid number format", e);
		}
		return 0;
	}
	
	public boolean ignoreCheckByUser() {
		return prefs.getBoolean(IGNORE_NIGHTLY_CHECK, false);
	}
	
	public long lastCheck() {
		return prefs.getLong(LAST_NIGHTLY_CHECK, (long) 0);
	}
	
	public UpdaterPopupLauncher getUpdaterPopup(boolean fallbackAlert) {
		UpdaterPopupLauncher popLauncher = null;
		Editor edt = prefs.edit();
		int onlineVersion = getLastOnlineVersion();
		// Reset ignore check value
		edt.putBoolean(IGNORE_NIGHTLY_CHECK, false);
		if(pinfo != null && pinfo.versionCode < onlineVersion) {
			popLauncher = new UpdaterPopupLauncher(context, onlineVersion);
		}else {
			// Set last check to now :)
			edt.putLong(LAST_NIGHTLY_CHECK, System.currentTimeMillis());
			// And delete latest nightly from cache
			File cachedFile = getCachedFile();
			if(cachedFile.exists()) {
				cachedFile.delete();
			}
			if(fallbackAlert) {
				popLauncher = new UpdaterPopupLauncher(context, 0);
			}
		}
		edt.commit();
		
		return popLauncher;
	}
	
	private File getCachedFile() {
		return new File(context.getCacheDir(), "CSipSimple-latest-trunk.apk");
	}
	
	public class UpdaterPopupLauncher implements Runnable {
		
		private Context context;
		private int version = 0;
		
		public UpdaterPopupLauncher(Context ctxt, int onlineVersion) {
			context = ctxt;
			version = onlineVersion;
		}
		
		@Override
		public void run() {
			Builder ab = new AlertDialog.Builder(context);
			
			
			ab.setIcon(R.drawable.ic_launcher_nightly)
				.setTitle("Update nightly build");
			
			if(version > 0) {
				ab.setMessage("Revision " + version + " available. Upgrade now?")
					.setPositiveButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							Intent it = new Intent();
							it.setData(Uri.parse(BASE_UPDATE_VERSION + "CSipSimple-r"+version+"-trunk.apk"));
							it.setClass(context, Downloader.class);
							it.putExtra(Downloader.EXTRA_ICON, R.drawable.ic_launcher_nightly);
							it.putExtra(Downloader.EXTRA_TITLE, "CSipSimple nightly build");
							it.putExtra(Downloader.EXTRA_CHECK_MD5, true);
							it.putExtra(Downloader.EXTRA_OUTPATH, getCachedFile().getAbsolutePath());
							
							Intent resultIntent = new Intent(context, DeviceStateReceiver.class);
							resultIntent.setAction(DeviceStateReceiver.APPLY_NIGHTLY_UPLOAD);
							resultIntent.putExtra(DOWNLOADED_VERSION, version);
							PendingIntent pi = PendingIntent.getBroadcast(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
							it.putExtra(Downloader.EXTRA_PENDING_FINISH_INTENT, pi);
							context.startService(it);
						}
					})
					.setNegativeButton(R.string.cancel, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Editor edt = prefs.edit();
							edt.putBoolean(IGNORE_NIGHTLY_CHECK, true);
							edt.commit();
							dialog.dismiss();
						}
					});
			}else {
				ab.setMessage("No update available")
					.setPositiveButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			}
			
			ab.create().show();
		}
	}

	public void applyUpdate(Intent i) {
		File f = getCachedFile();
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
    	intent.setDataAndType(Uri.fromFile(f.getAbsoluteFile()), "application/vnd.android.package-archive");
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	context.startActivity(intent);
	}
}
