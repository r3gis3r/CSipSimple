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
package com.csipsimple.pjsip;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_qos_params;
import org.pjsip.pjsua.pj_qos_type;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_tls_setting;
import org.pjsip.pjsua.pjsip_transport_type_e;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_call_flag;
import org.pjsip.pjsua.pjsua_config;
import org.pjsip.pjsua.pjsua_logging_config;
import org.pjsip.pjsua.pjsua_media_config;
import org.pjsip.pjsua.pjsua_transport_config;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyCharacterMap;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.service.MediaManager;
import com.csipsimple.service.SipService;
import com.csipsimple.service.SipService.ToCall;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class PjSipService {
	private static final String THIS_FILE = "PjService";
	public SipService service;

	private boolean created = false;

	public static boolean hasSipStack = false;
	private boolean sipStackIsCorrupted = false;
	public static boolean creating = false;
	private Integer udpTranportId, tcpTranportId, tlsTransportId;
	public PreferencesWrapper prefsWrapper;
	private PjStreamDialtoneGenerator dialtoneGenerator;

	private Integer hasBeenHoldByGSM = null;

	public UAStateReceiver userAgentReceiver;
	public MediaManager mediaManager;

	// -------
	// Locks
	// -------

	private Object creatingSipStack = new Object();
	private Object pjAccountsCreationLock = new Object();
	private Object activeAccountsLock = new Object();
	public Object callActionLock = new Object();

	// Map active account id (id for sql settings database) with acc_id (id for
	// pjsip)
	private HashMap<Integer, Integer> activeAccounts = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> accountsAddingStatus = new HashMap<Integer, Integer>();

	public PjSipService() {

	}

	public void setService(SipService aService) {
		service = aService;
		prefsWrapper = service.prefsWrapper;
	}

	public boolean isCreated() {
		return created;
	}

	public boolean tryToLoadStack() {
		if (hasSipStack) {
			return true;
		}

		File stackFile = NativeLibManager.getStackLibFile(service);
		if (stackFile != null && !sipStackIsCorrupted) {
			try {
				// Try to load the stack
				System.load(stackFile.getAbsolutePath());
				hasSipStack = true;
				return true;
			} catch (UnsatisfiedLinkError e) {
				// If it fails we probably are running on a special hardware,
				// redirect to support webpage
				Log.e(THIS_FILE, "We have a problem with the current stack.... NOT YET Implemented", e);
				hasSipStack = false;
				sipStackIsCorrupted = true;

				/*
				 * //Obsolete Intent it = new Intent(Intent.ACTION_VIEW);
				 * it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				 * it.setData(Uri.parse(
				 * "http://code.google.com/p/csipsimple/wiki/NewHardwareSupportRequest"
				 * ));
				 * 
				 * startActivity(it);
				 */
				// service.stopSelf();
				return false;
			} catch (Exception e) {
				Log.e(THIS_FILE, "We have a problem with the current stack....", e);
			}
		}
		return false;
	}

	// Start the sip stack according to current settings
	public synchronized boolean sipStart() {

		Log.setLogLevel(prefsWrapper.getLogLevel());

		if (!hasSipStack) {
			Log.e(THIS_FILE, "We have no sip stack, we can't start");
			return false;
		}

		try {
			synchronized (creatingSipStack) {
				// Ensure the stack is not already created or is being created
				if (!created/* && !creating */) {
					Log.d(THIS_FILE, "Starting sip stack");
					creating = true;
					udpTranportId = null;
					tcpTranportId = null;

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

						// GLOBAL CONFIG
						pjsua.config_default(cfg);
						Log.d(THIS_FILE, "default cb");
						cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);

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

						// MAIN CONFIG
						pjsua.set_use_compact_form(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM) ? pjsua.PJ_TRUE:pjsua.PJ_FALSE);
						cfg.setUser_agent(pjsua.pj_str_copy(prefsWrapper.getUserAgent(service)));
						cfg.setThread_cnt(prefsWrapper.getThreadCount());
						cfg.setUse_srtp(prefsWrapper.getUseSrtp());
						cfg.setSrtp_secure_signaling(0);

						// DNS
						if (prefsWrapper.enableDNSSRV() && !prefsWrapper.useIPv6()) {
							pj_str_t[] nameservers = prefsWrapper.getNameservers();
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
							String[] servers = prefsWrapper.getStunServer().split(",");
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

						// MEDIA CONFIG
						pjsua.media_config_default(mediaCfg);

						// For now only this cfg is supported
						mediaCfg.setChannel_count(1);
						mediaCfg.setSnd_auto_close_time(prefsWrapper.getAutoCloseTime());
						// Echo cancellation
						mediaCfg.setEc_tail_len(prefsWrapper.getEchoCancellationTail());
						mediaCfg.setEc_options(prefsWrapper.getEchoMode());
						mediaCfg.setNo_vad(prefsWrapper.getNoVad());
						mediaCfg.setQuality(prefsWrapper.getMediaQuality());
						mediaCfg.setClock_rate(prefsWrapper.getClockRate());
						mediaCfg.setAudio_frame_ptime(prefsWrapper.getAudioFramePtime());
						mediaCfg.setHas_ioqueue(prefsWrapper.getHasIOQueue());
						

						// ICE
						mediaCfg.setEnable_ice(prefsWrapper.getIceEnabled());
						// TURN
						int isTurnEnabled = prefsWrapper.getTurnEnabled();
						if (isTurnEnabled == 1) {
							mediaCfg.setEnable_turn(isTurnEnabled);
							mediaCfg.setTurn_server(pjsua.pj_str_copy(prefsWrapper.getTurnServer()));
							pjsua.set_turn_cfg(mediaCfg, pjsua.pj_str_copy(prefsWrapper.getPreferenceStringValue(SipConfigManager.TURN_USERNAME)),
									pjsua.pj_str_copy(prefsWrapper.getPreferenceStringValue(SipConfigManager.TURN_PASSWORD)));

						}

						// INITIALIZE
						status = pjsua.csipsimple_init(cfg, logCfg, mediaCfg);
						if (status != pjsuaConstants.PJ_SUCCESS) {
							String msg = "Fail to init pjsua " + pjsua.get_error_message(status).getPtr();
							Log.e(THIS_FILE, msg);
							service.notifyUserOfMessage(msg);
							cleanPjsua();
							return false;
						}

						/*
						 * if (stunServersCount > 0) { int s =
						 * pjsua.detect_nat_type(); Log.d(THIS_FILE,
						 * ">>> NAT TYPE is "+s); }
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
							int[] p_acc_id = new int[1];
							pjsua.acc_add_local(udpTranportId, pjsua.PJ_FALSE, p_acc_id);

							// Log.d(THIS_FILE, "Udp account "+p_acc_id);

						}
						// TCP
						if (prefsWrapper.isTCPEnabled() && !prefsWrapper.useIPv6()) {
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

						}

						// TLS
						if (prefsWrapper.isTLSEnabled() && !prefsWrapper.useIPv6() && (pjsua.can_use_tls() == pjsuaConstants.PJ_TRUE)) {
							tlsTransportId = createTransport(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, prefsWrapper.getTLSTransportPort());

							if (tlsTransportId == null) {
								cleanPjsua();
								return false;
							}
							// We need a local account to not have the
							// application lost when direct call to the IP
							int[] p_acc_id = new int[1];
							pjsua.acc_add_local(tlsTransportId, pjsua.PJ_FALSE, p_acc_id);
						}

						// RTP transport
						{
							pjsua_transport_config cfg = new pjsua_transport_config();
							pjsua.transport_config_default(cfg);
							cfg.setPort(prefsWrapper.getRTPPort());
							if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.ENABLE_QOS)) {
								Log.d(THIS_FILE, "Activate qos for voice packets");
								cfg.setQos_type(pj_qos_type.PJ_QOS_TYPE_VOICE);
							}

							if (prefsWrapper.useIPv6()) {
								status = pjsua.media_transports_create_ipv6(cfg);
							} else {
								status = pjsua.media_transports_create(cfg);
							}
							if (status != pjsuaConstants.PJ_SUCCESS) {
								String msg = "Fail to add media transport " + pjsua.get_error_message(status).getPtr();
								Log.e(THIS_FILE, msg);

								service.notifyUserOfMessage(msg);
								cleanPjsua();
								return false;
							}
						}
					}

					// Initialization is done, now start pjsua
					status = pjsua.start();

					if (status != pjsua.PJ_SUCCESS) {
						String msg = "Fail to start pjsip  " + pjsua.get_error_message(status).getPtr();
						Log.e(THIS_FILE, msg);
						service.notifyUserOfMessage(msg);
						cleanPjsua();
						return false;
					}

					// Init media codecs
					initCodecs();
					setCodecsPriorities();

					created = true;

					// Add accounts
					creating = false;

					return true;
				}
			}

		} catch (IllegalMonitorStateException e) {
			Log.e(THIS_FILE, "Not able to start sip right now", e);
		}

		return false;
	}

	/**
	 * Stop sip service
	 * @return true if stop has been performed
	 */
	public synchronized boolean sipStop() {
		Log.d(THIS_FILE, ">> SIP STOP <<");

		if (getActiveCallInProgress() != null) {
			Log.w(THIS_FILE, "We have a call in progress... DO NOT STOP !!!");
			// TODO : queue quit on end call;
			return false;
		}

		if (service.notificationManager != null) {
			service.notificationManager.cancelRegisters();
		}
		synchronized (creatingSipStack) {
			if (created) {
				cleanPjsua();
			}
		}
		Log.i(THIS_FILE, ">> Media m " + mediaManager);
		return true;
	}

	private void cleanPjsua() {
		Log.d(THIS_FILE, "Detroying...");
		// This will destroy all accounts so synchronize with accounts
		// management lock
		synchronized (pjAccountsCreationLock) {
			pjsua.csipsimple_destroy();
			synchronized (activeAccountsLock) {
				accountsAddingStatus.clear();
				activeAccounts.clear();
			}
		}
		if (userAgentReceiver != null) {
			userAgentReceiver.stopService();
			userAgentReceiver = null;
		}

		if (mediaManager != null) {
			mediaManager.stopService();
			mediaManager = null;
		}
		creating = false;
		created = false;
	}

	/**
	 * Utility to create a transport
	 * 
	 * @return transport id or -1 if failed
	 */
	private Integer createTransport(pjsip_transport_type_e type, int port) {
		pjsua_transport_config cfg = new pjsua_transport_config();
		int[] tId = new int[1];
		int status;
		pjsua.transport_config_default(cfg);
		cfg.setPort(port);

		if (type.equals(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS)) {
			pjsip_tls_setting tlsSetting = cfg.getTls_setting();
			/*
			 * String serverName =
			 * prefsWrapper.getPreferenceStringValue(PreferencesWrapper
			 * .TLS_SERVER_NAME); if (!TextUtils.isEmpty(serverName)) {
			 * tlsSetting.setServer_name(pjsua.pj_str_copy(serverName)); }
			 * String caListFile =
			 * prefsWrapper.getPreferenceStringValue(PreferencesWrapper
			 * .CA_LIST_FILE); if (!TextUtils.isEmpty(caListFile)) {
			 * tlsSetting.setCa_list_file(pjsua.pj_str_copy(caListFile)); }
			 * String certFile =
			 * prefsWrapper.getPreferenceStringValue(PreferencesWrapper
			 * .CERT_FILE); if (!TextUtils.isEmpty(certFile)) {
			 * tlsSetting.setCert_file(pjsua.pj_str_copy(certFile)); } String
			 * privKey =
			 * prefsWrapper.getPreferenceStringValue(PreferencesWrapper
			 * .PRIVKEY_FILE); if (!TextUtils.isEmpty(privKey)) {
			 * tlsSetting.setPrivkey_file(pjsua.pj_str_copy(privKey)); } String
			 * tlsPwd =
			 * prefsWrapper.getPreferenceStringValue(PreferencesWrapper.
			 * TLS_PASSWORD); if (!TextUtils.isEmpty(tlsPwd)) {
			 * tlsSetting.setPassword(pjsua.pj_str_copy(tlsPwd)); } boolean
			 * checkClient =
			 * prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper
			 * .TLS_VERIFY_CLIENT); tlsSetting.setVerify_client(checkClient ? 1
			 * : 0);
			 */

			tlsSetting.setMethod(prefsWrapper.getTLSMethod());
			boolean checkServer = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.TLS_VERIFY_SERVER);
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
			String errorMsg = pjsua.get_error_message(status).getPtr();
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

	public boolean addAccount(SipProfile profile) {
		int status = pjsuaConstants.PJ_FALSE;
		synchronized (pjAccountsCreationLock) {
			if (!created) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return status == pjsuaConstants.PJ_SUCCESS;

			}
			PjSipAccount account = new PjSipAccount(profile);

			account.applyExtraParams(service);

			Integer currentAccountId = null;
			synchronized (activeAccountsLock) {
				currentAccountId = activeAccounts.get(account.id);
			}

			// Force the use of a transport
			switch (account.transport) {
			case SipProfile.TRANSPORT_UDP:
				if (udpTranportId != null) {
					account.cfg.setTransport_id(udpTranportId);
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

			if (currentAccountId != null) {
				status = pjsua.acc_modify(currentAccountId, account.cfg);
				synchronized (activeAccountsLock) {
					accountsAddingStatus.put(account.id, status);
				}
				if (status == pjsuaConstants.PJ_SUCCESS) {
					status = pjsua.acc_set_registration(currentAccountId, 1);
					if (status == pjsuaConstants.PJ_SUCCESS) {
						pjsua.acc_set_online_status(currentAccountId, 1);
					}
				}
			} else {
				int[] accId = new int[1];
				if (account.wizard.equalsIgnoreCase("LOCAL")) {
					account.cfg.setReg_uri(pjsua.pj_str_copy(""));
					account.cfg.setProxy_cnt(0);
					status = pjsua.acc_add_local(udpTranportId, pjsuaConstants.PJ_FALSE, accId);
					
				} else {
					status = pjsua.acc_add(account.cfg, pjsuaConstants.PJ_FALSE, accId);

				}
				synchronized (activeAccountsLock) {
					accountsAddingStatus.put(account.id, status);
					if (status == pjsuaConstants.PJ_SUCCESS) {
						activeAccounts.put(account.id, accId[0]);
						pjsua.acc_set_online_status(accId[0], 1);
					}
				}
				
				//If no registrar update state right now
				if (TextUtils.isEmpty(account.cfg.getReg_uri().getPtr())) {
					service.updateRegistrationsState();
					//Broadcast the information
					Intent regStateChangedIntent = new Intent(SipManager.ACTION_SIP_REGISTRATION_CHANGED);
					service.sendBroadcast(regStateChangedIntent);
				}
			}

		}

		return status == pjsuaConstants.PJ_SUCCESS;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<SipProfileState> getAndUpdateActiveAccounts() {
		SipProfileState info;
		Set<Integer> activeAccountsClone;
		synchronized (activeAccountsLock) {
			activeAccountsClone = ((HashMap<Integer, Integer>) activeAccounts.clone()).keySet();
		}

		ArrayList<SipProfileState> activeAccountsInfos = new ArrayList<SipProfileState>();
		for (int accountDbId : activeAccountsClone) {
			info = service.getSipProfileState(accountDbId);
			if (info != null && info.isValidForCall()) {
				activeAccountsInfos.add(info);
			}
		}
		Collections.sort(activeAccountsInfos, SipProfileState.getComparator());
		return activeAccountsInfos;
	}

	public SipProfileState getAccountInfo(SipProfile account) {
		if (!created || account == null) {
			return null;
		}
		SipProfileState accountInfo;

		Integer activeAccountStatus = null;
		Integer activeAccountPjsuaId = null;
		synchronized (activeAccountsLock) {
			activeAccountStatus = accountsAddingStatus.get(account.id);
			if (activeAccountStatus != null) {
				activeAccountPjsuaId = activeAccounts.get(account.id);
			}
		}

		accountInfo = new SipProfileState(account);
		if (activeAccountStatus != null) {
			accountInfo.setAddedStatus(activeAccountStatus);
			if (activeAccountPjsuaId != null) {
				accountInfo.setPjsuaId(activeAccountPjsuaId);
				pjsua_acc_info pjAccountInfo = new pjsua_acc_info();
				// Log.d(THIS_FILE,
				// "Get account info for account id "+accountDbId+" ==> (active within pjsip as) "+activeAccounts.get(accountDbId));
				int success = pjsua.acc_get_info(activeAccountPjsuaId, pjAccountInfo);
				if (success == pjsuaConstants.PJ_SUCCESS) {

					try {
						// Should be fine : status code are coherent with RFC
						// status codes
						accountInfo.setStatusCode(pjAccountInfo.getStatus().swigValue());
					} catch (IllegalArgumentException e) {
						accountInfo.setStatusCode(SipCallSession.StatusCode.INTERNAL_SERVER_ERROR);
					}
					accountInfo.setStatusText(pjAccountInfo.getStatus_text().getPtr());
					accountInfo.setExpires(pjAccountInfo.getExpires());

				}
			}
		}

		return accountInfo;
	}

	private ArrayList<String> codecs;

	private void initCodecs() {
		if (codecs == null) {
			int nbrCodecs = pjsua.codecs_get_nbr();
			Log.d(THIS_FILE, "Codec nbr : " + nbrCodecs);
			codecs = new ArrayList<String>();
			for (int i = 0; i < nbrCodecs; i++) {
				String codecId = pjsua.codecs_get_id(i).getPtr();
				codecs.add(codecId);
				Log.d(THIS_FILE, "Added codec " + codecId);
			}
			// Set it in prefs if not already set correctly
			prefsWrapper.setCodecList(codecs);
		}

	}

	private void setCodecsPriorities() {
		if (codecs != null) {
			for (String codec : codecs) {
				if (prefsWrapper.hasCodecPriority(codec)) {
					Log.d(THIS_FILE, "Set codec " + codec + " : " + prefsWrapper.getCodecPriority(codec, "130"));
					pjsua.codec_set_priority(pjsua.pj_str_copy(codec), prefsWrapper.getCodecPriority(codec, "130"));

					/*
					 * pjmedia_codec_param param = new pjmedia_codec_param();
					 * pjsua.codec_get_param(pjsua.pj_str_copy(codec), param);
					 * param.getSetting().setPenh(0);
					 * pjsua.codec_set_param(pjsua.pj_str_copy(codec), param );
					 */
				}
			}
		}
	}

	// Call related

	/**
	 * Answer a call
	 * 
	 * @param callId
	 *            the id of the call to answer to
	 * @param code
	 *            the status code to send in the response
	 * @return
	 */
	public int callAnswer(int callId, int code) {

		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_answer(callId, code, null, null);
			}
		}
		return -1;
	}

	/**
	 * Hangup a call
	 * 
	 * @param callId
	 *            the id of the call to hangup
	 * @param code
	 *            the status code to send in the response
	 * @return
	 */
	public int callHangup(int callId, int code) {
		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_hangup(callId, code, null, null);
			}
		}
		return -1;
	}

	public int callXfer(int callId, String callee) {
		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_xfer(callId, pjsua.pj_str_copy(callee), null);
			}
		}
		return -1;
	}

	public int callXferReplace(int callId, int otherCallId, int options) {
		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_xfer_replaces(callId, otherCallId, options, null);
			}
		}
		return -1;
	}

	/**
	 * Make a call
	 * 
	 * @param callee
	 *            remote contact ot call If not well formated we try to add
	 *            domain name of the default account
	 */
	public int makeCall(String callee, int accountId) {
		if (!created) {
			return -1;
		}

		final ToCall toCall = sanitizeSipUri(callee, accountId);
		if (toCall != null) {
			Thread t = new Thread() {
				public void run() {

					pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());

					// Nothing to do with this values
					byte[] userData = new byte[1];
					int[] callId = new int[1];
					pjsua.call_make_call(toCall.getPjsipAccountId(), uri, 0, userData, null, callId);
				};
			};
			t.start();
			// Mmmm ...
			return 0;
		} else {
			service.notifyUserOfMessage(service.getString(R.string.invalid_sip_uri) + " : " + callee);
		}
		return -1;
	}

	/**
	 * Send a dtmf signal to a call
	 * 
	 * @param callId
	 *            the call to send the signal
	 * @param keyCode
	 *            the keyCode to send (android style)
	 * @return
	 */
	public int sendDtmf(int callId, int keyCode) {
		if (!created) {
			return -1;
		}
		int res = -1;

		KeyCharacterMap km = KeyCharacterMap.load(KeyCharacterMap.NUMERIC);

		String keyPressed = String.valueOf(km.getNumber(keyCode));
		pj_str_t pjKeyPressed = pjsua.pj_str_copy(keyPressed);
		synchronized (callActionLock) {
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
					if (dialtoneGenerator == null) {
						dialtoneGenerator = new PjStreamDialtoneGenerator();
					}
					res = dialtoneGenerator.sendPjMediaDialTone(callId, keyPressed);
					Log.d(THIS_FILE, "Has been sent DTMF analogic : " + res);
				}
			}
		}
		return res;
	}

	/**
	 * Send sms/message using SIP server
	 */
	public ToCall sendMessage(String callee, String message, int accountId) {
		if (!created) {
			return null;
		}

		ToCall toCall = sanitizeSipUri(callee, accountId);
		if (toCall != null) {

			pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());
			pj_str_t text = pjsua.pj_str_copy(message);
			Log.d(THIS_FILE, "get for outgoing");
			if (accountId == -1) {
				accountId = pjsua.acc_find_for_outgoing(uri);
			}

			// Nothing to do with this values
			byte[] userData = new byte[1];

			int status = pjsua.im_send(toCall.getPjsipAccountId(), uri, null, text, (org.pjsip.pjsua.SWIGTYPE_p_pjsua_msg_data) null, userData);
			return (status == pjsuaConstants.PJ_SUCCESS) ? toCall : null;
		}
		return toCall;
	}

	public void stopDialtoneGenerator() {
		if (dialtoneGenerator != null) {
			dialtoneGenerator.stopDialtoneGenerator();
			dialtoneGenerator = null;
		}
	}

	public int callHold(int callId) {
		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_set_hold(callId, null);
			}
		}
		return -1;
	}

	public int callReinvite(int callId, boolean unhold) {
		if (created) {
			synchronized (callActionLock) {
				return pjsua.call_reinvite(callId, unhold ? pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue() : 0, null);
			}
		}
		return -1;
	}

	public SipCallSession getCallInfo(int callId) {

		synchronized (creatingSipStack) {
			if (created/* && !creating */&& userAgentReceiver != null) {
				SipCallSession callInfo = userAgentReceiver.getCallInfo(callId, false);
				return callInfo;
			}
		}
		return null;
	}

	public void setBluetoothOn(boolean on) {
		if (created && mediaManager != null) {
			mediaManager.setBluetoothOn(on);
		}
	}

	public void setMicrophoneMute(boolean on) {
		if (created && mediaManager != null) {
			mediaManager.setMicrophoneMute(on);
		}
	}

	public void setSpeakerphoneOn(boolean on) {
		if (created && mediaManager != null) {
			mediaManager.setSpeakerphoneOn(on);
		}
	}

	public SipCallSession[] getCalls() {
		synchronized (creatingSipStack) {
			if (created && userAgentReceiver != null) {
				SipCallSession[] callsInfo = userAgentReceiver.getCalls();
				return callsInfo;
			}
		}
		return null;
	}

	public void confAdjustTxLevel(int port, float value) {
		if (created && userAgentReceiver != null) {
			pjsua.conf_adjust_tx_level(port, value);
		}
	}

	public void confAdjustRxLevel(int port, float value) {
		if (created && userAgentReceiver != null) {
			pjsua.conf_adjust_rx_level(port, value);
		}
	}

	public void setEchoCancellation(boolean on) {
		if (created && userAgentReceiver != null) {
			Log.d(THIS_FILE, "set echo cancelation " + on);
			pjsua.set_ec(on ? prefsWrapper.getEchoCancellationTail() : 0, prefsWrapper.getEchoMode());
		}
	}

	public void adjustStreamVolume(int stream, int direction, int flags) {
		if (mediaManager != null) {
			mediaManager.adjustStreamVolume(stream, direction, AudioManager.FLAG_SHOW_UI);
		}
	}
	

	public void silenceRinger() {
		if(mediaManager != null) {
			mediaManager.stopRing();
		}
	}

	public void startRecording(int callId) {
		if (created && userAgentReceiver != null) {
			userAgentReceiver.startRecording(callId);
		}
	}

	public void stopRecording() {
		if (created && userAgentReceiver != null) {
			userAgentReceiver.stopRecording();
		}
	}

	public int getRecordedCall() {
		if (created && userAgentReceiver != null) {
			return userAgentReceiver.getRecordedCall();
		}
		return -1;
	}

	public boolean canRecord(int callId) {
		if (created && userAgentReceiver != null) {
			return userAgentReceiver.canRecord(callId);
		}
		return false;
	}

	public boolean setAccountRegistration(SipProfile account, int renew) {
		int status = -1;
		synchronized (pjAccountsCreationLock) {
			if (!created || account == null) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return false;
			}
			if (activeAccounts.containsKey(account.id)) {
				// The account is already there in accounts list
				int cAccId = activeAccounts.get(account.id);
				synchronized (activeAccountsLock) {
					Log.d(THIS_FILE, "Removing this account from the list");
					activeAccounts.remove(account.id);
					accountsAddingStatus.remove(account.id);
				}

				if (renew == 1) {
					pjsua.acc_set_online_status(cAccId, 1);
					status = pjsua.acc_set_registration(cAccId, renew);
				} else {
					// if(status == pjsuaConstants.PJ_SUCCESS && renew == 0) {
					Log.d(THIS_FILE, "Delete account !!");
					status = pjsua.acc_del(cAccId);
				}
			} else {

				if (renew == 1) {
					addAccount(account);
				} else {
					Log.w(THIS_FILE, "Ask to delete an unexisting account !!" + account.id);
				}

			}
		}
		// PJ_SUCCESS = 0
		return status == 0;
	}

	@SuppressWarnings("unchecked")
	public SipProfile getAccountForPjsipId(int accId) {
		Set<Entry<Integer, Integer>> activeAccountsClone;
		synchronized (activeAccountsLock) {
			activeAccountsClone = ((HashMap<Integer, Integer>) activeAccounts.clone()).entrySet();
			// Quick quit
			if (!activeAccounts.containsValue(accId)) {
				return null;
			}
		}

		for (Entry<Integer, Integer> entry : activeAccountsClone) {
			if (entry.getValue().equals(accId)) {
				return service.getAccount(entry.getKey());
			}
		}

		return null;
	}

	public int setAudioInCall(int clockRate) {
		if (mediaManager != null) {
			return mediaManager.setAudioInCall(clockRate);
		} else {
			Log.e(THIS_FILE, "WARNING !!! WE HAVE NO MEDIA MANAGER AT THIS POINT");
		}
		return -1;
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

	// TO call utils

	private ToCall sanitizeSipUri(String callee, int accountId) {
		// accountId is the id in term of csipsimple database
		// pjsipAccountId is the account id in term of pjsip adding
		int pjsipAccountId = SipProfile.INVALID_ID;

		// If this is an invalid account id
		if (accountId == SipProfile.INVALID_ID || !activeAccounts.containsKey(accountId)) {
			int defaultPjsipAccount = pjsua.acc_get_default();

			// If default account is not active
			if (!activeAccounts.containsValue(defaultPjsipAccount)) {
				for (Integer accId : activeAccounts.keySet()) {
					// Use the first account as valid account
					if (accId != null) {
						accountId = accId;
						pjsipAccountId = activeAccounts.get(accId);
						break;
					}
				}
			} else {
				// Use the default account
				for (Integer accId : activeAccounts.keySet()) {
					if (activeAccounts.get(accId) == defaultPjsipAccount) {
						accountId = accId;
						pjsipAccountId = defaultPjsipAccount;
						break;
					}
				}
			}
		} else {
			// If the account is valid
			pjsipAccountId = activeAccounts.get(accountId);
		}

		if (pjsipAccountId == SipProfile.INVALID_ID) {
			Log.e(THIS_FILE, "Unable to find a valid account for this call");
			return null;
		}

		// Check integrity of callee field
		Pattern p = Pattern.compile("^.*(?:<)?(sip(?:s)?):([^@]*@[^>]*)(?:>)?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(callee);

		if (!m.matches()) {
			// Assume this is a direct call using digit dialer

			Log.d(THIS_FILE, "default acc : " + accountId);
			SipProfile account = service.getAccount(accountId);
			String defaultDomain = account.getDefaultDomain();

			Log.d(THIS_FILE, "default domain : " + defaultDomain);
			p = Pattern.compile("^sip(s)?:[^@]*$", Pattern.CASE_INSENSITIVE);
			if (p.matcher(callee).matches()) {
				callee = "<" + callee + "@" + defaultDomain + ">";
			} else {
				// Should it be encoded?
				callee = "<sip:" + /* Uri.encode( */callee/* ) */+ "@" + defaultDomain + ">";
			}
		} else {
			callee = "<" + m.group(1) + ":" + m.group(2) + ">";
		}

		Log.d(THIS_FILE, "will call " + callee);
		if (pjsua.verify_sip_url(callee) == 0) {
			// In worse worse case, find back the account id for uri.. but
			// probably useless case
			if (pjsipAccountId == SipProfile.INVALID_ID) {
				pjsipAccountId = pjsua.acc_find_for_outgoing(pjsua.pj_str_copy(callee));
			}
			return new ToCall(pjsipAccountId, callee);
		}

		return null;
	}

	public void onGSMStateChanged(int state, String incomingNumber) {
		// Avoid ringing if new GSM state is not idle
		if (state != TelephonyManager.CALL_STATE_IDLE && mediaManager != null) {
			mediaManager.stopRing();
		}

		// If new call state is not idle
		if (state != TelephonyManager.CALL_STATE_IDLE && userAgentReceiver != null) {
			SipCallSession currentActiveCall = userAgentReceiver.getActiveCallInProgress();

			if (currentActiveCall != null) {
				AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
				if (state != TelephonyManager.CALL_STATE_RINGING) {
					// New state is not ringing nor idle... so off hook, hold
					// current sip call
					hasBeenHoldByGSM = currentActiveCall.getCallId();
					callHold(hasBeenHoldByGSM);
					pjsua.set_no_snd_dev();

					am.setMode(AudioManager.MODE_IN_CALL);
				} else {
					// We have a ringing incoming call.
					// Avoid vibration
					am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
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
		}
	}

	public void sendKeepAlivePackets() {
		ArrayList<SipProfileState> accounts = getAndUpdateActiveAccounts();
		for (SipProfileState acc : accounts) {
			pjsua.send_keep_alive(acc.getPjsuaId());
		}
	}

	public void zrtpSASVerified() {
		pjsua.jzrtp_SASVerified();
	}


}
