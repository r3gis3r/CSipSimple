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
import com.csipsimple.models.Filter.RegExpRepresentation;
import com.csipsimple.utils.Log;

public class EditFilter extends Activity implements OnItemSelectedListener, TextWatcher {

	private static final String THIS_FILE = "EditFilter";
	private int filterId;
	private DBAdapter database;
	private Filter filter;
	private Button saveButton;
	private int accountId;
	private EditText replaceView;
	private Spinner actionSpinner;
	private EditText matchesView;
//	private View matchesContainer;
	private View replaceContainer;
	private Spinner replaceSpinner;
	private Spinner matcherSpinner;
	private boolean initMatcherSpinner;
	private boolean initReplaceSpinner;
	

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
		actionSpinner = (Spinner) findViewById(R.id.filter_action);
		matcherSpinner = (Spinner) findViewById(R.id.matcher_type);
		replaceSpinner = (Spinner) findViewById(R.id.replace_type);
		
		replaceView = (EditText) findViewById(R.id.filter_replace);
		matchesView = (EditText) findViewById(R.id.filter_matches);
		
		//Bind containers objects
//		matchesContainer = (View) findViewById(R.id.matcher_block);
		replaceContainer = (View) findViewById(R.id.replace_block);
		
		

		actionSpinner.setOnItemSelectedListener(this);
		matcherSpinner.setOnItemSelectedListener(this);
		initMatcherSpinner = false;
		replaceSpinner.setOnItemSelectedListener(this);
		initReplaceSpinner = false;
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
		//Update filter object
		
		filter.account = accountId;
		filter.action = Filter.getActionForPosition(actionSpinner.getSelectedItemPosition());
		RegExpRepresentation repr = new RegExpRepresentation();
		//Matcher
		repr.type = Filter.getMatcherForPosition(matcherSpinner.getSelectedItemPosition());
		repr.fieldContent = matchesView.getText().toString();
		filter.setMatcherRepresentation(repr);
		
		
		//Rewriter
		if(filter.action == Filter.ACTION_REPLACE) {
			repr.fieldContent = replaceView.getText().toString();
			repr.type = Filter.getReplaceForPosition(replaceSpinner.getSelectedItemPosition());
			filter.setReplaceRepresentation(repr);
		}else {
			filter.replace = "";
		}
		
		//Save
		database.open();
		if(filterId < 0) {
			filter.priority = database.getCountFiltersForAccount(filter.account);
			database.insertFilter(filter);
		}else {
			database.updateFilter(filter);
		}
		database.close();
	}
	
	private void fillLayout() {
		//Set action
		actionSpinner.setSelection(Filter.getPositionForAction(filter.action));
		RegExpRepresentation repr = filter.getRepresentationForMatcher();
		//Set matcher - selection must be done first since raise on item change listener
		matcherSpinner.setSelection(Filter.getPositionForMatcher(repr.type));
		matchesView.setText(repr.fieldContent);
		//Set replace
		repr = filter.getRepresentationForReplace();
		replaceSpinner.setSelection(Filter.getPositionForReplace(repr.type));
		replaceView.setText(repr.fieldContent);
		
	}
	
	private void checkFormValidity() {
		boolean isValid = true;
		
		if(TextUtils.isEmpty(matchesView.getText().toString()) && Filter.getActionForPosition(actionSpinner.getSelectedItemPosition()) != Filter.ACTION_REPLACE) {
			isValid = false;
		}
		/*
		if(Filter.getActionForPosition(actionSpinner.getSelectedItemPosition()) == Filter.ACTION_REPLACE) {
			if(TextUtils.isEmpty(replaceView.getText().toString())) {
				isValid = false;
			}
		}
		*/
		
		saveButton.setEnabled(isValid);
	}


	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int arg2, long arg3) {
		switch(spinner.getId()) {
		case R.id.filter_action:
			if(Filter.getActionForPosition(actionSpinner.getSelectedItemPosition()) == Filter.ACTION_REPLACE) {
				replaceContainer.setVisibility(View.VISIBLE);
			}else {
				replaceContainer.setVisibility(View.GONE);
			}
			break;
		case R.id.matcher_type:
			if(initMatcherSpinner) {
				matchesView.setText("");
			}else {
				initMatcherSpinner = true;
			}
			break;
		case R.id.replace_type:
			if(initReplaceSpinner) {
				replaceView.setText("");
			}else {
				initReplaceSpinner = true;
			}
			break;
		}
		checkFormValidity();
	}
	


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		checkFormValidity();
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
