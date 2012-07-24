/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2010 Robert B. Denny, Mesa, AZ, USA
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
/**
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2008 The Android Open Source Project
 */


package com.csipsimple.utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Vibrator;

import java.util.Timer;
import java.util.TimerTask;

public class DialingFeedback {

	/** The length of vibrate (haptic) feedback in milliseconds */
	private static final int HAPTIC_LENGTH_MS = 50;
	
    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    private boolean inCall;
    private int toneStream;
    private Activity context;
    
	private ToneGenerator toneGenerator = null;
	private Object toneGeneratorLock = new Object();
	private Vibrator vibrator = null;
	private Timer toneTimer = null;

	private PreferencesWrapper prefsWrapper;
	private boolean dialPressTone = false;
	private boolean dialPressVibrate = false;

	private int ringerMode;

	public DialingFeedback(Activity context, boolean inCall) {
		
		this.context = context;
		this.inCall = inCall;
		toneStream = inCall ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;
		prefsWrapper = new PreferencesWrapper(context);
	}
	
	public void resume() {
		
		dialPressTone = prefsWrapper.dialPressTone();
		dialPressVibrate = prefsWrapper.dialPressVibrate();
		
		if(dialPressTone ) {
			//Create dialtone just for user feedback
			synchronized (toneGeneratorLock) {
				if(toneTimer == null) {
					toneTimer = new Timer("Dialtone-timer");
				}
				if (toneGenerator == null) {
					try {
						toneGenerator = new ToneGenerator(toneStream, TONE_RELATIVE_VOLUME);
						//Allow user to control dialtone
						if (!inCall) {
						    context.setVolumeControlStream(toneStream);
						}
					} catch (RuntimeException e) {
						//If impossible, nothing to do
						toneGenerator = null;
					}
				}
			}
		} else {
			toneTimer = null;
			toneGenerator = null;
		}
		
		//Create the vibrator
		if (dialPressVibrate) {
			if (vibrator == null) {
				vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			}
		} else {
			vibrator = null;
		}

		//Store the current ringer mode
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		ringerMode = am.getRingerMode();

	}
	
	public void pause() {

		//Destroy dialtone
		synchronized (toneGeneratorLock) {
			if (toneGenerator != null) {
				toneGenerator.release();
				toneGenerator = null;
			}
			if(toneTimer != null) {
				toneTimer.cancel();
				toneTimer.purge();
				toneTimer = null;
			}
		}
		
	}

	public void giveFeedback(int tone) {
		
		switch (ringerMode) {
			case AudioManager.RINGER_MODE_NORMAL:
				if (dialPressVibrate) {
				    vibrator.vibrate(HAPTIC_LENGTH_MS);
				}
				if (dialPressTone) {
				    ThreadedTonePlay threadedTone = new ThreadedTonePlay(tone);
				    threadedTone.start();
				    toneTimer.schedule(new StopTimerTask(), TONE_LENGTH_MS);
				}
				break;
			case AudioManager.RINGER_MODE_VIBRATE:
				if (dialPressVibrate) {
				    vibrator.vibrate(HAPTIC_LENGTH_MS);
				}
				break;
			case AudioManager.RINGER_MODE_SILENT:
				break;
		}
	}
	
	public void hapticFeedback() {
		if (dialPressVibrate && ringerMode != AudioManager.RINGER_MODE_SILENT) {
			vibrator.vibrate(HAPTIC_LENGTH_MS);
		}
	}
	
	class ThreadedTonePlay extends Thread {
	    private final int tone;
        
	    ThreadedTonePlay(int t){
	        tone = t;
	    }
	    
	    @Override
	    public void run() {
            synchronized (toneGeneratorLock) {
                if (toneGenerator == null) {
                    return;
                }
                toneGenerator.startTone(tone);
            }
	    }
	}
	
	class StopTimerTask extends TimerTask{
		@Override
		public void run() {
			synchronized (toneGeneratorLock) {
				if (toneGenerator == null) {
					return;
				}
				toneGenerator.stopTone();
			}
		}
	}

}
