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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.ui.InCallActivity;
import com.csipsimple.utils.Log;

public class UAStateReceiver extends Callback {

	static String THIS_FILE = "SIP UA Receiver";

	private int savedVibrateRing;
	private int savedVibradeNotif;
	private int savedWifiPolicy;
	private int savedVolume;
	private boolean savedSpeakerPhone;
	private boolean savedMicrophoneMute;
	private boolean autoAcceptCurrent = false;
	private Ringtone ringtone;
	private Vibrator vibrator;

	private NotificationManager notificationManager;
	private SipService service;

	private int savedMode;


	@Override
	public void on_incoming_call(int acc_id, int callId, SWIGTYPE_p_pjsip_rx_data rdata) {
		Log.d(THIS_FILE, "Has incoming call " + callId);
		
		// Automatically answer incoming calls with 100/RINGING
		service.callAnswer(callId, 180);
		startRing();
		
		final CallInfo incomingCall = new CallInfo(callId);
		showNotificationForCall(incomingCall);
		
		if (autoAcceptCurrent) {
			// Automatically answer incoming calls with 200/OK
			service.callAnswer(callId, 200);
			autoAcceptCurrent = false;
		} else {
			Thread t = new Thread() {
				@Override
				public void run() {
					launchCallHandler(incomingCall);
				};
			};
			t.start();
		}
	}


	@Override
	public void on_call_state(int callId, pjsip_event e) {
		//Get current infos
		final CallInfo callInfo = new CallInfo(callId);
		final pjsip_inv_state call_state = callInfo.getCallState();
		Log.i(THIS_FILE, "State of call " + callId + " :: " + callInfo.getStringCallState());

		//Thread to avoid deadlocks
/*		Thread t = new Thread() {
			@Override
			public void run() {*/
				if (call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING) || call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING)) {
					showNotificationForCall(callInfo);
					launchCallHandler(callInfo);

				} else if (call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY)) {
					Log.d(THIS_FILE, "Early state");
				} else {
					Log.d(THIS_FILE, "Will stop ringing");
					stopRing();
					// Call is now ended
					if (call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)) {
						notificationManager.cancel(SipService.CALL_NOTIF_ID);
						Log.d(THIS_FILE, "Finish call2");
						unsetAudioInCall();
					}
				}
				
				onMediaState(callInfo);
		/*	}
		};
		t.start();
		*/
	}

	@Override
	public void on_reg_state(int accountId) {
		Log.d(THIS_FILE, "New reg state for : " + accountId);
		
		onRegisterState(accountId);
	}

	@Override
	public void on_stream_created(int call_id, SWIGTYPE_p_pjmedia_session sess, long stream_idx, SWIGTYPE_p_p_pjmedia_port p_port) {
		Log.d(THIS_FILE, "Stream created");
	}

	@Override
	public void on_call_media_state(int callId) {
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(callId, info);
		if (info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {

			setAudioInCall();

			// Should maybe done under media thread instead of this one
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

			// When media is active, connect call to sound device.
			pjsua.conf_connect(info.getConf_slot(), 0);
			pjsua.conf_connect(0, info.getConf_slot());

		} else if (info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_NONE || info.getMedia_status() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ERROR) {
			//
		}else {
			//
		}
	}

	// -------
	// Static constants
	// -------

	public static String UA_CALL_STATE_CHANGED = "com.csipsimple.ua.CALL_STATE_CHANGED";
	public static String UA_REG_STATE_CHANGED = "com.csipsimple.ua.REG_STATE_CHANGED";

	// -------
	// Public configuration for receiver
	// -------
	public void setAutoAnswerNext(boolean auto_response) {
		autoAcceptCurrent = auto_response;
	}

	public void initService(SipService srv) {
		service = srv;
		notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	// --------
	// Private methods
	// --------




	
	/**
	 * Register state for an account
	 * 
	 * @param info
	 */
	private void onRegisterState(final int accountId) {
		//This has to be threaded to avoid deadlocks
		Thread t = new Thread() {
			@Override
			public void run() {
				
				// Update sip service (for notifications
				((SipService) service).updateRegistrationsState();
				
				// Send a broadcast message that for an account
				// registration state has changed
				Intent regStateChangedIntent = new Intent(UA_REG_STATE_CHANGED);
				service.sendBroadcast(regStateChangedIntent);
			}
		};
		t.start();
	}

	private void onMediaState(final CallInfo callInfo) {	
		Intent callStateChangedIntent = new Intent(UA_CALL_STATE_CHANGED);
		callStateChangedIntent.putExtra("call_info", callInfo);
		service.sendBroadcast(callStateChangedIntent);
	
	}

	private void showNotificationForCall(CallInfo call_info) {
		// This is the pending call notification
		int icon = R.drawable.ic_incall_ongoing;
		CharSequence tickerText = "Ongoing call";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		Context context = service.getApplicationContext();

		Intent notificationIntent = new Intent(service, InCallActivity.class);
		notificationIntent.putExtra("call_info", call_info);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, "Ongoing Call", "There is a current call", contentIntent);
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		// notification.flags = Notification.FLAG_FOREGROUND_SERVICE;

		notificationManager.notify(SipService.CALL_NOTIF_ID, notification);
	}
	
	

	
	
	private void startRing() {
		//Store the current ringer mode
		AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		int ringerMode = am.getRingerMode();
		boolean silent = (ringerMode == AudioManager.RINGER_MODE_SILENT) || (ringerMode == AudioManager.RINGER_MODE_VIBRATE);
		//TODO add user pref for that
		boolean vibrate = silent || true;
		
		if(vibrate) {
			//Create the virator
			vibrator = (Vibrator) service.getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(new long[] {1000, 1500}, 0);
		}else {
			vibrator = null;
		}
		
		if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {                          
            String ringtoneUri = service.getPrefs().getRingtone();
            if(!TextUtils.isEmpty(ringtoneUri)) {
				try {
					ringtone = RingtoneManager.getRingtone(service, Uri.parse(ringtoneUri));
					ringtone.play();
				}catch(Exception e) {
					Log.e(THIS_FILE, "Your device has probably no ringtone");
					ringtone = null;
				}
            }
		}else {
			ringtone = null;
		}
	}
	
	
	private void stopRing() {
		if(ringtone != null) {
			ringtone.stop();
		}
		if(vibrator != null) {
			vibrator.cancel();
		}
	}

	/**
	 * 
	 * @param callInfo
	 */
	private void launchCallHandler(CallInfo callInfo) {

		// Launch activity to choose what to do with this call
		Intent callHandlerIntent = new Intent(service, InCallActivity.class);
		callHandlerIntent.putExtra("call_info", callInfo);
		callHandlerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		Log.i(THIS_FILE, "Anounce call activity please");
		service.startActivity(callHandlerIntent);

	}

	
	/**
	 * Set the audio mode as in call
	 */
	private void setAudioInCall() {
		AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		ContentResolver ctntResolver = service.getContentResolver();

		savedVibrateRing = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
		savedVibradeNotif = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
		savedWifiPolicy = android.provider.Settings.System.getInt(ctntResolver, android.provider.Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_DEFAULT);
		savedVolume = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		savedSpeakerPhone = am.isSpeakerphoneOn();
		savedMicrophoneMute = am.isMicrophoneMute();
		savedMode = am.getMode();

		int speaker = AudioManager.MODE_IN_CALL;

		// Settings.System.putInt(ctntResolver,
		// Settings.System.WIFI_SLEEP_POLICY,
		// Settings.System.WIFI_SLEEP_POLICY_NEVER);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);

		am.setSpeakerphoneOn(false);
		am.setMicrophoneMute(false);
		am.setMode(speaker);
	}

	/**
	 * Reset the audio mode
	 */
	private void unsetAudioInCall() {
		AudioManager am = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		ContentResolver ctntResolver = service.getContentResolver();

		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, savedWifiPolicy);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, savedVibrateRing);
		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, savedVibradeNotif);
		am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedVolume, 0);
		am.setSpeakerphoneOn(savedSpeakerPhone);
		am.setMicrophoneMute(savedMicrophoneMute);
		am.setMode(savedMode);
	}

}
