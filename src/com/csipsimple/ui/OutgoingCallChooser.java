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

import java.util.ArrayList;
import java.util.List;

import org.pjsip.pjsua.pjsip_status_code;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.csipsimple.R;
import com.csipsimple.db.AccountAdapter;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

public class OutgoingCallChooser extends ListActivity {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	String number;
	Window w;
	
	public final static int AUTO_CHOOSE_TIME = 8000;
	private List<Account> accountsList;

	private static final String THIS_FILE = "SIP OUTChoose";
	
	// [sentinel]
	private ISipService service = null;
	private ServiceConnection connection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			
			// Fill accounts with currently -usable- accounts
			// At this point we need 'service' to be live (see DBAdapter)
			updateList();
			
			/*
			 * Disabled since we don't want to force user to do a pstn call if sip should have been applied.
			if (adapter.isEmpty()) {
				Log.d(THIS_FILE, "No usable accounts for SIP, skip the chooser, -> PSTN call");
				placePstnCall();
				return;
			}
			*/

			// Need full selector, finish layout
			setContentView(R.layout.outgoing_account_list);
			
			//This need to be done after setContentView call
			w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
					android.R.drawable.ic_menu_call);
			//TODO : internationalisation should be %s form
			String phoneNumber = number;
			setTitle(getString(R.string.outgoing_call_chooser_call_text) + " " + phoneNumber);

			// Inform the list we provide context menus for items
			//	getListView().setOnCreateContextMenuListener(this);

			LinearLayout add_row = (LinearLayout) findViewById(R.id.use_pstn_row);
			add_row.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d(THIS_FILE, "Choosen : pstn");
					placePstnCall();
				}
			});
			
			
			if(!Compatibility.canMakeGSMCall(OutgoingCallChooser.this)) {
				add_row.setVisibility(View.GONE);
			}
			
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
    
   	private BroadcastReceiver regStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			updateList();
		}
	};
	
	private Integer accountToCallTo = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "Starting ");
		
		number = PhoneNumberUtils.getNumberFromIntent(getIntent(), this);
		
		if(number == null && getIntent().getAction().equalsIgnoreCase(Intent.ACTION_CALL)) {
			number = getIntent().getData().getSchemeSpecificPart();
		}
		/*else {
			Log.e(THIS_FILE, "This action : "+getIntent().getAction()+" is not supported by this view");
			return;
		}
		*/
		
		if(number == null) {
			Log.e(THIS_FILE, "No number detected for : "+getIntent().getAction());
			super.onCreate(savedInstanceState);
			finish();
			return;
		}
		/*
		Log.d(THIS_FILE, getIntent().getAction());
		Log.d(THIS_FILE, getIntent().getDataString());
		*/
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			accountToCallTo = extras.getInt(Account.FIELD_ACC_ID);
		}
		
		Log.d(THIS_FILE, "Choose to call : " + number);
		
		

		// Build minimal activity window
		w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);

		
		super.onCreate(savedInstanceState);

		// Start service and bind it. Finish selector in onServiceConnected
		Intent sipService = new Intent(this, SipService.class);
		startService(sipService);
		bindService(sipService, connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(SipService.ACTION_SIP_REGISTRATION_CHANGED));
		
		
	}
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (service != null) {
			unbindService(connection);
			unregisterReceiver(regStateReceiver);
		}
	}

	/**
	 * Place a PSTN call
	 */
	private void placePstnCall() {
		String phoneNumber = number;
		OutgoingCall.ignoreNext = phoneNumber;
		Intent intentMakePstnCall = new Intent(Intent.ACTION_CALL);
		intentMakePstnCall.setData(Uri.fromParts("tel", phoneNumber, null));
		startActivity(intentMakePstnCall);
		finish();
	}

	/**
	 * Flush and re-populate static list of account (static because should not exceed 3 or 4 accounts)
	 */
    private synchronized void updateList() {
    //	Log.d(THIS_FILE, "We are updating the list");
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
    	if(checkIfMustAccountNotValid()) {
    		//We need to do nothing else
    		return;
    	}
    	
    	database.open();
		accountsList = database.getListAccounts(true/*, service*/);
		database.close();
		
		//Exclude filtered accounts - TODO : move to db?
		List<Account> excludedAccounts = new ArrayList<Account>();
		String phoneNumber = number;
		for(Account acc : accountsList) {
			if(! acc.isCallableNumber(phoneNumber, database) ) {
				excludedAccounts.add(acc);
			}
		}
		for(Account acc : excludedAccounts) {
			accountsList.remove(acc);
		}
		
    	if(adapter == null) {
    		adapter = new AccountAdapter(this, accountsList, phoneNumber, database);
    		adapter.setNotifyOnChange(false);
    		setListAdapter(adapter);
    		if(service != null) {
    			adapter.updateService(service);
    		}
    	}else {
    		adapter.clear();
    		for(Account acc : accountsList) {
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index " + position + " id " + id);

		Account account = adapter.getItem(position);
		if (service != null) {
			AccountInfo accountInfo;
			try {
				accountInfo = service.getAccountInfo(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if (accountInfo.getPjsuaId() >= 0 && accountInfo.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
					try {
						String phoneNumber = number;
						String toCall = account.rewritePhoneNumber(phoneNumber, database);
						
						service.makeCall("sip:"+toCall, account.id);
						finish();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Unable to make the call", e);
					}
				}
			}
			//TODO : toast for elses
		}
	}
	
	private boolean checkIfMustAccountNotValid() {
		
		if (service != null && accountToCallTo != null) {

	    	database.open();
			Account account = database.getAccount(accountToCallTo);
			database.close();
			if(account == null) {
				return false;
			}
			AccountInfo accountInfo;
			try {
				accountInfo = service.getAccountInfo(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if (accountInfo.getPjsuaId() >= 0 && accountInfo.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
					try {
						String phoneNumber = number;
						String toCall = account.rewritePhoneNumber(phoneNumber, database);
						
						service.makeCall("sip:"+toCall, account.id);
						finish();
						return true;
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Unable to make the call", e);
					}
				}
			}
		}
		return false;
	}
	


}
