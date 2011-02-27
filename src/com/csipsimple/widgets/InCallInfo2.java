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
package com.csipsimple.widgets;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.InCallActivity2.OnBadgeTouchListener;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.InCallControls2.OnTriggerListener;

public class InCallInfo2 extends ExtensibleBadge {
	


	private static final String THIS_FILE = "InCallInfo";
	SipCallSession callInfo;
	String cachedRemoteUri = "";
	int cachedInvState = SipCallSession.InvState.INVALID;
	int cachedMediaState = SipCallSession.MediaState.ERROR;
	private ImageView photo;
	private TextView remoteName, status ;//, title;
	private Chronometer elapsedTime;
	private int colorConnected, colorEnd;
//	private LinearLayout currentInfo, currentDetailedInfo;
	
	
//	private TextView remotePhoneNumber;
	private DBAdapter db;
	//private TextView label;
//	private ImageView secure;
	

	public InCallInfo2(Context context, AttributeSet attrs) {
		super(context, attrs);
		db = new DBAdapter(context);
		initControllerView();
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		//remotePhoneNumber = (TextView) findViewById(R.id.card_status);
	}
	
	private void initControllerView() {
		photo = (ImageView) findViewById(R.id.card_img);
		remoteName = (TextView) findViewById(R.id.card_label);
//		title = (TextView) findViewById(R.id.title);
		elapsedTime = (Chronometer) findViewById(R.id.elapsedTime);
		status = (TextView) findViewById(R.id.card_status);
		
		//secure = (ImageView) findViewById(R.id.secureIndicator);
		
//		currentInfo = (LinearLayout) findViewById(R.id.currentCallInfo);
//		currentDetailedInfo = (LinearLayout) findViewById(R.id.currentCallDetailedInfo);
		
		
		//Colors
		colorConnected = Color.parseColor("#99CE3F");
		colorEnd = Color.parseColor("#FF6072");
		
//		secure.bringToFront();
		
	}
	

	public void setCallState(SipCallSession aCallInfo) {
		callInfo = aCallInfo;
		if(callInfo == null) {
			updateElapsedTimer();
			return;
		}
		
		updateRemoteName();
		updateTitle();
		updateQuickActions();
		updateElapsedTimer();
		
		cachedInvState = callInfo.getCallState();
		cachedMediaState = callInfo.getMediaStatus();
		
		dragListener.setCallState(callInfo);
	}
	


	private synchronized void updateQuickActions() {
		//Useless to process that
		if(cachedInvState == callInfo.getCallState() && 
			cachedMediaState == callInfo.getMediaStatus()) {
			return;
		}
		
		removeAllItems();

		
		//Add items if possible
		
		//Take/decline items
		if(callInfo.isBeforeConfirmed()) {
			//Answer
			if(callInfo.isIncoming()) {
				addItem(R.drawable.ic_in_call_touch_answer, getContext().getString(R.string.take_call), new OnClickListener() {
					@Override
					public void onClick(View v) {
						dispatchTriggerEvent(OnTriggerListener.TAKE_CALL);
						collapse();
					}
				});
			}
			//Decline
			addItem(R.drawable.ic_in_call_touch_end, getContext().getString(R.string.decline_call), new OnClickListener() {
				@Override
				public void onClick(View v) {
					dispatchTriggerEvent(OnTriggerListener.DECLINE_CALL);
					collapse();
				}
			});
		}
		
		//In comm items
		if(!callInfo.isAfterEnded() && !callInfo.isBeforeConfirmed()) {
			//End
			addItem(R.drawable.ic_in_call_touch_end, getContext().getString(R.string.clear_call), new OnClickListener() {
				@Override
				public void onClick(View v) {
					dispatchTriggerEvent(OnTriggerListener.CLEAR_CALL);
					collapse();
				}
			});
			
			//Hold
			addItem(callInfo.isLocalHeld()?R.drawable.ic_in_call_touch_unhold : R.drawable.ic_in_call_touch_hold, callInfo.isLocalHeld()?"Unhold" : "Hold", new OnClickListener() {
				@Override
				public void onClick(View v) {
					dispatchTriggerEvent(OnTriggerListener.TOGGLE_HOLD);
					collapse();
				}
			});
		}
		
		//Info item
		if(!callInfo.isAfterEnded()) {
			addItem(R.drawable.ic_in_call_touch_round_details, "Info", new OnClickListener() {
				@Override
				public void onClick(View v) {
					dispatchTriggerEvent(OnTriggerListener.DETAILED_DISPLAY);
					collapse();
				}
			});
		}
		
	}
	

	
	private synchronized void updateTitle() {
	
		/*if(callInfo != null) {
			title.setText(CallsUtils.getStringCallState(callInfo, context));
		}else {
			title.setText(R.string.call_state_disconnected);
		}*/
		
	}
	


	private synchronized void updateRemoteName() {
		
		final String aRemoteUri = callInfo.getRemoteContact();
		
		//If not already set with the same value, just ignore it
		if(aRemoteUri != null && !aRemoteUri.equalsIgnoreCase(cachedRemoteUri)) {
			cachedRemoteUri = aRemoteUri;
			ParsedSipContactInfos uriInfos = SipUri.parseSipContact(cachedRemoteUri);
			String text = SipUri.getDisplayedSimpleContact(aRemoteUri);
			String statusText = "";
			
			
			remoteName.setText( text );
			if(callInfo.getAccId() != SipProfile.INVALID_ID) {
				SipProfile acc = SipService.getAccount(callInfo.getAccId(), db);
				if(acc != null && acc.display_name != null) {
					statusText  += "SIP/"+acc.display_name + " : " ;
				}
			}else {
				statusText  += "SIP : " ;
			}
			
			statusText += uriInfos.userName;
			status.setText(statusText);
			
			Thread t = new Thread() {
				public void run() {
					//Looks like a phone number so search the contact throw contacts
					CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(getContext(), cachedRemoteUri);
					if(callerInfo != null && callerInfo.contactExists) {
						userHandler.sendMessage(userHandler.obtainMessage(LOAD_CALLER_INFO, callerInfo));
					}
				};
			};
			t.start();
			
			
		}
	}
	

	private void updateElapsedTimer() {
		
		
		if(callInfo == null) {
			elapsedTime.stop();
			elapsedTime.setVisibility(VISIBLE);
			elapsedTime.setTextColor(colorEnd);
			return;
		}
		elapsedTime.setBase(callInfo.getConnectStart());
	//	secure.setVisibility(callInfo.isSecure()?View.VISIBLE:View.GONE);
		
		int state = callInfo.getCallState();
		switch (state) {
		case SipCallSession.InvState.INCOMING:
		case SipCallSession.InvState.CALLING:
		case SipCallSession.InvState.EARLY:
		case SipCallSession.InvState.CONNECTING:
			elapsedTime.setVisibility(GONE);
			elapsedTime.start();
			break;
		case SipCallSession.InvState.CONFIRMED:
			Log.d(THIS_FILE, "we start the timer now ");
			
			elapsedTime.start();
			elapsedTime.setVisibility(VISIBLE);
			elapsedTime.setTextColor(colorConnected);
			
			break;
		case SipCallSession.InvState.NULL:
		case SipCallSession.InvState.DISCONNECTED:
			elapsedTime.stop();
			elapsedTime.setVisibility(VISIBLE);
			elapsedTime.setTextColor(colorEnd);
			break;
		}
		
		
	}
	
	public void switchDetailedInfo(boolean showDetails) {
		/*
		currentInfo.setVisibility(showDetails?GONE:VISIBLE);
		currentDetailedInfo.setVisibility(showDetails?VISIBLE:GONE);
		if(showDetails && callInfo != null) {
			String infos = PjSipCalls.dumpCallInfo(callInfo.getCallId());
			TextView detailText = (TextView) findViewById(R.id.detailsText);
			detailText.setText(infos);
		}
		*/
	}

	
	final private int LOAD_CALLER_INFO = 0;
	private Handler userHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case LOAD_CALLER_INFO:
				CallerInfo callerInfo = (CallerInfo) msg.obj;
				ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(getContext(), 
						photo,
						callerInfo,
						R.drawable.picture_unknown);
				remoteName.setText(callerInfo.name);
				break;

			default:
				break;
			}
			
		};
	};
	
	private OnTriggerListener onTriggerListener;
	private OnBadgeTouchListener dragListener;


	/*
	 * Registers a callback to be invoked when the user triggers an event.
	 * 
	 * @param listener
	 *            the OnTriggerListener to attach to this view
	 */
	public void setOnTriggerListener(OnTriggerListener listener) {
		onTriggerListener = listener;
	}

	private void dispatchTriggerEvent(int whichHandle) {
		Log.d(THIS_FILE, "dispatch "+onTriggerListener);
		if (onTriggerListener != null) {
			onTriggerListener.onTrigger(whichHandle, callInfo);
		}
	}

	public Rect setSize(int i, int j) {
		ViewGroup.LayoutParams lp = photo.getLayoutParams();
		//TODO remove bottom line and wrapping height/width
		int cstW = 5;
		int cstH = 10;
		int s = Math.min(i, j);
		lp.width = s - cstW;
		lp.height = s - cstH;
		
		photo.setLayoutParams(lp);
		
		return new Rect(0, 0, s, s);
	}
	
	public SipCallSession getCallInfo() {
		return callInfo;
	}
	
	
	public void setOnTouchListener(OnBadgeTouchListener l) {
		dragListener = l;
		super.setOnTouchListener(l);
	}

}
