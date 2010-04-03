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
package com.csipsimple.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import com.csipsimple.R;
import com.csipsimple.service.SipService;

public class SipHome extends TabActivity {
    public static final int ACCOUNTS_MENU = Menu.FIRST + 1;
	public static final int PARAMS_MENU = Menu.FIRST + 2;
	public static final int CLOSE_MENU = Menu.FIRST + 3;
	
	private static final String LAST_KNOWN_VERSION_PREF = "last_known_version";
	
	private static final String THIS_FILE = "SIP HOME";
	
	
	private Intent serviceIntent;
	
	private Intent dialerIntent;
	private Intent csipIntent;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(THIS_FILE, "On Create SIPHOME");
        super.onCreate(savedInstanceState);
        
        TabHost tabHost = getTabHost();
        Resources r = getResources();
        dialerIntent = new Intent(this, Dialer.class);
        csipIntent = new Intent(this, BuddyList.class);
        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator("Dialer", r.getDrawable(R.drawable.ic_tab_selected_dialer))
                .setContent(dialerIntent));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator("Buddy list")
                .setContent(csipIntent));
        
        //Check sip stack
        if( !SipService.hasStackLibFile(this) ){
        	Log.d(THIS_FILE, "Has no sip stack....");
			Intent welcomeIntent = new Intent(this, WelcomeScreen.class);
			//welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(welcomeIntent);
        }else{
        	Log.d(THIS_FILE, "We have already the sip stack, start sip service");
	        //Start the service
	        serviceIntent = new Intent(SipHome.this, SipService.class);
	        startService(serviceIntent);
        }
        
        
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
				
				
				//TODO : add a changelog screen 
				//(maybe we could brick the app while we are in alpha releases to force reinstall)
				Editor editor = prefs.edit();
				editor.putInt(LAST_KNOWN_VERSION_PREF, running_version);
				editor.commit();
			}
		} catch (NameNotFoundException e) {
			//Should not happen....or something is wrong with android...
		}
        
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
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(THIS_FILE, "---DESTROY SIP HOME END---");
	}
    

    private void populateMenu(Menu menu) {
    	menu.add(Menu.NONE, ACCOUNTS_MENU, Menu.NONE, "Accounts").setIcon(
				R.drawable.ic_menu_accounts);
		menu.add(Menu.NONE, PARAMS_MENU, Menu.NONE, "Params").setIcon(
				android.R.drawable.ic_menu_preferences);
		menu.add(Menu.NONE, CLOSE_MENU, Menu.NONE, "Quit").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
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
			startActivity(new Intent(this, Preferences.class));
			return true;
		case CLOSE_MENU:
	    	Log.d(THIS_FILE, "CLOSE");
	    	if(serviceIntent != null){
				stopService(serviceIntent);
			}
	    	serviceIntent = null;
			this.finish();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    
}
