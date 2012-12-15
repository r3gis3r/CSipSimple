/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import com.csipsimple.service.HeadsetButtonReceiver;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;


@TargetApi(8)
public class AudioFocus8 extends AudioFocusWrapper{
	
	
	protected static final String THIS_FILE = "AudioFocus 8";
	private AudioManager audioManager;
	private SipService service;
	private ComponentName headsetButtonReceiverName;
	
	private boolean isFocused = false;
	
	private OnAudioFocusChangeListener focusChangedListener = new OnAudioFocusChangeListener() {
		
		@Override
		public void onAudioFocusChange(int focusChange) {
			Log.d(THIS_FILE, "Focus changed");
		}
	};
	
	public void init(SipService aService, AudioManager manager) {
		service = aService;
		audioManager = manager;
		headsetButtonReceiverName = new ComponentName(service.getPackageName(), 
				HeadsetButtonReceiver.class.getName());
	}

	
	public void focus(boolean userWantsBT) {
		Log.d(THIS_FILE, "Focus again "+isFocused);
		if(!isFocused) {
			HeadsetButtonReceiver.setService(service.getUAStateReceiver());
			audioManager.registerMediaButtonEventReceiver(headsetButtonReceiverName);
			audioManager.requestAudioFocus(focusChangedListener, 
					Compatibility.getInCallStream(userWantsBT), AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			isFocused = true;
		}
	}
	
	public void unFocus() {
		if(isFocused) {
			HeadsetButtonReceiver.setService(null);
			audioManager.unregisterMediaButtonEventReceiver(headsetButtonReceiverName);
			//TODO : when switch to speaker -> failure to re-gain focus then cause music player will wait before reasking focus
			audioManager.abandonAudioFocus(focusChangedListener);
			isFocused = false;
		}
	}

}
