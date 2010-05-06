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

import com.csipsimple.R;
import com.csipsimple.models.CallInfo;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.Log;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

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
	private Object photoTracker;
	
	public InCallInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.in_call_info, this, true);
	//	photoTracker = new ContactsAsyncHelper.ImageTracker();
		
	}
	
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		photo = (ImageView) findViewById(R.id.photo);
		remoteName = (TextView) findViewById(R.id.name);
		title = (TextView) findViewById(R.id.title);
		elapsedTime = (Chronometer) findViewById(R.id.elapsedTime);
		
		
		//Colors
		colorConnected = Color.parseColor("#99CE3F");
		colorEnd = Color.parseColor("#FF6072");
		
	}

	public void setCallState(CallInfo aCallInfo) {
		callInfo = aCallInfo;
		updateRemoteName();
		updateElapsedTimer();
		updateTitle();
		
	}
	


	private void updateRemoteName() {
		String aRemoteUri = callInfo.getRemoteContact();
		if(aRemoteUri.equalsIgnoreCase(remoteUri)) {
			String remoteContact = aRemoteUri;
			Pattern p = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*<sip(?:s)?:([^@]*@[^>]*)>");
			Matcher m = p.matcher(remoteContact);
			if (m.matches()) {
				remoteContact = m.group(1);
				if(remoteContact == null || remoteContact.equalsIgnoreCase("")) {
					remoteContact = m.group(2);
				}
			}
			remoteName.setText(remoteContact);
			
			if(Pattern.matches("^[0-9\\-#]*$", remoteContact)) {
				//Looks like a phone number so search the contact throw contacts
				
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


}
