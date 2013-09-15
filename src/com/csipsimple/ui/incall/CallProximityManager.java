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

package com.csipsimple.ui.incall;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.ui.incall.AccelerometerListener.OrientationListener;
import com.csipsimple.ui.incall.locker.ScreenLocker;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Class to manage proximity detection while in call.
 *
 */
public class CallProximityManager implements SensorEventListener, OrientationListener {
    private static final String THIS_FILE = "CallProximityManager";


    private Context mContext;
    

    private SensorManager sensorManager;
    private PowerManager powerManager;
    
    // Timeout management of screen locker ui
    private ScreenLocker mScreenLocker;
    private Boolean useTimeoutOverlay = null;
    
    // Self management of proximity sensor
    private Sensor proximitySensor;
    private static final float PROXIMITY_THRESHOLD = 5.0f;
    private boolean invertProximitySensor = false;
    private boolean proximitySensorTracked = false;
    private boolean isFirstRun = true;
    private ProximityDirector mDirector = null;
    
    // The hidden api that uses a wake lock
    private WakeLock proximityWakeLock;
    
    // The accelerometer
    private AccelerometerListener accelerometerManager;
    private int mOrientation;
    private boolean accelerometerEnabled = false;
    
    private int WAIT_FOR_PROXIMITY_NEGATIVE = 1;
    private final static int SCREEN_LOCKER_ACQUIRE_DELAY = "google_sdk".equals(Build.PRODUCT) ? ScreenLocker.WAIT_BEFORE_LOCK_LONG
            : ScreenLocker.WAIT_BEFORE_LOCK_SHORT;

    private static Method powerLockReleaseIntMethod;
    
    public interface ProximityDirector {
        public boolean shouldActivateProximity();
        public void onProximityTrackingChanged(boolean acquired);
    }
    
    CallProximityManager(Context context, ProximityDirector director, ScreenLocker screenLocker){
        mContext = context;
        mDirector = director;
        mScreenLocker = screenLocker;
        
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        accelerometerManager = new AccelerometerListener(context, this);
        
        // Try to detect the hidden api
        if (powerManager != null) {
            WifiManager wman = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo winfo = wman.getConnectionInfo();
            if (winfo == null ||
                    !SipConfigManager.getPreferenceBooleanValue(context, SipConfigManager.KEEP_AWAKE_IN_CALL)) {
                // Try to use powermanager proximity sensor
                try {
                    boolean supportProximity = false;
                    Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
                    int proximityScreenOffWakeLock = (Integer) f.get(null);
                    if(Compatibility.isCompatible(17)) {
                        // Changes of the private API on android 4.2
                        Method method = powerManager.getClass().getDeclaredMethod("isWakeLockLevelSupported", int.class);
                        supportProximity = (Boolean) method.invoke(powerManager, proximityScreenOffWakeLock);
                        Log.d(THIS_FILE, "Use 4.2 detection way for proximity sensor detection. Result is " + supportProximity);
                    }else {
                        Method method = powerManager.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
                        int supportedFlags = (Integer) method.invoke(powerManager);
                        Log.d(THIS_FILE, "Proxmity flags supported : " + supportedFlags);
                        supportProximity = ((supportedFlags & proximityScreenOffWakeLock) != 0x0);
                    }
                    if (supportProximity) {
                        Log.d(THIS_FILE, "We can use native screen locker !!");
                        proximityWakeLock = powerManager.newWakeLock(proximityScreenOffWakeLock,
                                "com.csipsimple.CallProximity");
                        proximityWakeLock.setReferenceCounted(false);
                    }
                    
                } catch (Exception e) {
                    Log.d(THIS_FILE, "Impossible to get power manager supported wake lock flags ");
                }
                if(powerLockReleaseIntMethod == null) {
                    try {
                        powerLockReleaseIntMethod = proximityWakeLock.getClass().getDeclaredMethod(
                                "release", int.class);
                        
                    }catch (Exception e) {
                        Log.d(THIS_FILE, "Impossible to get power manager release with it");
                    }
                }
            }
        }

        // Try to detect a proximity sensor as fallback
        if(proximityWakeLock == null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            invertProximitySensor = SipConfigManager.getPreferenceBooleanValue(context, SipConfigManager.INVERT_PROXIMITY_SENSOR);
        }
        
    }

    public synchronized void startTracking() {
        // If we should manage it ourselves
        if (proximitySensor != null && !proximitySensorTracked) {
            // Fall back to manual mode
            isFirstRun = true;
            Log.d(THIS_FILE, "Register sensor");
            sensorManager.registerListener(this,
                    proximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            proximitySensorTracked = true;
        }
        if(!accelerometerEnabled) {
            accelerometerManager.enable(true);
            accelerometerEnabled = true;
        }
    }
    

    public synchronized void stopTracking() {
        if (proximitySensor != null && proximitySensorTracked) {
            proximitySensorTracked = false;
            sensorManager.unregisterListener(this);
            Log.d(THIS_FILE, "Unregister to sensor is done !!!");
        }
        if(accelerometerEnabled) {
            accelerometerManager.enable(false);
            accelerometerEnabled = false;
        }
        mScreenLocker.tearDown();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Log.d(THIS_FILE, "Tracked : "+proximitySensorTracked);
        if (proximitySensorTracked && !isFirstRun) {
            float distance = event.values[0];
            boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor
                    .getMaximumRange());
            if (invertProximitySensor) {
                active = !active;
            }
            Log.d(THIS_FILE, "Distance is now " + distance);
            
            boolean isValidCallState = false;
            if(mDirector != null) {
                isValidCallState = mDirector.shouldActivateProximity();
            }
            
            if (isValidCallState && active) {
                mScreenLocker.show();
                if(mDirector != null) {
                    mDirector.onProximityTrackingChanged(true);
                }
            } else {
                mScreenLocker.hide();
                if(mDirector != null) {
                    mDirector.onProximityTrackingChanged(false);
                }
            }

        }
        if (isFirstRun) {
            isFirstRun = false;
        }
    }

    private boolean isProximityWakeHeld = false;
    /**
     * Release any lock taken by the proximity sensor
     */
    public synchronized void release(int flag) {
        if (proximityWakeLock != null && isProximityWakeHeld) {
            boolean usedNewRelease = false;
            if(powerLockReleaseIntMethod != null) {
                try {
                    powerLockReleaseIntMethod.invoke(proximityWakeLock, flag);
                    usedNewRelease = true;
                    //Log.d(THIS_FILE, "CALL NEW RELEASE WITH FLAG " + flag);
                } catch (Exception e) {
                    Log.d(THIS_FILE, "Error calling new release method ", e);
                }
            }
            if(!usedNewRelease) {
                proximityWakeLock.release();
            }
            isProximityWakeHeld = false;
        }

        if(shouldUseTimeoutOverlay()) {
            mScreenLocker.hide();
        }
        // Notify
        if(mDirector != null) {
            mDirector.onProximityTrackingChanged(false);
        }
    }

    public synchronized void acquire() {
        if (proximityWakeLock != null && !isProximityWakeHeld) {
            proximityWakeLock.acquire();
            isProximityWakeHeld = true;
        }
        if(shouldUseTimeoutOverlay()) {
            mScreenLocker.delayedLock(SCREEN_LOCKER_ACQUIRE_DELAY);
        }
        // Notify
        if(mDirector != null) {
            mDirector.onProximityTrackingChanged(true);
        }
    }
    

    /**
     * Update proximity lock mode depending on current state
     */
    public synchronized void updateProximitySensorMode() {


        // We do not keep the screen off when the user is outside in-call screen and we are
        // horizontal, but we do not force it on when we become horizontal until the
        // proximity sensor goes negative.
        boolean horizontal =
                (mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL);

        boolean activeRegardingCalls = false;
        if(mDirector != null) {
            activeRegardingCalls = mDirector.shouldActivateProximity();
        }
        
        Log.d(THIS_FILE, "Horizontal : " + horizontal + " and activate for calls " + activeRegardingCalls);
        if (activeRegardingCalls && !horizontal) {
            // Phone is in use! Arrange for the screen to turn off
            // automatically when the sensor detects a close object.
            acquire();
        } else {
            // Phone is either idle, or ringing. We don't want any
            // special proximity sensor behavior in either case.
            int flags =
                    (!horizontal ? 0 : WAIT_FOR_PROXIMITY_NEGATIVE);
            release(flags);
        }
    }
    

    /**
     * Should the application display the overlay after a timeout.
     * @return false if we are in table mode or if proximity sensor can be used
     */
    private boolean shouldUseTimeoutOverlay() {
        if(useTimeoutOverlay == null) {
            useTimeoutOverlay = proximitySensor == null &&
                    proximityWakeLock == null &&
                    !Compatibility.isTabletScreen(mContext);
        }
        return useTimeoutOverlay;
    }
    
    public void restartTimer() {
        if (shouldUseTimeoutOverlay()) {
            mScreenLocker.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
        }
    }

    @Override
    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode();
    }


    
}
