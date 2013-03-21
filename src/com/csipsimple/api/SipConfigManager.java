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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */

package com.csipsimple.api;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaRecorder.AudioSource;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;

/**
 * Manage global configuration of the application.<br/>
 * Provides wrapper around preference content provider and define preference
 * keys constants
 */
public class SipConfigManager {

    // Media
    /**
     * Media quality, 0-10.<br/>
     * according to this table: 5-10: resampling use large filter<br/>
     * 3-4: resampling use small filter<br/>
     * 1-2: resampling use linear.<br/>
     * The media quality also sets speex codec quality/complexity to the number.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a4cada00a781ce06cd536c4e56a522065"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String SND_MEDIA_QUALITY = "snd_media_quality";
    /**
     * Echo canceller tail length, in miliseconds.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a82c1cf18d42f5ec0a645ed2bdd6ae955"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String ECHO_CANCELLATION_TAIL = "echo_cancellation_tail";
    /**
     * Starting RTP port number.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String RTP_PORT = "network_rtp_port";
    /**
     * Port to use for TCP transport.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TCP_TRANSPORT_PORT = "network_tcp_transport_port";
    /**
     * Port to use for UDP transport.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String UDP_TRANSPORT_PORT = "network_udp_transport_port";
    /**
     * Specify idle time of sound device before it is automatically closed, in
     * seconds. <br/>
     * Use value -1 to disable the auto-close feature of sound device<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a2c95e5ce554bbee9cc60d0328f508658"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String SND_AUTO_CLOSE_TIME = "snd_auto_close_time";
    /**
     * Clock rate to be applied to the conference bridge.<br/>
     * If value is zero, default clock rate will be used
     * (PJSUA_DEFAULT_CLOCK_RATE, which by default is 16KHz).<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a24792c277d6c6c309eccda9047f641a5"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String SND_CLOCK_RATE = "snd_clock_rate";
    /**
     * Enable echo cancellation ?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ECHO_CANCELLATION = "echo_cancellation";
    /**
     * Enable VAD?<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a9f99f0f3d10e14a7a0f75c7f2da8473b"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_VAD = "enable_vad";
    /**
     * Enable noise suppression ?
     * Only working if echo cancellation activated and webRTC echo canceller backend used.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_NOISE_SUPPRESSION = "enable_ns";
    /**
     * Default micro amplification between 0.0 and 10.0.
     * 
     * @see #setPreferenceFloatValue(Context, String, Float)
     */
    public static final String SND_MIC_LEVEL = "snd_mic_level";
    /**
     * Default speaker amplification between 0.0 and 10.0.
     * 
     * @see #setPreferenceFloatValue(Context, String, Float)
     */
    public static final String SND_SPEAKER_LEVEL = "snd_speaker_level";

    /**
     * Default Bluethooth micro amplification between 0.0 and 10.0.
     * 
     * @see #setPreferenceFloatValue(Context, String, Float)
     */
    public static final String SND_BT_MIC_LEVEL = "snd_bt_mic_level";

    /**
     * Default Bluethooth speaker amplification between 0.0 and 10.0.
     * 
     * @see #setPreferenceFloatValue(Context, String, Float)
     */
    public static final String SND_BT_SPEAKER_LEVEL = "snd_bt_speaker_level";
    /**
     * This option is not used anymore because requires multiple working thread
     * that is not suitable for mobility mode. <br/>
     * Specify whether the media manager should manage its own ioqueue for the
     * RTP/RTCP sockets. <br/>
     * If yes, ioqueue will be created and at least one worker thread will be
     * created too. <br/>
     * If no, the RTP/RTCP sockets will share the same ioqueue as SIP sockets,
     * and no worker thread is needed.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#ab1ddd57bc94ed7f5a64c819414cb9f96"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String HAS_IO_QUEUE = "has_io_queue";
    /**
     * Media thread count
     */
    public static final String MEDIA_THREAD_COUNT = "media_thread_count";

    /**
     * Sip stack thread count
     */
    public static final String THREAD_COUNT = "thread_count";
    /**
     * Backend for echo cancellation. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a734653d7e5d075984b9a51f053ded879"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #ECHO_MODE_AUTO
     * @see #ECHO_MODE_SIMPLE
     * @see #ECHO_MODE_SPEEX
     * @see #ECHO_MODE_WEBRTC_M
     */
    public static final String ECHO_MODE = "echo_mode";
    /**
     * Specify audio frame ptime. <br/>
     * The value here will affect the samples per frame of both the sound device
     * and the conference bridge. <br/>
     * Specifying lower ptime will normally reduce the latency.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#ac6e637f5fdd868c8e77a1d1f5e9d1a51"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String SND_PTIME = "snd_ptime";
    /**
     * How to send DTMF codes.<br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #DTMF_MODE_AUTO
     * @see #DTMF_MODE_INBAND
     * @see #DTMF_MODE_INFO
     * @see #DTMF_MODE_RTP
     */
    public static final String DTMF_MODE = "dtmf_mode";
    /**
     * Pause time in ms of DTMF , separator.<br/>
     * 
     * Default is 300ms
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DTMF_PAUSE_TIME = "dtmf_pause_time";
    /**
     * Pause time in ms of DTMF , separator.<br/>
     * 
     * Default is 2000ms
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DTMF_WAIT_TIME = "dtmf_wait_time";
    /**
     * Should the application use samsung galaxy S hack to establish audio?<br/>
     * Basically it starts opening audio in {@link AudioManager#MODE_IN_CALL}
     * and then {@link AudioManager#MODE_NORMAL} to have things routed to
     * earpiece<br/>
     * Leave it managed by CSipSimple if you want to benefit auto detection
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_SGS_CALL_HACK = "use_sgs_call_hack";
    /**
     * Should we generate a silent tone just after the audio is established as a workaround to some devices.<br/>
     * This is useful for some samsung devices. <br/>
     * Leave it managed by CSipSimple if you want to benefit auto detection
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String SET_AUDIO_GENERATE_TONE = "set_audio_generate_tone";
    /**
     * Should the application use the android legacy route api to route to speaker/earpiece?<br/>
     * Leave it managed by CSipSimple if you want to benefit auto detection
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_ROUTING_API = "use_routing_api";
    /**
     * Should the application use android {@link AudioManager#MODE_IN_CALL} and
     * {@link AudioManager#MODE_NORMAL} modes to route to speaker/earpiece?<br/>
     * Leave it managed by CSipSimple if you want to benefit auto detection
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_MODE_API = "use_mode_api";
    /**
     * Which mode to use when in a sip call.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see AudioManager#MODE_IN_CALL
     * @see AudioManager#MODE_IN_COMMUNICATION
     * @see AudioManager#MODE_NORMAL
     * @see AudioManager#MODE_RINGTONE
     */
    public static final String SIP_AUDIO_MODE = "sip_audio_mode";
    /**
     * Which audio source to use when in a sip call.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see AudioSource#DEFAULT
     * @see AudioSource#MIC
     * @see AudioSource#VOICE_CALL
     * @see AudioSource#VOICE_COMMUNICATION
     * @see AudioSource#VOICE_DOWNLINK
     * @see AudioSource#VOICE_RECOGNITION
     * @see AudioSource#VOICE_UPLINK
     */
    public static final String MICRO_SOURCE = "micro_source";
    /**
     * Should the application use webRTC library code to setup audio.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_WEBRTC_HACK = "use_webrtc_hack";
    /**
     * Should we focus audio stream used by the application.<br/>
     * It will for example allows to mute music app while in call
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String DO_FOCUS_AUDIO = "do_focus_audio";
    /**
     * Level of android audio stream when starting call.<br/>
     * This is the android audio level. <br/>
     * Between 0.0 and 1.0
     * 
     * @see #setPreferenceFloatValue(Context, String, Float)
     */
    public static final String SND_STREAM_LEVEL = "snd_stream_level";
    /**
     * Action to perform when headset button is pressed.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #HEADSET_ACTION_CLEAR_CALL
     * @see #HEADSET_ACTION_HOLD
     * @see #HEADSET_ACTION_MUTE
     */
    public static final String HEADSET_ACTION = "headset_action";
    /**
     * Have per bandwidth speed codecs lists ?<br/>
     * If true the user can manage one list per bandwidth speed (fast/slow)
     */
    public static final String CODECS_PER_BANDWIDTH = "codecs_per_bandwidth";
    /**
     * Backend implementation to use for audio calls.<br/>
     * Since android has several ways to plug to audio layer <br/>
     * And CSipSimple has different implementations for each ways, the backend
     * can be configured
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #AUDIO_IMPLEMENTATION_JAVA
     * @see #AUDIO_IMPLEMENTATION_OPENSLES
     */
    public static final String AUDIO_IMPLEMENTATION = "audio_implementation";
    /**
     * Should we automatically connect audio to bluetooth SCO if activated.<br/>
     * Depending on the device it may be introduce buggy routing <br/>
     * Not all manufacturers implements the Bluetooth SCO so that it
     * automatically ignore it if no device is paired
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String AUTO_CONNECT_BLUETOOTH = "auto_connect_bluetooth";
    /**
     * Should we automatically connect audio to speaker when call becomes active.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String AUTO_CONNECT_SPEAKER = "auto_connect_speaker";
    /**
     * Should we activate speaker automatically based on proximity and screen orientation.<br/>
     * The speaker will be automatically turned on when phone is horizontal and off when vertical.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String AUTO_DETECT_SPEAKER = "auto_detect_speaker";
    
    /**
     * Should the entire audio stream be restarted when audio routing change is asked ?<br/>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String RESTART_AUDIO_ON_ROUTING_CHANGES = "restart_aud_on_routing_change";
    
    /**
     * Should audio routing be done before media stream start ? <br/>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String SETUP_AUDIO_BEFORE_INIT = "setup_audio_before_init";

    /**
     * Suffix key for the number of frames per RTP packet for one codec. <br/>
     * To be prefixed with {codec rtp name}_{codec clock rate}_.
     * You can use {@link #getCodecKey(String, String)} if you have codec in form G729/8000 for example.
     */
    public static final String FRAMES_PER_PACKET_SUFFIX = "fpp";
    
    /**
     * H264 Codec profile.<br/>
     * 66 : baseline
     * 77 : mainline 
     */
    public static final String H264_PROFILE = "codec_h264_profile";
    /**
     * H264 Codec level.<br/>
     * 10 for 1.0, 20 for 2.0, 31 for 3.1 etc
     */
    public static final String H264_LEVEL = "codec_h264_level";
    /**
     * H264 Codec bitrate in kbps.<br/>
     * Use 0 for default bitrate for level.
     */
    public static final String H264_BITRATE = "codec_h264_bitrate";
    
    // UI
    /**
     * Should we use software volume instead of android audio volume? <br/>
     * Some manufacturers are buggy with android audio volume for the stream
     * used for voice over ip calls <br/>
     * Using software volume force to emulate volume change instide the software
     * instead of using android feature
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_SOFT_VOLUME = "use_soft_volume";
    /**
     * Prevent UI screen rotation?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String PREVENT_SCREEN_ROTATION = "prevent_screen_rotation";
    /**
     * Set the logging level of the application. <br/>
     * <ul>
     * <li>1 : error</li>
     * <li>2 : warning</li>
     * <li>3 : info</li>
     * <li>4 : debug</li>
     * <li>5 : verbose</li>
     * </ul>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String LOG_LEVEL = "log_level";
    
    /**
     * Use direct file logging instead of use of logcat. <br/>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String LOG_USE_DIRECT_FILE = "log_use_direct_file";
    
    /**
     * Theme to use for the UI. <br/>
     * Expect a {@link ComponentName#flattenToString()} string
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String THEME = "selected_theme";
    /**
     * Package to manage calls UI.<br/>
     * The package that will handle calls user interface.
     * Might be moved as per account later.
     * If invalid or empty the self application package will be used.
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String CALL_UI_PACKAGE = "call_ui_package";
    /**
     * Display the icon status bar when registered? <br/>
     * Warning, disabling that will unflag the application as important in
     * backgroud. And android may decide to kill it more frequently
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ICON_IN_STATUS_BAR = "icon_in_status_bar";
    /**
     * Display the number of registered accounts in status bar?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ICON_IN_STATUS_BAR_NBR = "icon_in_status_bar_nbr";
    /**
     * Should the application force the screen to remain on when a call is
     * ongoing and calling over wifi.<br/>
     * This is particularly useful for devices affected by the PSP behavior :<br/>
     * These devices turn the wifi card into a slow mode when screen is off.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String KEEP_AWAKE_IN_CALL = "keep_awake_incall";
    /**
     * How GSM is integrated inside the application user interface.<br/>
     * This excludes the outgoing call integration which should be managed using
     * filters
     * 
     * @see #GENERIC_TYPE_AUTO
     * @see #GENERIC_TYPE_FORCE
     * @see #GENERIC_TYPE_PREVENT
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String GSM_INTEGRATION_TYPE = "gsm_integration_type";
    /**
     * Should the application generate a tone when a dial key is pressed ?
     * 
     * @see #GENERIC_TYPE_AUTO
     * @see #GENERIC_TYPE_FORCE
     * @see #GENERIC_TYPE_PREVENT
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DIAL_PRESS_TONE_MODE = "dial_press_tone_mode";
    /**
     * Should the application generate a vibration when a dial key is pressed ?
     * 
     * @see #GENERIC_TYPE_AUTO
     * @see #GENERIC_TYPE_FORCE
     * @see #GENERIC_TYPE_PREVENT
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DIAL_PRESS_VIBRATE_MODE = "dial_press_vibrate_mode";
    /**
     * Should the application generate a tone when a dtmf key is pressed ?
     * 
     * @see #GENERIC_TYPE_AUTO
     * @see #GENERIC_TYPE_FORCE
     * @see #GENERIC_TYPE_PREVENT
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DTMF_PRESS_TONE_MODE = "dtmf_press_tone_mode";
    /**
     * Should we assume that proximity sensor values are inverted?<br/>
     * Let csipsimple automatically manage this setting if you want auto
     * detection to work
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String INVERT_PROXIMITY_SENSOR = "invert_proximity_sensor";
    /**
     * Should the application take a partial lock when sip is registered?<br/>
     * This particular wake lock will ensures CPU is running which leads to
     * higher battery consumption
     * 
     * @see PowerManager#PARTIAL_WAKE_LOCK
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_PARTIAL_WAKE_LOCK = "use_partial_wake_lock";
    /**
     * Should the application write its own calls logs into the system stock call
     * logs?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String INTEGRATE_WITH_CALLLOGS = "integrate_with_native_calllogs";
    /**
     * Should the application hook outgoing call stock system and display user dialog to choose the outgoing sip account or gsm?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String INTEGRATE_WITH_DIALER = "integrate_with_native_dialer";
    /**
     * Should the application hook outgoing call stock system for outgoing privileged calls.
     * This should normally be exclusive of {@link #INTEGRATE_WITH_DIALER}. 
     * This integration method is not recommanded.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String INTEGRATE_TEL_PRIVILEGED = "integrate_tel_privileged";
    /**
     * Should the application try to pause/resume music when in call?<br/>
     * This setting is only used prior to android 2.2 version that has a clean
     * way to do that
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String INTEGRATE_WITH_NATIVE_MUSIC = "integrate_with_native_music";
    /**
     * The default ringtone uri to setup if no ringtone is found for incoming call.<br/>
     * If empty will get the default ringtone of android.
     * 
     * @see System#RINGTONE
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String RINGTONE = "ringtone";
    /**
     * Should the application present buttons instead of slider?<br/>
     * By default the application will use this mode for tablets
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_ALTERNATE_UNLOCKER = "use_alternate_unlocker";
    /**
     * Start application dialer UI with text dialer instead of digit dialer.
     * By default false.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String START_WITH_TEXT_DIALER = "start_with_text_dialer";

    // NETWORK
    /**
     * Specify TURN domain name or host name, in "DOMAIN:PORT" or "HOST:PORT"
     * format.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#ac4761935f0f0bd1271dafde91ca8f83d"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String TURN_SERVER = "turn_server";
    /**
     * Enable TURN relay candidate in ICE.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#ac4761935f0f0bd1271dafde91ca8f83d"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_TURN = "enable_turn";
    /**
     * Specify username to use wnen authenticating with the TURN server.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a4305f174d0f9e3497b3e344236aeea91"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String TURN_USERNAME = "turn_username";
    /**
     * Specify password to use when authenticating with the TURN server.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a4305f174d0f9e3497b3e344236aeea91"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String TURN_PASSWORD = "turn_password";
    /**
     * Enable ICE.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__media__config.htm#a8c3030ecc6b84a888f49f6b3e1b204a9"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_ICE = "enable_ice";
    /**
     * Enable STUN.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__config.htm#abec69c2c899604352f3450368757f39b"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_STUN = "enable_stun";
    /**
     * Stun server.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__config.htm#abec69c2c899604352f3450368757f39b"
     * >Pjsip documentation</a><br/>
     * If you want to set more than one server, separate it with commas.
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String STUN_SERVER = "stun_server";
    /**
     * Enable STUN new format.<br/>
     * This specifies whether STUN requests for resolving socket mapped
     * address should use the new format, i.e: having STUN magic cookie
     * in its transaction ID.
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__config.htm#abec69c2c899604352f3450368757f39b"
     * >Pjsip documentation</a>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_STUN2 = "enable_stun2";
    
    /**
     * Use IPv6 support.<br/>
     * This has no effect now since the application by default supports IPv6
     * except for DNS resolution<br/>
     * This is a limitation of pjsip which resolution is in pjsip roadmap.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_IPV6 = "use_ipv6";
    /**
     * Enable UDP transport.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_UDP = "enable_udp";
    /**
     * Enable TCP transport.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_TCP = "enable_tcp";
    /**
     * Does the LOCK_WIFI ensures performance of wifi as well?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String LOCK_WIFI = "lock_wifi";
    /**
     * Does the {@link #LOCK_WIFI} ensures performance of wifi as well.<br/>
     * This should not be required and could lead to higher battery usage
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String LOCK_WIFI_PERFS = "lock_wifi_perfs";
    /**
     * Enable DNS SRV feature.<br/>
     * By default disabled, the DNS resolution is made using android system
     * directly <br/>
     * If activated the application will do dns srv requests which is slower. <br/>
     * It also requires to have dns servers. These will be retrieved from
     * android os, or can be set using {@link #OVERRIDE_NAMESERVER}
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_DNS_SRV = "enable_dns_srv";
    /**
     * Enable QoS.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_QOS = "enable_qos";
    /**
     * DSCP value for SIP packets.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjlib/docs/html/structpj__qos__params.htm#afa7a796d83d188894d207ebba951e425"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DSCP_VAL = "dscp_val";
    /**
     * DSCP value for RTP packets.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjlib/docs/html/structpj__qos__params.htm#afa7a796d83d188894d207ebba951e425"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String DSCP_RTP_VAL = "dscp_rtp_val";
    /**
     * Send UDP socket keep alive when connected using wifi, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String KEEP_ALIVE_INTERVAL_WIFI = "keep_alive_interval_wifi";
    /**
     * Send UDP socket keep alive when connected using mobile, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String KEEP_ALIVE_INTERVAL_MOBILE = "keep_alive_interval_mobile";
    /**
     * Send TCP socket keep alive when connected using wifi, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TCP_KEEP_ALIVE_INTERVAL_WIFI = "tcp_keep_alive_interval_wifi";
    /**
     * Send TCP socket keep alive when connected using mobile, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TCP_KEEP_ALIVE_INTERVAL_MOBILE = "tcp_keep_alive_interval_mobile";
    /**
     * Send TLS socket keep alive when connected using wifi, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TLS_KEEP_ALIVE_INTERVAL_WIFI = "tls_keep_alive_interval_wifi";
    /**
     * Send TLS socket keep alive when connected using mobile, in seconds.
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TLS_KEEP_ALIVE_INTERVAL_MOBILE = "tls_keep_alive_interval_mobile";
    /**
     * DNS to override instead of using the one configured in android
     * OS.<br/>
     * For now only supports one alternate dns.
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String OVERRIDE_NAMESERVER = "override_nameserver";
    /**
     * Use compact form for sip headers and sdp.<br/>
     * This will minimize size of packets sends.<br/>
     * Take care with this option because some sip server does not manage it
     * properly
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_COMPACT_FORM = "use_compact_form";
    /**
     * Change the user agent of the application.<br/>
     * By default if it's the name of the application, it will add extra information
     * about the device
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String USER_AGENT = "user_agent";

    /**
     * Avoid the use of UPDATE.<br/>
     * This will ignore what's announced by remote part as feature which is
     * useful for remote part that are buggy with that
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String FORCE_NO_UPDATE = "force_no_update";

    /**
     * Specify minimum session expiration period, in seconds. Must not be lower
     * than 90. Default is 90.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__timer__setting.htm#a313ff979b8e59590ec6d50cfa993768b"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TIMER_MIN_SE = "timer_min_se";

    /**
     * Specify session expiration period, in seconds. Must not be lower than
     * min_se. Default is 1800.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__timer__setting.htm#ae1923dbb2330ce7dbffa37042a50e727"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TIMER_SESS_EXPIRES = "timer_sess_expires";

    /**
     * Transaction T1 timeout value.<br/>
     * Timeout of SIP transactions.
     * -1 for default values
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TSX_T1_TIMEOUT = "tsx_t1_timeout";

    /**
     * Transaction T2 timeout value.<br/>
     * Timeout of SIP transactions.
     * -1 for default values
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TSX_T2_TIMEOUT = "tsx_t2_timeout";

    /**
     * Transaction T4 timeout value.<br/>
     * Timeout of SIP transactions.
     * -1 for default values
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TSX_T4_TIMEOUT = "tsx_t4_timeout";

    /**
     *  Transaction TD timeout value.
     *  Transaction completed timer for INVITE.
     *  -1 for default values
     *  
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TSX_TD_TIMEOUT = "tsx_td_timeout";
    
    
    /**
     * Whether media negotiation should include SDP
     * bandwidth modifier "TIAS" (RFC3890).
     * This option is known to be needed to have video working on
     * some Avaya server. It's also known to break buggy SDP parser
     * of some mainstream SIP providers.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ADD_BANDWIDTH_TIAS_IN_SDP = "add_bandwidth_tias_in_sdp";
    
    // SECURE
    /**
     * Enable TLS transport.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String ENABLE_TLS = "enable_tls";
    /**
     * Local port to bind to for TLS transport.<br/>
     * This is the listen port of the application. 0 means automatic
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TLS_TRANSPORT_PORT = "network_tls_transport_port";
    /**
     * Optionally specify the server name instance to connect to when making outgoing TLS connection. <br/>
     * This setting is useful when the server is hosting multiple domains for
     * the same TLS listening socket. <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#aa99531e84635a0a9e99046f62f11afa6"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String TLS_SERVER_NAME = "network_tls_server_name";
    /**
     * Certificate of Authority (CA) list file. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#a96d826c6675c08e465e9dee11f1114d7"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String CA_LIST_FILE = "ca_list_file";
    /**
     * Client certificate file, which will be used for outgoing TLS connections,
     * and server-side certificate for incoming TLS connection.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#a03c3308853ef75a0c76d07ddc8227171"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String CERT_FILE = "cert_file";
    /**
     * Optional private key for the endpoint certificate to be used.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#a76e62480d01210a7cc21b7bf7c94a89f"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String PRIVKEY_FILE = "privkey_file";
    /**
     * Password to open the private key.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#aa6d4b029668bf017162d4b1d09477fe5"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String TLS_PASSWORD = "tls_password";
    /**
     * Default behavior when TLS verification fails on the server side.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#aebbfb646cdfc7151edce2b5194cdbddb"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String TLS_VERIFY_SERVER = "tls_verify_server";
    /**
     * Default behavior when TLS verification fails on the client side.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#ade2b579f76aac470c27c6813a1f85b3c"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String TLS_VERIFY_CLIENT = "tls_verify_client";
    /**
     * TLS protocol method from pjsip_ssl_method.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__tls__setting.htm#a3a453c419c092ecc05f0141da36183fa"
     * >Pjsip documentation</a><br/>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String TLS_METHOD = "tls_method";
    /**
     * How use SRTP?<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__config.htm#a8d281b965658948b2bd0b72e01cd279c"
     * >Pjsip documentation</a><br/>
     * Values are integer of the <a target="_blank" href=
     * "http://www.pjsip.org/pjmedia/docs/html/group__PJMEDIA__TRANSPORT__SRTP.htm#ga52f4c561c77ebd7a992feefc77624ace"
     * >pjmedia_srtp_use</a>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String USE_SRTP = "use_srtp";
    /**
     * How should we use ZRTP? <br/>
     * <ul>
     * <li>0 : disable</li>
     * <li>2 : create ZRTP</li>
     * </ul>
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String USE_ZRTP = "use_zrtp";
    
    /**
     * Interval for polling network routes. <br/>
     * This is useful to set if using VPN on android 4.0
     * 
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     */
    public static final String NETWORK_ROUTES_POLLING = "network_route_polling";
    
    /**
     * Enable wifi for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_WIFI_IN = "use_wifi_in";

    /**
     * Enable wifi for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_WIFI_OUT = "use_wifi_out";
    
    /**
     * Enable other networks for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_OTHER_IN = "use_other_in";

    /**
     * Enable other networks for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_OTHER_OUT = "use_other_out";
    
    /**
     * Enable 3G for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_3G_IN = "use_3g_in";

    /**
     * Enable 3G for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_3G_OUT = "use_3g_out";
    
    /**
     * Enable gprs (2G) for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_GPRS_IN = "use_gprs_in";

    /**
     * Enable gprs (2G) for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_GPRS_OUT = "use_gprs_out";

    /**
     * Enable edge for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_EDGE_IN = "use_edge_in";

    /**
     * Enable edge for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_EDGE_OUT = "use_edge_out";
    
    /**
     * Enable anyway for incoming calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_ANYWAY_IN = "use_anyway_in";

    /**
     * Enable anyway for outgoing calls
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_ANYWAY_OUT = "use_anyway_out";
    
    // CALLS
    /**
     * Automatically record calls to wav files?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String AUTO_RECORD_CALLS = "auto_record_calls";
    /**
     * Default display name to use for sip contact.<br/>
     * This can be overriden per account.
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String DEFAULT_CALLER_ID = "default_caller_id";
    /**
     * Should the application allow multiple calls?<br/>
     * Disabling it can help when the app multiply registers
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String SUPPORT_MULTIPLE_CALLS = "support_multiple_calls";
    /**
     * Does the application enable video calls by default?<br/>
     * This setting is not yet stable because video feature is not fully
     * integrated yet.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String USE_VIDEO = "use_video";
    /**
     * Video capture size in form (width)x(height)@(fps)
     * 
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String VIDEO_CAPTURE_SIZE = "video_capture_size";
    
    /**
     * Should the stack never switch to TCP when packets are too big?
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String DISABLE_TCP_SWITCH = "disable_tcp_switch";
    
    /**
     * Disable rport in request
     * Only activate for buggy servers. Disable RFC 3581 support by not sending rport.
     * Preferred fix is to fix the server that get broken with extra parameters : even if 
     * not support this RFC should never forbid to register if rport appears.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     */
    public static final String DISABLE_RPORT = "disable_rport";

    // Enums
    /**
     * Automatic echo mode.
     * 
     * @see #ECHO_MODE
     */
    public static final int ECHO_MODE_AUTO = 0;
    /**
     * Simple echo mode. It's a basic implementation
     * 
     * @see #ECHO_MODE
     */
    public static final int ECHO_MODE_SIMPLE = 1;
    /**
     * Accoustic echo cancellation of Speex
     * 
     * @see #ECHO_MODE
     */
    public static final int ECHO_MODE_SPEEX = 2;
    /**
     * Accoustic echo cancellation of WebRTC
     * 
     * @see #ECHO_MODE
     */
    public static final int ECHO_MODE_WEBRTC_M = 3;

    /**
     * Automatic DTMF mode. <br/>
     * Will try RTP mode and if not available will fallback on in band
     * 
     * @see #DTMF_MODE
     */
    public static final int DTMF_MODE_AUTO = 0;
    /**
     * Uses RTP telephony events to send DTMF.
     * 
     * @see #DTMF_MODE
     */
    public static final int DTMF_MODE_RTP = 1;
    /**
     * Generate in-band tones as media to simulate DTMF tones.
     * 
     * @see #DTMF_MODE
     */
    public static final int DTMF_MODE_INBAND = 2;
    /**
     * Sends SIP info to send DTMF tones.
     * 
     * @see #DTMF_MODE
     */
    public static final int DTMF_MODE_INFO = 3;

    /**
     * Pressing headset button hangup the call.
     * 
     * @see #HEADSET_ACTION
     */
    public static final int HEADSET_ACTION_CLEAR_CALL = 0;
    /**
     * Pressing headset button mute the call
     * 
     * @see #HEADSET_ACTION
     */
    public static final int HEADSET_ACTION_MUTE = 1;
    /**
     * Pressing headset button hold the call
     * 
     * @see #HEADSET_ACTION
     */
    public static final int HEADSET_ACTION_HOLD = 2;

    /**
     * Uses java/jni implementation audio implementation.
     * 
     * @see #AUDIO_IMPLEMENTATION
     */
    public static final int AUDIO_IMPLEMENTATION_JAVA = 0;
    /**
     * Uses opensl-ES implementation audio implementation.
     * 
     * @see #AUDIO_IMPLEMENTATION
     */
    public static final int AUDIO_IMPLEMENTATION_OPENSLES = 1;

    /**
     * Auto detect options, depending on android settings.
     */
    public static final int GENERIC_TYPE_AUTO = 0;
    /**
     * Force this option on.
     */
    public static final int GENERIC_TYPE_FORCE = 1;
    /**
     * Disable this option.
     */
    public static final int GENERIC_TYPE_PREVENT = 2;

    public static final String PREFS_TABLE_NAME = "preferences";
    public static final String RESET_TABLE_NAME = "raz";

    // For Provider
    /**
     * Authority for preference content provider. <br/>
     * Maybe be changed for forked versions of the app.
     */
    public static final String AUTHORITY = "com.csipsimple.prefs";
    private static final String BASE_DIR_TYPE = "vnd.android.cursor.dir/vnd.csipsimple";
    private static final String BASE_ITEM_TYPE = "vnd.android.cursor.item/vnd.csipsimple";
    
    // Preference
    /**
     * Content type for preference provider.
     */
    public static final String PREF_CONTENT_TYPE = BASE_DIR_TYPE + ".pref";
    /**
     * Item type for preference provider.
     */
    public static final String PREF_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + ".pref";
    /**
     * Uri for preference content provider.<br/>
     * Deeply advised to not use directly
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     * @see #setPreferenceFloatValue(Context, String, Float)
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final Uri PREF_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/"
            + PREFS_TABLE_NAME);

    /**
     * Base uri for a specific preference in the content provider.<br/>
     * Deeply advised to not use directly
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     * @see #setPreferenceFloatValue(Context, String, Float)
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final Uri PREF_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/"
            + PREFS_TABLE_NAME + "/");

    // Raz
    /**
     * Reset uri to wipe the entire preference database clean.
     */
    public static final Uri RAZ_URI = Uri
            .parse(ContentResolver.SCHEME_CONTENT + "://" + AUTHORITY + "/" + RESET_TABLE_NAME);

    /**
     * Content value key for preference name.<br/>
     * It is strongly advised that you do NOT use this directly.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     * @see #setPreferenceFloatValue(Context, String, Float)
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String FIELD_NAME = "name";
    /**
     * Content value key for preference value.<br/>
     * It is strongly advised that you do NOT use this directly.
     * 
     * @see #setPreferenceBooleanValue(Context, String, boolean)
     * @see #setPreferenceFloatValue(Context, String, Float)
     * @see #setPreferenceIntegerValue(Context, String, Integer)
     * @see #setPreferenceStringValue(Context, String, String)
     */
    public static final String FIELD_VALUE = "value";

    /**
     * Narrow band type codec preference key.<br/>
     * 
     * @see #getCodecKey(String, String)
     */
    public static final String CODEC_NB = "nb";

    /**
     * Wide band type codec preference key.<br/>
     * 
     * @see #getCodecKey(String, String)
     */
    public static final String CODEC_WB = "wb";

    /**
     * Get the preference key for a codec priority
     * 
     * @param codecName Name of the codec as known by pjsip. Example PCMU/8000/1
     * @param type Type of the codec {@link #CODEC_NB} or {@link #CODEC_WB}
     * @return The key to use to set/get the priority of a codec for a given
     *         bandwidth
     */
    public static String getCodecKey(String codecName, String type) {
        String[] codecParts = codecName.split("/");
        String preferenceKey = null;
        if (codecParts.length >= 2) {
            return "codec_" + codecParts[0].toLowerCase() + "_" + codecParts[1] + "_" + type;
        }
        return preferenceKey;
    }

    /**
     * Get the preference <b>partial</b> key for a given network kind
     * 
     * @param networkType Type of the network {@link ConnectivityManager}
     * @param subType Subtype of the network {@link TelephonyManager}
     * @return The partial key for the network kind
     */
    private static String keyForNetwork(int networkType, int subType) {
        if (networkType == ConnectivityManager.TYPE_WIFI) {
            return "wifi";
        } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
            // 3G (or better)
            if (subType >= TelephonyManager.NETWORK_TYPE_UMTS) {
                return "3g";
            }

            // GPRS (or unknown)
            if (subType == TelephonyManager.NETWORK_TYPE_GPRS
                    || subType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                return "gprs";
            }

            // EDGE
            if (subType == TelephonyManager.NETWORK_TYPE_EDGE) {
                return "edge";
            }
        }

        return "other";
    }

    /**
     * Get preference key for the kind of bandwidth to associate to a network
     * 
     * @param networkType Type of the network {@link ConnectivityManager}
     * @param subType Subtype of the network {@link TelephonyManager}
     * @return the preference key for the network kind passed in argument
     */
    public static String getBandTypeKey(int networkType, int subType) {
        return "band_for_" + keyForNetwork(networkType, subType);
    }

    private static Uri getPrefUriForKey(String key) {
        return Uri.withAppendedPath(PREF_ID_URI_BASE, key);
    }

    /**
     * Get string configuration value with null default value
     * 
     * @see SipConfigManager#getPreferenceStringValue(Context, String, String)
     */
    public static String getPreferenceStringValue(Context ctxt, String key) {
        return getPreferenceStringValue(ctxt, key, null);
    }

    /**
     * Helper method to retrieve a csipsimple string config value
     * 
     * @param ctxt The context of your app
     * @param key the key for the setting you want to get
     * @param defaultValue the value you want to return if nothing found
     * @return the preference value
     */
    public static String getPreferenceStringValue(Context ctxt, String key, String defaultValue) {
        String value = defaultValue;
        Uri uri = getPrefUriForKey(key);
        Cursor c = ctxt.getContentResolver().query(uri, null, String.class.getName(), null, null);
        if (c != null) {
            c.moveToFirst();
            String strValue = c.getString(1);
            if (strValue != null) {
                value = strValue;
            }
            c.close();
        }
        return value;
    }

    /**
     * Get boolean configuration value with null default value
     * 
     * @see SipConfigManager#getPreferenceBooleanValue(Context, String, Boolean)
     */
    public static Boolean getPreferenceBooleanValue(Context ctxt, String key) {
        return getPreferenceBooleanValue(ctxt, key, null);
    }

    /**
     * Helper method to retrieve a csipsimple boolean config value
     * 
     * @param ctxt The context of your app
     * @param key the key for the setting you want to get
     * @param defaultValue the value you want to return if nothing found
     * @return the preference value
     */
    public static Boolean getPreferenceBooleanValue(Context ctxt, String key, Boolean defaultValue) {
        Boolean value = defaultValue;
        Uri uri = getPrefUriForKey(key);
        Cursor c = ctxt.getContentResolver().query(uri, null, Boolean.class.getName(), null, null);
        if (c != null) {
            c.moveToFirst();
            int intValue = c.getInt(1);
            if (intValue >= 0) {
                value = (intValue == 1);
            }
            c.close();
        }
        return value;
    }

    /**
     * Get float configuration value with null default value
     * 
     * @see SipConfigManager#getPreferenceFloatValue(Context, String, Float)
     */
    public static Float getPreferenceFloatValue(Context ctxt, String key) {
        return getPreferenceFloatValue(ctxt, key, null);
    }

    /**
     * Helper method to retrieve a csipsimple float config value
     * 
     * @param ctxt The context of your app
     * @param key the key for the setting you want to get
     * @param defaultValue the value you want to return if nothing found
     * @return the preference value
     */
    public static Float getPreferenceFloatValue(Context ctxt, String key, Float defaultValue) {
        Float value = defaultValue;
        Uri uri = getPrefUriForKey(key);
        Cursor c = ctxt.getContentResolver().query(uri, null, Float.class.getName(), null, null);
        if (c != null) {
            c.moveToFirst();
            Float fValue = c.getFloat(1);
            if (fValue != null) {
                value = fValue;
            }
            c.close();
        }
        return value;
    }

    /**
     * Get integer configuration value with null default value
     * 
     * @see SipConfigManager#getPreferenceIntegerValue(Context, String, Integer)
     */
    public static Integer getPreferenceIntegerValue(Context ctxt, String key) {
        return getPreferenceIntegerValue(ctxt, key, null);
    }

    /**
     * Helper method to retrieve a csipsimple float config value
     * 
     * @param ctxt The context of your app
     * @param key the key for the setting you want to get
     * @param defaultValue the value you want to return if nothing found
     * @return the preference value
     */
    public static Integer getPreferenceIntegerValue(Context ctxt, String key, Integer defaultValue) {
        Integer value = defaultValue;
        Uri uri = getPrefUriForKey(key);
        Cursor c = ctxt.getContentResolver().query(uri, null, Integer.class.getName(), null, null);
        if (c != null) {
            c.moveToFirst();
            Integer iValue = c.getInt(1);
            if (iValue != null) {
                value = iValue;
            }
            c.close();
        }
        return value;
    }

    /**
     * Set the value of a preference string
     * 
     * @param ctxt The context of android app
     * @param key The key config to change
     * @param value The value to set to
     */
    public static void setPreferenceStringValue(Context ctxt, String key, String value) {
        Uri uri = getPrefUriForKey(key);
        ContentValues values = new ContentValues();
        values.put(SipConfigManager.FIELD_VALUE, value);
        ctxt.getContentResolver().update(uri, values, String.class.getName(), null);
    }

    /**
     * Set the value of a preference string
     * 
     * @param ctxt The context of android app
     * @param key The key config to change
     * @param value The value to set to
     */
    public static void setPreferenceBooleanValue(Context ctxt, String key, boolean value) {
        Uri uri = getPrefUriForKey(key);
        ContentValues values = new ContentValues();
        values.put(SipConfigManager.FIELD_VALUE, value);
        ctxt.getContentResolver().update(uri, values, Boolean.class.getName(), null);
    }

    /**
     * Set the value of a preference string
     * 
     * @param ctxt The context of android app
     * @param key The key config to change
     * @param value The value to set to
     */
    public static void setPreferenceFloatValue(Context ctxt, String key, Float value) {
        Uri uri = getPrefUriForKey(key);
        ContentValues values = new ContentValues();
        values.put(SipConfigManager.FIELD_VALUE, value);
        ctxt.getContentResolver().update(uri, values, Float.class.getName(), null);
    }

    /**
     * Set the value of a preference integer
     * 
     * @param ctxt The context of android app
     * @param key The key config to change
     * @param value The value to set to
     */
    public static void setPreferenceIntegerValue(Context ctxt, String key, Integer value) {
        if (value != null) {
            setPreferenceStringValue(ctxt, key, value.toString());
        }
    }

}
