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
package com.csipsimple.wizards;

import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsua;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.ui.prefs.GenericPrefs;

public abstract class BasePrefsWizard extends GenericPrefs{
    public static final int SAVE_MENU = Menu.FIRST + 1;
	public static final int TRANSFORM_MENU = Menu.FIRST + 2;
	public static final int DELETE_MENU = Menu.FIRST + 3;
	
	public static final int CHOOSE_WIZARD = 0;
	
	private long accountId = -1;
	protected Account account = null;
	private Button saveButton;
	private DBAdapter database;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Get back the concerned account and if any set the current (if not a new account is created)
		Intent intent = getIntent();
        accountId = intent.getIntExtra(Intent.EXTRA_UID, -1);
        
        database = new DBAdapter(this);
		database.open();
		account = database.getAccount(accountId);
		database.close();

		super.onCreate(savedInstanceState);
		
		//Bind buttons to their actions
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
				//TODO: clean prefs
				saveAccount();
				setResult(RESULT_OK, getIntent());
				finish();
			}
		});
		fillLayout();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateDescriptions();
		saveButton.setEnabled(canSave());
	}
	
	@Override
	protected void beforeBuildPrefs() {
		//Use our custom wizard view
		setContentView(R.layout.wizard_prefs_base);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		updateDescriptions();
		saveButton.setEnabled(canSave());
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, SAVE_MENU, Menu.NONE, R.string.save).setIcon(
				android.R.drawable.ic_menu_save);

		if(account.id != null && !account.id.equals(0)){
			menu.add(Menu.NONE, TRANSFORM_MENU, Menu.NONE, R.string.prefs).setIcon(
					android.R.drawable.ic_menu_edit);
			menu.add(Menu.NONE, DELETE_MENU, Menu.NONE, R.string.delete_account).setIcon(
					android.R.drawable.ic_menu_delete);
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	 @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_MENU:
			saveAccount();
			setResult(RESULT_OK, getIntent());
			finish();
			return true;
		case TRANSFORM_MENU:
			startActivityForResult(new Intent(this, WizardChooser.class), CHOOSE_WIZARD);
			return true;
		case DELETE_MENU:
			if(account.id != null && !account.id.equals(0)){
				database.open();
				database.deleteAccount(account);
				database.close();
				setResult(RESULT_OK, getIntent());
				finish();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}
	 
	 @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case CHOOSE_WIZARD:
			if(resultCode == RESULT_OK) {
				if(data != null && data.getExtras() != null) {
					String wizardId = data.getExtras().getString(WizardUtils.ID);
					if(wizardId != null) {
						saveAccount(wizardId);
						setResult(RESULT_OK, getIntent());
						finish();
					}
				}
			}
			break;
		}
	}
	
	
	//Utilities functions
	protected boolean isEmpty(EditTextPreference edt){
		if(edt.getText() == null){
			return true;
		}
		if(edt.getText().equals("")){
			return true;
		}
		return false;
	}
	
	protected boolean isMatching(EditTextPreference edt, String regex) {
		if(edt.getText() == null){
			return false;
		}
		return Pattern.matches(regex, edt.getText());
	}

	protected pj_str_t getPjText(EditTextPreference edt){
		return pjsua.pj_str_copy(edt.getText());
	}
	
	protected void saveAccount() {
		saveAccount(getWizardId());
	}
	
	protected void saveAccount(String wizardId){
		buildAccount();
		account.wizard = wizardId;
		database.open();
		if(account.id == null || account.id.equals(0)){
			account.id = (int) database.insertAccount(account);
		}else{
			database.updateAccount(account);
		}
		database.close();
		
	}
	

	@Override
	protected String getDefaultFieldSummary(String field_name){
		String val = "";
		try {
			String keyid = R.string.class.getField("w_"+getXmlPrefix()+"_"+field_name+"_desc").get(null).toString();
			val = getString( Integer.parseInt(keyid) );
		} catch (SecurityException e) {
			//Nothing to do : desc is null
		} catch (NoSuchFieldException e) {
			//Nothing to do : desc is null
		} catch (IllegalArgumentException e) {
			//Nothing to do : desc is null
		} catch (IllegalAccessException e) {
			//Nothing to do : desc is null
		}
		return val;
	}
	
	private void markFieldInvalid(Preference field) {
		field.setLayoutResource(R.layout.invalid_preference_row);
	}
	
	private void markFieldValid(Preference field) {
		field.setLayoutResource(R.layout.valid_preference_row);
	}
	
	/**
	 * Check the validity of a field and if invalid mark it as invalid
	 * @param field field to check
	 * @param isNotValid if true this field is considered as invalid
	 * @return if the field is valid (!isNotValid)
	 * This is convenient for &= from a true variable over multiple fields
	 */
	protected boolean checkField(Preference field, boolean isNotValid) {
		if(isNotValid) {
			markFieldInvalid(field);
		}else {
			markFieldValid(field);
		}
		return !isNotValid;
	}

	protected abstract void fillLayout();
	protected abstract void updateDescriptions();
	protected abstract boolean canSave();
	protected abstract void buildAccount();
	protected abstract int getXmlPreferences();
	protected abstract String getXmlPrefix();
	protected abstract String getWizardId();
	
	
}
