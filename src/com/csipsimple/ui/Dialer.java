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
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.animation.Flip3dAnimation;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;

public class Dialer extends Activity implements OnClickListener,
		OnLongClickListener, OnDialKeyListener, TextWatcher {
	
    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_MUSIC;


	
	private ToneGenerator toneGenerator;
	private Object toneGeneratorLock = new Object();
	private static final String THIS_FILE = "Dialer";

	private Drawable digitsBackground, digitsEmptyBackground;
	private EditText digits;
	private ImageButton dialButton, deleteButton;
	private Vibrator vibrator;
	
	private View digitDialer, textDialer, rootView;
	private boolean isDigit;
	
	
	private int[] buttonsToAttach = new int[] {
		R.id.dialButton,
		R.id.deleteButton,
		R.id.domainButton,
		//Text dialer
		R.id.dialTextButton,
		R.id.deleteTextButton,
		R.id.domainTextButton
	};
	
	

	private Activity contextToBindTo = this;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}
		
    };
	private int ringerMode;
	private GestureDetector gestureDetector;
	private Dialpad dialPad;

	private EditText dialUser;

	private EditText dialDomain;

	private PreferencesWrapper prefsWrapper;

	private Timer toneTimer;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Bind to the service
		if(getParent() != null) {
			contextToBindTo = getParent();
		}
		
		prefsWrapper = new PreferencesWrapper(this);
		
		setContentView(R.layout.dialer_activity);
		
		
		// Store the backgrounds objects that will be in use later
		Resources r = getResources();
		digitsBackground = r.getDrawable(R.drawable.btn_dial_textfield_active);
		digitsEmptyBackground = r.getDrawable(R.drawable.btn_dial_textfield_normal);
		
		
		// Store some object that could be useful later
		dialButton = (ImageButton) findViewById(R.id.dialButton);
		deleteButton = (ImageButton) findViewById(R.id.deleteButton);
		digits = (EditText) findViewById(R.id.digitsText);
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		digitDialer = (View) findViewById(R.id.dialer_digit);
		textDialer = (View) findViewById(R.id.dialer_text);
		dialUser = (EditText) findViewById(R.id.dialtxt_user);
		dialDomain = (EditText) findViewById(R.id.dialtext_domain);
		rootView = (View) findViewById(R.id.toplevel);
		
		
		
		// @ is a special char for layouts, I didn't find another way to set @ as text in xml
		TextView atxt = (TextView) findViewById(R.id.arobase_txt);
		atxt.setText("@");
		
		
		isDigit = prefsWrapper.startIsDigit();
		digitDialer.setVisibility(isDigit?View.VISIBLE:View.GONE);
		textDialer.setVisibility(isDigit?View.GONE:View.VISIBLE);

		dialPad.setOnDialKeyListener(this);
		initButtons();
		
		
		//Add gesture detector
		gestureDetector = new GestureDetector(this, new SwitchDialerGestureDetector());
		
		//Add switcher gesture detector
		OnTouchListener touchTransmiter = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		digitDialer.setOnTouchListener(touchTransmiter);
		textDialer.setOnTouchListener(touchTransmiter);
		
		
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
		contextToBindTo.bindService(new Intent(contextToBindTo, SipService.class), connection, Context.BIND_AUTO_CREATE);

		//Create dialtone just for user feedback
		synchronized (toneGeneratorLock) {
			if(toneTimer == null) {
				toneTimer = new Timer();
			}
			if (toneGenerator == null) {
				try {
					toneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
					//Allow user to control dialtone
					setVolumeControlStream(DIAL_TONE_STREAM_TYPE);
				} catch (RuntimeException e) {
					//If impossible, nothing to do
					toneGenerator = null;
				}
			}
		}
		
		//Create the virator
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		//Store the current ringer mode
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		ringerMode = am.getRingerMode();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		//Unbind service
		//TODO : should be done by a cleaner way (check if bind function has been launched is better than check if bind has been done)
		if(service != null) {
    		contextToBindTo.unbindService(connection);
    	}
		
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
	
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            // Hide soft keyboard, if visible (it's fugly over button dialer).
            // The only known case where this will be true is when launching the dialer with
            // ACTION_DIAL via a soft keyboard.  we dismiss it here because we don't
            // have a window token yet in onCreate / onNewIntent
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(digits.getWindowToken(), 0);
        }
    }

	
	
	private void attachButtonListener(int id) {
		Log.d(THIS_FILE, "Attaching "+id);
		ImageButton button = (ImageButton) findViewById(id);
		button.setOnClickListener(this);

		if (id == R.id.button0 || id == R.id.button1 || id == R.id.deleteButton) {
			button.setOnLongClickListener(this);
		}
	}

	private void initButtons() {
		for (int buttonId : buttonsToAttach) {
			attachButtonListener(buttonId);
		}
		
		digits.setOnClickListener(this);
		digits.setKeyListener(DialerKeyListener.getInstance());
		PhoneNumberFormattingTextWatcher digitFormater = new PhoneNumberFormattingTextWatcher();
		digits.addTextChangedListener(digitFormater);
		digits.addTextChangedListener(this);
		digits.setInputType(android.text.InputType.TYPE_NULL);
		digits.setCursorVisible(false);
		afterTextChanged(digits.getText());
	}
	

	private void playTone(int tone) {
		boolean silent = (ringerMode == AudioManager.RINGER_MODE_SILENT) || (ringerMode == AudioManager.RINGER_MODE_VIBRATE);
		//TODO add user pref for that
		boolean vibrate = silent || true;
		
		if(vibrate) {
			vibrator.vibrate(30);
		}
		if(silent) {
			return;
		}

		synchronized (toneGeneratorLock) {
			if (toneGenerator == null) {
				return;
			}
			toneGenerator.startTone(tone);
			
			//TODO : see if it could not be factorized
			toneTimer.schedule(new StopTimerTask(), TONE_LENGTH_MS);
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
	

	private void keyPressed(int keyCode) {
		KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		digits.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		 switch (keyCode) {
	         case KeyEvent.KEYCODE_CALL: {
	        	 placeCall();
	             return true;
	         }
	     }

		return super.onKeyUp(keyCode, event);
	}
	
	private void placeCall() {
		if(service == null) {
			return;
		}
		String toCall = "";
		if(isDigit) {
			toCall = PhoneNumberUtils.stripSeparators(digits.getText().toString());
		}else {
			String userName = dialUser.getText().toString();
			String domain = dialDomain.getText().toString();
			if(TextUtils.isEmpty(userName) || TextUtils.isEmpty(domain)) {
				return;
			}
			toCall = "sip:"+userName+"@"+domain;
		}
		if(TextUtils.isEmpty(toCall) ) {
			return;
		}
		
		//Well we have now the fields, clear theses fields
		digits.getText().clear();
		dialUser.getText().clear();
		dialDomain.getText().clear();
		try {
			service.makeCall(toCall);
		} catch (RemoteException e) {
			Log.e(THIS_FILE, "Service can't be called to make the call");
		}
		
	}

	public void onClick(View view) {
		int view_id = view.getId();
		Log.d(THIS_FILE, "Im clicked....");

		switch (view_id) {
		case R.id.deleteButton: {
			keyPressed(KeyEvent.KEYCODE_DEL);
			break;
		}
		case R.id.deleteTextButton: {
			dialUser.getText().clear();
			dialDomain.getText().clear();
			break;
		}
		case R.id.dialButton: 
		case R.id.dialTextButton: {
			placeCall();
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
			digits.setCursorVisible(false);
			break;
		}
		}
	}

	public boolean onLongClick(View view) {
		switch (view.getId()) {
			case R.id.button0: {
				keyPressed(KeyEvent.KEYCODE_PLUS);
				return true;
			}
			case R.id.deleteButton: {
				digits.getText().clear();
				deleteButton.setPressed(false);
				return true;
			}
		}
		return false;
	}
	
	public void afterTextChanged(Editable input) {

		final boolean notEmpty = digits.length() != 0;
		digits.setBackgroundDrawable(notEmpty? digitsBackground : digitsEmptyBackground);
		dialButton.setEnabled(notEmpty);
		deleteButton.setEnabled(notEmpty);
	}

	
	private void flipView(boolean forward) {
		if(forward && !isDigit) {
			return;
		}
		if(!forward && isDigit) {
			return;
		}
		
		isDigit = !isDigit;
	    int cx = rootView.getWidth() / 2;
	    int cy = rootView.getHeight() / 2;
	    Animation animation = new Flip3dAnimation(digitDialer, textDialer, cx, cy, forward);
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
	    
	    rootView.startAnimation(animation);
	}
	
	
	// Gesture detector
	private class SwitchDialerGestureDetector extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if(e1 == null || e2 == null) {
				return false;
			}
			float deltaX = e2.getX() - e1.getX();
			float deltaY = e2.getY() - e1.getY();
			if(Math.abs(deltaX) > Math.abs(deltaY * 5)) {
				if(deltaX > 0 ) {
					flipView(true);
				}else {
					flipView(false);
				}
				
				return true;
			}
			return false;
		}
	}


	@Override
	public void onTrigger(int keyCode, int dialTone) {
		playTone(dialTone);
		keyPressed(keyCode);
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		//Nothing to do here
		
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		afterTextChanged(digits.getText());
		
	}

}
