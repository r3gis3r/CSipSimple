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

import android.content.ComponentName;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import com.csipsimple.service.HeadsetButtonReceiver;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;


public class AudioFocus8 {
	
	
	private AudioManager audioManager;
	private SipService service;
	private ComponentName headsetButtonReceiverName;
	
	private boolean isFocused = false;
	
	public AudioFocus8(SipService aService, AudioManager manager) {
		service = aService;
		audioManager = manager;
		headsetButtonReceiverName = new ComponentName(service.getPackageName(), 
				HeadsetButtonReceiver.class.getName());
	}

	
	public void focus() {
		if(!isFocused) {
			HeadsetButtonReceiver.setService(SipService.getUAStateReceiver());
			audioManager.registerMediaButtonEventReceiver(headsetButtonReceiverName);
			audioManager.requestAudioFocus((OnAudioFocusChangeListener) null, 
					Compatibility.getInCallStream(), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			isFocused = true;
		}
	}
	
	public void unFocus() {
		if(isFocused) {
			HeadsetButtonReceiver.setService(null);
			audioManager.unregisterMediaButtonEventReceiver(headsetButtonReceiverName);
			audioManager.abandonAudioFocus((OnAudioFocusChangeListener) null);
			isFocused = false;
		}
	}

}
