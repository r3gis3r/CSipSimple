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
package com.csipsimple.service;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_pool_t;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_port;
import org.pjsip.pjsua.pjmedia_tone_desc;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsip_transport_type_e;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_call_info;
import org.pjsip.pjsua.pjsua_config;
import org.pjsip.pjsua.pjsua_logging_config;
import org.pjsip.pjsua.pjsua_media_config;
import org.pjsip.pjsua.pjsua_transport_config;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyCharacterMap;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.CallInfo;
import com.csipsimple.models.IAccount;
import com.csipsimple.models.CallInfo.UnavailableException;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.RegistrationNotification;

public class SipService extends Service {

	static boolean created = false;
//	static boolean creating = false;
	static String THIS_FILE = "SIP SRV";

	
	// -------
	// Static constants
	// -------
	
	final public static String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
	final public static String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	final public static String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
	final public static String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
	final public static String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
	
	final public static int REGISTER_NOTIF_ID = 1;
	final public static int CALL_NOTIF_ID =  REGISTER_NOTIF_ID+1;
	
	private NotificationManager notificationManager;

	public final static String STACK_FILE_NAME = "libpjsipjni.so";

	private static Object pjAccountsCreationLock = new Object();
	private static Object activeAccountsLock = new Object();
	private static Object callActionLock = new Object();
	
	private static Object creatingSipStack = new Object();
	
	// Map active account id (id for sql settings database) with acc_id (id for
	// pjsip)
	private HashMap<Integer, Integer> activeAccounts = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> accountsAddingStatus = new HashMap<Integer, Integer>();

	// Implement public interface for the service
	private final ISipService.Stub binder = new ISipService.Stub() {
		/**
		 * Start the sip stack according to current settings (create pjsua)
		 */
		@Override
		public void sipStart() throws RemoteException {	SipService.this.sipStart();	}
		
		/**
		 * Stop the sip stack (destroy pjsua)
		 */
		@Override
		public void sipStop() throws RemoteException { SipService.this.sipStop(); }

		/**
		 * Force the stop of the service
		 */
		@Override
		public void forceStopService() throws RemoteException {
			Log.d(THIS_FILE,"Try to force service stop");
			stopSelf();
		}

		/**
		 * Restart the service (threaded)
		 */
		@Override
		public void askThreadedRestart() throws RemoteException {
			Thread t = new Thread() {
				public void run() {
					SipService.this.sipStop();
					SipService.this.sipStart();
				}
			};
			t.start();
		};
		
		
		/**
		 * Populate pjsip accounts with accounts saved in sqlite
		 */
		@Override
		public void addAllAccounts() throws RemoteException { SipService.this.addAllAccounts(); }
		

		/**
		 * Unregister and delete accounts registered
		 */
		@Override
		public void removeAllAccounts() throws RemoteException { SipService.this.unregisterAllAccounts(true); }
		
		
		/**
		 * Reload all accounts with values found in database
		 */
		@Override
		public void reAddAllAccounts() throws RemoteException { 
			SipService.this.reAddAllAccounts(); 
		}
		

		@Override
		public void setAccountRegistration(int accountId, int renew) throws RemoteException {
			Account account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountId);
				db.close();
			}
			SipService.this.setAccountRegistration(account, renew);
		}

		
		/**
		 * Get account and it's informations
		 * @param accountId the id (sqlite id) of the account
		 */
		@Override
		public AccountInfo getAccountInfo(int accountId) throws RemoteException { return SipService.this.getAccountInfo(accountId);}


		/**
		 * Switch in autoanswer mode
		 */
		@Override
		public void switchToAutoAnswer() throws RemoteException {
			if (userAgentReceiver != null) {
				userAgentReceiver.setAutoAnswerNext(true);
			}
		}
		

		/**
		 * Make a call
		 * @param callee remote contact ot call
		 * If not well formated we try to add domain name of the default account
		 */
		@Override
		public void makeCall(String callee, int accountId) throws RemoteException { 
			SipService.this.makeCall(callee, accountId);
		}

		
		
		/**
		 * Answer an incoming call
		 * @param callId the id of the call to answer to
		 * @param status the status code to send
		 */
		@Override
		public int answer(int callId, int status) throws RemoteException { 
			synchronized (callActionLock) {
				return SipService.this.callAnswer(callId, status);
			}
		}
		
		/**
		 * Hangup a call
		 * @param callId the id of the call to hang up
		 * @param status the status code to send
		 */
		@Override
		public int hangup(int callId, int status) throws RemoteException { 
			synchronized (callActionLock) {
				return SipService.this.callHangup(callId, status); 
			}
		}

		
		@Override
		public int sendDtmf(int callId, int keyCode) throws RemoteException { 
			synchronized (callActionLock) {
				return SipService.this.sendDtmf(callId, keyCode); 
			}
		}

		@Override
		public CallInfo getCallInfo(int callId) throws RemoteException {
			synchronized (creatingSipStack) {
				if(created/* && !creating*/) {
					CallInfo callInfo = userAgentReceiver.getCallInfo(callId);
					if(callInfo == null) {
						return null;
					}
					if(callId != callInfo.getCallId()) {
						Log.w(THIS_FILE, "we try to get an info for a call that is not the current one :  "+callId);
						try {
							callInfo = new CallInfo(callId);
						} catch (UnavailableException e) {
							throw new RemoteException();
						}
					}
					return callInfo;
				}
			}
			return null;
		}

		@Override
		public void setBluetoothOn(boolean on) throws RemoteException {
			if(created && mediaManager != null) { 
				mediaManager.setBluetoothOn(on);
			}
		}

		@Override
		public void setMicrophoneMute(boolean on) throws RemoteException {
			if(created && mediaManager != null) { 
				mediaManager.setMicrophoneMute(on);
			}
		}

		@Override
		public void setSpeakerphoneOn(boolean on) throws RemoteException {
			if(created && mediaManager != null) { 
				mediaManager.setSpeakerphoneOn(on);
			}
		}

		@Override
		public int hold(int callId) throws RemoteException {
			Log.d(THIS_FILE, "HOLDING");
			synchronized (callActionLock) {
				return SipService.this.callHold(callId);
			}
		}

		@Override
		public int reinvite(int callId, boolean unhold) throws RemoteException {
			Log.d(THIS_FILE, "REINVITING");
			synchronized (callActionLock) {
				return SipService.this.callReinvite(callId, unhold);
			}
		}

		
	};
	
	
	private final ISipConfiguration.Stub binderConfiguration = new ISipConfiguration.Stub() {

		@Override
		public long addOrUpdateAccount(IAccount acc) throws RemoteException {
			Log.d(THIS_FILE, ">>> addOrUpdateAccount from service");
			Account account = new Account(acc);
			long final_id = -1;
			synchronized(db){
				db.open();
				if(account.id == null) {
					final_id = db.insertAccount(account);
				}else {
					db.updateAccount(account);
					final_id = account.id;
				}
				db.close();
			}
			return final_id;
		}

		@Override
		public IAccount getAccount(long accId) throws RemoteException {
			IAccount result = null;
			Account account;
			synchronized(db){
				db.open();
				account = db.getAccount(accId);
				db.close();
			}
			if(account !=null) {
				result = account.getIAccount();
			}
			return result;
		}

		@Override
		public void setPreferenceBoolean(String key, boolean value) throws RemoteException {
			prefsWrapper.setPreferenceBooleanValue(key, value);
		}

		@Override
		public void setPreferenceFloat(String key, float value) throws RemoteException {
			prefsWrapper.setPreferenceFloatValue(key, value);
			
		}

		@Override
		public void setPreferenceString(String key, String value) throws RemoteException {
			prefsWrapper.setPreferenceStringValue(key, value);
			
		}
		
	};

	protected DBAdapter db;
	private WakeLock wakeLock;
	private WifiLock wifiLock;
	private static UAStateReceiver userAgentReceiver;
	public static MediaManager mediaManager;
	public static boolean hasSipStack = false;
	private boolean sipStackIsCorrupted = false;
	private ServiceDeviceStateReceiver deviceStateReceiver;
	public PreferencesWrapper prefsWrapper;
	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	private Object dialtoneMutext = new Object();
	private RegistrationNotification contentView;
	private ServicePhoneStateReceiver phoneConnectivityReceiver;
	private TelephonyManager telephonyManager;
	private ConnectivityManager connectivityManager;
	private Integer udpTranportId, tcpTranportId, tlsTransportId;
	
	private Integer hasBeenHoldByGSM = null;

	// Broadcast receiver for the service
	private class ServiceDeviceStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//
			// ACTION_CONNECTIVITY_CHANGED
			// Connectivity change is used to detect changes in the overall
			// data network status as well as a switch between wifi and mobile
			// networks.
			//
			// ACTION_DATA_STATE_CHANGED
			// Data state change is used to detect changes in the mobile
			// network such as a switch of network type (GPRS, EDGE, 3G) 
			// which are not detected by the Connectivity changed broadcast.
			//
			Log.d(THIS_FILE, "ServiceDeviceStateReceiver");
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) /*|| 
					intent.getAction().equals(ACTION_DATA_STATE_CHANGED)*/) {
				Log.d(THIS_FILE, "Connectivity or data state has changed");
				//Thread it to be sure to not block the device if registration take time
				Thread t = new Thread() {
					@Override
					public void run() {
						
						dataConnectionChanged();
						
					}
				};
				t.start();
			}
		}
	}
	
	
	private class ServicePhoneStateReceiver extends PhoneStateListener {
		@Override
		public void onDataConnectionStateChanged(int state) {
			Log.d(THIS_FILE, "Data connection state changed : "+state);
			Thread t = new Thread() {
				@Override
				public void run() {
					dataConnectionChanged();
				}
			};
			t.start();
			super.onDataConnectionStateChanged(state);
		}
		
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			Log.d(THIS_FILE, "Call state has changed !"+state+" : "+incomingNumber);
			
			//Avoid ringing while not idle
			if(state != TelephonyManager.CALL_STATE_IDLE && mediaManager != null) {
				mediaManager.stopRing();
			}
			
			if(state != TelephonyManager.CALL_STATE_IDLE) {
				CallInfo currentActiveCall = userAgentReceiver.getActiveCallInProgress();
				if(currentActiveCall != null && state != TelephonyManager.CALL_STATE_RINGING) {
					hasBeenHoldByGSM = currentActiveCall.getCallId();
					SipService.this.callHold(hasBeenHoldByGSM);
					pjsua.set_no_snd_dev();
					
					AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
					am.setMode(AudioManager.MODE_IN_CALL);
				}
			}else {
				if(hasBeenHoldByGSM != null) {
					pjsua.set_snd_dev(0, 0);
					SipService.this.callReinvite(hasBeenHoldByGSM, true);
					hasBeenHoldByGSM = null;
				}
			}
			
			super.onCallStateChanged(state, incomingNumber);
		}
	}
	
	
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(THIS_FILE, "Create SIP Service");
		db = new DBAdapter(this);
		prefsWrapper = new PreferencesWrapper(this);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// Check connectivity, else just finish itself
		if ( !prefsWrapper.isValidConnectionForOutgoing() && !prefsWrapper.isValidConnectionForIncoming()) {
			Log.d(THIS_FILE, "Harakiri... we are not needed since no way to use self");
			stopSelf();
			return;
		}
		
		Log.setLogLevel(prefsWrapper.getLogLevel());
		
		//Do not thread since must ensure stack is loaded
		loadAndConnectStack();
	}

	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(THIS_FILE, "Destroying SIP Service");
		try {
			Log.d(THIS_FILE, "Unregister telephony receiver");
			unregisterReceiver(deviceStateReceiver);
			deviceStateReceiver = null;
		}catch(IllegalArgumentException e) {
			//This is the case if already unregistered itself
			//Python style usage of try ;) : nothing to do here since it could be a standard case
			//And in this case nothing has to be done
		}
		if(phoneConnectivityReceiver != null) {
			Log.d(THIS_FILE, "Unregister telephony receiver");
			telephonyManager.listen(phoneConnectivityReceiver, 
					PhoneStateListener.LISTEN_NONE);
			phoneConnectivityReceiver = null;
		}
		sipStop();
		notificationManager.cancelAll();
		Log.i(THIS_FILE, "--- SIP SERVICE DESTROYED ---");
		
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Autostart the stack
		loadAndConnectStack();
		if(hasSipStack) {
			Thread t = new Thread() {
				public void run() {
					Log.d(THIS_FILE, "Start sip stack because start asked");
					try {
						sipStart();
					}catch(IllegalMonitorStateException e) {
						Log.e(THIS_FILE, "Not able to start sip right now", e);
					}
				}
			};
			t.start();
		}
	}
	
	private void loadAndConnectStack() {
		if(!hasSipStack) {
			Log.d(THIS_FILE, "TRY TO LOAD SIP STACK");
			tryToLoadStack();
		}
		
		if(hasSipStack) {
			// Register own broadcast receiver
			if(deviceStateReceiver == null) {
				IntentFilter intentfilter = new IntentFilter();
				intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				registerReceiver(deviceStateReceiver = new ServiceDeviceStateReceiver(), 
						intentfilter);
			}
			if(phoneConnectivityReceiver == null) {
				Log.d(THIS_FILE, "Listen for phone state ");
				telephonyManager.listen(phoneConnectivityReceiver = new ServicePhoneStateReceiver(), 
						PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_CALL_STATE);
			}
			
		}
	}

	private void tryToLoadStack() {
		File stack_file = getStackLibFile(this);
		if(stack_file != null && !sipStackIsCorrupted) {
			try {
				//Try to load the stack
				System.load(stack_file.getAbsolutePath());
				hasSipStack = true;
			} catch (UnsatisfiedLinkError e) {
				//If it fails we probably are running on a special hardware, redirect to support webpage
				Log.e(THIS_FILE, "We have a problem with the current stack.... NOT YET Implemented", e);
				hasSipStack = false;
				sipStackIsCorrupted = true;

				/*
				//Obsolete
				Intent it = new Intent(Intent.ACTION_VIEW);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				it.setData(Uri.parse("http://code.google.com/p/csipsimple/wiki/NewHardwareSupportRequest"));
				
				startActivity(it);
				*/
				stopSelf();
			} catch (Exception e) {
				Log.e(THIS_FILE, "We have a problem with the current stack....", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		
		String serviceName = intent.getAction();
		Log.d(THIS_FILE, "Action is "+serviceName);
		if(serviceName == null || serviceName.equalsIgnoreCase("com.csipsimple.service.SipService")) {
			Log.d(THIS_FILE, "Service returned");
			return binder;
		}else if(serviceName.equalsIgnoreCase("com.csipsimple.service.SipConfiguration")) {
			Log.d(THIS_FILE, "Conf returned");
			return binderConfiguration;
		}
		Log.d(THIS_FILE, "Default service (SipService) returned");
		return binder;
	}
	
	

	// Start the sip stack according to current settings
	synchronized void sipStart() {
		Log.setLogLevel(prefsWrapper.getLogLevel());

		if (!hasSipStack) {
			Log.e(THIS_FILE, "We have no sip stack, we can't start");
			return;
		}
		
		if(!prefsWrapper.isValidConnectionForIncoming() && !prefsWrapper.isValidConnectionForOutgoing()) {
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "Not able to start sip stack");
			return;
		}

		Log.i(THIS_FILE, "Will start sip : " + (!created /*&& !creating*/));
		synchronized (creatingSipStack) {
			//Ensure the stack is not already created or is being created
			if (!created/* && !creating*/) {
//				creating = true;
				udpTranportId = null;
				tcpTranportId = null;
				
				int status;
				status = pjsua.create();
				Log.i(THIS_FILE, "Created " + status);
				//General config
				{
					pjsua_config cfg = new pjsua_config();
					pjsua_logging_config log_cfg = new pjsua_logging_config();
					pjsua_media_config media_cfg = new pjsua_media_config();
	
					// GLOBAL CONFIG
					pjsua.config_default(cfg);
					cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);
					if(userAgentReceiver == null) {
						userAgentReceiver = new UAStateReceiver();
						userAgentReceiver.initService(SipService.this);
					}
					if(mediaManager == null) {
						mediaManager = new MediaManager(SipService.this);
					}
					pjsua.setCallbackObject(userAgentReceiver);
					
	
					Log.d(THIS_FILE, "Attach is done to callback");
					
					// MAIN CONFIG
					
					cfg.setUser_agent(pjsua.pj_str_copy(prefsWrapper.getUserAgent()));
					//cfg.setThread_cnt(4); // one thread seems to be enough for now
					cfg.setUse_srtp(prefsWrapper.getUseSrtp());
					cfg.setSrtp_secure_signaling(0);
					
					//DNS
					if(prefsWrapper.enableDNSSRV() && !prefsWrapper.useIPv6()) {
						pj_str_t[] nameservers = prefsWrapper.getNameservers();
						if(nameservers != null) {
							cfg.setNameserver_count(nameservers.length);
							cfg.setNameserver(nameservers);
						}else {
							cfg.setNameserver_count(0);
						}
					}
					//STUN
					int isStunEnabled = prefsWrapper.getStunEnabled();
					if(isStunEnabled == 1) {
						cfg.setStun_srv_cnt(1);
						pj_str_t[] stun_servers = cfg.getStun_srv();
						stun_servers[0] = pjsua.pj_str_copy(prefsWrapper.getStunServer());
						cfg.setStun_srv(stun_servers);
					}
					
					//IGNORE NOTIFY -- TODO : for now that's something we want since it polute battery life
					cfg.setEnable_unsolicited_mwi(pjsuaConstants.PJ_FALSE);
	
					// LOGGING CONFIG
					pjsua.logging_config_default(log_cfg);
					log_cfg.setConsole_level(prefsWrapper.getLogLevel());
					log_cfg.setLevel(prefsWrapper.getLogLevel());
					log_cfg.setMsg_logging(pjsuaConstants.PJ_TRUE);

					// MEDIA CONFIG
					pjsua.media_config_default(media_cfg);
	
					// For now only this cfg is supported
					media_cfg.setChannel_count(1);
					media_cfg.setSnd_auto_close_time(prefsWrapper.getAutoCloseTime());
					// Echo cancellation
					media_cfg.setEc_tail_len(prefsWrapper.getEchoCancellationTail());
					media_cfg.setNo_vad(prefsWrapper.getNoVad());
					media_cfg.setQuality(prefsWrapper.getMediaQuality());
					media_cfg.setClock_rate(prefsWrapper.getClockRate());
					media_cfg.setAudio_frame_ptime(prefsWrapper.getAudioFramePtime());
					
					//ICE
					media_cfg.setEnable_ice(prefsWrapper.getIceEnabled());
					//TURN
					int isTurnEnabled = prefsWrapper.getTurnEnabled();
					if(isTurnEnabled == 1) {
						media_cfg.setEnable_turn(isTurnEnabled);
						media_cfg.setTurn_server(pjsua.pj_str_copy(prefsWrapper.getTurnServer()));
					}
					
					// INITIALIZE
					status = pjsua.csipsimple_init(cfg, log_cfg, media_cfg);
					if (status != pjsuaConstants.PJ_SUCCESS) {
						Log.e(THIS_FILE, "Fail to init pjsua with failure code " + status);
						pjsua.csipsimple_destroy();
						created = false;
//						creating = false;
						return;
					}
				}
	
				// Add transports
				{
					//UDP
					if(prefsWrapper.isUDPEnabled()) {
						pjsip_transport_type_e t = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP;
						if(prefsWrapper.useIPv6()) {
							t = pjsip_transport_type_e.PJSIP_TRANSPORT_UDP6;
						}
						udpTranportId = createTransport(t, prefsWrapper.getUDPTransportPort());
						if (udpTranportId == null) {
							pjsua.csipsimple_destroy();
//							creating = false;
							created = false;
							return;
						}
					//	int[] p_acc_id = new int[1];
					//	pjsua.acc_add_local(udpTranportId, pjsua.PJ_FALSE, p_acc_id);
					//	Log.d(THIS_FILE, "Udp account "+p_acc_id);
						
					}
					//TCP
					if(prefsWrapper.isTCPEnabled() && !prefsWrapper.useIPv6()) {
						pjsip_transport_type_e t = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP;
						if(prefsWrapper.useIPv6()) {
							t = pjsip_transport_type_e.PJSIP_TRANSPORT_TCP6;
						}
						tcpTranportId = createTransport(t, prefsWrapper.getTCPTransportPort());
						if (tcpTranportId == null) {
							pjsua.csipsimple_destroy();
//							creating = false;
							created = false;
							return;
						}
					//	int[] p_acc_id = new int[1];
					//	pjsua.acc_add_local(tcpTranportId, pjsua.PJ_FALSE, p_acc_id);
						
					}
					
					//TLS
					if(prefsWrapper.isTLSEnabled() && !prefsWrapper.useIPv6() && (pjsua.can_use_tls() == pjsuaConstants.PJ_TRUE ) ){
						tlsTransportId = createTransport(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, prefsWrapper.getTLSTransportPort() );
						
						if (tlsTransportId == null) {
							pjsua.csipsimple_destroy();
//							creating = false;
							created = false;
							return;
						}
					//	int[] p_acc_id = new int[1];
					//	pjsua.acc_add_local(tlsTransportId, pjsua.PJ_FALSE, p_acc_id);
					}
					
					//RTP transport
					{
						pjsua_transport_config cfg = new pjsua_transport_config();
						pjsua.transport_config_default(cfg);
						cfg.setPort(prefsWrapper.getRTPPort());
						
						if(prefsWrapper.useIPv6()) {
							status = pjsua.media_transports_create_ipv6(cfg);
						}else {
							status = pjsua.media_transports_create(cfg);
						}
						if (status != pjsuaConstants.PJ_SUCCESS) {
							Log.e(THIS_FILE, "Fail to add media transport with failure code " + status);
							pjsua.csipsimple_destroy();
//							creating = false;
							created = false;
							return;
						}
					}
				}
	
				// Initialization is done, now start pjsua
				status = pjsua.start();
				
				if(status != pjsua.PJ_SUCCESS) {
					Log.e(THIS_FILE, "Fail to start pjsip " + status);
					pjsua.csipsimple_destroy();
//					creating = false;
					created = false;
					return;
				}
				
				// Init media codecs
				initCodecs();
				setCodecsPriorities();
				
				created = true;
	
				// Add accounts
				addAllAccounts();
//				creating = false;
			}
		}
	}

	
	/**
	 * Stop sip service
	 */
	synchronized void sipStop() {
		if (notificationManager != null) {
			notificationManager.cancel(REGISTER_NOTIF_ID);
		}
		synchronized (creatingSipStack) {
			if (created) {
				Log.d(THIS_FILE, "Detroying...");
				//This will destroy all accounts so synchronize with accounts management lock
				synchronized (pjAccountsCreationLock) {
					pjsua.csipsimple_destroy();
					synchronized (activeAccountsLock) {
						accountsAddingStatus.clear();
						activeAccounts.clear();
					}
				}
				if(userAgentReceiver != null) {
					userAgentReceiver.stopService();
					userAgentReceiver = null;
				}
				
				if(mediaManager != null) {
					mediaManager.stopService();
					mediaManager = null;
				}
			}
		}
		releaseResources();
		Log.i(THIS_FILE, ">> Media m "+mediaManager);
		created = false;
	}
	
	
	/**
	 * Utility to create a transport
	 * @return transport id or -1 if failed
	 */
	private Integer createTransport(pjsip_transport_type_e type, int port) {
		pjsua_transport_config cfg = new pjsua_transport_config();
		int[] t_id = new int[1];
		int status;
		pjsua.transport_config_default(cfg);
		cfg.setPort(port);
		
		status = pjsua.transport_create(type, cfg, t_id);
		if (status != pjsuaConstants.PJ_SUCCESS) {
			Log.e(THIS_FILE, "Fail to add transport with failure code " + status);
			return null;
		}
		return t_id[0];
	}
	
	/**
	 * Add accounts from database
	 */
	private void addAllAccounts() {
		Log.d(THIS_FILE, "We are adding all accounts right now....");
		
		boolean hasSomeSuccess = false;
		List<Account> accountList;
		synchronized (db) {
			db.open();
			accountList = db.getListAccounts();
			db.close();
		}
		
		for (Account account : accountList) {
			if (account.active) {
				if(addAccount(account) == pjsuaConstants.PJ_SUCCESS) {
					hasSomeSuccess = true;
				}
			}
		}
		
		if(hasSomeSuccess) {
			acquireResources();
		}else {
			releaseResources();
			if (notificationManager != null) {
				notificationManager.cancel(REGISTER_NOTIF_ID);
			}
		}
	}
	
	private int addAccount(Account account) {
		int status = pjsuaConstants.PJ_FALSE;
		synchronized (pjAccountsCreationLock) {
			if(!created) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return status;
			}
			account.applyExtraParams();
			
			Integer currentAccountId = null;
			synchronized (activeAccountsLock) {
				currentAccountId = activeAccounts.get(account.id);	
			}
			
			//Force the use of a transport
			if(account.use_tcp && tcpTranportId != null) {
			//	Log.d(THIS_FILE, "Attach account to transport : "+tcpTranportId);
			//	account.cfg.setTransport_id(tcpTranportId);
			}else if(account.prevent_tcp && udpTranportId != null) {
				account.cfg.setTransport_id(udpTranportId);
			}
			
			
			if (currentAccountId != null) {
				status = pjsua.acc_modify(currentAccountId, account.cfg);
				synchronized (activeAccountsLock) {
					accountsAddingStatus.put(account.id, status);
				}
				if(status == pjsuaConstants.PJ_SUCCESS){
					status = pjsua.acc_set_registration(currentAccountId, 1);
				}
			} else {
				int[] acc_id = new int[1];
				status = pjsua.acc_add(account.cfg, pjsuaConstants.PJ_FALSE, acc_id);
				synchronized (activeAccountsLock) {
					accountsAddingStatus.put(account.id, status);
					if(status == pjsuaConstants.PJ_SUCCESS) {
						activeAccounts.put(account.id, acc_id[0]);
					}
				}
			}
			
		}
		
		return status;
	}
	
	private int setAccountRegistration(Account account, int renew) {
		int status = pjsuaConstants.PJ_FALSE;
		synchronized (pjAccountsCreationLock) {
			if(!created || account == null) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return status;
			}
			if (activeAccounts.containsKey(account.id)) {
				int c_acc_id = activeAccounts.get(account.id);
				synchronized (activeAccountsLock) {
					activeAccounts.remove(account.id);
					accountsAddingStatus.remove(account.id);
				}
				
				if(renew ==1) {
					status = pjsua.acc_set_registration(c_acc_id, renew);
				}else {
				//if(status == pjsuaConstants.PJ_SUCCESS && renew == 0) {
					Log.d(THIS_FILE, "Delete account !!");
					status = pjsua.acc_del(c_acc_id);
				}
			}else {
				if(renew == 1) {
					addAccount(account);
				}else {
					Log.w(THIS_FILE, "Ask to delete an unexisting account !!"+account.id);
				}
				
			}
		}
		// Send a broadcast message that for an account
		// registration state has changed
		Intent regStateChangedIntent = new Intent(ACTION_SIP_REGISTRATION_CHANGED);
		sendBroadcast(regStateChangedIntent);
		
		updateRegistrationsState();
		return status;
	}

	/**
	 * Remove accounts from database
	 */
	private void unregisterAllAccounts(boolean cancelNotification) {
		if(!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}
		releaseResources();
		
		synchronized (pjAccountsCreationLock) {
			Log.d(THIS_FILE, "Remove all accounts");
			List<Account> accountList;
			synchronized (db) {
				db.open();
				accountList = db.getListAccounts();
				db.close();
			}
			
			for (Account account : accountList) {
				setAccountRegistration(account, 0);
			}
			
		}
		
		
		if (notificationManager != null && cancelNotification) {
			notificationManager.cancel(REGISTER_NOTIF_ID);
		}
	}
	
	private void reAddAllAccounts() {
		Log.d(THIS_FILE, "RE REGISTER ALL ACCOUNTS");
		unregisterAllAccounts(false);
		addAllAccounts();
	}
	
	

	private AccountInfo getAccountInfo(int accountDbId) {
		if(!created) {
			return null;
		}
		AccountInfo accountInfo;
		
		Integer activeAccountStatus = null;
		Integer activeAccountPjsuaId = null;
		synchronized (activeAccountsLock) {
			activeAccountStatus = accountsAddingStatus.get(accountDbId);
			if(activeAccountStatus != null) {
				activeAccountPjsuaId = activeAccounts.get(accountDbId);
			}
		}
		Account account;
		synchronized (db) {
			db.open();
			account = db.getAccount(accountDbId);
			db.close();
		}
		//If account has been removed meanwhile
		if(account == null) {
			return null;
		}
		accountInfo = new AccountInfo(account);
		if(activeAccountStatus != null) {
			accountInfo.setAddedStatus(activeAccountStatus);
			if(activeAccountPjsuaId != null) {
				accountInfo.setPjsuaId(activeAccountPjsuaId);
				pjsua_acc_info pjAccountInfo = new pjsua_acc_info();
				//Log.d(THIS_FILE, "Get account info for account id "+accountDbId+" ==> (active within pjsip as) "+activeAccounts.get(accountDbId));
				int success = pjsua.acc_get_info(activeAccountPjsuaId, pjAccountInfo);
				if(success == pjsuaConstants.PJ_SUCCESS) {
					accountInfo.fillWithPjInfo(pjAccountInfo);
				}
			}
		}
		
		return accountInfo;
	}
	
	
	@SuppressWarnings("unchecked")
	protected Account getAccountForPjsipId(int acc_id) {
		Set<Entry<Integer, Integer>> activeAccountsClone;
		synchronized (activeAccountsLock) {
			activeAccountsClone = ((HashMap<Integer, Integer>) activeAccounts.clone()).entrySet();
			//Quick quit
			if(!activeAccounts.containsValue(acc_id)) {
				return null;
			}
		}
		
		for( Entry<Integer, Integer> entry : activeAccountsClone) {
			if(entry.getValue().equals(acc_id)) {
				synchronized (db) {
					db.open();
					Account account = db.getAccount(entry.getKey());
					db.close();
					return account;
				}
			}
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public void updateRegistrationsState() {
		AccountInfo info;
		Set<Integer> activeAccountsClone;
		synchronized (activeAccountsLock) {
			activeAccountsClone = ((HashMap<Integer, Integer>) activeAccounts.clone()).keySet();
		}
		
		ArrayList<AccountInfo> activeAccountsInfos = new ArrayList<AccountInfo>();
		for (int accountDbId : activeAccountsClone) {
			info = getAccountInfo(accountDbId);
			if (info != null && info.getExpires() > 0 && info.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
				activeAccountsInfos.add(info);
			}
		}
		
		
		// Handle status bar notification
		if (activeAccountsInfos.size()>0) {
			
			int icon = R.drawable.sipok;
			CharSequence tickerText = getString(R.string.service_ticker_registered_text);
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);

			Intent notificationIntent = new Intent(ACTION_SIP_DIALER);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			if(contentView == null) {
				contentView = new RegistrationNotification(getPackageName());
			}
			contentView.clearRegistrations();
			contentView.addAccountInfos(this, activeAccountsInfos);

			//notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			notification.contentIntent = contentIntent;
			notification.contentView = contentView;
			notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			// notification.flags = Notification.FLAG_FOREGROUND_SERVICE;

			notificationManager.notify(REGISTER_NOTIF_ID, notification);
			acquireResources();
		} else {
			
			notificationManager.cancel(REGISTER_NOTIF_ID);
			releaseResources();
		}
	}
	
	/**
	 * Get the currently instanciated prefsWrapper (to be used by UAStateReceiver)
	 * @return the preferenceWrapper instanciated
	 */
	public PreferencesWrapper getPrefs() {
		//Is never null when call so ok, just not check...
		return prefsWrapper;
	}
	
	/**
	 * Ask to take the control of the wifi and the partial wake lock if configured
	 */
	private void acquireResources() {
		//Add a wake lock
		if(prefsWrapper.usePartialWakeLock()) {
			PowerManager pman = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (wakeLock == null) {
				wakeLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.SipService");
				wakeLock.setReferenceCounted(false);
			}
			//Extra check if set reference counted is false ???
			if(!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
		}
		
		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if(wifiLock == null) {
			wifiLock = wman.createWifiLock("com.csipsimple.SipService");
			wifiLock.setReferenceCounted(false);
		}
		if( prefsWrapper.getLockWifi() && !wifiLock.isHeld() ) {
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo != null) {
				DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
				//We assume that if obtaining ip addr, we are almost connected so can keep wifi lock
				if(dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
					if(!wifiLock.isHeld()) {
						wifiLock.acquire();
					}
				}
			}
		}
	}
	
	private void releaseResources() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		if(wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

	/**
	 * Answer a call
	 * @param callId the id of the call to answer to
	 * @param code the status code to send in the response
	 * @return
	 */
	public int callAnswer(int callId, int code) {
		if(created) {
			return pjsua.call_answer(callId, code, null, null);
		}
		return -1;
	}
	
	
	/**
	 * Hangup a call
	 * @param callId the id of the call to hangup
	 * @param code the status code to send in the response
	 * @return
	 */
	public int callHangup(int callId, int code) {
		if(created) {
			return pjsua.call_hangup(callId, code, null, null);
		}
		return -1;
	}
	
	/**
	 * Make a call
	 * @param callee remote contact ot call
	 * If not well formated we try to add domain name of the default account
	 */
	public int makeCall(String callee, int accountId) {
		if(!created) {
			return -1;
		}
		int pjsipAccountId = -1;
		
		//If this is an invalid account id
		if(accountId == -1 || !activeAccounts.containsKey(accountId)) {
			int defaultPjsipAccount = pjsua.acc_get_default();
			
			//If default account is not active
			if(!activeAccounts.containsValue(defaultPjsipAccount)) {
				for (Integer accId : activeAccounts.keySet()) {
					//Use the first account as valid account
					if(accId != null) {
						accountId = accId;
						pjsipAccountId = activeAccounts.get(accId);
						break;
					}
				}
			}else {
				//Use the default account 
				for (Integer accId : activeAccounts.keySet()) {
					if(activeAccounts.get(accId) == defaultPjsipAccount) {
						accountId = accId;
						pjsipAccountId = defaultPjsipAccount;
						break;
					}
				}
			}
		}else {
			pjsipAccountId = activeAccounts.get(accountId);
		}
		
		if(pjsipAccountId == -1 || accountId == -1) {
			Log.e(THIS_FILE, "Unable to find a valid account for this call");
			return -1;
		}
		
		
		//Check integrity of callee field
		if( ! Pattern.matches("^.*(<)?sip(s)?:[^@]*@[^@]*(>)?", callee) ) {
			//Assume this is a direct call using digit dialer
			Log.d(THIS_FILE, "default acc : "+accountId);
			Account account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountId);
				db.close();
			}
			String defaultDomain = account.getDefaultDomain();
			
			
			Log.d(THIS_FILE, "default domain : "+defaultDomain);
			if(Pattern.matches("^sip(s)?:[^@]*$", callee)) {
				callee = callee+"@"+defaultDomain;
			}else {
				callee = "sip:"+callee+"@"+defaultDomain;
			}
		}
		
		
		Log.d(THIS_FILE, "will call "+callee);
		if(pjsua.verify_sip_url(callee) == 0) {
			pj_str_t uri = pjsua.pj_str_copy(callee);
			Log.d(THIS_FILE, "get for outgoing");
			if(accountId == -1) {
				accountId = pjsua.acc_find_for_outgoing(uri);
			}
			
			//Nothing to do with this values
			byte[] user_data = new byte[1];
			int[] call_id = new int[1];
			return pjsua.call_make_call(pjsipAccountId, uri , 0, user_data, null, call_id);
		} else {
			Log.e(THIS_FILE, "Asked for a bad uri "+callee);
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.invalid_sip_uri, 0));
		}
		return -1;
	}
	
	
	/**
	 * Send a dtmf signal to a call
	 * @param callId the call to send the signal
	 * @param keyCode the keyCode to send (android style)
	 * @return
	 */
	protected int sendDtmf(int callId, int keyCode) {
		if(!created) {
			return -1;
		}
		
		KeyCharacterMap km = KeyCharacterMap.load(KeyCharacterMap.NUMERIC);
		
		String keyPressed = String.valueOf(km.getNumber(keyCode));
		pj_str_t pjKeyPressed = pjsua.pj_str_copy(keyPressed);
		
		int res = -1;
		if(prefsWrapper.useSipInfoDtmf()) {
			res = pjsua.send_dtmf_info(callId, pjKeyPressed);
			Log.d(THIS_FILE, "Has been sent DTMF : "+res);
		}else {
			res = pjsua.call_dial_dtmf(callId, pjKeyPressed);
			if(res != pjsua.PJ_SUCCESS) {
				res = sendPjMediaDialTone(callId, keyPressed);
			}
		}
		return res;
	}
	
	


	protected int callHold(int callId) {
		if(created) {
			return pjsua.call_set_hold(callId, null);
		}
		return -1;
	}



	protected int callReinvite(int callId, boolean unhold) {
		if(created) {
			return pjsua.call_reinvite(callId, unhold?1:0, null);
		}
		return -1;
	}

	
	
	// ------
	// Dialtone generator
	// ------
	
	private final static Map<String, short[]> digitMap = new HashMap<String, short[]>(){
		private static final long serialVersionUID = -6656807954448449227L;

		{
	    	put("0", new short[] {941, 1336});
	    	put("1", new short[] {697, 1209});
	    	put("2", new short[] {697, 1336});
	    	put("3", new short[] {697, 1477});
	    	put("4", new short[] {770, 1209});
	    	put("5", new short[] {770, 1336});
	    	put("6", new short[] {770, 1477});
	    	put("7", new short[] {852, 1209});
	    	put("8", new short[] {852, 1336});
	    	put("9", new short[] {852, 1477});
	    	put("a", new short[] {697, 1633});
	    	put("b", new short[] {770, 1633});
	    	put("c", new short[] {852, 1633});
	    	put("d", new short[] {941, 1633});
	    	put("*", new short[] {941, 1209});
	    	put("#", new short[] {941, 1477});
	    }
	};
	
	
	private int startDialtoneGenerator(int callId) {
		synchronized(dialtoneMutext) {
			pjsua_call_info info = new pjsua_call_info();
			pjsua.call_get_info(callId, info);
			int status;
			
			dialtonePool = pjsua.pjsua_pool_create("mycall", 512, 512);
			pj_str_t name = pjsua.pj_str_copy("dialtoneGen");
			long clock_rate = 8000;
			long channel_count = 1;
			long samples_per_frame = 160;
			long bits_per_sample = 16;
			long options = 0;
			int[] dialtoneSlotPtr = new int[1];
			dialtoneGen = new pjmedia_port();
			status = pjsua.pjmedia_tonegen_create2(dialtonePool, name, clock_rate, channel_count, samples_per_frame, bits_per_sample, options, dialtoneGen);
			if(status != pjsua.PJ_SUCCESS) {
				stopDialtoneGenerator();
				return status;
			}
			status = pjsua.conf_add_port(dialtonePool, dialtoneGen, dialtoneSlotPtr);
			if(status != pjsua.PJ_SUCCESS) {
				stopDialtoneGenerator();
				return status;
			}
			dialtoneSlot = dialtoneSlotPtr[0];
			status = pjsua.conf_connect(dialtoneSlot, info.getConf_slot());
			if(status != pjsua.PJ_SUCCESS) {
				dialtoneSlot = -1;
				stopDialtoneGenerator();
				return status;
			}
			return pjsua.PJ_SUCCESS;
		}
	}
	
	public void stopDialtoneGenerator() {
		synchronized(dialtoneMutext) {
			//Destroy the port
			if(dialtoneSlot != -1) {
				pjsua.conf_remove_port(dialtoneSlot);
				dialtoneSlot = -1;
			}
			
			dialtoneGen = null;
			//pjsua.port_destroy(dialtoneGen);
			
			if(dialtonePool != null) {
				pjsua.pj_pool_release(dialtonePool);
				dialtonePool = null;
			}
		}
			  
	}
	
	private int sendPjMediaDialTone(int callId, String character) {
		if(!digitMap.containsKey(character)) {
			return -1;
		}
		if(dialtoneGen == null) {
			int status = startDialtoneGenerator(callId);
			if(status != pjsua.PJ_SUCCESS) {
				return -1;
			}
		}
		
		short freq1 = digitMap.get(character)[0];
		short freq2 = digitMap.get(character)[1];
		
		//Play the tone
		pjmedia_tone_desc[] d = new pjmedia_tone_desc[1];
		d[0] = new pjmedia_tone_desc();
		d[0].setVolume((short) 0);
		d[0].setOn_msec((short) 100);
		d[0].setOff_msec((short) 200);
		d[0].setFreq1(freq1);
		d[0].setFreq2(freq2);
		return pjsua.pjmedia_tonegen_play(dialtoneGen, 1, d, 0);
	}
	
	/**
	 * Get the native library file
	 * First search in local files of the app (previously downloaded from the network)
	 * Then search in lib (bundlized method)
	 * @param context the context of the app that what to get it back
	 * @return the file if any, null else
	 */
	public static File getStackLibFile(Context context) {
		// Standard case
		File standard_out = getGuessedStackLibFile(context);
		if (standard_out.exists()) {
			return standard_out;
		}
		

		// One target build
		// TODO : find a clean way to access the libPath for one shot builds
		File target_for_build = new File(context.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
		Log.d(THIS_FILE, "Search for " + target_for_build.getAbsolutePath());
		if (target_for_build.exists()) {
			return target_for_build;
		}

		return null;
	}
	
	public static boolean isBundleStack(Context ctx) {
		File target_for_build = new File(ctx.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
		Log.d(THIS_FILE, "Search for " + target_for_build.getAbsolutePath());
		return target_for_build.exists();
	}
	
	public static boolean hasStackLibFile(Context ctx) {
		File guessed_file = getStackLibFile(ctx);
		if(guessed_file == null) {
			return false;
		}
		return guessed_file.exists();
	}
	
	public static File getGuessedStackLibFile(Context ctx) {
		return ctx.getFileStreamPath(SipService.STACK_FILE_NAME);
	}
	
	public static ArrayList<String> codecs;
	
	private void initCodecs(){
		if(codecs == null) {
			int nbr_codecs = pjsua.codecs_get_nbr();
			Log.d(THIS_FILE, "Codec nbr : "+nbr_codecs);
			codecs = new ArrayList<String>();
			for (int i = 0; i< nbr_codecs; i++) {
				String codecId = pjsua.codecs_get_id(i).getPtr();
				codecs.add(codecId);
				Log.d(THIS_FILE, "Added codec "+codecId);
			}
		}
	}
	
	
	private void setCodecsPriorities() {
		if(codecs != null) {
			for(String codec : codecs) {
				if(prefsWrapper.hasCodecPriority(codec)) {
					Log.d(THIS_FILE, "Set codec "+codec+" : "+prefsWrapper.getCodecPriority(codec, "130"));
					pjsua.codec_set_priority(pjsua.pj_str_copy(codec), prefsWrapper.getCodecPriority(codec, "130"));
					
					/*
					pjmedia_codec_param param = new pjmedia_codec_param();
					pjsua.codec_get_param(pjsua.pj_str_copy(codec), param);
					param.getSetting().setPenh(0);
					pjsua.codec_set_param(pjsua.pj_str_copy(codec), param );
					*/
				}
			}
		}
	}
	
	
	private Handler ToastHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 != 0) {
				Toast.makeText(SipService.this, msg.arg1, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(SipService.this, (String) msg.obj, Toast.LENGTH_LONG).show();
			}
		}
	};

	
	/**
	 * Method called by the native sip stack to set the audio mode to a valid state for a call
	 */
	public static void setAudioInCall() {
		Log.i(THIS_FILE, "Audio driver ask to set in call");
		if(mediaManager != null) {
			mediaManager.setAudioInCall();
		}
	}
	
	/**
	 * Method called by the native sip stack to unset audio mode when track and recorder are stopped
	 */
	public static void unsetAudioInCall() {
		Log.i(THIS_FILE, "Audio driver ask to unset in call");
		if(mediaManager != null) {
			mediaManager.unsetAudioInCall();
		}
	}
	
	public static UAStateReceiver getUAStateReceiver() {
		return userAgentReceiver;
	}
	
	private String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(THIS_FILE, "Error while getting self IP",  ex);
	    }
	    return null;
	}
	
	
	
//	private Integer oldNetworkType = null;
//	private State oldNetworkState = null;
	private String oldIPAddress = "0.0.0.0";
	
	private synchronized void dataConnectionChanged() {
		//Check if it should be ignored first
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		
		boolean ipHasChanged = false;

		if(ni != null) {
//			Integer currentType = ni.getType();
			String currentIPAddress = getLocalIpAddress();
//			State currentState = ni.getState();
			Log.d(THIS_FILE, "IP changes ?"+oldIPAddress+" vs "+currentIPAddress);
		//	if(/*currentType == oldNetworkType &&*/ currentState == oldNetworkState) {
				if(oldIPAddress == null || !oldIPAddress.equalsIgnoreCase(currentIPAddress)) {
					
					if(oldIPAddress == null || ( oldIPAddress != null && !oldIPAddress.equalsIgnoreCase("0.0.0.0") )) {
						//We just ignore this one
						Log.d(THIS_FILE, "IP changing request >> Must restart sip stack");
						ipHasChanged = true;
					}
				}
		//	}
			
			oldIPAddress = currentIPAddress;
//			oldNetworkState = currentState;
//			oldNetworkType = currentType;
		}else {
			oldIPAddress = null;
//			oldNetworkState = null;
//			oldNetworkType = null;
		}
		
		if (prefsWrapper.isValidConnectionForOutgoing() || prefsWrapper.isValidConnectionForIncoming()) {
			if (!created) {
				// we was not yet started, so start now
				Thread t = new Thread() {
					public void run() {
						try {
							sipStart();
						}catch(IllegalMonitorStateException e) {
							Log.e(THIS_FILE, "Not able to start sip right now", e);
						}
					}
				};
				t.start();
			} else if(ipHasChanged) {
				// Check if IP has changed between 
			/*	Log.i(THIS_FILE, "Ip changed remove/re - add all accounts");
				reRegisterAllAccounts();
				if(created) {
					pjsua.med
				}*/
				if(userAgentReceiver.getActiveCallInProgress() == null) {
					Thread t = new Thread() {
						public void run() {
							try {
								sipStop();
								sipStart();
							}catch(IllegalMonitorStateException e) {
								Log.e(THIS_FILE, "Not able to start sip right now", e);
							}
						}
					};
					t.start();
					//Log.e(THIS_FILE, "We should restart the stack ! ");
				}else {
					//TODO : else refine things => STUN, registration etc...
					ToastHandler.sendMessage(ToastHandler.obtainMessage(0, 0, 0, "Connection have been lost... you may have lost your communication. Hand over is not yet supported"));
				}
			}else {
				Log.d(THIS_FILE, "Nothing done since already well registered");
			}
			
		} else {
			if(created && userAgentReceiver != null) {
				if(userAgentReceiver.getActiveCallInProgress() != null) {
					Log.w(THIS_FILE, "There is an ongoing call ! don't stop !! and wait for network to be back...");
					return;
				}
			}
			Log.d(THIS_FILE, "Will stop SERVICE");
			Thread t = new Thread() {
				public void run() {
					Log.i(THIS_FILE, "Stop SERVICE");
					sipStop();
					//OK, this will be done only if the last bind is released
					stopSelf();
				}
			};
			t.start();
			
		}
	}


	public int getGSMCallState() {
		return telephonyManager.getCallState();
	}

}
