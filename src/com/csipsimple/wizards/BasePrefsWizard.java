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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.AccountFilters;
import com.csipsimple.ui.prefs.GenericPrefs;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
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
	protected SipProfile account = null;
	private Button saveButton;
	private DBAdapter database;
	private String wizardId = "";
	private WizardInfo wizardInfo = null;
	private WizardIface wizard = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Get back the concerned account and if any set the current (if not a new account is created)
		Intent intent = getIntent();
        accountId = intent.getIntExtra(Intent.EXTRA_UID, SipProfile.INVALID_ID);
        
        //TODO : ensure this is not null...
        setWizardId(intent.getStringExtra(SipProfile.FIELD_WIZARD));
        
        
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
	protected void onStart() {
		super.onStart();
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(THIS_FILE, "Unbind from service");
		try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateDescriptions();
		updateValidation();
	}
	
	
	// Service connection
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			updateValidation();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
	
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
		updateValidation();
	}
	
	private void updateValidation() {
		if(service != null) {
			saveButton.setEnabled(wizard.canSave());
		}else {
			saveButton.setEnabled(false);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, SAVE_MENU, Menu.NONE, R.string.save).setIcon(
				android.R.drawable.ic_menu_save);
		if(account.id != SipProfile.INVALID_ID){
			menu.add(Menu.NONE, TRANSFORM_MENU, Menu.NONE, R.string.choose_wizard).setIcon(
					android.R.drawable.ic_menu_edit);
			menu.add(Menu.NONE, FILTERS_MENU, Menu.NONE, R.string.filters).setIcon(
					R.drawable.ic_menu_filter);
			menu.add(Menu.NONE, DELETE_MENU, Menu.NONE, R.string.delete_account).setIcon(
					android.R.drawable.ic_menu_delete);
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(SAVE_MENU).setVisible(wizard.canSave());
	
		return super.onPrepareOptionsMenu(menu);
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
			if(account.id != SipProfile.INVALID_ID){
				database.open();
				database.deleteAccount(account);
				database.close();
				setResult(RESULT_OK, getIntent());
				finish();
			}
			return true;
		case FILTERS_MENU:
			if(account.id != SipProfile.INVALID_ID){
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
					String wizardId = data.getStringExtra(WizardUtils.ID);
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
		setResult(RESULT_OK, intent);
		finish();
	}
	
	protected void saveAccount() {
		saveAccount(wizardId);
	}
	
	protected void saveAccount(String wizardId){
		boolean needRestart = false;

		PreferencesWrapper prefs = new PreferencesWrapper(this);
		account = wizard.buildAccount(account);
		account.wizard = wizardId;
		database.open();
		if(account.id == SipProfile.INVALID_ID){
			wizard.setDefaultParams(prefs);
			account.id = (int) database.insertAccount(account);
			needRestart = wizard.needRestart();
		}else{
			//TODO : should not be done there but if not we should add an option to re-apply default params
			wizard.setDefaultParams(prefs);
			database.updateAccount(account);
		}
		database.close();
		
		
		if(needRestart) {
			restartAsync();
		}else {
			reloadAccountsAsync();
		}
	}
	
	private void restartAsync() {
		Thread t = new Thread() {
			@Override
			public void run() {
				Log.d(THIS_FILE, "Would like to restart stack");
				if (service != null) {
					Log.d(THIS_FILE, "Will reload the stack !");
					try {
						service.sipStop();
						service.sipStart();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to reload stack", e);
					}
				}
			};
		};
		t.start();
	}
	
	private void reloadAccountsAsync() {
		Thread t = new Thread() {
			@Override
			public void run() {
				Log.d(THIS_FILE, "Would like to reload all accounts");
				if (service != null) {
					Log.d(THIS_FILE, "Will reload accounts !");
					try {
						service.reAddAllAccounts();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to readd accoutns", e);
					}
				}
			};
		};
		t.start();
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
