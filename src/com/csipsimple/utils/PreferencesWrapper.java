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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.csipsimple.api.SipConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;


public class PreferencesWrapper {
	
	//Internal use
	public static final String HAS_BEEN_QUIT = "has_been_quit";
	public static final String IS_ADVANCED_USER = "is_advanced_user";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";
	public static final String HAS_ALREADY_SETUP_SERVICE = "has_already_setup_service";
    public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
    public static final String LAST_KNOWN_ANDROID_VERSION_PREF = "last_known_aos_version";
	
	
	private static final String THIS_FILE = "PreferencesWrapper";
	private SharedPreferences prefs;
	private ContentResolver resolver;
	private Context context;
    private Editor sharedEditor;

	
	
	public final static HashMap<String, String> STRING_PREFS = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;
	{
		
		put(SipConfigManager.USER_AGENT, CustomDistribution.getUserAgent());
		put(SipConfigManager.LOG_LEVEL, "1");
		
		put(SipConfigManager.USE_SRTP, "0");
		put(SipConfigManager.USE_ZRTP, "1"); /* 1 is no zrtp */
		put(SipConfigManager.UDP_TRANSPORT_PORT, "0");
		put(SipConfigManager.TCP_TRANSPORT_PORT, "0");
		put(SipConfigManager.TLS_TRANSPORT_PORT, "0");
		put(SipConfigManager.KEEP_ALIVE_INTERVAL_WIFI, "80");
		put(SipConfigManager.KEEP_ALIVE_INTERVAL_MOBILE, "40");
        put(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_WIFI, "180");
        put(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_MOBILE, "120");
        put(SipConfigManager.TLS_KEEP_ALIVE_INTERVAL_WIFI, "180");
        put(SipConfigManager.TLS_KEEP_ALIVE_INTERVAL_MOBILE, "120");
		put(SipConfigManager.RTP_PORT, "4000");
		put(SipConfigManager.OVERRIDE_NAMESERVER, "");
		put(SipConfigManager.TIMER_MIN_SE, "90");
		put(SipConfigManager.TIMER_SESS_EXPIRES, "1800");
        put(SipConfigManager.TSX_T1_TIMEOUT, "-1");
        put(SipConfigManager.TSX_T2_TIMEOUT, "-1");
        put(SipConfigManager.TSX_T4_TIMEOUT, "-1");
        put(SipConfigManager.TSX_TD_TIMEOUT, "-1");
		
		put(SipConfigManager.SND_AUTO_CLOSE_TIME, "1");
		put(SipConfigManager.ECHO_CANCELLATION_TAIL, "200");
		put(SipConfigManager.ECHO_MODE, "3"); /* WEBRTC */
		put(SipConfigManager.SND_MEDIA_QUALITY, "4");
		put(SipConfigManager.SND_CLOCK_RATE, "16000");
		put(SipConfigManager.SND_PTIME, "20");
		put(SipConfigManager.SIP_AUDIO_MODE, "0");
		put(SipConfigManager.MICRO_SOURCE, "1");
		put(SipConfigManager.THREAD_COUNT, "0");
		put(SipConfigManager.HEADSET_ACTION, "0");
		put(SipConfigManager.AUDIO_IMPLEMENTATION, "0");
		put(SipConfigManager.H264_PROFILE, "66");
		put(SipConfigManager.H264_LEVEL, "0");
        put(SipConfigManager.H264_BITRATE, "0");
        put(SipConfigManager.VIDEO_CAPTURE_SIZE, "");
		
		put(SipConfigManager.STUN_SERVER, "stun.counterpath.com");
		put(SipConfigManager.TURN_SERVER, "");
		put(SipConfigManager.TURN_USERNAME, "");
		put(SipConfigManager.TURN_PASSWORD, "");
		put(SipConfigManager.TLS_SERVER_NAME, "");
		put(SipConfigManager.CA_LIST_FILE, "");
		put(SipConfigManager.CERT_FILE, "");
		put(SipConfigManager.PRIVKEY_FILE, "");
		put(SipConfigManager.TLS_PASSWORD, "");
		put(SipConfigManager.TLS_METHOD, "0");
		put(SipConfigManager.NETWORK_ROUTES_POLLING, "0");
		
		put(SipConfigManager.DSCP_VAL, "26");
		put(SipConfigManager.DTMF_MODE, "0");
        put(SipConfigManager.DTMF_PAUSE_TIME, "300");
        put(SipConfigManager.DTMF_WAIT_TIME, "2000");
		

		put(SipConfigManager.GSM_INTEGRATION_TYPE, Integer.toString(SipConfigManager.GENERIC_TYPE_PREVENT));
		put(SipConfigManager.DIAL_PRESS_TONE_MODE, Integer.toString(SipConfigManager.GENERIC_TYPE_AUTO));
		put(SipConfigManager.DIAL_PRESS_VIBRATE_MODE, Integer.toString(SipConfigManager.GENERIC_TYPE_AUTO));
        put(SipConfigManager.DTMF_PRESS_TONE_MODE, Integer.toString(SipConfigManager.GENERIC_TYPE_PREVENT));
		
		put(SipConfigManager.DEFAULT_CALLER_ID, "");
		put(SipConfigManager.THEME, "");
		put(SipConfigManager.RINGTONE, "");
		
		
	}};
	
	
	private final static HashMap<String, Boolean> BOOLEAN_PREFS = new HashMap<String, Boolean>(){
		private static final long serialVersionUID = 1L;
	{
		//Network
		put(SipConfigManager.LOCK_WIFI, true);
		put(SipConfigManager.LOCK_WIFI_PERFS, false);
		put(SipConfigManager.ENABLE_TCP, true);
		put(SipConfigManager.ENABLE_UDP, true);
		put(SipConfigManager.ENABLE_TLS, false);
		put(SipConfigManager.USE_IPV6, false);
		put(SipConfigManager.ENABLE_DNS_SRV, false);
		put(SipConfigManager.ENABLE_ICE, false);
		put(SipConfigManager.ENABLE_TURN, false);
		put(SipConfigManager.ENABLE_STUN, false);
        put(SipConfigManager.ENABLE_STUN2, false);
		put(SipConfigManager.ENABLE_QOS, false);
		put(SipConfigManager.USE_COMPACT_FORM, false);
		put(SipConfigManager.USE_WIFI_IN, true);
		put(SipConfigManager.USE_WIFI_OUT, true);
		put(SipConfigManager.USE_OTHER_IN, true);
		put(SipConfigManager.USE_OTHER_OUT, true);
		put(SipConfigManager.USE_3G_IN, false);
		put(SipConfigManager.USE_3G_OUT, false);
		put(SipConfigManager.USE_GPRS_IN, false);
		put(SipConfigManager.USE_GPRS_OUT, false);
		put(SipConfigManager.USE_EDGE_IN, false);
		put(SipConfigManager.USE_EDGE_OUT, false);
        put(SipConfigManager.USE_ANYWAY_IN, false);
        put(SipConfigManager.USE_ANYWAY_OUT, false);
		put(SipConfigManager.FORCE_NO_UPDATE, true);
        put(SipConfigManager.DISABLE_TCP_SWITCH, true);
        put(SipConfigManager.DISABLE_RPORT, false);
        put(SipConfigManager.ADD_BANDWIDTH_TIAS_IN_SDP, false);
		
		
		//Media
		put(SipConfigManager.ECHO_CANCELLATION, true);
		put(SipConfigManager.ENABLE_VAD, false);
        put(SipConfigManager.ENABLE_NOISE_SUPPRESSION, false);
		put(SipConfigManager.USE_SOFT_VOLUME, false);
		put(SipConfigManager.USE_ROUTING_API, false);
		put(SipConfigManager.USE_MODE_API, false);
		put(SipConfigManager.HAS_IO_QUEUE, false);
		put(SipConfigManager.SET_AUDIO_GENERATE_TONE, false);
		put(SipConfigManager.USE_SGS_CALL_HACK, false);
		put(SipConfigManager.USE_WEBRTC_HACK, false);
		put(SipConfigManager.DO_FOCUS_AUDIO, true);
		put(SipConfigManager.INTEGRATE_WITH_NATIVE_MUSIC, true);
		put(SipConfigManager.AUTO_CONNECT_BLUETOOTH, false);
        put(SipConfigManager.AUTO_CONNECT_SPEAKER, false);
        put(SipConfigManager.AUTO_DETECT_SPEAKER, false);
		put(SipConfigManager.CODECS_PER_BANDWIDTH, true);
		put(SipConfigManager.RESTART_AUDIO_ON_ROUTING_CHANGES, true);
        put(SipConfigManager.SETUP_AUDIO_BEFORE_INIT, true);
		
		//UI
		put(SipConfigManager.PREVENT_SCREEN_ROTATION, true);
		put(SipConfigManager.KEEP_AWAKE_IN_CALL, false);
		put(SipConfigManager.INVERT_PROXIMITY_SENSOR, false);
		put(SipConfigManager.ICON_IN_STATUS_BAR, true);
		put(SipConfigManager.USE_PARTIAL_WAKE_LOCK, false);
		put(SipConfigManager.ICON_IN_STATUS_BAR_NBR, false);
		put(SipConfigManager.INTEGRATE_WITH_CALLLOGS, true);
		put(SipConfigManager.INTEGRATE_WITH_DIALER, true);
		put(SipConfigManager.INTEGRATE_TEL_PRIVILEGED, false);
		put(SipConfigManager.USE_ALTERNATE_UNLOCKER, false);
		put(HAS_BEEN_QUIT, false);
		put(HAS_ALREADY_SETUP_SERVICE, false);
		put(SipConfigManager.LOG_USE_DIRECT_FILE, false);
		put(SipConfigManager.START_WITH_TEXT_DIALER, false);
		
		//Calls
		put(SipConfigManager.AUTO_RECORD_CALLS, false);
		put(SipConfigManager.SUPPORT_MULTIPLE_CALLS, false);
        put(SipConfigManager.USE_VIDEO, false);
		
		//Secure
		put(SipConfigManager.TLS_VERIFY_SERVER, false);
		put(SipConfigManager.TLS_VERIFY_CLIENT, false);
		
	}};
	
	private final static HashMap<String, Float> FLOAT_PREFS = new HashMap<String, Float>(){
		private static final long serialVersionUID = 1L;
	{
		put(SipConfigManager.SND_MIC_LEVEL, (float)1.0);
		put(SipConfigManager.SND_SPEAKER_LEVEL, (float)1.0);
		put(SipConfigManager.SND_BT_MIC_LEVEL, (float)1.0);
		put(SipConfigManager.SND_BT_SPEAKER_LEVEL, (float)1.0);
		put(SipConfigManager.SND_STREAM_LEVEL, (float)8.0);
	}};
	
	private static boolean HAS_MANAGED_VERSION_UPGRADE = false;
	
	public PreferencesWrapper(Context aContext) {
		context = aContext;
		prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
		resolver = aContext.getContentResolver();
		
		// Check if we need an upgrade here
		// BUNDLE MODE -- upgrade settings
		if(!HAS_MANAGED_VERSION_UPGRADE) {
            Integer runningVersion = needUpgrade();
            if (runningVersion != null) {
                Editor editor = prefs.edit();
                editor.putInt(LAST_KNOWN_VERSION_PREF, runningVersion);
                editor.commit();
            }
            HAS_MANAGED_VERSION_UPGRADE = true;
		}
	}

    /**
     * Check wether an upgrade is needed
     * 
     * @return null if not needed, else the new version to upgrade to
     */
    private Integer needUpgrade() {
        Integer runningVersion = null;
        // Application upgrade
        PackageInfo pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(context);
        if (pinfo != null) {
            runningVersion = pinfo.versionCode;
            int lastSeenVersion = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);

            Log.d(THIS_FILE, "Last known version is " + lastSeenVersion
                    + " and currently we are running " + runningVersion);
            if (lastSeenVersion != runningVersion) {
                Compatibility.updateVersion(this, lastSeenVersion, runningVersion);
            } else {
                runningVersion = null;
            }
        }

        // Android upgrade
        if(prefs != null){
            int lastSeenVersion = prefs.getInt(LAST_KNOWN_ANDROID_VERSION_PREF, 0);
            Log.d(THIS_FILE, "Last known android version " + lastSeenVersion);
            if (lastSeenVersion != Compatibility.getApiLevel()) {
                Compatibility.updateApiVersion(this, lastSeenVersion,
                        Compatibility.getApiLevel());
                Editor editor = prefs.edit();
                editor.putInt(LAST_KNOWN_ANDROID_VERSION_PREF, Compatibility.getApiLevel());
                editor.commit();
            }
        }
        return runningVersion;
    }
	
	
	/**
	 * Enter in edit mode
	 * To use for bulk modifications
	 */
	public void startEditing() {
	    sharedEditor = prefs.edit();
	}
	
	/**
	 * Leave edit mode
	 */
	public void endEditing() {
	    if(sharedEditor != null) {
	        sharedEditor.commit();
	        sharedEditor = null;
	    }
	}

	//Public setters
	/**
	 * Set a preference string value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceStringValue(String key, String value) {
	    if(sharedEditor == null) {
    		Editor editor = prefs.edit();
    		editor.putString(key, value);
    		editor.commit();
	    }else {
	        sharedEditor.putString(key, value);
	    }
	}
	
	/**
	 * Set a preference boolean value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceBooleanValue(String key, boolean value) {
	    if(sharedEditor == null) {
    		Editor editor = prefs.edit();
    		editor.putBoolean(key, value);
    		editor.commit();
	    }else {
	        sharedEditor.putBoolean(key, value);
	    }
	}
	
	/**
	 * Set a preference float value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceFloatValue(String key, float value) {
	    if(sharedEditor == null) {
    		Editor editor = prefs.edit();
    		editor.putFloat(key, value);
    		editor.commit();
	    }else {
	        sharedEditor.putFloat(key, value);
	    }
	}
	
	//Private static getters
	// For string
	private static String gPrefStringValue(SharedPreferences aPrefs, String key) {
	    if(aPrefs == null) {
	        return STRING_PREFS.get(key);
	    }
		if(STRING_PREFS.containsKey(key)) {
			return aPrefs.getString(key, STRING_PREFS.get(key));
		}
		return aPrefs.getString(key, (String) null);
	}
	
	// For boolean
	private static Boolean gPrefBooleanValue(SharedPreferences aPrefs, String key) {
	    if(aPrefs == null) {
	        return BOOLEAN_PREFS.get(key);
	    }
		if(BOOLEAN_PREFS.containsKey(key)) {
			return aPrefs.getBoolean(key, BOOLEAN_PREFS.get(key));
		}
		if(aPrefs.contains(key)) {
		    return aPrefs.getBoolean(key, false);
		}
		return null;
	}
	
	// For float
	private static Float gPrefFloatValue(SharedPreferences aPrefs, String key) {
	    if(aPrefs == null) {
	        return FLOAT_PREFS.get(key);
	    }
		if(FLOAT_PREFS.containsKey(key)) {
			return aPrefs.getFloat(key, FLOAT_PREFS.get(key));
		}
		if(aPrefs.contains(key)) {
		    return aPrefs.getFloat(key, 0.0f); 
		}
		return null;
	}
	
	public static Class<?> gPrefClass(String key) {
		if(STRING_PREFS.containsKey(key)) {
			return String.class;
		}else if(BOOLEAN_PREFS.containsKey(key)) {
			return Boolean.class;
		}else if(FLOAT_PREFS.containsKey(key)) {
			return Float.class;
		}
		return null;
	}
	
	/**
	 * Get string preference value
	 * @param key the key preference to retrieve
	 * @return the value
	 */
	public String getPreferenceStringValue(String key) {
		return gPrefStringValue(prefs, key);
	}
	
	/**
	 * Get boolean preference value
	 * @param key the key preference to retrieve
	 * @return the value
	 */
	public Boolean getPreferenceBooleanValue(String key) {
		return gPrefBooleanValue(prefs, key);
	}
	
	/**
	 * Get float preference value
	 * @param key the key preference to retrieve
	 * @return the value
	 */
	public Float getPreferenceFloatValue(String key) {
		return gPrefFloatValue(prefs, key);
	}
	
	/**
	 * Get integer preference value
	 * @param key the key preference to retrieve
	 * @return the value
	 */
	public Integer getPreferenceIntegerValue(String key) {
		try {
			return Integer.parseInt(getPreferenceStringValue(key));
		}catch(NumberFormatException e) {
			Log.d(THIS_FILE, "Invalid "+key+" format : expect a int");
		}
		String val = STRING_PREFS.get(key);
		if(val != null) {
		    return Integer.parseInt(val);
		}
		return null;
	}
	
	/**
	 * Set all values to default
	 */
	public void resetAllDefaultValues() {
		for(String key : STRING_PREFS.keySet() ) {
			setPreferenceStringValue(key, STRING_PREFS.get(key));
		}
		for(String key : BOOLEAN_PREFS.keySet() ) {
			setPreferenceBooleanValue(key, BOOLEAN_PREFS.get(key));
		}
		for(String key : FLOAT_PREFS.keySet() ) {
			setPreferenceFloatValue(key, FLOAT_PREFS.get(key));
		}
		Compatibility.setFirstRunParameters(this);
		setPreferenceBooleanValue(PreferencesProviderWrapper.HAS_ALREADY_SETUP_SERVICE, true);
	}
	
	
	public JSONObject serializeSipSettings() {
		JSONObject jsonSipSettings = new JSONObject();
		for(String key : STRING_PREFS.keySet() ) {
			try {
				jsonSipSettings.put(key, getPreferenceStringValue(key));
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to add preference "+key);
			}
		}
		for(String key : BOOLEAN_PREFS.keySet() ) {
			try {
				jsonSipSettings.put(key, getPreferenceBooleanValue(key));
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to add preference "+key);
			}
		}
		for(String key : FLOAT_PREFS.keySet() ) {
			try {
				jsonSipSettings.put(key, getPreferenceFloatValue(key).doubleValue());
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to add preference "+key);
			}
		}
		
		
		// And get latest known version so that restore will be able to apply necessary patches
		int lastSeenVersion = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);
		try {
			jsonSipSettings.put(LAST_KNOWN_VERSION_PREF, lastSeenVersion);
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Not able to add last known version pref");
		}
		return jsonSipSettings;
	}
	
	/**
	 * Restore settings from a json object
	 * @param jsonSipSettings the json objet to restore from
	 */
	public void restoreSipSettings(JSONObject jsonSipSettings) {
	    
	    startEditing();
		for(String key : STRING_PREFS.keySet() ) {
			try {
				String val = jsonSipSettings.getString(key);
				if(val != null) {
					setPreferenceStringValue(key, val);
				}
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to get preference "+key);
			}
		}
		for(String key : BOOLEAN_PREFS.keySet() ) {
			try {
				Boolean val = jsonSipSettings.getBoolean(key);
				if(val != null) {
					setPreferenceBooleanValue(key, val);
				}
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to get preference "+key);
			}
		}
		for(String key : FLOAT_PREFS.keySet() ) {
			try {
				Double val = jsonSipSettings.getDouble(key);
				if(val != null) {
					setPreferenceFloatValue(key, val.floatValue());
				}
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Not able to get preference "+key);
			}
			
			getPreferenceFloatValue(key);
		}
		
		// And get latest known version so that restore will be able to apply necessary patches
		try {
			Integer lastSeenVersion = jsonSipSettings.getInt(LAST_KNOWN_VERSION_PREF);
			if(lastSeenVersion != null) {
			    sharedEditor.putInt(LAST_KNOWN_VERSION_PREF, lastSeenVersion);
			}
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Not able to add last known version pref");
		}
		
		endEditing();
	}
	
	
	
	private boolean hasStunServer(String string) {
		String[] servers = getPreferenceStringValue(SipConfigManager.STUN_SERVER).split(",");
		for(String server : servers) {
			if(server.equalsIgnoreCase(string)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void addStunServer(String server) {
		if(!hasStunServer(server)) {
			String oldStuns = getPreferenceStringValue(SipConfigManager.STUN_SERVER);
			Log.d(THIS_FILE, "Old stun > "+oldStuns+" vs "+STRING_PREFS.get(SipConfigManager.STUN_SERVER));
			if(oldStuns.equalsIgnoreCase(STRING_PREFS.get(SipConfigManager.STUN_SERVER))) {
				oldStuns = "";
			}else {
				oldStuns += ",";
			}
			
			setPreferenceStringValue(SipConfigManager.STUN_SERVER, oldStuns + server);
		}
		
	}
	
	// Codec
	public short getCodecPriority(String codecName, String type, String defaultValue) {
		String key = SipConfigManager.getCodecKey(codecName, type); 
		if(key != null) {
		    try {
		        return (short) Integer.parseInt(prefs.getString(key, defaultValue));
		    }catch(NumberFormatException e) {
		        Log.e(THIS_FILE, "Invalid codec priority", e);
		    }
		}
		return (short) Integer.parseInt(defaultValue);
	}
	
	public void setCodecPriority(String codecName, String type, String newValue) {
		String key = SipConfigManager.getCodecKey(codecName, type); 
		if(key != null) {
			setPreferenceStringValue(key, newValue);
		}
		//TODO : else raise error
	}
	

	// ---- 
	// UI related
	// ----
	public boolean dialPressTone(boolean inCall) {
		Integer mode = getPreferenceIntegerValue(inCall ? SipConfigManager.DTMF_PRESS_TONE_MODE : SipConfigManager.DIAL_PRESS_TONE_MODE);
		if(mode == null) {
		    mode = inCall ? SipConfigManager.GENERIC_TYPE_PREVENT : SipConfigManager.GENERIC_TYPE_AUTO; 
		}
		switch (mode) {
		case SipConfigManager.GENERIC_TYPE_AUTO:
			return Settings.System.getInt(resolver,
	                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
		case SipConfigManager.GENERIC_TYPE_FORCE:
			return true;
		case SipConfigManager.GENERIC_TYPE_PREVENT:
			return false;
		default:
			break;
		}
		return false;
	}

	public boolean dialPressVibrate() {
		int mode = getPreferenceIntegerValue(SipConfigManager.DIAL_PRESS_VIBRATE_MODE);
		switch (mode) {
		case SipConfigManager.GENERIC_TYPE_AUTO:
			return Settings.System.getInt(resolver,
	                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
		case SipConfigManager.GENERIC_TYPE_FORCE:
			return true;
		case SipConfigManager.GENERIC_TYPE_PREVENT:
			return false;
		default:
			break;
		}
		return false;
	}

	private final static String CONFIG_FOLDER = "configs";
	private final static String RECORDS_FOLDER = "records";
	private final static String LOGS_FOLDER = "logs";
	/*private final static String ZRTP_FOLDER = "zrtp";*/
	 
	private static File getStorageFolder(Context ctxt, boolean preferCache) {
		File root = Environment.getExternalStorageDirectory();
		if(!root.canWrite() || preferCache) {
			root = ctxt.getCacheDir();
		}
		
	    if (root.canWrite()){
			File dir = new File(root.getAbsolutePath() + File.separator + CustomDistribution.getSDCardFolder());
			if(!dir.exists()) {
				dir.mkdirs();
				Log.d(THIS_FILE, "Create directory " + dir.getAbsolutePath());
			}
			return dir;
	    }
	    return null;
	}
	
	
	private static File getSubFolder(Context ctxt, String subFolder, boolean preferCache) {
		File root = getStorageFolder(ctxt, preferCache);
		if(root != null) {
			File dir = new File(root.getAbsoluteFile() + File.separator + subFolder);
			dir.mkdirs();
			return dir;
		}
		return null;
	}
	
	public static File getConfigFolder(Context ctxt) {
		return getSubFolder(ctxt, CONFIG_FOLDER, false);
	}
	
	public static File getRecordsFolder(Context ctxt) {
		return getSubFolder(ctxt, RECORDS_FOLDER, false);
	}
	
	public static File getLogsFolder(Context ctxt) {
		return getSubFolder(ctxt, LOGS_FOLDER, false);
	}
	
	public static File getLogsFile(Context ctxt, boolean isPjsip) {
        File dir = PreferencesWrapper.getLogsFolder(ctxt);
        File outFile = null;
        if( dir != null) {
            Date d = new Date();
            StringBuffer fileName = new StringBuffer();
            if(isPjsip) {
                fileName.append("pjsip");
            }
            fileName.append("logs_");
            fileName.append(DateFormat.format("yy-MM-dd_kkmmss", d));
            fileName.append(".txt");
            outFile = new File(dir.getAbsoluteFile() + File.separator + fileName.toString());
        }
        
        return outFile;
	}
	
	public static File getZrtpFolder(Context ctxt) {
	    return ctxt.getFilesDir();
		/*return getSubFolder(ctxt, ZRTP_FOLDER, true);*/
	}
	
	public static void cleanLogsFiles(Context ctxt) {
		File logsFolder = getLogsFolder(ctxt);
		if(logsFolder != null) {
			File[] files = logsFolder.listFiles();
			if(files != null) {
    			for(File file: files) {
    				if(file.isFile()) {
    					file.delete();
    				}
    			}
			}
		}
	}

	/**
	 * Get current mode for the user
	 * By default the user is a default user. If becomes an advanced user he will have access to expert mode.
	 * @return
	 */
	public boolean isAdvancedUser() {
		return prefs.getBoolean(IS_ADVANCED_USER, false);
	}

	/**
	 * Toogle the user into an expert user. It will give him access to expert settings if was an expert user
	 */
	public void toogleExpertMode() {
		setPreferenceBooleanValue(IS_ADVANCED_USER, !isAdvancedUser());
	}
	
	/**
	 * Turn the application as quited by user. It will not register anymore
	 * @param quit true if the app should be considered as finished.
	 */
	public void setQuit(boolean quit) {
		setPreferenceBooleanValue(HAS_BEEN_QUIT, quit);
	}

	
	// Codec list management -- only internal use set at each start of the sip stack
	public static final String CODECS_SEPARATOR = "|";
	public static final String CODECS_LIST = "codecs_list";
    public static final String CODECS_VIDEO_LIST = "codecs_video_list";
	public static final String BACKUP_PREFIX = "backup_";
	public static final String LIB_CAP_TLS = "cap_tls";
	public static final String LIB_CAP_SRTP = "cap_srtp";
	
    /**
     * Get list of audio codecs registered in preference system
     * 
     * @return List of possible audio codecs
     * @see PreferencesProviderWrapper#setCodecList(java.util.List)
     */
	public String[] getCodecList() {
		return TextUtils.split(prefs.getString(CODECS_LIST, ""),  Pattern.quote(CODECS_SEPARATOR) );
	}

    /**
     * Get list of video codecs registered in preference system by
     * 
     * @return List of possible video codecs
     * @see PreferencesProviderWrapper#setVideoCodecList(java.util.List)
     */
    public String[] getVideoCodecList() {
        return TextUtils.split(prefs.getString(CODECS_VIDEO_LIST, ""),  Pattern.quote(CODECS_SEPARATOR) );
    }
	
    /**
     * Get the capability of the lib registered in preference system
     * 
     * @param cap on of the lib capabilty. <br/>
     *            For now valid caps are
     *            {@link PreferencesProviderWrapper#LIB_CAP_SRTP} and
     *            {@link PreferencesProviderWrapper#LIB_CAP_TLS}
     * @return True if the lib if capable of this feature
     */
	public boolean getLibCapability(String cap) {
		return prefs.getBoolean(BACKUP_PREFIX + cap, false);
	}

    /**
     * Retrieve the context used for this preference wrapper
     * 
     * @return an android context
     */
	public Context getContext() {
		return context;
	}
	
}
