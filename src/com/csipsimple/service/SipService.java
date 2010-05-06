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
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;
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
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class SipService extends Service {

	static boolean created = false;
	static boolean creating = false;
	static String THIS_FILE = "SIP SRV";

	
	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	final static String ACTION_CONNECTIVITY_CHANGED = "android.net.conn.CONNECTIVITY_CHANGE";
	

	public final static int REGISTER_NOTIF_ID = 1;
	public static final int CALL_NOTIF_ID =  REGISTER_NOTIF_ID+1;
	
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
		public void makeCall(String callee) throws RemoteException { SipService.this.makeCall(callee); }

		/**
		 * Force the stop of the service
		 */
		@Override
		public void forceStopService() throws RemoteException {
			Log.d(THIS_FILE,"Try to force service stop");
			stopSelf();
		}
		
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

		/**
		 * Get account and it's informations
		 * @param accountId the id (sqlite id) of the account
		 */
		@Override
		public AccountInfo getAccountInfo(int accountId) throws RemoteException { return SipService.this.getAccountInfo(accountId);}
		
		
		
		
	};

	private DBAdapter db;
	private WakeLock wakeLock;
	private WifiLock wifiLock;
	private UAStateReceiver userAgentReceiver;
	private boolean hasSipStack = false;
	private boolean sipStackIsCorrupted = false;
	private ServiceDeviceStateReceiver deviceStateReceiver;
	private PreferencesWrapper prefsWrapper;
	
	 

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
							SipService.this.unregisterReceiver(deviceStateReceiver);
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
		

		// TODO : check connectivity, else just finish itself

		Log.i(THIS_FILE, "Create SIP Service");
		db = new DBAdapter(this);
		prefsWrapper = new PreferencesWrapper(this);
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
			//Python like usage of try ;) : nothing to do here since it could be a standard case
			//And in this case nothing has to be done
		}
		sipStop();
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
	private synchronized void sipStart() {
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
					created = true;
					//General config
					{
						pjsua_config cfg = new pjsua_config();
						pjsua_logging_config log_cfg = new pjsua_logging_config();
						pjsua_media_config media_cfg = new pjsua_media_config();

						// GLOBAL CONFIG
						pjsua.config_default(cfg);
						cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);
						pjsua.setCallbackObject(userAgentReceiver = new UAStateReceiver());
						userAgentReceiver.initService(SipService.this);

						Log.d(THIS_FILE, "Attach is done to callback");

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
						if (!prefsWrapper.hasAutoCancellation()) {
							media_cfg.setEc_tail_len(0);
						}

						// INITIALIZE
						status = pjsua.init(cfg, log_cfg, media_cfg);
						if (status != pjsuaConstants.PJ_SUCCESS) {
							Log.e(THIS_FILE, "Fail to init pjsua with failure code " + status);
							pjsua.destroy();
							creating = false;
							created = false;
							return;
						}
					}

					// Add UDP transport
					{
						pjsua_transport_config cfg = new pjsua_transport_config();

						pjsua.transport_config_default(cfg);
						cfg.setPort(prefsWrapper.getTransportPort());
						status = pjsua.transport_create(prefsWrapper.getTransportType(), cfg, null);
						if (status != pjsuaConstants.PJ_SUCCESS) {
							Log.e(THIS_FILE, "Fail to add transport with failure code " + status);
							pjsua.destroy();
							creating = false;
							created = false;
							return;
						}
					}

					// Initialization is done, now start pjsua
					status = pjsua.start();					
					
					// Init media codecs
					setCodecsPriorities();

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
	private synchronized void sipStop() {
		if (notificationManager != null) {
			notificationManager.cancel(REGISTER_NOTIF_ID);
		}

		if (created) {
			Log.d(THIS_FILE, "Detroying...");

			pjsua.destroy();
			accountsAddingStatus.clear();
			activeAccounts.clear();
		}
		created = false;
	}
	
	/**
	 * Add accounts from database
	 */
	private void registerAllAccounts() {
		if(!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}
		synchronized (pjAccountsCreationLock) {
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
			Intent regStateChangedIntent = new Intent(UAStateReceiver.UA_REG_STATE_CHANGED);
			sendBroadcast(regStateChangedIntent);
			
			if (notificationManager != null && cancelNotification) {
				notificationManager.cancel(REGISTER_NOTIF_ID);
			}
		}
	}
	
	private void reRegisterAllAccounts() {
		unregisterAllAccounts(false);
		registerAllAccounts();
	}
	
	

	private AccountInfo getAccountInfo(int accountDbId) {
		if(!created) {
			return null;
		}
		AccountInfo accountInfo;
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
		for (int accountDbId : activeAccounts.keySet()) {
			info = getAccountInfo(accountDbId);
			if (info.getExpires() > 0 && info.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
				hasSomeSuccess = true;
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
			pjsua.call_answer(callId, code, null, null);
		}
		return 0;
	}
	
	
	/**
	 * Hangup a call
	 * @param callId the id of the call to hangup
	 * @param code the status code to send in the response
	 * @return
	 */
	public int callHangup(int callId, int code) {
		if(created) {
			pjsua.call_hangup(callId, code, null, null);
		}
		return 0;
	}
	
	/**
	 * Make a call
	 * @param callee remote contact ot call
	 * If not well formated we try to add domain name of the default account
	 */
	public void makeCall(String callee) {
		if(!created) {
			return;
		}
		
		//Check integrity of callee field
		if( ! Pattern.matches("^.*(<)?sip(s)?:[^@]*@[^@]*(>)?", callee) ) {
			//Assume this is a direct call using digit dialer
			
			int default_acc = pjsua.acc_get_default();
			Log.d(THIS_FILE, "default acc : "+default_acc);
			pjsua_acc_info acc_info = new pjsua_acc_info();
			pjsua.acc_get_info(default_acc, acc_info);
			//Reformat with default account
			String default_domain = acc_info.getAcc_uri().getPtr();
			if(default_domain == null) {
				Log.e(THIS_FILE, "No default domain can't gess a domain for what you are asking");
				return;
			}
			Pattern p = Pattern.compile(".*<sip(s)?:[^@]*@([^@]*)>", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(default_domain);
			Log.d(THIS_FILE, "Try to find into "+default_domain);
			if(!m.matches()) {
				Log.e(THIS_FILE, "Default domain can't be guessed from regUri of this account");
				return;
			}
			default_domain = m.group(2);
			Log.d(THIS_FILE, "default domain : "+default_domain);
			//TODO : split domain
			callee = "sip:"+callee+"@"+default_domain;
		}
		
		Log.d(THIS_FILE, "will call "+callee);
		if(pjsua.verify_sip_url(callee) == 0) {
			pj_str_t uri = pjsua.pj_str_copy(callee);
			Log.d(THIS_FILE, "get for outgoing");
			int acc_id = pjsua.acc_find_for_outgoing(uri);
			Log.d(THIS_FILE, "acc id : "+acc_id);
			
			//Nothing to do with this values
			byte[] user_data = new byte[1];
			pjsua_msg_data msg = new pjsua_msg_data();
			int[] call_id = new int[1];
			pjsua.call_make_call(acc_id, uri , 0, user_data, msg, call_id);
		} else {
			Log.e(THIS_FILE, "asked for a bad uri "+callee);
			
		}
		
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
	
	
	private void setCodecsPriorities() {
		//TODO : we should get back the available codecs from stack
		pj_str_t current_codec;
		current_codec = pjsua.pj_str_copy("speex/16000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("speex/16000/1", "130"));
		current_codec = pjsua.pj_str_copy("speex/8000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("speex/8000/1", "129")); 
		current_codec = pjsua.pj_str_copy("speex/32000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("speex/32000/1", "128"));
		current_codec = pjsua.pj_str_copy("GSM/8000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("GSM/8000/1", "128"));
		current_codec = pjsua.pj_str_copy("PCMU/8000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("PCMU/8000/1", "128"));
		current_codec = pjsua.pj_str_copy("PCMA/8000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("PCMA/8000/1", "128"));
		current_codec = pjsua.pj_str_copy("g722/8000/1");
		pjsua.codec_set_priority(current_codec, prefsWrapper.getCodecPriority("g722/8000/1", "128"));
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
