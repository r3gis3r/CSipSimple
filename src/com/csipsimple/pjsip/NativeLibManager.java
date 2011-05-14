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
package com.csipsimple.pjsip;

import java.io.File;
import java.lang.reflect.Field;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.csipsimple.service.DownloadLibService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class NativeLibManager {

	private static final String THIS_FILE = "NativeLibMgr";

	public static final String STACK_FILE_NAME = "libpjsipjni.so";
	
	public static boolean USE_BUNDLE = true;
	
	/**
	 * Get the guessed stack for market version
	 * @param ctx Context you are running in
	 * @return File pointing to the stack file
	 */
	public static File getDownloadableStackLibFile(Context ctx) {
		return ctx.getFileStreamPath(STACK_FILE_NAME);
	}
	
	private static File getBundledStackLibFile(Context ctx) {
		// To keep backward compatibility with android 3, do not use the easy way
		if(Compatibility.isCompatible(9)) {
			PackageInfo packageInfo = PreferencesWrapper.getCurrentPackageInfos(ctx);
			if(packageInfo != null) {
				
				ApplicationInfo appInfo = packageInfo.applicationInfo;
				try {
					Field f = ApplicationInfo.class.getField("nativeLibraryDir");
					File nativeFile = new File((String) f.get(appInfo), "libpjsipjni.so");
					if(nativeFile.exists()) {
						Log.d(THIS_FILE, "Found native lib using clean way");
						return nativeFile;
					}
				} catch (Exception e) {
					Log.e(THIS_FILE, "Cant get field for native lib dir", e);
				}
			}
		}
		// This is the fallback method
		return new File(ctx.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
	}
	
	/**
	 * Get the native library file First search in local files of the app
	 * (previously downloaded from the network) Then search in lib (bundlized
	 * method)
	 * 
	 * @param context
	 *            the context of the app that what to get it back
	 * @return the file if any, null else
	 */
	public static File getStackLibFile(Context context) {
		
		if(USE_BUNDLE) {
			return getBundledStackLibFile(context);
		}
		
		// For now this code is now dead code.
		
		// Standard case
		File standardOut = getDownloadableStackLibFile(context);
		//If production .so file exists and app is not in debuggable mode 
		//if debuggable we have to get the file from bundle dir
		if (standardOut.exists() && !isDebuggableApp(context)) {
			return standardOut;
		}

		// Have a look if it's not a dev build
		File targetForBuild = getBundledStackLibFile(context);
		Log.d(THIS_FILE, "Search for " + targetForBuild.getAbsolutePath());
		if (targetForBuild.exists()) {
			return targetForBuild;
		}

		//Oups none exists.... reset version history
		PreferencesWrapper prefs = new PreferencesWrapper(context);
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_VERSION, "0.00-00");
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_ID, "");
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_URI, "");
		return null;
		
	}

	public static boolean hasBundleStack(Context ctx) {
		File targetForBuild = getBundledStackLibFile(ctx);
		Log.d(THIS_FILE, "Search for " + targetForBuild.getAbsolutePath());
		return targetForBuild.exists();
	}

	public static boolean hasStackLibFile(Context ctx) {
		File guessedFile = getStackLibFile(ctx);
		if (guessedFile == null) {
			return false;
		}
		return guessedFile.exists();
	}

	
	public static boolean isDebuggableApp(Context ctx) {
		try {
			PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			return ( (pinfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (NameNotFoundException e) {
			// Should not happen....or something is wrong with android...
			Log.e(THIS_FILE, "Not possible to find self name", e);
		}
		return false;
	}
	
	
	public static void cleanStack(Context ctx) {
		File file = getDownloadableStackLibFile(ctx);
		if(file.exists()) {
			file.delete();
		}
	}

}
