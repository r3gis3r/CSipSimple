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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_qos_params;
import org.pjsip.pjsua.pj_qos_type;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsip_tls_setting;
import org.pjsip.pjsua.pjsip_transport_type_e;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_config;
import org.pjsip.pjsua.pjsua_logging_config;
import org.pjsip.pjsua.pjsua_media_config;
import org.pjsip.pjsua.pjsua_transport_config;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyCharacterMap;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.CallInfo;
import com.csipsimple.models.CallInfo.UnavailableException;
import com.csipsimple.models.PjSipAccount;
import com.csipsimple.models.SipMessage;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.SipUri;

public class SipService extends Service {

	public static final String INTENT_SIP_CONFIGURATION = "com.csipsimple.service.SipConfiguration";
	public static final String INTENT_SIP_SERVICE = "com.csipsimple.service.SipService";
	public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "com.csipsimple.accounts.activate";
	
	static boolean created = false;
	// static boolean creating = false;
	private static final String THIS_FILE = "SIP SRV";

	// -------
	// Static constants
	// -------
	// ACTIONS
	public static final String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
	public static final String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
	public static final String ACTION_SIP_CALLLOG = "com.csipsimple.phone.action.CALLLOG";
	public static final String ACTION_SIP_MESSAGES = "com.csipsimple.phone.action.MESSAGES";
	
	// SERVICE BROADCASTS
	public static final String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
	public static final String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	public static final String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
	public static final String ACTION_SIP_ACCOUNT_ACTIVE_CHANGED = "com.csipsimple.service.ACCOUNT_ACTIVE_CHANGED";
	public static final String ACTION_SIP_MESSAGE_RECEIVED = "com.csipsimple.service.MESSAGE_RECEIVED";
	//TODO : message sent?
	public static final String ACTION_SIP_MESSAGE_STATUS = "com.csipsimple.service.MESSAGE_STATUS";
	
	
	// EXTRAS
	public static final String EXTRA_CALL_INFO = "call_info";
	public static final String EXTRA_ACCOUNT_ID = "acc_id";
	public static final String EXTRA_ACTIVATE = "activate";

	public static final String STACK_FILE_NAME = "libpjsipjni.so";

	private static Object pjAccountsCreationLock = new Object();
	private static Object activeAccountsLock = new Object();
	private static Object callActionLock = new Object();

	private static Object creatingSipStack = new Object();

	// Map active account id (id for sql settings database) with acc_id (id for
	// pjsip)
	private static HashMap<Integer, Integer> activeAccounts = new HashMap<Integer, Integer>();
	private static HashMap<Integer, Integer> accountsAddingStatus = new HashMap<Integer, Integer>();

	// Implement public interface for the service
	private final ISipService.Stub binder = new ISipService.Stub() {
		/**
		 * Start the sip stack according to current settings (create pjsua)
		 */
		@Override
		public void sipStart() throws RemoteException {
			SipService.this.sipStart();
		}

		/**
		 * Stop the sip stack (destroy pjsua)
		 */
		@Override
		public void sipStop() throws RemoteException {
			SipService.this.sipStop();
		}

		/**
		 * Send SMS using
		 */
		@Override
		public void sendMessage(String msg,String to,int accountId)throws RemoteException { 
			SipService.this.sendMessage(msg, to, accountId);
		}
	
		/**
		 * Force the stop of the service
		 */
		@Override
		public void forceStopService() throws RemoteException {
			Log.d(THIS_FILE, "Try to force service stop");
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
		public void addAllAccounts() throws RemoteException {
			SipService.this.addAllAccounts();
		}

		/**
		 * Unregister and delete accounts registered
		 */
		@Override
		public void removeAllAccounts() throws RemoteException {
			SipService.this.unregisterAllAccounts(true);
		}

		/**
		 * Reload all accounts with values found in database
		 */
		@Override
		public void reAddAllAccounts() throws RemoteException {
			SipService.this.reAddAllAccounts();
		}

		@Override
		public void setAccountRegistration(int accountId, int renew) throws RemoteException {
			SipProfile account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountId);
				db.close();
			}
			SipService.this.setAccountRegistration(account, renew);
		}

		/**
		 * Get account and it's informations
		 * 
		 * @param accountId
		 *            the id (sqlite id) of the account
		 */
		@Override
		public AccountInfo getAccountInfo(int accountId) throws RemoteException {
			return SipService.this.getAccountInfo(accountId);
		}

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
		 * 
		 * @param callee
		 *            remote contact ot call If not well formated we try to add
		 *            domain name of the default account
		 */
		@Override
		public void makeCall(String callee, int accountId) throws RemoteException {
			SipService.this.makeCall(callee, accountId);
		}

		/**
		 * Answer an incoming call
		 * 
		 * @param callId
		 *            the id of the call to answer to
		 * @param status
		 *            the status code to send
		 */
		@Override
		public int answer(int callId, int status) throws RemoteException {
			synchronized (callActionLock) {
				return SipService.this.callAnswer(callId, status);
			}
		}

		/**
		 * Hangup a call
		 * 
		 * @param callId
		 *            the id of the call to hang up
		 * @param status
		 *            the status code to send
		 */
		@Override
		public int hangup(int callId, int status) throws RemoteException {
			synchronized (callActionLock) {
				return SipService.this.callHangup(callId, status);
			}
		}
		

		@Override
		public int xfer(int callId, String callee) throws RemoteException {
			Log.d(THIS_FILE, "XFER");
			synchronized (callActionLock) {
				return SipService.this.callXfer(callId, callee);
			}
		}

		@Override
		public int xferReplace(int callId, int otherCallId, int options) throws RemoteException {
			Log.d(THIS_FILE, "XFER-replace");
			synchronized (callActionLock) {
				return SipService.this.callXferReplace(callId, otherCallId, options);
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
				if (created/* && !creating */ && userAgentReceiver != null) {
					CallInfo callInfo = userAgentReceiver.getCallInfo(callId);
					if (callInfo == null) {
						return null;
					}
					if (callId != callInfo.getCallId()) {
						Log.w(THIS_FILE, "we try to get an info for a call that is not the current one :  " + callId);
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
			if (created && mediaManager != null) {
				mediaManager.setBluetoothOn(on);
			}
		}

		@Override
		public void setMicrophoneMute(boolean on) throws RemoteException {
			if (created && mediaManager != null) {
				mediaManager.setMicrophoneMute(on);
			}
		}

		@Override
		public void setSpeakerphoneOn(boolean on) throws RemoteException {
			if (created && mediaManager != null) {
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

		@Override
		public CallInfo[] getCalls() throws RemoteException {
			synchronized (creatingSipStack) {
				if (created && userAgentReceiver != null) {
					CallInfo[] callsInfo = userAgentReceiver.getCalls();
					return callsInfo;
				}
			}
			return null;
		}

		@Override
		public void confAdjustTxLevel(int port, float value) throws RemoteException {
			if (created && userAgentReceiver != null) {
				pjsua.conf_adjust_tx_level(port, value);
			}
			
		}

		@Override
		public void confAdjustRxLevel(int port, float value) throws RemoteException {
			if (created && userAgentReceiver != null) {
				pjsua.conf_adjust_rx_level(port, value);
			}
		}

		@Override
		public void setEchoCancellation(boolean on) throws RemoteException {
			if (created && userAgentReceiver != null) {
				Log.d(THIS_FILE, "set echo cancelation " + on);
				pjsua.set_ec(on ? prefsWrapper.getEchoCancellationTail() : 0, 0);
			}
		}

		@Override
		public void startRecording(int callId) throws RemoteException {
			if (created && userAgentReceiver != null) {
				userAgentReceiver.startRecording(callId);
				//TODO : broadcast it
			}
		}

		@Override
		public void stopRecording() throws RemoteException {
			if (created && userAgentReceiver != null) {
				userAgentReceiver.stopRecording();
			}
		}

		@Override
		public int getRecordedCall() throws RemoteException {
			if (created && userAgentReceiver != null) {
				return userAgentReceiver.getRecordedCall();
			}
			return -1;
		}

		@Override
		public boolean canRecord(int callId) throws RemoteException {
			if (created && userAgentReceiver != null) {
				return userAgentReceiver.canRecord(callId);
			}
			return false;
		}


	};

	private final ISipConfiguration.Stub binderConfiguration = new ISipConfiguration.Stub() {

		@Override
		public long addOrUpdateAccount(SipProfile acc) throws RemoteException {
			Log.d(THIS_FILE, ">>> addOrUpdateAccount from service");
			long finalId = -1;
			if (!hasSipStack) {
				Log.d(THIS_FILE, "TRY TO LOAD SIP STACK");
				tryToLoadStack();
			}

			if (hasSipStack) {
				synchronized (db) {
					db.open();
					if (acc.id == SipProfile.INVALID_ID) {
						finalId = db.insertAccount(acc);
					} else {
						db.updateAccount(acc);
						finalId = acc.id;
					}
					db.close();
				}
			}
			return finalId;
		}

		@Override
		public SipProfile getAccount(long accId) throws RemoteException {
			SipProfile result = null;

			if (!hasSipStack) {
				Log.d(THIS_FILE, "TRY TO LOAD SIP STACK");
				tryToLoadStack();
			}

			if (hasSipStack) {
				synchronized (db) {
					db.open();
					result = db.getAccount(accId);
					db.close();
				}
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

		@Override
		public String getPreferenceString(String key) throws RemoteException {
			return prefsWrapper.getPreferenceStringValue(key);
			
		}

		@Override
		public boolean getPreferenceBoolean(String key) throws RemoteException {
			return prefsWrapper.getPreferenceBooleanValue(key);
			
		}

		@Override
		public float getPreferenceFloat(String key) throws RemoteException {
			return prefsWrapper.getPreferenceFloatValue(key);
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
	private StreamDialtoneGenerator dialtoneGenerator;
	public PreferencesWrapper prefsWrapper;
	private ServicePhoneStateReceiver phoneConnectivityReceiver;
	private TelephonyManager telephonyManager;
	private ConnectivityManager connectivityManager;
	private Integer udpTranportId, tcpTranportId, tlsTransportId;

	private Integer hasBeenHoldByGSM = null;
	private SipNotifications notificationManager;
	public static boolean creating = false;

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
			String action = intent.getAction();
			if(action == null) {
				return;
			}
			
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				 //|| intent.getAction( ).equals(ACTION_DATA_STATE_CHANGED)
				Log.d(THIS_FILE, "Connectivity or data state has changed");
				// Thread it to be sure to not block the device if registration
				// take time
				Thread t = new Thread() {
					@Override
					public void run() {

						dataConnectionChanged();

					}
				};
				t.start();
			}else if(action.equals(ACTION_SIP_ACCOUNT_ACTIVE_CHANGED)) {
				final long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, -1);
				final boolean active = intent.getBooleanExtra(EXTRA_ACTIVATE, false);
				//Should that be threaded?
				if(accountId != -1) {
					SipProfile account;
					synchronized (db) {
						db.open();
						account = db.getAccount(accountId);
						db.close();
					}
					if(account != null) {
						setAccountRegistration(account, active?1:0);
					}
				}
			}
		}
	}

	private class ServicePhoneStateReceiver extends PhoneStateListener {
		@Override
		public void onDataConnectionStateChanged(int state) {
			Log.d(THIS_FILE, "Data connection state changed : " + state);
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
			Log.d(THIS_FILE, "Call state has changed !" + state + " : " + incomingNumber);

			// Avoid ringing if new GSM state is not idle
			if (state != TelephonyManager.CALL_STATE_IDLE && mediaManager != null) {
				mediaManager.stopRing();
			}

			// If new call state is not idle
			if (state != TelephonyManager.CALL_STATE_IDLE && userAgentReceiver != null) {
				CallInfo currentActiveCall = userAgentReceiver.getActiveCallInProgress();
				
				if (currentActiveCall != null) {
				
					if(state != TelephonyManager.CALL_STATE_RINGING) {
						//New state is not ringing nor idle... so off hook, hold current sip call
						hasBeenHoldByGSM = currentActiveCall.getCallId();
						SipService.this.callHold(hasBeenHoldByGSM);
						pjsua.set_no_snd_dev();
	
						AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
						am.setMode(AudioManager.MODE_IN_CALL);
					}else {
						//We have a ringing incoming call.
					}
				}
			} else {
				//GSM is now back to an IDLE state, resume previously stopped SIP calls 
				if (hasBeenHoldByGSM != null && created) {
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
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		notificationManager = new SipNotifications(this);

		// Check connectivity, else just finish itself
		if (!prefsWrapper.isValidConnectionForOutgoing() && !prefsWrapper.isValidConnectionForIncoming()) {
			Log.d(THIS_FILE, "Harakiri... we are not needed since no way to use self");
			stopSelf();
			return;
		}

		Log.setLogLevel(prefsWrapper.getLogLevel());

		// Do not thread since must ensure stack is loaded
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
		} catch (IllegalArgumentException e) {
			// This is the case if already unregistered itself
			// Python style usage of try ;) : nothing to do here since it could
			// be a standard case
			// And in this case nothing has to be done
			Log.d(THIS_FILE, "Has not to unregister telephony receiver");
		}
		if (phoneConnectivityReceiver != null) {
			Log.d(THIS_FILE, "Unregister telephony receiver");
			telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_NONE);
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
		if (hasSipStack) {
		//	final long accountToRenew = intent.getLongExtra(SipService.EXTRA_ACCOUNT_ID, -1);
			
			Thread t = new Thread() {
				public void run() {
					Log.d(THIS_FILE, "Start sip stack because start asked");
					try {
						sipStart();
					} catch (IllegalMonitorStateException e) {
						Log.e(THIS_FILE, "Not able to start sip right now", e);
					}
				}
			};
			t.start();
			
		}
		
		
	}

	private void loadAndConnectStack() {
		if (!hasSipStack) {
			Log.d(THIS_FILE, "TRY TO LOAD SIP STACK");
			tryToLoadStack();
		}

		if (hasSipStack) {
			// Register own broadcast receiver
			if (deviceStateReceiver == null) {
				IntentFilter intentfilter = new IntentFilter();
				intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				intentfilter.addAction(ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
				deviceStateReceiver = new ServiceDeviceStateReceiver();
				registerReceiver(deviceStateReceiver, intentfilter);
			}
			if (phoneConnectivityReceiver == null) {
				Log.d(THIS_FILE, "Listen for phone state ");
				phoneConnectivityReceiver = new ServicePhoneStateReceiver();
				telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
						| PhoneStateListener.LISTEN_CALL_STATE);
			}

		}
	}

	private void tryToLoadStack() {
		File stackFile = getStackLibFile(this);
		if (stackFile != null && !sipStackIsCorrupted) {
			try {
				// Try to load the stack
				System.load(stackFile.getAbsolutePath());
				hasSipStack = true;
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
				stopSelf();
			} catch (Exception e) {
				Log.e(THIS_FILE, "We have a problem with the current stack....", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {

		String serviceName = intent.getAction();
		Log.d(THIS_FILE, "Action is " + serviceName);
		if (serviceName == null || serviceName.equalsIgnoreCase(INTENT_SIP_SERVICE)) {
			Log.d(THIS_FILE, "Service returned");
			return binder;
		} else if (serviceName.equalsIgnoreCase(INTENT_SIP_CONFIGURATION)) {
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

		if (!prefsWrapper.isValidConnectionForIncoming() && !prefsWrapper.isValidConnectionForOutgoing()) {
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "Not able to start sip stack");
			return;
		}

		Log.i(THIS_FILE, "Will start sip : " + (!created /* && !creating */));
		synchronized (creatingSipStack) {
			// Ensure the stack is not already created or is being created
			if (!created/* && !creating */) {
				creating  = true;
				udpTranportId = null;
				tcpTranportId = null;

				int status;
				status = pjsua.create();
				Log.i(THIS_FILE, "Created " + status);
				// General config
				{
					pjsua_config cfg = new pjsua_config();
					pjsua_logging_config logCfg = new pjsua_logging_config();
					pjsua_media_config mediaCfg = new pjsua_media_config();

					// GLOBAL CONFIG
					pjsua.config_default(cfg);
					cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);
					if (userAgentReceiver == null) {
						userAgentReceiver = new UAStateReceiver();
						userAgentReceiver.initService(SipService.this);
					}
					if (mediaManager == null) {
						mediaManager = new MediaManager(SipService.this);
					}
					
					mediaManager.startService();
					
					pjsua.setCallbackObject(userAgentReceiver);
					

					Log.d(THIS_FILE, "Attach is done to callback");

					// MAIN CONFIG

					cfg.setUser_agent(pjsua.pj_str_copy(prefsWrapper.getUserAgent()));
					cfg.setThread_cnt(prefsWrapper.getThreadCount()); // one thread seems to be enough
					// for now
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
						pj_str_t[] stunServers = cfg.getStun_srv();
						int i = 0;
						for(String server : servers) {
							Log.d(THIS_FILE, "add server " + server.trim());
							stunServers[i] = pjsua.pj_str_copy(server.trim());
							i++;
						}
						cfg.setStun_srv(stunServers);
					}

					// IGNORE NOTIFY -- TODO : for now that's something we want
					// since it pollute battery life
			//		cfg.setEnable_unsolicited_mwi(pjsuaConstants.PJ_FALSE);
					

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
					mediaCfg.setEc_options(2); // ECHO SIMPLE : TODO -> setting that
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
					}

					// INITIALIZE
					status = pjsua.csipsimple_init(cfg, logCfg, mediaCfg);
					if (status != pjsuaConstants.PJ_SUCCESS) {
						String msg = "Fail to init pjsua "+ pjsua.get_error_message(status).getPtr();
						Log.e(THIS_FILE, msg);
						ToastHandler.sendMessage(ToastHandler.obtainMessage(0, msg));
						pjsua.csipsimple_destroy();
						created = false;
						creating = false;
						return;
					}
					
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
							pjsua.csipsimple_destroy();
							creating = false;
							created = false;
							return;
						}
			/*			 int[] p_acc_id = new int[1];
						 pjsua.acc_add_local(udpTranportId, pjsua.PJ_FALSE,
						 p_acc_id);
			*/
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
							pjsua.csipsimple_destroy();
							creating = false;
							created = false;
							return;
						}
			/*
						int[] p_acc_id = new int[1];
						pjsua.acc_add_local(tcpTranportId, pjsua.PJ_FALSE,
						p_acc_id);
			*/

					}

					// TLS
					if (prefsWrapper.isTLSEnabled() && !prefsWrapper.useIPv6() && (pjsua.can_use_tls() == pjsuaConstants.PJ_TRUE)) {
						tlsTransportId = createTransport(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, prefsWrapper.getTLSTransportPort());

						if (tlsTransportId == null) {
							pjsua.csipsimple_destroy();
							creating = false;
							created = false;
							return;
						}
						// int[] p_acc_id = new int[1];
						// pjsua.acc_add_local(tlsTransportId, pjsua.PJ_FALSE,
						// p_acc_id);
					}

					// RTP transport
					{
						pjsua_transport_config cfg = new pjsua_transport_config();
						pjsua.transport_config_default(cfg);
						cfg.setPort(prefsWrapper.getRTPPort());
						if(prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.ENABLE_QOS)) {
							Log.d(THIS_FILE, "Activate qos for voice packets");
							cfg.setQos_type(pj_qos_type.PJ_QOS_TYPE_VOICE);
						}

						if (prefsWrapper.useIPv6()) {
							status = pjsua.media_transports_create_ipv6(cfg);
						} else {
							status = pjsua.media_transports_create(cfg);
						}
						if (status != pjsuaConstants.PJ_SUCCESS) {
							String msg = "Fail to add media transport "+ pjsua.get_error_message(status).getPtr();
							Log.e(THIS_FILE, msg);
							
							ToastHandler.sendMessage(ToastHandler.obtainMessage(0, msg));
							pjsua.csipsimple_destroy();
							creating = false;
							created = false;
							return;
						}
					}
				}

				// Initialization is done, now start pjsua
				status = pjsua.start();

				if (status != pjsua.PJ_SUCCESS) {
					String msg = "Fail to start pjsip  "+ pjsua.get_error_message(status).getPtr();
					Log.e(THIS_FILE, msg);
					ToastHandler.sendMessage(ToastHandler.obtainMessage(0, msg));
					pjsua.csipsimple_destroy();
					creating = false;
					created = false;
					return;
				}

				// Init media codecs
				initCodecs();
				setCodecsPriorities();

				created = true;

				// Add accounts
				creating = false;
				addAllAccounts();
				
				updateRegistrationsState();
			}
		}
	}

	/**
	 * Stop sip service
	 */
	synchronized void sipStop() {
		if(created && userAgentReceiver != null) {
			if (userAgentReceiver.getActiveCallInProgress() != null) {
				return;
			}
		}
		
		if (notificationManager != null) {
			notificationManager.cancelRegisters();
		}
		synchronized (creatingSipStack) {
			if (created) {
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
				}
			}
		}
		releaseResources();
		Log.i(THIS_FILE, ">> Media m " + mediaManager);
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
		
		if(type.equals(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS)) {
			pjsip_tls_setting tlsSetting = cfg.getTls_setting();
			/*
			String serverName = prefsWrapper.getPreferenceStringValue(PreferencesWrapper.TLS_SERVER_NAME);
			if (!TextUtils.isEmpty(serverName)) {
				tlsSetting.setServer_name(pjsua.pj_str_copy(serverName));
			}
			String caListFile = prefsWrapper.getPreferenceStringValue(PreferencesWrapper.CA_LIST_FILE);
			if (!TextUtils.isEmpty(caListFile)) {
				tlsSetting.setCa_list_file(pjsua.pj_str_copy(caListFile));
			}
			String certFile = prefsWrapper.getPreferenceStringValue(PreferencesWrapper.CERT_FILE);
			if (!TextUtils.isEmpty(certFile)) {
				tlsSetting.setCert_file(pjsua.pj_str_copy(certFile));
			}
			String privKey = prefsWrapper.getPreferenceStringValue(PreferencesWrapper.PRIVKEY_FILE);
			if (!TextUtils.isEmpty(privKey)) {
				tlsSetting.setPrivkey_file(pjsua.pj_str_copy(privKey));
			}
			String tlsPwd = prefsWrapper.getPreferenceStringValue(PreferencesWrapper.TLS_PASSWORD);
			if (!TextUtils.isEmpty(tlsPwd)) {
				tlsSetting.setPassword(pjsua.pj_str_copy(tlsPwd));
			}
			boolean checkClient = prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.TLS_VERIFY_CLIENT);
			tlsSetting.setVerify_client(checkClient ? 1 : 0);
			
			*/
		
			tlsSetting.setMethod(prefsWrapper.getTLSMethod());
			boolean checkServer = prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.TLS_VERIFY_SERVER);
			tlsSetting.setVerify_server(checkServer ? 1 : 0);
			
			cfg.setTls_setting(tlsSetting);
		}
		
		//else?
		if(prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.ENABLE_QOS)) {
			Log.d(THIS_FILE, "Activate qos for this transport");
			pj_qos_params qosParam = cfg.getQos_params();
			qosParam.setDscp_val((short) prefsWrapper.getDSCPVal());
			qosParam.setFlags((short) 1); //DSCP
			cfg.setQos_params(qosParam);
		}
		

		status = pjsua.transport_create(type, cfg, tId);
		if (status != pjsuaConstants.PJ_SUCCESS) {
			String errorMsg = pjsua.get_error_message(status).getPtr();
			String msg = "Fail to create transport " + errorMsg +" ("+status+")";
			Log.e(THIS_FILE, msg);
			if(status == 120098) { /* Already binded */
				msg = getString(R.string.another_application_use_sip_port);
			}
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, msg));
			return null;
		}
		return tId[0];
	}

	/**
	 * Add accounts from database
	 */
	private void addAllAccounts() {
		Log.d(THIS_FILE, "We are adding all accounts right now....");

		boolean hasSomeSuccess = false;
		List<SipProfile> accountList;
		synchronized (db) {
			db.open();
			accountList = db.getListAccounts();
			db.close();
		}

		for (SipProfile account : accountList) {
			if (account.active) {
				if (addAccount(account) == pjsuaConstants.PJ_SUCCESS) {
					hasSomeSuccess = true;
				}
			}
		}

		if (hasSomeSuccess) {
			acquireResources();
		} else {
			releaseResources();
			if (notificationManager != null) {
				notificationManager.cancelRegisters();
			}
		}
	}

	private int addAccount(SipProfile profile) {
		int status = pjsuaConstants.PJ_FALSE;
		synchronized (pjAccountsCreationLock) {
			if (!created) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return status;
			}
			PjSipAccount account = new PjSipAccount(profile);
			
			account.applyExtraParams();

			Integer currentAccountId = null;
			synchronized (activeAccountsLock) {
				currentAccountId = activeAccounts.get(account.id);
			}

			// Force the use of a transport
			switch (account.transport) {
			case SipProfile.TRANSPORT_UDP:
				if(udpTranportId != null) {
					account.cfg.setTransport_id(udpTranportId);
				}
				break;
			case SipProfile.TRANSPORT_TCP:
				if(tcpTranportId != null) {
			//		account.cfg.setTransport_id(tcpTranportId);
				}
				break;
			case SipProfile.TRANSPORT_TLS:
				if(tlsTransportId != null) {
				//	account.cfg.setTransport_id(tlsTransportId);
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
					if(status == pjsuaConstants.PJ_SUCCESS) {
						pjsua.acc_set_online_status(currentAccountId, 1);
					}
				}
			} else {
				int[] accId = new int[1];
				if(account.cfg.getReg_uri().getPtr().equals("localhost")) {
					account.cfg.setReg_uri(pjsua.pj_str_copy(""));
					account.cfg.setProxy_cnt(0);
					status = pjsua.acc_add_local(udpTranportId, pjsuaConstants.PJ_FALSE, accId);
				}else {
					status = pjsua.acc_add(account.cfg, pjsuaConstants.PJ_FALSE, accId);
					
				}
				synchronized (activeAccountsLock) {
					accountsAddingStatus.put(account.id, status);
					if (status == pjsuaConstants.PJ_SUCCESS) {
						activeAccounts.put(account.id, accId[0]);
						pjsua.acc_set_online_status(accId[0], 1);
					}
				}
			}

		}

		return status;
	}

	private int setAccountRegistration(SipProfile account, int renew) {
		int status = pjsuaConstants.PJ_FALSE;
		synchronized (pjAccountsCreationLock) {
			if (!created || account == null) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return status;
			}
			if (activeAccounts.containsKey(account.id)) {
				int cAccId = activeAccounts.get(account.id);
				synchronized (activeAccountsLock) {
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
		if (!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}
		releaseResources();

		synchronized (pjAccountsCreationLock) {
			Log.d(THIS_FILE, "Remove all accounts");
			List<SipProfile> accountList;
			synchronized (db) {
				db.open();
				accountList = db.getListAccounts();
				db.close();
			}

			for (SipProfile account : accountList) {
				setAccountRegistration(account, 0);
			}

		}

		if (notificationManager != null && cancelNotification) {
			notificationManager.cancelRegisters();
		}
	}

	private void reAddAllAccounts() {
		Log.d(THIS_FILE, "RE REGISTER ALL ACCOUNTS");
		unregisterAllAccounts(false);
		addAllAccounts();
	}

	private AccountInfo getAccountInfo(int accountDbId) {
		if (!created) {
			return null;
		}
		AccountInfo accountInfo;

		Integer activeAccountStatus = null;
		Integer activeAccountPjsuaId = null;
		synchronized (activeAccountsLock) {
			activeAccountStatus = accountsAddingStatus.get(accountDbId);
			if (activeAccountStatus != null) {
				activeAccountPjsuaId = activeAccounts.get(accountDbId);
			}
		}
		SipProfile account;
		synchronized (db) {
			db.open();
			account = db.getAccount(accountDbId);
			db.close();
		}
		// If account has been removed meanwhile
		if (account == null) {
			return null;
		}
		
		accountInfo = new AccountInfo(account);
		if (activeAccountStatus != null) {
			accountInfo.setAddedStatus(activeAccountStatus);
			if (activeAccountPjsuaId != null) {
				accountInfo.setPjsuaId(activeAccountPjsuaId);
				pjsua_acc_info pjAccountInfo = new pjsua_acc_info();
				// Log.d(THIS_FILE,
				// "Get account info for account id "+accountDbId+" ==> (active within pjsip as) "+activeAccounts.get(accountDbId));
				int success = pjsua.acc_get_info(activeAccountPjsuaId, pjAccountInfo);
				if (success == pjsuaConstants.PJ_SUCCESS) {
					accountInfo.fillWithPjInfo(pjAccountInfo);
				}
			}
		}

		return accountInfo;
	}

	protected SipProfile getAccountForPjsipId(int accId) {
		return getAccountForPjsipId(accId, db);
	}
	
	@SuppressWarnings("unchecked")
	public static SipProfile getAccountForPjsipId(int accId, DBAdapter database) {
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
				synchronized (database) {
					database.open();
					SipProfile account = database.getAccount(entry.getKey());
					database.close();
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
			if( info != null ) {
				if(info.getWizard().equalsIgnoreCase("LOCAL")) {
					activeAccountsInfos.add(info);
				}else {
					if (info.getExpires() > 0 && info.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
						activeAccountsInfos.add(info);
					}
				}
			}
		}
		Collections.sort(activeAccountsInfos, accountInfoComparator);

		// Handle status bar notification
		if (activeAccountsInfos.size() > 0 && prefsWrapper.showIconInStatusBar()) {
			notificationManager.notifyRegisteredAccounts(activeAccountsInfos);
			acquireResources();
		} else {
			notificationManager.cancelRegisters();
			releaseResources();
		}
	}
	
	 
	private Comparator<AccountInfo> accountInfoComparator = new Comparator<AccountInfo>() {
		@Override
		public int compare(AccountInfo infos1,AccountInfo infos2) {
			if (infos1 != null && infos2 != null) {
				
				int c1 = infos1.getPriority();
				int c2 = infos2.getPriority();
				
				if (c1 > c2) {
					return -1;
				}
				if (c1 < c2) {
					return 1;
				}
			}

			return 0;
		}
	};

	/**
	 * Get the currently instanciated prefsWrapper (to be used by
	 * UAStateReceiver)
	 * 
	 * @return the preferenceWrapper instanciated
	 */
	public PreferencesWrapper getPrefs() {
		// Is never null when call so ok, just not check...
		return prefsWrapper;
	}

	/**
	 * Ask to take the control of the wifi and the partial wake lock if
	 * configured
	 */
	private void acquireResources() {
		// Add a wake lock
		if (prefsWrapper.usePartialWakeLock()) {
			PowerManager pman = (PowerManager) getSystemService(Context.POWER_SERVICE);
			if (wakeLock == null) {
				wakeLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.SipService");
				wakeLock.setReferenceCounted(false);
			}
			// Extra check if set reference counted is false ???
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
		}

		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiLock == null) {
			wifiLock = wman.createWifiLock("com.csipsimple.SipService");
			wifiLock.setReferenceCounted(false);
		}
		if (prefsWrapper.getLockWifi() && !wifiLock.isHeld()) {
			WifiInfo winfo = wman.getConnectionInfo();
			if (winfo != null) {
				DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
				// We assume that if obtaining ip addr, we are almost connected
				// so can keep wifi lock
				if (dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
					if (!wifiLock.isHeld()) {
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
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

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
			return pjsua.call_answer(callId, code, null, null);
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
			return pjsua.call_hangup(callId, code, null, null);
		}
		return -1;
	}
	
	public int callXfer(int callId, String callee) {
		if (created) {
			return pjsua.call_xfer(callId, pjsua.pj_str_copy(callee), null);
		}
		return -1;
	}
	
	public int callXferReplace(int callId, int otherCallId, int options) {
		if (created) {
			return pjsua.call_xfer_replaces(callId, otherCallId, options, null);
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
		//We have to ensure service is properly started and not just binded
		startService(new Intent(this, SipService.class));
		

		ToCall toCall = sanitizeSipUri(callee, accountId);
		if(toCall != null) {
			pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());
			
			// Nothing to do with this values
			byte[] userData = new byte[1];
			int[] callId = new int[1];
			return pjsua.call_make_call(toCall.getPjsipAccountId(), uri, 0, userData, null, callId);
		} else {
			Log.e(THIS_FILE, "Asked for a bad uri " + callee);
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.invalid_sip_uri, 0));
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
	protected int sendDtmf(int callId, int keyCode) {
		if (!created) {
			return -1;
		}

		KeyCharacterMap km = KeyCharacterMap.load(KeyCharacterMap.NUMERIC);

		String keyPressed = String.valueOf(km.getNumber(keyCode));
		pj_str_t pjKeyPressed = pjsua.pj_str_copy(keyPressed);

		int res = -1;
		if (prefsWrapper.useSipInfoDtmf()) {
			res = pjsua.send_dtmf_info(callId, pjKeyPressed);
			Log.d(THIS_FILE, "Has been sent DTMF INFO : " + res);
		} else {
			if (!prefsWrapper.forceDtmfInBand()) {
				//Generate using RTP
				res = pjsua.call_dial_dtmf(callId, pjKeyPressed);
				Log.d(THIS_FILE, "Has been sent in RTP DTMF : " + res);
			}
			
			if (res != pjsua.PJ_SUCCESS && !prefsWrapper.forceDtmfRTP()) {
				//Generate using analogic inband
				if(dialtoneGenerator == null) {
					dialtoneGenerator = new StreamDialtoneGenerator();
				}
				res = dialtoneGenerator.sendPjMediaDialTone(callId, keyPressed);
				Log.d(THIS_FILE, "Has been sent DTMF analogic : " + res);
			}
		}
		return res;
	}

	protected int callHold(int callId) {
		if (created) {
			return pjsua.call_set_hold(callId, null);
		}
		return -1;
	}

	protected int callReinvite(int callId, boolean unhold) {
		if (created) {
			return pjsua.call_reinvite(callId, unhold ? 1 : 0, null);
		}
		return -1;
	}


	public void stopDialtoneGenerator() {
		if(dialtoneGenerator != null) {
			dialtoneGenerator.stopDialtoneGenerator();
			dialtoneGenerator = null;
		}
	}
	


	/**
	 * Get the native library file First search in local files of the app
	 * (previously downloaded from the network) Then search in lib (bundlized
	 * method)
	 * 
	 * @param context
	 *            the context of the app that what to get it back
	 * @return the file if any, null else
	 */
	public static File getStackLibFile(Context context) {
		// Standard case
		File standardOut = getGuessedStackLibFile(context);
		//If production .so file exists and app is not in debuggable mode 
		//if debuggable we have to get the file from bundle dir
		if (standardOut.exists() && !isDebuggableApp(context)) {
			return standardOut;
		}

		// Have a look if it's not a dev build
		// TODO : find a clean way to access the libPath for one shot builds
		File targetForBuild = new File(context.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
		Log.d(THIS_FILE, "Search for " + targetForBuild.getAbsolutePath());
		if (targetForBuild.exists()) {
			return targetForBuild;
		}

		//Oups none exists.... reset version history
		PreferencesWrapper prefs = new PreferencesWrapper(context);
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_VERSION, "0.00-00");
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_ID, "");
		prefs.setPreferenceStringValue(DownloadLibService.CURRENT_STACK_URI, "");
		return null;
		
	}

	public static boolean hasBundleStack(Context ctx) {
		File targetForBuild = new File(ctx.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
		Log.d(THIS_FILE, "Search for " + targetForBuild.getAbsolutePath());
		return targetForBuild.exists();
	}

	public static boolean hasStackLibFile(Context ctx) {
		File guessedFile = getStackLibFile(ctx);
		if (guessedFile == null) {
			return false;
		}
		return guessedFile.exists();
	}

	public static File getGuessedStackLibFile(Context ctx) {
		return ctx.getFileStreamPath(SipService.STACK_FILE_NAME);
	}
	
	public static boolean isDebuggableApp(Context ctx) {
		try {
			PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			return ( (pinfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
		} catch (NameNotFoundException e) {
			// Should not happen....or something is wrong with android...
			Log.e(THIS_FILE, "Not possible to find self name", e);
		}
		return false;
	}

	public static ArrayList<String> codecs;

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
	 * Method called by the native sip stack to set the audio mode to a valid
	 * state for a call
	 */
	public static void setAudioInCall() {
		Log.i(THIS_FILE, "Audio driver ask to set in call");
		if (mediaManager != null) {
			mediaManager.setAudioInCall();
		}
	}

	/**
	 * Method called by the native sip stack to unset audio mode when track and
	 * recorder are stopped
	 */
	public static void unsetAudioInCall() {
		Log.i(THIS_FILE, "Audio driver ask to unset in call");
		if (mediaManager != null) {
			mediaManager.unsetAudioInCall();
		}
	}
	
	/**
	 * Send sms/message using SIP server
	 */
	public int sendMessage(String message, String callee, int accountId) {
		if (!created) {
			return -1;
		}
		
		//We have to ensure service is properly started and not just binded
		startService(new Intent(this, SipService.class));
		

		Log.d(THIS_FILE, "will sms " + callee);
		
		ToCall toCall = sanitizeSipUri(callee, accountId);
		if(toCall != null) {
			pj_str_t uri = pjsua.pj_str_copy(toCall.getCallee());
			pj_str_t text = pjsua.pj_str_copy(message);
			Log.d(THIS_FILE, "get for outgoing");
			if (accountId == -1) {
				accountId = pjsua.acc_find_for_outgoing(uri);
			}

			// Nothing to do with this values
			byte[] userData = new byte[1];
			
			Log.d("Sent", callee + " " + message + " " + toCall.getPjsipAccountId());
			SipMessage msg = new SipMessage(SipMessage.SELF, 
					SipUri.getCanonicalSipUri(toCall.getCallee()), SipUri.getCanonicalSipUri(toCall.getCallee()), 
					message, "text/plain", System.currentTimeMillis(), 
					SipMessage.MESSAGE_TYPE_QUEUED);
			msg.setRead(true);
			db.open();
			db.insertMessage(msg);
			db.close();
			Log.d(THIS_FILE, "Inserted "+msg.getTo());
			return pjsua.im_send(toCall.getPjsipAccountId(), uri, null, text, (org.pjsip.pjsua.SWIGTYPE_p_pjsua_msg_data)null, userData);
			
			
		}else {
			Log.e(THIS_FILE, "Asked for a bad uri " + callee);
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.invalid_sip_uri, 0));
		}
		
		return -1;
	}
	
	public void  showMessage(String msg)
	{
		Toast tag = Toast.makeText(SipService.this,msg, 100);
		tag.setDuration(100);
		tag.show();
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
			Log.e(THIS_FILE, "Error while getting self IP", ex);
		}
		return null;
	}

	// private Integer oldNetworkType = null;
	// private State oldNetworkState = null;
	private String oldIPAddress = "0.0.0.0";

	private synchronized void dataConnectionChanged() {
		// Check if it should be ignored first
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

		boolean ipHasChanged = false;

		if (ni != null) {
			// Integer currentType = ni.getType();
			String currentIPAddress = getLocalIpAddress();
			// State currentState = ni.getState();
			Log.d(THIS_FILE, "IP changes ?" + oldIPAddress + " vs " + currentIPAddress);
			// if(/*currentType == oldNetworkType &&*/ currentState ==
			// oldNetworkState) {
			if (oldIPAddress == null || !oldIPAddress.equalsIgnoreCase(currentIPAddress)) {

				if (oldIPAddress == null || (oldIPAddress != null && !oldIPAddress.equalsIgnoreCase("0.0.0.0"))) {
					// We just ignore this one
					Log.d(THIS_FILE, "IP changing request >> Must restart sip stack");
					ipHasChanged = true;
				}
			}
			// }

			oldIPAddress = currentIPAddress;
			// oldNetworkState = currentState;
			// oldNetworkType = currentType;
		} else {
			oldIPAddress = null;
			// oldNetworkState = null;
			// oldNetworkType = null;
		}

		if (prefsWrapper.isValidConnectionForOutgoing() || prefsWrapper.isValidConnectionForIncoming()) {
			if (!created) {
				// we was not yet started, so start now
				Thread t = new Thread() {
					public void run() {
						try {
							sipStart();
						} catch (IllegalMonitorStateException e) {
							Log.e(THIS_FILE, "Not able to start sip right now", e);
						}
					}
				};
				t.start();
			} else if (ipHasChanged) {
				// Check if IP has changed between
				/*
				 * Log.i(THIS_FILE, "Ip changed remove/re - add all accounts");
				 * reRegisterAllAccounts(); if(created) { pjsua.med }
				 */
				if (userAgentReceiver.getActiveCallInProgress() == null) {
					Thread t = new Thread() {
						public void run() {
							try {
								sipStop();
								sipStart();
							} catch (IllegalMonitorStateException e) {
								Log.e(THIS_FILE, "Not able to start sip right now", e);
							}
						}
					};
					t.start();
					// Log.e(THIS_FILE, "We should restart the stack ! ");
				} else {
					// TODO : else refine things => STUN, registration etc...
					ToastHandler.sendMessage(ToastHandler.obtainMessage(0, 0, 0,
							"Connection have been lost... you may have lost your communication. Hand over is not yet supported"));
				}
			} else {
				Log.d(THIS_FILE, "Nothing done since already well registered");
			}

		} else {
			if (created && userAgentReceiver != null) {
				if (userAgentReceiver.getActiveCallInProgress() != null) {
					Log.w(THIS_FILE, "There is an ongoing call ! don't stop !! and wait for network to be back...");
					return;
				}
			}
			Log.d(THIS_FILE, "Will stop SERVICE");
			Thread t = new Thread() {
				public void run() {
					Log.i(THIS_FILE, "Stop SERVICE");
					sipStop();
					// OK, this will be done only if the last bind is released
					stopSelf();
				}
			};
			t.start();

		}
	}

	public int getGSMCallState() {
		return telephonyManager.getCallState();
	}

	private class ToCall {
		private Integer pjsipAccountId;
		private String callee;
		public ToCall(Integer acc, String uri) {
			pjsipAccountId = acc;
			callee = uri;
		}
		
		/**
		 * @return the pjsipAccountId
		 */
		public Integer getPjsipAccountId() {
			return pjsipAccountId;
		}
		/**
		 * @return the callee
		 */
		public String getCallee() {
			return callee;
		}
	};
	
	private ToCall sanitizeSipUri(String callee, int accountId) {
		//accountId is the id in term of csipsimple database
		//pjsipAccountId is the account id in term of pjsip adding
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
			//If the account is valid
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
			SipProfile account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountId);
				db.close();
			}
			String defaultDomain = account.getDefaultDomain();

			Log.d(THIS_FILE, "default domain : " + defaultDomain);
			p = Pattern.compile("^sip(s)?:[^@]*$", Pattern.CASE_INSENSITIVE);
			if (p.matcher(callee).matches()) {
				callee = "<"+callee + "@" + defaultDomain+">";
			} else {
				//Should it be encoded?
				callee = "<sip:" + /*Uri.encode(*/callee/*)*/ + "@" + defaultDomain+">";
			}
		}else {
			callee = "<" + m.group(1) + ":" + m.group(2) + ">";
		}

		Log.d(THIS_FILE, "will call " + callee);
		if (pjsua.verify_sip_url(callee) == 0) {
			//In worse worse case, find back the account id for uri.. but probably useless case
			if(pjsipAccountId == SipProfile.INVALID_ID) {
				pjsipAccountId = pjsua.acc_find_for_outgoing(pjsua.pj_str_copy(callee));
			}
			return new ToCall(pjsipAccountId, callee);
		}
		
		return null;
	}
	
	

}
