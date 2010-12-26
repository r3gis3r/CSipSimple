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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.utils.contacts.ContactsWrapper.OnPhoneNumberSelected;
import com.csipsimple.widgets.EditSipUri;
import com.csipsimple.widgets.EditSipUri.ToCall;

public class PickupSipUri extends Activity implements OnClickListener {

	private static final String THIS_FILE = "PickupUri";
	private EditSipUri sipUri;
	private Button okBtn;
	private BroadcastReceiver registrationReceiver;
	private LinearLayout searchInContactRow;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.pickup_uri);
		
		
		//Set window size
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		
		//Set title
		((TextView) findViewById(R.id.my_title)).setText(R.string.pickup_sip_uri);
		((ImageView) findViewById(R.id.my_icon)).setImageResource(android.R.drawable.ic_menu_call);
		
		
		sipUri = (EditSipUri) findViewById(R.id.sip_uri);
		
		okBtn = (Button) findViewById(R.id.ok);
		okBtn.setOnClickListener(this);
		Button btn = (Button) findViewById(R.id.cancel);
		btn.setOnClickListener(this);
		
		searchInContactRow = (LinearLayout) findViewById(R.id.search_contacts);
		searchInContactRow.setOnClickListener(this);
		
		registrationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateRegistrations();
			}
		};
		
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(THIS_FILE, "Resume pickup URI");
		// Bind service
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		registerReceiver(registrationReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unbind service
		try {
			unbindService(connection);
		}catch (Exception e) {
			//Just ignore that -- TODO : should be more clean
		}

		try {
			unregisterReceiver(registrationReceiver);
		} catch (Exception e) {
			// Nothing to do here -- TODO : should be more clean
		}
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.ok:
			 Intent resultValue = new Intent();
			 ToCall result = sipUri.getValue();
			 if(result != null) {
				 resultValue.putExtra(Intent.EXTRA_PHONE_NUMBER,
							result.getCallee());
				 resultValue.putExtra(SipProfile.FIELD_ACC_ID,
							result.getAccountId());
				 setResult(RESULT_OK, resultValue);
			 }else {
				setResult(RESULT_CANCELED);
			 }
			finish();
			break;
		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.search_contacts:
			startActivityForResult(Compatibility.getContactPhoneIntent(), PICKUP_PHONE);
			break;
		}
	}

	protected static final int PICKUP_PHONE = 0;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(THIS_FILE, "On activity result");
		switch (requestCode) {
		case PICKUP_PHONE:
			if(resultCode == RESULT_OK) {
				ContactsWrapper.getInstance().treatContactPickerPositiveResult(this, data, new OnPhoneNumberSelected() {
					@Override
					public void onTrigger(String number) {
						// TODO : filters... how to find a fancy way to integrate it back here 
						// * auto once selected according to currently selected account?
						// * keep in mind initial call number and rewrite number each time account is changed in selection (maybe the best way but must be handled properly)
                        // TODO : Code similar to that in SipHome.onActivityResult() - Refactor
					    if (number.startsWith("sip:")) {
					        sipUri.setTextValue(number);
					    } else {
	                        //Code from android source : com/android/phone/OutgoingCallBroadcaster.java 
	                        // so that we match exactly the same case that an outgoing call from android
    						number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
    			            number = PhoneNumberUtils.stripSeparators(number);
    			        	sipUri.setTextValue(number);
					    }
					}
				});
				return;
			}
			break;

		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			sipUri.updateService(service);
			updateRegistrations();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}

	};
	
	private void updateRegistrations(){
		sipUri.updateRegistration();
	}
	
}
