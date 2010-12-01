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
import java.util.HashMap;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsua;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;


public class PreferencesWrapper {
	
	
	public static final String TURN_SERVER = "turn_server";
	public static final String ENABLE_TURN = "enable_turn";
	public static final String ENABLE_ICE = "enable_ice";
	public static final String SND_MEDIA_QUALITY = "snd_media_quality";
	public static final String ECHO_CANCELLATION_TAIL = "echo_cancellation_tail";
	public static final String USER_AGENT = "user_agent";
	public static final String RTP_PORT = "network_rtp_port";
	public static final String TCP_TRANSPORT_PORT = "network_tcp_transport_port";
	public static final String UDP_TRANSPORT_PORT = "network_udp_transport_port";
	public static final String IS_ADVANCED_USER = "is_advanced_user";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";
	public static final String SND_AUTO_CLOSE_TIME = "snd_auto_close_time";
	public static final String SND_CLOCK_RATE = "snd_clock_rate";
	public static final String ECHO_CANCELLATION = "echo_cancellation";
	public static final String ENABLE_VAD = "enable_vad";
	public static final String KEEP_AWAKE_IN_CALL = "keep_awake_incall";
	public static final String SND_MIC_LEVEL = "snd_mic_level";
	public static final String SND_SPEAKER_LEVEL = "snd_speaker_level";
	public static final String HAS_IO_QUEUE = "has_io_queue";
	public static final String BITS_PER_SAMPLE = "bits_per_sample";
	
	public static final String USE_SOFT_VOLUME = "use_soft_volume";
	public static final String PREVENT_SCREEN_ROTATION = "prevent_screen_rotation";
	public static final String LOG_LEVEL = "log_level";
	public static final String DTMF_MODE = "dtmf_mode";
	public static final String USE_ROUTING_API = "use_routing_api";
	public static final String USE_MODE_API = "use_mode_api";
	public static final String SIP_AUDIO_MODE = "sip_audio_mode";
	
	public static final String ICON_IN_STATUS_BAR = "icon_in_status_bar";
	
	// NETWORK
	public static final String ENABLE_STUN = "enable_stun";
	public static final String STUN_SERVER = "stun_server";
	public static final String USE_IPV6 = "use_ipv6";
	public static final String ENABLE_UDP = "enable_udp";
	public static final String ENABLE_TCP = "enable_tcp";
	public static final String LOCK_WIFI = "lock_wifi";
	public static final String ENABLE_DNS_SRV = "enable_dns_srv";
	public static final String ENABLE_QOS = "enable_qos";
	public static final String DSCP_VAL = "dscp_val";
	
	// SECURE
	public static final String ENABLE_TLS = "enable_tls";
	public static final String TLS_TRANSPORT_PORT = "network_tls_transport_port";
//	public static final String TLS_SERVER_NAME = "network_tls_server_name";
//	public static final String CA_LIST_FILE = "ca_list_file";
//	public static final String CERT_FILE = "cert_file";
//	public static final String PRIVKEY_FILE = "privkey_file";
//	public static final String TLS_PASSWORD = "tls_password";
	public static final String TLS_VERIFY_SERVER = "tls_verify_server";
//	public static final String TLS_VERIFY_CLIENT = "tls_verify_client";
	public static final String TLS_METHOD = "tls_method";
	public static final String USE_SRTP = "use_srtp";
	
	//Internal use
	public static final String HAS_BEEN_QUIT = "has_been_quit"; 
	
	
	private static final String THIS_FILE = "PreferencesWrapper";
	private SharedPreferences prefs;
	private ConnectivityManager connectivityManager;
	private ContentResolver resolver;

	
	
	private final static HashMap<String, String> STRING_PREFS = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;
	{
		
		put(USER_AGENT, CustomDistribution.getUserAgent());
		put(LOG_LEVEL, "1");
		
		put(USE_SRTP, "0");
		put(UDP_TRANSPORT_PORT, "5060");
		put(TCP_TRANSPORT_PORT, "5060");
		put(TLS_TRANSPORT_PORT, "5061");
		put(RTP_PORT, "4000");
		put(SND_AUTO_CLOSE_TIME, "1");
		put(ECHO_CANCELLATION_TAIL, "200");
		put(SND_MEDIA_QUALITY, "4");
		put(SND_CLOCK_RATE, "16000");
		put(BITS_PER_SAMPLE, "16");
		put(SIP_AUDIO_MODE, "0");
		
		put(STUN_SERVER, "stun.counterpath.com");
		put(TURN_SERVER, "");
//		put(TLS_SERVER_NAME, "");
//		put(CA_LIST_FILE, "");
//		put(CERT_FILE, "");
//		put(PRIVKEY_FILE, "");
//		put(TLS_PASSWORD, "");
		put(TLS_METHOD, "0");
		
		put(DSCP_VAL, "26");
		put(DTMF_MODE, "0");
		
		
	}};
	
	
	private final static HashMap<String, Boolean> BOOLEAN_PREFS = new HashMap<String, Boolean>(){
		private static final long serialVersionUID = 1L;
	{
		put(LOCK_WIFI, true);
		put(ENABLE_TCP, true);
		put(ENABLE_UDP, true);
		put(ENABLE_TLS, false);
		put(USE_IPV6, false);
		put(ENABLE_DNS_SRV, false);
		put(ENABLE_ICE, false);
		put(ENABLE_TURN, false);
		put(ENABLE_STUN, false);
		
		put(ECHO_CANCELLATION, true);
		put(ENABLE_VAD, false);
		put(USE_SOFT_VOLUME, false);
		put(USE_ROUTING_API, false);
		put(USE_MODE_API, false);
		put(HAS_IO_QUEUE, false);
		
		put(PREVENT_SCREEN_ROTATION, true);
		put(ICON_IN_STATUS_BAR, true);
		
		put(TLS_VERIFY_SERVER, false);
//		put(TLS_VERIFY_CLIENT, false);
		
		put(ENABLE_QOS, true);
		
		//Network
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
	}};
	
	private final static HashMap<String, Float> FLOAT_PREFS = new HashMap<String, Float>(){
		private static final long serialVersionUID = 1L;
	{
		put(SND_MIC_LEVEL, (float)1.0);
		put(SND_SPEAKER_LEVEL, (float)1.0);
	}};
	
	
	public PreferencesWrapper(Context aContext) {
		prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		resolver = aContext.getContentResolver();
		
	}
	

	//Public setters
	public void setPreferenceStringValue(String key, String value) {
		//TODO : authorized values
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
	
	//Private static getters
	private static String gPrefStringValue(SharedPreferences aPrefs, String key) {
		if(STRING_PREFS.containsKey(key)) {
			return aPrefs.getString(key, STRING_PREFS.get(key));
		}
		return null;
	}
	
	private static Boolean gPrefBooleanValue(SharedPreferences aPrefs, String key) {
		if(BOOLEAN_PREFS.containsKey(key)) {
			return aPrefs.getBoolean(key, BOOLEAN_PREFS.get(key));
		}
		return null;
	}
	
	private static Float gPrefFloatValue(SharedPreferences aPrefs, String key) {
		if(FLOAT_PREFS.containsKey(key)) {
			return aPrefs.getFloat(key, FLOAT_PREFS.get(key));
		}
		return null;
	}
	
	//Public getters
	public String getPreferenceStringValue(String key) {
		return gPrefStringValue(prefs, key);
	}
	
	public Boolean getPreferenceBooleanValue(String key) {
		return gPrefBooleanValue(prefs, key);
	}
	
	public Float getPreferenceFloatValue(String key) {
		return gPrefFloatValue(prefs, key);
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
	
	public boolean getLockWifi() {
		return getPreferenceBooleanValue(LOCK_WIFI);
	}
	
	public pjmedia_srtp_use getUseSrtp() {
		try {
			int use_srtp = Integer.parseInt(getPreferenceStringValue(USE_SRTP));
			pjmedia_srtp_use.swigToEnum(use_srtp);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return pjmedia_srtp_use.PJMEDIA_SRTP_DISABLED;
	}
	
	public boolean isTCPEnabled() {
		return getPreferenceBooleanValue(ENABLE_TCP);
	}
	
	public boolean isUDPEnabled() {
		return getPreferenceBooleanValue(ENABLE_UDP);
	}

	public boolean isTLSEnabled() {
		return getPreferenceBooleanValue(ENABLE_TLS);
	}
	
	public boolean useIPv6() {
		return getPreferenceBooleanValue(USE_IPV6);
	}
	
	private int getPrefPort(String key) {
		try {
			int port = Integer.parseInt(getPreferenceStringValue(key));
			if(isValidPort(port)) {
				return port;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return Integer.parseInt(STRING_PREFS.get(key));
	}
	
	public int getUDPTransportPort() {
		return getPrefPort(UDP_TRANSPORT_PORT);
	}
	
	public int getTCPTransportPort() {
		return getPrefPort(TCP_TRANSPORT_PORT);
	}
	
	public int getTLSTransportPort() {
		return getPrefPort(TLS_TRANSPORT_PORT);
	}
	
	public int getRTPPort() {
		return getPrefPort(RTP_PORT);
	}
	
	public boolean enableDNSSRV() {
		return getPreferenceBooleanValue(ENABLE_DNS_SRV);
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
	
	public int getDSCPVal() {
		try {
			return Integer.parseInt(getPreferenceStringValue(DSCP_VAL));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "DSCP_VAL not well formated");
		}
		return Integer.parseInt(STRING_PREFS.get(DSCP_VAL));
	}
	
	public int getTLSMethod() {
		try {
			return Integer.parseInt(getPreferenceStringValue(TLS_METHOD));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "TLS not well formated");
		}
		return Integer.parseInt(STRING_PREFS.get(TLS_METHOD));
	}
	
	private boolean hasStunServer(String string) {
		String[] servers = getPreferenceStringValue(PreferencesWrapper.STUN_SERVER).split(",");
		for(String server : servers) {
			if(server.equalsIgnoreCase(string)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void addStunServer(String server) {
		if(!hasStunServer(server)) {
			setPreferenceStringValue(PreferencesWrapper.STUN_SERVER, getPreferenceStringValue(PreferencesWrapper.STUN_SERVER)+","+server);
		}
		
	}

	public String getUserAgent() {
		return getPreferenceStringValue(USER_AGENT);
	}
	
	
	//Media part
	
	/**
	 * Get auto close time after end of the call
	 * To avoid crash after hangup -- android 1.5 only but
	 * even sometimes crash
	 */
	public int getAutoCloseTime() {
		String autoCloseTime = getPreferenceStringValue(SND_AUTO_CLOSE_TIME);
		try {
			return Integer.parseInt(autoCloseTime);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Auto close time "+autoCloseTime+" not well formated");
		}
		return 1;
	}
	
	
	/**
	 * Whether echo cancellation is enabled
	 * @return true if enabled
	 */
	public boolean hasEchoCancellation() {
		return getPreferenceBooleanValue(ECHO_CANCELLATION);
	}
	

	public long getEchoCancellationTail() {
		if(!hasEchoCancellation()) {
			return 0;
		}
		String tailLength = getPreferenceStringValue(ECHO_CANCELLATION_TAIL);
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
		return getPreferenceBooleanValue(ENABLE_VAD) ?0:1;
	}

	
	/**
	 * Get the audio codec quality setting
	 * @return the audio quality
	 */
	public long getMediaQuality() {
		String mediaQuality = getPreferenceStringValue(SND_MEDIA_QUALITY);
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
			return Integer.parseInt(getPreferenceStringValue(BITS_PER_SAMPLE));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Bits per sample not well formated");
		}
		return Integer.parseInt(STRING_PREFS.get(BITS_PER_SAMPLE));
	}
	
	/**
	 * Get the audio codec quality setting
	 * @return the audio quality
	 */
	public int getInCallMode() {
		String mode = getPreferenceStringValue(SIP_AUDIO_MODE);
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
		String clockRate = getPreferenceStringValue(SND_CLOCK_RATE);
		try {
			return Integer.parseInt(clockRate);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Clock rate "+clockRate+" not well formated");
		}
		return 16000;
	}
	
	
	public boolean getUseRoutingApi() {
		return getPreferenceBooleanValue(USE_ROUTING_API);
	}
	
	public boolean getUseModeApi() {
		return getPreferenceBooleanValue(USE_MODE_API);
	}
	
	/**
	 * Get whether ice is enabled
	 * @return 1 if enabled (pjstyle)
	 */
	public int getIceEnabled() {
		return getPreferenceBooleanValue(ENABLE_ICE)?1:0;
	}

	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getTurnEnabled() {
		return getPreferenceBooleanValue(ENABLE_TURN)?1:0;
	}
	
	/**
	 * Get stun server
	 * @return host:port or blank if not set
	 */
	public String getStunServer() {
		return getPreferenceStringValue(STUN_SERVER);
	}
	
	
	/**
	 * Get whether turn is enabled
	 * @return 1 if enabled (pjstyle)
	 */ 
	public int getStunEnabled() {
		return getPreferenceBooleanValue(ENABLE_STUN)?1:0;
	}
	
	/**
	 * Get turn server
	 * @return host:port or blank if not set
	 */
	public String getTurnServer() {
		return getPreferenceStringValue(TURN_SERVER);
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
		String ringtone = prefs.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
		
		if(ringtone == null || TextUtils.isEmpty(ringtone)) {
			ringtone = Settings.System.DEFAULT_RINGTONE_URI.toString();
		}
		return ringtone;
	}


	public float getMicLevel() {
		return getPreferenceFloatValue(SND_MIC_LEVEL);
	}
	
	public float getSpeakerLevel() {
		return getPreferenceFloatValue(SND_SPEAKER_LEVEL);
	}
	
	public int getAudioFramePtime() {
		try {
			int value = Integer.parseInt(prefs.getString("snd_ptime", "20"));
			return value;
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "snd_ptime not well formated");
		}
		return 30;
	}
	
	public int getHasIOQueue() {
		return getPreferenceBooleanValue(HAS_IO_QUEUE)?1:0;
	}
	
	

	public boolean useSipInfoDtmf() {
		return getPreferenceStringValue(DTMF_MODE).equalsIgnoreCase("3");
	}
	
	public boolean forceDtmfInBand() {
		return getPreferenceStringValue(DTMF_MODE).equalsIgnoreCase("2");
	}

	public boolean forceDtmfRTP() {
		return getPreferenceStringValue(DTMF_MODE).equalsIgnoreCase("1");
	}


	public long getThreadCount() {
		try {
			int value = Integer.parseInt(prefs.getString("thread_count", "1"));
			if(value < 10) {
				return value;
			}
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Thread count not well formatted");
		}
		return 1;
	}

	// ---- 
	// UI related
	// ----
	public boolean getDialPressTone() {
		if(prefs.getBoolean("dial_press_tone", false)) {
			return Settings.System.getInt(resolver,
	                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
		}
		return false;
	}

	public boolean getDialPressVibrate() {
		if(prefs.getBoolean("dial_press_tone", false)) {
			return Settings.System.getInt(resolver,
	                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
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
		int prefsValue = 1;
		String logLevel = getPreferenceStringValue(LOG_LEVEL);
		try {
			prefsValue = Integer.parseInt(logLevel);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Audio quality "+logLevel+" not well formated");
		}
		if(prefsValue <= 5 && prefsValue >= 1) {
			return prefsValue;
		}
		return 1;
	}
	
	public boolean showIconInStatusBar() {
		return getPreferenceBooleanValue(ICON_IN_STATUS_BAR);
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




	




}
