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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.ISipConfiguration;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.ui.InCallMediaControl;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class SipService extends Service {

	public static final String INTENT_SIP_CONFIGURATION = "com.csipsimple.service.SipConfiguration";
	public static final String INTENT_SIP_SERVICE = "com.csipsimple.service.SipService";
	public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "com.csipsimple.accounts.activate";
	
	// static boolean creating = false;
	private static final String THIS_FILE = "SIP SRV";

	
	private SipWakeLock sipWakeLock;

	// Implement public interface for the service
	private final ISipService.Stub binder = new ISipService.Stub() {
		/**
		 * Start the sip stack according to current settings (create pjsua)
		 */
		@Override
		public void sipStart() throws RemoteException {
			startSipStack();
		}

		/**
		 * Stop the sip stack (destroy pjsua)
		 */
		@Override
		public void sipStop() throws RemoteException {
			pjService.sipStop();
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
					pjService.sipStop();
					startSipStack();
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
		public SipProfileState getSipProfileState(int accountId) throws RemoteException {
			return SipService.this.getSipProfileState(accountId);
		}

		/**
		 * Switch in autoanswer mode
		 */
		@Override
		public void switchToAutoAnswer() throws RemoteException {
			if (pjService.userAgentReceiver != null) {
				pjService.userAgentReceiver.setAutoAnswerNext(true);
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
			//We have to ensure service is properly started and not just binded
			SipService.this.startService(new Intent(SipService.this, SipService.class));
			pjService.makeCall(callee, accountId);
		}
		

		/**
		 * Send SMS using
		 */
		@Override
		public void sendMessage(String message, String callee,int accountId)throws RemoteException {
			//We have to ensure service is properly started and not just binded
			SipService.this.startService(new Intent(SipService.this, SipService.class));
			

			Log.d(THIS_FILE, "will sms " + callee);
			
			ToCall called = pjService.sendMessage(callee, message, accountId);
			if(called!=null) {
				SipMessage msg = new SipMessage(SipMessage.SELF, 
						SipUri.getCanonicalSipContact(callee), SipUri.getCanonicalSipContact(called.getCallee()), 
						message, "text/plain", System.currentTimeMillis(), 
						SipMessage.MESSAGE_TYPE_QUEUED);
				msg.setRead(true);
				synchronized (db) {
					db.open();
					db.insertMessage(msg);
					db.close();	
				}
				Log.d(THIS_FILE, "Inserted "+msg.getTo());
			}else {
				SipService.this.notifyUserOfMessage( getString(R.string.invalid_sip_uri)+ " : "+callee );
			}
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
			return pjService.callAnswer(callId, status);
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
			return pjService.callHangup(callId, status);
		}
		

		@Override
		public int xfer(int callId, String callee) throws RemoteException {
			Log.d(THIS_FILE, "XFER");
			return pjService.callXfer(callId, callee);
		}

		@Override
		public int xferReplace(int callId, int otherCallId, int options) throws RemoteException {
			Log.d(THIS_FILE, "XFER-replace");
			return pjService.callXferReplace(callId, otherCallId, options);
		}

		@Override
		public int sendDtmf(int callId, int keyCode) throws RemoteException {
			return pjService.sendDtmf(callId, keyCode);
		}

		@Override
		public int hold(int callId) throws RemoteException {
			Log.d(THIS_FILE, "HOLDING");
			return pjService.callHold(callId);
		}

		@Override
		public int reinvite(int callId, boolean unhold) throws RemoteException {
			Log.d(THIS_FILE, "REINVITING");
			return pjService.callReinvite(callId, unhold);
		}
		

		@Override
		public SipCallSession getCallInfo(int callId) throws RemoteException {
			return pjService.getCallInfo(callId);
		}

		@Override
		public void setBluetoothOn(boolean on) throws RemoteException {
			pjService.setBluetoothOn(on);
			
		}

		@Override
		public void setMicrophoneMute(boolean on) throws RemoteException {
			pjService.setMicrophoneMute(on);
		}

		@Override
		public void setSpeakerphoneOn(boolean on) throws RemoteException {
			pjService.setSpeakerphoneOn(on);
		}


		@Override
		public SipCallSession[] getCalls() throws RemoteException {
			return pjService.getCalls();
		}

		@Override
		public void confAdjustTxLevel(int port, float value) throws RemoteException {
			pjService.confAdjustTxLevel(port, value);
		}

		@Override
		public void confAdjustRxLevel(int port, float value) throws RemoteException {
			pjService.confAdjustRxLevel(port, value);
			
		}
		
		@Override
		public void adjustVolume(SipCallSession callInfo, int direction, int flags) throws RemoteException {
			
			int state = callInfo.getCallState();
    		
    		boolean ringing = ( (state == SipCallSession.InvState.INCOMING) ||
    							(state == SipCallSession.InvState.EARLY) );
    		
        	// Mode ringing
    		if(ringing) {
	        	pjService.adjustStreamVolume(AudioManager.STREAM_RING, direction, AudioManager.FLAG_SHOW_UI);
    		}else {
	        	// Mode in call
	        	if(prefsWrapper.getPreferenceBooleanValue(PreferencesWrapper.USE_SOFT_VOLUME)) {
	        		Intent adjustVolumeIntent = new Intent(SipService.this, InCallMediaControl.class);
	        		adjustVolumeIntent.putExtra(Intent.EXTRA_KEY_EVENT, direction);
	        		startActivity(adjustVolumeIntent);
	        	}else {
	        		pjService.adjustStreamVolume(Compatibility.getInCallStream(), direction, flags);
	        	}
    		}
		}

		@Override
		public void setEchoCancellation(boolean on) throws RemoteException {
			pjService.setEchoCancellation(on);
		}

		@Override
		public void startRecording(int callId) throws RemoteException {
			pjService.startRecording(callId);
		}

		@Override
		public void stopRecording() throws RemoteException {
			pjService.stopRecording();
		}

		@Override
		public int getRecordedCall() throws RemoteException {
			return pjService.getRecordedCall();
		}

		@Override
		public boolean canRecord(int callId) throws RemoteException {
			return pjService.canRecord(callId);
		}


	};

	private final ISipConfiguration.Stub binderConfiguration = new ISipConfiguration.Stub() {

		@Override
		public long addOrUpdateAccount(SipProfile acc) throws RemoteException {
			Log.d(THIS_FILE, ">>> addOrUpdateAccount from service");
			long finalId = SipProfile.INVALID_ID;
			
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
			return finalId;
		}

		@Override
		public SipProfile getAccount(long accId) throws RemoteException {
			SipProfile result = null;

			synchronized (db) {
				db.open();
				result = db.getAccount(accId);
				db.close();
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
	private ServiceDeviceStateReceiver deviceStateReceiver;
	public PreferencesWrapper prefsWrapper;
	private ServicePhoneStateReceiver phoneConnectivityReceiver;
	private TelephonyManager telephonyManager;
	private ConnectivityManager connectivityManager;

	SipNotifications notificationManager;
	private MyExecutor mExecutor;
	public static PjSipService pjService;

	// Broadcast receiver for the service
	private class ServiceDeviceStateReceiver extends BroadcastReceiver {
		 private Timer mTimer = new Timer();
	        private MyTimerTask mTask;

	        @Override
	        public void onReceive(final Context context, final Intent intent) {
	            // Run the handler in MyExecutor to be protected by wake lock
	            getExecutor().execute(new Runnable() {
	                public void run() {
	                    onReceiveInternal(context, intent);
	                }
	            });
	        }

	        private void onReceiveInternal(Context context, Intent intent) {
	            String action = intent.getAction();
	            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	                Bundle b = intent.getExtras();
	                if (b != null) {
	                    NetworkInfo netInfo = (NetworkInfo)
	                            b.get(ConnectivityManager.EXTRA_NETWORK_INFO);
	                    String type = netInfo.getTypeName();
	                    NetworkInfo.State state = netInfo.getState();

	                    NetworkInfo activeNetInfo = getActiveNetworkInfo();
	                        if (activeNetInfo != null) {
                            Log.d(THIS_FILE, "active network: "
                                    + activeNetInfo.getTypeName()
                                    + ((activeNetInfo.getState() == NetworkInfo.State.CONNECTED)
                                            ? " CONNECTED" : " DISCONNECTED"));
                        } else {
                            Log.d(THIS_FILE, "active network: null");
                        }
	                    if ((state == NetworkInfo.State.CONNECTED)
	                            && (activeNetInfo != null)
	                            && (activeNetInfo.getType() != netInfo.getType())) {
	                        Log.d(THIS_FILE, "ignore connect event: " + type
	                                + ", active: " + activeNetInfo.getTypeName());
	                        return;
	                    }

	                    if (state == NetworkInfo.State.CONNECTED) {
	                        Log.d(THIS_FILE, "Connectivity alert: CONNECTED " + type);
	                        onChanged(type, true);
	                    } else if (state == NetworkInfo.State.DISCONNECTED) {
	                        Log.d(THIS_FILE, "Connectivity alert: DISCONNECTED " + type);
	                        onChanged(type, false);
	                    } else {
	                        Log.d(THIS_FILE, "Connectivity alert not processed: "
	                                + state + " " + type);
	                    }
	                }
	            }else if(action.equals(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED)) {
					final long accountId = intent.getLongExtra(SipManager.EXTRA_ACCOUNT_ID, -1);
					final boolean active = intent.getBooleanExtra(SipManager.EXTRA_ACTIVATE, false);
					//Should that be threaded?
					if(accountId != SipProfile.INVALID_ID) {
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

	        private NetworkInfo getActiveNetworkInfo() {
	            ConnectivityManager cm = (ConnectivityManager)
	                    SipService.this.getSystemService(Context.CONNECTIVITY_SERVICE);
	            return cm.getActiveNetworkInfo();
	        }

	        private void onChanged(String type, boolean connected) {
	            synchronized (SipService.this) {
	                // When turning on WIFI, it needs some time for network
	                // connectivity to get stabile so we defer good news (because
	                // we want to skip the interim ones) but deliver bad news
	                // immediately
	                if (connected) {
	                    if (mTask != null) {
	                    	mTask.cancel();
	                    }
	                    mTask = new MyTimerTask(type, connected);
	                    mTimer.schedule(mTask, 2 * 1000L);
	                    // hold wakup lock so that we can finish changes before the
	                    // device goes to sleep
	                    sipWakeLock.acquire(mTask);
	                } else {
	                    if ((mTask != null) && mTask.mNetworkType.equals(type)) {
	                        mTask.cancel();
	                        sipWakeLock.release(mTask);
	                    }
	                 //   onConnectivityChanged(type, false);
	                    dataConnectionChanged();
	                }
	            }
	        }

	        private class MyTimerTask extends TimerTask {
	            private boolean mConnected;
	            private String mNetworkType;

	            public MyTimerTask(String type, boolean connected) {
	                mNetworkType = type;
	                mConnected = connected;
	            }

	            // timeout handler
	            @Override
	            public void run() {
	                // delegate to mExecutor
	                getExecutor().execute(new Runnable() {
	                    public void run() {
	                        realRun();
	                    }
	                });
	            }

	            private void realRun() {
	                synchronized (SipService.this) {
	                    if (mTask != this) {
	                        Log.w(THIS_FILE, "  unexpected task: " + mNetworkType
	                                + (mConnected ? " CONNECTED" : "DISCONNECTED"));
	                        return;
	                    }
	                    mTask = null;
	                    Log.d(THIS_FILE, " deliver change for " + mNetworkType
	                            + (mConnected ? " CONNECTED" : "DISCONNECTED"));
	                  //  onConnectivityChanged(mNetworkType, mConnected);
	                    dataConnectionChanged();
	                    sipWakeLock.release(this);
	                }
	            }
	        }
		
		
		
		/*
		
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
			}else if(action.equals(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED)) {
				final long accountId = intent.getLongExtra(SipManager.EXTRA_ACCOUNT_ID, -1);
				final boolean active = intent.getBooleanExtra(SipManager.EXTRA_ACTIVATE, false);
				//Should that be threaded?
				if(accountId != SipProfile.INVALID_ID) {
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
			
			
		}*/
	}
	
	

    private MyExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
        	mExecutor = new MyExecutor();
        }
        return mExecutor;
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

			pjService.onGSMStateChanged(state, incomingNumber);

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
		sipWakeLock = new SipWakeLock((PowerManager) getSystemService(Context.POWER_SERVICE));
		
		
		if(!prefsWrapper.hasAlreadySetupService()) {
			prefsWrapper.resetAllDefaultValues();
			prefsWrapper.setPreferenceBooleanValue(PreferencesWrapper.HAS_ALREADY_SETUP_SERVICE, true);
		}
		
		// Check connectivity, else just finish itself
		if (!prefsWrapper.isValidConnectionForOutgoing() && !prefsWrapper.isValidConnectionForIncoming()) {
			Log.d(THIS_FILE, "Harakiri... we are not needed since no way to use self");
			stopSelf();
			return;
		}
		

		if(pjService == null) {
			pjService = new PjSipService(this);
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
		pjService.sipStop();
		notificationManager.cancelAll();
		Log.i(THIS_FILE, "--- SIP SERVICE DESTROYED ---");

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Autostart the stack
		if (loadAndConnectStack()) {
			Thread t = new Thread() {
				public void run() {
					Log.d(THIS_FILE, "Start sip stack because start asked");
					startSipStack();
				}
			};
			t.start();
		}
	}

	private boolean loadAndConnectStack() {

		if (pjService.tryToLoadStack()) {
			// Register own broadcast receiver
			if (deviceStateReceiver == null) {
				IntentFilter intentfilter = new IntentFilter();
				intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				intentfilter.addAction(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
				deviceStateReceiver = new ServiceDeviceStateReceiver();
				registerReceiver(deviceStateReceiver, intentfilter);
			}
			if (phoneConnectivityReceiver == null) {
				Log.d(THIS_FILE, "Listen for phone state ");
				phoneConnectivityReceiver = new ServicePhoneStateReceiver();
				telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
						| PhoneStateListener.LISTEN_CALL_STATE);
			}
			return true;
		}
		return false;
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

	public void startSipStack() {
		if(!needToStartSip()) {
			ToastHandler.sendMessage(ToastHandler.obtainMessage(0, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "Not able to start sip stack");
			return;
		}
		if(pjService.sipStart()) {
			addAllAccounts();
			updateRegistrationsState();
		}
	}
	
	public void stopSipStack() {
		pjService.sipStop();
		releaseResources();
	}
	
	
	
	public boolean needToStartSip() {
		//The connection is valid?
		return prefsWrapper.isValidConnectionForIncoming() || prefsWrapper.isValidConnectionForOutgoing();
	}

	
	public void notifyUserOfMessage(String msg) {
		ToastHandler.sendMessage(ToastHandler.obtainMessage(0, msg));
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
				if (pjService.addAccount(account) ) {
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

	

	private boolean setAccountRegistration(SipProfile account, int renew) {
		boolean status = pjService.setAccountRegistration(account, renew);
		
		// Send a broadcast message that for an account
		// registration state has changed
		Intent regStateChangedIntent = new Intent(SipManager.ACTION_SIP_REGISTRATION_CHANGED);
		sendBroadcast(regStateChangedIntent);

		updateRegistrationsState();
		
		
		return status;
	}

	/**
	 * Remove accounts from database
	 */
	private void unregisterAllAccounts(boolean cancelNotification) {

		releaseResources();
		
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


		if (notificationManager != null && cancelNotification) {
			notificationManager.cancelRegisters();
		}
	}

	private void reAddAllAccounts() {
		Log.d(THIS_FILE, "RE REGISTER ALL ACCOUNTS");
		unregisterAllAccounts(false);
		addAllAccounts();
	}


	
	
	public SipProfileState getSipProfileState(int accountDbId) {
		SipProfile account;
		synchronized (db) {
			db.open();
			account = db.getAccount(accountDbId);
			db.close();
		}
		return pjService.getAccountInfo(account);
	}

	public void updateRegistrationsState() {
		ArrayList<SipProfileState> activeAccountsInfos = pjService.getAndUpdateActiveAccounts();

		// Handle status bar notification
		if (activeAccountsInfos.size() > 0 && prefsWrapper.showIconInStatusBar()) {
			notificationManager.notifyRegisteredAccounts(activeAccountsInfos);
			acquireResources();
		} else {
			notificationManager.cancelRegisters();
			releaseResources();
		}
	}
	
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
		pjService.setAudioInCall();
	}

	/**
	 * Method called by the native sip stack to unset audio mode when track and
	 * recorder are stopped
	 */
	public static void unsetAudioInCall() {
		Log.i(THIS_FILE, "Audio driver ask to unset in call");
		pjService.unsetAudioInCall();
	}
	
	
	
	public static UAStateReceiver getUAStateReceiver() {
		return pjService.userAgentReceiver;
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
			if (!pjService.isCreated()) {
				// we was not yet started, so start now
				Thread t = new Thread() {
					public void run() {
						startSipStack();
					}
				};
				t.start();
			} else if (ipHasChanged) {
				// Check if IP has changed between
				/*
				 * Log.i(THIS_FILE, "Ip changed remove/re - add all accounts");
				 * reRegisterAllAccounts(); if(created) { pjsua.med }
				 */
				if (pjService.getActiveCallInProgress() == null) {
					Thread t = new Thread() {
						public void run() {
							stopSipStack();
							startSipStack();
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
			if (pjService.getActiveCallInProgress() != null) {
				Log.w(THIS_FILE, "There is an ongoing call ! don't stop !! and wait for network to be back...");
				return;
			}
			Log.d(THIS_FILE, "Will stop SERVICE");
			Thread t = new Thread() {
				public void run() {
					Log.i(THIS_FILE, "Stop SERVICE");
					pjService.sipStop();
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

	public static final class ToCall {
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
	
	public SipProfile getAccount(int accountId) {
		synchronized (db) {
			return SipService.getAccount(accountId, db);
		}
	}
	
	public static SipProfile getAccount(int accountId, DBAdapter db) {
		db.open();
		SipProfile account = db.getAccount(accountId);
		db.close();
		return account;
	}
	
	

    private static Looper createLooper() {
        HandlerThread thread = new HandlerThread("SipService.Executor");
        thread.start();
        return thread.getLooper();
    }

    // Executes immediate tasks in a single thread.
    // Hold/release wake lock for running tasks
    private class MyExecutor extends Handler {
        MyExecutor() {
            super(createLooper());
        }

        void execute(Runnable task) {
            sipWakeLock.acquire(task);
            Message.obtain(this, 0/* don't care */, task).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof Runnable) {
                executeInternal((Runnable) msg.obj);
            } else {
                Log.w(THIS_FILE, "can't handle msg: " + msg);
            }
        }

        private void executeInternal(Runnable task) {
            try {
                task.run();
            } catch (Throwable t) {
                Log.e(THIS_FILE, "run task: " + task, t);
            } finally {
                sipWakeLock.release(task);
            }
        }
    }
	
	

}
