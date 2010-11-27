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
package com.csipsimple.utils.audio;

import com.csipsimple.service.HeadsetButtonReceiver;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

public class AudioFocus3 extends AudioFocusWrapper {
	

	final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
	final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
	private static final String THIS_FILE = "AudioFocus3";
	
	private AudioManager audioManager;
	private SipService service;
	
	private boolean isMusicActive = false;
	private boolean isFocused = false;
	private HeadsetButtonReceiver headsetButtonReceiver;
	
	public void init(SipService aService, AudioManager manager) {
		service = aService;
		audioManager = manager;
	}

	public void focus() {
		if(!isFocused) {
			pauseMusic();
			registerHeadsetButton();
			isFocused = true;
		}
	}
	
	public void unFocus() {
		if(isFocused) {
			restartMusic();
			unregisterHeadsetButton();
			isFocused = false;
		}
	}

	
	private void pauseMusic() {
		isMusicActive = audioManager.isMusicActive();
		if(isMusicActive && service.prefsWrapper.integrateWithMusicApp()) {
			service.sendBroadcast(new Intent(PAUSE_ACTION));
		}
	}
	
	private void restartMusic() {
		if(isMusicActive && service.prefsWrapper.integrateWithMusicApp()) {
			service.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
		}
	}
	
	private void registerHeadsetButton() {
		Log.d(THIS_FILE, "Register media button");
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
		intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY +100 );
		if(headsetButtonReceiver == null) {
			headsetButtonReceiver = new HeadsetButtonReceiver();
			HeadsetButtonReceiver.setService(SipService.getUAStateReceiver());
		}
		service.registerReceiver(headsetButtonReceiver, intentFilter);
	}
	
	private void unregisterHeadsetButton() {
		try {
			service.unregisterReceiver(headsetButtonReceiver);
			HeadsetButtonReceiver.setService(null);
			headsetButtonReceiver = null;
		}catch(Exception e) {
			//Nothing to do else. just consider it has not been registered
		}
	}
}
