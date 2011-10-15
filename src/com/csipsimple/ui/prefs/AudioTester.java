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
package com.csipsimple.ui.prefs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipManager;
import com.csipsimple.service.SipService;

public class AudioTester extends Activity {

	private final static String THIS_FILE = "AudioTester";
	
	int currentStatus = R.string.test_audio_prepare;
	private TextView statusTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		statusTextView = new TextView(this);
		statusTextView.setPadding(20, 20, 20, 20);
		statusTextView.setTextSize(16);
		
		addContentView(statusTextView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			if(service != null) {
				try {
					int res = service.startLoopbackTest();
					if(res == SipManager.SUCCESS) {
						currentStatus = R.string.test_audio_ongoing;
					}else {
						currentStatus = R.string.test_audio_network_failure;
					}
					updateStatusDisplay();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Error in test", e);
				}
			}
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			if(service != null) {
				try {
					service.stopLoopbackTest();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Error in test", e);
				}
			}
		}
    };
    
	
	@Override
	protected void onResume() {
		super.onResume();
		currentStatus = R.string.test_audio_prepare;
		updateStatusDisplay();
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(service != null) {
			try {
				service.stopLoopbackTest();
			} catch (RemoteException e) {
				Log.e(THIS_FILE, "Error in test", e);
			}
		}
		unbindService(connection);
	}
	
	private void updateStatusDisplay() {
		if(statusTextView != null) {
			statusTextView.setText(currentStatus);
		}
	}
}
