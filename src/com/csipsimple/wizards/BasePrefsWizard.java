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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.ui.AccountFilters;
import com.csipsimple.ui.prefs.GenericPrefs;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class BasePrefsWizard extends GenericPrefs{
    public static final int SAVE_MENU = Menu.FIRST + 1;
	public static final int TRANSFORM_MENU = Menu.FIRST + 2;
	public static final int FILTERS_MENU = Menu.FIRST + 3;
	public static final int DELETE_MENU = Menu.FIRST + 4;
	
	public static final int CHOOSE_WIZARD = 0;
	public static final int MODIFY_FILTERS = 1;
	private static final String THIS_FILE = "Base Prefs wizard";
	
	private long accountId = -1;
	protected Account account = null;
	private Button saveButton;
	private DBAdapter database;
	private String wizardId = "";
	private WizardInfo wizardInfo = null;
	private WizardIface wizard = null;
	private boolean needRestart = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Get back the concerned account and if any set the current (if not a new account is created)
		Intent intent = getIntent();
        accountId = intent.getIntExtra(Intent.EXTRA_UID, -1);
        
        //TODO : ensure this is not null...
        setWizardId(intent.getStringExtra(Intent.EXTRA_REMOTE_INTENT_TOKEN));
        
        
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
				setResult(RESULT_CANCELED, getIntent());
				finish();
			}
		});
		
		saveButton = (Button) findViewById(R.id.save_bt);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				saveAndFinish();
			}
		});
		wizard.fillLayout(account);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateDescriptions();
		saveButton.setEnabled(wizard.canSave());
	}
	
	private boolean setWizardId(String wId) {
		if(wizardId == null) {
        	return setWizardId("EXPERT");
        }
        
        wizardInfo = WizardUtils.getWizardClass(wId);
        if(wizardInfo == null) {
        	if(!wizardId.equals("EXPERT")) {
        		return setWizardId("EXPERT");
        	}
        	return false;
        }
        
        try {
			wizard = (WizardIface) wizardInfo.classObject.newInstance();
		} catch (IllegalAccessException e) {
			Log.e(THIS_FILE, "Can't access wizard class", e);
			if(!wizardId.equals("EXPERT")) {
        		return setWizardId("EXPERT");
        	}
        	return false;
		} catch (InstantiationException e) {
			Log.e(THIS_FILE, "Can't access wizard class", e);
			if(!wizardId.equals("EXPERT")) {
        		return setWizardId("EXPERT");
        	}
        	return false;
		}
		wizardId = wId;
        wizard.setParent(this);
        
        return true;
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
		saveButton.setEnabled(wizard.canSave());
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, SAVE_MENU, Menu.NONE, R.string.save).setIcon(
				android.R.drawable.ic_menu_save);

		if(account.id != null && !account.id.equals(-1)){
			menu.add(Menu.NONE, TRANSFORM_MENU, Menu.NONE, R.string.choose_wizard).setIcon(
					android.R.drawable.ic_menu_edit);
			menu.add(Menu.NONE, FILTERS_MENU, Menu.NONE, R.string.filters).setIcon(
					android.R.drawable.ic_menu_manage);
			menu.add(Menu.NONE, DELETE_MENU, Menu.NONE, R.string.delete_account).setIcon(
					android.R.drawable.ic_menu_delete);
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	 @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SAVE_MENU:
			saveAndFinish();
			return true;
		case TRANSFORM_MENU:
			startActivityForResult(new Intent(this, WizardChooser.class), CHOOSE_WIZARD);
			return true;
		case DELETE_MENU:
			if(account.id != null && !account.id.equals(-1)){
				database.open();
				database.deleteAccount(account);
				database.close();
				setResult(RESULT_OK, getIntent());
				finish();
			}
			return true;
		case FILTERS_MENU:
			if(account.id != null && !account.id.equals(-1)){
				Intent it = new Intent(this, AccountFilters.class);
    			it.putExtra(Intent.EXTRA_UID,  (int) account.id);
    			startActivityForResult(it, MODIFY_FILTERS);
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
	
	private void saveAndFinish() {
		saveAccount();
		Intent intent = getIntent();
		intent.putExtra("need_restart", needRestart);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	protected void saveAccount() {
		saveAccount(wizardId);
	}
	
	protected void saveAccount(String wizardId){
		needRestart = false;
		account = wizard.buildAccount(account);
		account.wizard = wizardId;
		database.open();
		if(account.id == null || account.id.equals(-1)){
			account.id = (int) database.insertAccount(account);
			if(wizard.needRestart()) {
				needRestart = true;
			}
		}else{
			database.updateAccount(account);
		}
		database.close();
		
	}

	@Override
	protected int getXmlPreferences() {
		return wizard.getBasePreferenceResource();
	}

	@Override
	protected void updateDescriptions() {
		wizard.updateDescriptions();
	}
	
	protected String getDefaultFieldSummary(String fieldName){
		return wizard.getDefaultFieldSummary(fieldName);
	}
	
}
