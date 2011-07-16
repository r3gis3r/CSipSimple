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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.SystemClock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.csipsimple.R;
import com.csipsimple.api.ISipConfiguration;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipUri;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.pjsip.PjSipService;
import com.csipsimple.pjsip.UAStateReceiver;
import com.csipsimple.ui.InCallMediaControl;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class SipService extends Service {

	
	// static boolean creating = false;
	private static final String THIS_FILE = "SIP SRV";

	// Comes from android.net.vpn.VpnManager.java
	// Action for broadcasting a connectivity state.
    private static final String ACTION_VPN_CONNECTIVITY = "vpn.connectivity";
    /** Key to the connectivity state of a connectivity broadcast event. */
    private static final String BROADCAST_CONNECTION_STATE = "connection_state";
    private boolean lastKnownVpnState = false;
	
	private SipWakeLock sipWakeLock;
	private boolean autoAcceptCurrent = false;
	public boolean supportMultipleCalls = false;
	

	// Implement public interface for the service
	private final ISipService.Stub binder = new ISipService.Stub() {
		/**
		 * Start the sip stack according to current settings (create the stack)
		 */
		@Override
		public void sipStart() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Start required from third party app/serv");
			getExecutor().execute(new StartRunnable());
		}

		/**
		 * Stop the sip stack (destroy the stack)
		 */
		@Override
		public void sipStop() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new StopRunnable());
		}

	
		/**
		 * Force the stop of the service
		 */
		@Override
		public void forceStopService() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Try to force service stop");
			cleanStop();
			//stopSelf();
		}

		/**
		 * Restart the service (threaded)
		 */
		@Override
		public void askThreadedRestart() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Restart required from third part app/serv");
			getExecutor().execute(new RestartRunnable());
		};

		/**
		 * Populate pjsip accounts with accounts saved in sqlite
		 */
		@Override
		public void addAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.addAllAccounts();
				}
			});
		}

		/**
		 * Unregister and delete accounts registered
		 */
		@Override
		public void removeAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.unregisterAllAccounts(true);
				}
			});
		}

		/**
		 * Reload all accounts with values found in database
		 */
		@Override
		public void reAddAllAccounts() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.reAddAllAccounts();
					
				}
			});
		}

		@Override
		public void setAccountRegistration(int accountId, int renew) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			SipProfile account;
			synchronized (db) {
				db.open();
				account = db.getAccount(accountId);
				db.close();
			}
			final SipProfile acc = account;
			final int ren = renew;
			getExecutor().execute(new SipRunnable() {
				@Override
				public void doRun() throws SameThreadException {
					SipService.this.setAccountRegistration(acc, ren);
				}
			});
		}

		/**
		 * Get account and it's informations
		 * 
		 * @param accountId
		 *            the id (sqlite id) of the account
		 */
		@Override
		public SipProfileState getSipProfileState(int accountId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			return SipService.this.getSipProfileState(accountId);
		}

		/**
		 * Switch in autoanswer mode
		 */
		@Override
		public void switchToAutoAnswer() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "Switch to auto answer");
			setAutoAnswerNext(true);
		}

		/**
		 * Make a call
		 * 
		 * @param callee
		 *            remote contact ot call If not well formated we try to add
		 *            domain name of the default account
		 */
		@Override
		public void makeCall(final String callee, final int accountId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			//We have to ensure service is properly started and not just binded
			SipService.this.startService(new Intent(SipService.this, SipService.class));
			
			if(!supportMultipleCalls && pjService != null) {
				// Check if there is no ongoing calls if so drop this request by alerting user
				SipCallSession activeCall = pjService.getActiveCallInProgress();
				if(activeCall != null) {
					if(!CustomDistribution.forceNoMultipleCalls()) {
						serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, R.string.not_configured_multiple_calls, 0));
					}
					return;
				}
			}
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.makeCall(callee, accountId);
				}
			});
			
		}
		

		/**
		 * Send SMS using
		 */
		@Override
		public void sendMessage(final String message, final String callee, final int accountId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			//We have to ensure service is properly started and not just binded
			SipService.this.startService(new Intent(SipService.this, SipService.class));
			
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					Log.d(THIS_FILE, "will sms " + callee);
					ToCall called = pjService.sendMessage(callee, message, accountId);
					if(called!=null) {
						SipMessage msg = new SipMessage(SipMessage.SELF, 
								SipUri.getCanonicalSipContact(callee), SipUri.getCanonicalSipContact(called.getCallee()), 
								message, "text/plain", System.currentTimeMillis(), 
								SipMessage.MESSAGE_TYPE_QUEUED, called.getCallee());
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
			});
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
		public int answer(final int callId, final int status) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					 pjService.callAnswer(callId, status);
				}
			});
			return 0;
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
		public int hangup(final int callId, final int status) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.callHangup(callId, status);
				}
			});
			return 0;
		}
		

		@Override
		public int xfer(final int callId, final String callee) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "XFER");
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.callXfer(callId, callee);
				}
			});
			return 0;
		}

		@Override
		public int xferReplace(final int callId, final int otherCallId, final int options) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "XFER-replace");
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.callXferReplace(callId, otherCallId, options);
				}
			});
			return 0; 
		}

		@Override
		public int sendDtmf(final int callId, final int keyCode) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);

			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.sendDtmf(callId, keyCode);
				}
			});
			return 0;
		}

		@Override
		public int hold(final int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "HOLDING");
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.callHold(callId);
				}
			});
			return 0;
		}

		@Override
		public int reinvite(final int callId, final boolean unhold) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			Log.d(THIS_FILE, "REINVITING");
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.callReinvite(callId, unhold);
				}
			});
			return 0;
		}
		

		@Override
		public SipCallSession getCallInfo(final int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			return pjService.getCallInfo(callId);
		}

		@Override
		public void setBluetoothOn(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setBluetoothOn(on);
				}
			});
			
		}

		@Override
		public void setMicrophoneMute(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setMicrophoneMute(on);
				}
			});
		}

		@Override
		public void setSpeakerphoneOn(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setSpeakerphoneOn(on);
				}
			});
		}


		@Override
		public SipCallSession[] getCalls() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService != null) {
				return pjService.getCalls();
			}
			return new SipCallSession[0];
		}

		@Override
		public void confAdjustTxLevel(final int port, final float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					if(pjService == null) {
		    			return;
		    		}
					pjService.confAdjustTxLevel(port, value);
				}
			});
		}

		@Override
		public void confAdjustRxLevel(final int port, final float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					if(pjService == null) {
		    			return;
		    		}
					pjService.confAdjustRxLevel(port, value);
				}
			});
			
		}
		
		@Override
		public void adjustVolume(SipCallSession callInfo, int direction, int flags) throws RemoteException {

			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			
			if(pjService == null) {
    			return;
    		}
			
    		boolean ringing = callInfo.isIncoming() && callInfo.isBeforeConfirmed();
        	// Mode ringing
    		if(ringing) {
	        	// What is expected here is to silence ringer
    			//pjService.adjustStreamVolume(AudioManager.STREAM_RING, direction, AudioManager.FLAG_SHOW_UI);
    			pjService.silenceRinger();
    		}else {
	        	// Mode in call
	        	if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_SOFT_VOLUME)) {
	        		Intent adjustVolumeIntent = new Intent(SipService.this, InCallMediaControl.class);
	        		adjustVolumeIntent.putExtra(Intent.EXTRA_KEY_EVENT, direction);
	        		adjustVolumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        		startActivity(adjustVolumeIntent);
	        	}else {
	        		pjService.adjustStreamVolume(Compatibility.getInCallStream(), direction, flags);
	        	}
    		}
		}

		@Override
		public void setEchoCancellation(final boolean on) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.setEchoCancellation(on);
				}
			});
		}

		@Override
		public void startRecording(int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			pjService.startRecording(callId);
		}

		@Override
		public void stopRecording() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			pjService.stopRecording();
		}

		@Override
		public int getRecordedCall() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService == null) {
				return -1;
			}
			return pjService.getRecordedCall();
		}

		@Override
		public boolean canRecord(int callId) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			return pjService.canRecord(callId);
		}

		@Override
		public void zrtpSASVerified() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			getExecutor().execute(new SipRunnable() {
				@Override
				protected void doRun() throws SameThreadException {
					pjService.zrtpSASVerified();
				}
			});
		}

		@Override
		public MediaState getCurrentMediaState() throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			MediaState ms = new MediaState();
			if(pjService != null && pjService.mediaManager != null) {
				ms = pjService.mediaManager.getMediaState();
			}
			return ms;
		}

		@Override
		public void playWaveFile(String filePath, int callId, int way) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_USE_SIP, null);
			if(pjService == null) {
				return;
			}
			pjService.playWaveFile(filePath, callId, way);
		}
		
		@Override
		public int getVersion() throws RemoteException {
			return SipManager.CURRENT_API;
		}

		
	};

	private final ISipConfiguration.Stub binderConfiguration = new ISipConfiguration.Stub() {

		@Override
		public long addOrUpdateAccount(SipProfile acc) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
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
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
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
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceBooleanValue(key, value);
		}

		@Override
		public void setPreferenceFloat(String key, float value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceFloatValue(key, value);

		}

		@Override
		public void setPreferenceString(String key, String value) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			prefsWrapper.setPreferenceStringValue(key, value);

		}

		@Override
		public String getPreferenceString(String key) throws RemoteException {
			return prefsWrapper.getPreferenceStringValue(key);
			
		}

		@Override
		public boolean getPreferenceBoolean(String key) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
			return prefsWrapper.getPreferenceBooleanValue(key);
			
		}

		@Override
		public float getPreferenceFloat(String key) throws RemoteException {
			SipService.this.enforceCallingOrSelfPermission(SipManager.PERMISSION_CONFIGURE_SIP, null);
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

	public SipNotifications notificationManager;
	private SipServiceExecutor mExecutor;
	private static PjSipService pjService;
	private static HandlerThread executorThread;

	// Broadcast receiver for the service
	private class ServiceDeviceStateReceiver extends BroadcastReceiver {
		private Timer mTimer = null;
		private MyTimerTask mTask;
		private Object createLock = new Object();

		@Override
		public void onReceive(final Context context, final Intent intent) {
			// Run the handler in SipServiceExecutor to be protected by wake lock
			getExecutor().execute(new Runnable() {
				public void run() {
					onReceiveInternal(context, intent);
				}
			});
		}
		
		public void stop() {
			synchronized (createLock) {
				if (mTask != null) {
					Log.d(THIS_FILE, "Delete already pushed task in stack");
					mTask.cancel();
					sipWakeLock.release(mTask);
				}
				
				if(mTimer != null) {
					mTimer.purge();
					mTimer.cancel();
				}
				mTimer = null;
			}
		}

		private void onReceiveInternal(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				Bundle b = intent.getExtras();
				if (b != null) {
					NetworkInfo netInfo = (NetworkInfo) b.get(ConnectivityManager.EXTRA_NETWORK_INFO);
					String type = netInfo.getTypeName();
					NetworkInfo.State state = netInfo.getState();

					NetworkInfo activeNetInfo = getActiveNetworkInfo();
					
					/*
					if (activeNetInfo != null) {
						Log.d(THIS_FILE, "active network: " + activeNetInfo.getTypeName()
								+ ((activeNetInfo.getState() == NetworkInfo.State.CONNECTED) ? " CONNECTED" : " DISCONNECTED"));
					} else {
						Log.d(THIS_FILE, "active network: null");
					}
					*/
					
					if ((state == NetworkInfo.State.CONNECTED) && (activeNetInfo != null) && (activeNetInfo.getType() != netInfo.getType())) {
						Log.d(THIS_FILE, "ignore connect event: " + type + ", active: " + activeNetInfo.getTypeName());
						return;
					}

					if (state == NetworkInfo.State.CONNECTED) {
						Log.d(THIS_FILE, "Connectivity alert: CONNECTED " + type);
						// Fire the event only if valid for incoming else ignore this event
						if(prefsWrapper.isValidConnectionForIncoming() && !prefsWrapper.hasBeenQuit()) {
							onChanged(type, true);
						}
					} else if (state == NetworkInfo.State.DISCONNECTED) {
						Log.d(THIS_FILE, "Connectivity alert: DISCONNECTED " + type);
						onChanged(type, false);
					} else {
						Log.d(THIS_FILE, "Connectivity alert not processed: " + state + " " + type);
					}
				}
			} else if (action.equals(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED)) {
				final long accountId = intent.getLongExtra(SipManager.EXTRA_ACCOUNT_ID, -1);
				final boolean active = intent.getBooleanExtra(SipManager.EXTRA_ACTIVATE, false);
				// Should that be threaded?
				if (accountId != SipProfile.INVALID_ID) {
					final SipProfile account = getAccount((int)accountId);
					if (account != null) {
						getExecutor().execute(new SipRunnable() {
							@Override
							protected void doRun() throws SameThreadException {
								setAccountRegistration(account, active ? 1 : 0);
							}
						});
					}
				}
			} else if (action.equals(SipManager.ACTION_SIP_CAN_BE_STOPPED)) {
				if(mTask != null) {
					Log.d(THIS_FILE, "Cancel current force registration task cause stop asked");
					mTask.cancel();
					sipWakeLock.release(mTask);
				}
				cleanStop();
			} else if(action.equals(ACTION_VPN_CONNECTIVITY)) {
				// TODO : ensure no current call
				String connection_state = intent.getSerializableExtra(BROADCAST_CONNECTION_STATE).toString();
				boolean currentVpnState = connection_state.equalsIgnoreCase("CONNECTED");
				if(lastKnownVpnState != currentVpnState) {
					getExecutor().execute(new RestartRunnable());
					lastKnownVpnState = currentVpnState;
				}
			}
		}

		private NetworkInfo getActiveNetworkInfo() {
			ConnectivityManager cm = (ConnectivityManager) SipService.this.getSystemService(Context.CONNECTIVITY_SERVICE);
			return cm.getActiveNetworkInfo();
		}

		protected void onChanged(String type, boolean connected) {
			boolean fireChanges = false;
			synchronized (createLock) {
				// When turning on WIFI, it needs some time for network
				// connectivity to get stabile so we defer good news (because
				// we want to skip the interim ones) but deliver bad news
				// immediately
				if (connected) {
					Log.d(THIS_FILE, "Push a task to connected timer");
					if (mTask != null) {
						Log.d(THIS_FILE, "We already have a current task in stack");
						mTask.cancel();
						sipWakeLock.release(mTask);
					}
					mTask = new MyTimerTask(type, connected);
					if(mTimer == null) {
						mTimer = new Timer("Connected-timer");
					}
					mTimer.schedule(mTask, 2 * 1000L);
					// hold wakup lock so that we can finish changes before the
					// device goes to sleep
					sipWakeLock.acquire(mTask);
				} else {
					// If it's about the same connection type cancel the pending connection
					if ((mTask != null) && mTask.mNetworkType.equals(type)) {
						mTask.cancel();
						sipWakeLock.release(mTask);
					}
					fireChanges = true;
				}
			}
			if(fireChanges) {
				Log.d(THIS_FILE, "Fire changes right now cause it's a deconnect info");
				dataConnectionChanged(type, false);
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
				Log.d(THIS_FILE, "Run connected timeout");
				// delegate to mExecutor
				getExecutor().execute(new Runnable() {
					public void run() {
						realRun();
					}
				});
			}

			private void realRun() {
				synchronized (createLock) {
					if (mTask != this) {
						Log.w(THIS_FILE, "  unexpected task: " + mNetworkType + (mConnected ? " CONNECTED" : "DISCONNECTED"));
						sipWakeLock.release(this);
						return;
					}
					mTask = null;
					Log.d(THIS_FILE, " deliver change for " + mNetworkType + (mConnected ? " CONNECTED" : "DISCONNECTED"));
					// onConnectivityChanged(mNetworkType, mConnected);
					dataConnectionChanged(mNetworkType, true);
					sipWakeLock.release(this);
				}
			}
		}

	}
	
	

    public SipServiceExecutor getExecutor() {
        // create mExecutor lazily
        if (mExecutor == null) {
        	mExecutor = new SipServiceExecutor();
        }
        return mExecutor;
    }

	private class ServicePhoneStateReceiver extends PhoneStateListener {
		
		private boolean ignoreFirstConnectionState = true;
		private boolean ignoreFirstCallState = true;
		
		@Override
		public void onDataConnectionStateChanged(final int state) {
			if(!ignoreFirstConnectionState) {
				Log.d(THIS_FILE, "Data connection state changed : " + state);
				Thread t = new Thread("DataConnectionDetach") {
					@Override
					public void run() {
						if(deviceStateReceiver != null) {
							deviceStateReceiver.onChanged("MOBILE", state == TelephonyManager.DATA_CONNECTED);
						}
					}
				};
				t.start();
			}else {
				ignoreFirstConnectionState = false;
			}
			super.onDataConnectionStateChanged(state);
		}

		@Override
		public void onCallStateChanged(final int state, final String incomingNumber) {
			if(!ignoreFirstCallState) {
				Log.d(THIS_FILE, "Call state has changed !" + state + " : " + incomingNumber);
				getExecutor().execute(new SipRunnable() {
					
					@Override
					protected void doRun() throws SameThreadException {
						if(pjService != null) {
							pjService.onGSMStateChanged(state, incomingNumber);
						}
					}
				});
			}else {
				ignoreFirstCallState = false;
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
		Log.setLogLevel(prefsWrapper.getLogLevel());
		
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
			cleanStop();
			return;
		}

		//registerBroadcasts();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(THIS_FILE, "Destroying SIP Service");
		unregisterBroadcasts();
		
		getExecutor().execute(new FinalizeDestroyRunnable());
	}
	
	public void cleanStop () {
		getExecutor().execute(new DestroyRunnable());
	}
	
	private void registerBroadcasts() {
		// Register own broadcast receiver
		if (deviceStateReceiver == null) {
			IntentFilter intentfilter = new IntentFilter();
			intentfilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			intentfilter.addAction(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
			intentfilter.addAction(SipManager.ACTION_SIP_CAN_BE_STOPPED);
			intentfilter.addAction(ACTION_VPN_CONNECTIVITY);
			deviceStateReceiver = new ServiceDeviceStateReceiver();
			registerReceiver(deviceStateReceiver, intentfilter);
		}
		if (phoneConnectivityReceiver == null) {
			Log.d(THIS_FILE, "Listen for phone state ");
			phoneConnectivityReceiver = new ServicePhoneStateReceiver();
			
			telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
					| PhoneStateListener.LISTEN_CALL_STATE );
		}
	}

	private void unregisterBroadcasts() {
		if(deviceStateReceiver != null) {
			try {
				Log.d(THIS_FILE, "Stop and unregister device receiver");
				deviceStateReceiver.stop();
				unregisterReceiver(deviceStateReceiver);
				deviceStateReceiver = null;
			} catch (IllegalArgumentException e) {
				// This is the case if already unregistered itself
				// Python style usage of try ;) : nothing to do here since it could
				// be a standard case
				// And in this case nothing has to be done
				Log.d(THIS_FILE, "Has not to unregister telephony receiver");
			}
		}
		if (phoneConnectivityReceiver != null) {
			Log.d(THIS_FILE, "Unregister telephony receiver");
			telephonyManager.listen(phoneConnectivityReceiver, PhoneStateListener.LISTEN_NONE);
			phoneConnectivityReceiver = null;
		}
	}
	
	
	public static final String EXTRA_DIRECT_CONNECT = "direct_connect";
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		// Check connectivity, else just finish itself
		if (!prefsWrapper.isValidConnectionForOutgoing() && !prefsWrapper.isValidConnectionForIncoming()) {
			Log.d(THIS_FILE, "Harakiri... we are not needed since no way to use self");
			cleanStop();
			return;
		}
		
		
		boolean directConnect = true;
		if(intent != null) {
			directConnect = intent.getBooleanExtra(EXTRA_DIRECT_CONNECT, true);
		}
		// Autostart the stack
		// NOTE : the stack may also be autostarted cause of phoneConnectivityReceiver
		if(pjService == null) {
			if (!loadStack()) {
				return;
			}
		}
		
		if(directConnect) {
			Log.d(THIS_FILE, "Direct sip start");
			getExecutor().execute(new StartRunnable());
		}else {
			Log.d(THIS_FILE, "Defered SIP start !!");
			NetworkInfo netInfo = (NetworkInfo) connectivityManager.getActiveNetworkInfo();
			if(netInfo != null) {
				String type = netInfo.getTypeName();
				NetworkInfo.State state = netInfo.getState();
				if(state == NetworkInfo.State.CONNECTED) {
					Log.d(THIS_FILE, ">> on changed connected");
					deviceStateReceiver.onChanged(type, true);
				}else if(state == NetworkInfo.State.DISCONNECTED) {
					Log.d(THIS_FILE, ">> on changed disconnected");
					deviceStateReceiver.onChanged(type, false);
				}
			}else {
				deviceStateReceiver.onChanged(null, false);
				Log.d(THIS_FILE, ">> on changed disconnected");
			}
		}
	}

	private boolean loadStack() {
		//Ensure pjService exists
		if(pjService == null) {
			pjService = new PjSipService();
		}
		pjService.setService(this);
		
		if (pjService.tryToLoadStack()) {
			registerBroadcasts();
			return true;
		}
		return false;
	}


	@Override
	public IBinder onBind(Intent intent) {

		String serviceName = intent.getAction();
		Log.d(THIS_FILE, "Action is " + serviceName);
		if (serviceName == null || serviceName.equalsIgnoreCase(SipManager.INTENT_SIP_SERVICE)) {
			Log.d(THIS_FILE, "Service returned");
			return binder;
		} else if (serviceName.equalsIgnoreCase(SipManager.INTENT_SIP_CONFIGURATION)) {
			Log.d(THIS_FILE, "Conf returned");
			return binderConfiguration;
		}
		Log.d(THIS_FILE, "Default service (SipService) returned");
		return binder;
	}

	private KeepAliveTimer kaAlarm;
	// This is always done in SipExecutor thread
	private void startSipStack() throws SameThreadException {
		//Cache some prefs
		supportMultipleCalls = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS);
		
		if(!needToStartSip()) {
			serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, R.string.connection_not_valid, 0));
			Log.e(THIS_FILE, "No need to start sip");
			return;
		}
		Log.d(THIS_FILE, "Start was asked and we should actually start now");
		if(pjService == null) {
			Log.d(THIS_FILE, "Start was asked and pjService in not there");
			if(!loadStack()) {
				Log.e(THIS_FILE, "Unable to load SIP stack !! ");
				return;
			}
		}
		Log.d(THIS_FILE, "Ask pjservice to start itself");
		if(pjService.sipStart()) {
			Log.d(THIS_FILE, "Add all accounts");
			addAllAccounts();
		}
	}
	
	/**
	 * Safe stop the sip stack
	 * @return true if can be stopped, false if there is a pending call and the sip service should not be stopped
	 */
	private boolean stopSipStack() throws SameThreadException {
		Log.d(THIS_FILE, "Stop sip stack");
		sipWakeLock.acquire(this);
		boolean canStop = true;
		if(pjService != null) {
			canStop &= pjService.sipStop();
			if(canStop) {
				pjService = null;
			}
		}
		if(canStop) {
			releaseResources();
		}
		unregisterBroadcasts();
		sipWakeLock.release(this);
		return canStop;
	}
	
	
	
	public boolean needToStartSip() {
		//The connection is valid?
		return prefsWrapper.isValidConnectionForIncoming() || prefsWrapper.isValidConnectionForOutgoing();
	}

	
	public void notifyUserOfMessage(String msg) {
		serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, msg));
	}
	
	/**
	 * Add accounts from database
	 */
	private void addAllAccounts() throws SameThreadException {
		Log.d(THIS_FILE, "We are adding all accounts right now....");

		boolean hasSomeSuccess = false;
		List<SipProfile> accountList;
		synchronized (db) {
			db.open();
			accountList = db.getListAccounts();
			db.close();
		}
		int account_limit = 10;
		for (SipProfile account : accountList) {
			if (account.active && account_limit > 0) {
				if (pjService != null && pjService.addAccount(account) ) {
					hasSomeSuccess = true;
				}
				account_limit --;
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

	

	private boolean setAccountRegistration(SipProfile account, int renew) throws SameThreadException {
		boolean status = false;
		if(pjService != null) {
			 pjService.setAccountRegistration(account, renew);
			// Send a broadcast message that for an account
			// registration state has changed
			Intent regStateChangedIntent = new Intent(SipManager.ACTION_SIP_REGISTRATION_CHANGED);
			sendBroadcast(regStateChangedIntent);
	
			updateRegistrationsState();
		}		
		
		return status;
	}

	/**
	 * Remove accounts from database
	 */
	private void unregisterAllAccounts(boolean cancelNotification) throws SameThreadException {

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

	private void reAddAllAccounts() throws SameThreadException {
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
		if(pjService != null) {
			return pjService.getProfileState(account);
		}
		return null;
	}

	public void updateRegistrationsState() {
		ArrayList<SipProfileState> activeAccountsInfos = null;
		Log.d(THIS_FILE, "Update registration state");
		if(pjService != null) {
			activeAccountsInfos = pjService.getActiveProfilesState();
		}

		// Handle status bar notification
		if (activeAccountsInfos != null && activeAccountsInfos.size() > 0 && prefsWrapper.showIconInStatusBar()) {
		// Testing memory / CPU leak as per issue 676
		//	for(int i=0; i < 10; i++) {
		//		Log.d(THIS_FILE, "Notify ...");
				notificationManager.notifyRegisteredAccounts(activeAccountsInfos, prefsWrapper.getPreferenceBooleanValue(SipConfigManager.ICON_IN_STATUS_BAR_NBR));
		//		try {
		//			Thread.sleep(6000);
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
		//	}
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
	
	//Binders for media manager to sip stack
	/**
	 * Adjust tx software sound level
	 * @param speakVolume volume 0.0 - 1.0
	 */
	public void confAdjustTxLevel(float speakVolume) throws SameThreadException {
		if(pjService != null) {
			pjService.confAdjustTxLevel(0, speakVolume);
		}
	}
	/**
	 * Adjust rx software sound level
	 * @param speakVolume volume 0.0 - 1.0
	 */
	public void confAdjustRxLevel(float speakVolume) throws SameThreadException {
		if(pjService != null) {
			pjService.confAdjustRxLevel(0, speakVolume);
		}
	}

	
	private boolean hold_resources = false;
	/**
	 * Ask to take the control of the wifi and the partial wake lock if
	 * configured
	 */
	private synchronized void acquireResources() {
		if(hold_resources) {
			return;
		}
		
		// Add a wake lock for CPU if necessary
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

		// Add a lock for WIFI if necessary
		WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiLock == null) {
			int mode = WifiManager.WIFI_MODE_FULL;
			if(Compatibility.isCompatible(9) && prefsWrapper.getPreferenceBooleanValue(SipConfigManager.LOCK_WIFI_PERFS)) {
				mode = 0x3; // WIFI_MODE_FULL_HIGH_PERF 
			}
			wifiLock = wman.createWifiLock(mode, "com.csipsimple.SipService");
			wifiLock.setReferenceCounted(false);
		}
		if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.LOCK_WIFI) && !wifiLock.isHeld()) {
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
		
		// Add a alarm for keep alive
		if(kaAlarm == null) {
			kaAlarm = new KeepAliveTimer(this);
		}
		Log.d(THIS_FILE, "KA Start");
		kaAlarm.start();
		hold_resources = true;
	}

	private synchronized void releaseResources() {
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
		if(kaAlarm != null) {
			Log.d(THIS_FILE, "KA Stop");
			kaAlarm.stop();
			kaAlarm = null;
		}
		hold_resources = false;
	}


	

	private static final int TOAST_MESSAGE = 0;

	private Handler serviceHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			switch(msg.what) {
			case TOAST_MESSAGE:
				if (msg.arg1 != 0) {
					Toast.makeText(SipService.this, msg.arg1, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(SipService.this, (String) msg.obj, Toast.LENGTH_LONG).show();
				}
				break;
			}
		}
	};
	
	
	
	public UAStateReceiver getUAStateReceiver() {
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

	private synchronized void dataConnectionChanged(String type, boolean connecting) {
		// Check if it should be ignored first
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

		boolean ipHasChanged = false;
		Log.d(THIS_FILE, "Fire dataConnectionChanged for a " + (connecting ? "connection" : "disconnection"));
		
		if(ni != null && !(ni.getTypeName().equalsIgnoreCase(type))) {
			// We should not stop here cause UTMS could stop while wifi continue to be active and to be the main way to transmit things
			Log.d(THIS_FILE, "Ignore this disconnection cause does it is not relevant of current connection");
			return;
		}

		if (ni != null && connecting) {
			//String currentType = ni.getTypeName();
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
		} else {
			oldIPAddress = null;
			// oldNetworkState = null;
			// oldNetworkType = null;
		}

		if (prefsWrapper.isValidConnectionForOutgoing() || prefsWrapper.isValidConnectionForIncoming()) {
			
			// Only care if we are actually asking for a connect action
			// else it means we recieve a disconnect from another connection 
			// and we should wait for the connect call that will arrive soon
			if(connecting) {
				if (pjService == null || !pjService.isCreated()) {
					// we was not yet started, so start now
					getExecutor().execute(new StartRunnable());
				} else if (ipHasChanged) {
					// Check if IP has changed between
					if (pjService != null && pjService.getActiveCallInProgress() == null) {
						getExecutor().execute(new RestartRunnable());
						// Log.e(THIS_FILE, "We should restart the stack ! ");
					} else {
						// TODO : else refine things => STUN, registration etc...
						serviceHandler.sendMessage(serviceHandler.obtainMessage(TOAST_MESSAGE, 0, 0,
								"Connection have been lost... you may have lost your communication. Hand over is not yet supported"));
					}
				} else {
					Log.d(THIS_FILE, "Nothing done since already well registered");
				}
			}

		} else {
			if (pjService != null && pjService.getActiveCallInProgress() != null) {
				Log.w(THIS_FILE, "There is an ongoing call ! don't stop !! and wait for network to be back...");
				return;
			}
			Log.d(THIS_FILE, "Will stop SERVICE");
			getExecutor().execute(new DestroyRunnable());
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
	
	/**
	 * Get the entire sip profile infos for a given account id
	 * @param accountId the account id we are currently searching on
	 * @param db a database that should be in state closed. This method will open the database and close it
	 * @return The entire sip profile as per current database infos
	 */
	public static SipProfile getAccount(int accountId, DBAdapter db) {
		db.open();
		SipProfile account = db.getAccount(accountId);
		db.close();
		return account;
	}
	

    // Auto answer feature

	public void setAutoAnswerNext(boolean auto_response) {
		autoAcceptCurrent = auto_response;
	}
	
	public boolean shouldAutoAnswer(String remContact, SipProfile acc) {

		Log.d(THIS_FILE, "Search if should I auto answer for " + remContact);
		boolean shouldAutoAnswer = false;
		
		if(autoAcceptCurrent) {
			Log.d(THIS_FILE, "I should auto answer this one !!! ");
			autoAcceptCurrent = false;
			return true;
		}
		
		if(acc != null) {
			Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*@[^>]*)(?:>)?", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(remContact);
			String number = remContact;
			if (m.matches()) {
				number = m.group(2);
			}
			Log.w(THIS_FILE, "Search if should auto answer : " + number);
			synchronized (db) {
				shouldAutoAnswer = Filter.isAutoAnswerNumber(acc, number, db);
			}
		}else {
			Log.d(THIS_FILE, "Oupps... that come from an unknown account...");
			//TODO : add an option to auto hangup if unknown account
		}
		return shouldAutoAnswer;
	}
	
	// Media direct binders
	public void setNoSnd() throws SameThreadException {
		if (pjService != null) {
			pjService.setNoSnd();
		}
	}
	
	public void setSnd() throws SameThreadException {
		if (pjService != null) {
			pjService.setSnd();
		}
	}

    private static Looper createLooper() {
    //	synchronized (executorThread) {
	    	if(executorThread == null) {
	    		Log.w(THIS_FILE, "Creating new handler thread");
		        executorThread = new HandlerThread("SipService.Executor");
		        executorThread.start();
	    	}
	//	}
        return executorThread.getLooper();
    }

    // Executes immediate tasks in a single executorThread.
    // Hold/release wake lock for running tasks
    public class SipServiceExecutor extends Handler {
        SipServiceExecutor() {
            super(createLooper());
        }

        public void execute(Runnable task) {
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
	
    
    /**
     * Timer that can schedule keep alives to occur even when the device is in sleep.
     * Only used internally in this package.
     */
    class KeepAliveTimer extends BroadcastReceiver {
    	private Context context;
		private AlarmManager alarmManager;
		private PendingIntent pendingIntent;
		
		private static final String KA_ACTION = "com.csipsimple.ACTION_KA";
		
		private int interval = 1000;
		private boolean use_wake = false;
    	
		public KeepAliveTimer(Context aContext) {
			context = aContext;
			alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			use_wake = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.KEEP_ALIVE_USE_WAKE);
			if(interval > 0) {
				IntentFilter filter = new IntentFilter(KA_ACTION);
				context.registerReceiver(this, filter);
			}
		}
		
		public void stop() {
			Log.d(THIS_FILE, "KA -> stopping");
			try {
				context.unregisterReceiver(this);
			} catch (IllegalArgumentException e) {
				Log.e(THIS_FILE, "Impossible to destroy KA timer", e);
			}
			if(pendingIntent != null) {
				alarmManager.cancel(pendingIntent);
			}
			pendingIntent = null;
		}
		
		
		public void start() {
			if(pendingIntent != null) {
				Log.w(THIS_FILE, "Ignore starting request cause already started");
			}
			interval = prefsWrapper.getKeepAliveInterval();
			if(interval > 0) {
				Log.d(THIS_FILE, "KA -> starting");
				scheduleNext();
			}
		}
		
		private void scheduleNext() {
            if (pendingIntent != null) {
            	Log.e(THIS_FILE, "Ignore schedule next cause pending intent is not null");
				return;
			}
			Intent intent = new Intent(KA_ACTION);
			pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			long firstTime = SystemClock.elapsedRealtime() + interval * 1000;
			Log.d(THIS_FILE, "KA@" + SystemClock.elapsedRealtime() + " :: next @"+ firstTime );
			alarmManager.set(use_wake ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME, firstTime, pendingIntent);
        }
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(KA_ACTION.equalsIgnoreCase(intent.getAction()) ) {
				Log.d(THIS_FILE, "KA@"+SystemClock.elapsedRealtime()+" :: recieved");
				pendingIntent = null;
				if(pjService != null && interval > 0) {
					Log.d(THIS_FILE, "Send a keep alive packet");
					getExecutor().execute(new SipRunnable() {
						@Override
						protected void doRun() throws SameThreadException {
							pjService.sendKeepAlivePackets();
						}
					});
					scheduleNext();
				}else {
					stop();
				}
			}
		}
    }

    class StartRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
    		startSipStack();
    	}
    }
	
    class StopRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
    		stopSipStack();
    	}
    }
    
	class RestartRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			if(stopSipStack()) {
				startSipStack();
			}
		}
	} 
	
	class DestroyRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			if(stopSipStack()) {
				stopSelf();
			}
		}
	}
	
	class FinalizeDestroyRunnable extends SipRunnable {
		@Override
		protected void doRun() throws SameThreadException {
			
			mExecutor = null;
			
			Log.d(THIS_FILE, "Destroy sip stack");
			
			sipWakeLock.reset();
			
			if(stopSipStack()) {
				notificationManager.cancelAll();
				notificationManager.cancelCalls();
			}else {
				Log.e(THIS_FILE, "Somebody has stopped the service while there is an ongoing call !!!");
			}
			/* If we activate that we can get two concurrent executorThread 
			synchronized (executorThread) {
				HandlerThread currentHandlerThread = executorThread;
				executorThread = null;
				System.gc();
				// This is a little bit crappy, we are cutting were we sit.
				Threading.stopHandlerThread(currentHandlerThread, false);
			}
			*/
			
			// We will not go longer
			Log.i(THIS_FILE, "--- SIP SERVICE DESTROYED ---");
		}
	}
	
	// Enforce same thread contract to ensure we do not call from somewhere else
	public class SameThreadException extends Exception {
		private static final long serialVersionUID = -905639124232613768L;

		public SameThreadException() {
			super("Should be launched from a single worker thread");
		}
	}

	public abstract class SipRunnable  implements Runnable {
		protected abstract void doRun() throws SameThreadException;
		
		public void run() {
			try {
				doRun();
			}catch(SameThreadException e) {
				Log.e(THIS_FILE, "Not done from same thread");
			}
		}
	}
}
