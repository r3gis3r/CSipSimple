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
package com.csipsimple.ui.help;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.CollectLogs;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class Help extends Activity implements OnClickListener {
	
	
	private static final String THIS_FILE = "Help";
	private PreferencesWrapper prefsWrapper;
	private static final int REQUEST_SEND_LOGS = 0;
	
	private ISipService sipService = null;
	
	private ServiceConnection restartServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder aService) {
			sipService = ISipService.Stub.asInterface(aService);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.help);
	//	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.white_title);
		
		prefsWrapper = new PreferencesWrapper(this);
		
		//Set window size
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		
		//Set title
		((TextView) findViewById(R.id.my_title)).setText(R.string.help);
		((ImageView) findViewById(R.id.my_icon)).setImageResource(android.R.drawable.ic_menu_help);
		
		bindView();
		
		//Attach to the service
		Intent serviceIntent =  new Intent(this, SipService.class);
		try {
			bindService(serviceIntent, restartServiceConnection, 0);
		}catch(Exception e) {
			
		}
	}
	
	private void bindView() {
		LinearLayout line;
		line = (LinearLayout) findViewById(R.id.faq_line);
		line.setOnClickListener(this);
		line = (LinearLayout) findViewById(R.id.record_logs_line);
		line.setOnClickListener(this);
		if(CustomDistribution.getSupportEmail() == null) {
			line.setVisibility(View.GONE);
		}
		line = (LinearLayout) findViewById(R.id.issues_line);
		line.setOnClickListener(this);
		if(!CustomDistribution.showIssueList()) {
			line.setVisibility(View.GONE);
		}
		
		//Recording logs
		ImageView recordImage = (ImageView) findViewById(R.id.record_logs_image);
		TextView recordText = (TextView) findViewById(R.id.record_logs_text);
		if(isRecording()) {
			recordImage.setImageResource(android.R.drawable.ic_menu_send);
			recordText.setText(R.string.send_logs);
		}else {
			recordImage.setImageResource(android.R.drawable.ic_menu_save);
			recordText.setText(R.string.record_logs);
		}
		
		// Revision
		TextView rev = (TextView) findViewById(R.id.revision);
		rev.setText(CollectLogs.getApplicationInfo(this));
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		sipService = null;
		if(restartServiceConnection != null) {
			try {
				unbindService(restartServiceConnection);
			}catch(Exception e) {
				//Nothing to do service was just not binded
			}
		}
	}
	
	private boolean isRecording() {
		return (prefsWrapper.getLogLevel() >= 3);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.faq_line:
			startActivity(new Intent(this, Faq.class));
			break;
		case R.id.record_logs_line:
			Log.e(THIS_FILE, "Clicked on record logs line while isRecording is : " + isRecording());
			if (!isRecording()) {
				prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "4");
				Log.setLogLevel(4);
				if(sipService !=null ) {
					try {
						sipService.askThreadedRestart();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to restart sip", e);
					}
				}

				finish();
			} else {
				prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "1");
				try {
					startActivityForResult(CollectLogs.getLogReportIntent("<<<PLEASE ADD THE BUG DESCRIPTION HERE>>>", this), REQUEST_SEND_LOGS);
				}catch(Exception e) {
					Log.e(THIS_FILE, "Impossible to send logs...", e);
				}
				Log.setLogLevel(1);
			}
			break;
		case R.id.issues_line:
			Intent it = new Intent(Intent.ACTION_VIEW);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			it.setData(Uri.parse("http://code.google.com/p/csipsimple/issues"));
			startActivity(it);
			break;
		default:
			break;
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_SEND_LOGS) {
			//Do not that here !!! if so mailer will be lost..
			//PreferencesWrapper.cleanLogsFiles();
			finish();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
