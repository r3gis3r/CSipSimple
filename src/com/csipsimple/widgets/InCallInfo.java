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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pjsip_inv_state;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.Log;

@SuppressWarnings("deprecation")
public class InCallInfo extends FrameLayout {
	
	private static final String THIS_FILE = "InCallInfo";
	CallInfo callInfo;
	String remoteUri = "";
	private ImageView photo;
	private TextView remoteName;
	private TextView title;
	private Chronometer elapsedTime;
	private int colorConnected;
	private int colorEnd;
	
	private Context context;
	private TextView remotePhoneNumber;
	
	public InCallInfo(Context aContext, AttributeSet attrs) {
		super(aContext, attrs);
		context = aContext;
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.in_call_info, this, true);
		
	}
	
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		photo = (ImageView) findViewById(R.id.photo);
		remoteName = (TextView) findViewById(R.id.name);
		title = (TextView) findViewById(R.id.title);
		elapsedTime = (Chronometer) findViewById(R.id.elapsedTime);
		remotePhoneNumber = (TextView) findViewById(R.id.phoneNumber);
		
		//Colors
		colorConnected = Color.parseColor("#99CE3F");
		colorEnd = Color.parseColor("#FF6072");
		
	}

	public void setCallState(CallInfo aCallInfo) {
		callInfo = aCallInfo;
//		Thread t = new Thread() {
//			public void run() {
				updateRemoteName();
				updateTitle();
//			};
//		};
//		t.start();
		updateElapsedTimer();
		
	}
	


	private void updateRemoteName() {
		String aRemoteUri = callInfo.getRemoteContact();
		//If not already set with the same value, just ignore it
		if(aRemoteUri != null && !aRemoteUri.equalsIgnoreCase(remoteUri)) {
			remoteUri = aRemoteUri;
			String remoteContact = aRemoteUri;
			String phoneNumber = null;
			Log.d(THIS_FILE, "Parsing ...."+remoteContact);
			Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*)@[^>]*(?:>)?");
			Matcher m = p.matcher(remoteContact);
			if (m.matches()) {
				
				remoteContact = m.group(1);
				phoneNumber =  m.group(2);
				Log.d(THIS_FILE, "We found .... "+remoteContact+" et "+phoneNumber);
				if(!TextUtils.isEmpty(phoneNumber) && TextUtils.isEmpty(remoteContact)) {
					remoteContact = phoneNumber;
				}
				
				
			}
			remoteName.setText(remoteContact);
			remotePhoneNumber.setText(phoneNumber);
			if(!TextUtils.isEmpty(phoneNumber)) {
				if(Pattern.matches("^[0-9\\-#]*$", phoneNumber)) {
					final String launchPhoneNumber = phoneNumber;
					Thread t = new Thread() {
						public void run() {
							//Looks like a phone number so search the contact throw contacts
							CallerInfo callerInfo = CallerInfo.getCallerInfo(context, launchPhoneNumber);
							if(callerInfo != null && callerInfo.contactExists) {
								userHandler.sendMessage(userHandler.obtainMessage(LOAD_CALLER_INFO, callerInfo));
							}
						};
					};
					t.start();
					
				}
			}
			
		}
	}
	

	private void updateElapsedTimer() {
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
		title.setText(callInfo.getStringCallState());
		
	}

	
	final private int LOAD_CALLER_INFO = 0;
	private Handler userHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case LOAD_CALLER_INFO:
				CallerInfo callerInfo = (CallerInfo) msg.obj;
				ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(context, 
						photo, 
						ContentUris.withAppendedId(Contacts.People.CONTENT_URI, callerInfo.personId), 
						R.drawable.picture_unknown);
				remoteName.setText(callerInfo.name);
				break;

			default:
				break;
			}
			
		};
	};

}
