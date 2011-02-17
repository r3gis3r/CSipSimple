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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.animation.Flip3dAnimation;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.ISipService;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.AccountChooserButton;
import com.csipsimple.widgets.Dialpad;
import com.csipsimple.widgets.Dialpad.OnDialKeyListener;
import com.csipsimple.widgets.EditSipUri;
import com.csipsimple.widgets.EditSipUri.ToCall;

public class Dialer extends Activity implements OnClickListener, OnLongClickListener, OnDialKeyListener, TextWatcher {

	private static final String THIS_FILE = "Dialer";

	private Drawable digitsBackground, digitsEmptyBackground;
	private EditText digits;
	private ImageButton dialButton, deleteButton;

	private View digitDialer, textDialer, rootView;

	private LinearLayout digitsWrapper;
	private AccountChooserButton accountChooserButton;
	private boolean isDigit;

	private DialingFeedback dialFeedback;

	private int[] buttonsToAttach = new int[] { R.id.button0, R.id.dialButton, R.id.deleteButton, R.id.domainButton,
	// Text dialer
			R.id.dialTextButton, R.id.deleteTextButton, R.id.domainTextButton };

	private Activity contextToBindTo = this;
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			accountChooserButton.updateService(service);
			sipTextUri.updateService(service);
			updateRegistrations();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}

	};

	private GestureDetector gestureDetector;
	private Dialpad dialPad;
	
	private PreferencesWrapper prefsWrapper;
	private BroadcastReceiver registrationReceiver;
	//private TextView domainTextHelper;

	private EditSipUri sipTextUri;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Bind to the service
		if (getParent() != null) {
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
		digitsWrapper = (LinearLayout) findViewById(R.id.topField);
		dialPad = (Dialpad) findViewById(R.id.dialPad);
		digitDialer = (View) findViewById(R.id.dialer_digit);
		textDialer = (View) findViewById(R.id.dialer_text);
		sipTextUri = (EditSipUri) findViewById(R.id.dialer_edit_text);
		// dialDomain = (EditText) findViewById(R.id.dialtext_domain);
		rootView = (View) findViewById(R.id.toplevel);
		accountChooserButton = (AccountChooserButton) findViewById(R.id.accountChooserButton);

		isDigit = prefsWrapper.startIsDigit();
		digitDialer.setVisibility(isDigit ? View.VISIBLE : View.GONE);
		textDialer.setVisibility(isDigit ? View.GONE : View.VISIBLE);

		dialPad.setOnDialKeyListener(this);
		initButtons();

		// Add gesture detector
		gestureDetector = new GestureDetector(this, new SwitchDialerGestureDetector());

		// Add switcher gesture detector
		OnTouchListener touchTransmiter = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};
		digitDialer.setOnTouchListener(touchTransmiter);
		textDialer.setOnTouchListener(touchTransmiter);
		
		
		

		dialFeedback = new DialingFeedback(this, false);

		registrationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateRegistrations();
			}
		};
		

	}

	protected void updateRegistrations() {
		Log.d(THIS_FILE, "Update chooser choice");
		boolean canChangeIfValid = TextUtils.isEmpty(digits.getText().toString());
		accountChooserButton.updateRegistration(canChangeIfValid);
		
		sipTextUri.updateRegistration();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(THIS_FILE, "--- DIALER DESTROYED ---");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(THIS_FILE, "Resuming dialer");
		// Bind service
		registerReceiver(registrationReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		contextToBindTo.bindService(new Intent(contextToBindTo, SipService.class), connection, Context.BIND_AUTO_CREATE);

		dialFeedback.resume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(THIS_FILE, "Pausing dialer");
		// Unbind service

		try {
			unregisterReceiver(registrationReceiver);
		} catch (Exception e) {
			Log.e(THIS_FILE, "Not possible to unregister ", e);
		}
		
		try {
			contextToBindTo.unbindService(connection);
		}catch (Exception e) {
			//Just ignore that -- TODO : should be more clean
			Log.w(THIS_FILE, "Unable to un bind", e);
		}

		dialFeedback.pause();
	}
	

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			// Hide soft keyboard, if visible (it's fugly over button dialer).
			// The only known case where this will be true is when launching the
			// dialer with
			// ACTION_DIAL via a soft keyboard. we dismiss it here because we
			// don't
			// have a window token yet in onCreate / onNewIntent
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(digits.getWindowToken(), 0);
		}
	}

	private void attachButtonListener(int id) {
		Log.d(THIS_FILE, "Attaching " + id);
		ImageButton button = (ImageButton) findViewById(id);
		button.setOnClickListener(this);

		if (id == R.id.button0 || id == R.id.deleteButton) {
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK : {
			onBackPressed();
			//return true;
			break;
		}
		}

		return super.onKeyUp(keyCode, event);
	}
	
	public void onBackPressed() {
		if(contextToBindTo != null && contextToBindTo instanceof SipHome) {
			((SipHome) contextToBindTo).onBackPressed();
		}else {
			finish();
		}
	}

	private static final int USE_GSM = -2;

	private void placeCall() {
		if (service == null) {
			return;
		}
		String toCall = "";
		Integer accountToUse = USE_GSM;

		if (isDigit) {
			toCall = PhoneNumberUtils.stripSeparators(digits.getText().toString());
			SipProfile acc = accountChooserButton.getSelectedAccount();
			if (acc != null) {
				accountToUse = acc.id;
			}
		} else {
			ToCall objToCall = sipTextUri.getValue();
			if (objToCall == null) {
				return;
			}
			if(objToCall.getAccountId() != null) {
				accountToUse = objToCall.getAccountId(); 
			}
			toCall = objToCall.getCallee();
		}
		if (TextUtils.isEmpty(toCall)) {
			return;
		}

		// Well we have now the fields, clear theses fields
		digits.getText().clear();
		sipTextUri.clear();
		// dialDomain.getText().clear();
		if (accountToUse != USE_GSM) {
			try {
				service.makeCall(toCall, accountToUse);
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Service can't be called to make the call");
			}
		} else {
			OutgoingCall.ignoreNext = toCall;
			Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", toCall, null));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}

	}

	public void onClick(View view) {
		// ImageButton b = null;
		int view_id = view.getId();
		Log.d(THIS_FILE, "Im clicked....");

		// if (view_id != R.id.digitsText) { // Prevent IllegalCast if add
		// non-Button here!
		// b = (ImageButton)view;
		// }

		switch (view_id) {

		case R.id.button0: {
			dialFeedback.giveFeedback(ToneGenerator.TONE_DTMF_0);
			keyPressed(KeyEvent.KEYCODE_0);
			break;
		}
		case R.id.deleteButton: {
			// TODO (dc3denny) How to get this to come through speaker. Regis is
			// doing
			// things with audio and Bluetooth, so I'm not going to do anything
			// here!
			// Commented out in other places... and creating 'b' above
			//
			// UPDATE: in r194 the keypress sounds are playing anyway! This
			// stuff
			// can probably be removed.
			//
			// b.playSoundEffect(SoundEffectConstants.CLICK); // Plays through
			// earpiece (typ.)
			keyPressed(KeyEvent.KEYCODE_DEL);
			break;
		}
		case R.id.deleteTextButton: {
			sipTextUri.clear();
			break;
		}
		case R.id.dialButton:
		case R.id.dialTextButton: {
			placeCall();
			break;
		}
		case R.id.domainButton: {
			// b.playSoundEffect(SoundEffectConstants.CLICK);
			flipView(true);
			break;
		}
		case R.id.domainTextButton: {
			// b.playSoundEffect(SoundEffectConstants.CLICK);
			flipView(false);
			break;
		}

		case R.id.digitsText: {
			if (digits.length() != 0) {
				digits.setCursorVisible(true);
			}
			break;
		}
		}
	}

	public boolean onLongClick(View view) {
		// ImageButton b = (ImageButton)view;
		switch (view.getId()) {
		case R.id.button0: {
			// b.playSoundEffect(SoundEffectConstants.CLICK);
			dialFeedback.hapticFeedback();
			keyPressed(KeyEvent.KEYCODE_PLUS);
			return true;
		}
		case R.id.deleteButton: {
			// b.playSoundEffect(SoundEffectConstants.CLICK);
			digits.getText().clear();
			deleteButton.setPressed(false);
			return true;
		}
		}
		return false;
	}

	public void afterTextChanged(Editable input) {
		//Change state of digit dialer
		final boolean notEmpty = digits.length() != 0;
		digitsWrapper.setBackgroundDrawable(notEmpty ? digitsBackground : digitsEmptyBackground);
		digitsWrapper.setPadding(15, 0, 0, 2);
		dialButton.setEnabled(notEmpty);
		deleteButton.setEnabled(notEmpty);
		
		if(!notEmpty) {
			digits.setCursorVisible(false);
		}
	}
	

	private void flipView(boolean forward) {
		if (forward && !isDigit) {
			return;
		}
		if (!forward && isDigit) {
			return;
		}

		isDigit = !isDigit;
		if(Compatibility.useFlipAnimation()) {
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
		}else {
			digitDialer.setVisibility(isDigit?View.VISIBLE:View.GONE);
			textDialer.setVisibility(isDigit?View.GONE:View.VISIBLE);
		}
	}

	// Gesture detector
	private class SwitchDialerGestureDetector extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (e1 == null || e2 == null) {
				return false;
			}
			float deltaX = e2.getX() - e1.getX();
			float deltaY = e2.getY() - e1.getY();
			if (Math.abs(deltaX) > Math.abs(deltaY * 5)) {
				if (deltaX > 0) {
					flipView(true);
				} else {
					flipView(false);
				}

				return true;
			}
			return false;
		}
	}

	@Override
	public void onTrigger(int keyCode, int dialTone) {
		dialFeedback.giveFeedback(dialTone);
		keyPressed(keyCode);
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// Nothing to do here

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		afterTextChanged(digits.getText());

	}

}
