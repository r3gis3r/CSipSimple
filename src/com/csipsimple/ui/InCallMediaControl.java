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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.csipsimple.R;
import com.csipsimple.api.ISipConfiguration;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;

public class InCallMediaControl extends Activity implements OnSeekBarChangeListener, OnCheckedChangeListener, OnClickListener {
	protected static final String THIS_FILE = "inCallMediaCtrl";
	private SeekBar speakerAmplification;
	private SeekBar microAmplification;
	private Button saveButton;
	private CheckBox echoCancellation;
//	private Button recordButton;
	
	private boolean isAutoClose = false;
	
	private int AUTO_QUIT_DELAY = 3000;
	private Timer quitTimer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.in_call_media);

		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		
		speakerAmplification = (SeekBar) findViewById(R.id.speaker_level);
		microAmplification = (SeekBar) findViewById(R.id.micro_level);
		saveButton = (Button) findViewById(R.id.save_bt);
		echoCancellation = (CheckBox) findViewById(R.id.echo_cancellation);
	//	recordButton = (Button) findViewById(R.id.record);
		
		speakerAmplification.setOnSeekBarChangeListener(this);
		microAmplification.setOnSeekBarChangeListener(this);
		
	//	recordButton.setOnClickListener(this);
		saveButton.setOnClickListener(this);
		
		echoCancellation.setOnCheckedChangeListener(this);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent confServiceIntent = new Intent("com.csipsimple.service.SipConfiguration");
		bindService(confServiceIntent , configurationConnection, BIND_AUTO_CREATE);
		
		Intent sipServiceIntent = new Intent("com.csipsimple.service.SipService");
		bindService(sipServiceIntent , sipConnection, BIND_AUTO_CREATE);
		
		
		int direction = getIntent().getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
		if(direction == AudioManager.ADJUST_LOWER  || direction == AudioManager.ADJUST_RAISE) {
			isAutoClose = true;
			LinearLayout l = (LinearLayout) findViewById(R.id.ok_bar);
			if(l != null) {
				l.setVisibility(View.GONE);
			}
			delayedQuit(AUTO_QUIT_DELAY);
		}else {
			isAutoClose = false;
		}
		
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		try {
			unbindService(configurationConnection);
		}catch(Exception e) {
			//Just ignore that
		}
		try {
			unbindService(sipConnection);
		}catch(Exception e) {
			//Just ignore that
		}
		
		if(quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		try {
			unregisterReceiver(callStateReceiver);
		}catch (IllegalArgumentException e) {
			//That's the case if not registered (early quit)
		}
		
		configurationService = null;
		sipService = null;
	}
	
	
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(SipManager.ACTION_SIP_CALL_CHANGED)){
				if(sipService != null) {
					try {
						SipCallSession[] callsInfo = sipService.getCalls();
						SipCallSession currentCallInfo = null;
						if(callsInfo != null) {
							for(SipCallSession callInfo : callsInfo) {
								int state = callInfo.getCallState();
								switch (state) {
									case SipCallSession.InvState.NULL:
									case SipCallSession.InvState.DISCONNECTED:
										break;
									default:
										currentCallInfo = callInfo;
										break;
								}
								if(currentCallInfo != null) {
									break;
								}
							}
						}
						if(currentCallInfo == null) {
							finish();
						}
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Not able to retrieve calls");
					}
				}
			}
		}
	};
	
	private class LockTimerTask extends TimerTask{
		@Override
		public void run() {
			finish();
		}
	};
	

	public void delayedQuit(int time) {
		if(quitTimer != null) {
			quitTimer.cancel();
			quitTimer.purge();
			quitTimer = null;
		}
		
		quitTimer = new Timer("Quit-timer-media");
		
		quitTimer.schedule(new LockTimerTask(), time);
	}
	
	
	
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	
        	if(speakerAmplification != null) {
        		int step = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)? - 1 : + 1;
        		int newValue = speakerAmplification.getProgress() + step;
        		if(newValue >= 0 && newValue < speakerAmplification.getMax()) {
        			speakerAmplification.setProgress(newValue);
        		}
        	}
        	
        	
        	return true;
        case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_ENDCALL:
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
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_CALL:
		case KeyEvent.KEYCODE_ENDCALL:
		case KeyEvent.KEYCODE_SEARCH:
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	
	
	private ISipConfiguration configurationService;
	private ServiceConnection configurationConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			Log.d(THIS_FILE, "Configuration service -> "+arg0.flattenToString());
			configurationService = ISipConfiguration.Stub.asInterface(arg1);
			updateUIFromMedia();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
    
    private ISipService sipService;
	private ServiceConnection sipConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			Log.d(THIS_FILE, "SipService is connected");
			sipService = ISipService.Stub.asInterface(arg1);
			updateUIFromMedia();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
	

	private void updateUIFromMedia() {
		if(sipService != null && configurationService != null) {
			try {
				boolean useBT = sipService.getCurrentMediaState().isBluetoothScoOn;
				
				Float speakerLevel = configurationService.getPreferenceFloat(useBT ? 
						SipConfigManager.SND_BT_SPEAKER_LEVEL : SipConfigManager.SND_SPEAKER_LEVEL) * 10;
				speakerAmplification.setProgress(speakerLevel.intValue());
				Float microLevel = configurationService.getPreferenceFloat(useBT ?
						SipConfigManager.SND_BT_MIC_LEVEL : SipConfigManager.SND_MIC_LEVEL) * 10;
				microAmplification.setProgress(microLevel.intValue());
				
				echoCancellation.setChecked(configurationService.getPreferenceBoolean(SipConfigManager.ECHO_CANCELLATION));
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Impossible to get mic/speaker level", e);
			}
			
		//	updateCallButton();
		}
		
	}


	@Override
	public void onProgressChanged(SeekBar arg0, int value, boolean arg2) {
		Log.d(THIS_FILE, "Progress has changed");
		if(sipService != null && configurationService != null) {
			try {
				Float newValue = (float) ( value / 10.0 );
				String key;
				boolean useBT = sipService.getCurrentMediaState().isBluetoothScoOn;
				switch(arg0.getId()) {
				case R.id.speaker_level:
					sipService.confAdjustTxLevel(0, newValue);
					key =  useBT ? SipConfigManager.SND_BT_SPEAKER_LEVEL : SipConfigManager.SND_SPEAKER_LEVEL;
					configurationService.setPreferenceFloat(key, newValue);
					break;
				case R.id.micro_level:
					sipService.confAdjustRxLevel(0, newValue);
					key =  useBT ? SipConfigManager.SND_BT_MIC_LEVEL : SipConfigManager.SND_MIC_LEVEL;
					configurationService.setPreferenceFloat(key, newValue);
					break;
				}
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Impossible to set mic/speaker level", e);
			}
			
		}else {
			//TODO : revert changes here !
		}
		
		//Update quit timer
		if(isAutoClose) {
			delayedQuit(AUTO_QUIT_DELAY);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// Nothing to do
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// Nothing to do
	}


	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean value) {
		if(sipService != null && configurationService != null) {
			try {
				switch(arg0.getId()) {
				case R.id.echo_cancellation:
					sipService.setEchoCancellation(value);
					configurationService.setPreferenceBoolean(SipConfigManager.ECHO_CANCELLATION, value);
					break;
				}
				//Update quit timer
				if(isAutoClose) {
					delayedQuit(AUTO_QUIT_DELAY);
				}
			}catch (RemoteException e) {
				Log.e(THIS_FILE, "Impossible to set mic/speaker level", e);
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		
		switch(v.getId()) {
		/*
		case R.id.record:
			if(sipService != null) {
				try {
					if(sipService.getRecordedCall() != -1) {
						sipService.stopRecording();
					}else {
						int callId = -1;
						CallInfo[] calls = sipService.getCalls();
						for(CallInfo call : calls) {
							if(call.isActive()) {
								callId = call.getCallId();
								break;
							}
						}
						if(callId != -1) {
							sipService.startRecording(callId);
						}
					}
					
					updateCallButton();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Impossible to record", e);
				}
			}
			break;
			*/
		case R.id.save_bt:
			finish();
			break;
		}
	}
	
	/*
	private void updateCallButton() {
		if(sipService != null) {
			try {
				boolean isRecording = (sipService.getRecordedCall() != -1);
				recordButton.setText(isRecording?"Stop":"Record");
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Impossible to find record state", e);
			}
		}
	}*/
}
