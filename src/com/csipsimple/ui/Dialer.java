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

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.method.DialerKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.animation.Flip3dAnimation;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

public class Dialer extends Activity implements OnClickListener,
		OnLongClickListener {
	
	
	private ToneGenerator mToneGenerator;
	private Object mToneGeneratorLock = new Object();
	private static final String THIS_FILE = "Dialer";

	private Drawable mDigitsBackground;
	private Drawable mDigitsEmptyBackground;
	private EditText digitsView;
	private ImageButton dialButton, deleteButton;
	private Vibrator mVibrator;
	
	private View mDigitDialer, mTextDialer, mRootView;
	private boolean isDigit;
	
	
	private int[] buttonsToAttach = new int[] {
		//Digital dialer
		R.id.button0,
		R.id.button1,
		R.id.button2,
		R.id.button3,
		R.id.button4,
		R.id.button5,
		R.id.button6,
		R.id.button7,
		R.id.button8,
		R.id.button9,
		R.id.buttonstar,
		R.id.buttonpound,
		R.id.dialButton,
		R.id.deleteButton,
		R.id.domainButton,
		//Text dialer
		R.id.dialTextButton,
		R.id.deleteTextButton,
		R.id.domainTextButton
	};
	
	private HashMap<Integer, int[]> digitsButtons;
	

	private Activity mToBindTo = this;
	private ISipService mService;
	private ServiceConnection mConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			Log.d(THIS_FILE, "now i am binded");
			mService = ISipService.Stub.asInterface(arg1);
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.d(THIS_FILE, "Unbind done");
			mService = null;
		}
		
    };
	private int ringerMode;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Bind to the service
		Log.d(THIS_FILE, "Try to bind service");
		if(getParent() != null) {
			mToBindTo = getParent();
		}
		setContentView(R.layout.dialer_activity);

		
		//Pfff java is so so verbose.... if only it was python ...
		//I don't want to use introspection since could impact perfs, so the best way i found is using a map
		digitsButtons = new HashMap<Integer, int[]>();
		digitsButtons.put(R.id.button0, new int[] {ToneGenerator.TONE_DTMF_0, KeyEvent.KEYCODE_0});
		digitsButtons.put(R.id.button1, new int[] {ToneGenerator.TONE_DTMF_1, KeyEvent.KEYCODE_1});
		digitsButtons.put(R.id.button2, new int[] {ToneGenerator.TONE_DTMF_2, KeyEvent.KEYCODE_2});
		digitsButtons.put(R.id.button3, new int[] {ToneGenerator.TONE_DTMF_3, KeyEvent.KEYCODE_3});
		digitsButtons.put(R.id.button4, new int[] {ToneGenerator.TONE_DTMF_4, KeyEvent.KEYCODE_4});
		digitsButtons.put(R.id.button5, new int[] {ToneGenerator.TONE_DTMF_5, KeyEvent.KEYCODE_5});
		digitsButtons.put(R.id.button6, new int[] {ToneGenerator.TONE_DTMF_6, KeyEvent.KEYCODE_6});
		digitsButtons.put(R.id.button7, new int[] {ToneGenerator.TONE_DTMF_7, KeyEvent.KEYCODE_7});
		digitsButtons.put(R.id.button8, new int[] {ToneGenerator.TONE_DTMF_8, KeyEvent.KEYCODE_8});
		digitsButtons.put(R.id.button9, new int[] {ToneGenerator.TONE_DTMF_9, KeyEvent.KEYCODE_9});
		digitsButtons.put(R.id.buttonpound, new int[] {ToneGenerator.TONE_DTMF_P, KeyEvent.KEYCODE_POUND});
		digitsButtons.put(R.id.buttonstar, new int[] {ToneGenerator.TONE_DTMF_S, KeyEvent.KEYCODE_STAR});
		
		
		// Store the backgrounds objects that will be in use later
		Resources r = getResources();
		mDigitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
		mDigitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);
		
		
		// Store some object that could be useful later
		dialButton = (ImageButton) findViewById(R.id.dialButton);
		deleteButton = (ImageButton) findViewById(R.id.deleteButton);
		digitsView = (EditText) findViewById(R.id.digitsText);
		mDigitDialer = (View) findViewById(R.id.dialer_digit);
		mTextDialer = (View) findViewById(R.id.dialer_text);
		mRootView = (View) findViewById(R.id.toplevel);
		
		
		// @ is a special char for layouts, I didn't find another way to set @ as text in xml
		TextView atxt = (TextView) findViewById(R.id.arobase_txt);
		atxt.setText("@");
		
		
		//TODO : set default in params
		isDigit = true;
		mDigitDialer.setVisibility(View.VISIBLE);
		mTextDialer.setVisibility(View.GONE);
		
		
		initButtons();
	}

	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	Log.d(THIS_FILE, "Dialer destroyed");
    }
	
    
	@Override
	protected void onResume() {
		super.onResume();
		
		//Bind service
		mToBindTo.bindService(new Intent(mToBindTo, SipService.class), mConnection, Context.BIND_AUTO_CREATE);

		//Create dialtone just for user feedback
		synchronized (mToneGeneratorLock) {
			if (mToneGenerator == null) {
				try {
					mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
					//Allow user to control dialtone
					setVolumeControlStream(AudioManager.STREAM_MUSIC);
				} catch (RuntimeException e) {
					//If impossible, nothing to do
					mToneGenerator = null;
				}
			}
		}
		
		//Create the virator
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		//Store the current ringer mode
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		ringerMode = am.getRingerMode();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		//Unbind service
		//TODO : should be done by a cleaner way (check if bind function has been launched is better than check if bind has been done)
		if(mService != null) {
    		mToBindTo.unbindService(mConnection);
    	}
		
		//Destroy dialtone
		synchronized (mToneGeneratorLock) {
			if (mToneGenerator != null) {
				mToneGenerator.release();
				mToneGenerator = null;
			}
		}
	}
	
	private void attachButtonListener(int id) {
		ImageButton button = (ImageButton) findViewById(id);
		button.setOnClickListener(this);

		if (id == R.id.button0 || id == R.id.button1 || id == R.id.deleteButton) {
			button.setOnLongClickListener(this);
		}
	}

	private void initButtons() {
		for (int btn_id : buttonsToAttach) {
			attachButtonListener(btn_id);
		}
		digitsView.setOnClickListener(this);
		digitsView.setKeyListener(DialerKeyListener.getInstance());
		digitsView.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
		digitsView.setInputType(android.text.InputType.TYPE_NULL);
		toggleDrawable();
	}
	

	private void playTone(int tone) {
		boolean silent = (ringerMode == AudioManager.RINGER_MODE_SILENT) || (ringerMode == AudioManager.RINGER_MODE_VIBRATE);
		//TODO add user pref for that
		boolean vibrate = silent || true;
		
		if(vibrate) {
			mVibrator.vibrate(30);
		}
		if(silent) {
			return;
		}

		synchronized (mToneGeneratorLock) {
			if (mToneGenerator == null) {
				return;
			}
			mToneGenerator.startTone(tone);
			
			//TODO : see if it could not be factorized
			Timer toneTimer = new Timer();
			toneTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					synchronized (mToneGeneratorLock) {
						if (mToneGenerator == null) {
							return;
						}
						mToneGenerator.stopTone();
					}
				}
			}, 100);
		}
	}

	private void keyPressed(int keyCode) {
		// vibrate();
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		digitsView.onKeyDown(keyCode, event);
	}

	public void onClick(View view) {
		int view_id = view.getId();
		if (digitsButtons.containsKey(view.getId())) {
			int[] btn_infos = digitsButtons.get(view_id);
			playTone(btn_infos[0]);
			keyPressed(btn_infos[1]);
		} else {

			switch (view_id) {
			case R.id.deleteButton: {
				keyPressed(KeyEvent.KEYCODE_DEL);
				break;
			}
			case R.id.deleteTextButton: {
				EditText et;
				et = (EditText) findViewById(R.id.dialtxt_user);
				et.setText("");
				et = (EditText) findViewById(R.id.dialtext_domain);
				et.setText("");

				break;
			}
			case R.id.dialButton: {
				if (mService != null) {
					try {
						mService.makeCall(digitsView.getText().toString());
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Service can't be called to make the call");
					}
				}
				break;
			}
			case R.id.dialTextButton: {
				if (mService != null) {
					try {
						// TODO: allow to choose between sip and sips
						String callee = "sip:";
						EditText et;
						et = (EditText) findViewById(R.id.dialtxt_user);
						callee += et.getText();
						callee += "@";
						et = (EditText) findViewById(R.id.dialtext_domain);
						callee += et.getText();
						mService.makeCall(callee);
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Service can't be called to make the call");
					}
				}
				break;
			}
			case R.id.domainButton: {
				flipView(true);
				break;
			}
			case R.id.domainTextButton: {
				flipView(false);
				break;
			}

			case R.id.digitsText: {
				digitsView.setCursorVisible(false);
				if (digitsView.length() != 0) {
					digitsView.setCursorVisible(true);
				}
				break;
			}
			}
		}
		toggleDrawable();
	}

	public boolean onLongClick(View view) {
		boolean result = false;
		switch (view.getId()) {
			case R.id.button0: {
				keyPressed(KeyEvent.KEYCODE_PLUS);
				result = true;
				break;
			}
			case R.id.deleteButton: {
				removeAll();
				deleteButton.setPressed(false);
				result = true;
				break;
			}
		}
		toggleDrawable();
		return result;
	}

	private void removeAll() {
		digitsView.getText().clear();
	}


	private void toggleDrawable() {
		final boolean notEmpty = digitsView.length() != 0;
		if (notEmpty) {
			digitsView.setBackgroundDrawable(mDigitsBackground);
			dialButton.setEnabled(true);
			deleteButton.setEnabled(true);
		} else {
			digitsView.setCursorVisible(false);
			digitsView.setBackgroundDrawable(mDigitsEmptyBackground);
			dialButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}
	}
	
	
	private void flipView(boolean forward) {
		if(forward && !isDigit) {
			return;
		}
		if(!forward && isDigit) {
			return;
		}
		
		isDigit = !isDigit;
	    int cx = mRootView.getWidth() / 2;
	    int cy = mRootView.getHeight() / 2;
	    Animation animation = new Flip3dAnimation(mDigitDialer, mTextDialer, cx, cy, forward);
	    animation.setAnimationListener(new AnimationListener() {
	      @Override
	      public void onAnimationEnd(Animation animation) {
	      }
	      @Override
	      public void onAnimationRepeat(Animation animation) {
	      }
	      @Override
	      public void onAnimationStart(Animation animation) {
	      }
	    });
	    
	    mRootView.startAnimation(animation);
	}

}
