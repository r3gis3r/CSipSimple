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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Ringer;
import com.csipsimple.utils.accessibility.AccessibilityWrapper;
import com.csipsimple.utils.audio.AudioFocusWrapper;
import com.csipsimple.utils.bluetooth.BluetoothWrapper;

public class MediaManager {
	
	final private static String THIS_FILE = "MediaManager";
	
	
	private SipService service;
	private AudioManager audioManager;
	private Ringer ringer;

	//Locks
	private WifiLock wifiLock;
	private WakeLock screenLock;
	private WakeLock cpuLock;
	
	// Media settings to save / resore
	private boolean isSetAudioMode = false;
	

	

	//By default we assume user want bluetooth.
	//If bluetooth is not available connection will never be done and then
	//UI will not show bluetooth is activated
	private boolean userWantBluetooth = true;
	private boolean userWantSpeaker = false;
	private boolean userWantMicrophoneMute = false;

	private Intent mediaStateChangedIntent;
	
	//Bluetooth related
	private BluetoothWrapper bluetoothWrapper;

	private AudioFocusWrapper audioFocusWrapper;
	private AccessibilityWrapper accessibilityManager;
	
	
	private boolean USE_SGS_WRK_AROUND = false;


	private SharedPreferences prefs;
	private static int MODE_SIP_IN_CALL = AudioManager.MODE_NORMAL;
	


	
	public MediaManager(SipService aService) {
		service = aService;
		audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(service);
		accessibilityManager = AccessibilityWrapper.getInstance();
		accessibilityManager.init(service);
		
		ringer = new Ringer(service);
		mediaStateChangedIntent = new Intent(SipManager.ACTION_SIP_MEDIA_CHANGED);
		
		//Try to reset if there were a crash in a call could restore previous settings
		restoreAudioState();
	}
	
	
	public void startService() {
		if(bluetoothWrapper == null) {
			bluetoothWrapper = BluetoothWrapper.getInstance();
			bluetoothWrapper.init(service, this);
		}
		if(audioFocusWrapper == null) {
			audioFocusWrapper = AudioFocusWrapper.getInstance();
			audioFocusWrapper.init(service, audioManager);
		}
		MODE_SIP_IN_CALL = service.prefsWrapper.getInCallMode();
		USE_SGS_WRK_AROUND = service.prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_SGS_CALL_HACK);
	}
	
	public void stopService() {
		Log.i(THIS_FILE, "Remove media manager....");
		if(bluetoothWrapper != null) {
			bluetoothWrapper.unregister();
		}
	}
	
	private int getAudioTargetMode() {
		int targetMode = MODE_SIP_IN_CALL;
//Manage that is BT connected part
//		if(userWantBluetooth) {
//			// Force normal mode for BT SCO
//			return AudioManager.MODE_NORMAL;
//		}
		
		if(service.prefsWrapper.getUseModeApi()) {
			Log.d(THIS_FILE, "User want speaker now..."+userWantSpeaker);
			if(!service.prefsWrapper.generateForSetCall()) {
				return userWantSpeaker ? AudioManager.MODE_NORMAL : AudioManager.MODE_IN_CALL;
			}else {
				return userWantSpeaker ? AudioManager.MODE_IN_CALL: AudioManager.MODE_NORMAL ;
			}
		}
		
		return targetMode;
	}
	
	
	public int setAudioInCall(int clockRate) {
		if(bluetoothWrapper != null && clockRate != 8000) {
			if(userWantBluetooth && bluetoothWrapper.canBluetooth()) {
				return -1;
			}
		}
		
		actualSetAudioInCall();
		
		return 0;
	}
	
	public void unsetAudioInCall() {
	//	Thread t = new Thread() {
	//		public void run() {
				actualUnsetAudioInCall();
	//		};
	//	};
	//	t.start();
	}
	
	
	/**
	 * Set the audio mode as in call
	 */
	private synchronized void actualSetAudioInCall() {
		//Ensure not already set
		if(isSetAudioMode) {
			return;
		}
		stopRing();
		saveAudioState();
		
		//Set the rest of the phone in a better state to not interferate with current call
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
		audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
		
		//LOCKS
		
		//Wifi management if necessary
		ContentResolver ctntResolver = service.getContentResolver();
		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_NEVER);
		
		
		//Acquire wifi lock
		WifiManager wman = (WifiManager) service.getSystemService(Context.WIFI_SERVICE);
		if(wifiLock == null) {
			wifiLock = wman.createWifiLock( 
							(Compatibility.isCompatible(9)) ? 3 : WifiManager.WIFI_MODE_FULL, 
							"com.csipsimple.InCallLock");
			wifiLock.setReferenceCounted(false);
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
		            screenLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall.SCREEN");
		            screenLock.setReferenceCounted(false);
				}
				//Ensure single lock
				if(!screenLock.isHeld()) {
					screenLock.acquire();
					
				}
				
			}
		}
		
		if(cpuLock == null) {
			PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
			cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.csipsimple.onIncomingCall.CPU");
			cpuLock.setReferenceCounted(false);
		}
		if(!cpuLock.isHeld()) {
			cpuLock.acquire();
		}
		
		
		//Audio routing
		int targetMode = getAudioTargetMode();
		Log.d(THIS_FILE, "Set mode audio in call to "+targetMode);
		
		if(service.prefsWrapper.generateForSetCall()) {
			ToneGenerator toneGenerator = new ToneGenerator( AudioManager.STREAM_VOICE_CALL, 1);
			toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM);
			toneGenerator.stopTone();
			toneGenerator.release();
		}
		
		//Set mode
		if(targetMode != AudioManager.MODE_IN_CALL && USE_SGS_WRK_AROUND) {
			//For galaxy S we need to set in call mode before to reset stack
			audioManager.setMode(AudioManager.MODE_IN_CALL);
		}
		
		
		audioManager.setMode(targetMode);
		
		//Routing
		if(service.prefsWrapper.getUseRoutingApi()) {
			audioManager.setRouting(targetMode, userWantSpeaker?AudioManager.ROUTE_SPEAKER:AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
		}else {
			audioManager.setSpeakerphoneOn(userWantSpeaker ? true : false);
		}
		
		audioManager.setMicrophoneMute(false);
		if(bluetoothWrapper != null && userWantBluetooth && bluetoothWrapper.canBluetooth()) {
			Log.d(THIS_FILE, "Try to enable bluetooth");
			bluetoothWrapper.setBluetoothOn(true);
		}
		
		//Set stream solo/volume/focus
		int inCallStream = Compatibility.getInCallStream();
		if(!accessibilityManager.isEnabled()) {
			audioManager.setStreamSolo(inCallStream, true);
		}
		audioFocusWrapper.focus();
		
		setStreamVolume(inCallStream,  (int) (audioManager.getStreamMaxVolume(inCallStream)*service.prefsWrapper.getInitialVolumeLevel()),  0);
		
		
		isSetAudioMode = true;
	//	System.gc();
	}
	
	
	/**
	 * Save current audio mode in order to be able to restore it once done
	 */
	private synchronized void saveAudioState() {
		if( prefs.getBoolean("isSavedAudioState", false) ) {
			//If we have already set, do not set it again !!! 
			return;
		}
		ContentResolver ctntResolver = service.getContentResolver();
		
		Editor ed = prefs.edit();
		ed.putInt("savedVibrateRing", audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER));
		ed.putInt("savedVibradeNotif", audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION));
		ed.putInt("savedRingerMode", audioManager.getRingerMode());
		ed.putInt("savedWifiPolicy" , android.provider.Settings.System.getInt(ctntResolver, android.provider.Settings.System.WIFI_SLEEP_POLICY, Settings.System.WIFI_SLEEP_POLICY_DEFAULT));
		
		int inCallStream = Compatibility.getInCallStream();
		ed.putInt("savedVolume", audioManager.getStreamVolume(inCallStream));
		
		int targetMode = getAudioTargetMode();
		if(service.prefsWrapper.getUseRoutingApi()) {
			ed.putInt("savedRoute", audioManager.getRouting(targetMode));
		}else {
			ed.putBoolean("savedSpeakerPhone", audioManager.isSpeakerphoneOn());
		}
		ed.putInt("savedMode", audioManager.getMode());
		
		ed.putBoolean("isSavedAudioState", true);
		ed.commit();
	}
	
	private synchronized void restoreAudioState() {
		if( !prefs.getBoolean("isSavedAudioState", false) ) {
			//If we have NEVER set, do not try to reset !
			return;
		}
		
		ContentResolver ctntResolver = service.getContentResolver();

		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, prefs.getInt("savedWifiPolicy", Settings.System.WIFI_SLEEP_POLICY_DEFAULT));
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, prefs.getInt("savedVibrateRing", AudioManager.VIBRATE_SETTING_ONLY_SILENT));
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, prefs.getInt("savedVibradeNotif", AudioManager.VIBRATE_SETTING_OFF));
		audioManager.setRingerMode(prefs.getInt("savedRingerMode", AudioManager.RINGER_MODE_NORMAL));
		
		int inCallStream = Compatibility.getInCallStream();
		setStreamVolume(inCallStream, prefs.getInt("savedVolume", (int)(audioManager.getStreamMaxVolume(inCallStream)*0.8) ), 0);
		
		int targetMode = getAudioTargetMode();
		if(service.prefsWrapper.getUseRoutingApi()) {
			audioManager.setRouting(targetMode, prefs.getInt("savedRoute", AudioManager.ROUTE_SPEAKER), AudioManager.ROUTE_ALL);
		}else {
			audioManager.setSpeakerphoneOn(prefs.getBoolean("savedSpeakerPhone", false));
		}
		audioManager.setMode(prefs.getInt("savedMode", AudioManager.MODE_NORMAL));
		

		Editor ed = prefs.edit();
		ed.putBoolean("isSavedAudioState", false);
		ed.commit();
	}
	
	/**
	 * Reset the audio mode
	 */
	private synchronized void actualUnsetAudioInCall() {
		
		if(!prefs.getBoolean("isSavedAudioState", false) || !isSetAudioMode) {
			return;
		}

		Log.d(THIS_FILE, "Unset Audio In call");

		int inCallStream = Compatibility.getInCallStream();
		if(bluetoothWrapper != null) {
			//This fixes the BT activation but... but... seems to introduce a lot of other issues
			//bluetoothWrapper.setBluetoothOn(true);
			Log.d(THIS_FILE, "Unset bt");
			bluetoothWrapper.setBluetoothOn(false);
		}
		audioManager.setMicrophoneMute(false);
		audioManager.setStreamSolo(inCallStream, false);
		
		restoreAudioState();
		
		if(wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
		if(screenLock != null && screenLock.isHeld()) {
			Log.d(THIS_FILE, "Release screen lock");
			screenLock.release();
		}
		if(cpuLock != null && cpuLock.isHeld()) {
			cpuLock.release();
		}
		
		audioFocusWrapper.unFocus();
		
		isSetAudioMode = false;
	}
	
	
	public void startRing(String remoteContact) {
		saveAudioState();
		audioFocusWrapper.focus();
		
		if(!ringer.isRinging()) {
			ringer.ring(remoteContact, service.getPrefs().getRingtone());
		}else {
			Log.d(THIS_FILE, "Already ringing ....");
		}
		
	}
	
	public void stopRing() {
		if(ringer.isRinging()) {
			ringer.stopRing();
		}
	}
	
	public void stopAnnoucing() {
		stopRing();
		audioFocusWrapper.unFocus();
	}
	
	public void resetSettings() {
		userWantBluetooth = true;
		userWantMicrophoneMute = false;
		userWantSpeaker = false;
	}


	public void toggleMute() throws SameThreadException {
		setMicrophoneMute(!userWantMicrophoneMute);
	}
	
	public void setMicrophoneMute(boolean on) {
		if(on != userWantMicrophoneMute ) {
			userWantMicrophoneMute = on;
			setSoftwareVolume();
			broadcastMediaChanged();
		}
	}
	
	public void setSpeakerphoneOn(boolean on) throws SameThreadException {
		if(service != null) {
			service.setNoSnd();
			userWantSpeaker = on;
			service.setSnd();
		}
		broadcastMediaChanged();
	}
	
	public void setBluetoothOn(boolean on) throws SameThreadException {
		Log.d(THIS_FILE, "Set BT "+on);
		if(service != null) {
			service.setNoSnd();
			userWantBluetooth = on;
			service.setSnd();
		}
		broadcastMediaChanged();
	}
	

	

	public MediaState getMediaState() {
		MediaState mediaState = new MediaState();
		
		// Micro 
		mediaState.isMicrophoneMute = userWantMicrophoneMute;
		mediaState.canMicrophoneMute = true; /*&& !mediaState.isBluetoothScoOn*/ //Compatibility.isCompatible(5);
		
		// Speaker
		mediaState.isSpeakerphoneOn = userWantSpeaker;
		mediaState.canSpeakerphoneOn = true && !mediaState.isBluetoothScoOn; //Compatibility.isCompatible(5);
		
		//Bluetooth
		
		if(bluetoothWrapper != null) {
			mediaState.isBluetoothScoOn = bluetoothWrapper.isBluetoothOn();
			mediaState.canBluetoothSco = bluetoothWrapper.canBluetooth();
		}else {
			mediaState.isBluetoothScoOn = false;
			mediaState.canBluetoothSco = false;
		}
		
		return mediaState;
	}
	
	/**
	 * Change the audio volume amplification according to the fact we are using bluetooth
	 */
	public void setSoftwareVolume(){
		
		if(service != null) {
			service.getExecutor().execute(service.new SipRunnable() {
				
				@Override
				protected void doRun() throws SameThreadException {
					boolean useBT = (bluetoothWrapper != null && bluetoothWrapper.isBluetoothOn());
					
					String speaker_key = useBT ? SipConfigManager.SND_BT_SPEAKER_LEVEL : SipConfigManager.SND_SPEAKER_LEVEL;
					String mic_key = useBT ? SipConfigManager.SND_BT_MIC_LEVEL : SipConfigManager.SND_MIC_LEVEL;
					
					float speakVolume = service.prefsWrapper.getPreferenceFloatValue(speaker_key);
					float micVolume = userWantMicrophoneMute? 0 : service.prefsWrapper.getPreferenceFloatValue(mic_key);

					service.confAdjustTxLevel(speakVolume);
					service.confAdjustRxLevel(micVolume);
					
					// Force the BT mode to normal
					if(useBT) {
						audioManager.setMode(AudioManager.MODE_NORMAL);
					}
				}
			});
		}
	}
	
	
	
	public void broadcastMediaChanged() {
		service.sendBroadcast(mediaStateChangedIntent);
	}
	
	private static final String ACTION_AUDIO_VOLUME_UPDATE = "org.openintents.audio.action_volume_update";
	private static final String EXTRA_STREAM_TYPE = "org.openintents.audio.extra_stream_type";
	private static final String EXTRA_VOLUME_INDEX = "org.openintents.audio.extra_volume_index";
	private static final String EXTRA_RINGER_MODE = "org.openintents.audio.extra_ringer_mode";
	private static final int EXTRA_VALUE_UNKNOWN = -9999;
	
	private void broadcastVolumeWillBeUpdated(int streamType, int index) {
		Intent notificationIntent = new Intent(ACTION_AUDIO_VOLUME_UPDATE);
		notificationIntent.putExtra(EXTRA_STREAM_TYPE, streamType);
		notificationIntent.putExtra(EXTRA_VOLUME_INDEX, index);
		notificationIntent.putExtra(EXTRA_RINGER_MODE, EXTRA_VALUE_UNKNOWN);

		service.sendBroadcast(notificationIntent, null);
	}
	
	public void setStreamVolume(int streamType, int index, int flags) {
		broadcastVolumeWillBeUpdated(streamType, index);
        audioManager.setStreamVolume(streamType, index, flags);
	}
	
	public void adjustStreamVolume(int streamType, int direction, int flags) {
		broadcastVolumeWillBeUpdated(streamType, EXTRA_VALUE_UNKNOWN);
        audioManager.adjustStreamVolume(streamType, direction, flags);
        
        if(streamType == AudioManager.STREAM_RING) {
        	// Update ringer 
        	ringer.updateRingerMode();
        }
	}
	
	// Public accessor
	public boolean isUserWantMicrophoneMute() {
		return userWantMicrophoneMute;
	}
}
