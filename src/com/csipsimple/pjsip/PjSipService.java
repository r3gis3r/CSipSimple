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

package com.csipsimple.pjsip;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipManager.PresenceStatus;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.service.MediaManager;
import com.csipsimple.service.SipService;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.service.SipService.ToCall;
import com.csipsimple.utils.ExtraPlugins;
import com.csipsimple.utils.ExtraPlugins.DynCodecInfos;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.TimerWrapper;
import com.csipsimple.wizards.WizardUtils;

import org.pjsip.pjsua.csipsimple_config;
import org.pjsip.pjsua.dynamic_factory;
import org.pjsip.pjsua.pj_qos_params;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsip_timer_setting;
import org.pjsip.pjsua.pjsip_tls_setting;
import org.pjsip.pjsua.pjsip_transport_type_e;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_config;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_buddy_config;
import org.pjsip.pjsua.pjsua_call_flag;
import org.pjsip.pjsua.pjsua_call_setting;
import org.pjsip.pjsua.pjsua_config;
import org.pjsip.pjsua.pjsua_logging_config;
import org.pjsip.pjsua.pjsua_media_config;
import org.pjsip.pjsua.pjsua_msg_data;
import org.pjsip.pjsua.pjsua_transport_config;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

public class PjSipService {
    private static final String THIS_FILE = "PjService";
    private static final int DTMF_TONE_PAUSE_LENGTH = 300;
    private static final int DTMF_TONE_WAIT_LENGTH = 2000;
    public SipService service;

    private boolean created = false;

    private boolean hasSipStack = false;
    private boolean sipStackIsCorrupted = false;
    private Integer udpTranportId, tcpTranportId, tlsTransportId;
    private Integer localUdpAccPjId, localTcpAccPjId, localTlsAccPjId;
    public PreferencesProviderWrapper prefsWrapper;
    //private PjStreamDialtoneGenerator dialtoneGenerator;

    private Integer hasBeenHoldByGSM = null;
    private Integer hasBeenChangedRingerMode = null;
    
    public UAStateReceiver userAgentReceiver;
    public MediaManager mediaManager;

    private Timer tasksTimer;
    private SparseArray<String> dtmfToAutoSend = new SparseArray<String>(5);
    private SparseArray<TimerTask> dtmfTasks = new SparseArray<TimerTask>(5);
    private SparseArray<PjStreamDialtoneGenerator> dtmfDialtoneGenerators = new SparseArray<PjStreamDialtoneGenerator>(5);
    
    // -------
    // Locks
    // -------

    public PjSipService() {

    }

    public void setService(SipService aService) {
        service = aService;
        prefsWrapper = service.getPrefs();
    }

    public boolean isCreated() {
        return created;
    }

    public boolean tryToLoadStack() {
        if (hasSipStack) {
            return true;
        }

        // File stackFile = NativeLibManager.getStackLibFile(service);
        if (!sipStackIsCorrupted) {
            try {
                // Try to load the stack
                // System.load(NativeLibManager.getBundledStackLibFile(service,
                // "libcrypto.so").getAbsolutePath());
                // System.load(NativeLibManager.getBundledStackLibFile(service,
                // "libssl.so").getAbsolutePath());
                // System.loadLibrary("crypto");
                // System.loadLibrary("ssl");
                System.loadLibrary(NativeLibManager.STACK_NAME);
                hasSipStack = true;
                return true;
            } catch (UnsatisfiedLinkError e) {
                // If it fails we probably are running on a special hardware
                Log.e(THIS_FILE,
                        "We have a problem with the current stack.... NOT YET Implemented", e);
                hasSipStack = false;
                sipStackIsCorrupted = true;

                service.notifyUserOfMessage("Can't load native library. CPU arch invalid for this build");
                return false;
            } catch (Exception e) {
                Log.e(THIS_FILE, "We have a problem with the current stack....", e);
            }
        }
        return false;
    }

    // Start the sip stack according to current settings
    /**
     * Start the sip stack Thread safing of this method must be ensured by upper
     * layer Every calls from pjsip that require start/stop/getInfos from the
     * underlying stack must be done on the same thread
     */
    public boolean sipStart() throws SameThreadException {

        Log.setLogLevel(prefsWrapper.getLogLevel());

        if (!hasSipStack) {
            Log.e(THIS_FILE, "We have no sip stack, we can't start");
            return false;
        }

        // Ensure the stack is not already created or is being created
        if (!created) {
            Log.d(THIS_FILE, "Starting sip stack");
            udpTranportId = null;
            tcpTranportId = null;
            tlsTransportId = null;

            // Pj timer
            TimerWrapper.create(service);

            int status;
            status = pjsua.create();

            Log.i(THIS_FILE, "Created " + status);
            // General config
            {
                pj_str_t[] stunServers = null;
                int stunServersCount = 0;
                pjsua_config cfg = new pjsua_config();
                pjsua_logging_config logCfg = new pjsua_logging_config();
                pjsua_media_config mediaCfg = new pjsua_media_config();
                csipsimple_config cssCfg = new csipsimple_config();

                // SERVICE CONFIG

                if (userAgentReceiver == null) {
                    Log.d(THIS_FILE, "create receiver....");
                    userAgentReceiver = new UAStateReceiver();
                    userAgentReceiver.initService(this);
                }
                if (mediaManager == null) {
                    mediaManager = new MediaManager(service);
                }

                mediaManager.startService();

                pjsua.setCallbackObject(userAgentReceiver);

                Log.d(THIS_FILE, "Attach is done to callback");
                int isTurnEnabled = prefsWrapper.getTurnEnabled();

                // CSS CONFIG
                pjsua.csipsimple_config_default(cssCfg);
                cssCfg.setUse_compact_form_headers(prefsWrapper
                        .getPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM) ? pjsua.PJ_TRUE
                        : pjsua.PJ_FALSE);
                cssCfg.setUse_compact_form_sdp(prefsWrapper
                        .getPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM) ? pjsua.PJ_TRUE
                        : pjsua.PJ_FALSE);
                cssCfg.setUse_no_update(prefsWrapper
                        .getPreferenceBooleanValue(SipConfigManager.FORCE_NO_UPDATE) ? pjsua.PJ_TRUE
                        : pjsua.PJ_FALSE);
                
                cssCfg.setTcp_keep_alive_interval(prefsWrapper.getTcpKeepAliveInterval());
                cssCfg.setTls_keep_alive_interval(prefsWrapper.getTlsKeepAliveInterval());
                
                // Transaction timeouts
                int tsx_to = prefsWrapper.getPreferenceIntegerValue(SipConfigManager.TSX_T1_TIMEOUT);
                if(tsx_to > 0) {
                    cssCfg.setTsx_t1_timeout(tsx_to);
                }
                tsx_to = prefsWrapper.getPreferenceIntegerValue(SipConfigManager.TSX_T2_TIMEOUT);
                if(tsx_to > 0) {
                    cssCfg.setTsx_t2_timeout(tsx_to);
                }
                tsx_to = prefsWrapper.getPreferenceIntegerValue(SipConfigManager.TSX_T4_TIMEOUT);
                if(tsx_to > 0) {
                    cssCfg.setTsx_t4_timeout(tsx_to);
                }
                
                // TURN
                if (isTurnEnabled == 1) {
                    cssCfg.setTurn_username(pjsua.pj_str_copy(prefsWrapper
                            .getPreferenceStringValue(SipConfigManager.TURN_USERNAME)));
                    cssCfg.setTurn_password(pjsua.pj_str_copy(prefsWrapper
                            .getPreferenceStringValue(SipConfigManager.TURN_PASSWORD)));
                }

                // -- USE_ZRTP 1 is no_zrtp, 2 is create_zrtp
                File zrtpFolder = PreferencesWrapper.getZrtpFolder(service);
                if (zrtpFolder != null) {
                    cssCfg.setUse_zrtp((prefsWrapper
                            .getPreferenceIntegerValue(SipConfigManager.USE_ZRTP) > 1) ? pjsua.PJ_TRUE
                            : pjsua.PJ_FALSE);
                    cssCfg.setStorage_folder(pjsua.pj_str_copy(zrtpFolder.getAbsolutePath()));
                } else {
                    cssCfg.setUse_zrtp(pjsua.PJ_FALSE);
                    cssCfg.setStorage_folder(pjsua.pj_str_copy(""));
                }

                Map<String, DynCodecInfos> availableCodecs = ExtraPlugins.getDynPlugins(service, SipManager.ACTION_GET_EXTRA_CODECS);
                dynamic_factory[] cssCodecs = cssCfg.getExtra_aud_codecs();
                int i = 0;
                for (Entry<String, DynCodecInfos> availableCodec : availableCodecs.entrySet()) {
                    DynCodecInfos dyn = availableCodec.getValue();
                    if (!TextUtils.isEmpty(dyn.libraryPath)) {
                        cssCodecs[i].setShared_lib_path(pjsua.pj_str_copy(dyn.libraryPath));
                        cssCodecs[i++].setInit_factory_name(pjsua
                                .pj_str_copy(dyn.factoryInitFunction));
                    }
                }
                cssCfg.setExtra_aud_codecs_cnt(i);

                // Audio implementation
                int implementation = prefsWrapper
                        .getPreferenceIntegerValue(SipConfigManager.AUDIO_IMPLEMENTATION);
                if (implementation == SipConfigManager.AUDIO_IMPLEMENTATION_OPENSLES) {
                    dynamic_factory audImp = cssCfg.getAudio_implementation();
                    audImp.setInit_factory_name(pjsua.pj_str_copy("pjmedia_opensl_factory"));
                    File openslLib = NativeLibManager.getBundledStackLibFile(service,
                            "libpj_opensl_dev.so");
                    audImp.setShared_lib_path(pjsua.pj_str_copy(openslLib.getAbsolutePath()));
                    cssCfg.setAudio_implementation(audImp);
                    Log.d(THIS_FILE, "Use OpenSL-ES implementation");
                }

                // Video implementation
                if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO)){
                    // TODO :: Have plugins per capture / render / video codec / converter
                    Map<String, DynCodecInfos> videoPlugins = ExtraPlugins.getDynPlugins(service, SipManager.ACTION_GET_VIDEO_PLUGIN);
                    
                    if(videoPlugins.size() > 0) {
                        DynCodecInfos videoPlugin = videoPlugins.values().iterator().next();
                        pj_str_t pjVideoFile = pjsua.pj_str_copy(videoPlugin.libraryPath);
                        Log.d(THIS_FILE, "Load video plugin at " + videoPlugin.libraryPath );
                        // Render
                        {
                            dynamic_factory vidImpl = cssCfg.getVideo_render_implementation();
                            vidImpl.setInit_factory_name(pjsua
                                    .pj_str_copy("pjmedia_webrtc_vid_render_factory"));
                            vidImpl.setShared_lib_path(pjVideoFile);
                        }
                        // Capture
                        {
                            dynamic_factory vidImpl = cssCfg.getVideo_capture_implementation();
                            vidImpl.setInit_factory_name(pjsua
                                    .pj_str_copy("pjmedia_webrtc_vid_capture_factory"));
                            vidImpl.setShared_lib_path(pjVideoFile);
                            /* -- For testing video screen -- Not yet released
                            try {
                                ComponentName cmp = new ComponentName("com.csipsimple.plugins.video", "com.csipsimple.plugins.video.CaptureReceiver");
                                DynCodecInfos screenCapt = new ExtraPlugins.DynCodecInfos(service, cmp);
                                vidImpl.setInit_factory_name(pjsua
                                        .pj_str_copy(screenCapt.factoryInitFunction));
                                vidImpl.setShared_lib_path(pjsua
                                        .pj_str_copy(screenCapt.libraryPath));
                            } catch (NameNotFoundException e) {
                                Log.e(THIS_FILE, "Not found capture plugin");
                            }
                            */
                        }
                        // Codecs
                        cssCodecs = cssCfg.getExtra_vid_codecs();
                        cssCodecs[0].setShared_lib_path(pjVideoFile);
                        cssCodecs[0].setInit_factory_name(pjsua
                                .pj_str_copy("pjmedia_codec_ffmpeg_vid_init"));
                        cssCodecs = cssCfg.getExtra_vid_codecs_destroy();
                        cssCodecs[0].setShared_lib_path(pjVideoFile);
                        cssCodecs[0].setInit_factory_name(pjsua
                                .pj_str_copy("pjmedia_codec_ffmpeg_vid_deinit"));
                        cssCfg.setExtra_vid_codecs_cnt(1);
                        
                        // Converter
                        dynamic_factory convertImpl = cssCfg.getVid_converter();
                        convertImpl.setShared_lib_path(pjVideoFile);
                        convertImpl.setInit_factory_name(pjsua
                                .pj_str_copy("pjmedia_libswscale_converter_init"));
                    }
                }

                // MAIN CONFIG
                pjsua.config_default(cfg);
                cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);
                cfg.setUser_agent(pjsua.pj_str_copy(prefsWrapper.getUserAgent(service)));
                // With new timer implementation, thread count of pjsip can be 0
                // it will use less CPU since now thread are launched by
                // alarmManager
                cfg.setThread_cnt(0);
                cfg.setUse_srtp(getUseSrtp());
                cfg.setSrtp_secure_signaling(0);
                cfg.setNat_type_in_sdp(0);

                pjsip_timer_setting timerSetting = cfg.getTimer_setting();
                int minSe = prefsWrapper.getPreferenceIntegerValue(SipConfigManager.TIMER_MIN_SE);
                int sessExp = prefsWrapper
                        .getPreferenceIntegerValue(SipConfigManager.TIMER_SESS_EXPIRES);
                if (minSe <= sessExp && minSe >= 90) {
                    timerSetting.setMin_se(minSe);
                    timerSetting.setSess_expires(sessExp);
                    cfg.setTimer_setting(timerSetting);
                }
                // DNS
                if (prefsWrapper.enableDNSSRV() && !prefsWrapper.useIPv6()) {
                    pj_str_t[] nameservers = getNameservers();
                    if (nameservers != null) {
                        cfg.setNameserver_count(nameservers.length);
                        cfg.setNameserver(nameservers);
                    } else {
                        cfg.setNameserver_count(0);
                    }
                }
                // STUN
                int isStunEnabled = prefsWrapper.getStunEnabled();
                if (isStunEnabled == 1) {
                    String[] servers = prefsWrapper.getPreferenceStringValue(
                            SipConfigManager.STUN_SERVER).split(",");
                    cfg.setStun_srv_cnt(servers.length);
                    stunServers = cfg.getStun_srv();
                    for (String server : servers) {
                        Log.d(THIS_FILE, "add server " + server.trim());
                        stunServers[stunServersCount] = pjsua.pj_str_copy(server.trim());
                        stunServersCount++;
                    }
                    cfg.setStun_srv(stunServers);
                }

                // LOGGING CONFIG
                pjsua.logging_config_default(logCfg);
                logCfg.setConsole_level(prefsWrapper.getLogLevel());
                logCfg.setLevel(prefsWrapper.getLogLevel());
                logCfg.setMsg_logging(pjsuaConstants.PJ_TRUE);
                
                if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.LOG_USE_DIRECT_FILE, false)) {
                    File dir = PreferencesWrapper.getLogsFolder(service);
                    if( dir != null) {
                        Date d = new Date();
                        File outFile = new File(dir.getAbsoluteFile() + File.separator + "logs_"+DateFormat.format("MM-dd-yy", d)+".txt");
                        logCfg.setLog_filename(pjsua.pj_str_copy(outFile.getAbsolutePath()));
                        logCfg.setLog_file_flags(0x1108 /* PJ_O_APPEND */);
                    }
                }

                // MEDIA CONFIG
                pjsua.media_config_default(mediaCfg);

                // For now only this cfg is supported
                mediaCfg.setChannel_count(1);
                mediaCfg.setSnd_auto_close_time(prefsWrapper.getAutoCloseTime());
                // Echo cancellation
                mediaCfg.setEc_tail_len(prefsWrapper.getEchoCancellationTail());
                int echoMode = prefsWrapper.getPreferenceIntegerValue(SipConfigManager.ECHO_MODE);
                long clockRate = prefsWrapper.getClockRate();
                if (clockRate > 16000 && echoMode == SipConfigManager.ECHO_MODE_WEBRTC_M) {
                    // WebRTC mobile does not allow higher that 16kHz for now
                    // TODO : warn user about this point
                    echoMode = SipConfigManager.ECHO_MODE_SIMPLE;
                }
                mediaCfg.setEc_options(echoMode);
                mediaCfg.setNo_vad(prefsWrapper
                        .getPreferenceBooleanValue(SipConfigManager.ENABLE_VAD) ? 0 : 1);
                mediaCfg.setQuality(prefsWrapper.getMediaQuality());
                mediaCfg.setClock_rate(clockRate);
                mediaCfg.setAudio_frame_ptime(prefsWrapper
                        .getPreferenceIntegerValue(SipConfigManager.SND_PTIME));
                // Disabled because only one thread enabled now for battery perfs on normal state
                mediaCfg.setHas_ioqueue(/*prefsWrapper
                        .getPreferenceBooleanValue(SipConfigManager.HAS_IO_QUEUE) ? 1 :*/ 0);

                // ICE
                mediaCfg.setEnable_ice(prefsWrapper.getIceEnabled());
                // TURN
                if (isTurnEnabled == 1) {
                    mediaCfg.setEnable_turn(isTurnEnabled);
                    mediaCfg.setTurn_server(pjsua.pj_str_copy(prefsWrapper.getTurnServer()));
                }

                // INITIALIZE
                status = pjsua.csipsimple_init(cfg, logCfg, mediaCfg, cssCfg, service);
                if (status != pjsuaConstants.PJ_SUCCESS) {
                    String msg = "Fail to init pjsua "
                            + pjStrToString(pjsua.get_error_message(status));
                    Log.e(THIS_FILE, msg);
                    service.notifyUserOfMessage(msg);
                    cleanPjsua();
                    return false;
                }

                /*
                 * if (stunServersCount > 0) { int s = pjsua.detect_nat_type();
                 * Log.d(THIS_FILE, ">>> NAT TYPE is "+s); }
                 */
            }

            // Add transports
            {
                // UDP
                if (prefsWrapper.isUDPEnabled()) {
                    pjsip_transport_type_e t = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP;
                    if (prefsWrapper.useIPv6()) {
                        t = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP6;
                    }
                    udpTranportId = createTransport(t, prefsWrapper.getUDPTransportPort());
                    if (udpTranportId == null) {
                        cleanPjsua();
                        return false;
                    }

                    // We need a local account to not have the
                    // application lost when direct call to the IP
                    // TODO : allow to configure this account
                    
                    int[] p_acc_id = new int[1];
                    pjsua.acc_add_local(udpTranportId, pjsua.PJ_FALSE, p_acc_id);
                    localUdpAccPjId = p_acc_id[0];
                    // Log.d(THIS_FILE, "Udp account "+p_acc_id);

                }
                // TCP
                if (prefsWrapper.isTCPEnabled()) {
                    pjsip_transport_type_e t = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP;
                    if (prefsWrapper.useIPv6()) {
                        t = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP6;
                    }
                    tcpTranportId = createTransport(t, prefsWrapper.getTCPTransportPort());
                    if (tcpTranportId == null) {
                        cleanPjsua();
                        return false;
                    }

                    // We need a local account to not have the
                    // application lost when direct call to the IP
                    int[] p_acc_id = new int[1];
                    pjsua.acc_add_local(tcpTranportId, pjsua.PJ_FALSE, p_acc_id);
                    localTcpAccPjId = p_acc_id[0];
                }

                // TLS
                if (prefsWrapper.isTLSEnabled() && !prefsWrapper.useIPv6()) {
                    tlsTransportId = createTransport(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS,
                            prefsWrapper.getTLSTransportPort());

                    if (tlsTransportId == null) {
                        cleanPjsua();
                        return false;
                    }
                    // We need a local account to not have the
                    // application lost when direct call to the IP
                    int[] p_acc_id = new int[1];
                    pjsua.acc_add_local(tlsTransportId, pjsua.PJ_FALSE, p_acc_id);
                    localTlsAccPjId = p_acc_id[0];
                }
            }

            // Initialization is done, now start pjsua
            status = pjsua.start();

            if (status != pjsua.PJ_SUCCESS) {
                String msg = "Fail to start pjsip  "
                        + pjStrToString(pjsua.get_error_message(status));
                Log.e(THIS_FILE, msg);
                service.notifyUserOfMessage(msg);
                cleanPjsua();
                return false;
            }

            // Init media codecs
            initCodecs();
            setCodecsPriorities();

            created = true;

            return true;
        }

        return false;
    }

    /**
     * Stop sip service
     * 
     * @return true if stop has been performed
     */
    public boolean sipStop() throws SameThreadException {
        Log.d(THIS_FILE, ">> SIP STOP <<");

        if (getActiveCallInProgress() != null) {
            Log.e(THIS_FILE, "We have a call in progress... DO NOT STOP !!!");
            // TODO : queue quit on end call;
            return false;
        }

        if (service.notificationManager != null) {
            service.notificationManager.cancelRegisters();
        }
        if (created) {
            cleanPjsua();
            TimerWrapper.destroy();
        }
        if(tasksTimer != null) {
            tasksTimer.cancel();
            tasksTimer.purge();
            tasksTimer = null;
        }
        return true;
    }

    private void cleanPjsua() throws SameThreadException {
        Log.d(THIS_FILE, "Detroying...");
        // This will destroy all accounts so synchronize with accounts
        // management lock
        pjsua.csipsimple_destroy();
        service.getContentResolver().delete(SipProfile.ACCOUNT_STATUS_URI, null, null);
        if (userAgentReceiver != null) {
            userAgentReceiver.stopService();
            userAgentReceiver = null;
        }

        if (mediaManager != null) {
            mediaManager.stopService();
            mediaManager = null;
        }

        created = false;
    }

    /**
     * Utility to create a transport
     * 
     * @return transport id or -1 if failed
     */
    private Integer createTransport(pjsip_transport_type_e type, int port)
            throws SameThreadException {
        pjsua_transport_config cfg = new pjsua_transport_config();
        int[] tId = new int[1];
        int status;
        pjsua.transport_config_default(cfg);
        cfg.setPort(port);

        if (type.equals(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS)) {
            pjsip_tls_setting tlsSetting = cfg.getTls_setting();

            /* TODO : THIS IS OBSOLETE -- remove from UI
            String serverName = prefsWrapper
                    .getPreferenceStringValue(SipConfigManager.TLS_SERVER_NAME);
            if (!TextUtils.isEmpty(serverName)) {
                tlsSetting.setServer_name(pjsua.pj_str_copy(serverName));
            }
            */
            
            String caListFile = prefsWrapper
                    .getPreferenceStringValue(SipConfigManager.CA_LIST_FILE);
            if (!TextUtils.isEmpty(caListFile)) {
                tlsSetting.setCa_list_file(pjsua.pj_str_copy(caListFile));
            }

            String certFile = prefsWrapper.getPreferenceStringValue(SipConfigManager.CERT_FILE);
            if (!TextUtils.isEmpty(certFile)) {
                tlsSetting.setCert_file(pjsua.pj_str_copy(certFile));
            }

            String privKey = prefsWrapper.getPreferenceStringValue(SipConfigManager.PRIVKEY_FILE);

            if (!TextUtils.isEmpty(privKey)) {
                tlsSetting.setPrivkey_file(pjsua.pj_str_copy(privKey));
            }

            String tlsPwd = prefsWrapper.getPreferenceStringValue(SipConfigManager.TLS_PASSWORD);
            if (!TextUtils.isEmpty(tlsPwd)) {
                tlsSetting.setPassword(pjsua.pj_str_copy(tlsPwd));
            }

            boolean checkClient = prefsWrapper
                    .getPreferenceBooleanValue(SipConfigManager.TLS_VERIFY_CLIENT);
            tlsSetting.setVerify_client(checkClient ? 1 : 0);

            tlsSetting.setMethod(prefsWrapper.getTLSMethod());
            boolean checkServer = prefsWrapper
                    .getPreferenceBooleanValue(SipConfigManager.TLS_VERIFY_SERVER);
            tlsSetting.setVerify_server(checkServer ? 1 : 0);

            cfg.setTls_setting(tlsSetting);
        }

        // else?
        if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.ENABLE_QOS)) {
            Log.d(THIS_FILE, "Activate qos for this transport");
            pj_qos_params qosParam = cfg.getQos_params();
            qosParam.setDscp_val((short) prefsWrapper.getDSCPVal());
            qosParam.setFlags((short) 1); // DSCP
            cfg.setQos_params(qosParam);
        }

        status = pjsua.transport_create(type, cfg, tId);
        if (status != pjsuaConstants.PJ_SUCCESS) {
            String errorMsg = pjStrToString(pjsua.get_error_message(status));
            String msg = "Fail to create transport " + errorMsg + " (" + status + ")";
            Log.e(THIS_FILE, msg);
            if (status == 120098) { /* Already binded */
                msg = service.getString(R.string.another_application_use_sip_port);
            }
            service.notifyUserOfMessage(msg);
            return null;
        }
        return tId[0];
    }

    public boolean addAccount(SipProfile profile) throws SameThreadException {
        int status = pjsuaConstants.PJ_FALSE;
        if (!created) {
            Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
            return status == pjsuaConstants.PJ_SUCCESS;

        }
        PjSipAccount account = new PjSipAccount(profile);

        account.applyExtraParams(service);

        // Force the use of a transport
        /*
        switch (account.transport) {
            case SipProfile.TRANSPORT_UDP:
                if (udpTranportId != null) {
                    //account.cfg.setTransport_id(udpTranportId);
                }
                break;
            case SipProfile.TRANSPORT_TCP:
                if (tcpTranportId != null) {
                    // account.cfg.setTransport_id(tcpTranportId);
                }
                break;
            case SipProfile.TRANSPORT_TLS:
                if (tlsTransportId != null) {
                    // account.cfg.setTransport_id(tlsTransportId);
                }
                break;
            default:
                break;
        }
        */

        SipProfileState currentAccountStatus = getProfileState(profile);

        if (currentAccountStatus.isAddedToStack()) {
            pjsua.csipsimple_set_acc_user_data(account.cfg, account.css_cfg);
            status = pjsua.acc_modify(currentAccountStatus.getPjsuaId(), account.cfg);
            ContentValues cv = new ContentValues();
            cv.put(SipProfileState.ADDED_STATUS, status);
            service.getContentResolver().update(
                    ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_ID_URI_BASE, profile.id),
                    cv, null, null);

            if (!account.wizard.equalsIgnoreCase(WizardUtils.LOCAL_WIZARD_TAG)) {
                // Re register
                if (status == pjsuaConstants.PJ_SUCCESS) {
                    status = pjsua.acc_set_registration(currentAccountStatus.getPjsuaId(), 1);
                    if (status == pjsuaConstants.PJ_SUCCESS) {
                        pjsua.acc_set_online_status(currentAccountStatus.getPjsuaId(), 1);
                    }
                }
            }
        } else {
            int[] accId = new int[1];
            if (account.wizard.equalsIgnoreCase(WizardUtils.LOCAL_WIZARD_TAG)) {
                // We already have local account by default
                // For now consider we are talking about UDP one
                // In the future local account should be set per transport
                switch(account.transport) {
                    case SipProfile.TRANSPORT_UDP:
                        accId[0] = localUdpAccPjId;
                        break;
                    case SipProfile.TRANSPORT_TCP:
                        accId[0] = localTcpAccPjId;
                        break;
                    case SipProfile.TRANSPORT_TLS:
                        accId[0] = localTlsAccPjId;
                        break;
                    default:
                        // By default use UDP
                        accId[0] = localUdpAccPjId;
                        break;
                }
                
                pjsua_acc_config nCfg = new pjsua_acc_config();
                pjsua.acc_get_config(accId[0], nCfg);
                pjsua.csipsimple_set_acc_user_data(nCfg, account.css_cfg);
                nCfg.setVid_in_auto_show(pjsuaConstants.PJ_TRUE);
                nCfg.setVid_out_auto_transmit(pjsuaConstants.PJ_TRUE);
                status = pjsua.acc_modify(accId[0], nCfg);
            } else {
                // Cause of standard account different from local account :)
                pjsua.csipsimple_set_acc_user_data(account.cfg, account.css_cfg);
                status = pjsua.acc_add(account.cfg, pjsuaConstants.PJ_FALSE, accId);

            }

            if (status == pjsuaConstants.PJ_SUCCESS) {
                SipProfileState ps = new SipProfileState(profile);
                ps.setAddedStatus(status);
                ps.setPjsuaId(accId[0]);
                service.getContentResolver().insert(
                        ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_ID_URI_BASE,
                                account.id), ps.getAsContentValue());

                pjsua.acc_set_online_status(accId[0], 1);
            }
        }

        return status == pjsuaConstants.PJ_SUCCESS;
    }

    /**
     * Synchronize content provider backend from pjsip stack
     * @param pjsuaId the pjsua id of the account to synchronize
     * @throws SameThreadException
     */
    public void updateProfileStateFromService(int pjsuaId) throws SameThreadException {
        if (!created) {
            return;
        }
        long accId = getAccountIdForPjsipId(pjsuaId);
        Log.d(THIS_FILE, "Update profile from service for " + pjsuaId + " aka in db " + accId);
        if (accId != SipProfile.INVALID_ID) {
            int success = pjsuaConstants.PJ_FALSE;
            pjsua_acc_info pjAccountInfo;
            pjAccountInfo = new pjsua_acc_info();
            success = pjsua.acc_get_info(pjsuaId, pjAccountInfo);
            if (success == pjsuaConstants.PJ_SUCCESS && pjAccountInfo != null) {
                ContentValues cv = new ContentValues();

                try {
                    // Should be fine : status code are coherent with RFC
                    // status codes
                    cv.put(SipProfileState.STATUS_CODE, pjAccountInfo.getStatus().swigValue());
                } catch (IllegalArgumentException e) {
                    cv.put(SipProfileState.STATUS_CODE,
                            SipCallSession.StatusCode.INTERNAL_SERVER_ERROR);
                }

                cv.put(SipProfileState.STATUS_TEXT, pjStrToString(pjAccountInfo.getStatus_text()));
                cv.put(SipProfileState.EXPIRES, pjAccountInfo.getExpires());

                service.getContentResolver().update(
                        ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_ID_URI_BASE, accId),
                        cv, null, null);

                Log.d(THIS_FILE, "Profile state UP : " + cv);
            }
        }
    }

    /**
     * Get the dynamic state of the profile
     * @param account the sip profile from database. Important field is id. 
     * @return the dynamic sip profile state
     */
    public SipProfileState getProfileState(SipProfile account) {
        if (!created || account == null) {
            return null;
        }
        if(account.id == SipProfile.INVALID_ID) {
            return null;
        }
        SipProfileState accountInfo = new SipProfileState(account);
        Cursor c = service.getContentResolver().query(
                ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_ID_URI_BASE, account.id),
                null, null, null, null);
        if (c != null) {
            try {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    accountInfo.createFromDb(c);
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles states", e);
            } finally {
                c.close();
            }
        }
        return accountInfo;
    }

    private static ArrayList<String> codecs = new ArrayList<String>();
    private static ArrayList<String> video_codecs = new ArrayList<String>();
    private static boolean codecs_initialized = false;

    /**
     * Reset the list of codecs stored
     */
    public static void resetCodecs() {
        synchronized (codecs) {
            if (codecs_initialized) {
                codecs.clear();
                video_codecs.clear();
                codecs_initialized = false;
            }
        }
    }

    /**
     * Retrieve codecs from pjsip stack and store it inside preference storage 
     * so that it can be retrieved in the interface view
     * @throws SameThreadException
     */
    private void initCodecs() throws SameThreadException {

        synchronized (codecs) {
            if (!codecs_initialized) {
                int nbrCodecs, i;
                
                // Audio codecs
                nbrCodecs = pjsua.codecs_get_nbr();
                for (i = 0; i < nbrCodecs; i++) {
                    String codecId = pjStrToString(pjsua.codecs_get_id(i));
                    codecs.add(codecId);
                    // Log.d(THIS_FILE, "Added codec " + codecId);
                }
                // Set it in prefs if not already set correctly
                prefsWrapper.setCodecList(codecs);
                
                // Video codecs
                nbrCodecs = pjsua.codecs_vid_get_nbr();
                for (i = 0; i < nbrCodecs; i++) {
                    String codecId = pjStrToString(pjsua.codecs_vid_get_id(i));
                    video_codecs.add(codecId);
                    Log.d(THIS_FILE, "Added video codec " + codecId);
                }
                // Set it in prefs if not already set correctly
                prefsWrapper.setVideoCodecList(video_codecs);
                
                codecs_initialized = true;
                // We are now always capable of tls and srtp !
                prefsWrapper.setLibCapability(PreferencesProviderWrapper.LIB_CAP_TLS, true);
                prefsWrapper.setLibCapability(PreferencesProviderWrapper.LIB_CAP_SRTP, true);
            }
        }

    }
    
    /**
     * Append log for the codec in String builder
     * @param sb the buffer to be appended with the codec info
     * @param codec the codec name
     * @param prio the priority of the codec
     */
    private void buffCodecLog(StringBuilder sb, String codec, short prio) {
        if (prio > 0 && Log.getLogLevel() >= 4) {
            sb.append(codec);
            sb.append(" (");
            sb.append(prio);
            sb.append(") - ");
        }
    }

    /**
     * Set the codec priority in pjsip stack layer based on preference store
     * @throws SameThreadException
     */
    private void setCodecsPriorities() throws SameThreadException {
        ConnectivityManager cm = ((ConnectivityManager) service
                .getSystemService(Context.CONNECTIVITY_SERVICE));
        
        synchronized (codecs) {
            if (codecs_initialized) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null) {
                    
                    StringBuilder audioSb = new StringBuilder();
                    StringBuilder videoSb = new StringBuilder();
                    audioSb.append("Audio codecs : ");
                    videoSb.append("Video codecs : ");
                    
                    String currentBandType = prefsWrapper.getPreferenceStringValue(
                            SipConfigManager.getBandTypeKey(ni.getType(), ni.getSubtype()),
                            SipConfigManager.CODEC_WB);
                    
                    synchronized (codecs) {
                        
                        for (String codec : codecs) {
                            short aPrio = prefsWrapper.getCodecPriority(codec, currentBandType,
                                    "-1");
                            buffCodecLog(audioSb, codec, aPrio);
                            pj_str_t codecStr = pjsua.pj_str_copy(codec);
                            if (aPrio >= 0) {
                                pjsua.codec_set_priority(codecStr, aPrio);
                            }
                            
                            String codecKey = SipConfigManager.getCodecKey(codec, SipConfigManager.FRAMES_PER_PACKET_SUFFIX);
                            Integer frmPerPacket = SipConfigManager.getPreferenceIntegerValue(service, codecKey);
                            if(frmPerPacket != null && frmPerPacket > 0) {
                                Log.d(THIS_FILE, "Set codec " + codec + " fpp : " + frmPerPacket);
                                pjsua.codec_set_frames_per_packet(codecStr, frmPerPacket);
                            }
                        }
                        
                        for(String codec : video_codecs) {
                            short aPrio = prefsWrapper.getCodecPriority(codec, currentBandType,
                                    "-1");
                            buffCodecLog(videoSb, codec, aPrio);
                            if (aPrio >= 0) {
                                pjsua.codec_set_priority(pjsua.pj_str_copy(codec), aPrio);
                            }
                        }
                    }
                    
                    Log.d(THIS_FILE, audioSb.toString());
                    Log.d(THIS_FILE, videoSb.toString());
                }
                
                
            }
        }
    }

    // Call related

    /**
     * Answer a call
     * 
     * @param callId the id of the call to answer to
     * @param code the status code to send in the response
     * @return
     */
    public int callAnswer(int callId, int code) throws SameThreadException {

        if (created) {

            pjsua_call_setting cs = new pjsua_call_setting();
            pjsua.call_setting_default(cs);
            cs.setAud_cnt(1);
            cs.setVid_cnt(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO) ? 1 : 0);
            cs.setFlag(0);
            return pjsua.call_answer2(callId, cs, code, null, null);
            //return pjsua.call_answer(callId, code, null, null);
        }
        return -1;
    }

    /**
     * Hangup a call
     * 
     * @param callId the id of the call to hangup
     * @param code the status code to send in the response
     * @return
     */
    public int callHangup(int callId, int code) throws SameThreadException {
        if (created) {
            return pjsua.call_hangup(callId, code, null, null);
        }
        return -1;
    }

    public int callXfer(int callId, String callee) throws SameThreadException {
        if (created) {
            return pjsua.call_xfer(callId, pjsua.pj_str_copy(callee), null);
        }
        return -1;
    }

    public int callXferReplace(int callId, int otherCallId, int options) throws SameThreadException {
        if (created) {
            return pjsua.call_xfer_replaces(callId, otherCallId, options, null);
        }
        return -1;
    }

    /**
     * Make a call
     * 
     * @param callee remote contact ot call If not well formated we try to add
     *            domain name of the default account
     */
    public int makeCall(String callee, int accountId) throws SameThreadException {
        if (!created) {
            return -1;
        }

        final ToCall toCall = sanitizeSipUri(callee, accountId);
        if (toCall != null) {
            pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());

            // Nothing to do with this values
            byte[] userData = new byte[1];
            int[] callId = new int[1];
            pjsua_call_setting cs = new pjsua_call_setting();
            pjsua_msg_data msgData = new pjsua_msg_data();
            int pjsuaAccId = toCall.getPjsipAccountId();
            
            // Call settings to add video
            pjsua.call_setting_default(cs);
            cs.setAud_cnt(1);
            cs.setVid_cnt(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO) ? 1 : 0);
            cs.setFlag(0);
            // Msg data to add headers
            pjsua.msg_data_init(msgData);
            pjsua.csipsimple_init_acc_msg_data(pjsuaAccId, msgData);
            
            int status = pjsua.call_make_call(pjsuaAccId, uri, cs, userData, msgData, callId);
            if(status == pjsuaConstants.PJ_SUCCESS) {
                dtmfToAutoSend.put(callId[0], toCall.getDtmf());
                Log.d(THIS_FILE, "DTMF - Store for " + callId[0] + " - "+toCall.getDtmf());
            }
            return status;
        } else {
            service.notifyUserOfMessage(service.getString(R.string.invalid_sip_uri) + " : "
                    + callee);
        }
        return -1;
    }

    /**
     * Send a dtmf signal to a call
     * 
     * @param callId the call to send the signal
     * @param keyCode the keyCode to send (android style)
     * @return
     */
    public int sendDtmf(int callId, int keyCode) throws SameThreadException {
        if (!created) {
            return -1;
        }
        String keyPressed = "";
        // Since some device (xoom...) are apparently buggy with key character map loading... 
        // we have to do crappy thing here
        if(keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9 ) {
            keyPressed = Integer.toString(keyCode - KeyEvent.KEYCODE_0);
        } else if (keyCode == KeyEvent.KEYCODE_POUND){
            keyPressed = "#";
        } else if (keyCode == KeyEvent.KEYCODE_STAR){
            keyPressed = "*";
        } else {
            // Fallback... should never be there if using visible dialpad, but possible using keyboard
            KeyCharacterMap km = KeyCharacterMap.load(KeyCharacterMap.NUMERIC);
            keyPressed = Integer.toString(km.getNumber(keyCode));
        }
        return sendDtmf(callId, keyPressed);
    }
    
    private int sendDtmf(final int callId, String keyPressed) throws SameThreadException {
        if(TextUtils.isEmpty(keyPressed)) {
            return pjsua.PJ_SUCCESS;
        }
        
        String dtmfToDial = keyPressed;
        String remainingDtmf = "";
        int pauseBeforeRemaining = 0;
        boolean foundSeparator = false;
        if(keyPressed.contains(",") || keyPressed.contains(";")) {
            dtmfToDial = "";
            for(int i = 0; i < keyPressed.length(); i++) {
                char c = keyPressed.charAt(i);
                if(!foundSeparator) {
                    if(c == ',' || c == ';') {
                        pauseBeforeRemaining += (c == ',') ? DTMF_TONE_PAUSE_LENGTH : DTMF_TONE_WAIT_LENGTH;
                        foundSeparator = true;
                    } else {
                        dtmfToDial += c;
                    }
                }else {
                    if ((c == ',' || c == ';') && TextUtils.isEmpty(remainingDtmf)){
                        pauseBeforeRemaining += (c == ',') ? DTMF_TONE_PAUSE_LENGTH : DTMF_TONE_WAIT_LENGTH;
                    }else {
                        remainingDtmf += c;
                    }
                }
            }
            
        }
        
        
        
        pj_str_t pjKeyPressed = pjsua.pj_str_copy(dtmfToDial);
        int res = -1;
        if (prefsWrapper.useSipInfoDtmf()) {
            res = pjsua.send_dtmf_info(callId, pjKeyPressed);
            Log.d(THIS_FILE, "Has been sent DTMF INFO : " + res);
        } else {
            if (!prefsWrapper.forceDtmfInBand()) {
                // Generate using RTP
                res = pjsua.call_dial_dtmf(callId, pjKeyPressed);
                Log.d(THIS_FILE, "Has been sent in RTP DTMF : " + res);
            }

            if (res != pjsua.PJ_SUCCESS && !prefsWrapper.forceDtmfRTP()) {
                // Generate using analogic inband
                if (dtmfDialtoneGenerators.get(callId) == null) {
                    dtmfDialtoneGenerators.put(callId, new PjStreamDialtoneGenerator(callId));
                }
                res = dtmfDialtoneGenerators.get(callId).sendPjMediaDialTone(keyPressed);
                Log.d(THIS_FILE, "Has been sent DTMF analogic : " + res);
            }
        }
        
        
        
        // Finally, push remaining DTMF in the future
        if(!TextUtils.isEmpty(remainingDtmf)) {
            if(tasksTimer == null) {
                tasksTimer = new Timer("com.csipsimple.PjSipServiceTasks");
            }
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    service.getExecutor().execute(new SipRunnable() {
                        @Override
                        protected void doRun() throws SameThreadException {
                            Log.d(THIS_FILE, "Running pending DTMF send");
                            sendPendingDtmf(callId);
                        }
                    });
                }
            };
            dtmfToAutoSend.put(callId, remainingDtmf);
            dtmfTasks.put(callId, tt);
            Log.d(THIS_FILE, "Schedule DTMF " + remainingDtmf + " in "+ pauseBeforeRemaining);
            tasksTimer.schedule(tt, pauseBeforeRemaining);
        }else {
            if(dtmfToAutoSend.get(callId) != null) {
                dtmfToAutoSend.put(callId, null);
            }
            if(dtmfTasks.get(callId) != null) {
                dtmfTasks.put(callId, null);
            }
        }
        
        
        return res;
    }

    /**
     * Send sms/message using SIP server
     */
    public ToCall sendMessage(String callee, String message, long accountId)
            throws SameThreadException {
        if (!created) {
            return null;
        }

        ToCall toCall = sanitizeSipUri(callee, accountId);
        if (toCall != null) {

            pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());
            pj_str_t text = pjsua.pj_str_copy(message);
            /*
             * Log.d(THIS_FILE, "get for outgoing"); int finalAccountId =
             * accountId; if (accountId == -1) { finalAccountId =
             * pjsua.acc_find_for_outgoing(uri); }
             */
            // Nothing to do with this values
            byte[] userData = new byte[1];

            int status = pjsua.im_send(toCall.getPjsipAccountId(), uri, null, text, null, userData);
            return (status == pjsuaConstants.PJ_SUCCESS) ? toCall : null;
        }
        return toCall;
    }
    
    /**
     * Add a buddy to buddies list
     * @param buddyUri the uri to register to
     * @throws SameThreadException
     */
    public int addBuddy(String buddyUri) throws SameThreadException {
        if (!created) {
            return -1;
        }
        int[] p_buddy_id = new int[1];
        
        pjsua_buddy_config buddy_cfg = new pjsua_buddy_config();
        pjsua.buddy_config_default(buddy_cfg);
        buddy_cfg.setSubscribe(1);
        buddy_cfg.setUri(pjsua.pj_str_copy(buddyUri));
        
        pjsua.buddy_add(buddy_cfg , p_buddy_id);
        
        return p_buddy_id[0];
    }
    
    /**
     * Remove one buddy from the buddy list managed by pjsip
     * @param buddyUri he uri to unregister
     * @throws SameThreadException
     */
    public void removeBuddy(String buddyUri) throws SameThreadException {
        if (!created) {
            return;
        }
        int buddyId = pjsua.buddy_find(pjsua.pj_str_copy(buddyUri));
        if(buddyId >= 0) {
            pjsua.buddy_del(buddyId);
        }
    }
    
    public void sendPendingDtmf(int callId) throws SameThreadException {
        if(dtmfToAutoSend.get(callId) != null) {
            Log.d(THIS_FILE, "DTMF - Send pending dtmf " + dtmfToAutoSend.get(callId) + " for " + callId);
            sendDtmf(callId, dtmfToAutoSend.get(callId));
        }
    }

    public void stopDialtoneGenerator(int callId) {
        if (dtmfDialtoneGenerators.get(callId) != null) {
            dtmfDialtoneGenerators.get(callId).stopDialtoneGenerator();
            dtmfDialtoneGenerators.put(callId, null);
        }
        if(dtmfToAutoSend.get(callId) != null) {
            dtmfToAutoSend.put(callId, null);
        }
        if(dtmfTasks.get(callId) != null) {
            dtmfTasks.get(callId).cancel();
            dtmfTasks.put(callId, null);
        }
    }

    public int callHold(int callId) throws SameThreadException {
        if (created) {
            return pjsua.call_set_hold(callId, null);
        }
        return -1;
    }

    public int callReinvite(int callId, boolean unhold) throws SameThreadException {
        if (created) {
            return pjsua.call_reinvite(callId,
                    unhold ? pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue() : 0, null);

        }
        return -1;
    }

    public SipCallSession getCallInfo(int callId) {
        if (created/* && !creating */&& userAgentReceiver != null) {
            SipCallSession callInfo = userAgentReceiver.getCallInfo(callId);
            return callInfo;
        }
        return null;
    }

    public void setBluetoothOn(boolean on) throws SameThreadException {
        if (created && mediaManager != null) {
            mediaManager.setBluetoothOn(on);
        }
    }

    /**
     * Mute microphone
     * @param on true if microphone has to be muted
     * @throws SameThreadException
     */
    public void setMicrophoneMute(boolean on) throws SameThreadException {
        if (created && mediaManager != null) {
            mediaManager.setMicrophoneMute(on);
        }
    }

    /**
     * Change speaker phone mode
     * @param on true if the speaker mode has to be on.
     * @throws SameThreadException
     */
    public void setSpeakerphoneOn(boolean on) throws SameThreadException {
        if (created && mediaManager != null) {
            mediaManager.setSpeakerphoneOn(on);
        }
    }

    public SipCallSession[] getCalls() {
        if (created && userAgentReceiver != null) {
            SipCallSession[] callsInfo = userAgentReceiver.getCalls();
            return callsInfo;
        }
        return new SipCallSession[0];
    }

    public void confAdjustTxLevel(int port, float value) throws SameThreadException {
        if (created && userAgentReceiver != null) {
            pjsua.conf_adjust_tx_level(port, value);
        }
    }

    public void confAdjustRxLevel(int port, float value) throws SameThreadException {
        if (created && userAgentReceiver != null) {
            pjsua.conf_adjust_rx_level(port, value);
        }
    }

    public void setEchoCancellation(boolean on) throws SameThreadException {
        if (created && userAgentReceiver != null) {
            Log.d(THIS_FILE, "set echo cancelation " + on);
            pjsua.set_ec(on ? prefsWrapper.getEchoCancellationTail() : 0,
                    prefsWrapper.getPreferenceIntegerValue(SipConfigManager.ECHO_MODE));
        }
    }

    public void adjustStreamVolume(int stream, int direction, int flags) {
        if (mediaManager != null) {
            mediaManager.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI);
        }
    }

    public void silenceRinger() {
        if (mediaManager != null) {
            mediaManager.stopRingAndUnfocus();
        }
    }

    /**
     * Change account registration / adding state
     * @param account The account to modify registration
     * @param renew if 0 we ask for deletion of this account; if 1 we ask for registration of this account (and add if necessary)
     * @param forceReAdd if true, we will first remove the account and then re-add it
     * @return true if the operation get completed without problem
     * @throws SameThreadException
     */
    public boolean setAccountRegistration(SipProfile account, int renew, boolean forceReAdd)
            throws SameThreadException {
        int status = -1;
        if (!created || account == null) {
            Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
            return false;
        }
        if(account.id == SipProfile.INVALID_ID) {
            Log.w(THIS_FILE, "Trying to set registration on a deleted account");
            return false;
        }

        // If local account -- Ensure we are not deleting, because this would be invalid
        if(account.wizard.equalsIgnoreCase(WizardUtils.LOCAL_WIZARD_TAG)) {
            if(renew == 0) {
                return false;
            }
        }
        
        SipProfileState profileState = getProfileState(account);
        
        // In case of already added, we have to act finely
        // If it's local we can just consider that we have to re-add account 
        // since it will actually just touch the account with a modify
        if (profileState != null && profileState.isAddedToStack() && !account.wizard.equalsIgnoreCase(WizardUtils.LOCAL_WIZARD_TAG)) {
            // The account is already there in accounts list
            service.getContentResolver().delete(
                    ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_URI, account.id), null,
                    null);
            Log.d(THIS_FILE, "Account already added to stack, remove and re-load or delete");
            if (renew == 1) {
                if (forceReAdd) {
                    status = pjsua.acc_del(profileState.getPjsuaId());
                    addAccount(account);
                } else {
                    pjsua.acc_set_online_status(profileState.getPjsuaId(), getOnlineForStatus(service.getPresence()));
                    status = pjsua.acc_set_registration(profileState.getPjsuaId(), renew);
                }
            } else {
                // if(status == pjsuaConstants.PJ_SUCCESS && renew == 0) {
                Log.d(THIS_FILE, "Delete account !!");
                status = pjsua.acc_del(profileState.getPjsuaId());
            }
        } else {
            if (renew == 1) {
                addAccount(account);
            } else {
                Log.w(THIS_FILE, "Ask to unregister an unexisting account !!" + account.id);
            }

        }
        // PJ_SUCCESS = 0
        return status == 0;
    }
    

    /**
     * Set self presence
     * @param presence the SipManager.SipPresence
     * @param statusText the text of the presence
     * @throws SameThreadException
     */
    public void setPresence(PresenceStatus presence, String statusText, long accountId) throws SameThreadException  {
        if (!created) {
            Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
            return;
        }
        SipProfile account = new SipProfile();
        account.id = accountId;
        SipProfileState profileState = getProfileState(account);
        
        // In case of already added, we have to act finely
        // If it's local we can just consider that we have to re-add account 
        // since it will actually just touch the account with a modify
        if (profileState != null && profileState.isAddedToStack()) {
            // The account is already there in accounts list
            pjsua.acc_set_online_status(profileState.getPjsuaId(), getOnlineForStatus(presence));
        }
        
    }
    
    private int getOnlineForStatus(PresenceStatus presence) {
        return presence == PresenceStatus.ONLINE ? 1 : 0;
    }

    public long getAccountIdForPjsipId(int pjId) {
        long accId = SipProfile.INVALID_ID;

        Cursor c = service.getContentResolver().query(SipProfile.ACCOUNT_STATUS_URI, null, null,
                null, null);
        if (c != null) {
            try {
                c.moveToFirst();
                do {
                    int pjsuaId = c.getInt(c.getColumnIndex(SipProfileState.PJSUA_ID));
                    Log.d(THIS_FILE, "Found pjsua " + pjsuaId + " searching " + pjId);
                    if (pjsuaId == pjId) {
                        accId = c.getInt(c.getColumnIndex(SipProfileState.ACCOUNT_ID));
                        break;
                    }
                } while (c.moveToNext());
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }
        return accId;
    }

    public SipProfile getAccountForPjsipId(int pjId) {
        long accId = getAccountIdForPjsipId(pjId);
        if (accId == SipProfile.INVALID_ID) {
            return null;
        } else {
            return service.getAccount(accId);
        }
    }

    public int validateAudioClockRate(int aClockRate) {
        if (mediaManager != null) {
            return mediaManager.validateAudioClockRate(aClockRate);
        }
        return -1;
    }
    
    public void setAudioInCall() {
        if (mediaManager != null) {
            mediaManager.setAudioInCall();
        }
    }

    public void unsetAudioInCall() {

        if (mediaManager != null) {
            mediaManager.unsetAudioInCall();
        }
    }

    public SipCallSession getActiveCallInProgress() {
        if (created && userAgentReceiver != null) {
            return userAgentReceiver.getActiveCallInProgress();
        }
        return null;
    }

    /**
     * Transform a string callee into a valid sip uri in the context of an
     * account
     * 
     * @param callee the callee string to call
     * @param accountId the context account
     * @return ToCall object representing what to call and using which account
     */
    private ToCall sanitizeSipUri(String callee, long accountId) throws SameThreadException {
        // accountId is the id in term of csipsimple database
        // pjsipAccountId is the account id in term of pjsip adding
        int pjsipAccountId = (int) SipProfile.INVALID_ID;

        // Fake a sip profile empty to get it's profile state
        // Real get from db will be done later
        SipProfile account = new SipProfile();
        account.id = accountId;
        SipProfileState profileState = getProfileState(account);
        long finalAccountId = accountId;
        

        // If this is an invalid account id
        if (accountId == SipProfile.INVALID_ID || !profileState.isAddedToStack()) {
            int defaultPjsipAccount = pjsua.acc_get_default();

            boolean valid = false;
            account = getAccountForPjsipId(defaultPjsipAccount);
            if (account != null) {
                profileState = getProfileState(account);
                valid = profileState.isAddedToStack();
            }
            // If default account is not active
            if (!valid) {
                Cursor c = service.getContentResolver().query(SipProfile.ACCOUNT_STATUS_URI, null,
                        null, null, null);
                if (c != null) {
                    try {
                        if (c.getCount() > 0) {
                            c.moveToFirst();
                            do {
                                SipProfileState ps = new SipProfileState(c);
                                if (ps.isValidForCall()) {
                                    finalAccountId = ps.getDatabaseId();
                                    pjsipAccountId = ps.getPjsuaId();
                                    break;
                                }
                            } while (c.moveToNext());
                        }
                    } catch (Exception e) {
                        Log.e(THIS_FILE, "Error on looping over sip profiles state", e);
                    } finally {
                        c.close();
                    }
                }
            } else {
                // Use the default account
                finalAccountId = profileState.getDatabaseId();
                pjsipAccountId = profileState.getPjsuaId();
            }
        } else {
            // If the account is valid
            pjsipAccountId = profileState.getPjsuaId();
        }

        if (pjsipAccountId == SipProfile.INVALID_ID) {
            Log.e(THIS_FILE, "Unable to find a valid account for this call");
            return null;
        }

        // Check integrity of callee field
        
        ParsedSipContactInfos finalCallee = SipUri.parseSipContact(callee);
        
        if (TextUtils.isEmpty(finalCallee.domain) ||
                TextUtils.isEmpty(finalCallee.scheme)) {
            Log.d(THIS_FILE, "default acc : " + finalAccountId);
            account = service.getAccount((int) finalAccountId);
        }
        
        if (TextUtils.isEmpty(finalCallee.domain)) {
            String defaultDomain = account.getDefaultDomain();
            finalCallee.domain = defaultDomain;
        }
        if(TextUtils.isEmpty(finalCallee.scheme)) {
            if(account.transport == SipProfile.TRANSPORT_TLS) {
                finalCallee.scheme = "sips";
            }else {
                finalCallee.scheme = "sip";
            }
        }
        String digitsToAdd = null;
        if(!TextUtils.isEmpty(finalCallee.userName) && finalCallee.userName.contains(",")) {
            int commaIndex = finalCallee.userName.indexOf(",");
            digitsToAdd = finalCallee.userName.substring(commaIndex);
            finalCallee.userName = finalCallee.userName.substring(0, commaIndex);
        }
        
        Log.d(THIS_FILE, "will call " + finalCallee);
        
        if (pjsua.verify_sip_url(finalCallee.toString(false)) == 0) {
            // In worse worse case, find back the account id for uri.. but
            // probably useless case
            if (pjsipAccountId == SipProfile.INVALID_ID) {
                pjsipAccountId = pjsua.acc_find_for_outgoing(pjsua.pj_str_copy(finalCallee.toString(false)));
            }
            return new ToCall(pjsipAccountId, finalCallee.toString(false), digitsToAdd);
        }

        return null;
    }

    public void onGSMStateChanged(int state, String incomingNumber) throws SameThreadException {
        // Avoid ringing if new GSM state is not idle
        if (state != TelephonyManager.CALL_STATE_IDLE && mediaManager != null) {
            mediaManager.stopRingAndUnfocus();
        }

        
        
        // If new call state is not idle
        if (state != TelephonyManager.CALL_STATE_IDLE && userAgentReceiver != null) {
            SipCallSession currentActiveCall = userAgentReceiver.getActiveCallInProgress();
            // If we have a sip call on our side
            if (currentActiveCall != null) {
                AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // GSM is now off hook => hold current sip call
                    hasBeenHoldByGSM = currentActiveCall.getCallId();
                    callHold(hasBeenHoldByGSM);
                    pjsua.set_no_snd_dev();

                    am.setMode(AudioManager.MODE_IN_CALL);
                } else {
                    // We have a ringing incoming call.
                    // Avoid ringing
                    hasBeenChangedRingerMode = am.getRingerMode();
                    am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    // And try to notify with tone
                    if(mediaManager != null) {
                        mediaManager.playInCallTone(MediaManager.TONE_CALL_WAITING);
                    }
                }
            }
        } else {
            // GSM is now back to an IDLE state, resume previously stopped SIP
            // calls
            if (hasBeenHoldByGSM != null && isCreated()) {
                pjsua.set_snd_dev(0, 0);
                callReinvite(hasBeenHoldByGSM, true);
                hasBeenHoldByGSM = null;
            }
            
            // GSM is now back to an IDLE state, reset ringerMode if was changed.
            if(hasBeenChangedRingerMode != null) {
                AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
                am.setRingerMode(hasBeenChangedRingerMode);
                hasBeenChangedRingerMode = null;
            }
        }
    }

    /*
     * public void sendKeepAlivePackets() throws SameThreadException {
     * ArrayList<SipProfileState> accounts = getActiveProfilesState(); for
     * (SipProfileState acc : accounts) {
     * pjsua.send_keep_alive(acc.getPjsuaId()); } }
     */

    public void zrtpSASVerified(int dataPtr) throws SameThreadException {
        if (!created) {
            return;
        }
        pjsua.jzrtp_SASVerified(dataPtr);
    }

    // Config subwrapper
    private pj_str_t[] getNameservers() {
        pj_str_t[] nameservers = null;

        if (prefsWrapper.enableDNSSRV()) {
            String prefsDNS = prefsWrapper
                    .getPreferenceStringValue(SipConfigManager.OVERRIDE_NAMESERVER);
            if (TextUtils.isEmpty(prefsDNS)) {
                String ipv6Escape = "[ \\[\\]]";
                String ipv4Matcher = "^\\d+(\\.\\d+){3}$";
                String ipv6Matcher = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$";
                List<String> dnsServers;
                List<String> dnsServersAll = new ArrayList<String>();
                List<String> dnsServersIpv4 = new ArrayList<String>();
                for(int i = 1; i <= 2; i++) {
                    String dnsName = prefsWrapper.getSystemProp("net.dns"+i);
                    if(!TextUtils.isEmpty(dnsName)) {
                        dnsName = dnsName.replaceAll(ipv6Escape, "");
                        if (!TextUtils.isEmpty(dnsName) && !dnsServersAll.contains(dnsName)) {
                                if(dnsName.matches(ipv4Matcher) || dnsName.matches(ipv6Matcher)) {
                                    dnsServersAll.add(dnsName);
                                }
                                if(dnsName.matches(ipv4Matcher)) {
                                    dnsServersIpv4.add(dnsName);
                                }
                        }
                    }
                }
                
                if(dnsServersIpv4.size() > 0) {
                    // Prefer pure ipv4 list since pjsua doesn't manage ipv6 resolution yet
                    dnsServers = dnsServersIpv4;
                }else {
                    dnsServers = dnsServersAll;
                }
                
                if (dnsServers.size() == 0) {
                    // This is the ultimate fallback... we should never be there !
                    nameservers = new pj_str_t[] {
                        pjsua.pj_str_copy("127.0.0.1")
                    };
                } else if (dnsServers.size() == 1) {
                    nameservers = new pj_str_t[] {
                        pjsua.pj_str_copy(dnsServers.get(0))
                    };
                } else {
                    nameservers = new pj_str_t[] {
                        pjsua.pj_str_copy(dnsServers.get(0)),
                        pjsua.pj_str_copy(dnsServers.get(1))
                    };
                }
            } else {
                nameservers = new pj_str_t[] {
                    pjsua.pj_str_copy(prefsDNS)
                };
            }
        }
        return nameservers;
    }

    private pjmedia_srtp_use getUseSrtp() {
        try {
            int use_srtp = Integer.parseInt(prefsWrapper
                    .getPreferenceStringValue(SipConfigManager.USE_SRTP));
            if(use_srtp >= 0) {
                return pjmedia_srtp_use.swigToEnum(use_srtp);
            }
        } catch (NumberFormatException e) {
            Log.e(THIS_FILE, "Transport port not well formated");
        }
        return pjmedia_srtp_use.PJMEDIA_SRTP_DISABLED;
    }

    public void setNoSnd() throws SameThreadException {
        if (!created) {
            return;
        }
        pjsua.set_no_snd_dev();
    }

    public void setSnd() throws SameThreadException {
        if (!created) {
            return;
        }
        pjsua.set_snd_dev(0, 0);
    }


    // Recorder
    /**
     * Start recording of a call.
     * 
     * @param callId the call id of the call to record
     * @throws SameThreadException virtual exception to be sure we are calling this from correct thread
     */
    public void startRecording(int callId) throws SameThreadException {
        // Make sure we are in a valid state for recording
        if (!canRecord(callId)) {
            return;
        }
        SipCallSession callInfo = getCallInfo(callId);
        File wavFile = getRecordFile(callInfo.getRemoteContact());
        if (wavFile != null) {
            pj_str_t filename = pjsua.pj_str_copy(wavFile.getAbsolutePath());
            if (pjsua.call_recording_start(callId, filename, pjsuaConstants.PJ_FALSE) == pjsua.PJ_SUCCESS) {
                userAgentReceiver.updateRecordingStatus(callId, false, true);
            } else {
                service.notifyUserOfMessage(R.string.cant_write_file);
            }
        } else {
            service.notifyUserOfMessage(R.string.cant_write_file);
        }
    }

    /**
     * Stop recording of a call.
     * 
     * @param callId the call to stop record for.
     * @throws SameThreadException virtual exception to be sure we are calling this from correct thread
     */
    public void stopRecording(int callId) throws SameThreadException {
        if (!created) {
            return;
        }

        if (pjsua.call_recording_stop(callId) == pjsuaConstants.PJ_SUCCESS) {
            userAgentReceiver.updateRecordingStatus(callId, true, false);
        }
    }
    
    /**
     * Can we record for this call id ?
     * 
     * @param callId The call id to record to a file
     * @return true if seems to be possible to record this call.
     */
    public boolean canRecord(int callId) {
        if (!created) {
            // Not possible to record if service not here
            return false;
        }
        SipCallSession callInfo = getCallInfo(callId);
        if (callInfo == null) {
            // Not possible to record if no call info for given call id
            return false;
        }
        int ms = callInfo.getMediaStatus();
        if( ms != SipCallSession.MediaState.ACTIVE &&
             ms != SipCallSession.MediaState.REMOTE_HOLD) {
            // We can't record if media state not running on our side
            return false;
        }
        return true;
    
    }
    
    /**
     * Are we currently recording the call?
     * 
     * @param callId The call id to test for a recorder presence 
     * @return true if recording this call
     */
    public boolean isRecording(int callId) throws SameThreadException {
        return (pjsua.call_recording_status(callId) == pjsuaConstants.PJ_TRUE);
    }

    /**
     * Get the file to record to for a given remote contact.
     * This will implicitly get the current date in file name.
     * 
     * @param remoteContact The remote contact name
     * @return The file to store conversation
     */
    private File getRecordFile(String remoteContact) {
        File dir = PreferencesProviderWrapper.getRecordsFolder(service);
        if (dir != null) {
            Date d = new Date();
            File file = new File(dir.getAbsoluteFile() + File.separator
                    + sanitizeForFile(remoteContact) + "_"
                    + DateFormat.format("MM-dd-yy_kkmmss", d) + ".wav");
            Log.d(THIS_FILE, "Out dir " + file.getAbsolutePath());
            return file;
        }
        return null;
    }

    private String sanitizeForFile(String remoteContact) {
        String fileName = remoteContact;
        fileName = fileName.replaceAll("[\\.\\\\<>:; \"\'\\*]", "_");
        return fileName;
    }

    // Wave player
    public final static int INVALID_PLAYER = -1;
    private SparseArray<Integer> playedCalls = new SparseArray<Integer>();
    public final static int BITMASK_OUT = 1 << 0;
    public final static int BITMASK_IN = 1 << 1;
    
    /**
     * Play one wave file in call stream.
     * 
     * @param filePath The path to the file we'd like to play
     * @param callId The call id we want to play to. Even if we only use
     *            {@link #BITMASK_IN} this must correspond to some call since
     *            it's used to identify internally created player.
     * @param way The way we want to play this file to. Bitmasked value that
     *            could be compounded of {@link #BITMASK_IN} (read local) and
     *            {@link #BITMASK_OUT} (read to remote party of the call)
     * @throws SameThreadException virtual exception to be sure we are calling
     *             this from correct thread
     */
    public void playWaveFile(String filePath, int callId, int way) throws SameThreadException {
        if (!created) {
            return;
        }

        // Stop any current player
        stopPlaying(callId);

        if(TextUtils.isEmpty(filePath)) {
            // Nothing to do if we have not file path
            return;
        }
        
        // We create a new player conf port.
        
        int[] plId = new int[1];
        int status = pjsua.player_create(pjsua.pj_str_copy(filePath), 1 /* PJMEDIA_FILE_NO_LOOP */, plId);
        
        if (status == pjsuaConstants.PJ_SUCCESS) {
            // Save player
            int playerId = plId[0];
            playedCalls.put(callId, playerId);
            
            // Connect player to requested ports
            int wavPort = pjsua.player_get_conf_port(playerId);
            if ((way & BITMASK_OUT) == BITMASK_OUT) {
                SipCallSession callInfo = getCallInfo(callId);
                int wavConfPort = callInfo.getConfPort();
                pjsua.conf_connect(wavPort, wavConfPort);
            }
            if ((way & BITMASK_IN) == BITMASK_IN) {
                pjsua.conf_connect(wavPort, 0);
            }
            // Once connected, start to play
            pjsua.player_set_pos(playerId, 0);
        }
        
    }

    /**
     * Stop eventual player for a given call.
     * 
     * @param callId the call id corresponding to player previously created with
     *            {@link #playWaveFile(String, int, int)}
     * @throws SameThreadException virtual exception to be sure we are calling
     *             this from correct thread
     */
    public void stopPlaying(int callId) throws SameThreadException {
        int playerId = playedCalls.get(callId, INVALID_PLAYER);
        if(playerId != INVALID_PLAYER) {
            pjsua.player_destroy(playerId);
            playedCalls.delete(callId);
        }
    }

    public void updateTransportIp(String oldIPAddress) throws SameThreadException {
        if (!created) {
            return;
        }
        Log.d(THIS_FILE, "Trying to update my address in the current call to " + oldIPAddress);
        pjsua.update_transport(pjsua.pj_str_copy(oldIPAddress));
    }

    public static String pjStrToString(pj_str_t pjStr) {
        try {
            if (pjStr != null) {
                // If there's utf-8 ptr length is possibly lower than slen
                int len = pjStr.getSlen();
                if(pjStr.getPtr() != null) {
                    if(pjStr.getPtr().length() < len) {
                        len = pjStr.getPtr().length();
                    }
                
                    if (len > 0) {
                        return pjStr.getPtr().substring(0, len);
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            Log.e(THIS_FILE, "Impossible to retrieve string from pjsip ", e);
        }
        return "";
    }

    public long getRxTxLevel(int port) {
        long[] rx_level = new long[1];
        long[] tx_level = new long[1];
        pjsua.conf_get_signal_level(port, tx_level, rx_level);
        return (rx_level[0] << 8 | tx_level[0] );
    }


}
