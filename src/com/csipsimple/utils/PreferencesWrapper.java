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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsua;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;


public class PreferencesWrapper {
	
	private static final String THIS_FILE = "PreferencesWrapper";
	private SharedPreferences prefs;
	private ConnectivityManager connectivityManager;

	public final static String HAS_ALREADY_SETUP = "has_already_setup";
	public final static String SND_AUTO_CLOSE_TIME = "snd_auto_close_time";
	public final static String SND_CLOCK_RATE = "snd_clock_rate";
	public final static String ECHO_CANCELLATION = "echo_cancellation";
	public final static String ENABLE_VAD = "enable_vad";
	public final static String KEEP_AWAKE_IN_CALL = "keep_awake_incall";
	
	public final static String SND_MIC_LEVEL = "snd_mic_level";
	public final static String SND_SPEAKER_LEVEL = "snd_speaker_level";
	
	public PreferencesWrapper(Context aContext) {
		prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	

	public void setPreferenceStringValue(String key, String value) {
		Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}
	

	public void setPreferenceBooleanValue(String key, boolean value) {
		Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	public void setPreferenceFloatValue(String key, float value) {
		Editor editor = prefs.edit();
		editor.putFloat(key, value);
		editor.commit();
	}
	
	// Network part
	
	// Check for wifi
	static public boolean isValidWifiConnectionFor(NetworkInfo ni, SharedPreferences aPrefs, String suffix) {
		
		boolean valid_for_wifi = aPrefs.getBoolean("use_wifi_" + suffix, true);
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

		boolean valid_for_3g = aPrefs.getBoolean("use_3g_" + suffix, false);
		boolean valid_for_edge = aPrefs.getBoolean("use_edge_" + suffix, false);
		boolean valid_for_gprs = aPrefs.getBoolean("use_gprs_" + suffix, false);
		
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
		
		boolean valid_for_other = aPrefs.getBoolean("use_other_" + suffix, true);
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
			if(prefs.getBoolean("use_"+network+"_in", network.equals("wifi")?true:false)) {
				incomingNetworks.add(network);
			}
		}
		
		return incomingNetworks;
	}
	
	public boolean getLockWifi() {
		return prefs.getBoolean("lock_wifi", true);
	}
	

	public pjmedia_srtp_use getUseSrtp() {
		try {
			int use_srtp = Integer.parseInt(prefs.getString("use_srtp", "0"));
			pjmedia_srtp_use.swigToEnum(use_srtp);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return pjmedia_srtp_use.PJMEDIA_SRTP_DISABLED;
	}
	
	
	
	public boolean isTCPEnabled() {
		return prefs.getBoolean("enable_tcp", true);
	}
	
	public boolean isUDPEnabled() {
		return prefs.getBoolean("enable_udp", true);
	}

	public boolean isTLSEnabled() {
		return prefs.getBoolean("enable_tls", false);
	}
	
	public boolean useIPv6() {
		return prefs.getBoolean("use_ipv6", false);
	}
	
	
	
	public int getUDPTransportPort() {
		try {
			int port = Integer.parseInt(prefs.getString("network_udp_transport_port", "5060"));
			if(isValidPort(port)) {
				return port;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5060;
	}
	
	public int getTCPTransportPort() {
		try {
			int port =  Integer.parseInt(prefs.getString("network_tcp_transport_port", "5060"));
			if(isValidPort(port)) {
				return port;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5060;
	}
	
	public int getTLSTransportPort() {
		try {
			int port =  Integer.parseInt(prefs.getString("network_tls_transport_port", "5061"));
			if(isValidPort(port)) {
				return port;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5061;
	}
	
	public int getRTPPort() {
		try {
			int port = Integer.parseInt(prefs.getString("network_rtp_port", "4000"));
			if(isValidPort(port)) {
				return port;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 4000;
	}
	

	public boolean enableDNSSRV() {
		return prefs.getBoolean("enable_dns_srv", true);
	}
	
	public pj_str_t[] getNameservers() {
		pj_str_t[] nameservers = null;
		
		if(enableDNSSRV()) {
			String prefsDNS = prefs.getString("override_nameserver", "");
			if(TextUtils.isEmpty(prefsDNS)) {
				String dnsName1 = getSystemProp("net.dns1");
				String dnsName2 = getSystemProp("net.dns2");
				Log.d(THIS_FILE, "DNS server will be set to : "+dnsName1+ " / "+dnsName2);
				
				if(dnsName1 == null && dnsName2 == null) {
					//TODO : WARNING : In this case....we have probably a problem !
					nameservers = new pj_str_t[] {};
				}else if(dnsName1 == null) {
					nameservers = new pj_str_t[] {pjsua.pj_str_copy(dnsName2)};
				}else if(dnsName2 == null) {
					nameservers = new pj_str_t[] {pjsua.pj_str_copy(dnsName1)};
				}else {
					nameservers = new pj_str_t[] {pjsua.pj_str_copy(dnsName1), pjsua.pj_str_copy(dnsName2)};
				}
			}else {
				nameservers = new pj_str_t[] {pjsua.pj_str_copy(prefsDNS)};
			}
		}
		return nameservers;
	}
	
	
	
	
	//Media part
	
	/**
	 * Get auto close time after end of the call
	 * To avoid crash after hangup -- android 1.5 only but
	 * even sometimes crash
	 */
	public int getAutoCloseTime() {
		String autoCloseTime = prefs.getString(SND_AUTO_CLOSE_TIME, "1");
		try {
			return Integer.parseInt(autoCloseTime);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Auto close time "+autoCloseTime+" not well formated");
		}
		return 1;
	}
	
	public String getUserAgent() {
		return prefs.getString("user_agent","CSipSimple");
	}
	
	/**
	 * Whether echo cancellation is enabled
	 * @return true if enabled
	 */
	public boolean hasEchoCancellation() {
		return prefs.getBoolean(ECHO_CANCELLATION, true);
	}
	

	public long getEchoCancellationTail() {
		if(!hasEchoCancellation()) {
			return 0;
		}
		String tailLength = prefs.getString("echo_cancellation_tail", "200");
		try {
			return Integer.parseInt(tailLength);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Tail length "+tailLength+" not well formated");
		}
		return 0;
	}

	/**
	 * Whether voice audio detection is enabled
	 * @return 1 if Voice audio detection is disabled
	 */
	public int getNoVad() {
		return prefs.getBoolean(ENABLE_VAD, false)?0:1;
	}

	
	/**
	 * Get the audio codec quality setting
	 * @return the audio quality
	 */
	public long getMediaQuality() {
		int defaultValue = 4;
		int prefsValue = 4;
		String mediaQuality = prefs.getString("snd_media_quality", String.valueOf(defaultValue));
		try {
			prefsValue = Integer.parseInt(mediaQuality);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Audio quality "+mediaQuality+" not well formated");
		}
		if(prefsValue <= 10 && prefsValue >= 0) {
			return prefsValue;
		}
		return defaultValue;
	}
	
	/**
	 * Get current clock rate
	 * @return clock rate in Hz
	 */
	public long getClockRate() {
		String clockRate = prefs.getString(SND_CLOCK_RATE, "16000");
		try {
			return Integer.parseInt(clockRate);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Clock rate "+clockRate+" not well formated");
		}
		return 8000;
	}
	
	/**
	 * Get whether ice is enabled
	 * @return 1 if enabled (pjstyle)
	 */
	public int getIceEnabled() {
		return prefs.getBoolean("enable_ice", false)?1:0;
	}

	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getTurnEnabled() {
		return prefs.getBoolean("enable_turn", false)?1:0;
	}
	
	/**
	 * Get turn server
	 * @return host:port or blank if not set
	 */
	public String getStunServer() {
		return prefs.getString("stun_server", "");
	}
	
	
	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getStunEnabled() {
		return prefs.getBoolean("enable_stun", false)?1:0;
	}
	
	/**
	 * Get turn server
	 * @return host:port or blank if not set
	 */
	public String getTurnServer() {
		return prefs.getString("turn_server", "");
	}
	
	/**
	 * Get the codec priority
	 * @param codecName codec name formated in the pjsip format (the corresponding pref is codec_{{lower(codecName)}}_{{codecFreq}})
	 * @param defaultValue the default value if the pref is not found MUST be casteable as Integer/short
	 * @return the priority of the codec as defined in preferences
	 */
	public short getCodecPriority(String codecName, String defaultValue) {
		String[] codecParts = codecName.split("/");
		if(codecParts.length >=2 ) {
			return (short) Integer.parseInt(prefs.getString("codec_"+codecParts[0].toLowerCase()+"_"+codecParts[1], defaultValue));
		}
		return (short) Integer.parseInt(defaultValue);
	}
	
	public void setCodecPriority(String codecName, String newValue) {
		String[] codecParts = codecName.split("/");
		if(codecParts.length >=2 ) {
			setPreferenceStringValue("codec_"+codecParts[0].toLowerCase()+"_"+codecParts[1], newValue);
		}
		//TODO : else raise error
	}
	
	
	public boolean hasCodecPriority(String codecName) {
		String[] codecParts = codecName.split("/");
		if(codecParts.length >=2 ) {
			return prefs.contains("codec_"+codecParts[0].toLowerCase()+"_"+codecParts[1]);
		}
		return false;
	}
	

	/**
	 * Get sip ringtone
	 * @return string uri
	 */
	public String getRingtone() {
		return prefs.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
	}


	public float getMicLevel() {
		return prefs.getFloat(SND_MIC_LEVEL, (float) 1.0);
	}
	
	public float getSpeakerLevel() {
		return prefs.getFloat(SND_SPEAKER_LEVEL, (float) 1.0);
	}
	
	public int getAudioFramePtime() {
		try {
			int value = Integer.parseInt(prefs.getString("snd_ptime", "20"));
			return value;
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 30;
	}


	// ---- 
	// UI related
	// ----
	public boolean getDialPressTone() {
		return prefs.getBoolean("dial_press_tone", false);
	}

	public boolean getDialPressVibrate() {
		return prefs.getBoolean("dial_press_vibrate", true);
	}

	public boolean startIsDigit() {
		return !prefs.getBoolean("start_with_text_dialer", false);
	}

	public boolean getUseAlternateUnlocker() {
		return prefs.getBoolean("use_alternate_unlocker", false);
	}
	
	public boolean useIntegrateDialer() {
		return prefs.getBoolean("integrate_with_native_dialer", true);
	}
	public boolean useIntegrateCallLogs() {
		return prefs.getBoolean("integrate_with_native_calllogs", true);
	}


	public boolean keepAwakeInCall() {
		return prefs.getBoolean(KEEP_AWAKE_IN_CALL, false);
	}

	public float getInitialVolumeLevel() {
		return (float) ((float) (prefs.getFloat("snd_stream_level", (float) 8.0)) / 10.0);
	}

	public boolean usePartialWakeLock() {
		return prefs.getBoolean("use_partial_wake_lock", false);
	}
	

	public boolean integrateWithMusicApp() {
		return prefs.getBoolean("integrate_with_native_music", true);
	}
	
	public int getLogLevel() {
		int defaultValue = 1;
		int prefsValue = 1;
		String logLevel = prefs.getString("log_level", String.valueOf(defaultValue));
		try {
			prefsValue = Integer.parseInt(logLevel);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Audio quality "+logLevel+" not well formated");
		}
		if(prefsValue <= 5 && prefsValue >= 1) {
			return prefsValue;
		}
		return defaultValue;
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

	//Utils
	
	/**
	 * Check TCP/UDP validity of a network port
	 */
	private boolean isValidPort(int port) {
		return (port>0 && port < 65535);
	}

	/**
	 * Get a property from android property subsystem
	 * @param prop property to get
	 * @return the value of the property command line or null if failed
	 */
	private String getSystemProp(String prop) {
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




	
}
