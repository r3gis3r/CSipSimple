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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.widgets.EditSipUri;
import com.csipsimple.widgets.EditSipUri.ToCall;

public class PickupSipUri extends Activity implements OnClickListener {

	private static final String THIS_FILE = "PickupUri";
	private EditSipUri sipUri;
	private Button okBtn;
	private BroadcastReceiver registrationReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pickup_uri);
		
		sipUri = (EditSipUri) findViewById(R.id.sip_uri);
		
		okBtn = (Button) findViewById(R.id.ok);
		okBtn.setOnClickListener(this);
		Button btn = (Button) findViewById(R.id.cancel);
		btn.setOnClickListener(this);
		
		
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
		// Bind service
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		registerReceiver(registrationReceiver, new IntentFilter(SipService.ACTION_SIP_REGISTRATION_CHANGED));
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
				 resultValue.putExtra(Account.FIELD_ACC_ID,
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
		}
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
