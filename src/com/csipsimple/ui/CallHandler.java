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
package com.csipsimple.ui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.csipsimple.utils.Log;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.service.UAStateReceiver;

public class CallHandler extends Activity {
	private static String THIS_FILE = "SIP CALL HANDLER";

	/**
	 * Service binding
	 */
	private boolean m_servicedBind = false;
	private ISipService mService;
	private ServiceConnection m_connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			mService = ISipService.Stub.asInterface(arg1);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
	};

	private CallInfo mCallInfo = null;
	private Button mTakeCall;
	private Button mClearCall;
	private TextView mRemoteContact;
	private LinearLayout mMainFrame;

	private WakeLock wl;
    private KeyguardManager mKeyguardManager;
    private KeyguardManager.KeyguardLock mKeyguardLock;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		setContentView(R.layout.callhandler);
		Log.d(THIS_FILE, "Creating call handler.....");
		m_servicedBind = bindService(new Intent(this, SipService.class), m_connection, Context.BIND_AUTO_CREATE);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mCallInfo = (CallInfo) extras.get("call_info");
		}

		if (mCallInfo == null) {
			Log.e(THIS_FILE, "You provide an empty call info....");
			finish();
		}

		Log.d(THIS_FILE, "Creating call handler for " + mCallInfo.getCallId());
		
		
		
		

		mRemoteContact = (TextView) findViewById(R.id.remoteContact);
		mMainFrame = (LinearLayout) findViewById(R.id.mainFrame);
		mTakeCall = (Button) findViewById(R.id.take_call);
		mClearCall = (Button) findViewById(R.id.hangup);
		updateUIFromCall();
		
		CallInfo call_info = new CallInfo(mCallInfo.getCallId());
		if(call_info.getCallState().equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) ||
				call_info.getCallState().equals(pjsip_inv_state.PJSIP_INV_STATE_NULL)) {
			Log.w(THIS_FILE, "Early failure for call "+mCallInfo.getCallId());
			delayedQuit();
			
		}else {		
			registerReceiver(callStateReceiver, new IntentFilter(UAStateReceiver.UA_CALL_STATE_CHANGED));
			
			
	
			mTakeCall.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (mCallInfo != null) {
						if(mService != null) {
							try {
								mService.answer(mCallInfo.getCallId(), pjsip_status_code.PJSIP_SC_OK.swigValue());
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			});
	
			mClearCall.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (mCallInfo != null) {
						if(mService != null) {
							try {
								mService.hangup(mCallInfo.getCallId(), 0);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
	
				}
			});

		}
		
	}
	

	private boolean manage_keyguard = false;
	
	@Override
	protected void onStart() {
		super.onStart();
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            mKeyguardLock = mKeyguardManager.newKeyguardLock("com.csipsimple.inCallKeyguard");
        }
        if(mKeyguardManager.inKeyguardRestrictedInputMode()) {
        	manage_keyguard = true;
        	mKeyguardLock.disableKeyguard();
        }
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(manage_keyguard) {
			mKeyguardLock.reenableKeyguard();
		}

	}

	private void updateUIFromCall() {

		Log.d(THIS_FILE, "Update ui from call " + mCallInfo.getCallId() + " state " + mCallInfo.getStringCallState());

		pjsip_inv_state state = mCallInfo.getCallState();
		String remote_contact = mCallInfo.getRemoteContact();
		Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*<sip(?:s)?:([^@]*@[^>]*)>");
		Matcher m = p.matcher(remote_contact);
		if (m.matches()) {
			remote_contact = m.group(1);
			if(remote_contact == null || remote_contact.equalsIgnoreCase("")) {
				remote_contact = m.group(2);
			}
		}
		
		mRemoteContact.setText(remote_contact);

		int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;
        if (wl == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.csipsimple.onIncomingCall");
        }


		switch (state) {
		case PJSIP_INV_STATE_INCOMING:
			mTakeCall.setVisibility(View.VISIBLE);
			mClearCall.setVisibility(View.VISIBLE);
			mClearCall.setText("Decline");
		    wl.acquire();
			break;
		case PJSIP_INV_STATE_CALLING:
			mTakeCall.setVisibility(View.GONE);
			mClearCall.setVisibility(View.VISIBLE);
			mClearCall.setText("Hang up");
            if (wl != null && wl.isHeld()) {
                wl.release();
            }
			break;
		case PJSIP_INV_STATE_CONFIRMED:
			mTakeCall.setVisibility(View.GONE);
			mClearCall.setVisibility(View.VISIBLE);
			mClearCall.setText("Hang up");
			backgroundResId = R.drawable.bg_in_call_gradient_connected;
			if (wl != null && wl.isHeld()) {
                wl.release();
            }
			break;
		case PJSIP_INV_STATE_NULL:
			Log.i(THIS_FILE, "WTF?");
			if (wl != null && wl.isHeld()) {
                wl.release();
            }
			break;
		case PJSIP_INV_STATE_DISCONNECTED:
			Log.i(THIS_FILE, "Disconnected here !!!");
			delayedQuit();
			return;
		case PJSIP_INV_STATE_EARLY:
		case PJSIP_INV_STATE_CONNECTING:
			break;
		}

		mMainFrame.setBackgroundResource(backgroundResId);
	}


	private void delayedQuit() {
		mMainFrame.setBackgroundResource(R.drawable.bg_in_call_gradient_ended);
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
		if (m_servicedBind) {
			unbindService(m_connection);
			m_servicedBind = false;
		}
		if (wl != null && wl.isHeld()) {
            wl.release();
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

			Log.d(THIS_FILE, "BC recieve");

			if (notif_call != null && mCallInfo.equals(notif_call)) {
				mCallInfo = notif_call;
				updateUIFromCall();
			}
		}
	};
}
