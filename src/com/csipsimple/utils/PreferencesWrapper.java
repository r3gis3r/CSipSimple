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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.ui.SipHome;


public class PreferencesWrapper {
	

	
	//Internal use
	public static final String HAS_BEEN_QUIT = "has_been_quit";
	public static final String IS_ADVANCED_USER = "is_advanced_user";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";
	public static final String HAS_ALREADY_SETUP_SERVICE = "has_already_setup_service";
	
	
	private static final String THIS_FILE = "PreferencesWrapper";
	private SharedPreferences prefs;
	private ConnectivityManager connectivityManager;
	private ContentResolver resolver;
	private Context context;

	
	
	private final static HashMap<String, String> STRING_PREFS = new HashMap<String, String>(){
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
		put(SipConfigManager.RTP_PORT, "4000");
		put(SipConfigManager.OVERRIDE_NAMESERVER, "");
		
		put(SipConfigManager.SND_AUTO_CLOSE_TIME, "1");
		put(SipConfigManager.ECHO_CANCELLATION_TAIL, "200");
		put(SipConfigManager.ECHO_MODE, "2");
		put(SipConfigManager.SND_MEDIA_QUALITY, "4");
		put(SipConfigManager.SND_CLOCK_RATE, "16000");
		put(SipConfigManager.SND_PTIME, "20");
		put(SipConfigManager.BITS_PER_SAMPLE, "16");
		put(SipConfigManager.SIP_AUDIO_MODE, "0");
		put(SipConfigManager.MICRO_SOURCE, "1");
		put(SipConfigManager.THREAD_COUNT, "3");
		
		put(SipConfigManager.STUN_SERVER, "stun.counterpath.com");
		put(SipConfigManager.TURN_SERVER, "");
		put(SipConfigManager.TURN_USERNAME, "");
		put(SipConfigManager.TURN_PASSWORD, "");
//		put(TLS_SERVER_NAME, "");
//		put(CA_LIST_FILE, "");
//		put(CERT_FILE, "");
//		put(PRIVKEY_FILE, "");
//		put(TLS_PASSWORD, "");
		put(SipConfigManager.TLS_METHOD, "0");
		
		put(SipConfigManager.DSCP_VAL, "26");
		put(SipConfigManager.DTMF_MODE, "0");
		

		put(SipConfigManager.GSM_INTEGRATION_TYPE, "0");
		put(SipConfigManager.DIAL_PRESS_TONE_MODE, "0");
		put(SipConfigManager.DIAL_PRESS_VIBRATE_MODE, "0");
		
		put(SipConfigManager.DEFAULT_CALLER_ID, "");
		put(SipConfigManager.THEME, "");
		
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
		put(SipConfigManager.ENABLE_QOS, false);
		put(SipConfigManager.TLS_VERIFY_SERVER, false);
		put(SipConfigManager.USE_COMPACT_FORM, false);
		put("use_wifi_in", true);
		put("use_wifi_out", true);
		put("use_other_in", true);
		put("use_other_out", true);
		put("use_3g_in", false);
		put("use_3g_out", false);
		put("use_gprs_in", false);
		put("use_gprs_out", false);
		put("use_edge_in", false);
		put("use_edge_out", false);
		put(SipConfigManager.KEEP_ALIVE_USE_WAKE, true);
		
		//Media
		put(SipConfigManager.ECHO_CANCELLATION, false);
		put(SipConfigManager.ENABLE_VAD, false);
		put(SipConfigManager.USE_SOFT_VOLUME, false);
		put(SipConfigManager.USE_ROUTING_API, false);
		put(SipConfigManager.USE_MODE_API, false);
		put(SipConfigManager.HAS_IO_QUEUE, false);
		put(SipConfigManager.SET_AUDIO_GENERATE_TONE, false);
		put(SipConfigManager.USE_SGS_CALL_HACK, false);
		put(SipConfigManager.USE_WEBRTC_HACK, false);
		put(SipConfigManager.DO_FOCUS_AUDIO, false);
		
		//UI
		put(SipConfigManager.PREVENT_SCREEN_ROTATION, true);
		put(SipConfigManager.KEEP_AWAKE_IN_CALL, false);
		put(SipConfigManager.INVERT_PROXIMITY_SENSOR, false);
		put(SipConfigManager.ICON_IN_STATUS_BAR, true);
		put(SipConfigManager.USE_PARTIAL_WAKE_LOCK, false);
		put(SipConfigManager.ICON_IN_STATUS_BAR_NBR, false);
		put(SipConfigManager.INTEGRATE_WITH_CALLLOGS, true);
		put(SipConfigManager.INTEGRATE_WITH_DIALER, true);
		
		//Calls
		put(SipConfigManager.AUTO_RECORD_CALLS, false);
		put(SipConfigManager.SUPPORT_MULTIPLE_CALLS, false);
	}};
	
	private final static HashMap<String, Float> FLOAT_PREFS = new HashMap<String, Float>(){
		private static final long serialVersionUID = 1L;
	{
		put(SipConfigManager.SND_MIC_LEVEL, (float)1.0);
		put(SipConfigManager.SND_SPEAKER_LEVEL, (float)1.0);
		put(SipConfigManager.SND_BT_MIC_LEVEL, (float)1.0);
		put(SipConfigManager.SND_BT_SPEAKER_LEVEL, (float)1.0);
	}};
	
	
	public PreferencesWrapper(Context aContext) {
		context = aContext;
		prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		resolver = aContext.getContentResolver();
		
	}
	

	//Public setters
	/**
	 * Set a preference string value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceStringValue(String key, String value) {
		//TODO : authorized values
		Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	/**
	 * Set a preference boolean value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceBooleanValue(String key, boolean value) {
		Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	/**
	 * Set a preference float value
	 * @param key the preference key to set
	 * @param value the value for this key
	 */
	public void setPreferenceFloatValue(String key, float value) {
		Editor editor = prefs.edit();
		editor.putFloat(key, value);
		editor.commit();
	}
	
	//Private static getters
	// For string
	private static String gPrefStringValue(SharedPreferences aPrefs, String key) {
		if(STRING_PREFS.containsKey(key)) {
			return aPrefs.getString(key, STRING_PREFS.get(key));
		}
		return null;
	}
	
	// For boolean
	private static Boolean gPrefBooleanValue(SharedPreferences aPrefs, String key) {
		if(BOOLEAN_PREFS.containsKey(key)) {
			return aPrefs.getBoolean(key, BOOLEAN_PREFS.get(key));
		}
		return null;
	}
	
	// For float
	private static Float gPrefFloatValue(SharedPreferences aPrefs, String key) {
		if(FLOAT_PREFS.containsKey(key)) {
			return aPrefs.getFloat(key, FLOAT_PREFS.get(key));
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
	public int getPreferenceIntegerValue(String key) {
		try {
			return Integer.parseInt(getPreferenceStringValue(key));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Invalid "+key+" format : expect a int");
		}
		return Integer.parseInt(STRING_PREFS.get(key));
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
		int lastSeenVersion = prefs.getInt(SipHome.LAST_KNOWN_VERSION_PREF, 0);
		try {
			jsonSipSettings.put(SipHome.LAST_KNOWN_VERSION_PREF, lastSeenVersion);
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Not able to add last known version pref");
		}
		return jsonSipSettings;
	}
	
	public void restoreSipSettings(JSONObject jsonSipSettings) {
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
			Integer lastSeenVersion = jsonSipSettings.getInt(SipHome.LAST_KNOWN_VERSION_PREF);
			if(lastSeenVersion != null) {
				Editor editor = prefs.edit();
				editor.putInt(SipHome.LAST_KNOWN_VERSION_PREF, lastSeenVersion);
				editor.commit();
			}
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Not able to add last known version pref");
		}
	}
	
	
	public SharedPreferences getDirectPrefs() {
		return prefs;
	}
	
	
	// Network part
	
	// Check for wifi
	static public boolean isValidWifiConnectionFor(NetworkInfo ni, SharedPreferences aPrefs, String suffix) {
		
		boolean valid_for_wifi = gPrefBooleanValue(aPrefs, "use_wifi_" + suffix);
		if (valid_for_wifi && 
			ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
			
			// Wifi connected
			//TODO : check if not CONNECTING is not good as value here
			Log.d(THIS_FILE, "Wifi state is now "+ni.getState().name());
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}
		return false;
	}
	
	// Check for acceptable mobile data network connection
	static public boolean isValidMobileConnectionFor(NetworkInfo ni, SharedPreferences aPrefs, String suffix) {

		boolean valid_for_3g = gPrefBooleanValue(aPrefs, "use_3g_" + suffix);
		boolean valid_for_edge = gPrefBooleanValue(aPrefs, "use_edge_" + suffix);
		boolean valid_for_gprs = gPrefBooleanValue(aPrefs, "use_gprs_" + suffix);
		
		if ((valid_for_3g || valid_for_edge || valid_for_gprs) &&
			 ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE) {

			// Any mobile network connected
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				int subType = ni.getSubtype();
				
				// 3G (or better)
				if (valid_for_3g &&
					subType >= TelephonyManager.NETWORK_TYPE_UMTS) {
					return true;
				}
				
				// GPRS (or unknown)
				if (valid_for_gprs &&	
					(subType == TelephonyManager.NETWORK_TYPE_GPRS || subType == TelephonyManager.NETWORK_TYPE_UNKNOWN)) {
					return true;
				}
				
				// EDGE
				if (valid_for_edge &&
					subType == TelephonyManager.NETWORK_TYPE_EDGE) {
					return true;
				}
			}
		}
		return false;
	}
	
	// Check for other (wimax for example)
	static public boolean isValidOtherConnectionFor(NetworkInfo ni, SharedPreferences aPrefs, String suffix) {
		
		boolean valid_for_other = gPrefBooleanValue(aPrefs, "use_other_" + suffix);
		//boolean valid_for_other = true;
		if (valid_for_other && 
			ni != null && 
			ni.getType() != ConnectivityManager.TYPE_MOBILE && ni.getType() != ConnectivityManager.TYPE_WIFI) {
			
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}
		return false;
	}
	
	// Generic function for both incoming and outgoing
	static public boolean isValidConnectionFor(NetworkInfo ni, SharedPreferences aPrefs, String suffix) {
		if (isValidWifiConnectionFor(ni, aPrefs, suffix)) {
			Log.d(THIS_FILE, "We are valid for WIFI");
			return true;
		}
		if(isValidMobileConnectionFor(ni, aPrefs, suffix)) {
			Log.d(THIS_FILE, "We are valid for MOBILE");
			return true;
		}
		if(isValidOtherConnectionFor(ni, aPrefs, suffix)) {
			Log.d(THIS_FILE, "We are valid for OTHER");
			return true;
		}
		return false;
	}
	
	/**
	 * Say whether current connection is valid for outgoing calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForOutgoing() {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		return isValidConnectionFor(ni, prefs, "out");
	}

	/**
	 * Say whether current connection is valid for incoming calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForIncoming() {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		return isValidConnectionFor(ni, prefs, "in");
	}

	public ArrayList<String> getAllIncomingNetworks(){
		ArrayList<String> incomingNetworks = new ArrayList<String>();
		String[] availableNetworks = {"3g", "edge", "gprs", "wifi", "other"};
		for(String network:availableNetworks) {
			if(getPreferenceBooleanValue("use_"+network+"_in")) {
				incomingNetworks.add(network);
			}
		}
		
		return incomingNetworks;
	}
	

	public void disableAllForIncoming() {
		String[] availableNetworks = {"3g", "edge", "gprs", "wifi", "other"};
		for(String network:availableNetworks) {
			setPreferenceBooleanValue("use_"+network+"_in", false);
		}
	}
	
	public boolean isTCPEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_TCP);
	}
	
	public boolean isUDPEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_UDP);
	}

	public boolean isTLSEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_TLS);
	}
	
	public boolean useIPv6() {
		return getPreferenceBooleanValue(SipConfigManager.USE_IPV6);
	}
	
	private int getPrefPort(String key) {
		int port = getPreferenceIntegerValue(key);
		if(isValidPort(port)) {
			return port;
		}
		return Integer.parseInt(STRING_PREFS.get(key));
	}
	
	public int getUDPTransportPort() {
		return getPrefPort(SipConfigManager.UDP_TRANSPORT_PORT);
	}
	
	public int getTCPTransportPort() {
		return getPrefPort(SipConfigManager.TCP_TRANSPORT_PORT);
	}
	
	public int getTLSTransportPort() {
		return getPrefPort(SipConfigManager.TLS_TRANSPORT_PORT);
	}
	
	public int getKeepAliveInterval() {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		if(ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
			return getPreferenceIntegerValue(SipConfigManager.KEEP_ALIVE_INTERVAL_WIFI);
		}
		return getPreferenceIntegerValue(SipConfigManager.KEEP_ALIVE_INTERVAL_MOBILE);
	}
	
	public int getRTPPort() {
		return getPrefPort(SipConfigManager.RTP_PORT);
	}
	
	public boolean enableDNSSRV() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV);
	}
	
	
	public int getDSCPVal() {
		return getPreferenceIntegerValue(SipConfigManager.DSCP_VAL);
	}
	
	public int getTLSMethod() {
		return getPreferenceIntegerValue(SipConfigManager.TLS_METHOD);
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

	public String getUserAgent(Context ctx) {
		String userAgent = getPreferenceStringValue(SipConfigManager.USER_AGENT);
		if(userAgent.equalsIgnoreCase(CustomDistribution.getUserAgent())) {
			//If that's the official -not custom- user agent, send the release, the device and the api level
			PackageInfo pinfo = getCurrentPackageInfos(ctx);
			if(pinfo != null) {
				userAgent +=  " r" + pinfo.versionCode+" / "+android.os.Build.DEVICE+"-"+Compatibility.getApiLevel();
			}
		}
		return userAgent;
	}
	
	
	public final static PackageInfo getCurrentPackageInfos(Context ctx) {
		PackageInfo pinfo = null;
		try {
			pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(THIS_FILE, "Impossible to find version of current package !!");
		}
		return pinfo;
	}
	
	//Media part
	
	/**
	 * Get auto close time after end of the call
	 * To avoid crash after hangup -- android 1.5 only but
	 * even sometimes crash
	 */
	public int getAutoCloseTime() {
		return getPreferenceIntegerValue(SipConfigManager.SND_AUTO_CLOSE_TIME);
	}
	
	
	/**
	 * Whether echo cancellation is enabled
	 * @return true if enabled
	 */
	public boolean hasEchoCancellation() {
		return getPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION);
	}
	

	public long getEchoCancellationTail() {
		if(!hasEchoCancellation()) {
			return 0;
		}
		return getPreferenceIntegerValue(SipConfigManager.ECHO_CANCELLATION_TAIL);
	}
	
	public int getEchoMode() {
		return getPreferenceIntegerValue(SipConfigManager.ECHO_MODE);
		
	}

	/**
	 * Whether voice audio detection is enabled
	 * @return 1 if Voice audio detection is disabled
	 */
	public int getNoVad() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_VAD) ?0:1;
	}

	
	/**
	 * Get the audio codec quality setting
	 * @return the audio quality
	 */
	public long getMediaQuality() {
		String mediaQuality = getPreferenceStringValue(SipConfigManager.SND_MEDIA_QUALITY);
		//prefs.getString(SND_MEDIA_QUALITY, String.valueOf(defaultValue));
		try {
			int prefsValue = Integer.parseInt(mediaQuality);
			if(prefsValue <= 10 && prefsValue >= 0) {
				return prefsValue;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Audio quality "+mediaQuality+" not well formated");
		}
		
		return 4;
	}
	
	public int getBitsPerSample() {
		try {
			return Integer.parseInt(getPreferenceStringValue(SipConfigManager.BITS_PER_SAMPLE));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Bits per sample not well formated");
		}
		return Integer.parseInt(STRING_PREFS.get(SipConfigManager.BITS_PER_SAMPLE));
	}
	
	/**
	 * Get the audio codec quality setting
	 * @return the audio quality
	 */
	public int getInCallMode() {
		String mode = getPreferenceStringValue(SipConfigManager.SIP_AUDIO_MODE);
		try {
			return Integer.parseInt(mode);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "In call mode "+mode+" not well formated");
		}
		
		return AudioManager.MODE_NORMAL;
	}
	
	/**
	 * Get current clock rate
	 * @return clock rate in Hz
	 */
	public long getClockRate() {
		String clockRate = getPreferenceStringValue(SipConfigManager.SND_CLOCK_RATE);
		try {
			return Integer.parseInt(clockRate);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Clock rate "+clockRate+" not well formated");
		}
		return 16000;
	}
	
	
	public boolean getUseRoutingApi() {
		return getPreferenceBooleanValue(SipConfigManager.USE_ROUTING_API);
	}
	
	public boolean getUseModeApi() {
		return getPreferenceBooleanValue(SipConfigManager.USE_MODE_API);
	}
	
	/**
	 * Get whether ice is enabled
	 * @return 1 if enabled (pjstyle)
	 */
	public int getIceEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_ICE)?1:0;
	}

	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getTurnEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_TURN)?1:0;
	}
	
	/**
	 * Get stun server
	 * @return host:port or blank if not set
	 */
	public String getStunServer() {
		return getPreferenceStringValue(SipConfigManager.STUN_SERVER);
	}
	
	
	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getStunEnabled() {
		return getPreferenceBooleanValue(SipConfigManager.ENABLE_STUN)?1:0;
	}
	
	/**
	 * Get turn server
	 * @return host:port or blank if not set
	 */
	public String getTurnServer() {
		return getPreferenceStringValue(SipConfigManager.TURN_SERVER);
	}
	
	/**
	 * Get the codec priority
	 * @param codecName codec name formated in the pjsip format (the corresponding pref is codec_{{lower(codecName)}}_{{codecFreq}})
	 * @param defaultValue the default value if the pref is not found MUST be casteable as Integer/short
	 * @return the priority of the codec as defined in preferences
	 */
	
	public short getCodecPriority(String codecName, String defaultValue) {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		if(ni != null) {
			String currentBandType = prefs.getString(SipConfigManager.getBandTypeKey(ni.getType(), ni.getSubtype()), 
					SipConfigManager.CODEC_WB);
			
			return getCodecPriority(codecName, currentBandType, defaultValue);
		}
		return (short) Integer.parseInt(defaultValue);
		
	}
	
	public short getCodecPriority(String codecName, String type, String defaultValue) {
		String key = SipConfigManager.getCodecKey(codecName, type); 
		if(key != null) {
			return (short) Integer.parseInt(prefs.getString(key, defaultValue));
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
	
	
	public boolean hasCodecPriority(String codecName) {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		String[] codecParts = codecName.split("/");
		if(codecParts.length >=2 ) {
			if(ni != null) {
				String currentBandType = prefs.getString(SipConfigManager.getBandTypeKey(ni.getType(), ni.getSubtype()), 
						SipConfigManager.CODEC_WB);
				String key = SipConfigManager.getCodecKey(codecName, currentBandType); 
				return prefs.contains(key);
			}else {
				String key = SipConfigManager.getCodecKey(codecName, SipConfigManager.CODEC_WB); 
				return prefs.contains(key);
			}
		}
		return false;
	}
	

	/**
	 * Get sip ringtone
	 * @return string uri
	 */
	public String getRingtone() {
		String ringtone = prefs.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
		
		if(ringtone == null || TextUtils.isEmpty(ringtone)) {
			ringtone = Settings.System.DEFAULT_RINGTONE_URI.toString();
		}
		return ringtone;
	}

	
	public int getAudioFramePtime() {
		return getPreferenceIntegerValue(SipConfigManager.SND_PTIME);
	}
	
	public int getHasIOQueue() {
		return getPreferenceBooleanValue(SipConfigManager.HAS_IO_QUEUE)?1:0;
	}
	
	public boolean generateForSetCall() {
		return getPreferenceBooleanValue(SipConfigManager.SET_AUDIO_GENERATE_TONE);
	}

	
	public static final String DTMF_MODE_AUTO = "0";
	public static final String DTMF_MODE_RTP = "1";
	public static final String DTMF_MODE_INBAND = "2";
	public static final String DTMF_MODE_INFO = "3";
	
	
	public boolean useSipInfoDtmf() {
		return getPreferenceStringValue(SipConfigManager.DTMF_MODE).equalsIgnoreCase(DTMF_MODE_INFO);
	}
	
	public boolean forceDtmfInBand() {
		return getPreferenceStringValue(SipConfigManager.DTMF_MODE).equalsIgnoreCase(DTMF_MODE_INBAND);
	}

	public boolean forceDtmfRTP() {
		return getPreferenceStringValue(SipConfigManager.DTMF_MODE).equalsIgnoreCase(DTMF_MODE_RTP);
	}


	public long getThreadCount() {
		int value = getPreferenceIntegerValue(SipConfigManager.THREAD_COUNT);
		if(value < 10) {
			return value;
		}
		return Integer.parseInt(STRING_PREFS.get(SipConfigManager.THREAD_COUNT));
	}

	// ---- 
	// UI related
	// ----
	public boolean getDialPressTone() {
		int mode = getPreferenceIntegerValue(SipConfigManager.DIAL_PRESS_TONE_MODE);
		switch (mode) {
		case 0:
			return Settings.System.getInt(resolver,
	                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
		case 1:
			return true;
		case 2:
			return false;
		default:
			break;
		}
		return false;
	}

	public boolean getDialPressVibrate() {
		int mode = getPreferenceIntegerValue(SipConfigManager.DIAL_PRESS_VIBRATE_MODE);
		switch (mode) {
		case 0:
			return Settings.System.getInt(resolver,
	                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
		case 1:
			return true;
		case 2:
			return false;
		default:
			break;
		}
		return false;
	}

	public boolean startIsDigit() {
		return !prefs.getBoolean("start_with_text_dialer", false);
	}

	public boolean getUseAlternateUnlocker() {
		return prefs.getBoolean("use_alternate_unlocker", false);
	}
	
	public boolean useIntegrateDialer() {
		return getPreferenceBooleanValue(SipConfigManager.INTEGRATE_WITH_DIALER);
	}
	public boolean useIntegrateCallLogs() {
		return getPreferenceBooleanValue(SipConfigManager.INTEGRATE_WITH_CALLLOGS);
	}


	public boolean keepAwakeInCall() {
		return getPreferenceBooleanValue(SipConfigManager.KEEP_AWAKE_IN_CALL);
	}
	
	public boolean invertProximitySensor() {
		return getPreferenceBooleanValue(SipConfigManager.INVERT_PROXIMITY_SENSOR);
	}

	public float getInitialVolumeLevel() {
		return (float) ((float) (prefs.getFloat("snd_stream_level", (float) 8.0)) / 10.0);
	}

	public boolean usePartialWakeLock() {
		return getPreferenceBooleanValue(SipConfigManager.USE_PARTIAL_WAKE_LOCK);
	}
	

	public boolean integrateWithMusicApp() {
		return prefs.getBoolean("integrate_with_native_music", true);
	}
	
	public int getLogLevel() {
		int prefsValue = getPreferenceIntegerValue(SipConfigManager.LOG_LEVEL);
		if(prefsValue <= 6 && prefsValue >= 1) {
			return prefsValue;
		}
		return 1;
	}
	
	
	public final static int GSM_TYPE_AUTO = 0;
	public final static int GSM_TYPE_FORCE = 1;
	public final static int GSM_TYPE_PREVENT = 2;
	
	public int getGsmIntegrationType() {
		int prefsValue = 1;
		String gsmType = getPreferenceStringValue(SipConfigManager.GSM_INTEGRATION_TYPE);
		try {
			prefsValue = Integer.parseInt(gsmType);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Gsm type " + gsmType + " not well formated");
		}
		return prefsValue;
	}
	
	public boolean showIconInStatusBar() {
		return getPreferenceBooleanValue(SipConfigManager.ICON_IN_STATUS_BAR);
	}


	public final static int HEADSET_ACTION_CLEAR_CALL = 0;
	public final static int HEADSET_ACTION_MUTE = 1;
	public final static int HEADSET_ACTION_HOLD = 2;
	/**
	 * Action do do when headset is pressed
	 * @return
	 */
	public int getHeadsetAction() {
		try {
			return Integer.parseInt(prefs.getString("headset_action", "0"));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Headset action option not well formated");
		}
		return HEADSET_ACTION_CLEAR_CALL;
	}

	
	/**
	 * Check wether setup has already been done
	 * @return
	 */
	public boolean hasAlreadySetup() {
		return prefs.getBoolean(HAS_ALREADY_SETUP, false);
	}
	
	public boolean hasAlreadySetupService() {
		return prefs.getBoolean(HAS_ALREADY_SETUP_SERVICE, false);
	}

	//Utils
	
	/**
	 * Check TCP/UDP validity of a network port
	 */
	private boolean isValidPort(int port) {
		return (port>=0 && port < 65535);
	}

	/**
	 * Get a property from android property subsystem
	 * @param prop property to get
	 * @return the value of the property command line or null if failed
	 */
	public String getSystemProp(String prop) {
		//String re1 = "^\\d+(\\.\\d+){3}$";
		//String re2 = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$";
		try {
			String line; 
			Process p = Runtime.getRuntime().exec("getprop "+prop); 
			InputStream in = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(isr);
			while ((line = br.readLine()) != null ) { 
				return line;
			}
		} catch ( Exception e ) { 
			// ignore resolutely
		}
		return null;
	}

	private static String CONFIG_FOLDER = "configs";
	private static String RECORDS_FOLDER = "records";
	private static String LOGS_FOLDER = "logs";
	
	private static File getStorageFolder() {
		File root = Environment.getExternalStorageDirectory();
		
	    if (root.canWrite()){
			File dir = new File(root.getAbsolutePath() + File.separator + "CSipSimple");
			if(!dir.exists()) {
				dir.mkdirs();
				Log.d(THIS_FILE, "Create directory " + dir.getAbsolutePath());
			}
			return dir;
	    }
	    return null;
	}
	
	
	private static File getSubFolder(String subFolder) {
		File root = getStorageFolder();
		if(root != null) {
			File dir = new File(root.getAbsoluteFile() + File.separator + subFolder);
			dir.mkdirs();
			return dir;
		}
		return null;
	}
	
	public static File getConfigFolder() {
		return getSubFolder(CONFIG_FOLDER);
	}
	
	public static File getRecordsFolder() {
		return getSubFolder(RECORDS_FOLDER);
	}
	
	public static File getLogsFolder() {
		return getSubFolder(LOGS_FOLDER);
	}
	
	
	public static void cleanLogsFiles() {
		File logsFolder = getLogsFolder();
		if(logsFolder != null) {
			File[] files = logsFolder.listFiles();
			for(File file: files) {
				if(file.isFile()) {
					file.delete();
				}
			}
		}
	}

	public boolean isAdvancedUser() {
		return prefs.getBoolean(IS_ADVANCED_USER, false);
	}


	public void toogleExpertMode() {
		setPreferenceBooleanValue(IS_ADVANCED_USER, !isAdvancedUser());
	}
	
	public boolean hasBeenQuit() {
		return prefs.getBoolean(HAS_BEEN_QUIT, false);
	}

	public void setQuit(boolean quit) {
		setPreferenceBooleanValue(HAS_BEEN_QUIT, quit);
	}

	
	// Codec list management -- only internal use set at each start of the sip stack
	private static final String CODECS_SEPARATOR = "|";
	private static final String CODECS_LIST = "codecs_list";
	public void setCodecList(ArrayList<String> codecs) {
		if(codecs != null) {
			setPreferenceStringValue(CODECS_LIST, TextUtils.join(CODECS_SEPARATOR, codecs));
		}
	}

	public String[] getCodecList() {
		return TextUtils.split(prefs.getString(CODECS_LIST, ""),  Pattern.quote(CODECS_SEPARATOR) );
	}

	public static final String LIB_CAP_TLS = "cap_tls";
	public static final String LIB_CAP_SRTP = "cap_srtp";
	public void setLibCapability(String cap, boolean canDo) {
		setPreferenceBooleanValue("backup_" + cap, canDo);
	}
	public boolean getLibCapability(String cap) {
		return prefs.getBoolean("backup_" + cap, false);
	}


	public Context getContext() {
		return context;
	}
	
}
