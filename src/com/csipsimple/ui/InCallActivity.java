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

import java.util.Timer;
import java.util.TimerTask;

import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsip_status_code;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.InCallControls;
import com.csipsimple.widgets.InCallInfo;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.InCallControls.OnTriggerListener;


public class InCallActivity extends Activity implements OnTriggerListener, OnDialKeyListener {
	private static String THIS_FILE = "SIP CALL HANDLER";

	private CallInfo callInfo = null;
	private FrameLayout mainFrame;
	private InCallControls inCallControls;
	private InCallInfo inCallInfo;

	private WakeLock wakeLock;
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;

	private Dialpad dialPad;

	private View callInfoPanel;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		setContentView(R.layout.in_call_main);
		Log.d(THIS_FILE, "Creating call handler.....");
		serviceBound = bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			callInfo = (CallInfo) extras.get("call_info");
		}

		if (callInfo == null) {
			Log.e(THIS_FILE, "You provide an empty call info....");
			finish();
		}

		Log.d(THIS_FILE, "Creating call handler for " + callInfo.getCallId());
		
		
		
		

		//remoteContact = (TextView) findViewById(R.id.remoteContact);
		mainFrame = (FrameLayout) findViewById(R.id.mainFrame);
		inCallControls = (InCallControls) findViewById(R.id.inCallControls);
		inCallControls.setOnTriggerListener(this);
		
		inCallInfo = (InCallInfo) findViewById(R.id.inCallInfo);
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		dialPad.setOnDialKeyListener(this);
		callInfoPanel = (View) findViewById(R.id.callInfoPanel);
		
		registerReceiver(callStateReceiver, new IntentFilter(SipService.ACTION_SIP_CALL_CHANGED));
		
		
		
		//Check if currently in use call is not already invalid (could be the case for example if we are not authorized to make the call)
		//There is a time between when the call change notification that starts the InCallActivity and
		//when this view registers the on ua call state changed
		CallInfo realCallInfo = new CallInfo(callInfo.getCallId());
		callInfo = realCallInfo;
		updateUIFromCall();
		
		/*
		if(realCallInfo.getCallState().equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) ||
				realCallInfo.getCallState().equals(pjsip_inv_state.PJSIP_INV_STATE_NULL)) {
			Log.w(THIS_FILE, "Early failure for call "+callInfo.getCallId());
			
			delayedQuit();
			
		}else {		
			
			
		}
		*/
	}
	
	private boolean manageKeyguard = false;
	
	@Override
	protected void onStart() {
		super.onStart();
        if (keyguardManager == null) {
            keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("com.csipsimple.inCallKeyguard");
        }
        
        if(keyguardManager.inKeyguardRestrictedInputMode()) {
        	manageKeyguard = true;
        	keyguardLock.disableKeyguard();
        }
        
        //Enlight the screen
        
        
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(manageKeyguard) {
			keyguardLock.reenableKeyguard();
		}

	}

	private void updateUIFromCall() {

		Log.d(THIS_FILE, "Update ui from call " + callInfo.getCallId() + " state " + callInfo.getStringCallState());
		
		pjsip_inv_state state = callInfo.getCallState();


		//Update in call actions
		inCallInfo.setCallState(callInfo);
		inCallControls.setCallState(callInfo);
		
		
		int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
		
		Log.d(THIS_FILE, "Manage wake lock");
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
        }
        
		switch (state) {
		case PJSIP_INV_STATE_INCOMING:
		case PJSIP_INV_STATE_EARLY:
		    wakeLock.acquire();
			break;
		case PJSIP_INV_STATE_CALLING:
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
			break;
		case PJSIP_INV_STATE_CONFIRMED:
			backgroundResId = R.drawable.bg_in_call_gradient_connected;
			if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
			break;
		case PJSIP_INV_STATE_NULL:
			Log.i(THIS_FILE, "WTF?");
			if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
			break;
		case PJSIP_INV_STATE_DISCONNECTED:
			dialPad.setVisibility(View.GONE);
			callInfoPanel.setVisibility(View.VISIBLE);
			delayedQuit();
			return;
		case PJSIP_INV_STATE_CONNECTING:
			break;
		}
		mainFrame.setBackgroundResource(backgroundResId);
	}


	private void delayedQuit() {
		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				finish();
			}
		}, 3000);
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
        	int action = AudioManager.ADJUST_RAISE;
        	if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
        		action = AudioManager.ADJUST_LOWER;
        	}
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.adjustStreamVolume( AudioManager.STREAM_VOICE_CALL, action, AudioManager.FLAG_SHOW_UI);
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
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			CallInfo notif_call = null;
			if (extras != null) {
				notif_call = (CallInfo) extras.get("call_info");
			}

			if (notif_call != null && callInfo.equals(notif_call)) {
				callInfo = notif_call;
				updateUIFromCall();
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
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
	};


	private boolean canTakeCall = true;
	private boolean canDeclineCall = true;

	@Override
	public void onTrigger(int whichAction) {
		Log.d(THIS_FILE, "In Call Activity is triggered");
		Log.d(THIS_FILE, "We have a call info : "+callInfo);
		Log.d(THIS_FILE, "And a service : "+service);
		
		switch(whichAction) {
			case TAKE_CALL:{
				if (callInfo != null && service != null && canTakeCall) {
					try {
						service.answer(callInfo.getCallId(), pjsip_status_code.PJSIP_SC_OK.swigValue());
						canTakeCall = false;
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Was not able to take the call", e);
					}
				}
				break;
			}
			case DECLINE_CALL: {
				if (callInfo != null && service != null && canDeclineCall) {
					try {
						service.hangup(callInfo.getCallId(), 0);
						canDeclineCall = false;
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Was not able to decline the call", e);
					}
				}
				break;
			}
			case CLEAR_CALL: {
				if (callInfo != null && service != null && canDeclineCall) {
					try {
						service.hangup(callInfo.getCallId(), 0);
						canDeclineCall=false;
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Was not able to clear the call", e);
					}
				}
				break;
			}
			case MUTE_ON:{
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				am.setMicrophoneMute(true);
				Log.d(THIS_FILE, "Microphone is now muted");
				break;
			}
			case MUTE_OFF:{
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				am.setMicrophoneMute(false);
				Log.d(THIS_FILE, "Microphone is now un muted");
				break;
			}
			case SPEAKER_ON :{
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				am.setSpeakerphoneOn(true);
				break;
			}
			case SPEAKER_OFF :{
				AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				am.setSpeakerphoneOn(false);
				break;
			}
			case BLUETOOTH_ON:{
		//		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//		am.setBluetoothA2dpOn(true);
		//		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_bluetooth);
				break;
			}
			case BLUETOOTH_OFF:{
		//		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//		am.setBluetoothA2dpOn(false);
		//		mainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_connected);
				break;
			}
			case DIALPAD_ON:{
				dialPad.setVisibility(View.VISIBLE);
				callInfoPanel.setVisibility(View.GONE);
				break;
			}
			case DIALPAD_OFF:{
				dialPad.setVisibility(View.GONE);
				callInfoPanel.setVisibility(View.VISIBLE);
				break;
			}
		}
	}

	@Override
	public void onTrigger(int keyCode, int dialTone) {
		if (callInfo != null && service != null) {
			try {
				service.sendDtmf(callInfo.getCallId(), keyCode);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Was not able to take the call", e);
			}
		}
		
	}
}
