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

import org.pjsip.pjsua.pjsip_transport_type_e;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class PreferencesWrapper {
	
	private static final String THIS_FILE = "PreferencesWrapper";
	private SharedPreferences prefs;
	private ConnectivityManager connectivityManager;
	
	public PreferencesWrapper(Context aContext) {
		prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	
	// Network part 
	
	//Private generic function for both incoming and outgoing
	private boolean isValidConnectionFor(String suffix) {
		// Check for gsm
		boolean valid_for_gsm = prefs.getBoolean("use_3g_" + suffix, false);
		NetworkInfo ni;
		if (valid_for_gsm) {
			ni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}

		// Check for wifi
		boolean valid_for_wifi = prefs.getBoolean("use_wifi_" + suffix, true);
		if (valid_for_wifi) {
			ni = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Say whether current connection is valid for outgoing calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForOutgoing() {
		return isValidConnectionFor("out");
	}

	/**
	 * Say whether current connection is valid for incoming calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForIncoming() {
		return isValidConnectionFor("in");
	}

	
	public boolean getLockWifi() {
		return prefs.getBoolean("lock_wifi", true);
	}
	
	
	//Media part
	
	/**
	 * Get auto close time after end of the call
	 * To avoid crash after hangup -- android 1.5 only but
	 * even sometimes crash
	 */
	public int getAutoCloseTime() {
		String defaultValue = "1";
		if(Build.VERSION.SDK == "3") {
			defaultValue = "5";
		}
		String autoCloseTime = prefs.getString("snd_auto_close_time", defaultValue);
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
		if(Build.VERSION.SDK == "3") {
			return false;
		}
		return prefs.getBoolean("echo_cancellation", true);
	}
	

	/**
	 * Whether voice audio detection is enabled
	 * @return 1 if Voice audio detection is disabled
	 */
	public int getNoVad() {
		return prefs.getBoolean("enable_vad", true)?0:1;
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
		String clockRate = prefs.getString("snd_clock_rate", "16000");
		try {
			return Integer.parseInt(clockRate);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Clock rate "+clockRate+" not well formated");
		}
		return 16000;
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
	
	
	
	
	public boolean isTCPEnabled() {
		return prefs.getBoolean("enable_tcp", false);
	}
	
	public boolean isUDPEnabled() {
		return prefs.getBoolean("enable_udp", true);
	}
	
	public int getTCPTransportPort() {
		try {
			return Integer.parseInt(prefs.getString("network_tcp_transport_port", "5060"));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5060;
	}
	
	public int getUDPTransportPort() {
		try {
			return Integer.parseInt(prefs.getString("network_udp_transport_port", "5060"));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5060;
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
			Editor editor = prefs.edit();
			editor.putString("codec_"+codecParts[0].toLowerCase()+"_"+codecParts[1], newValue);
			editor.commit();
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




	// ---- 
	// UI related
	// ----
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
}
