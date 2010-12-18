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


import org.pjsip.pjsua.pjsip_inv_state;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.CallInfo;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.Log;


public class InCallInfo extends FrameLayout {
	
	private static final String THIS_FILE = "InCallInfo";
	CallInfo callInfo;
	String remoteUri = "";
	private ImageView photo;
	private TextView remoteName, title;
	private Chronometer elapsedTime;
	private int colorConnected, colorEnd;
	private LinearLayout currentInfo, currentDetailedInfo;
	
	
	private Context context;
	private TextView remotePhoneNumber;
	private DBAdapter db;
	private TextView label;
	private ImageView secure;
	
	public InCallInfo(Context aContext, AttributeSet attrs) {
		super(aContext, attrs);
		context = aContext;
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.in_call_info, this, true);
		db = new DBAdapter(context);
	}
	
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		photo = (ImageView) findViewById(R.id.photo);
		remoteName = (TextView) findViewById(R.id.name);
		title = (TextView) findViewById(R.id.title);
		elapsedTime = (Chronometer) findViewById(R.id.elapsedTime);
		remotePhoneNumber = (TextView) findViewById(R.id.phoneNumber);
		label = (TextView) findViewById(R.id.label);
		secure = (ImageView) findViewById(R.id.secureIndicator);
		
		currentInfo = (LinearLayout) findViewById(R.id.currentCallInfo);
		currentDetailedInfo = (LinearLayout) findViewById(R.id.currentCallDetailedInfo);
		
		
		//Colors
		colorConnected = Color.parseColor("#99CE3F");
		colorEnd = Color.parseColor("#FF6072");
		
		secure.bringToFront();
	}

	public void setCallState(CallInfo aCallInfo) {
		callInfo = aCallInfo;
		//TODO: see if should be threaded now could improve loading speed of this view on old devices
//		Thread t = new Thread() {
//			public void run() {
				updateRemoteName();
				updateTitle();
//			};
//		};
//		t.start();
		updateElapsedTimer();
		
	}
	


	private synchronized void updateRemoteName() {
		if(callInfo == null) {
			return;
		}
		
		final String aRemoteUri = callInfo.getRemoteContact();
		
		//If not already set with the same value, just ignore it
		if(aRemoteUri != null && !aRemoteUri.equalsIgnoreCase(remoteUri)) {
			remoteUri = aRemoteUri;
			ParsedSipContactInfos uriInfos = SipUri.parseSipContact(remoteUri);

			remoteName.setText(SipUri.getDisplayedSimpleContact(aRemoteUri));
			remotePhoneNumber.setText(uriInfos.userName);
			
			SipProfile acc = SipService.getAccountForPjsipId(callInfo.getAccId(), db);
			if(acc != null && acc.display_name != null) {
				label.setText("SIP/"+acc.display_name+" :");
			}
			
			Thread t = new Thread() {
				public void run() {
					//Looks like a phone number so search the contact throw contacts
					CallerInfo callerInfo = CallerInfo.getCallerInfoFromSipUri(context, remoteUri);
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
		secure.setVisibility(callInfo.isSecure()?View.VISIBLE:View.GONE);
		
		pjsip_inv_state state = callInfo.getCallState();
		switch (state) {
		case PJSIP_INV_STATE_INCOMING:
		case PJSIP_INV_STATE_CALLING:
		case PJSIP_INV_STATE_EARLY:
		case PJSIP_INV_STATE_CONNECTING:
			elapsedTime.setVisibility(GONE);
			elapsedTime.start();
			break;
		case PJSIP_INV_STATE_CONFIRMED:
			Log.d(THIS_FILE, "we start the timer now ");
			
			elapsedTime.start();
			elapsedTime.setVisibility(VISIBLE);
			elapsedTime.setTextColor(colorConnected);
			
			break;
		case PJSIP_INV_STATE_NULL:
		case PJSIP_INV_STATE_DISCONNECTED:
			elapsedTime.stop();
			elapsedTime.setVisibility(VISIBLE);
			elapsedTime.setTextColor(colorEnd);
			break;
		}
	}
	
	private void updateTitle() {
		if(callInfo != null) {
			title.setText(callInfo.getStringCallState(context));
		}else {
			title.setText(R.string.call_state_disconnected);
		}
		
	}
	
	
	public void switchDetailedInfo(boolean showDetails) {
		currentInfo.setVisibility(showDetails?GONE:VISIBLE);
		currentDetailedInfo.setVisibility(showDetails?VISIBLE:GONE);
		if(showDetails && callInfo != null) {
			String infos = callInfo.dumpCallInfo();
			TextView detailText = (TextView) findViewById(R.id.detailsText);
			detailText.setText(infos);
		}
	}

	
	final private int LOAD_CALLER_INFO = 0;
	private Handler userHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case LOAD_CALLER_INFO:
				CallerInfo callerInfo = (CallerInfo) msg.obj;
				ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(context, 
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

}
