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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

import com.csipsimple.api.SipManager;
import com.csipsimple.pjsip.NativeLibManager;
import com.csipsimple.pjsip.PjSipService;

public class ExtraCodecs {

	public static class DynCodecInfos {
		public String libraryPath;
		public String factoryInitFunction;
		
		public DynCodecInfos(Context ctxt, ComponentName cmp) throws NameNotFoundException {
			PackageManager pm = ctxt.getPackageManager();
			ActivityInfo infos = pm.getReceiverInfo(cmp, PackageManager.GET_META_DATA);
			factoryInitFunction = infos.metaData.getString(SipManager.META_LIB_INIT_FACTORY);
			
			String libName = infos.metaData.getString(SipManager.META_LIB_NAME);
			
			PackageInfo pInfos = pm.getPackageInfo(cmp.getPackageName(), PackageManager.GET_SHARED_LIBRARY_FILES);
			// TODO : for now only api-9 compatible
			File libFile = NativeLibManager.getLibFileFromPackage(pInfos.applicationInfo, libName, true);
			if(libFile != null) {
				libraryPath = libFile.getAbsolutePath(); 
			}
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("File : ");
			sb.append(libraryPath);
			sb.append("/");
			sb.append(factoryInitFunction);
			return super.toString();
		}
	}


	private static final String THIS_FILE = "ExtraCodecs";
	
	
	private static Map<String, DynCodecInfos> AVAILABLE_DYN_CODECS = null;
	
	public static Map<String, DynCodecInfos> getDynCodecs(Context ctxt){
		if(AVAILABLE_DYN_CODECS == null) {
			AVAILABLE_DYN_CODECS = new HashMap<String, DynCodecInfos>();
			
			PackageManager packageManager = ctxt.getPackageManager();
			Intent it = new Intent(SipManager.ACTION_GET_EXTRA_CODECS);
			
			List<ResolveInfo> availables = packageManager.queryBroadcastReceivers(it, 0);
			for(ResolveInfo resInfo : availables) {
				ActivityInfo actInfos = resInfo.activityInfo;
				if( packageManager.checkPermission(SipManager.PERMISSION_CONFIGURE_SIP, actInfos.packageName) == PackageManager.PERMISSION_GRANTED) {
					ComponentName cmp = new ComponentName(actInfos.packageName, actInfos.name);
					DynCodecInfos dynInfos;
					try {
						dynInfos = new DynCodecInfos(ctxt, cmp);
						AVAILABLE_DYN_CODECS.put(cmp.flattenToString(), dynInfos);
					} catch (NameNotFoundException e) {
						Log.e(THIS_FILE, "Error while retrieving infos from dyn codec ", e);
					}
				}
			}
		}
		return AVAILABLE_DYN_CODECS;
	}
	
	public static void clearDynCodecs() {
		AVAILABLE_DYN_CODECS = null;
		PjSipService.resetCodecs();
	}
}
