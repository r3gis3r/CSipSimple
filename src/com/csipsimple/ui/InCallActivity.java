/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2008 The Android Open Source Project
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
package com.csipsimple.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua_call_media_status;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.service.MediaManager.MediaState;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.InCallControls;
import com.csipsimple.widgets.InCallInfo;
import com.csipsimple.widgets.ScreenLocker;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.InCallControls.OnTriggerListener;


public class InCallActivity extends Activity implements OnTriggerListener, OnDialKeyListener, SensorEventListener, com.csipsimple.widgets.SlidingTab.OnTriggerListener {
	private static String THIS_FILE = "SIP CALL HANDLER";

	private CallInfo callInfo = null;
	private FrameLayout mainFrame;
	private InCallControls inCallControls;
	private InCallInfo inCallInfo;
	private ScreenLocker lockOverlay;

	//Screen wake lock for incoming call
	private WakeLock wakeLock;
	//Keygard for incoming call
	private boolean manageKeyguard = false;
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;

	private Dialpad dialPad;
	private LinearLayout dialPadContainer;
	private EditText dialPadTextView;

	private View callInfoPanel;
	private Timer quitTimer;

	private LinearLayout detailedContainer, holdContainer;

	//True if running unit tests
//	private boolean inTest;

	private MediaState lastMediaState;

	private SensorManager sensorManager;
	private Sensor proximitySensor;

	private DialingFeedback dialFeedback;

	private boolean proximitySensorTracked = false;

	private PowerManager powerManager;

	private WakeLock proximityWakeLock;

	private PreferencesWrapper prefsWrapper;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.in_call_main);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			callInfo = (CallInfo) extras.get("call_info");
		}

		if (callInfo == null) {
			Log.e(THIS_FILE, "You provide an empty call info....");
			finish();
			return;
		}
		
		Log.d(THIS_FILE, "Creating call handler for " + callInfo.getCallId()+" state "+callInfo.getRemoteContact());
		/*
		inTest = extras.getBoolean("in_test", false);
		if(!inTest) {
		*/
			Log.d(THIS_FILE, "Creating call handler.....");
			serviceBound = bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
			/*
		}
		*/
			
		prefsWrapper = new PreferencesWrapper(this);

		Log.d(THIS_FILE, "Creating call handler for " + callInfo.getCallId()+" state "+callInfo.getRemoteContact());
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
		
		takeKeyEvents(true);
		

		//remoteContact = (TextView) findViewById(R.id.remoteContact);
		mainFrame = (FrameLayout) findViewById(R.id.mainFrame);
		inCallControls = (InCallControls) findViewById(R.id.inCallControls);
		inCallControls.setOnTriggerListener(this);
		
		inCallInfo = (InCallInfo) findViewById(R.id.inCallInfo);
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		dialPad.setOnDialKeyListener(this);
		dialPadContainer = (LinearLayout) findViewById(R.id.dialPadContainer);
		dialPadTextView = (EditText) findViewById(R.id.digitsText);
		callInfoPanel = (View) findViewById(R.id.callInfoPanel);
		
		detailedContainer = (LinearLayout) findViewById(R.id.detailedContainer);
		holdContainer = (LinearLayout) findViewById(R.id.holdContainer);
		
		lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
		lockOverlay.setActivity(this, this);
		
		
		registerReceiver(callStateReceiver, new IntentFilter(SipService.ACTION_SIP_CALL_CHANGED));
		registerReceiver(callStateReceiver, new IntentFilter(SipService.ACTION_SIP_MEDIA_CHANGED));
		
		// Sensor management
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		Log.d(THIS_FILE, "Proximty sensor : "+proximitySensor);
		
		dialFeedback = new DialingFeedback(this, true);
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
        if (keyguardManager == null) {
            keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("com.csipsimple.inCallKeyguard");
        }
        
        // If this line is uncommented keyguard will be prevented only if in keyguard mode is locked 
        // when incoming call arrives
        //if(keyguardManager.inKeyguardRestrictedInputMode()) {
        
        manageKeyguard = true;
        keyguardLock.disableKeyguard();
        //}
        
        if(quitTimer == null) {
    		quitTimer = new Timer();
        }
        
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(manageKeyguard) {
			keyguardLock.reenableKeyguard();
		}
		if(quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		
		lockOverlay.tearDown();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateUIFromCall();
		if(proximitySensor != null) {
			WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo == null || !prefsWrapper.keepAwakeInCall()) {
				// Try to use powermanager proximity sensor
				if(powerManager != null) {
					try {
						Method method = powerManager.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
						int supportedFlags = (Integer) method.invoke(powerManager);
						Log.d(THIS_FILE, ">>> Flags supported : "+supportedFlags);
						Field f = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
						int proximityScreenOffWakeLock = (Integer) f.get(null);
						if( (supportedFlags & proximityScreenOffWakeLock) != 0x0 ) {
							Log.d(THIS_FILE, ">>> We can use native screen locker !!");
							proximityWakeLock = powerManager.newWakeLock(proximityScreenOffWakeLock, "com.csipsimple.CallProximity");
							proximityWakeLock.setReferenceCounted(false);
						}
						
					} catch (Exception e) {
						Log.d(THIS_FILE, "Impossible to get power manager supported wake lock flags");
					} 
					/*
					if ((powerManager.getSupportedWakeLockFlags()  & PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) != 0x0) {
						mProximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, THIS_FILE);
					}
					*/
				}
			}
			
			if(proximityWakeLock == null) {
				//Fall back to manual mode
				isFirstRun = true;
				sensorManager.registerListener(this, 
		                proximitySensor,
		                SensorManager.SENSOR_DELAY_NORMAL);
				proximitySensorTracked  = true;
			}
			
		}
		dialFeedback.resume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(proximitySensor != null) {
			proximitySensorTracked = false;
			sensorManager.unregisterListener(this);
		}
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		dialFeedback.pause();
	}

	private synchronized void updateUIFromCall() {

		Log.d(THIS_FILE, "Update ui from call " + callInfo.getCallId() + " state " + callInfo.getStringCallState(this));
		
		pjsip_inv_state state = callInfo.getCallState();


		//Update in call actions
		inCallInfo.setCallState(callInfo);
		inCallControls.setCallState(callInfo);
		
		
		int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
		
		Log.d(THIS_FILE, "Manage wake lock");
        
        
        
		switch (state) {
		case PJSIP_INV_STATE_INCOMING:
		case PJSIP_INV_STATE_EARLY:
		case PJSIP_INV_STATE_CALLING:
			Log.d(THIS_FILE, "Acquire wake up lock");
			if(wakeLock != null && !wakeLock.isHeld()) {
				wakeLock.acquire();
			}
			if(proximitySensor == null && proximityWakeLock == null) {
				lockOverlay.hide();
			}
			if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
				proximityWakeLock.release();
			}
			break;
		case PJSIP_INV_STATE_CONFIRMED:
			backgroundResId = R.drawable.bg_in_call_gradient_connected;
			if(lastMediaState != null && lastMediaState.isBluetoothScoOn) {
				backgroundResId = R.drawable.bg_in_call_gradient_bluetooth;
			}
			if (wakeLock != null && wakeLock.isHeld()) {
				Log.d(THIS_FILE, "Releasing wake up lock - confirmed");
                wakeLock.release();
            }
			if(proximitySensor == null && proximityWakeLock == null) {
				lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_START);
			}
			
			if(proximityWakeLock != null && !proximityWakeLock.isHeld()) {
				proximityWakeLock.acquire();
			}
			
			break;
		case PJSIP_INV_STATE_NULL:
			Log.i(THIS_FILE, "WTF?");
		case PJSIP_INV_STATE_DISCONNECTED:
			if (wakeLock != null && wakeLock.isHeld()) {
				Log.d(THIS_FILE, "Releasing wake up lock");
                wakeLock.release();
            }
			if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
				proximityWakeLock.release();
			}
			
			//Set background to red and delay quit
			delayedQuit();
			return;
		case PJSIP_INV_STATE_CONNECTING:
			
			break;
		}
		
		pjsua_call_media_status mediaStatus = callInfo.getMediaStatus();
		switch (mediaStatus) {
		case PJSUA_CALL_MEDIA_ACTIVE:
			break;
		case PJSUA_CALL_MEDIA_REMOTE_HOLD:
		case PJSUA_CALL_MEDIA_LOCAL_HOLD:
		case PJSUA_CALL_MEDIA_NONE:
			if(backgroundResId == R.drawable.bg_in_call_gradient_connected ||
					backgroundResId == R.drawable.bg_in_call_gradient_bluetooth) {
				backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
			}
			break;
		case PJSUA_CALL_MEDIA_ERROR:
		default:
			break;
		}
		
		
		mainFrame.setBackgroundResource(backgroundResId);
		
		
		Log.d(THIS_FILE, "we leave the update ui function");
	}
	
	
	private synchronized void updateUIFromMedia() {
		if(SipService.mediaManager != null) {
			MediaState mediaState = SipService.mediaManager.getMediaState();
			Log.d(THIS_FILE, "Media update ....");
			if(!mediaState.equals(lastMediaState)) {
				lastMediaState = mediaState;
				
				if(callInfo != null) {
					pjsip_inv_state state = callInfo.getCallState();
					
					// Background
					if(state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
						mainFrame.setBackgroundResource(lastMediaState.isBluetoothScoOn?R.drawable.bg_in_call_gradient_bluetooth:R.drawable.bg_in_call_gradient_connected);
					}
				}
				
				// Actions
				inCallControls.setMediaState(lastMediaState);
			}
		}
	}
	
	private void delayedQuit() {
		//Update ui
		lockOverlay.hide();
		setDialpadVisibility(View.GONE);
		callInfoPanel.setVisibility(View.VISIBLE);
		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);
		
		
		if(quitTimer != null) {
			quitTimer.schedule(new QuitTimerTask(), 3000);
		}else {
			finish();
		}
	}
	
	private class QuitTimerTask extends TimerTask{
		@Override
		public void run() {
			finish();
		}
	};
	
	
	private void setDialpadVisibility(int visibility) {
		dialPadContainer.setVisibility(visibility);
		int antiVisibility = visibility == View.GONE? View.VISIBLE:View.GONE;
		detailedContainer.setVisibility(antiVisibility);
		holdContainer.setVisibility(antiVisibility);
		callInfoPanel.setVisibility(antiVisibility);
	}
	
	
	
	@Override
	protected void onDestroy() {
		if (serviceBound) {
			unbindService(connection);
			serviceBound = false;
		}
		
		if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
		try {
			unregisterReceiver(callStateReceiver);
		}catch (IllegalArgumentException e) {
			//That's the case if not registered (early quit)
		}
		super.onDestroy();
	}
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	//
    		// Volume has been adjusted by the user.
    		//
        	Log.d(THIS_FILE, "onKeyDown: Volume button pressed");
        	int action = AudioManager.ADJUST_RAISE;
        	if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
        		action = AudioManager.ADJUST_LOWER;
        	}
        	if(SipService.mediaManager != null) {
        		SipService.mediaManager.adjustStreamVolume(Compatibility.getInCallStream(), action, AudioManager.FLAG_SHOW_UI);
        	}
        	return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(SipService.ACTION_SIP_CALL_CHANGED)){
				Bundle extras = intent.getExtras();
				CallInfo notif_call = null;
				if (extras != null) {
					notif_call = (CallInfo) extras.get("call_info");
				}
	
				if (notif_call != null && callInfo.equals(notif_call)) {
					callInfo = notif_call;
					updateUIFromCall();
				}
			}else if(action.equals(SipService.ACTION_SIP_MEDIA_CHANGED)) {
				updateUIFromMedia();
			}
		}
	};
	
	
	/**
	 * Service binding
	 */
	private boolean serviceBound = false;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			//Check if currently in use call is not already invalid (could be the case for example if we are not authorized to make the call)
			//There is a time between when the call change notification that starts the InCallActivity and
			//when this view registers the on ua call state changed
			CallInfo realCallInfo;
			try {
				Log.d(THIS_FILE, "Service started get real call info "+callInfo.getCallId());
				realCallInfo = service.getCallInfo(callInfo.getCallId());
				callInfo = realCallInfo;
				if(callInfo == null) {
					finish();
				}else {
					Log.d(THIS_FILE, "Real call info "+callInfo.getCallId());
					updateUIFromCall();
					updateUIFromMedia();
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
	};


	private boolean showDetails = true;

	private boolean isFirstRun = true;

	@Override
	public void onTrigger(int whichAction) {
		Log.d(THIS_FILE, "In Call Activity is triggered");
		Log.d(THIS_FILE, "We have a call info : "+callInfo);
		Log.d(THIS_FILE, "And a service : "+service);
		if(proximitySensor == null && proximityWakeLock == null) {
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
		}
		try {
			switch(whichAction) {
				case TAKE_CALL:{
					if (callInfo != null && service != null) {
						service.answer(callInfo.getCallId(), pjsip_status_code.PJSIP_SC_OK.swigValue());
					}
					break;
				}
				case DECLINE_CALL: 
				case CLEAR_CALL:
				{
					if (callInfo != null && service != null) {
						service.hangup(callInfo.getCallId(), 0);
					}
					break;
				}
				case MUTE_ON:
				case MUTE_OFF:
				{
					if (callInfo != null && service != null) {
						service.setMicrophoneMute((whichAction == MUTE_ON)?true:false);
					}
					break;
				}
				case SPEAKER_ON :
				case SPEAKER_OFF :
				{
					if (callInfo != null && service != null) {
						service.setSpeakerphoneOn((whichAction == SPEAKER_ON)?true:false);
					}
					break;
				}
				case BLUETOOTH_ON:
				case BLUETOOTH_OFF: {
					if (callInfo != null && service != null) {
						service.setBluetoothOn((whichAction == BLUETOOTH_ON)?true:false);
					}
					break;
				}
				case DIALPAD_ON:
				case DIALPAD_OFF:
				{
					setDialpadVisibility((whichAction == DIALPAD_ON)?View.VISIBLE:View.GONE);
					break;
				}
				case DETAILED_DISPLAY:{
					inCallInfo.switchDetailedInfo( showDetails );
					showDetails = !showDetails;
					break;
				}
				case TOGGLE_HOLD:{
					if (callInfo != null && service != null) {
						Log.d(THIS_FILE, "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
						if(callInfo.getMediaStatus().equals(pjsua_call_media_status.PJSUA_CALL_MEDIA_LOCAL_HOLD) ||
								callInfo.getMediaStatus().equals(pjsua_call_media_status.PJSUA_CALL_MEDIA_NONE)) {
							service.reinvite(callInfo.getCallId(), true);
						}else {
							service.hold(callInfo.getCallId());
						}
					}
				}
			}
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Was not able to call service method", e);
		}
	}
	

	@Override
	public void onTrigger(int keyCode, int dialTone) {
		if (callInfo != null && service != null) {
			try {
				service.sendDtmf(callInfo.getCallId(), keyCode);
				dialFeedback.giveFeedback(dialTone);
				KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
				char nbr = event.getNumber();
				dialPadTextView.getText().append(nbr);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Was not able to send dtmf tone", e);
			}
		}
		
	}

	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}


	private static final float PROXIMITY_THRESHOLD = 5.0f;
	@Override
	public void onSensorChanged(SensorEvent event) {
		Log.d(THIS_FILE, "Tracked : "+proximitySensorTracked);
		if(proximitySensorTracked && !isFirstRun) {
			pjsip_inv_state state = callInfo.getCallState();
			float distance = event.values[0];
			boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
			Log.d(THIS_FILE, "Distance is now "+distance);
			boolean isValidCallState = state.equals( pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) || 
										state.equals(pjsip_inv_state.PJSIP_INV_STATE_CONNECTING)|| 
										state.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING)|| 
										state.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY);
			if( isValidCallState && active) {
				lockOverlay.show();
			}else {
				lockOverlay.hide();
			}
		}
		if(isFirstRun) {
			isFirstRun = false;
		}
	}


	@Override
	public void onTrigger(View v, int whichHandle) {
		switch (whichHandle) {
		case LEFT_HANDLE:
			Log.d(THIS_FILE, "We unlock");
			lockOverlay.hide();
			lockOverlay.reset();
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
			break;
		case RIGHT_HANDLE:
			Log.d(THIS_FILE, "We clear the call");
			onTrigger(OnTriggerListener.CLEAR_CALL);
			lockOverlay.reset();
		default:
			break;
		}
		
	}
	

	
}
