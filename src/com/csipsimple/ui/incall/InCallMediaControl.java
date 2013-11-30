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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;

import java.util.Timer;
import java.util.TimerTask;

public class InCallMediaControl extends Activity implements OnSeekBarChangeListener, OnCheckedChangeListener, OnClickListener {
	protected static final String THIS_FILE = "inCallMediaCtrl";
	private SeekBar speakerAmplification;
	private SeekBar microAmplification;
	private Button saveButton;
	private CheckBox echoCancellation;
//	private Button recordButton;
	
	private boolean isAutoClose = false;
	
	private final static int AUTO_QUIT_DELAY = 3000;
	private Timer quitTimer;
    private ProgressBar txProgress;
    private ProgressBar rxProgress;
    private LinearLayout okBar;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.in_call_media);


		
		speakerAmplification = (SeekBar) findViewById(R.id.speaker_level);
		microAmplification = (SeekBar) findViewById(R.id.micro_level);
		saveButton = (Button) findViewById(R.id.save_bt);
		echoCancellation = (CheckBox) findViewById(R.id.echo_cancellation);
		okBar = (LinearLayout) findViewById(R.id.ok_bar);
		
		speakerAmplification.setMax((int) (max * subdivision * 2));
		microAmplification.setMax((int) (max * subdivision * 2));
		
		speakerAmplification.setOnSeekBarChangeListener(this);
		microAmplification.setOnSeekBarChangeListener(this);
		
		saveButton.setOnClickListener(this);
		
		echoCancellation.setOnCheckedChangeListener(this);
		
        rxProgress = (ProgressBar) findViewById(R.id.rx_bar);
        txProgress = (ProgressBar) findViewById(R.id.tx_bar);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Intent sipServiceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
        // Optional, but here we bundle so just ensure we are using csipsimple package
		sipServiceIntent.setPackage(getPackageName());
		bindService(sipServiceIntent , sipConnection, BIND_AUTO_CREATE);
		
		
		int direction = getIntent().getIntExtra(Intent.EXTRA_KEY_EVENT, 0);
		if(direction == AudioManager.ADJUST_LOWER  || direction == AudioManager.ADJUST_RAISE) {
			isAutoClose = true;
			okBar.setVisibility(View.GONE);
			delayedQuit(AUTO_QUIT_DELAY);
		}else {
		    okBar.setVisibility(View.VISIBLE);
			isAutoClose = false;
		}
		
		registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
        if (monitorThread == null) {
            monitorThread = new MonitorThread();
            monitorThread.start();
        }
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
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

        if (monitorThread != null) {
            monitorThread.markFinished();
            monitorThread = null;
        }
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

        boolean useBT = false;
        if (sipService != null) {
            try {
                useBT = sipService.getCurrentMediaState().isBluetoothScoOn;
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Sip service not avail for request ", e);
            }
        }

        Float speakerLevel = SipConfigManager.getPreferenceFloatValue(this, useBT ?
                SipConfigManager.SND_BT_SPEAKER_LEVEL : SipConfigManager.SND_SPEAKER_LEVEL);
        speakerAmplification.setProgress(valueToProgressUnit(speakerLevel));

        Float microLevel = SipConfigManager.getPreferenceFloatValue(this, useBT ?
                SipConfigManager.SND_BT_MIC_LEVEL : SipConfigManager.SND_MIC_LEVEL);
        microAmplification.setProgress(valueToProgressUnit(microLevel));

        echoCancellation.setChecked(SipConfigManager.getPreferenceBooleanValue(this,
                SipConfigManager.ECHO_CANCELLATION));

    }

    private double subdivision = 5;
    private double max = 15;
    
    private int valueToProgressUnit(float val) {
        Log.d(THIS_FILE, "Value is " + val);
        double dB = (10.0f * Math.log10(val));
        return (int) ( (dB + max) * subdivision);
    }
    
    private float progressUnitToValue(int pVal) {
        Log.d(THIS_FILE, "Progress is " + pVal);
        double dB = pVal / subdivision - max;
        return (float) Math.pow(10, dB / 10.0f);
    }

	@Override
	public void onProgressChanged(SeekBar arg0, int value, boolean arg2) {
		Log.d(THIS_FILE, "Progress has changed");
		if(sipService != null) {
			try {
				float newValue = progressUnitToValue( value );
				String key;
				boolean useBT = sipService.getCurrentMediaState().isBluetoothScoOn;
				int sId = arg0.getId();
				if (sId == R.id.speaker_level) {
					sipService.confAdjustTxLevel(0, newValue);
					key =  useBT ? SipConfigManager.SND_BT_SPEAKER_LEVEL : SipConfigManager.SND_SPEAKER_LEVEL;
					SipConfigManager.setPreferenceFloatValue(this, key, newValue);
				} else if (sId == R.id.micro_level) {
					sipService.confAdjustRxLevel(0, newValue);
					key =  useBT ? SipConfigManager.SND_BT_MIC_LEVEL : SipConfigManager.SND_MIC_LEVEL;
					SipConfigManager.setPreferenceFloatValue(this, key, newValue);
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
		if(sipService != null) {
			try {
				int bId = arg0.getId();
				if (bId == R.id.echo_cancellation) {
					sipService.setEchoCancellation(value);
					SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ECHO_CANCELLATION, value);
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
		if (v.getId() == R.id.save_bt) {
			finish();
		}
	}

    private MonitorThread monitorThread;

    private class MonitorThread extends Thread {
        private boolean finished = false;

        public synchronized void markFinished() {
            finished = true;
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                if (sipService != null) {
                    try {
                        long value = sipService.confGetRxTxLevel(0);
                        runOnUiThread(new UpdateConfLevelRunnable((int) ((value >> 8) & 0xff), (int) (value & 0xff)));
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Problem with remote service", e);
                        break;
                    }
                }

                // End of loop, sleep for a while and exit if necessary
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    Log.e(THIS_FILE, "Interupted monitor thread", e);
                }
                synchronized (this) {
                    if (finished) {
                        break;
                    }
                }
            }
        }
    }

    private class UpdateConfLevelRunnable implements Runnable {
        private final int mRx, mTx;
        UpdateConfLevelRunnable(int rx, int tx){
            mRx = rx;
            mTx = tx;
        }
        @Override
        public void run() {
            txProgress.setProgress(mTx);
            rxProgress.setProgress(mRx);
        }
    }
    
}
