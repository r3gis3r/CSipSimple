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
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PreferencesProviderWrapper {


	private static final String THIS_FILE = "Prefs";
	private ContentResolver resolver;
	private ConnectivityManager connectivityManager;
	private Context context;
	

	public static final String LIB_CAP_TLS = PreferencesWrapper.LIB_CAP_TLS;
	public static final String LIB_CAP_SRTP = PreferencesWrapper.LIB_CAP_SRTP;
	public static final String HAS_BEEN_QUIT = PreferencesWrapper.HAS_BEEN_QUIT;
	public static final String HAS_ALREADY_SETUP_SERVICE = PreferencesWrapper.HAS_ALREADY_SETUP_SERVICE;

	public PreferencesProviderWrapper(Context aContext) {
	    context = aContext;
		resolver = aContext.getContentResolver();
		connectivityManager = (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	

	/**
	 * Set all values to default
	 */
	public void resetAllDefaultValues() {
		Uri uri = SipConfigManager.RAZ_URI;
		resolver.update(uri, new ContentValues(), null, null);
	}
	
	// Api compat part
	public boolean getPreferenceBooleanValue(String string, boolean b) {
        return SipConfigManager.getPreferenceBooleanValue(context, string, b);
    }

    public boolean getPreferenceBooleanValue(String string) {
        return SipConfigManager.getPreferenceBooleanValue(context, string);
    }
    
    public String getPreferenceStringValue(String key) {
        return SipConfigManager.getPreferenceStringValue(context, key);
    }
    public String getPreferenceStringValue(String key, String defaultVal) {
        return SipConfigManager.getPreferenceStringValue(context, key, defaultVal);
    }


    public int getPreferenceIntegerValue(String key) {
        return SipConfigManager.getPreferenceIntegerValue(context, key);
    }

    public float getPreferenceFloatValue(String key) {
        return SipConfigManager.getPreferenceFloatValue(context, key);
    }
    
    public float getPreferenceFloatValue(String key, float f) {
        return SipConfigManager.getPreferenceFloatValue(context, key, f);
    }
    
    public void setPreferenceStringValue(String key, String newValue) {
        SipConfigManager.setPreferenceStringValue(context, key, newValue);
    }

    public void setPreferenceBooleanValue(String key, boolean newValue) {
        SipConfigManager.setPreferenceBooleanValue(context, key, newValue);
    }
    
    public void setPreferenceFloatValue(String key, float newValue) {
        SipConfigManager.setPreferenceFloatValue(context, key, newValue);
    }
	
	// Network part
	
	// Check for wifi
	private boolean isValidWifiConnectionFor(NetworkInfo ni, String suffix) {
		
        boolean valid_for_wifi = getPreferenceBooleanValue("use_wifi_" + suffix, true);
        // We consider ethernet as wifi
        if (valid_for_wifi && ni != null) {
            int type = ni.getType();
            // Wifi connected
            if (ni.isConnected() &&
                    // 9 = ConnectivityManager.TYPE_ETHERNET
                    (type == ConnectivityManager.TYPE_WIFI || type == 9 )) {
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
			 ni != null) {
		    int type = ni.getType();
		    
			// Any mobile network connected
			if (ni.isConnected() && 
			        // Type 3,4,5 are other mobile data ways
			        (type == ConnectivityManager.TYPE_MOBILE || (type <= 5 && type >= 3))) {
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
			return ni.isConnected();
		}
		return false;
	}
	
	private boolean isValidAnywayConnectionFor(NetworkInfo ni, String suffix) {
	    return getPreferenceBooleanValue("use_anyway_" + suffix, false);
        
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
		if(isValidAnywayConnectionFor(ni, suffix)) {
		    Log.d(THIS_FILE, "We are valid ANYWAY");
            return true;
		}
		return false;
	}
	
	/**
	 * Say whether current connection is valid for outgoing calls 
	 * @return true if connection is valid
	 */
	public boolean isValidConnectionForOutgoing() {
	    if(getPreferenceBooleanValue(PreferencesWrapper.HAS_BEEN_QUIT, false)) {
	        // Don't go further, we have been explicitly stopped
	        return false;
	    }
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
	
	
	public boolean useRoutingApi() {
		return getPreferenceBooleanValue(SipConfigManager.USE_ROUTING_API);
	}
	
	public boolean useModeApi() {
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
		
		if(TextUtils.isEmpty(ringtone)) {
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
	
	
	private int getKeepAliveInterval(String wifi_key, String mobile_key) {
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        if(ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
            return getPreferenceIntegerValue(wifi_key);
        }
        return getPreferenceIntegerValue(mobile_key);
    }
	
	/**
	 * Retrieve UDP keep alive interval for the current connection
	 * @return KA Interval in second 
	 */
	public int getUdpKeepAliveInterval() {
		return getKeepAliveInterval(SipConfigManager.KEEP_ALIVE_INTERVAL_WIFI, SipConfigManager.KEEP_ALIVE_INTERVAL_MOBILE);
	}

    /**
     * Retrieve TCP keep alive interval for the current connection
     * @return KA Interval in second 
     */
    public int getTcpKeepAliveInterval() {
        return getKeepAliveInterval(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_WIFI, SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_MOBILE);
    }

    /**
     * Retrieve TLS keep alive interval for the current connection
     * @return KA Interval in second 
     */
    public int getTlsKeepAliveInterval() {
        return getKeepAliveInterval(SipConfigManager.TLS_KEEP_ALIVE_INTERVAL_WIFI, SipConfigManager.TLS_KEEP_ALIVE_INTERVAL_MOBILE);
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
		return SipConfigManager.HEADSET_ACTION_CLEAR_CALL;
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
	
	/**
	 * Setup codecs list
	 * Should be only done by the service that get infos from the sip stack(s)
	 * @param codecs the list of codecs
	 */
	public void setCodecList(List<String> codecs) {
		if(codecs != null) {
			setPreferenceStringValue(PreferencesWrapper.CODECS_LIST, TextUtils.join(PreferencesWrapper.CODECS_SEPARATOR, codecs));
		}
	}
	
    public void setVideoCodecList(List<String> codecs) {
        if(codecs != null) {
            setPreferenceStringValue(PreferencesWrapper.CODECS_VIDEO_LIST, TextUtils.join(PreferencesWrapper.CODECS_SEPARATOR, codecs));
        }
    }
	
	public void setLibCapability(String cap, boolean canDo) {
		setPreferenceBooleanValue(PreferencesWrapper.BACKUP_PREFIX + cap, canDo);
	}



    // DTMF
	
	public boolean useSipInfoDtmf() {
		return (getPreferenceIntegerValue(SipConfigManager.DTMF_MODE) == SipConfigManager.DTMF_MODE_INFO);
	}
	
	public boolean forceDtmfInBand() {
		return (getPreferenceIntegerValue(SipConfigManager.DTMF_MODE) == SipConfigManager.DTMF_MODE_INBAND);
	}

	public boolean forceDtmfRTP() {
		return (getPreferenceIntegerValue(SipConfigManager.DTMF_MODE) == SipConfigManager.DTMF_MODE_RTP);
	}
	
	
	// Codecs
	

    /**
     * Get the codec priority
     * 
     * @param codecName codec name formated in the pjsip format (the
     *            corresponding pref is
     *            codec_{{lower(codecName)}}_{{codecFreq}})
     * @param defaultValue the default value if the pref is not found MUST be
     *            casteable as Integer/short
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
	
    /**
     * Set the priority for the codec for a given bandwidth type
     * 
     * @param codecName the name of the codec as announced by codec
     * @param type bandwidth type <br/>
     *            For now, valid constants are :
     *            {@link SipConfigManager#CODEC_NB} and
     *            {@link SipConfigManager#CODEC_WB}
     * @param newValue Short value for preference as a string.
     */
	public void setCodecPriority(String codecName, String type, String newValue) {
		String key = SipConfigManager.getCodecKey(codecName, type); 
		if(key != null) {
			setPreferenceStringValue(key, newValue);
		}
		//TODO : else raise error
	}
	

	public static File getRecordsFolder(Context ctxt) {
		return PreferencesWrapper.getRecordsFolder(ctxt);
	}
}
