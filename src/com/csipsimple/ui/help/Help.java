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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.utils.CollectLogs;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class Help extends Activity implements OnClickListener {
	
	
	private static final String THIS_FILE = "Help";
	private PreferencesWrapper prefsWrapper;

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

	}
	
	private void bindView() {
		LinearLayout line;
		line = (LinearLayout) findViewById(R.id.faq_line);
		line.setOnClickListener(this);
		line = (LinearLayout) findViewById(R.id.record_logs_line);
		line.setOnClickListener(this);
		line = (LinearLayout) findViewById(R.id.issues_line);
		line.setOnClickListener(this);
		
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
			if (!isRecording()) {
				prefsWrapper.setPreferenceStringValue(PreferencesWrapper.LOG_LEVEL, "4");
				Log.setLogLevel(4);
				//TODO : set log level of native stack....
				//TODO : toaster that explain user that he should now try to reproduce the bug...
			} else {
				prefsWrapper.setPreferenceStringValue(PreferencesWrapper.LOG_LEVEL, "1");
				try {
					startActivity(CollectLogs.getLogReportIntent("<<<PLEASE ADD THE BUG DESCRIPTION HERE>>>"));
				}catch(Exception e) {
					Log.e(THIS_FILE, "Impossible to send logs...", e);
				}
			}
			finish();
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
	
}
