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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.service.PreferenceProvider;

public class PreferencesProviderWrapper {


	private static final String THIS_FILE = "Prefs";
	private ContentResolver resolver;
	private ConnectivityManager connectivityManager;
	

	public static final String LIB_CAP_TLS = PreferencesWrapper.LIB_CAP_TLS;
	public static final String LIB_CAP_SRTP = PreferencesWrapper.LIB_CAP_SRTP;
	public static final String HAS_BEEN_QUIT = PreferencesWrapper.HAS_BEEN_QUIT;
	public static final String HAS_ALREADY_SETUP_SERVICE = PreferencesWrapper.HAS_ALREADY_SETUP_SERVICE;

	public PreferencesProviderWrapper(Context aContext) {
		resolver = aContext.getContentResolver();
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	private Uri getPrefUriForKey(String key) {
		return Uri.withAppendedPath(PreferenceProvider.PREF_ID_URI_BASE, key);
	}

	public String getPreferenceStringValue(String key) {
		return getPreferenceStringValue(key, null);
	}
	
	public String getPreferenceStringValue(String key, String defaultValue) {
		String value = defaultValue;
		Uri uri = getPrefUriForKey(key);
		Cursor c = resolver.query(uri, null, String.class.getName(), null, null);
		if(c!=null) {
			c.moveToFirst();
			value = c.getString(PreferenceProvider.COL_INDEX_VALUE);
			c.close();
		}
		return value;
	}

	public Boolean getPreferenceBooleanValue(String key) {
		return getPreferenceBooleanValue(key, null);
	}
	
	public Boolean getPreferenceBooleanValue(String key, Boolean defaultValue) {
		Boolean value = defaultValue;
		Uri uri = getPrefUriForKey(key);
		Cursor c = resolver.query(uri, null, Boolean.class.getName(), null, null);
		if(c!=null) {
			c.moveToFirst();
			value = (c.getInt(PreferenceProvider.COL_INDEX_VALUE) == 1);
			c.close();
		}
		return value;
	}

	public float getPreferenceFloatValue(String key) {
		return getPreferenceFloatValue(key, null);
	}
	
	public float getPreferenceFloatValue(String key,  Float defaultValue) {
		Float value = defaultValue;
		Uri uri = getPrefUriForKey(key);
		Cursor c = resolver.query(uri, null, Float.class.getName(), null, null);
		if(c!=null) {
			c.moveToFirst();
			value = c.getFloat(PreferenceProvider.COL_INDEX_VALUE);
			c.close();
		}
		return value;
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
		return Integer.parseInt(PreferencesWrapper.STRING_PREFS.get(key));
	}

	
	public void setPreferenceStringValue(String key, String value) {
		Uri uri = getPrefUriForKey(key);
		ContentValues values = new ContentValues();
		values.put(PreferenceProvider.FIELD_VALUE, value);
		resolver.update(uri, values, String.class.getName(), null);
	}
	
	public void setPreferenceBooleanValue(String key, boolean value) {
		Uri uri = getPrefUriForKey(key);
		ContentValues values = new ContentValues();
		values.put(PreferenceProvider.FIELD_VALUE, value);
		resolver.update(uri, values, Boolean.class.getName(), null);
	}
	
	public void setPreferenceFloatValue(String key, Float value) {
		Uri uri = getPrefUriForKey(key);
		ContentValues values = new ContentValues();
		values.put(PreferenceProvider.FIELD_VALUE, value);
		resolver.update(uri, values, Float.class.getName(), null);
	}

	/**
	 * Set all values to default
	 */
	public void resetAllDefaultValues() {
		Uri uri = PreferenceProvider.RAZ_URI;
		resolver.update(uri, new ContentValues(), null, null);
	}
	
	
	// Network part
	
	// Check for wifi
	private boolean isValidWifiConnectionFor(NetworkInfo ni, String suffix) {
		
		boolean valid_for_wifi = getPreferenceBooleanValue("use_wifi_" + suffix, true);
		if (valid_for_wifi && 
			ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
			
			// Wifi connected
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}
		return false;
	}
	
	// Check for acceptable mobile data network connection
	private boolean isValidMobileConnectionFor(NetworkInfo ni, String suffix) {

		boolean valid_for_3g = getPreferenceBooleanValue("use_3g_" + suffix, false);
		boolean valid_for_edge = getPreferenceBooleanValue("use_edge_" + suffix, false);
		boolean valid_for_gprs = getPreferenceBooleanValue("use_gprs_" + suffix, false);
		
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
	private boolean isValidOtherConnectionFor(NetworkInfo ni, String suffix) {
		
		boolean valid_for_other = getPreferenceBooleanValue("use_other_" + suffix, true);
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
	private boolean isValidConnectionFor(NetworkInfo ni, String suffix) {
		if (isValidWifiConnectionFor(ni, suffix)) {
			Log.d(THIS_FILE, "We are valid for WIFI");
			return true;
		}
		if(isValidMobileConnectionFor(ni, suffix)) {
			Log.d(THIS_FILE, "We are valid for MOBILE");
			return true;
		}
		if(isValidOtherConnectionFor(ni, suffix)) {
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
		return isValidConnectionFor(ni, "out");
	}

	/**
	 * Say whether current connection is valid for incoming calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForIncoming() {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		return isValidConnectionFor(ni, "in");
	}

	public int getLogLevel() {
		int prefsValue = getPreferenceIntegerValue(SipConfigManager.LOG_LEVEL);
		if(prefsValue <= 6 && prefsValue >= 1) {
			return prefsValue;
		}
		return 1;
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
	

	public boolean generateForSetCall() {
		return getPreferenceBooleanValue(SipConfigManager.SET_AUDIO_GENERATE_TONE);
	}
	


	public float getInitialVolumeLevel() {
		return (float) (getPreferenceFloatValue(SipConfigManager.SND_STREAM_LEVEL, 8.0f) / 10.0f);
	}
	


	/**
	 * Get sip ringtone
	 * @return string uri
	 */
	public String getRingtone() {
		String ringtone = getPreferenceStringValue(SipConfigManager.RINGTONE, 
				Settings.System.DEFAULT_RINGTONE_URI.toString());
		
		if(ringtone == null || TextUtils.isEmpty(ringtone)) {
			ringtone = Settings.System.DEFAULT_RINGTONE_URI.toString();
		}
		return ringtone;
	}
	
	
	
	/// ---- PURE SIP SETTINGS -----

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
		return Integer.parseInt(PreferencesWrapper.STRING_PREFS.get(key));
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
	


	public final static int HEADSET_ACTION_CLEAR_CALL = 0;
	public final static int HEADSET_ACTION_MUTE = 1;
	public final static int HEADSET_ACTION_HOLD = 2;
	/**
	 * Action do do when headset is pressed
	 * @return
	 */
	public int getHeadsetAction() {
		try {
			return Integer.parseInt(getPreferenceStringValue(SipConfigManager.HEADSET_ACTION));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Headset action option not well formated");
		}
		return HEADSET_ACTION_CLEAR_CALL;
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
		return Integer.parseInt(PreferencesWrapper.STRING_PREFS.get(SipConfigManager.BITS_PER_SAMPLE));
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
	
	public void setCodecList(ArrayList<String> codecs) {
		if(codecs != null) {
			setPreferenceStringValue(PreferencesWrapper.CODECS_LIST, TextUtils.join(PreferencesWrapper.CODECS_SEPARATOR, codecs));
		}
	}
	
	public void setLibCapability(String cap, boolean canDo) {
		setPreferenceBooleanValue(PreferencesWrapper.BACKUP_PREFIX + cap, canDo);
	}

	// DTMF
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
	
	
	// Codecs
	

	/**
	 * Get the codec priority
	 * @param codecName codec name formated in the pjsip format (the corresponding pref is codec_{{lower(codecName)}}_{{codecFreq}})
	 * @param defaultValue the default value if the pref is not found MUST be casteable as Integer/short
	 * @return the priority of the codec as defined in preferences
	 */
	
	public short getCodecPriority(String codecName, String type, String defaultValue) {
		String key = SipConfigManager.getCodecKey(codecName, type); 
		if(key != null) {
			String val = getPreferenceStringValue(key, defaultValue);
			if(!TextUtils.isEmpty(val)) {
				try {
					return (short) Integer.parseInt(val);
				}catch(NumberFormatException e) {
					Log.e(THIS_FILE, "Impossible to parse " + val);
				}
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
	
	
	public boolean hasCodecPriority(String codecName) {
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		String[] codecParts = codecName.split("/");
		if(codecParts.length >=2 ) {
			if(ni != null) {
				String currentBandType = getPreferenceStringValue(SipConfigManager.getBandTypeKey(ni.getType(), ni.getSubtype()), 
						SipConfigManager.CODEC_WB);
				String key = SipConfigManager.getCodecKey(codecName, currentBandType); 
				
				String val = getPreferenceStringValue(key, null);
				return (val != null);
			}else {
				String key = SipConfigManager.getCodecKey(codecName, SipConfigManager.CODEC_WB); 
				String val = getPreferenceStringValue(key, null);
				return (val != null);
			}
		}
		return false;
	}

	public static File getRecordsFolder() {
		return PreferencesWrapper.getRecordsFolder();
	}
}
