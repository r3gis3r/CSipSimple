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
package com.csipsimple.widgets;

import org.pjsip.pjsua.pjsip_inv_state;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.SlidingTab.OnTriggerListener;

public class InCallControls extends FrameLayout implements OnTriggerListener, OnClickListener {

	private static final String THIS_FILE = "InCallControls";
	OnTriggerListener onTriggerListener;
	private SlidingTab slidingTabWidget;
	private Button clearCallButton, dialButton;
	private ToggleButton bluetoothButton, speakerButton, muteButton;
	private RelativeLayout inCallButtons;
	private boolean isDialpadOn = false;
	private Button takeCallButton, declineCallButton;
	ImageButton detailsButton;
	private boolean useSlider;
	private LinearLayout alternateLockerWidget;
	
	
	private static final int MODE_LOCKER = 0;
	private static final int MODE_CONTROL = 1;
	private static final int MODE_NO_ACTION = 2;
	private int controlMode;

	/**
	 * Interface definition for a callback to be invoked when a tab is triggered
	 * by moving it beyond a target zone.
	 */
	public interface OnTriggerListener {
		/**
		 * When user clics on clear call
		 */
		public static final int CLEAR_CALL = 1;
		/**
		 * When user clics on take call
		 */
		public static final int TAKE_CALL = CLEAR_CALL + 1;
		/**
		 * When user clics on take call
		 */
		public static final int DECLINE_CALL = TAKE_CALL + 1;
		/**
		 * When user clics on dialpad
		 */
		public static final int DIALPAD_ON = DECLINE_CALL + 1;
		/**
		 * When user clics on dialpad
		 */
		public static final int DIALPAD_OFF = DIALPAD_ON + 1;
		/**
		 * When mute is set on
		 */
		public static final int MUTE_ON = DIALPAD_OFF + 1;
		/**
		 * When mute is set off
		 */
		public static final int MUTE_OFF = MUTE_ON + 1;
		/**
		 * When bluetooth is set on
		 */
		public static final int BLUETOOTH_ON = MUTE_OFF + 1;
		/**
		 * When bluetooth is set off
		 */
		public static final int BLUETOOTH_OFF = BLUETOOTH_ON + 1;
		/**
		 * When speaker is set on
		 */
		public static final int SPEAKER_ON = BLUETOOTH_OFF + 1;
		/**
		 * When speaker is set off
		 */
		public static final int SPEAKER_OFF = SPEAKER_ON + 1;
		/**
		 * When speaker is set off
		 */
		public static final int DETAILED_DISPLAY = SPEAKER_OFF + 1;
		

		/**
		 * Called when the user make an action
		 * 
		 * @param whichAction
		 *            what action has been done
		 */
		void onTrigger(int whichAction);
	}

	public InCallControls(Context context, AttributeSet attrs) {
		super(context, attrs);

		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.in_call_controls, this, true);
		PreferencesWrapper prefs = new PreferencesWrapper(context);
		useSlider = !prefs.getUseAlternateUnlocker();

	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		slidingTabWidget = (SlidingTab) findViewById(R.id.takeCallUnlocker);
		alternateLockerWidget = (LinearLayout) findViewById(R.id.takeCallUnlockerAlternate);
		inCallButtons = (RelativeLayout) findViewById(R.id.inCallButtons);

		clearCallButton = (Button) findViewById(R.id.clearCallButton);
		dialButton = (Button) findViewById(R.id.dialpadButton);
		bluetoothButton = (ToggleButton) findViewById(R.id.bluetoothButton);
		speakerButton = (ToggleButton) findViewById(R.id.speakerButton);
		muteButton = (ToggleButton) findViewById(R.id.muteButton);
		
		
		takeCallButton = (Button) findViewById(R.id.takeCallButton);
		declineCallButton = (Button) findViewById(R.id.declineCallButton);
		detailsButton = (ImageButton) findViewById(R.id.detailsButton);
		
		
		// Finalize object style
		slidingTabWidget.setLeftHintText(R.string.take_call);
		slidingTabWidget.setRightHintText(R.string.decline_call);
		setEnabledMediaButtons(false);
		controlMode = MODE_LOCKER;
		inCallButtons.setVisibility(GONE);
		setCallLockerVisibility(VISIBLE);
		inCallButtons.setVisibility(GONE);

		// Attach objects
		slidingTabWidget.setOnTriggerListener(this);
		clearCallButton.setOnClickListener(this);
		dialButton.setOnClickListener(this);
		bluetoothButton.setOnClickListener(this);
		speakerButton.setOnClickListener(this);
		muteButton.setOnClickListener(this);
		takeCallButton.setOnClickListener(this);
		declineCallButton.setOnClickListener(this);
		detailsButton.setOnClickListener(this);

	}

	public void setEnabledMediaButtons(boolean isInCall) {
		speakerButton.setEnabled(isInCall);
		muteButton.setEnabled(isInCall);
		bluetoothButton.setEnabled(isInCall);
		dialButton.setEnabled(isInCall);
	}
	
	
	private void setCallLockerVisibility(int visibility) {
		if(useSlider) {
			slidingTabWidget.setVisibility(visibility);
		}else {
			alternateLockerWidget.setVisibility(visibility);
		}
	}
	
	
	/**
	 * Toggle the mute button as if pressed by the user.
	 */
	public void toggleMuteButton() {
		muteButton.setChecked(!muteButton.isChecked());
		muteButton.performClick();
	}


	public void setCallState(CallInfo callInfo) {
		pjsip_inv_state state = callInfo.getCallState();
		switch (state) {
		case PJSIP_INV_STATE_INCOMING:
			controlMode = MODE_LOCKER;
			inCallButtons.setVisibility(GONE);
			setCallLockerVisibility(VISIBLE);
			inCallButtons.setVisibility(GONE);
			break;
		case PJSIP_INV_STATE_CALLING:
		case PJSIP_INV_STATE_CONNECTING:
			controlMode = MODE_CONTROL;
			setCallLockerVisibility(GONE);
			inCallButtons.setVisibility(VISIBLE);
			clearCallButton.setEnabled(true);
			setEnabledMediaButtons(true);
			break;
		case PJSIP_INV_STATE_CONFIRMED:
			controlMode = MODE_CONTROL;
			setCallLockerVisibility(GONE);
			inCallButtons.setVisibility(VISIBLE);

			clearCallButton.setEnabled(true);
			setEnabledMediaButtons(true);
			break;
		case PJSIP_INV_STATE_NULL:
		case PJSIP_INV_STATE_DISCONNECTED:
			controlMode = MODE_NO_ACTION;
			inCallButtons.setVisibility(GONE);
			setCallLockerVisibility(GONE);
			break;
		case PJSIP_INV_STATE_EARLY:
		default:
			if(callInfo.isIncoming()) {
				controlMode = MODE_LOCKER;
				inCallButtons.setVisibility(GONE);
				setCallLockerVisibility(VISIBLE);
				inCallButtons.setVisibility(GONE);
			}else {
				controlMode = MODE_CONTROL;
				setCallLockerVisibility(GONE);
				inCallButtons.setVisibility(VISIBLE);
				clearCallButton.setEnabled(true);
				setEnabledMediaButtons(true);
			}
			break;
		}
	}
	

	/**
	 * Registers a callback to be invoked when the user triggers an event.
	 * 
	 * @param listener
	 *            the OnTriggerListener to attach to this view
	 */
	public void setOnTriggerListener(OnTriggerListener listener) {
		onTriggerListener = listener;
	}

	private void dispatchTriggerEvent(int whichHandle) {
		if (onTriggerListener != null) {
			onTriggerListener.onTrigger(whichHandle);
		}
	}

	@Override
	public void onTrigger(View v, int whichHandle) {
		Log.d(THIS_FILE, "Call controls receive info from slider " + whichHandle);
		if(controlMode != MODE_LOCKER) {
			//Oups we are not in locker mode and we get a trigger from locker...
			// Should not happen... but... to be sure
			return;
		}
		switch (whichHandle) {
		case LEFT_HANDLE:
			Log.d(THIS_FILE, "We take the call");
			
			dispatchTriggerEvent(OnTriggerListener.TAKE_CALL);
			break;
		case RIGHT_HANDLE:
			Log.d(THIS_FILE, "We clear the call");
			dispatchTriggerEvent(OnTriggerListener.DECLINE_CALL);
		default:
			break;
		}

	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.clearCallButton:
			dispatchTriggerEvent(OnTriggerListener.CLEAR_CALL);
			break;
		case R.id.dialpadButton:
			dispatchTriggerEvent(isDialpadOn?OnTriggerListener.DIALPAD_OFF:OnTriggerListener.DIALPAD_ON);
			isDialpadOn = ! isDialpadOn;
			break;
		case R.id.bluetoothButton:
			if (((ToggleButton) v).isChecked()) {
				dispatchTriggerEvent(OnTriggerListener.BLUETOOTH_ON);
			} else {
				dispatchTriggerEvent(OnTriggerListener.BLUETOOTH_OFF);
			}
			break;
		case R.id.speakerButton:
			if (((ToggleButton) v).isChecked()) {
				dispatchTriggerEvent(OnTriggerListener.SPEAKER_ON);
			} else {
				dispatchTriggerEvent(OnTriggerListener.SPEAKER_OFF);
			}
			break;
		case R.id.muteButton:
			if (((ToggleButton) v).isChecked()) {
				dispatchTriggerEvent(OnTriggerListener.MUTE_ON);
			} else {
				dispatchTriggerEvent(OnTriggerListener.MUTE_OFF);
			}
			break;
		case R.id.takeCallButton:
			dispatchTriggerEvent(OnTriggerListener.TAKE_CALL);
			break;
		case R.id.declineCallButton:
			dispatchTriggerEvent(OnTriggerListener.DECLINE_CALL);
			break;
			
		case R.id.detailsButton:
			dispatchTriggerEvent(OnTriggerListener.DETAILED_DISPLAY);
			break;
		}
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(THIS_FILE, "Hey you hit the key : "+keyCode);
		switch (keyCode) {
		case KeyEvent.KEYCODE_CALL:
			if(controlMode == MODE_LOCKER) {
				dispatchTriggerEvent(OnTriggerListener.TAKE_CALL);
				return true;
			}
			break;
		case KeyEvent.KEYCODE_ENDCALL:
			if(controlMode == MODE_LOCKER) {
				dispatchTriggerEvent(OnTriggerListener.DECLINE_CALL);
				return true;
			}else if (controlMode == MODE_CONTROL) {
				dispatchTriggerEvent(OnTriggerListener.CLEAR_CALL);
				return true;
			}
		default:
			
			break;
		}
		
		return super.onKeyDown(keyCode, event);
	}
}
