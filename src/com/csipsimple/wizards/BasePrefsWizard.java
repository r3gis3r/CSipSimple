/**
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

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsua;

import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.csipsimple.R;

public abstract class BasePrefsWizard extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	private long mAccountId = -1;
	protected Account mAccount = null;
	private Button mSaveButton;
	private DBAdapter db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Use our custom wizard view
		setContentView(R.layout.wizard_prefs_base);
		// Use preference resource. Since it's easier to manage,
		// i use prefs here : (inspired from android Email application
		// Things should be done by a cleaner way (besides, right now prefs remains in shared prefs)
		// but for now it's ok, since it's well managed
		addPreferencesFromResource(getXmlPreferences());
		
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
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
		
		
		
		mSaveButton = (Button) findViewById(R.id.save_bt);
		mSaveButton.setEnabled(false);
		mSaveButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//TODO: clean prefs
				saveAccount();
				setResult(RESULT_OK, getIntent());
				finish();
			}
		});
		
		
		Intent i = getIntent();
        mAccountId = i.getIntExtra(Intent.EXTRA_UID, -1);
        
        db = new DBAdapter(this);
		db.open();
		mAccount = db.getAccount(mAccountId);
		db.close();
		
		
        
		fillLayout();
		
		updateDescriptions();
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		updateDescriptions();
		mSaveButton.setEnabled(canSave());	
	}
	
	
	protected boolean isEmpty(EditTextPreference edt){
		if(edt.getText() == null){
			return true;
		}
		if(edt.getText().equals("")){
			return true;
		}
		return false;
	}
	

	protected pj_str_t getPjText(EditTextPreference edt){
		return pjsua.pj_str_copy(edt.getText());
	}
	
	protected void saveAccount(){
		buildAccount();		
		
		mAccount.wizard = getWizardId();
		db.open();
		if(mAccount.id == null || mAccount.id.equals(0)){
			mAccount.id = (int) db.insertAccount(mAccount);
		}else{
			db.updateAccount(mAccount);
		}
		db.close();
		
	}
	
	
	private String getDefaultFieldSummary(String field_name){
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
	

	protected void setStringFieldSummary(String field_name){
		PreferenceScreen pfs = getPreferenceScreen();
		SharedPreferences sp = pfs.getSharedPreferences();
		Preference pref = pfs.findPreference(field_name);
		
		String val = sp.getString(field_name, "");
		if(val.equals("")){
			val = getDefaultFieldSummary(field_name);
		}
		pref.setSummary(val);
	}
	
	protected void setPasswordFieldSummary(String field_name){
		PreferenceScreen pfs = getPreferenceScreen();
		SharedPreferences sp = pfs.getSharedPreferences();
		Preference pref = pfs.findPreference(field_name);
		
		String val = sp.getString(field_name, "");
		
		if(val.equals("")){
			val = getDefaultFieldSummary(field_name);
		}else{
			val = val.replaceAll(".", "*");
		}
		pref.setSummary(val);
	}

	protected abstract void fillLayout();
	protected abstract void updateDescriptions();
	protected abstract boolean canSave();
	protected abstract void buildAccount();
	protected abstract int getXmlPreferences();
	protected abstract String getWizardId();
	protected abstract String getXmlPrefix();
	
	
}
