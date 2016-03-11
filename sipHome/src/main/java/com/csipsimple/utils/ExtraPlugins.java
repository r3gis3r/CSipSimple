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
import android.os.Bundle;

import com.csipsimple.api.SipManager;
import com.csipsimple.pjsip.NativeLibManager;
import com.csipsimple.pjsip.PjSipService;

public class ExtraPlugins {
    private static final String THIS_FILE = "ExtraPlugins";

    /**
     * Class that stores and help to build infos from a remote plugin application
     * that holds a native library extension
     */
	public static class DynCodecInfos {
	    /**
	     * Path of the native library .so file corresponding
	     */
		public String libraryPath;
		/**
		 * The function to call inside the native library to init this plugin
		 * The params depends of the kind of plugin it is
		 */
		public String factoryInitFunction;
		
		/**
		 * The function to call inside the native library to deinit this plugin
		 * The params depends of the kind of plugin it is
		 */
		public String factoryDeinitFunction;
		
		/**
		 * Build codec infos based on a component name of a manifest
		 * It will retrieve {@link SipManager#META_LIB_NAME} and {@link SipManager#META_LIB_INIT_FACTORY} from the meta datas of the component
		 * @param ctxt The current application context
		 * @param cmp The component name of the remote plugin application
		 * @throws NameNotFoundException If the remote component is not found
		 */
		public DynCodecInfos(Context ctxt, ComponentName cmp) throws NameNotFoundException {
			PackageManager pm = ctxt.getPackageManager();
			ActivityInfo infos = pm.getReceiverInfo(cmp, PackageManager.GET_META_DATA);
			factoryInitFunction = infos.metaData.getString(SipManager.META_LIB_INIT_FACTORY);
            factoryDeinitFunction = infos.metaData.getString(SipManager.META_LIB_DEINIT_FACTORY);
			
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


	
	private static Map<String, Map<String, DynCodecInfos>> CACHED_RESOLUTION = new HashMap<String, Map<String, DynCodecInfos>>();

    /**
     * Reset all dynamic plugins infos
     */
    public static void clearDynPlugins() {
        CACHED_RESOLUTION.clear();
        CACHED_ACTIVITY_RESOLUTION.clear();
        PjSipService.resetCodecs();
    }
    

    /**
     * Retrieve all dynamic codec plugins available in the platform
     * It will resolve for a given action available sip plugins
     * @param ctxt Context of the application
     * @param action Action of the plugin to be resolved
     * For example {@link SipManager#ACTION_GET_EXTRA_CODECS}, {@link SipManager.ACTION_GET_VIDEO_PLUGIN}
     * @return a map containing plugins infos and registrered component name as key
     */
    public static Map<String, DynCodecInfos> getDynCodecPlugins(Context ctxt, String action){
        if(!CACHED_RESOLUTION.containsKey(action)) {
            HashMap<String, DynCodecInfos> plugins = new HashMap<String, DynCodecInfos>();
            
            PackageManager packageManager = ctxt.getPackageManager();
            Intent it = new Intent(action);
            
            List<ResolveInfo> availables = packageManager.queryBroadcastReceivers(it, 0);
            for(ResolveInfo resInfo : availables) {
                ActivityInfo actInfos = resInfo.activityInfo;
                if( packageManager.checkPermission(SipManager.PERMISSION_CONFIGURE_SIP, actInfos.packageName) == PackageManager.PERMISSION_GRANTED) {
                    ComponentName cmp = new ComponentName(actInfos.packageName, actInfos.name);
                    DynCodecInfos dynInfos;
                    try {
                        dynInfos = new DynCodecInfos(ctxt, cmp);
                        plugins.put(cmp.flattenToString(), dynInfos);
                    } catch (NameNotFoundException e) {
                        Log.e(THIS_FILE, "Error while retrieving infos from dyn codec ", e);
                    }
                }
            }
            CACHED_RESOLUTION.put(action, plugins);
        }
        
        return CACHED_RESOLUTION.get(action);
    }
    

    
    private static Map<String, Map<String, DynActivityPlugin>> CACHED_ACTIVITY_RESOLUTION = new HashMap<String, Map<String, DynActivityPlugin>>();
    
    /**
     * Part for incall plugin
     */
    
    public static class DynActivityPlugin {
        private final ComponentName cmp;
        private final String name;
        private final String action;
        private final Bundle metaDatas;
        
        public DynActivityPlugin(String name, String action, ComponentName componentName, Bundle meta) {
            this.cmp = componentName;
            this.action = action;
            this.name = name;
            this.metaDatas = meta;
        }
        
        public Intent getIntent() {
            Intent it = new Intent(action);
            it.addCategory(Intent.CATEGORY_EMBED);
            it.setComponent(cmp);
            return it;
        }
        
        public String getMetaDataString(String key, String defaultValue) {
            if(metaDatas == null) {
                return defaultValue;
            }
            String res = metaDatas.getString(key);
            if(res == null) {
                return defaultValue;
            }
            return res;
        }
        
        public Integer getMetaDataInt(String key, int defaultValue) {
            if(metaDatas == null) {
                return defaultValue;
            }
            return metaDatas.getInt(key, defaultValue);
        }

        public CharSequence getName() {
            return name;
        }
    }
    

    public static Map<String, DynActivityPlugin> getDynActivityPlugins(Context ctxt, String action){
        if(!CACHED_ACTIVITY_RESOLUTION.containsKey(action)) {
            HashMap<String, DynActivityPlugin> plugins = new HashMap<String, DynActivityPlugin>();
            
            PackageManager packageManager = ctxt.getPackageManager();
            Intent it = new Intent(action);
            
            List<ResolveInfo> availables = packageManager.queryIntentActivities(it, 0);
            for(ResolveInfo resInfo : availables) {
                ActivityInfo actInfos = resInfo.activityInfo;
                if( packageManager.checkPermission(SipManager.PERMISSION_USE_SIP, actInfos.packageName) == PackageManager.PERMISSION_GRANTED) {
                    ComponentName cmp = new ComponentName(actInfos.packageName, actInfos.name);
                    DynActivityPlugin dynInfos;
                    dynInfos = new DynActivityPlugin(actInfos.loadLabel(packageManager).toString(), action,
                            cmp, actInfos.metaData);
                    plugins.put(cmp.flattenToString(), dynInfos);
                }
            }
            CACHED_ACTIVITY_RESOLUTION.put(action, plugins);
        }
        
        return CACHED_ACTIVITY_RESOLUTION.get(action);
    }
}
