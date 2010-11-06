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

import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

public class PickupSipUri extends Activity implements OnClickListener, TextWatcher {

	private static final String THIS_FILE = "PickupUri";
	private EditText sipUri;
	private Button okBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pickup_uri);
		
		sipUri = (EditText) findViewById(R.id.sip_uri);
		sipUri.addTextChangedListener(this);
		
		okBtn = (Button) findViewById(R.id.ok);
		okBtn.setOnClickListener(this);
		Button btn = (Button) findViewById(R.id.cancel);
		btn.setOnClickListener(this);
		
		updateValidation();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.ok:
			 Intent resultValue = new Intent();
	         resultValue.putExtra(Intent.EXTRA_PHONE_NUMBER,
	                            sipUri.getText().toString());
			setResult(RESULT_OK, resultValue);
			finish();
			break;
		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		}
	}
	
	void updateValidation(){
		boolean valid = false;
		String callee = sipUri.getText().toString();
		//Log.d(THIS_FILE, "Update validation with "+callee);
		try {
		//	Log.d(THIS_FILE, "Is valid ? : "+pjsua.verify_sip_url(callee));
			valid = (pjsua.verify_sip_url(callee) == pjsuaConstants.PJ_SUCCESS);
		}catch (Exception e) {
			Log.w(THIS_FILE, "Can't check validity");
		}
		
		okBtn.setEnabled(valid);
	}
	
	
	

	@Override
	public void afterTextChanged(Editable s) {
		updateValidation();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Nothing to do;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		updateValidation();
	}
}
