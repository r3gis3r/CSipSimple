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

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.prefs.MainPrefs;
import com.csipsimple.ui.prefs.PrefsFast;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.IndicatorTab;

public class SipHome extends TabActivity {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	
	public static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	public static final String HAS_ALREADY_SETUP = "has_already_setup";
	
	private static final String THIS_FILE = "SIP HOME";
	
	private Intent serviceIntent;
	
	private Intent dialerIntent;
	private Intent calllogsIntent;
	private PreferencesWrapper prefWrapper;
	
	private boolean has_tried_once_to_activate_account = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(THIS_FILE, "On Create SIPHOME");

    	
        super.onCreate(savedInstanceState);
        
        prefWrapper = new PreferencesWrapper(this);
        
        //Check sip stack
        if( !SipService.hasStackLibFile(this) ){
        	Log.d(THIS_FILE, "Has no sip stack....");
			Intent welcomeIntent = new Intent(this, WelcomeScreen.class);
			welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			welcomeIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_WELCOME);
			startActivity(welcomeIntent);
			finish();
			return;
        }else if(!SipService.isBundleStack(this)){
            //We have to check and save current version
            try {
    			PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
    			int running_version = pinfo.versionCode;
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    			int last_seen_version = prefs.getInt(LAST_KNOWN_VERSION_PREF, 0);
    			
    			Log.d(THIS_FILE, "Last known version is "+last_seen_version+" and currently we are running "+running_version);
    			if(last_seen_version != running_version) {
    				//TODO : check if greater version
    				//(should be most of the case...but if not we should maybe popup the user that 
    				//if n+1 version doesn't work for him he could fill a bug on bug tracker)
    				
    				
    				Log.d(THIS_FILE, "Sip stack may have changed");
    				Intent changelogIntent = new Intent(this, WelcomeScreen.class);
    				changelogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    				changelogIntent.putExtra(WelcomeScreen.KEY_MODE, WelcomeScreen.MODE_CHANGELOG);
    				startActivity(changelogIntent);
    				finish();
    				return;
    			}else {
    	        	Log.d(THIS_FILE, "WE CAN NOW start SIP service");
    		        //Start the service
    		        startSipService();
    			}
    		} catch (NameNotFoundException e) {
    			//Should not happen....or something is wrong with android...
    		}
        }else {
        	Log.d(THIS_FILE, "WE CAN NOW start SIP service");
	        startSipService();
        }
        
        setContentView(R.layout.home);
        
        dialerIntent = new Intent(this, Dialer.class);
        calllogsIntent = new Intent(this, CallLogsList.class);
        
        addTab("tab1", getString(R.string.dial_tab_name_text), R.drawable.ic_tab_selected_dialer, R.drawable.ic_tab_unselected_dialer, dialerIntent);
        addTab("tab2", getString(R.string.calllog_tab_name_text), R.drawable.ic_tab_selected_recent, R.drawable.ic_tab_unselected_recent, calllogsIntent);

        
        has_tried_once_to_activate_account = false;
    }
    
    private void startSipService() {
    	if(serviceIntent == null) {
    		serviceIntent = new Intent(SipHome.this, SipService.class);
    	}
    	startService(serviceIntent);
        
    }
    
    private void addTab(String tag, String label, int icon, int ficon, Intent content) {
    	TabHost tabHost = getTabHost();
    	
		TabSpec tabspecDialer = tabHost.newTabSpec(tag).setContent(content);
		
		boolean fails = true;
		if(Compatibility.isCompatible(4)) {
			IndicatorTab icTab = new IndicatorTab(this, null);
		 	icTab.setResources(label, icon, ficon);
		 	try {
				Method method = tabspecDialer.getClass().getDeclaredMethod("setIndicator", View.class);
				method.invoke(tabspecDialer, icTab);
				fails = false;
			} catch (Exception e) {
				Log.d(THIS_FILE, "We are probably on 1.5 : use standard simple tabs");
			} 
		 	
		}
		if(fails){
			// Fallback to old style icons
		    tabspecDialer.setIndicator(label, getResources().getDrawable(icon));
		}
		
		tabHost.addTab(tabspecDialer);
    }
    
    @Override
    protected void onPause() {
    	Log.d(THIS_FILE, "On Pause SIPHOME");
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	Log.d(THIS_FILE, "On Resume SIPHOME");
    	super.onResume();
    	
    	//If we have never set fast settings
    	if(!prefWrapper.hasAlreadySetup()) {
    		Intent prefsIntent = new Intent(this, PrefsFast.class);
    		prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 			startActivity(prefsIntent);
 			return;
    	}
    	
    	//If we have no account yet, open account panel,
    	if(!has_tried_once_to_activate_account) {
	        DBAdapter db = new DBAdapter(this);
	        db.open();
	        int nbrOfAccount = db.getNbrOfAccount();
	        db.close();
	        if(nbrOfAccount == 0) {
		        Intent accountIntent = new Intent(this, AccountsList.class);
		        accountIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(accountIntent);
				has_tried_once_to_activate_account = true;
				return;
	        }
	        has_tried_once_to_activate_account = true;
    	}
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
	}
    
    
    private void populateMenu(Menu menu) {
    	menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, R.string.accounts).setIcon(
				R.drawable.ic_menu_accounts);
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, R.string.prefs).setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, R.string.menu_quit_text).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		
	}
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
    	PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
    	menu.findItem(CLOSE_MENU).setVisible(!prefsWrapper.isValidConnectionForIncoming());
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ACCOUNTS_MENU:
			startActivity(new Intent(this, AccountsList.class));
			return true;
		case PARAMS_MENU:
			startActivity(new Intent(this, MainPrefs.class));
			return true;
		case CLOSE_MENU:
	    	Log.d(THIS_FILE, "CLOSE");
	    	if(serviceIntent != null){
				stopService(serviceIntent);
			}
	    	serviceIntent = null;
	    	PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
	    	ArrayList<String> networks = prefsWrapper.getAllIncomingNetworks();
	    	if(networks.size()>0) {
	    		//TODO: translate %s style
	    		String msg =  "Will automatically restart when : ";
	    		msg += TextUtils.join(", ", networks);
	    		msg += " will become available (change settings if you don't want)";
	    		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	    	}
	    	
			this.finish();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    
}
