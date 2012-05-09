/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.models.Filter.RegExpRepresentation;
import com.csipsimple.utils.Log;

public class EditFilter extends Activity implements OnItemSelectedListener, TextWatcher {

	private static final String THIS_FILE = "EditFilter";
	private int filterId;
	private Filter filter;
	private Button saveButton;
	private long accountId;
	private EditText replaceTextEditor;
	private Spinner actionSpinner;
	private EditText matchesTextEditor;
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
        accountId = intent.getLongExtra(Filter.FIELD_ACCOUNT, SipProfile.INVALID_ID);
        
        if(accountId == SipProfile.INVALID_ID) {
        	Log.e(THIS_FILE, "Invalid account");
        	finish();
        }
        
        filter = Filter.getProfileFromDbId(this, filterId, Filter.FULL_PROJ);
        
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.edit_filter);
		
		
		// Bind view objects
		actionSpinner = (Spinner) findViewById(R.id.filter_action);
		matcherSpinner = (Spinner) findViewById(R.id.matcher_type);
		replaceSpinner = (Spinner) findViewById(R.id.replace_type);
		
		replaceTextEditor = (EditText) findViewById(R.id.filter_replace);
		matchesTextEditor = (EditText) findViewById(R.id.filter_matches);
		
		//Bind containers objects
//		matchesContainer = (View) findViewById(R.id.matcher_block);
		replaceContainer = (View) findViewById(R.id.replace_block);
		
		

		actionSpinner.setOnItemSelectedListener(this);
		matcherSpinner.setOnItemSelectedListener(this);
		initMatcherSpinner = false;
		replaceSpinner.setOnItemSelectedListener(this);
		initReplaceSpinner = false;
		matchesTextEditor.addTextChangedListener(this);
		replaceTextEditor.addTextChangedListener(this);
		
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
		
		filter.account = (int) accountId;
		filter.action = Filter.getActionForPosition(actionSpinner.getSelectedItemPosition());
		RegExpRepresentation repr = new RegExpRepresentation();
		//Matcher
		repr.type = Filter.getMatcherForPosition(matcherSpinner.getSelectedItemPosition());
		repr.fieldContent = matchesTextEditor.getText().toString();
		filter.setMatcherRepresentation(repr);
		
		
		//Rewriter
		if(filter.action == Filter.ACTION_REPLACE) {
			repr.fieldContent = replaceTextEditor.getText().toString();
			repr.type = Filter.getReplaceForPosition(replaceSpinner.getSelectedItemPosition());
			filter.setReplaceRepresentation(repr);
		}else if(filter.action == Filter.ACTION_AUTO_ANSWER){
		    filter.replacePattern = replaceTextEditor.getText().toString();
		}else{
			filter.replacePattern = "";
		}
		
		//Save
		if(filterId < 0) {
			Cursor currentCursor = getContentResolver().query(SipManager.FILTER_URI, new String[] {Filter._ID}, 
					Filter.FIELD_ACCOUNT + "=?", 
					new String[] {
						filter.account.toString()
					}, null);
			filter.priority = 0;
			if(currentCursor != null) {
				filter.priority = currentCursor.getCount();
				currentCursor.close();
			}
			getContentResolver().insert(SipManager.FILTER_URI, filter.getDbContentValues());
		}else {
			getContentResolver().update(ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, filterId), filter.getDbContentValues(), null, null);
		}
	}
	
	private void fillLayout() {
		//Set action
		actionSpinner.setSelection(Filter.getPositionForAction(filter.action));
		RegExpRepresentation repr = filter.getRepresentationForMatcher();
		//Set matcher - selection must be done first since raise on item change listener
		matcherSpinner.setSelection(Filter.getPositionForMatcher(repr.type));
		matchesTextEditor.setText(repr.fieldContent);
		//Set replace
		repr = filter.getRepresentationForReplace();
		replaceSpinner.setSelection(Filter.getPositionForReplace(repr.type));
		replaceTextEditor.setText(repr.fieldContent);
		
	}
	
	private void checkFormValidity() {
		boolean isValid = true;
		int action = Filter.getActionForPosition(actionSpinner.getSelectedItemPosition());
		
		if(TextUtils.isEmpty(matchesTextEditor.getText().toString()) && 
				Filter.getMatcherForPosition(matcherSpinner.getSelectedItemPosition() ) != Filter.MATCHER_ALL ){
			isValid = false;
		}
		if(action == Filter.ACTION_AUTO_ANSWER) {
		    if(!TextUtils.isEmpty(replaceTextEditor.getText().toString())) {
		        try{
		            Integer.parseInt(replaceTextEditor.getText().toString());
		        }catch(NumberFormatException e) {
		            isValid = false;
		        }
		    }
		}
		
		saveButton.setEnabled(isValid);
	}


	@Override
	public void onItemSelected(AdapterView<?> spinner, View arg1, int arg2, long arg3) {
		int spinnerId = spinner.getId();
		if (spinnerId == R.id.filter_action) {
		    int action = Filter.getActionForPosition(actionSpinner.getSelectedItemPosition()) ;
			if(action == Filter.ACTION_REPLACE || action == Filter.ACTION_AUTO_ANSWER) {
				replaceContainer.setVisibility(View.VISIBLE);
				if(action == Filter.ACTION_REPLACE) {
                    replaceSpinner.setVisibility(View.VISIBLE);
                    replaceTextEditor.setHint("");
				}else {
				    replaceSpinner.setVisibility(View.GONE);
				    replaceTextEditor.setHint(R.string.optional_sip_code);
				}
			}else {
				replaceContainer.setVisibility(View.GONE);
			}
		} else if (spinnerId == R.id.matcher_type) {
			if(initMatcherSpinner) {
				matchesTextEditor.setText("");
			}else {
				initMatcherSpinner = true;
			}
		} else if (spinnerId == R.id.replace_type) {
			if(initReplaceSpinner) {
				replaceTextEditor.setText("");
			}else {
				initReplaceSpinner = true;
			}
		}
		boolean showMatcherView = Filter.getMatcherForPosition(matcherSpinner.getSelectedItemPosition() ) != Filter.MATCHER_ALL ;
		matchesTextEditor.setVisibility(showMatcherView ? View.VISIBLE : View.GONE);
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
