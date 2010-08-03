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
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;

public class EditFilter extends Activity implements OnItemSelectedListener, TextWatcher {

	private static final String THIS_FILE = "EditFilter";
	private int filterId;
	private DBAdapter database;
	private Filter filter;
	private Button saveButton;
	private int accountId;
	private EditText replaceView;
	private Spinner actionView;
	private EditText matchesView;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		//Get back the concerned account and if any set the current (if not a new account is created)
		Intent intent = getIntent();
        filterId = intent.getIntExtra(Intent.EXTRA_UID, -1);
        accountId = intent.getIntExtra(Filter.FIELD_ACCOUNT, -1);
        
        if(accountId < 0) {
        	Log.e(THIS_FILE, "Invalid account");
        	finish();
        }
        
        database = new DBAdapter(this);
		database.open();
		filter = database.getFilter(filterId);
		database.close();
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.edit_filter);
		
		
		// Bind view objects
		actionView = (Spinner) findViewById(R.id.filter_action);
		replaceView = (EditText) findViewById(R.id.filter_replace);
		matchesView = (EditText) findViewById(R.id.filter_matches);
		
		actionView.setOnItemSelectedListener(this);
		matchesView.addTextChangedListener(this);
		replaceView.addTextChangedListener(this);
		
		// Bind buttons to their actions
		Button bt = (Button) findViewById(R.id.cancel_bt);
		bt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//TODO : clean prefs
				setResult(RESULT_CANCELED, getIntent());
				finish();
			}
		});
		
		saveButton = (Button) findViewById(R.id.save_bt);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				saveFilter();
				setResult(RESULT_OK, getIntent());
				finish();
			}
		});
		fillLayout();
		checkFormValidity();
	}
	
	
	private void saveFilter() {
		filter.account = accountId;
		filter.matches = matchesView.getText().toString();
		filter.replace = replaceView.getText().toString();
		filter.action = actionView.getSelectedItemPosition();
		database.open();
		if(filterId < 0) {
			database.insertFilter(filter);
		}else {
			database.updateFilter(filter);
		}
		database.close();
	}
	
	private void fillLayout() {
		matchesView.setText(filter.matches);
		replaceView.setText(filter.replace);
		if(filter.action != null) {
			actionView.setSelection(filter.action);
		}
		
	}
	
	private void checkFormValidity() {
		boolean isValid = true;
		
		if(TextUtils.isEmpty(matchesView.getText().toString())) {
			isValid = false;
		}
		
		if(TextUtils.isEmpty(replaceView.getText().toString())) {
			isValid = false;
		}
		
		saveButton.setEnabled(isValid);
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		checkFormValidity();
	}
	


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		// Nothing to do
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Nothing to do
		
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		checkFormValidity();
		
	}


}
