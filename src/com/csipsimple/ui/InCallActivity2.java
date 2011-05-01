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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.pjsip.PjSipCalls;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.InCallControls2;
import com.csipsimple.widgets.InCallControls2.OnTriggerListener;
import com.csipsimple.widgets.InCallInfo2;
import com.csipsimple.widgets.ScreenLocker;


public class InCallActivity2 extends Activity implements OnTriggerListener, OnDialKeyListener, SensorEventListener, com.csipsimple.widgets.SlidingTab.OnTriggerListener {
	private static String THIS_FILE = "SIP CALL HANDLER";
	private static final int DRAGGING_DELAY = 150;

	private SipCallSession[] callsInfo = null;
	private FrameLayout mainFrame;
	private InCallControls2 inCallControls;
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
	

	private HashMap<Integer, InCallInfo2> badges = new HashMap<Integer, InCallInfo2>();

	private ViewGroup callInfoPanel;
	private Timer quitTimer;

//	private LinearLayout detailedContainer, holdContainer;

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
	
	// Dnd views
	private ImageView endCallTarget;
	private Rect endCallTargetRect;
	private ImageView holdTarget, answerTarget, xferTarget;
	private Rect holdTargetRect, answerTargetRect, xferTargetRect;
	private Button middleAddCall;
	
	

	private static DisplayMetrics METRICS;
	
	private final static int PICKUP_SIP_URI_XFER = 0;
	private final static int PICKUP_SIP_URI_NEW_CALL = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(THIS_FILE, "Create in call");
		setContentView(R.layout.in_call_main2);
		
		METRICS = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(METRICS);
		
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		prefsWrapper = new PreferencesWrapper(this);
		INVERT_PROXIMITY_SENSOR = prefsWrapper.invertProximitySensor();

//		Log.d(THIS_FILE, "Creating call handler for " + callInfo.getCallId()+" state "+callInfo.getRemoteContact());
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
		
		takeKeyEvents(true);
		

		//remoteContact = (TextView) findViewById(R.id.remoteContact);
		mainFrame = (FrameLayout) findViewById(R.id.mainFrame);
		inCallControls = (InCallControls2) findViewById(R.id.inCallControls);
		inCallControls.setOnTriggerListener(this);
		
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		dialPad.setOnDialKeyListener(this);
		dialPadContainer = (LinearLayout) findViewById(R.id.dialPadContainer);
		dialPadTextView = (EditText) findViewById(R.id.digitsText);
		callInfoPanel = (ViewGroup) findViewById(R.id.callInfoPanel);
		
		lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
		lockOverlay.setActivity(this, this);
		
		endCallTarget = (ImageView) findViewById(R.id.dropHangup);
		endCallTarget.getBackground().setDither(true);
		holdTarget = (ImageView) findViewById(R.id.dropHold);
		holdTarget.getBackground().setDither(true);
		answerTarget = (ImageView) findViewById(R.id.dropAnswer);
		answerTarget.getBackground().setDither(true);
		xferTarget = (ImageView) findViewById(R.id.dropXfer);
		xferTarget.getBackground().setDither(true);
		
		middleAddCall = (Button) findViewById(R.id.add_call_button);
		middleAddCall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				onTrigger(ADD_CALL, null);
			}
		});
		
		//Listen to media & sip events to update the UI
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_MEDIA_CHANGED));
		
		// Sensor management
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		Log.d(THIS_FILE, "Proximty sensor : "+proximitySensor);
		
		dialFeedback = new DialingFeedback(this, true);
		

		if(!prefsWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		

        if(quitTimer == null) {
    		quitTimer = new Timer("Quit-timer");
        }
        
        applyTheme();
	}
	


	@Override
	protected void onStart() {
		Log.d(THIS_FILE, "Start in call");
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
        

        if(proximitySensor != null && powerManager != null) {
	        WifiManager wman = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo winfo = wman.getConnectionInfo();
			if(winfo == null || !prefsWrapper.keepAwakeInCall()) {
				// Try to use powermanager proximity sensor
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
        

	}
	

	@Override
	protected void onResume() {
		super.onResume();
		
		endCallTargetRect = null;
		holdTargetRect = null;
		answerTargetRect = null;
		xferTargetRect = null;

        
        //If we should manage it ourselves
        if(proximitySensor != null && proximityWakeLock == null && !proximitySensorTracked) {
			//Fall back to manual mode
			isFirstRun = true;
			Log.d(THIS_FILE, "Register sensor");
			sensorManager.registerListener(this, 
	                proximitySensor,
	                SensorManager.SENSOR_DELAY_NORMAL);
			proximitySensorTracked  = true;
		}
        dialFeedback.resume();
        handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
		
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		
		
		if(proximitySensor != null && proximitySensorTracked) {
			proximitySensorTracked = false;
			sensorManager.unregisterListener(this);
			Log.d(THIS_FILE, "Unregister to sensor is done !!!");
		}
		
		dialFeedback.pause();
		
		
		lockOverlay.tearDown();
	}
	
	@Override
	protected void onStop() {
		super.onStop();

		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		if(manageKeyguard) {
			keyguardLock.reenableKeyguard();
		}
	}
	

	@Override
	protected void onDestroy() {
		
		if(quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		
		if(draggingTimer != null) {
			draggingTimer.cancel();
			draggingTimer.purge();
			draggingTimer = null;
		}
		
		try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
		service = null;
		if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		try {
			unregisterReceiver(callStateReceiver);
		}catch (IllegalArgumentException e) {
			//That's the case if not registered (early quit)
		}
		
		//Remove badges
		badges.clear();
		
		super.onDestroy();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		//TODO : update UI
		Log.d(THIS_FILE, "New intent is launched");
		
		
		super.onNewIntent(intent);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		updateUIFromCall();
	}


	
	private void applyTheme() {
		String theme = prefsWrapper.getPreferenceStringValue(SipConfigManager.THEME);
		if(! TextUtils.isEmpty(theme)) {
			new Theme(this, theme, new Theme.onLoadListener() {
				@Override
				public void onLoad(Theme t) {
					dialPad.applyTheme(t);
					inCallControls.applyTheme(t);
				}
			});
		}
	}
	
	
	private static final int UPDATE_FROM_CALL = 1;
	private static final int UPDATE_FROM_MEDIA = 2;
	private static final int UPDATE_DRAGGING = 3;
	// Ui handler
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_FROM_CALL:
				updateUIFromCall();
				break;
			case UPDATE_FROM_MEDIA:
				updateUIFromMedia();
				break;
			case UPDATE_DRAGGING :
				DraggingInfo di = (DraggingInfo) msg.obj;
				inCallControls.setVisibility(di.isDragging? View.GONE: View.VISIBLE);
				endCallTarget.setVisibility(di.isDragging ? View.VISIBLE : View.GONE);
				holdTarget.setVisibility(
						(di.isDragging && di.call.isActive() && ! di.call.isBeforeConfirmed())? 
						View.VISIBLE : View.GONE);
				answerTarget.setVisibility(( di.call.isActive() && di.call.isBeforeConfirmed() && di.call.isIncoming() && di.isDragging) ? 
						View.VISIBLE : View.GONE);
				xferTarget.setVisibility( ( !di.call.isBeforeConfirmed() && !di.call.isAfterEnded()  && di.isDragging ) ? 
						View.VISIBLE : View.GONE); 
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PICKUP_SIP_URI_XFER:
			if(resultCode == RESULT_OK && service != null) {
				String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				try {
					// TODO : should get the call that was xfered in buffer first.
					SipCallSession currentCall = getActiveCallInfo();
					if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
						service.xfer(currentCall.getCallId(), callee);
					}
				} catch (RemoteException e) {
					//TODO : toaster 
				}
			}
			return;
		case PICKUP_SIP_URI_NEW_CALL:
			if(resultCode == RESULT_OK && service != null) {
				String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				int accountId = data.getIntExtra(SipProfile.FIELD_ACC_ID, SipProfile.INVALID_ID);
				if(accountId != SipProfile.INVALID_ID) {
					try {
						service.makeCall(callee, accountId);
					} catch (RemoteException e) {
						//TODO : toaster 
					}
				}
			}
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public static final int AUDIO_SETTINGS_MENU = Menu.FIRST + 1;
	public static final int RECORD_MENU = Menu.FIRST + 2;
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem recItem = menu.findItem(RECORD_MENU);
		boolean valueOk = false;
		
		if(service != null) {
			try {
				boolean isRecording = (service.getRecordedCall() != -1);
				if(isRecording) {
					recItem.setTitle(R.string.stop_recording);
					recItem.setIcon(R.drawable.stop);
					recItem.setEnabled(true);
					valueOk = true;
				}else {
					SipCallSession currentCall = getActiveCallInfo();
					if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
						if(service.canRecord(currentCall.getCallId())) {
							recItem.setTitle(R.string.record);
							recItem.setIcon(R.drawable.record);
							recItem.setEnabled(true);
							valueOk = true;
						}
					}
				}
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't call services methods", e);
			}
		}
		if(!valueOk) {
			recItem.setEnabled(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, AUDIO_SETTINGS_MENU, Menu.NONE, R.string.prefs_media).setIcon(R.drawable.ic_menu_media);
		menu.add(Menu.NONE, RECORD_MENU, Menu.NONE, R.string.record).setIcon(R.drawable.record);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case AUDIO_SETTINGS_MENU:
			startActivity(new Intent(this, InCallMediaControl.class));
			return true;
		case RECORD_MENU:
			try {
				if(service != null) {
					SipCallSession currentCall = getActiveCallInfo();
					if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
						service.startRecording(currentCall.getCallId());
					}
				}
			} catch (RemoteException e) {
				//TODO : toaster 
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Get the call that is active on the view
	 * @param excludeHold if true we do not return cals hold locally
	 * @return
	 */
	private SipCallSession getActiveCallInfo() {
		SipCallSession currentCallInfo = null;
		if(callsInfo == null) {
			return null;
		}
		for(SipCallSession callInfo : callsInfo) {
			currentCallInfo = getPrioritaryCall(callInfo, currentCallInfo);
		}
		return currentCallInfo;
	}
	
	
	
	private SipCallSession getCallInfo(int callId) {
		if(callsInfo == null) {
			return null;
		}
		for(SipCallSession callInfo : callsInfo) {
			if(callInfo.getCallId() == callId) {
				return callInfo;
			}
		}
		return null;
	}
	
	private static final int MAIN_PADDING = 15;
	private static final int HOLD_PADDING = 7;
	
	
	private SipCallSession getPrioritaryCall(SipCallSession call1, SipCallSession call2) {
		// We prefer the not null
		if(call1 == null) {
			return call1;
		}else if(call2 == null) {
			return call1;
		}
		// We prefer the one not terminated
		if(call1.isAfterEnded()) {
			return call2;
		}else if(call2.isAfterEnded()) {
			return call1;
		}
		// We prefer the one not held
		if(call1.isLocalHeld()) {
			return call2;
		}else if(call2.isLocalHeld()) {
			return call1;
		}
		// We prefer the most recent
		return (call1.callStart > call2.callStart) ? call2 : call1;
	}
	
	private synchronized void updateUIFromCall() {
		if(!serviceConnected) {
			return;
		}
		
		//Current call is the call emphasis by the UI.
		SipCallSession mainCallInfo = null;
		
		int mainsCalls = 0;
		int heldsCalls = 0;
		int heldIndex = 0;
		int mainIndex = 0;
		
		//Add badges if necessary
		for(SipCallSession  callInfo : callsInfo) {
			Log.d(THIS_FILE, "We have a call "+callInfo.getCallId()+" / "+callInfo.getCallState()+"/"+callInfo.getMediaStatus());
			
			if ( !callInfo.isAfterEnded() && !hasBadgeForCall(callInfo) ) {
				Log.d(THIS_FILE, "Has to add badge for "+callInfo.getCallId());
				addBadgeForCall(callInfo);
			}
			
			if( ! callInfo.isAfterEnded()) {
				if(callInfo.isLocalHeld()) {
					heldsCalls ++;
				}else {
					mainsCalls ++;
				}
			}
			
			mainCallInfo = getPrioritaryCall(callInfo, mainCallInfo);
		}
		
		
		int mainWidth = METRICS.widthPixels;
		if(heldsCalls > 0) {
			//In this case available width for MAIN part is 2/3 of the view
			mainWidth -= METRICS.widthPixels/3;
		}
		//this is not the good way to do that -- FIXME 
		int mainHeight = METRICS.heightPixels * 7/15;
		
		
		//Update each badges
		ArrayList<InCallInfo2> badgesToRemove = new ArrayList<InCallInfo2>();
		for( Entry<Integer, InCallInfo2> badgeSet : badges.entrySet()) {
			SipCallSession callInfo = getCallInfo(badgeSet.getKey());
			InCallInfo2 badge = badgeSet.getValue();
			if(callInfo != null) {
				//Main call position / size should be done at the end
				if (callInfo.isAfterEnded() && (mainsCalls + heldsCalls > 0) ) {
					//The badge should be removed
					badgesToRemove.add(badge);
					
				} else if (callInfo.isLocalHeld()) {
					// The call is held
					int y = MAIN_PADDING + heldIndex * (mainHeight/3 + MAIN_PADDING);
					Rect wrap = new Rect(
							mainWidth + HOLD_PADDING, 
							y,
							METRICS.widthPixels - HOLD_PADDING, 
							y + mainHeight/3);
					layoutBadge(wrap, badge);
					heldIndex ++;
					
				} else {

					// The call is normal
					int x = MAIN_PADDING;
					int y = MAIN_PADDING;
					int end_x = mainWidth - MAIN_PADDING;
					int end_y = mainHeight - MAIN_PADDING;
					if(mainsCalls > 1) {
						// we split view in 4
						if( (mainIndex % 2) == 0) {
							//First column
							end_x = mainWidth/2 - MAIN_PADDING;
						}else {
							//Second column
							x = mainWidth/2 + MAIN_PADDING;
						}
						if( mainIndex < 2 ) {
							end_y = mainHeight/2 - MAIN_PADDING;
						}else {
							y = mainHeight /2 + MAIN_PADDING;
						}
					}
					Rect wrap = new Rect(
							x, 
							y,
							end_x, 
							end_y);
					layoutBadge(wrap, badge);
					mainIndex ++;
				}
			}
			//Update badge state
			badge.setCallState(callInfo);
		}
		
		//Remove useless badges
		for(InCallInfo2 badge : badgesToRemove) {
			callInfoPanel.removeView(badge);
			SipCallSession ci = badge.getCallInfo();
			if(ci != null) {
				badges.remove(ci.getCallId());
			}
		}
		
		if( (mainsCalls == 1 || (mainsCalls == 0 && heldsCalls == 1) ) && mainCallInfo != null) {
			Log.d(THIS_FILE, "Current call is " + mainCallInfo.getCallId());
			
			//Update in call actions
			inCallControls.setCallState(mainCallInfo);
			inCallControls.setVisibility(View.VISIBLE);
		}else {
			inCallControls.setVisibility(View.GONE);
		}
		
		if(mainCallInfo != null) {
			Log.d(THIS_FILE, "Active call is "+mainCallInfo.getCallId());
			Log.d(THIS_FILE, "Update ui from call " + mainCallInfo.getCallId() + " state " + CallsUtils.getStringCallState(mainCallInfo, this));
			int state = mainCallInfo.getCallState();
			
			int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
			
			//We manage wake lock
			switch (state) {
			case SipCallSession.InvState.INCOMING:
			case SipCallSession.InvState.EARLY:
			case SipCallSession.InvState.CALLING:
			case SipCallSession.InvState.CONNECTING:
				
				Log.d(THIS_FILE, "Acquire wake up lock");
				if(wakeLock != null && !wakeLock.isHeld()) {
					wakeLock.acquire();
				}
				
				
				if(proximitySensor == null && proximityWakeLock == null) {
					if(mainCallInfo.isIncoming()) {
						lockOverlay.hide();
					}else {
						lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_START);
					}
				}
				
				if(proximityWakeLock != null) {
					if(mainCallInfo.isIncoming()) {
						// If call is incoming we do not use proximity sensor to allow to take call
						if(proximityWakeLock.isHeld()) {
							proximityWakeLock.release();
						}
					} else {
						// Else we acquire wake lock
						if(!proximityWakeLock.isHeld()) {
							proximityWakeLock.acquire();
						}
					}
				}
				break;
			case SipCallSession.InvState.CONFIRMED:
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
			case SipCallSession.InvState.NULL:
			case SipCallSession.InvState.DISCONNECTED:
				Log.d(THIS_FILE, "Active call session is disconnected or null wait for quit...");
				// This will release locks
				delayedQuit();
				return;
				
			}
			
			int mediaStatus = mainCallInfo.getMediaStatus();
			switch (mediaStatus) {
			case SipCallSession.MediaState.ACTIVE:
				break;
			case SipCallSession.MediaState.REMOTE_HOLD:
			case SipCallSession.MediaState.LOCAL_HOLD:
			case SipCallSession.MediaState.NONE:
				if(backgroundResId == R.drawable.bg_in_call_gradient_connected ||
						backgroundResId == R.drawable.bg_in_call_gradient_bluetooth) {
					backgroundResId = R.drawable.bg_in_call_gradient_on_hold;
				}
				break;
			case SipCallSession.MediaState.ERROR:
			default:
				break;
			}
			
			
			mainFrame.setBackgroundResource(backgroundResId);
			Log.d(THIS_FILE, "we leave the update ui function");
		}
		
		if(mainsCalls == 0) {
			middleAddCall.setVisibility(View.VISIBLE);
		}else {
			middleAddCall.setVisibility(View.GONE);
		}
		
		if(heldsCalls + mainsCalls == 0) {
			delayedQuit();
		}
	}
	
	
	private synchronized void updateUIFromMedia() {
		if(service != null) {
			MediaState mediaState;
			try {
				mediaState = service.getCurrentMediaState();
				Log.d(THIS_FILE, "Media update ....");
				if(!mediaState.equals(lastMediaState)) {
					SipCallSession callInfo = getActiveCallInfo();
					lastMediaState = mediaState;
					
					if(callInfo != null) {
						int state = callInfo.getCallState();
						
						// Background
						if(state == SipCallSession.InvState.CONFIRMED) {
							mainFrame.setBackgroundResource(lastMediaState.isBluetoothScoOn?R.drawable.bg_in_call_gradient_bluetooth:R.drawable.bg_in_call_gradient_connected);
						}
					}
					
					// Actions
					inCallControls.setMediaState(lastMediaState);
				}
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't get the media state ", e);
			}
		}
	}
	
	private synchronized void delayedQuit() {
		
		if (wakeLock != null && wakeLock.isHeld()) {
			Log.d(THIS_FILE, "Releasing wake up lock");
            wakeLock.release();
        }
		if(proximityWakeLock != null && proximityWakeLock.isHeld()) {
			proximityWakeLock.release();
		}
		
		//Update ui
		lockOverlay.hide();
		setDialpadVisibility(View.GONE);
		middleAddCall.setVisibility(View.GONE);
		setCallBadgesVisibility(View.VISIBLE);
		inCallControls.setVisibility(View.GONE);
		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);
		
		Log.d(THIS_FILE, "Start quit timer");
		if(quitTimer != null) {
			quitTimer.schedule(new QuitTimerTask(), 3000);
		}else {
			finish();
		}
	}
	
	private class QuitTimerTask extends TimerTask{
		@Override
		public void run() {
			Log.d(THIS_FILE, "Run quit timer");
			finish();
		}
	};
	
	
	private void setDialpadVisibility(int visibility) {
		dialPadContainer.setVisibility(visibility);
		int antiVisibility = visibility == View.GONE? View.VISIBLE:View.GONE;
		setCallBadgesVisibility(antiVisibility);
	}
	
	private void setCallBadgesVisibility(int visibility) {
		callInfoPanel.setVisibility(visibility);
	}
	
	private boolean hasBadgeForCall(SipCallSession call) {
		return badges.containsKey(call.getCallId());
	}
	
	private void addBadgeForCall(SipCallSession call) {
		InCallInfo2 badge = new InCallInfo2(this, null);
		callInfoPanel.addView(badge);
		
		badge.forceLayout();
		badge.setOnTriggerListener(this);
		badge.setOnTouchListener(new OnBadgeTouchListener(badge, call));
		badges.put(call.getCallId(), badge);
	}
	
	private void layoutBadge(Rect wrap, InCallInfo2 mainBadge) {
		//Log.d(THIS_FILE, "Layout badge for "+wrap.width()+" x "+wrap.height()+ " +"+wrap.top+" + "+wrap.left);
		//Set badge size
		Rect r = mainBadge.setSize(wrap.width(), wrap.height());
		//Reposition badge at the correct place
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.topMargin = Math.round(wrap.centerY() - 0.5f * r.height());
		lp.leftMargin = Math.round(wrap.centerX() - 0.5f * r.width());
		lp.gravity = Gravity.TOP | Gravity.LEFT;
		Log.d(THIS_FILE, "Set margins : " + lp.topMargin + " , " + lp.leftMargin);
		//		Math.round( wrap.centerX() - 0.5f * r.width() ), Math.round(wrap.centerY() - 0.5f * r.height()));
		mainBadge.setLayoutParams(lp);
	}
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(THIS_FILE, "Key down : " + keyCode);
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

        	// Detect if ringing
        	SipCallSession currentCallInfo = getActiveCallInfo();
    		//If not any active call active
    		if(currentCallInfo == null && serviceConnected) {
    			break;
    		}
    		
    		if(service != null) {
        		try {
					service.adjustVolume(currentCallInfo, action, AudioManager.FLAG_SHOW_UI);
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Can't adjust volume", e);
				}
        	}
    		
    		
        	return true;
        case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_ENDCALL:
        	return inCallControls.onKeyDown(keyCode, event);
		case KeyEvent.KEYCODE_SEARCH:
			//Prevent search
			return true;
        default:
        	//Nothing to do	
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(THIS_FILE, "Key up : "+keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_SEARCH:
			return true;
		case KeyEvent.KEYCODE_ENDCALL:
        	return inCallControls.onKeyDown(keyCode, event);
			
		}
		return super.onKeyUp(keyCode, event);
	}

	
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(SipManager.ACTION_SIP_CALL_CHANGED)){
				if(service != null) {
					try {
						callsInfo = service.getCalls();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Not able to retrieve calls");
					}
				}

				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
			}else if(action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
			}
		}
	};
	
	
	/**
	 * Service binding
	 */
	private boolean serviceConnected = false;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			try {
				//Log.d(THIS_FILE, "Service started get real call info "+callInfo.getCallId());
				callsInfo = service.getCalls();
				serviceConnected = true;
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_CALL));
				handler.sendMessage(handler.obtainMessage(UPDATE_FROM_MEDIA));
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Can't get back the call", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serviceConnected = false;
			callsInfo = null;
		}
	};


	//private boolean showDetails = true;

	private boolean isFirstRun = true;

	@Override
	public void onTrigger(int whichAction, SipCallSession call) {
		Log.d(THIS_FILE, "In Call Activity is triggered");
		Log.d(THIS_FILE, "We have a current call : " + call);
		
		if(whichAction != ADD_CALL) {
			// We check that current call is valid for any actions
			if(call == null) {
				Log.e(THIS_FILE, "Try to do an action on a null call !!!");
				return;
			}
			if(call.getCallId() == SipCallSession.INVALID_CALL_ID) {
				Log.e(THIS_FILE, "Try to do an action on an invalid call !!!");
				return;
			}
		}
		
		//Reset proximity sensor timer
		if(proximitySensor == null && proximityWakeLock == null) {
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
		}
		
		try {
			switch(whichAction) {
				case TAKE_CALL:{
					if (service != null) {
						Log.d(THIS_FILE, "Answer call "+call.getCallId());
						
						boolean shouldHoldOthers = false;
						
						//Well actually we should be always before confirmed
						if(call.isBeforeConfirmed() && callsInfo != null) {
							shouldHoldOthers = true;
						}
						
						service.answer(call.getCallId(), SipCallSession.StatusCode.OK);
						
						//if it's a ringing call, we assume that user wants to hold other calls
						if(shouldHoldOthers) {
							for(SipCallSession  callInfo : callsInfo) {
								//For each active and running call
								if(SipCallSession.InvState.CONFIRMED == callInfo.getCallState()
										&& !callInfo.isLocalHeld() 
										&& callInfo.getCallId() != call.getCallId() ) {
									
									Log.d(THIS_FILE, "Hold call "+callInfo.getCallId());
									service.hold(callInfo.getCallId());
									
								}
							}
						}
					}
					break;
				}
				case DECLINE_CALL: 
				case CLEAR_CALL:
				{
					if (service != null) {
						service.hangup(call.getCallId(), 0);
					}
					break;
				}
				case MUTE_ON:
				case MUTE_OFF:
				{
					if ( service != null) {
						service.setMicrophoneMute((whichAction == MUTE_ON)?true:false);
					}
					break;
				}
				case SPEAKER_ON :
				case SPEAKER_OFF :
				{
					if (service != null) {
						service.setSpeakerphoneOn((whichAction == SPEAKER_ON)?true:false);
					}
					break;
				}
				case BLUETOOTH_ON:
				case BLUETOOTH_OFF: {
					if (service != null) {
						service.setBluetoothOn((whichAction == BLUETOOTH_ON)?true:false);
					}
					break;
				}
				case DIALPAD_ON:
				case DIALPAD_OFF: {
					setDialpadVisibility((whichAction == DIALPAD_ON)?View.VISIBLE:View.GONE);
					break;
				}
				case DETAILED_DISPLAY:{
					String infos = PjSipCalls.dumpCallInfo(call.getCallId());
					Log.d(THIS_FILE, infos);
					SpannableStringBuilder buf = new SpannableStringBuilder();
					Builder builder = new AlertDialog.Builder(this);
					
					buf.append(infos);
					TextAppearanceSpan textSmallSpan = new TextAppearanceSpan(this, android.R.style.TextAppearance_Small);
					buf.setSpan(textSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					
					AlertDialog dialog = builder.setIcon(android.R.drawable.ic_dialog_info)
						.setMessage(buf)
						.setNeutralButton(R.string.ok, null)
						.create();
					dialog.show();
					break;
				}
				case TOGGLE_HOLD:{
					if (service != null) {
						//Log.d(THIS_FILE, "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
						if(call.getMediaStatus() == SipCallSession.MediaState.LOCAL_HOLD ||
								call.getMediaStatus() == SipCallSession.MediaState.NONE ) {
							service.reinvite(call.getCallId(), true);
						}else {
							service.hold(call.getCallId());
						}
					}
					break;
				}
				case MEDIA_SETTINGS:{
					startActivity(new Intent(this, InCallMediaControl.class));
					break;
				}
				case XFER_CALL : {
					Intent pickupIntent = new Intent(this, PickupSipUri.class);
					startActivityForResult(pickupIntent, PICKUP_SIP_URI_XFER);
					break;
				}
				
				case ADD_CALL : {
					Intent pickupIntent = new Intent(this, PickupSipUri.class);
					startActivityForResult(pickupIntent, PICKUP_SIP_URI_NEW_CALL);
					break;
				}
			}
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Was not able to call service method", e);
		}
	}
	

	@Override
	public void onTrigger(int keyCode, int dialTone) {
		if(proximitySensor == null && proximityWakeLock == null) {
			lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
		}
		
		if (service != null) {
			SipCallSession currentCall = getActiveCallInfo();
			if(currentCall != null && currentCall.getCallId() != SipCallSession.INVALID_CALL_ID) {
				try {
					service.sendDtmf(currentCall.getCallId(), keyCode);
					dialFeedback.giveFeedback(dialTone);
					KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
					char nbr = event.getNumber();
					dialPadTextView.getText().append(nbr);
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Was not able to send dtmf tone", e);
				}
			}
		}
		
	}

	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}


	private static final float PROXIMITY_THRESHOLD = 5.0f;
	private static boolean INVERT_PROXIMITY_SENSOR = false;
	@Override
	public void onSensorChanged(SensorEvent event) {
		//Log.d(THIS_FILE, "Tracked : "+proximitySensorTracked);
		if(proximitySensorTracked && !isFirstRun) {
			float distance = event.values[0];
			boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
			if(INVERT_PROXIMITY_SENSOR) {
				active = !active;
			}
			Log.d(THIS_FILE, "Distance is now " + distance);
			boolean isValidCallState = false;

			if(callsInfo != null) {
				for(SipCallSession callInfo : callsInfo) {
					int state = callInfo.getCallState();
					isValidCallState |= ( 
						(state == SipCallSession.InvState.CONFIRMED ) || 
						(state == SipCallSession.InvState.CONNECTING )|| 
						(state == SipCallSession.InvState.CALLING )|| 
						(state == SipCallSession.InvState.EARLY && !callInfo.isIncoming() )
					);
				}
			}
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
			onTrigger(OnTriggerListener.CLEAR_CALL, getActiveCallInfo());
			lockOverlay.reset();
		default:
			break;
		}
		
	}
	
	
	//Drag and drop feature
	private Timer draggingTimer;
	
	public class OnBadgeTouchListener implements OnTouchListener {
		private SipCallSession call;
		private InCallInfo2 badge;
		private boolean isDragging = false;
		private SetDraggingTimerTask draggingDelayTask;
		Vibrator vibrator;
		int beginX = 0;
		int beginY = 0;

		private class SetDraggingTimerTask extends TimerTask {
			@Override
			public void run() {
				vibrator.vibrate(50);
	        	setDragging(true);
				Log.d(THIS_FILE, "Begin dragging");
			}
		};
		
		
		public OnBadgeTouchListener(InCallInfo2 aBadge, SipCallSession aCall) {
			call = aCall;
			badge = aBadge;
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			//TODO : move somewhere else
			if(draggingTimer == null) {
				draggingTimer = new Timer("Dragging-timer");
			}
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
	        int action = event.getAction();
	        int X = (int)event.getRawX();
	        int Y = (int)event.getRawY();
	        
	        // Reset the not proximity sensor lock overlay
			if(proximitySensor == null && proximityWakeLock == null) {
				lockOverlay.delayedLock(ScreenLocker.WAIT_BEFORE_LOCK_LONG);
			}
	        
	        switch ( action ) {
	        case MotionEvent.ACTION_DOWN:
	        	if(draggingDelayTask != null) {
	        		draggingDelayTask.cancel();
	        	}
	        	draggingDelayTask = new SetDraggingTimerTask();
	        	beginX = X;
	        	beginY = Y;
	        	draggingTimer.schedule(draggingDelayTask, DRAGGING_DELAY);
	        case MotionEvent.ACTION_MOVE:
	        	if(isDragging) {
	        		float size = Math.max(75.0f, event.getSize() + 50.0f);
	        		
	        		
	        		Rect wrap = new Rect(
							(int) (X - (size) ), 
							(int) (Y - (size) ),
							(int) (X + (size/2.0f) ), 
							(int) (Y + (size/2.0f) ) );
					layoutBadge(wrap, badge);
					badge.bringToFront();
		        	//Log.d(THIS_FILE, "Is moving to "+X+", "+Y);
	        		return true;
	        	}else {
	        		if(Math.abs(X-beginX) > 50 || Math.abs(Y-beginY) > 50) {
		        		Log.d(THIS_FILE, "Stop dragging");
			        	stopDragging();
			        	return true;
	        		}
	        		return false;
	        	}

	        case MotionEvent.ACTION_UP:
	        	onDropBadge(X, Y, badge, call);
	        	stopDragging();
	        	return true;
	        	//Yes we continue cause this is a stop action
	        case MotionEvent.ACTION_CANCEL:
	        case MotionEvent.ACTION_OUTSIDE:
	        	Log.d(THIS_FILE, "Stop dragging");
	        	stopDragging();
	        	return false;
	        }
			return false;
		}
		
		
		
		private void stopDragging() {
        	//TODO : thread save it
        	draggingDelayTask.cancel();
        	setDragging(false);
		}
		
		private void setDragging(boolean dragging) {
			isDragging = dragging;
			handler.sendMessage(handler.obtainMessage(UPDATE_DRAGGING, new DraggingInfo(isDragging, badge, call)));
		}
		public void setCallState(SipCallSession callInfo) {
			Log.d(THIS_FILE, "Updated call infos : "+call.getCallState()+" and "+ call.getMediaStatus()+" et "+call.isLocalHeld());
			call = callInfo;
		}
	}
	
	private void onDropBadge(int X, int Y, InCallInfo2 badge, SipCallSession call) {
		Log.d(THIS_FILE, "Dropping !!! in "+X+", "+Y);
		
		//Rectangle init if not already done
		if(endCallTargetRect == null && endCallTarget.getVisibility() == View.VISIBLE) {
			endCallTargetRect = new Rect(endCallTarget.getLeft(), endCallTarget.getTop(), endCallTarget.getRight(), endCallTarget.getBottom());
		}
		if(holdTargetRect == null && holdTarget.getVisibility() == View.VISIBLE) {
			holdTargetRect = new Rect(holdTarget.getLeft(), holdTarget.getTop(), holdTarget.getRight(), holdTarget.getBottom());
		}
		if(answerTargetRect == null && answerTarget.getVisibility() == View.VISIBLE) {
			answerTargetRect = new Rect(answerTarget.getLeft(), answerTarget.getTop(), answerTarget.getRight(), answerTarget.getBottom());
		}
		if(xferTargetRect == null && xferTarget.getVisibility() == View.VISIBLE) {
			xferTargetRect = new Rect(xferTarget.getLeft(), xferTarget.getTop(), xferTarget.getRight(), xferTarget.getBottom());
		}
		

		//Rectangle matching
		
		if (endCallTargetRect != null && endCallTargetRect.contains(X, Y)) {
			//Drop in end call zone
			onTrigger(call.isIncoming() && call.isBeforeConfirmed() ? DECLINE_CALL : CLEAR_CALL, call);
		}else if (holdTargetRect != null && holdTargetRect.contains(X, Y)) {
			// check if not drop on held call
			boolean dropOnOtherCall = false;
			for( Entry<Integer, InCallInfo2> badgeSet : badges.entrySet()) {
				Log.d(THIS_FILE, "On drop target searching for another badge");
				int callId = badgeSet.getKey();
				if(callId != call.getCallId()) {
					Log.d(THIS_FILE, "found a different badge than self");
					SipCallSession callInfo = getCallInfo(callId);
					if(callInfo.isLocalHeld()) {
						Log.d(THIS_FILE, "Other badge is hold");
						InCallInfo2 otherBadge = badgeSet.getValue();
						Rect r = new Rect( otherBadge.getLeft(), otherBadge.getTop(), otherBadge.getRight(), otherBadge.getBottom());
						Log.d(THIS_FILE, "Current X, Y "+X+", "+Y+ " -- "+r.top+", "+r.left+", "+r.right+", "+r.bottom);
						if(r.contains(X, Y)) {
							Log.d(THIS_FILE, "Yep we've got one");
							dropOnOtherCall = true;
							if(service != null) {
								try {
									service.xferReplace(call.getCallId(), callId, 1);
								} catch (RemoteException e) {
									// TODO : toaster
								}
							}
						}
					}
				}
			}
			
			//Drop in hold zone
			
			if (!dropOnOtherCall && !call.isLocalHeld()) {
				onTrigger(TOGGLE_HOLD, call);
			}
		} else if(answerTargetRect != null && answerTargetRect.contains(X, Y)){
			if(call.isIncoming() && call.isBeforeConfirmed()) {
				onTrigger(TAKE_CALL, call);
			}
		} else if(xferTargetRect != null && xferTargetRect.contains(X, Y)) {
			if(!call.isBeforeConfirmed() && !call.isAfterEnded()) {
				onTrigger(XFER_CALL, call);
			}
			
		}else {
			Log.d(THIS_FILE, "Drop is done somewhere else " + call.getMediaStatus());
			//Drop somewhere else
			if(call.isLocalHeld()) {
				Log.d(THIS_FILE, "Try to unhold");
				onTrigger(TOGGLE_HOLD, call);
			}
		}
		updateUIFromCall();
	}
	
	private class DraggingInfo {
		public boolean isDragging = false;
//		public InCallInfo2 badge;
		public SipCallSession call;
		
		public DraggingInfo(boolean aIsDragging, InCallInfo2 aBadge, SipCallSession aCall) {
			isDragging = aIsDragging;
//			badge = aBadge;
			call = aCall;
		}
	}
}
