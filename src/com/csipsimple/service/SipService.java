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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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
import org.pjsip.pjsua.pjsua_msg_data;
import org.pjsip.pjsua.pjsua_transport_config;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
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
import android.view.KeyCharacterMap;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.CallInfo;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class SipService extends Service {

	static boolean created = false;
	static boolean creating = false;
	static String THIS_FILE = "SIP SRV";

	
	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	final static String ACTION_CONNECTIVITY_CHANGED = "android.net.conn.CONNECTIVITY_CHANGE";
	// -------
	// Static constants
	// -------
	
	final public static String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
	final public static String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	final public static String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";

	

	final public static int REGISTER_NOTIF_ID = 1;
	final public static int CALL_NOTIF_ID =  REGISTER_NOTIF_ID+1;
	
	private NotificationManager notificationManager;

	public final static String STACK_FILE_NAME = "libpjsipjni.so";

	private static Object pjAccountsCreationLock = new Object();
	
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
		public void sipStart() throws RemoteException { SipService.this.sipStart(); }
		
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
		 * Populate pjsip accounts with accounts saved in sqlite
		 */
		@Override
		public void addAllAccounts() throws RemoteException { SipService.this.registerAllAccounts(); }
		

		/**
		 * Unregister and delete accounts registred
		 */
		@Override
		public void removeAllAccounts() throws RemoteException { SipService.this.unregisterAllAccounts(true); }
		
		
		/**
		 * Reload all accounts with values found in database
		 */
		@Override
		public void reAddAllAccounts() throws RemoteException { SipService.this.reRegisterAllAccounts(); }
		
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
			// TODO: popup here
		}

		/**
		 * Make a call
		 * @param callee remote contact ot call
		 * If not well formated we try to add domain name of the default account
		 */
		@Override
		public void makeCall(String callee, int accountId) throws RemoteException { SipService.this.makeCall(callee, accountId); }

		
		
		/**
		 * Answer an incoming call
		 * @param callId the id of the call to answer to
		 * @param status the status code to send
		 */
		@Override
		public int answer(int callId, int status) throws RemoteException { return SipService.this.callAnswer(callId, status); }
		
		/**
		 * Hangup a call
		 * @param callId the id of the call to hang up
		 * @param status the status code to send
		 */
		@Override
		public int hangup(int callId, int status) throws RemoteException { return SipService.this.callHangup(callId, status); }

		
		@Override
		public int sendDtmf(int callId, int keyCode) throws RemoteException { return SipService.this.sendDtmf(callId, keyCode); }

		@Override
		public CallInfo getCallInfo(int callId) throws RemoteException {
			if(created) {
				CallInfo callInfo = userAgentReceiver.getCurrentCallInfo();
				if(callId != callInfo.getCallId()) {
					Log.w(THIS_FILE, "we try to get an info for a call that is not the current one :  "+callId);
					callInfo = new CallInfo(callId);
				}
				return callInfo;
			}
			return null;
		}
	};

	private DBAdapter db;
	private WakeLock wakeLock;
	private WifiLock wifiLock;
	private UAStateReceiver userAgentReceiver;
	public static boolean hasSipStack = false;
	private boolean sipStackIsCorrupted = false;
	private ServiceDeviceStateReceiver deviceStateReceiver;
	PreferencesWrapper prefsWrapper;
	private pj_pool_t dialtonePool;
	private pjmedia_port dialtoneGen;
	private int dialtoneSlot = -1;
	private Object dialtoneMutext = new Object();
	
	 

	// Broadcast receiver for the service
	private class ServiceDeviceStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Handle the connectivity changed method
			// should re-register sip
			if (intent.getAction().equals(ACTION_CONNECTIVITY_CHANGED)) {
				Log.v(THIS_FILE, "Connectivity has changed");
				//Thread it to be sure to not block the device if registration take time
				Thread t = new Thread() {
					@Override
					public void run() {
						if (prefsWrapper.isValidConnectionForOutgoing() || prefsWrapper.isValidConnectionForIncoming()) {
							if (!created) {
								// we was not yet started, so start now
								sipStart();
							} else {
								// update registration IP : for now remove / reregister all accounts
								reRegisterAllAccounts();
							}
						} else {
							Log.i(THIS_FILE, "Stop SERVICE");
							try {
								SipService.this.unregisterReceiver(deviceStateReceiver);
							}catch(IllegalArgumentException e) {
								//This is the case if already unregistered itself
								//Python like usage of try ;) : nothing to do here since it could be a standard case
								//And in this case nothing has to be done
							}
							SipService.this.sipStop();
							//OK, this will be done only if the last bind is released
							SipService.this.stopSelf();
							
						}
					}
				};
				t.start();
			}
		}
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i(THIS_FILE, "Create SIP Service");
		db = new DBAdapter(this);
		prefsWrapper = new PreferencesWrapper(this);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		
		// TODO : check connectivity, else just finish itself
		if ( !prefsWrapper.isValidConnectionForOutgoing() && !prefsWrapper.isValidConnectionForIncoming()) {
			stopSelf();
			return;
		}


		// Register own broadcast receiver
		IntentFilter intentfilter = new IntentFilter();
		intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		// TODO : handle theses receiver filters (-- inspired from SipDroid project
		// : sipdroid.org)
		// intentfilter.addAction(Receiver.ACTION_DATA_STATE_CHANGED);
		// intentfilter.addAction(Receiver.ACTION_PHONE_STATE_CHANGED);
		// intentfilter.addAction(Receiver.ACTION_DOCK_EVENT);
		// intentfilter.addAction(Intent.ACTION_HEADSET_PLUG);
		// intentfilter.addAction(Intent.ACTION_USER_PRESENT);
		// intentfilter.addAction(Intent.ACTION_SCREEN_OFF);
		// intentfilter.addAction(Intent.ACTION_SCREEN_ON);
		// intentfilter.addAction(Receiver.ACTION_VPN_CONNECTIVITY);
		registerReceiver(deviceStateReceiver = new ServiceDeviceStateReceiver(), intentfilter);

		tryToLoadStack();
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(THIS_FILE, "Destroying SIP Service");
		try {
			unregisterReceiver(deviceStateReceiver);
		}catch(IllegalArgumentException e) {
			//This is the case if already unregistered itself
			//Python style usage of try ;) : nothing to do here since it could be a standard case
			//And in this case nothing has to be done
		}
		sipStop();
		notificationManager.cancelAll();
		Log.i(THIS_FILE, "--- SIP SERVICE DESTROYED ---");
		
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Autostart the stack
		if(!hasSipStack) {
			tryToLoadStack();
		}
		sipStart();
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

				//TODO: explain user what is happening
				Intent it = new Intent(Intent.ACTION_VIEW);
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				it.setData(Uri.parse("http://code.google.com/p/csipsimple/wiki/NewHardwareSupportRequest"));
				startActivity(it);
				stopSelf();
			} catch (Exception e) {
				Log.e(THIS_FILE, "We have a problem with the current stack....", e);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	// Start the sip stack according to current settings
	synchronized void sipStart() {
		if (!hasSipStack) {
			Log.e(THIS_FILE, "We have no sip stack, we can't start");
			return;
		}
		
		if(!prefsWrapper.isValidConnectionForIncoming() && !prefsWrapper.isValidConnectionForOutgoing()) {
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "Not able to start sip stack");
			return;
		}

		Log.i(THIS_FILE, "Will start sip : " + (!created));
		//Ensure the stack is not already created or is being created
		if (!created && !creating) {
			creating = true;
			//Thread it to not lock everything
			Thread thread = new Thread() {
				@Override
				public void run() {
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
						pjsua.setCallbackObject(userAgentReceiver);
						

						Log.d(THIS_FILE, "Attach is done to callback");
						
						// MAIN CONFIG
						int isStunEnabled = prefsWrapper.getStunEnabled();
						if(isStunEnabled == 1) {
							//TODO : WARNING : This is deprecated, should use array instead but ok for now
							cfg.setStun_host(pjsua.pj_str_copy(prefsWrapper.getStunServer()));
						}
						cfg.setUser_agent(pjsua.pj_str_copy("CSipSimple"));

						// LOGGING CONFIG
						pjsua.logging_config_default(log_cfg);
						log_cfg.setConsole_level(Log.LOG_LEVEL);
						log_cfg.setLevel(Log.LOG_LEVEL);
						log_cfg.setMsg_logging(pjsuaConstants.PJ_TRUE);

						// MEDIA CONFIG
						pjsua.media_config_default(media_cfg);

						// For now only this cfg is supported
						media_cfg.setChannel_count(1);
						media_cfg.setSnd_auto_close_time(prefsWrapper.getAutoCloseTime());
						// Disable echo cancellation
						if (!prefsWrapper.hasEchoCancellation()) {
							media_cfg.setEc_tail_len(0);
						}
						media_cfg.setNo_vad(prefsWrapper.getNoVad());
						media_cfg.setQuality(prefsWrapper.getMediaQuality());
						media_cfg.setClock_rate(prefsWrapper.getClockRate());
						
						media_cfg.setEnable_ice(prefsWrapper.getIceEnabled());
						
						int isTurnEnabled = prefsWrapper.getTurnEnabled();
						
						if(isTurnEnabled == 1) {
							media_cfg.setEnable_turn(isTurnEnabled);
							media_cfg.setTurn_server(pjsua.pj_str_copy(prefsWrapper.getTurnServer()));
						}
						
						
						
						// INITIALIZE
						status = pjsua.init(cfg, log_cfg, media_cfg);
						if (status != pjsuaConstants.PJ_SUCCESS) {
							Log.e(THIS_FILE, "Fail to init pjsua with failure code " + status);
							pjsua.destroy();
							created = false;
							creating = false;
							return;
						}
					}

					// Add transports
					{
						//TODO : factorize this !!
						if(prefsWrapper.isTCPEnabled()) {
							pjsua_transport_config cfg = new pjsua_transport_config();

							pjsua.transport_config_default(cfg);
							cfg.setPort(prefsWrapper.getTCPTransportPort());
							
							status = pjsua.transport_create(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, cfg, null);
							if (status != pjsuaConstants.PJ_SUCCESS) {
								Log.e(THIS_FILE, "Fail to add transport with failure code " + status);
								pjsua.destroy();
								creating = false;
								created = false;
								return;
							}
						}
						if(prefsWrapper.isUDPEnabled()) {
							pjsua_transport_config cfg = new pjsua_transport_config();

							pjsua.transport_config_default(cfg);
							cfg.setPort(prefsWrapper.getUDPTransportPort());
							
							status = pjsua.transport_create(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, cfg, null);
							if (status != pjsuaConstants.PJ_SUCCESS) {
								Log.e(THIS_FILE, "Fail to add transport with failure code " + status);
								pjsua.destroy();
								creating = false;
								created = false;
								return;
							}
						}
						
					}

					// Initialization is done, now start pjsua
					status = pjsua.start();
					
					if(status != pjsua.PJ_SUCCESS) {
						Log.e(THIS_FILE, "Fail to start pjsip " + status);
						pjsua.destroy();
						creating = false;
						created = false;
						return;
					}
					
					// Init media codecs
					initCodecs();
					setCodecsPriorities();
					
					created = true;

					// Add accounts
					registerAllAccounts();
					creating = false;
					super.run();
				}

			};

			thread.start();
		}
	}

	
	/**
	 * Stop sip service
	 */
	synchronized void sipStop() {
		if (notificationManager != null) {
			notificationManager.cancel(REGISTER_NOTIF_ID);
		}

		if (created) {
			Log.d(THIS_FILE, "Detroying...");
			//This will destroy all accounts so synchronize with accounts management lock
			synchronized (pjAccountsCreationLock) {
				pjsua.destroy();
				accountsAddingStatus.clear();
				activeAccounts.clear();
			}
			if(userAgentReceiver != null) {
				userAgentReceiver.stopService();
				userAgentReceiver = null;
			}
		}
		releaseResources();
		
		created = false;
	}
	private boolean isRegistering = false;
	
	
	
	/**
	 * Add accounts from database
	 */
	private void registerAllAccounts() {
		synchronized (pjAccountsCreationLock) {
			if(!created) {
				Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
				return;
			}
			
			isRegistering = true;
			Log.d(THIS_FILE, "We are registring all accounts right now....");
			
			
			boolean hasSomeSuccess = false;
			List<Account> accountList;
			synchronized (db) {
				db.open();
				accountList = db.getListAccounts();
				db.close();
			}
			for (Account account : accountList) {
				if (account.active) {
					int status;
					if (activeAccounts.containsKey(account.id)) {
						status = pjsua.acc_modify(activeAccounts.get(account.id), account.cfg);
						// if(status == pjsuaConstants.PJ_SUCCESS){
						//		
						// }else{
						// Log.w(THIS_FILE,
						// "Modify account "+acc.display_name+" failed !!! ");
						// activeAccounts.put(acc.id, activeAccounts.get(acc.id));
						// }
						pjsua.acc_set_registration(activeAccounts.get(account.id), 1);
					} else {
						int[] acc_id = new int[1];
						status = pjsua.acc_add(account.cfg, pjsuaConstants.PJ_TRUE, acc_id);
						accountsAddingStatus.put(account.id, status);
	
						if (status == pjsuaConstants.PJ_SUCCESS) {
							Log.i(THIS_FILE, "Account " + account.display_name + " ( " + account.id + " ) added as " + acc_id[0]);
							activeAccounts.put(account.id, acc_id[0]);
							hasSomeSuccess = true;
						} else {
							Log.w(THIS_FILE, "Add account " + account.display_name + " failed !!! ");
	
						}
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
			isRegistering = false;
		}
	}

	/**
	 * Remove accounts from database
	 */
	private void unregisterAllAccounts(boolean cancelNotification) {
		if(!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}

		synchronized (pjAccountsCreationLock) {
			releaseResources();
			
			for (int c_acc_id : activeAccounts.values()) {
				pjsua.acc_set_registration(c_acc_id, 0);
				pjsua.acc_del(c_acc_id);
			}
			accountsAddingStatus.clear();
			activeAccounts.clear();
			
			// Send a broadcast message that for an account
			// registration state has changed
			Intent regStateChangedIntent = new Intent(ACTION_SIP_REGISTRATION_CHANGED);
			sendBroadcast(regStateChangedIntent);
			
			if (notificationManager != null && cancelNotification) {
				notificationManager.cancel(REGISTER_NOTIF_ID);
			}
		}
	}
	
	private void reRegisterAllAccounts() {
		if(!isRegistering) {
			unregisterAllAccounts(false);
			registerAllAccounts();
		}
	}
	
	

	private AccountInfo getAccountInfo(int accountDbId) {
		if(!created) {
			return null;
		}
		AccountInfo accountInfo;
		Log.d(THIS_FILE, "Get account infos....");
		synchronized (pjAccountsCreationLock) {
			Account account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountDbId);
				db.close();
			}
			accountInfo = new AccountInfo(account);
			if(accountsAddingStatus.containsKey(accountDbId)) {
				accountInfo.setAddedStatus(accountsAddingStatus.get(accountDbId));
				if(activeAccounts.containsKey(accountDbId)) {
					accountInfo.setPjsuaId(activeAccounts.get(accountDbId));
					pjsua_acc_info pjAccountInfo = new pjsua_acc_info();
					Log.d(THIS_FILE, "Get account info for "+accountDbId+" ==> "+activeAccounts.get(accountDbId));
					int success = pjsua.acc_get_info(activeAccounts.get(accountDbId), pjAccountInfo);
					if(success == pjsuaConstants.PJ_SUCCESS) {
						accountInfo.fillWithPjInfo(pjAccountInfo);
					}
				}
			}
		}
		return accountInfo;
	}
	
	
	public void updateRegistrationsState() {
		boolean hasSomeSuccess = false;
		AccountInfo info;
		synchronized (pjAccountsCreationLock) {
			for (int accountDbId : activeAccounts.keySet()) {
				info = getAccountInfo(accountDbId);
				if (info.getExpires() > 0 && info.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
					hasSomeSuccess = true;
				}
			}
		}
		
		// Handle status bar notification
		if (hasSomeSuccess) {
			int icon = R.drawable.sipok;
			CharSequence tickerText = "Sip Registred";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			Context context = getApplicationContext();
			CharSequence contentTitle = "SIP";
			CharSequence contentText = "Registred";

			Intent notificationIntent = new Intent(this, SipHome.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
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
		PowerManager pman = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (wakeLock == null) {
			wakeLock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.SipService");
			wakeLock.setReferenceCounted(false);
		}
		wakeLock.acquire();
		
		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if(wifiLock == null) {
			wifiLock = wman.createWifiLock("com.csipsimple.SipService");
		}
		if( prefsWrapper.getLockWifi() ) {
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo != null) {
				DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
				//We assume that if obtaining ip addr, we are almost connected so can keep wifi lock
				if(dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
					wifiLock.acquire();
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
		
		//Check integrity of callee field
		if( ! Pattern.matches("^.*(<)?sip(s)?:[^@]*@[^@]*(>)?", callee) ) {
			//Assume this is a direct call using digit dialer
			
			int defaultAccount = accountId;
			if(defaultAccount == -1) {
				defaultAccount = pjsua.acc_get_default();
			}
			Log.d(THIS_FILE, "default acc : "+defaultAccount);
			//TODO : use the standard call 
			pjsua_acc_info acc_info = new pjsua_acc_info();
			pjsua.acc_get_info(defaultAccount, acc_info);
			//Reformat with default account
			String default_domain = acc_info.getAcc_uri().getPtr();
			if(default_domain == null) {
				Log.e(THIS_FILE, "No default domain can't gess a domain for what you are asking");
				return -1;
			}
			Pattern p = Pattern.compile(".*<sip(s)?:[^@]*@([^@]*)>", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(default_domain);
			Log.d(THIS_FILE, "Try to find into "+default_domain);
			if(!m.matches()) {
				Log.e(THIS_FILE, "Default domain can't be guessed from regUri of this account");
				return -1;
			}
			default_domain = m.group(2);
			Log.d(THIS_FILE, "default domain : "+default_domain);
			//TODO : split domain
			if(Pattern.matches("^sip(s)?:[^@]*$", callee)) {
				callee = callee+"@"+default_domain;
			}else {
				callee = "sip:"+callee+"@"+default_domain;
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
			pjsua_msg_data msg = new pjsua_msg_data();
			int[] call_id = new int[1];
			return pjsua.call_make_call(accountId, uri , 0, user_data, msg, call_id);
		} else {
			//TODO : toast error
			Log.e(THIS_FILE, "asked for a bad uri "+callee);
			
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
		int res = pjsua.call_dial_dtmf(callId, pjsua.pj_str_copy(keyPressed));
		if(res != pjsua.PJ_SUCCESS) {
			res = sendPjMediaDialTone(callId, keyPressed);
		}
		return res;
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
	
	//TODO : provide a getter
	public static ArrayList<String> codecs;
	
	private void initCodecs(){
		//TODO : provide a way to flush this var
		if(codecs == null) {
			int nbr_codecs = pjsua.get_nbr_of_codecs();
			Log.d(THIS_FILE, "Codec nbr : "+nbr_codecs);
			codecs = new ArrayList<String>();
			for (int i = 0; i< nbr_codecs; i++) {
				String codecId = pjsua.codec_get_id(i).getPtr();
				codecs.add(codecId);
				Log.d(THIS_FILE, "Added codec "+codecId);
			}
		}
	}
	
	private void setCodecsPriorities() {
		if(codecs != null) {
			for(String codec : codecs) {
				if(prefsWrapper.hasCodecPriority(codec)) {
					pjsua.codec_set_priority(pjsua.pj_str_copy(codec), prefsWrapper.getCodecPriority(codec, "130"));
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


}
