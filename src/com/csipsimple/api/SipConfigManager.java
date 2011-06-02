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
 *  
 *  This file and this file only is released under dual Apache license
 */
package com.csipsimple.api;

import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

public class SipConfigManager {

	//Media
	public static final String SND_MEDIA_QUALITY = "snd_media_quality";
	public static final String ECHO_CANCELLATION_TAIL = "echo_cancellation_tail";
	public static final String RTP_PORT = "network_rtp_port";
	public static final String TCP_TRANSPORT_PORT = "network_tcp_transport_port";
	public static final String UDP_TRANSPORT_PORT = "network_udp_transport_port";
	public static final String SND_AUTO_CLOSE_TIME = "snd_auto_close_time";
	public static final String SND_CLOCK_RATE = "snd_clock_rate";
	public static final String ECHO_CANCELLATION = "echo_cancellation";
	public static final String ENABLE_VAD = "enable_vad";
	public static final String SND_MIC_LEVEL = "snd_mic_level";
	public static final String SND_SPEAKER_LEVEL = "snd_speaker_level";
	public static final String SND_BT_MIC_LEVEL = "snd_bt_mic_level";
	public static final String SND_BT_SPEAKER_LEVEL = "snd_bt_speaker_level";
	public static final String HAS_IO_QUEUE = "has_io_queue";
	public static final String BITS_PER_SAMPLE = "bits_per_sample";
	public static final String SET_AUDIO_GENERATE_TONE = "set_audio_generate_tone";
	public static final String THREAD_COUNT = "thread_count";
	public static final String ECHO_MODE = "echo_mode";
	public static final String SND_PTIME = "snd_ptime";
	public static final String USE_SGS_CALL_HACK = "use_sgs_call_hack";
	public static final String DTMF_MODE = "dtmf_mode";
	public static final String USE_ROUTING_API = "use_routing_api";
	public static final String USE_MODE_API = "use_mode_api";
	public static final String SIP_AUDIO_MODE = "sip_audio_mode";
	public static final String MICRO_SOURCE = "micro_source";
	public static final String USE_WEBRTC_HACK = "use_webrtc_hack";
	public static final String DO_FOCUS_AUDIO = "do_focus_audio";
	
	
	//UI
	public static final String USE_SOFT_VOLUME = "use_soft_volume";
	public static final String PREVENT_SCREEN_ROTATION = "prevent_screen_rotation";
	public static final String LOG_LEVEL = "log_level";
	public static final String THEME = "selected_theme";
	public static final String ICON_IN_STATUS_BAR = "icon_in_status_bar";
	public static final String ICON_IN_STATUS_BAR_NBR = "icon_in_status_bar_nbr";
	public static final String KEEP_AWAKE_IN_CALL = "keep_awake_incall";
	public static final String GSM_INTEGRATION_TYPE = "gsm_integration_type";
	public static final String DIAL_PRESS_TONE_MODE = "dial_press_tone_mode";
	public static final String DIAL_PRESS_VIBRATE_MODE = "dial_press_vibrate_mode";
	public static final String INVERT_PROXIMITY_SENSOR = "invert_proximity_sensor";
	public static final String USE_PARTIAL_WAKE_LOCK = "use_partial_wake_lock";
	
	
	// NETWORK
	public static final String TURN_SERVER = "turn_server";
	public static final String ENABLE_TURN = "enable_turn";
	public static final String TURN_USERNAME = "turn_username";
	public static final String TURN_PASSWORD = "turn_password";
	public static final String ENABLE_ICE = "enable_ice";
	public static final String ENABLE_STUN = "enable_stun";
	public static final String STUN_SERVER = "stun_server";
	public static final String USE_IPV6 = "use_ipv6";
	public static final String ENABLE_UDP = "enable_udp";
	public static final String ENABLE_TCP = "enable_tcp";
	public static final String LOCK_WIFI = "lock_wifi";
	public static final String LOCK_WIFI_PERFS = "lock_wifi_perfs";
	public static final String ENABLE_DNS_SRV = "enable_dns_srv";
	public static final String ENABLE_QOS = "enable_qos";
	public static final String DSCP_VAL = "dscp_val";
	public static final String KEEP_ALIVE_INTERVAL_WIFI = "keep_alive_interval_wifi";
	public static final String KEEP_ALIVE_INTERVAL_MOBILE = "keep_alive_interval_mobile";
	public static final String OVERRIDE_NAMESERVER = "override_nameserver";
	public static final String USE_COMPACT_FORM = "use_compact_form";
	public static final String USER_AGENT = "user_agent"; 
	public static final String KEEP_ALIVE_USE_WAKE = "ka_use_wake";
	
	
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
	public static final String USE_ZRTP = "use_zrtp";
	
	// CALLS
	public static final String AUTO_RECORD_CALLS = "auto_record_calls";
	public static final String DEFAULT_CALLER_ID = "default_caller_id";
	public static final String SUPPORT_MULTIPLE_CALLS = "support_multiple_calls";
	
	public static final String CODEC_NB = "nb";
	public static final String CODEC_WB = "wb";
	
	
	
	
	public static String getCodecKey(String codecName, String type) {
		String[] codecParts = codecName.split("/");
		String preferenceKey = null;
		if(codecParts.length >=2 ) {
			return "codec_" + codecParts[0].toLowerCase() + "_" + codecParts[1] + "_" + type;
		}
		return preferenceKey;
	}
	
	private static String keyForNetwork(int networkType, int subType) {
		if(networkType == ConnectivityManager.TYPE_WIFI) {
			return "wifi";
		}else if(networkType == ConnectivityManager.TYPE_MOBILE) {
			// 3G (or better)
			if (subType >= TelephonyManager.NETWORK_TYPE_UMTS) {
				return "3g";
			}
			
			// GPRS (or unknown)
			if ( subType == TelephonyManager.NETWORK_TYPE_GPRS || subType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
				return "gprs";
			}
			
			// EDGE
			if ( subType == TelephonyManager.NETWORK_TYPE_EDGE) {
				return "edge";
			}
		}
		
		return "other";
	}
	
	public static String getBandTypeKey(int networkType, int subType) {
		return "band_for_" + keyForNetwork(networkType, subType);
	}
	
	
}
