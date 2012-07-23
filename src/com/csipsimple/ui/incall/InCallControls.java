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
import android.view.View;
import android.widget.FrameLayout;

import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuBuilder.Callback;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.view.menu.MenuView;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Log;

/**
 * Manages in call controls not relative to a particular call such as media route
 */
public class InCallControls extends FrameLayout implements Callback {

	private static final String THIS_FILE = "InCallControls";
	IOnCallActionTrigger onTriggerListener;
	
	private MediaState lastMediaState;
	private SipCallSession currentCall;
    private MenuBuilder btnMenuBuilder;
	private boolean supportMultipleCalls = false;


	public InCallControls(Context context) {
        this(context, null, 0);
    }
	
	public InCallControls(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
    public InCallControls(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        
        if(!isInEditMode()) {
            supportMultipleCalls = SipConfigManager.getPreferenceBooleanValue(getContext(), SipConfigManager.SUPPORT_MULTIPLE_CALLS, false);
        }
        
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        ActionMenuPresenter mActionMenuPresenter = new ActionMenuPresenter(getContext()) {
            public void bindItemView(MenuItemImpl item, MenuView.ItemView itemView) {
                super.bindItemView(item, itemView);
                View actionItemView = (View) itemView;
                actionItemView.setBackgroundResource(R.drawable.btn_compound_background);
            }
        };
        mActionMenuPresenter.setReserveOverflow(true);
        // Full width
        mActionMenuPresenter.setWidthLimit(
                getContext().getResources().getDisplayMetrics().widthPixels, true);
        btnMenuBuilder = new MenuBuilder(getContext());
        btnMenuBuilder.setCallback(this);
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.inflate(R.menu.in_call_controls_menu, btnMenuBuilder);
        btnMenuBuilder.addMenuPresenter(mActionMenuPresenter);
        ActionMenuView menuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
        menuView.setBackgroundResource(R.drawable.abs__ab_bottom_transparent_dark_holo);
        
        this.addView(menuView, layoutParams);
    }
    
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// Finalize object style
		setEnabledMediaButtons(false);
	}

	
	
	private boolean callOngoing = false;
	public void setEnabledMediaButtons(boolean isInCall) {
        callOngoing = isInCall;
        setMediaState(lastMediaState);
	}
	
	public void setCallState(SipCallSession callInfo) {
		currentCall = callInfo;
		
		if(currentCall == null) {
			setVisibility(GONE);
			return;
		}
		
		int state = currentCall.getCallState();
		Log.d(THIS_FILE, "Mode is : "+state);
		switch (state) {
		case SipCallSession.InvState.INCOMING:
		    setVisibility(GONE);
			break;
		case SipCallSession.InvState.CALLING:
		case SipCallSession.InvState.CONNECTING:
		    setVisibility(VISIBLE);
			setEnabledMediaButtons(true);
			break;
		case SipCallSession.InvState.CONFIRMED:
		    setVisibility(VISIBLE);
			setEnabledMediaButtons(true);
			break;
		case SipCallSession.InvState.NULL:
		case SipCallSession.InvState.DISCONNECTED:
		    setVisibility(GONE);
			break;
		case SipCallSession.InvState.EARLY:
		default:
			if (currentCall.isIncoming()) {
			    setVisibility(GONE);
			} else {
			    setVisibility(VISIBLE);
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
	public void setOnTriggerListener(IOnCallActionTrigger listener) {
		onTriggerListener = listener;
	}

	private void dispatchTriggerEvent(int whichHandle) {
		if (onTriggerListener != null) {
			onTriggerListener.onTrigger(whichHandle, currentCall);
		}
	}
	
	public void setMediaState(MediaState mediaState) {
		lastMediaState = mediaState;

        // Update menu
		// BT
		boolean enabled, checked;
		if(lastMediaState == null) {
		    enabled = callOngoing;
		    checked = false;
		}else {
    		enabled = callOngoing && lastMediaState.canBluetoothSco;
    		checked = lastMediaState.isBluetoothScoOn;
		}
        btnMenuBuilder.findItem(R.id.bluetoothButton).setVisible(enabled).setChecked(checked);
        
        // Mic
        if(lastMediaState == null) {
            enabled = callOngoing;
            checked = false;
        }else {
            enabled = callOngoing && lastMediaState.canMicrophoneMute;
            checked = lastMediaState.isMicrophoneMute;
        }
        btnMenuBuilder.findItem(R.id.muteButton).setVisible(enabled).setChecked(checked);
        

        // Speaker
        Log.d(THIS_FILE, ">> Speaker " + lastMediaState);
        if(lastMediaState == null) {
            enabled = callOngoing;
            checked = false;
        }else {
            Log.d(THIS_FILE, ">> Speaker " + lastMediaState.isSpeakerphoneOn);
            enabled = callOngoing && lastMediaState.canSpeakerphoneOn;
            checked = lastMediaState.isSpeakerphoneOn;
        }
        btnMenuBuilder.findItem(R.id.speakerButton).setVisible(enabled).setChecked(checked);
        
        // Add call
        btnMenuBuilder.findItem(R.id.addCallButton).setVisible(supportMultipleCalls && callOngoing);
	}

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        int id = item.getItemId();
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }
        if (id == R.id.bluetoothButton) {
            if (item.isChecked()) {
                dispatchTriggerEvent(IOnCallActionTrigger.BLUETOOTH_ON);
            } else {
                dispatchTriggerEvent(IOnCallActionTrigger.BLUETOOTH_OFF);
            }
            return true;
        } else if (id == R.id.speakerButton) {
            if (item.isChecked()) {
                dispatchTriggerEvent(IOnCallActionTrigger.SPEAKER_ON);
            } else {
                dispatchTriggerEvent(IOnCallActionTrigger.SPEAKER_OFF);
            }
            return true;
        } else if (id == R.id.muteButton) {
            if (item.isChecked()) {
                dispatchTriggerEvent(IOnCallActionTrigger.MUTE_ON);
            } else {
                dispatchTriggerEvent(IOnCallActionTrigger.MUTE_OFF);
            }
            return true;
        } else if (id == R.id.addCallButton) {
            dispatchTriggerEvent(IOnCallActionTrigger.ADD_CALL);
            return true;
        } else if (id == R.id.mediaSettingsButton) {
            dispatchTriggerEvent(IOnCallActionTrigger.MEDIA_SETTINGS);
            return true;
        }
        return false;
    }

    @Override
    public void onMenuModeChange(MenuBuilder menu) {
        // Nothing to do.
    }

}
