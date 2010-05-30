/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2006 The Android Open Source Project
 * 
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
package com.csipsimple.utils;

import com.csipsimple.service.UAStateReceiver;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
/**
 * Ringer manager for the Phone app.
 */
public class Ringer {
    private static final String THIS_FILE = "Ringer";
   
    private static final int PLAY_RING_ONCE = 1;
    private static final int STOP_RING = 3;

    private static final int VIBRATE_LENGTH = 1000; // ms
    private static final int PAUSE_LENGTH = 1000; // ms

    // Uri for the ringtone.
    Uri customRingtoneUri;

    Ringtone ringtone;
    Vibrator vibrator ;
    volatile boolean mContinueVibrating;
    VibratorThread vibratorThread;
    Context context;
    private Worker ringThread;
    private Handler ringHandler;
    private boolean ringPending;
    private long firstRingEventTime = -1;
    private long firstRingStartTime = -1;

    public Ringer(Context aContext) {
        context = aContext;
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    //    mHardwareService = IHardwareService.Stub.asInterface(ServiceManager.getService("hardware"));
    }

    /**
     * @return true if we're playing a ringtone and/or vibrating
     *     to indicate that there's an incoming call.
     *     ("Ringing" here is used in the general sense.  If you literally
     *     need to know if we're playing a ringtone or vibrating, use
     *     isRingtonePlaying() or isVibrating() instead.)
     *
     * @see isVibrating
     * @see isRingtonePlaying
     */
    public boolean isRinging() {
        synchronized (this) {
            return (isRingtonePlaying() || isVibrating());
        }
    }

    /**
     * @return true if the ringtone is playing
     * @see isVibrating
     * @see isRinging
     */
    private boolean isRingtonePlaying() {
        synchronized (this) {
            return (ringtone != null && ringtone.isPlaying()) ||
                    (ringHandler != null && ringHandler.hasMessages(PLAY_RING_ONCE));
        }
    }

    /**
     * @return true if we're vibrating in response to an incoming call
     * @see isVibrating
     * @see isRinging
     */
    private boolean isVibrating() {
        synchronized (this) {
            return (vibratorThread != null);
        }
    }

    /**
     * Starts the ringtone and/or vibrator
     */
    public void ring() {
        Log.d(THIS_FILE, "ring()...");

        synchronized (this) {

            if (shouldVibrate() && vibratorThread == null) {
                mContinueVibrating = true;
                vibratorThread = new VibratorThread();
                Log.d(THIS_FILE, "- starting vibrator...");
                vibratorThread.start();
            }
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                Log.d(THIS_FILE, "skipping ring because volume is zero");
                return;
            }

            if (!isRingtonePlaying() && !ringPending) {
                makeLooper();
                ringHandler.removeCallbacksAndMessages(null);
                ringPending = true;
                if (firstRingEventTime < 0) {
                    firstRingEventTime = SystemClock.elapsedRealtime();
                    ringHandler.sendEmptyMessage(PLAY_RING_ONCE);
                } else {
                    // For repeat rings, figure out by how much to delay
                    // the ring so that it happens the correct amount of
                    // time after the previous ring
                    if (firstRingStartTime > 0) {
                        // Delay subsequent rings by the delta between event
                        // and play time of the first ring
                        Log.d(THIS_FILE, "delaying ring by " + (firstRingStartTime - firstRingEventTime));
                        
                        ringHandler.sendEmptyMessageDelayed(PLAY_RING_ONCE,
                                firstRingStartTime - firstRingEventTime);
                    } else {
                        // We've gotten two ring events so far, but the ring
                        // still hasn't started. Reset the event time to the
                        // time of this event to maintain correct spacing.
                        firstRingEventTime = SystemClock.elapsedRealtime();
                    }
                }
            } else {
                Log.d(THIS_FILE ,"skipping ring, already playing or pending: "
                             + ringtone + "/" + ringHandler);
            }
        }
    }

    boolean shouldVibrate() {
    	return true;
     //   AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
     //   return audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER);
    }

    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    public void stopRing() {
        synchronized (this) {
            Log.d(THIS_FILE, "stopRing()...");

            if (ringHandler != null) {
                ringHandler.removeCallbacksAndMessages(null);
                Message msg = ringHandler.obtainMessage(STOP_RING);
                msg.obj = ringtone;
                ringHandler.sendMessage(msg);
                ringThread = null;
                ringHandler = null;
                ringtone = null;
                firstRingEventTime = -1;
                firstRingStartTime = -1;
                ringPending = false;
            } 

            if (vibratorThread != null) {
                mContinueVibrating = false;
                vibratorThread = null;
            }
            // Also immediately cancel any vibration in progress.
            vibrator.cancel();
        }
    }

    private class VibratorThread extends Thread {
        public void run() {
            while (mContinueVibrating) {
                vibrator.vibrate(VIBRATE_LENGTH);
                SystemClock.sleep(VIBRATE_LENGTH + PAUSE_LENGTH);
            }
        }
    }
    private class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            mLooper.quit();
        }
    }

    /**
     * Sets the ringtone uri in preparation for ringtone creation
     * in makeLooper().  This uri is defaulted to the phone-wide
     * default ringtone.
     */
    public void setCustomRingtoneUri (Uri uri) {
        if (uri != null) {
            customRingtoneUri = uri;
        }
    }

    private void makeLooper() {
        if (ringThread == null) {
            ringThread = new Worker("ringer");
            ringHandler = new Handler(ringThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Ringtone r = null;
                    switch (msg.what) {
                        case PLAY_RING_ONCE:
                            if (ringtone == null && !hasMessages(STOP_RING)) {
                                // create the ringtone with the uri
                                r = RingtoneManager.getRingtone(context, customRingtoneUri);
                                synchronized (Ringer.this) {
                                    if (!hasMessages(STOP_RING)) {
                                        ringtone = r;
                                    }
                                }
                            }
                            r = ringtone;
                            if (r != null && !hasMessages(STOP_RING)) {
                                r.play();
                                synchronized (Ringer.this) {
                                    ringPending = false;
                                    if (firstRingStartTime < 0) {
                                        firstRingStartTime = SystemClock.elapsedRealtime();
                                    }
                                }
                            }
                            break;
                        case STOP_RING:
                            r = (Ringtone) msg.obj;
                            if (r != null) {
                                r.stop();
                            }
                            getLooper().quit();
                            break;
                    }
                }
            };
        }
    }

}
