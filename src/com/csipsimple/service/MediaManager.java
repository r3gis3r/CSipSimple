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

import org.pjsip.pjsua.pjsua;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;

import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Ringer;
import com.csipsimple.utils.bluetooth.BluetoothWrapper;

public class MediaManager {
	
	final private static String THIS_FILE = "MediaManager";
	
	final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
	final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
	
	private SipService service;
	private AudioManager audioManager;
	private Ringer ringer;

	//Locks
	private WifiLock wifiLock;
	private WakeLock screenLock;
	
	// Media settings to save / resore
	private int savedVibrateRing, savedVibradeNotif, savedWifiPolicy;
	private int savedVolume;
	private boolean savedSpeakerPhone;
	//private boolean savedMicrophoneMute;
	private int savedRoute, savedMode;
	private boolean isSavedAudioState = false, isSetAudioMode = false, isMusicActive = false;

	private Intent mediaStateChangedIntent;
	
	//Bluetooth related
	private static boolean bluetoothClassAvailable;
	private BluetoothWrapper bluetoothWrapper;

	private static int MODE_SIP_IN_CALL = AudioManager.MODE_NORMAL;
	

	/* establish whether the "new" class is available to us */
	static {
		try {
			BluetoothWrapper.checkAvailable();
			bluetoothClassAvailable = true;
		} catch (Throwable t) {
			bluetoothClassAvailable = false;
		}
	}

	public MediaManager(SipService aService) {
		service = aService;
		audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
		ringer = new Ringer(service);
		
		mediaStateChangedIntent = new Intent(SipService.ACTION_SIP_MEDIA_CHANGED);
		
		if(bluetoothClassAvailable) {
			bluetoothWrapper = new BluetoothWrapper(service, this);
		}
		MODE_SIP_IN_CALL = Compatibility.getInCallMode();
		
	}
	
	public void stopService() {
		Log.i(THIS_FILE, "Remove media manager....");
		if(bluetoothWrapper != null) {
			bluetoothWrapper.destroy();
			bluetoothWrapper = null;
		}
	}
	
	/**
	 * Set the audio mode as in call
	 */
	public synchronized void setAudioInCall() {
		
		//Ensure not already set
		if(isSetAudioMode) {
			return;
		}
		
		saveAudioState();
		
		Log.d(THIS_FILE, "Set mode audio in call");
		audioManager.setMode(MODE_SIP_IN_CALL);
		
		
		if(Compatibility.useRoutingApi()) {
			audioManager.setRouting(MODE_SIP_IN_CALL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
		}
		
		//Set stream solo/volume
		int inCallStream = Compatibility.getInCallStream();
		audioManager.setStreamSolo(inCallStream, true);
		setStreamVolume(inCallStream,  (int) (audioManager.getStreamMaxVolume(inCallStream)*service.prefsWrapper.getInitialVolumeLevel()),  0);
		
		audioManager.setSpeakerphoneOn(false);
		audioManager.setMicrophoneMute(false);
		if(bluetoothClassAvailable && userWantBluetooth && bluetoothWrapper.canBluetooth()) {
			Log.d(THIS_FILE, "Try to enable bluetooth");
			bluetoothWrapper.setBluetoothOn(true);
		}
		
		//Set the rest of the phone in a better state to not interferate with current call
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);

		if(isMusicActive && service.prefsWrapper.integrateWithMusicApp()) {
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
	//	System.gc();
	}
	
	
	/**
	 * Save current audio mode in order to be able to restore it once done
	 */
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
		savedRoute = audioManager.getRouting(MODE_SIP_IN_CALL);
		
		isSavedAudioState = true;
		
		isMusicActive = audioManager.isMusicActive();
	}

	
	/**
	 * Reset the audio mode
	 */
	public synchronized void unsetAudioInCall() {
		
		if(!isSavedAudioState || !isSetAudioMode) {
			return;
		}

		Log.d(THIS_FILE, "Unset Audio In call");
		
		ContentResolver ctntResolver = service.getContentResolver();

		Settings.System.putInt(ctntResolver, Settings.System.WIFI_SLEEP_POLICY, savedWifiPolicy);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, savedVibrateRing);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, savedVibradeNotif);
		
		audioManager.setMode(savedMode);
		if(Compatibility.useRoutingApi()) {
			audioManager.setRouting(MODE_SIP_IN_CALL, savedRoute, AudioManager.ROUTE_ALL);
		}
		
		if(bluetoothClassAvailable) {
			//This fixes the BT activation but... but... seems to introduce a lot of other issues
			//bluetoothWrapper.setBluetoothOn(true);
			bluetoothWrapper.setBluetoothOn(false);
		}
		audioManager.setSpeakerphoneOn(savedSpeakerPhone);
		audioManager.setMicrophoneMute(false);

		
		int inCallStream = Compatibility.getInCallStream();
		setStreamVolume(inCallStream, savedVolume, 0);
		audioManager.setStreamSolo(inCallStream, false);
				
		if(wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
		if(screenLock != null && screenLock.isHeld()) {
			Log.d(THIS_FILE, "Release screen lock");
			screenLock.release();
		}
		
		if(isMusicActive && service.prefsWrapper.integrateWithMusicApp()) {
			service.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
		}
		
		
		isSavedAudioState = false;
		isSetAudioMode = false;
		isMusicActive = false;
		
	}
	
	
	public void startRing(String remoteContact) {
		saveAudioState();
		
		if(!ringer.isRinging()) {
			ringer.ring(remoteContact, service.getPrefs().getRingtone());
		}
		
	}
	
	public void stopRing() {
		if(ringer.isRinging()) {
			ringer.stopRing();
		}
	}
	

	//By default we assume user want bluetooth.
	//If bluetooth is not available connection will never be done and then
	//UI will not show bluetooth is activated
	private boolean userWantBluetooth = false;

	public void toggleMute() {
		setMicrophoneMute(!audioManager.isMicrophoneMute());
	}
	
	public synchronized void setMicrophoneMute(boolean on) {
		if(audioManager.isMicrophoneMute() != on) {
			audioManager.setMicrophoneMute(on);
			broadcastMediaChanged();
		}
	}
	
	public synchronized void setSpeakerphoneOn(boolean on) {
		if(audioManager.isSpeakerphoneOn() != on) {
			if(Compatibility.useRoutingApi()) {
				audioManager.setRouting(MODE_SIP_IN_CALL, 
						on?AudioManager.ROUTE_SPEAKER:AudioManager.ROUTE_EARPIECE, 
								AudioManager.ROUTE_ALL);
			}
			audioManager.setSpeakerphoneOn(on);
			broadcastMediaChanged();
		}
	}

	public synchronized void setBluetoothOn(boolean on) {
//		if(on == userWantBluetooth) {
//			return;
//		}
		if(on) {
			Log.d(THIS_FILE, "starting bt");
			pjsua.set_no_snd_dev();
			userWantBluetooth = true;
			pjsua.set_snd_dev(0, 0);
		}else {
			Log.d(THIS_FILE, "Stopping bt");
			pjsua.set_no_snd_dev();
			userWantBluetooth = false;
			pjsua.set_snd_dev(0, 0);
			
		}
		broadcastMediaChanged();
	}
	
	public class MediaState {
		public boolean isMicrophoneMute = false;
		public boolean isSpeakerphoneOn = false;
		public boolean isBluetoothScoOn = false;
		public boolean canMicrophoneMute = true;
		public boolean canSpeakerphoneOn = true;
		public boolean canBluetoothSco = false;
		
		@Override
		public boolean equals(Object o) {
			
			if(o != null && o.getClass() == MediaState.class) {
				MediaState oState = (MediaState) o;
				if(oState.isBluetoothScoOn == isBluetoothScoOn &&
						oState.isMicrophoneMute == isMicrophoneMute &&
						oState.isSpeakerphoneOn == isSpeakerphoneOn &&
						oState.canBluetoothSco == canBluetoothSco &&
						oState.canSpeakerphoneOn == canSpeakerphoneOn &&
						oState.canMicrophoneMute == canMicrophoneMute) {
					return true;
				}else {
					return false;
				}
				
			}
			return super.equals(o);
		}
	}
	
	
	public MediaState getMediaState() {
		MediaState mediaState = new MediaState();
		
		// Micro 
		mediaState.isMicrophoneMute = audioManager.isMicrophoneMute();
		mediaState.canMicrophoneMute = Compatibility.isCompatible(5); /*&& !mediaState.isBluetoothScoOn*/ //Compatibility.isCompatible(5);
		
		// Speaker
		if(Compatibility.isCompatible(4)) {
			mediaState.isSpeakerphoneOn = audioManager.isSpeakerphoneOn();
		}else {
			mediaState.isSpeakerphoneOn = (audioManager.getRouting(audioManager.getMode()) == AudioManager.ROUTE_SPEAKER);
		}
		mediaState.canSpeakerphoneOn = true && !mediaState.isBluetoothScoOn; //Compatibility.isCompatible(5);
		
		//Bluetooth
		
		if(bluetoothClassAvailable) {
			mediaState.isBluetoothScoOn = bluetoothWrapper.isBluetoothOn();
			mediaState.canBluetoothSco = bluetoothWrapper.canBluetooth();
		}else {
			mediaState.isBluetoothScoOn = false;
			mediaState.canBluetoothSco = false;
		}
		
		return mediaState;
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
	}
}
