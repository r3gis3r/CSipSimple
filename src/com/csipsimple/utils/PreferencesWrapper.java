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
	
	
	public int getTransportPort() {
		try {
			return Integer.parseInt(prefs.getString("network_transport_port", "5060"));
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Transport port not well formated");
		}
		return 5060;
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
		String default_value = "1";
		if(Build.VERSION.SDK == "3") {
			default_value = "5";
		}
		String autoCloseTime = prefs.getString("snd_auto_close_time", default_value);
		try {
			return Integer.parseInt(autoCloseTime);
		}catch(NumberFormatException e) {
			Log.e(THIS_FILE, "Auto close time "+autoCloseTime+" not well formated");
		}
		return 0;
	}
	
	public boolean hasAutoCancellation() {
		return prefs.getBoolean("echo_cancellation", true);
	}


	public pjsip_transport_type_e getTransportType() {
		String choosenTransport = prefs.getString("network_transport", "UDP");
		if(choosenTransport.equalsIgnoreCase("TCP")) {
			return pjsip_transport_type_e.PJSIP_TRANSPORT_TCP;
		}
		
		return pjsip_transport_type_e.PJSIP_TRANSPORT_UDP;
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


	public String getRingtone() {
		return prefs.getString("ringtone", Settings.System.DEFAULT_RINGTONE_URI.toString());
	}
	
}
