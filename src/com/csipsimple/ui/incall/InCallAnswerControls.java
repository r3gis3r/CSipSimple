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
/**
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2008 The Android Open Source Project
 */

package com.csipsimple.ui.incall;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.accessibility.AccessibilityWrapper;
import com.csipsimple.widgets.AlternateUnlocker;
import com.csipsimple.widgets.IOnLeftRightChoice;
import com.csipsimple.widgets.SlidingTab;

public class InCallAnswerControls extends RelativeLayout implements IOnLeftRightChoice {

	private static final String THIS_FILE = "InCallAnswerControls";
	private SlidingTab slidingTabWidget;
    private AlternateUnlocker alternateLockerWidget;
	
	
	private static final int MODE_LOCKER = 0;
	private static final int MODE_NO_ACTION = 1;
	private int controlMode;
	private SipCallSession currentCall;
    private boolean useSlider;
    private IOnCallActionTrigger onTriggerListener;


	public InCallAnswerControls(Context context) {
        this(context, null, 0);
    }
	
	public InCallAnswerControls(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
    public InCallAnswerControls(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        
        useSlider = false;
        setGravity(Gravity.CENTER_VERTICAL);
        if(!isInEditMode()) {
            AccessibilityWrapper accessibilityManager = AccessibilityWrapper.getInstance();
            accessibilityManager.init(getContext());
            if(!accessibilityManager.isEnabled()) {
                useSlider = !SipConfigManager.getPreferenceBooleanValue(context, SipConfigManager.USE_ALTERNATE_UNLOCKER, useSlider);
            }
        }
        
    }
    
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// Finalize object style
		controlMode = MODE_NO_ACTION;
	}

	
	private void setCallLockerVisibility(int visibility) {
	    controlMode = visibility == View.VISIBLE ? MODE_LOCKER : MODE_NO_ACTION;
	    setVisibility(visibility);
        if(visibility == View.VISIBLE) {
            // Inflate sub views only if display is requested
            if(useSlider) {
                if(slidingTabWidget == null) {
                    slidingTabWidget = new SlidingTab(getContext());
                    slidingTabWidget.setOnLeftRightListener(this);
                    this.addView(slidingTabWidget, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                    slidingTabWidget.setLeftHintText(R.string.take_call);
                    slidingTabWidget.setRightHintText(R.string.decline_call);
                    LayoutParams lp = (LayoutParams) slidingTabWidget.getLayoutParams();
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    slidingTabWidget.setLayoutParams(lp);
                }
            }else {
                if(alternateLockerWidget == null) {
                    alternateLockerWidget = new AlternateUnlocker(getContext());
                    alternateLockerWidget.setOnLeftRightListener(this);
                    this.addView(alternateLockerWidget, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                }
            }
        }
        
        if(slidingTabWidget != null) {
            slidingTabWidget.setVisibility(visibility);
        }else if(alternateLockerWidget != null) {
            alternateLockerWidget.setVisibility(visibility);
        }
	}
	

	public void setCallState(SipCallSession callInfo) {
		currentCall = callInfo;
		
		if(currentCall == null) {
			setCallLockerVisibility(GONE);
			return;
		}
		
		int state = currentCall.getCallState();
		switch (state) {
		case SipCallSession.InvState.INCOMING:
			setCallLockerVisibility(VISIBLE);
			break;
		case SipCallSession.InvState.CALLING:
		case SipCallSession.InvState.CONNECTING:
		case SipCallSession.InvState.CONFIRMED:
		case SipCallSession.InvState.NULL:
		case SipCallSession.InvState.DISCONNECTED:
			setCallLockerVisibility(GONE);
			break;
		case SipCallSession.InvState.EARLY:
		default:
			if (currentCall.isIncoming()) {
				setCallLockerVisibility(VISIBLE);
			} else {
				setCallLockerVisibility(GONE);
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
	public void setOnTriggerListener(IOnCallActionTrigger listener) {
		onTriggerListener = listener;
	}

	private void dispatchTriggerEvent(int whichHandle) {
		if (onTriggerListener != null) {
			onTriggerListener.onTrigger(whichHandle, currentCall);
		}
	}
	

	@Override
	public void onLeftRightChoice(int whichHandle) {
		Log.d(THIS_FILE, "Call controls receive info from slider " + whichHandle);
		if (controlMode != MODE_LOCKER) {
			// Oups we are not in locker mode and we get a trigger from
			// locker...
			// Should not happen... but... to be sure
			return;
		}
		switch (whichHandle) {
		case LEFT_HANDLE:
			Log.d(THIS_FILE, "We take the call");
			
			dispatchTriggerEvent(IOnCallActionTrigger.TAKE_CALL);
			break;
		case RIGHT_HANDLE:
			Log.d(THIS_FILE, "We clear the call");
			dispatchTriggerEvent(IOnCallActionTrigger.DECLINE_CALL);
		default:
			break;
		}
		if(slidingTabWidget != null) {
		    slidingTabWidget.resetView();
		}
	}

	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(THIS_FILE, "Hey you hit the key : " + keyCode);
        if (controlMode == MODE_LOCKER) {
    		switch (keyCode) {
    		case KeyEvent.KEYCODE_CALL:
    			dispatchTriggerEvent(IOnCallActionTrigger.TAKE_CALL);
    			return true;
    		case KeyEvent.KEYCODE_ENDCALL:
    		//case KeyEvent.KEYCODE_POWER:
    			dispatchTriggerEvent(IOnCallActionTrigger.DECLINE_CALL);
    			return true;
    		default:
    			break;
    		}
        }
		
		return super.onKeyDown(keyCode, event);
	}


	public void applyTheme(Theme t) {
		//Apply backgrounds
        
        // TODO : re-enable
        /*
		// To toggle buttons
		StateListDrawable tStd = getToggleButtonDrawable(t);
		if(tStd != null) {
			speakerButton.setBackgroundDrawable(tStd);
			// yeah we can't recycle the std drawable
			muteButton.setBackgroundDrawable(getToggleButtonDrawable(t));
			bluetoothButton.setBackgroundDrawable(getToggleButtonDrawable(t));
		}
		// To buttons
		StateListDrawable bStd = getButtonDrawable(t);
		if(bStd != null) {
			addCallButton.setBackgroundDrawable(bStd);
			clearCallButton.setBackgroundDrawable(getButtonDrawable(t));
			dialButton.setBackgroundDrawable(getButtonDrawable(t));
		}
		
		// To buttons icons
		Drawable addCallDrawable = t.getDrawableResource("ic_in_call_touch_add_call");
		if(addCallDrawable != null) {
			addCallButton.setCompoundDrawablesWithIntrinsicBounds(null, addCallDrawable, null, null);
		}
		Drawable clearCallDrawable = t.getDrawableResource("ic_in_call_touch_end");
		if(clearCallDrawable != null) {
			clearCallButton.setCompoundDrawablesWithIntrinsicBounds(null, clearCallDrawable, null, null);
		}
		Drawable dialDrawable = t.getDrawableResource("ic_in_call_touch_dialpad");
		if(dialDrawable != null) {
			dialButton.setCompoundDrawablesWithIntrinsicBounds(null, dialDrawable, null, null);
		}
		*/
		
		if(slidingTabWidget != null) {
    		// To sliding tab
    		slidingTabWidget.setLeftTabDrawables(t.getDrawableResource("ic_jog_dial_answer"), 
    				t.getDrawableResource("jog_tab_target_green"), 
    				t.getDrawableResource("jog_tab_bar_left_answer"), 
    				t.getDrawableResource("jog_tab_left_answer"));
    		
    		slidingTabWidget.setRightTabDrawables(t.getDrawableResource("ic_jog_dial_decline"), 
    				t.getDrawableResource("jog_tab_target_red"), 
    				t.getDrawableResource("jog_tab_bar_right_decline"), 
    				t.getDrawableResource("jog_tab_right_decline"));
    		
		}
	}
	/*
	private StateListDrawable getToggleButtonDrawable(Theme t) {

		Drawable toggleOnNormal = t.getDrawableResource("btn_in_call_switch_on_normal");
		Drawable toggleOnDisabled = t.getDrawableResource("btn_in_call_switch_on_disable");
		Drawable toggleOnPressed = t.getDrawableResource("btn_in_call_switch_on_pressed");
		Drawable toggleOnSelected = t.getDrawableResource("btn_in_call_switch_on_selected");
		Drawable toggleOnDisabledFocus = t.getDrawableResource("btn_in_call_switch_on_disable_focused");
		Drawable toggleOffNormal = t.getDrawableResource("btn_in_call_switch_off_normal");
		Drawable toggleOffDisabled = t.getDrawableResource("btn_in_call_switch_off_disable");
		Drawable toggleOffPressed = t.getDrawableResource("btn_in_call_switch_off_pressed");
		Drawable toggleOffSelected = t.getDrawableResource("btn_in_call_switch_off_selected");
		Drawable toggleOffDisabledFocus = t.getDrawableResource("btn_in_call_switch_off_disable_focused");
		
		if(toggleOnSelected == null) {
			toggleOnSelected = toggleOnPressed;
		}
		if(toggleOffSelected == null) {
			toggleOffSelected = toggleOffPressed;
		}
		
		if(toggleOnNormal != null && toggleOnDisabled != null && 
				toggleOnPressed != null && toggleOnSelected != null &&
				toggleOnDisabledFocus != null && toggleOffNormal != null &&
				toggleOffDisabled != null && toggleOffPressed != null &&
				toggleOffSelected != null && toggleOffDisabledFocus != null ){
			
			StateListDrawable toggleStd = new StateListDrawable();
		//	toggleStd.addState(new int[] { - android.R.attr.state_focused, android.R.attr.state_enabled, android.R.attr.state_checked}, toggleOnNormal);
			toggleStd.addState(new int[] { - android.R.attr.state_focused, - android.R.attr.state_enabled, android.R.attr.state_checked}, toggleOnDisabled);
			toggleStd.addState(new int[] { android.R.attr.state_pressed, android.R.attr.state_checked}, toggleOnPressed);
			toggleStd.addState(new int[] { android.R.attr.state_focused, android.R.attr.state_enabled, android.R.attr.state_checked}, toggleOnSelected);
			toggleStd.addState(new int[] { android.R.attr.state_enabled, android.R.attr.state_checked}, toggleOnNormal);
			toggleStd.addState(new int[] { android.R.attr.state_focused, android.R.attr.state_checked}, toggleOnDisabledFocus);
			toggleStd.addState(new int[] { android.R.attr.state_checked}, toggleOnDisabled);
			
			// UnChecked
		//	toggleStd.addState(new int[] { - android.R.attr.state_focused, android.R.attr.state_enabled, -android.R.attr.state_checked}, toggleOffNormal);
			toggleStd.addState(new int[] { - android.R.attr.state_focused, - android.R.attr.state_enabled, -android.R.attr.state_checked}, toggleOffDisabled);
			toggleStd.addState(new int[] { android.R.attr.state_pressed, -android.R.attr.state_checked}, toggleOffPressed);
			toggleStd.addState(new int[] { android.R.attr.state_focused, android.R.attr.state_enabled, -android.R.attr.state_checked}, toggleOffSelected);
			toggleStd.addState(new int[] { android.R.attr.state_enabled, -android.R.attr.state_checked}, toggleOffNormal);
			toggleStd.addState(new int[] { android.R.attr.state_focused, -android.R.attr.state_checked}, toggleOffDisabledFocus);
			toggleStd.addState(new int[] { -android.R.attr.state_checked}, toggleOffDisabled);
			return toggleStd;
		}
		return null;
	}
	
	
	private StateListDrawable getButtonDrawable(Theme t) {

		Drawable btNormal = t.getDrawableResource("btn_in_call_main_normal");
		Drawable btDisabled = t.getDrawableResource("btn_in_call_main_disable");
		Drawable btPressed = t.getDrawableResource("btn_in_call_main_pressed");
		Drawable btSelected = t.getDrawableResource("btn_in_call_main_selected");
		Drawable btDisabledFocus = t.getDrawableResource("btn_in_call_main_disable_focused");
		
		if(btSelected == null) {
			btSelected = btPressed;
		}
		
		if(btNormal != null && btDisabled != null && 
				btPressed != null && btSelected != null &&
				btDisabledFocus != null  ){
			
			StateListDrawable btStd = new StateListDrawable();
	//		btStd.addState(new int[] { -android.R.attr.state_focused, android.R.attr.state_enabled }, btNormal);
			btStd.addState(new int[] { -android.R.attr.state_focused, -android.R.attr.state_enabled }, btDisabled);
			btStd.addState(new int[] { android.R.attr.state_pressed }, btPressed);
			btStd.addState(new int[] { android.R.attr.state_focused, android.R.attr.state_enabled }, btSelected);
			btStd.addState(new int[] { android.R.attr.state_enabled }, btNormal);
			btStd.addState(new int[] { android.R.attr.state_focused }, btDisabledFocus);
			btStd.addState(new int[] {}, btDisabled);
			
			return btStd;
		}
		return null;
	}
	*/

}
