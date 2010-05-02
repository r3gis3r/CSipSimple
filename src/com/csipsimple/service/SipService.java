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
import org.pjsip.pjsua.pjsip_transport_type_e;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;
import org.pjsip.pjsua.pjsua_config;
import org.pjsip.pjsua.pjsua_logging_config;
import org.pjsip.pjsua.pjsua_media_config;
import org.pjsip.pjsua.pjsua_msg_data;
import org.pjsip.pjsua.pjsua_transport_config;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.utils.Log;

public class SipService extends Service {

	static boolean created = false;
	static boolean creating = false;
	static String THIS_FILE = "SIP SRV";

	
	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	final static String ACTION_CONNECTIVITY_CHANGED = "android.net.conn.CONNECTIVITY_CHANGE";

	public final static String STACK_FILE_NAME = "libpjsipjni.so";

	// Map active account id (id for sql settings database) with acc_id (id for
	// pjsip)
	public static HashMap<Integer, Integer> active_acc_map = new HashMap<Integer, Integer>();
	public static HashMap<Integer, Integer> status_acc_map = new HashMap<Integer, Integer>();

	// Implement public interface for the service
	private final ISipService.Stub mBinder = new ISipService.Stub() {
		@Override
		public void sipStart() throws RemoteException {
			SipService.this.sipStart();
		}

		@Override
		public void sipStop() throws RemoteException {
			SipService.this.sipStop();
		}

		@Override
		public void addAllAccounts() throws RemoteException {
			SipService.this.registerAllAccounts();

		}

		@Override
		public void removeAllAccounts() throws RemoteException {
			SipService.this.unregisterAllAccounts();

		}

		@Override
		public void switchToAutoAnswer() throws RemoteException {
			if (mUAReceiver != null) {
				mUAReceiver.setAutoAnswerNext(true);
			}
			// TODO: popup here
		}

		@Override
		public void makeCall(String callee) throws RemoteException {
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

		@Override
		public void forceStopService() throws RemoteException {
			Log.d(THIS_FILE,"Try to force service stop");
			stopSelf();
			
		}

		@Override
		public int answer(int callId, int status) throws RemoteException {
			if(created) {
				return pjsua.call_answer(callId, status, null, null);
			}
			return 0;
		}
		
		@Override
		public int hangup(int callId, int status) throws RemoteException {
			if(created) {
				return pjsua.call_hangup(callId, status, null, null);
			}
			return 0;
		}
	};

	private DBAdapter db;
	private WakeLock wakelock;
	private WifiLock wifilock;
	private UAStateReceiver mUAReceiver;
	private SharedPreferences prefs;
	private ConnectivityManager connManager;
	private boolean has_sip_stack = false;
	private boolean sip_stack_corrupted = false;
	private ServiceDeviceStateReceiver sd_receiver;
	
	 

	// Broadcast receiver for the service

	private class ServiceDeviceStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_CONNECTIVITY_CHANGED)) {
				Log.i(THIS_FILE, "+++ Connectivity has changed");
				Thread t = new Thread() {
					@Override
					public void run() {
						if (isValidConnectionForOutgoing() || isValidConnectionForIncoming()) {
							if (!created) {
								sipStart();
							} else {
								// update registration IP
								unregisterAllAccounts();
								registerAllAccounts();
							}
						} else {
							Log.i(THIS_FILE, "Stop SERVICE");
							SipService.this.unregisterReceiver(sd_receiver);
							SipService.this.sipStop();
							//OK, this will be done only if the last bind is released
							SipService.this.stopSelf();
							
						}
					}
				};
				
				t.start();
				Log.i(THIS_FILE, "--- Connectivity has changed");
			}
			
		}

	}

	@Override
	public void onCreate() {
		super.onCreate();
		connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		// TODO : check connectivity, else just finish itself

		Log.i(THIS_FILE, "Create SIP Service");
		db = new DBAdapter(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

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
		registerReceiver(sd_receiver = new ServiceDeviceStateReceiver(), intentfilter);

		tryToLoadStack();
		

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.i(THIS_FILE, "Destroying SIP Service");
		try {
			unregisterReceiver(sd_receiver);
		}catch(IllegalArgumentException e) {
			//This is the case if already unregistered itself
			//Python like usage of try ;)
		}
		sipStop();

		Log.i(THIS_FILE, "Destroyed SIP Service");
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Autostart the stack
		if(!has_sip_stack) {
			tryToLoadStack();
		}
		sipStart();
	}
	

	private void tryToLoadStack() {
		// TODO : autodetect version
		File stack_file = getStackLibFile(this);
		if(stack_file != null && !sip_stack_corrupted) {
			try {
				System.load(stack_file.getAbsolutePath());
				has_sip_stack = true;
			} catch (UnsatisfiedLinkError e) {
				Log.e(THIS_FILE, "We have a problem with the current stack.... NOT YET Implemented", e);
				has_sip_stack = false;
				sip_stack_corrupted = true;

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
		return mBinder;
	}

	private synchronized void sipStart() {
		if (!has_sip_stack) {
			Log.e(THIS_FILE, "We have no sip stack, we can't start");
			return;
		}
		
		if(!isValidConnectionForIncoming() && !isValidConnectionForOutgoing()) {
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "Not able to start sip stack");
			return;
		}

		Log.i(THIS_FILE, "Will start sip : " + (!created));
		if (!created && !creating) {
			creating = true;
			Thread thread = new Thread() {

				@Override
				public void run() {
					int status;

					status = pjsua.create();

					Log.i(THIS_FILE, "Created " + status);
					created = true;

					{
						pjsua_config cfg = new pjsua_config();
						pjsua_logging_config log_cfg = new pjsua_logging_config();
						pjsua_media_config media_cfg = new pjsua_media_config();

						// GLOBAL CONFIG
						pjsua.config_default(cfg);
						cfg.setCb(pjsuaConstants.WRAPPER_CALLBACK_STRUCT);
						pjsua.setCallbackObject(mUAReceiver = new UAStateReceiver());
						mUAReceiver.initService(SipService.this);

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
						
						// To avoid crash after hangup -- android 1.5 only but
						// even sometimes crash
						String default_value = "-1";
						if(Build.VERSION.SDK == "3") {
							default_value = "5";
						}
						String snd_auto_close_time = prefs.getString("snd_auto_close_time", default_value);
						
						media_cfg.setSnd_auto_close_time(Integer.parseInt(snd_auto_close_time));
						
						
						// Disable echo cancellation
						boolean echo_cancellation = prefs.getBoolean("echo_cancellation", true);
						if (!echo_cancellation) {
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
						cfg.setPort(5060);
						status = pjsua.transport_create(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, cfg, null);
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
		if (mUAReceiver != null) {
			mUAReceiver.forceDeleteNotifications();
		}

		if (created) {
			Log.d(THIS_FILE, "Detroying...");

			pjsua.destroy();
			status_acc_map.clear();
			active_acc_map.clear();
		}
		created = false;
	}
	
	/**
	 * Add accounts from database
	 */
	private synchronized void registerAllAccounts() {
		if(!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}
		

		
		boolean has_some_success = false;
		db.open();
		List<Account> acc_list = db.getListAccounts();
		db.close();
		for (Account acc : acc_list) {
			if (acc.active) {
				int status;
				if (active_acc_map.containsKey(acc.id)) {
					status = pjsua.acc_modify(active_acc_map.get(acc.id), acc.cfg);
					// if(status == pjsuaConstants.PJ_SUCCESS){
					//		
					// }else{
					// Log.w(THIS_FILE,
					// "Modify account "+acc.display_name+" failed !!! ");
					// active_acc_map.put(acc.id, active_acc_map.get(acc.id));
					// }
					pjsua.acc_set_registration(active_acc_map.get(acc.id), 1);
				} else {
					int[] acc_id = new int[1];
					status = pjsua.acc_add(acc.cfg, pjsuaConstants.PJ_TRUE, acc_id);
					status_acc_map.put(acc.id, status);

					if (status == pjsuaConstants.PJ_SUCCESS) {
						Log.i(THIS_FILE, "Account " + acc.display_name + " ( " + acc.id + " ) added as " + acc_id[0]);
						active_acc_map.put(acc.id, acc_id[0]);
						has_some_success = true;
					} else {
						Log.w(THIS_FILE, "Add account " + acc.display_name + " failed !!! ");

					}
				}
			}
		}
		
		if(has_some_success) {
			lockResources();
			
		}
	}

	/**
	 * Remove accounts from database
	 */
	private synchronized void unregisterAllAccounts() {
		if(!created) {
			Log.e(THIS_FILE, "PJSIP is not started here, nothing can be done");
			return;
		}
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		if(wifilock != null && wifilock.isHeld()) {
			wifilock.release();
		}
		
		
		for (int c_acc_id : active_acc_map.values()) {
			pjsua.acc_set_registration(c_acc_id, 0);
			pjsua.acc_del(c_acc_id);
		}
		status_acc_map.clear();
		active_acc_map.clear();
		if (mUAReceiver != null) {
			mUAReceiver.forceDeleteNotifications();
		}
	}
	
	public synchronized void lockResources() {
		//Add a wake lock
		PowerManager pman = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (wakelock == null) {
			wakelock = pman.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.SipService");
			wakelock.setReferenceCounted(false);
		}
		wakelock.acquire();
		
		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if(wifilock == null) {
			wifilock = wman.createWifiLock("com.csipsimple.SipService");
		}
		if(prefs.getBoolean("lock_wifi", true) ) {
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo != null) {
				DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
				//We assume that if obtaining ip addr, we are almost connected so can keep wifi lock
				if(dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
					wifilock.acquire();
				}
			}

		}
	}

	
	public void callAnswer(int c_id, int code) {
		if(!created) {
			return;
		}
		pjsua.call_answer(c_id, code, null, null);
	}


	private boolean isValidConnectionFor(String suffix) {

		// Check for gsm
		boolean valid_for_gsm = prefs.getBoolean("use_3g_" + suffix, false);
		NetworkInfo ni;
		if (valid_for_gsm) {
			ni = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}

		// Check for wifi
		boolean valid_for_wifi = prefs.getBoolean("use_wifi_" + suffix, true);
		if (valid_for_wifi) {
			ni = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (ni.getState() == NetworkInfo.State.CONNECTED) {
				return true;
			}
		}
		return false;
	}

	private boolean isValidConnectionForOutgoing() {
		return isValidConnectionFor("out");
	}

	private boolean isValidConnectionForIncoming() {
		return isValidConnectionFor("in");
	}

	public static File getStackLibFile(Context ctx) {
		// Standard case
		File standard_out = getGuessedStackLibFile(ctx);
		if (standard_out.exists()) {
			return standard_out;
		}

		// One target build
		// TODO : find a clean way to access the libPath for one shot builds
		File target_for_build = new File(ctx.getFilesDir().getParent(), "lib" + File.separator + "libpjsipjni.so");
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
		pj_str_t current_codec;
		current_codec = pjsua.pj_str_copy("speex/16000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_speex_16000", "130")));
		current_codec = pjsua.pj_str_copy("speex/8000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_speex_8000", "129")));
		current_codec = pjsua.pj_str_copy("speex/32000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_speex_32000", "128")));
		current_codec = pjsua.pj_str_copy("GSM/8000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_gsm_8000", "128")));
		current_codec = pjsua.pj_str_copy("PCMU/8000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_pcmu_8000", "128")));
		current_codec = pjsua.pj_str_copy("PCMA/8000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_pcma_8000", "128")));
		current_codec = pjsua.pj_str_copy("g722/8000/1");
		pjsua.codec_set_priority(current_codec, (short) Integer.parseInt(prefs.getString("codec_g722_8000", "128")));
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
