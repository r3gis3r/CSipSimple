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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.Callback;
import org.pjsip.pjsua.SWIGTYPE_p_p_pjmedia_port;
import org.pjsip.pjsua.SWIGTYPE_p_pjmedia_session;
import org.pjsip.pjsua.SWIGTYPE_p_pjsip_rx_data;
import org.pjsip.pjsua.pjsip_event;
import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;
import org.pjsip.pjsua.pjsua_call_media_status;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.CallInfo;
import com.csipsimple.models.CallInfo.UnavailableException;
import com.csipsimple.utils.CallLogHelper;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Ringer;

public class UAStateReceiver extends Callback {
	static String THIS_FILE = "SIP UA Receiver";

	final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
	final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
	final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";


	private int savedVibrateRing, savedVibradeNotif, savedWifiPolicy;
	private int savedVolume;
	private boolean savedSpeakerPhone;
	//private boolean savedMicrophoneMute;
	//private boolean savedBluetooth;
	private int savedRoute, savedMode;
	
	
	private boolean autoAcceptCurrent = false;

	private NotificationManager notificationManager;
	private SipService service;
	private ComponentName remoteControlResponder;


	private Ringer ringer;
	private boolean isSavedAudioState = false, isSetAudioMode = false, isMusicActive = false;
	
	@Override
	public void on_incoming_call(int acc_id, final int callId, SWIGTYPE_p_pjsip_rx_data rdata) {
		CallInfo callInfo = getCallInfo(callId);
		msgHandler.sendMessage(msgHandler.obtainMessage(ON_INCOMING_CALL, callInfo));
	}
	
	
	@Override
	public void on_call_state(int callId, pjsip_event e) {
		//Get current infos
		CallInfo callInfo = getCallInfo(callId);
		pjsip_inv_state callState = callInfo.getCallState();
		if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)) {
			stopRing();
			// Call is now ended
			service.stopDialtoneGenerator();

			//unsetAudioInCall();
		}
		msgHandler.sendMessage(msgHandler.obtainMessage(ON_CALL_STATE, callInfo));
	}

	@Override
	public void on_reg_state(int accountId) {
		Log.d(THIS_FILE, "New reg state for : " + accountId);
		msgHandler.sendMessage(msgHandler.obtainMessage(ON_REGISTRATION_STATE, accountId));
	}

	@Override
	public void on_stream_created(int call_id, SWIGTYPE_p_pjmedia_session sess, long stream_idx, SWIGTYPE_p_p_pjmedia_port p_port) {
		Log.d(THIS_FILE, "Stream created");
	}
	
	@Override
	public void on_stream_destroyed(int callId, SWIGTYPE_p_pjmedia_session sess, long streamIdx) {
		Log.d(THIS_FILE, "Stream destroyed");
	}

	@Override
	public void on_call_media_state(int callId) {
		stopRing();
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(callId, info);
		if (info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
			pjsua.conf_connect(info.getConf_slot(), 0);
			pjsua.conf_connect(0, info.getConf_slot());
			pjsua.conf_adjust_tx_level(0, service.prefsWrapper.getSpeakerLevel());
			pjsua.conf_adjust_rx_level(0, service.prefsWrapper.getMicLevel());
			
		}
	}
	
	
	
	// -------
	// Current call management -- assume for now one unique call is managed
	// -------
	private HashMap<Integer, CallInfo> callsList = new HashMap<Integer, CallInfo>();
	//private long currentCallStart = 0;
	
	public CallInfo getCallInfo(Integer callId) {
		CallInfo callInfo = callsList.get(callId);
		if(callInfo == null) {
			try {
				callInfo = new CallInfo(callId);
				callsList.put(callId, callInfo);
			} catch (UnavailableException e) {
				//TODO : treat error
			}
		} else {
			//Update from pjsip
			try {
				callInfo.updateFromPj();
			} catch (UnavailableException e) {
				//TODO : treat error
			}
		}
		
		return callInfo;
	}


	private WorkerHandler msgHandler;
	private AudioManager audioManager;
	private HandlerThread handlerThread;

	private WifiLock wifiLock;
	private WakeLock screenLock;




	private static final int ON_INCOMING_CALL = 1;
	private static final int ON_CALL_STATE = 2;
	private static final int ON_REGISTRATION_STATE = 3;



    
	private class WorkerHandler extends Handler {

		public WorkerHandler(Looper looper) {
            super(looper);
			Log.d(THIS_FILE, "Create async worker !!!");
        }
			
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ON_INCOMING_CALL:{
				CallInfo callInfo = (CallInfo) msg.obj;
				int callId = callInfo.getCallId();
				callInfo.setIncoming(true);
				showNotificationForCall(callInfo);
				startRing();
				broadCastAndroidCallState("RINGING", callInfo.getRemoteContact());
				
				// Automatically answer incoming calls with 180/RINGING
				service.callAnswer(callId, 180);
				
				if (autoAcceptCurrent) {
					// Automatically answer incoming calls with 200/OK
					service.callAnswer(callId, 200);
					autoAcceptCurrent = false;
				} else {
					launchCallHandler(callInfo);
				}
				
				break;
			}
			
			case ON_CALL_STATE:{
				CallInfo callInfo = (CallInfo) msg.obj;
				pjsip_inv_state callState = callInfo.getCallState();
				
				if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING) || 
						callState.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING)) {
					showNotificationForCall(callInfo);
					launchCallHandler(callInfo);
					broadCastAndroidCallState("RINGING", callInfo.getRemoteContact());
				} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY)) {
					broadCastAndroidCallState("OFFHOOK", callInfo.getRemoteContact());
				} else if(callState.equals(pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)) {
					broadCastAndroidCallState("OFFHOOK", callInfo.getRemoteContact());
					callInfo.callStart = System.currentTimeMillis();
				}else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)) {
					notificationManager.cancel(SipService.CALL_NOTIF_ID);
					Log.d(THIS_FILE, "Finish call2");
					
					//CallLog
					ContentValues cv = CallLogHelper.logValuesForCall(callInfo, callInfo.callStart);
					
					//Fill our own database
					DBAdapter database = new DBAdapter(service);
					database.open();
					database.insertCallLog(cv);
					database.close();
					
					//If needed fill native database
					if(service.prefsWrapper.useIntegrateCallLogs()) {
						//Reformat number for callogs
						Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*)@[^>]*(?:>)?");
						Matcher m = p.matcher(cv.getAsString(Calls.NUMBER));
						if (m.matches()) {
							
						//	remoteContact = m.group(1);
							String phoneNumber =  m.group(2);
							if(!TextUtils.isEmpty(phoneNumber)) {
								cv.put(Calls.NUMBER, phoneNumber);
								CallLogHelper.addCallLog(service.getContentResolver(), cv);
							}
						}
						
					}
					callInfo.setIncoming(false);
					callInfo.callStart = 0;
					broadCastAndroidCallState("IDLE", callInfo.getRemoteContact());
				}
				onBroadcastCallState(callInfo);
				break;
			}
			case ON_REGISTRATION_STATE:{
				Log.d(THIS_FILE, "In reg state");
				// Update sip service (for notifications
				((SipService) service).updateRegistrationsState();
				// Send a broadcast message that for an account
				// registration state has changed
				Intent regStateChangedIntent = new Intent(SipService.ACTION_SIP_REGISTRATION_CHANGED);
				service.sendBroadcast(regStateChangedIntent);
				break;
			}
			}
		}
	};

	
	private void startRing() {
		saveAudioState();
		
		if(!ringer.isRinging()) {
			String ringtoneUri = service.getPrefs().getRingtone();
			ringer.setCustomRingtoneUri(Uri.parse(ringtoneUri));
			ringer.ring();
		}
		
	}
	
	private void stopRing() {
		if(ringer.isRinging()) {
			ringer.stopRing();
		}
	}
	
	// -------
	// Public configuration for receiver
	// -------
	public void setAutoAnswerNext(boolean auto_response) {
		autoAcceptCurrent = auto_response;
	}
	

	public void initService(SipService srv) {
		service = srv;
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
		ringer = new Ringer(service);
		
		if(handlerThread == null) {
			handlerThread = new HandlerThread("UAStateAsyncWorker");
			handlerThread.start();
		}
		if(msgHandler == null) {
			msgHandler = new WorkerHandler(handlerThread.getLooper());
		}
		
		//
		// Android 2.2 has introduced a new way of handling headset
		// action button presses. This involves registering to handle
		// the button presses every time one needs it and unregistering
		// once the button events are no longer needed. Last app to
		// register gets the focus.
		//
		if (Compatibility.isCompatible(8)) {
			remoteControlResponder = new ComponentName(service.getPackageName(), DeviceStateReceiver.class.getName());
		}
	}
	

	public void stopService() {
		if(handlerThread != null) {
			boolean fails = true;
			
			if(Compatibility.isCompatible(5)) {
				try {
					Method method = handlerThread.getClass().getDeclaredMethod("quit");
					method.invoke(handlerThread);
					fails = false;
				} catch (Exception e) {
					Log.d(THIS_FILE, "Something is wrong with api level declared use fallback method");
				}
			}
			if (fails && handlerThread.isAlive()) {
				try {
					//This is needed for android 4 and lower
					handlerThread.join(500);
					/*
					if (handlerThread.isAlive()) {
						handlerThread.
					}
					*/
				} catch (Exception e) {
					Log.e(THIS_FILE, "Can t finish handler thread....", e);
				}
			}
			handlerThread = null;
		}
	}

	// --------
	// Private methods
	// --------
	

	private void onBroadcastCallState(final CallInfo callInfo) {
		//Internal event
		Intent callStateChangedIntent = new Intent(SipService.ACTION_SIP_CALL_CHANGED);
		callStateChangedIntent.putExtra("call_info", callInfo);
		service.sendBroadcast(callStateChangedIntent);
		
		
	}

	private void broadCastAndroidCallState(String state, String number) {
		//Android normalized event
		Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
		intent.putExtra("state", state);
		if (number != null) {
			intent.putExtra("incoming_number", number);
		}
		intent.putExtra(service.getString(R.string.app_name), true);
		service.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
	}
	
	
	private void showNotificationForCall(CallInfo currentCallInfo2) {
		// This is the pending call notification
		int icon = R.drawable.ic_incall_ongoing;
		CharSequence tickerText =  service.getText(R.string.ongoing_call);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		Context context = service.getApplicationContext();

		Intent notificationIntent = new Intent(SipService.ACTION_SIP_CALL_UI);
		notificationIntent.putExtra("call_info", currentCallInfo2);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);

		
		notification.setLatestEventInfo(context, service.getText(R.string.ongoing_call), 
				currentCallInfo2.getRemoteContact(), contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		// notification.flags = Notification.FLAG_FOREGROUND_SERVICE;

		notificationManager.notify(SipService.CALL_NOTIF_ID, notification);
	}
	
	

	
	/**
	 * 
	 * @param currentCallInfo2 
	 * @param callInfo
	 */
	private void launchCallHandler(CallInfo currentCallInfo2) {
		
		// Launch activity to choose what to do with this call
		Intent callHandlerIntent = new Intent(SipService.ACTION_SIP_CALL_UI); //new Intent(service, getInCallClass());
		callHandlerIntent.putExtra("call_info", currentCallInfo2);
		callHandlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Log.i(THIS_FILE, "Anounce call activity please");
		service.startActivity(callHandlerIntent);

	}

	
	private void saveAudioState() {
		if(isSavedAudioState) {
			return;
		}
		ContentResolver ctntResolver = service.getContentResolver();
		
		savedVibrateRing = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
		savedVibradeNotif = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
		savedWifiPolicy = android.provider.Settings.System.getInt(ctntResolver, android.provider.Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
		savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		
		savedSpeakerPhone = audioManager.isSpeakerphoneOn();
		savedMode = audioManager.getMode();
		savedRoute = audioManager.getRouting(AudioManager.MODE_IN_CALL);
		
		isSavedAudioState = true;
		
		isMusicActive = audioManager.isMusicActive();
	}
	
	
	/**
	 * Check if the specific call info indicate it is an active
	 * call in progress.
	 */
	private boolean isActiveCallInProgress(CallInfo callInfo) {
    	if (callInfo != null) {
    		pjsip_inv_state state = callInfo.getCallState();

    		switch (state) {
    		case PJSIP_INV_STATE_INCOMING:
    		case PJSIP_INV_STATE_EARLY:
    		case PJSIP_INV_STATE_CALLING:
    		case PJSIP_INV_STATE_CONFIRMED:
    		case PJSIP_INV_STATE_CONNECTING:
    			return true;
    			
    		case PJSIP_INV_STATE_DISCONNECTED:
    		case PJSIP_INV_STATE_NULL:
    			break;
    		}
    	}
    	return false;
	}
	
	
	/**
	 * Check if any of call infos indicate there is an active
	 * call in progress.
	 */
	public boolean isActiveCallInProgress() {
		Log.d(THIS_FILE, "isActiveCallInProgress(), number of calls: " + callsList.keySet().size());
		
		//
		// Go through the whole list of calls and check if
		// any call is in an active state.
		//
		for (Integer i : callsList.keySet()) { 
			CallInfo callInfo = getCallInfo(i);
			if (isActiveCallInProgress(callInfo)) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Broadcast the Headset button press event internally if
	 * there is any call in progress.
	 */
	public boolean handleHeadsetButton() {
		
		if (isActiveCallInProgress()) {
			Intent regStateChangedIntent = new Intent(SipService.ACTION_SIP_SERVICE_HEADSET);
			service.sendBroadcast(regStateChangedIntent);
			return true;
		}	
		return false;
	}
	
	
	/**
	 * Internal receiver of Headset button presses events.
	 * This class is only used for Android versions prior to
	 * v2.2 (API-Level 8). For older versions the DeviceStateReceiver
	 * is used to handle the incoming button actions.
	 */
	private BroadcastReceiver headsetButtonReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(THIS_FILE, "headsetButtonReceiver::onReceive");

	    	//
			// Headset button has been pressed by user. Normally when 
			// the UI is active this event will never be generated instead
			// a headset button press will be handled as a regular key
			// press event.
			//
	        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
				KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				
				if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
		        	if (handleHeadsetButton()) {
			        	//
						// After processing the event we will prevent other applications
						// from receiving the button press since we have handled it ourself
						// and do not want any media player to start playing for example.
						//
		        		abortBroadcast();
		        	}
				}
			}
		}
	};
	
	
	/**
	 * Register to be the sole handler of Headset button presses
	 * to prevent other applications such as media players from
	 * acting on the button presses.
	 */
	private void registerMediaButtonReceiver() {
		Log.d(THIS_FILE, "registerMediaButtonReceiver");
		
		if (Compatibility.isCompatible(8)) {
			//
			// Register as the sole headset button receiver
			// (for Android 2.2 or above).
			//
			try {
				Method method = AudioManager.class.getMethod("registerMediaButtonEventReceiver", new Class[] { ComponentName.class } );
				method.invoke(audioManager, remoteControlResponder);
			} catch (Exception e) {
				Log.d(THIS_FILE, "Something is wrong with api level declared when registering media button receiver, " + e.getMessage());
			}
		} else {
			//
			// Register am event receiver for ACTION_MEDIA_BUTTON events,
			// and adjust its priority to make sure we get these events
			// before any media player which hijacks the button presses.
			//
			IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
			intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
			service.registerReceiver(headsetButtonReceiver, intentFilter);
		}
	}
	
	
	/**
	 * Unregister as the the sole handler of Headset button presses
	 * to enable other applications such as media players to handle
	 * the button presses instead.
	 */
	private void unregisterMediaButtonReceiver() {
		Log.d(THIS_FILE, "unregisterMediaButtonReceiver");
		
		if (Compatibility.isCompatible(8)) {
			//
			// Unregister as the sole headset button receiver
			// (for Android 2.2 or above).
			//
			try {
				Method method = AudioManager.class.getMethod("unregisterMediaButtonEventReceiver", new Class[] { ComponentName.class } );
				method.invoke(audioManager, remoteControlResponder);
			} catch (Exception e) {
				Log.d(THIS_FILE, "Something is wrong with api level declared when unregistering media button receiver, " + e.getMessage());
			}
		} else {
			//
			// Unregister the receiver of headset button pressed events
			// for older versions of Android that do not support the
			// unregisterMediaButtonEventReceiver method.
			//
			service.unregisterReceiver(headsetButtonReceiver);
		}
	}
	
	
	/**
	 * Set the audio mode as in call
	 */
	public synchronized void setAudioInCall() {
		registerMediaButtonReceiver();

		//Ensure not already set
		if(isSetAudioMode) {
			return;
		}
		
		saveAudioState();
		
		Log.d(THIS_FILE, "Set mode audio in call");
	//	PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_OFFHOOK);
		
		
		if(!Compatibility.isCompatible(5)) {
			audioManager.setRouting(AudioManager.MODE_IN_CALL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
			audioManager.setMode(AudioManager.MODE_IN_CALL);
		}
		audioManager.setSpeakerphoneOn(false);
		
		//Set stream solo/volume
		int inCallStream = Compatibility.getInCallStream();
		
		
		audioManager.setStreamSolo(inCallStream, true);
		audioManager.setStreamVolume(inCallStream,  (int) (audioManager.getStreamMaxVolume(inCallStream)*service.prefsWrapper.getInitialVolumeLevel()),  0);
		
		//Set the rest of the phone in a better state to not interferate with current call
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);

		if(isMusicActive) {
			service.sendBroadcast(new Intent(PAUSE_ACTION));
		}
		
		
		//LOCKS
		
		//Wifi management if necessary
		ContentResolver ctntResolver = service.getContentResolver();
		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
		
		
		//Acquire wifi lock
		WifiManager wman = (WifiManager) service.getSystemService(Context.WIFI_SERVICE);
		if(wifiLock == null) {
			wifiLock = wman.createWifiLock("com.csipsimple.InCallLock");
		}
		WifiInfo winfo = wman.getConnectionInfo();
		if(winfo != null) {
			DetailedState dstate = WifiInfo.getDetailedStateOf(winfo.getSupplicantState());
			//We assume that if obtaining ip addr, we are almost connected so can keep wifi lock
			if(dstate == DetailedState.OBTAINING_IPADDR || dstate == DetailedState.CONNECTED) {
				if(!wifiLock.isHeld()) {
					wifiLock.acquire();
				}
			}
			
			//This wake lock purpose is to prevent PSP wifi mode 
			if(service.prefsWrapper.keepAwakeInCall()) {
				if(screenLock == null) {
					PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
		            screenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
				}
				//Ensure single lock
				if(!screenLock.isHeld()) {
					screenLock.acquire();
				}
				
			}
		}
		
		
		
		isSetAudioMode = true;
		System.gc();
	}
	
	
	/**
	 * Reset the audio mode
	 */
	public synchronized void unsetAudioInCall() {
		unregisterMediaButtonReceiver();
		
		if(!isSavedAudioState || !isSetAudioMode) {
			return;
		}

		Log.d(THIS_FILE, "Unset Audio In call");
		
		ContentResolver ctntResolver = service.getContentResolver();

		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, savedWifiPolicy);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, savedVibrateRing);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, savedVibradeNotif);
		
		if(!Compatibility.isCompatible(5)) {
			audioManager.setMode(savedMode);
			audioManager.setRouting(AudioManager.MODE_IN_CALL, savedRoute, AudioManager.ROUTE_ALL);
		}
		audioManager.setSpeakerphoneOn(savedSpeakerPhone);
		
		audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedVolume, 0);
		audioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
				
		if(wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
		if(screenLock != null && screenLock.isHeld()) {
			Log.d(THIS_FILE, "Release screen lock");
			screenLock.release();
		}
		
		if(isMusicActive) {
			service.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
		}
		
		isSavedAudioState = false;
		isSetAudioMode = false;
		isMusicActive = false;
	}
	
	
	

}
