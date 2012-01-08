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
import java.util.Map;

import android.app.ListActivity;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.db.AccountAdapter;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.CallHandler;
import com.csipsimple.utils.CallHandler.onLoadListener;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;

public class OutgoingCallChooser extends ListActivity {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	String number;
	Window w;
	
	public final static int AUTO_CHOOSE_TIME = 8000;
	private List<SipProfile> accountsList;

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
			checkNumberWithSipStarted();

			//This need to be done after setContentView call
			w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
					android.R.drawable.ic_menu_call);
			
			String phoneNumber = number;
			setTitle(getString(R.string.outgoing_call_chooser_call_text) + " " + phoneNumber);

		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			service = null;
		}
    };
    
   	private BroadcastReceiver regStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			updateList();
		}
	};
	
	private Long accountToCallTo = null;
	private PreferencesProviderWrapper prefsWrapper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "Starting ");

		super.onCreate(savedInstanceState);
		
		prefsWrapper = new PreferencesProviderWrapper(this);
		
		// First step is to retrieve the number that was asked to us.
		number = PhoneNumberUtils.getNumberFromIntent(getIntent(), this);
		if(number == null) {
			String action = getIntent().getAction();
			if( action != null) {
				if( action.equalsIgnoreCase(Intent.ACTION_CALL)) {
					number = getIntent().getData().getSchemeSpecificPart();
				}
				if( action.equalsIgnoreCase(Intent.ACTION_SENDTO)){
					Uri data =  getIntent().getData();
					if(data.getScheme().equalsIgnoreCase("imto")) {
						String auth = data.getAuthority();
						if( "skype".equalsIgnoreCase(auth) ||
							"sip".equalsIgnoreCase(auth) ) {
							String sipUser = data.getLastPathSegment();
							Log.d(THIS_FILE, ">> Found skype account "+sipUser);
							number = "sip:"+sipUser;
						}
					}
				}
			}
		}
		
		// Then we get if we are trying to force an account to use for this call
		long shouldCallId = getIntent().getLongExtra(SipProfile.FIELD_ACC_ID, SipProfile.INVALID_ID);
		if(shouldCallId != SipProfile.INVALID_ID) {
			accountToCallTo = shouldCallId;
		}
		
		// Sanity check
		if(number == null) {
			Log.e(THIS_FILE, "No number detected for : "+getIntent().getAction());
			finish();
			return;
		}
		
		Log.d(THIS_FILE, "Choose to call : " + number);
		
		
		// Build minimal activity window
		w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);

		
	    // Connect to database
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
    	
		// Need full selector, finish layout
		setContentView(R.layout.outgoing_account_list);
		

		
		// Start service and bind it. Finish selector in onServiceConnected
		if(prefsWrapper.isValidConnectionForOutgoing()) {
			Intent sipService = new Intent(this, SipService.class);
			startService(sipService);
			bindService(sipService, connection, Context.BIND_AUTO_CREATE);
			registerReceiver(regStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		}
		
		
		addExternalRows();
	}
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(connection);
		}catch(Exception e) {}
		
		try {
			unregisterReceiver(regStateReceiver);
		}catch(Exception e) {}
	}

	private void addRow(CharSequence label, Drawable dr, OnClickListener l) {
		Log.d(THIS_FILE, "Append ROW "+label);
		// Get attr
		
		TypedArray a = obtainStyledAttributes(android.R.style.Theme, new int[]{android.R.attr.listPreferredItemHeight});
		int sListItemHeight = a.getDimensionPixelSize(0, 0);
		a.recycle();
		
		// Add line
		LinearLayout root = (LinearLayout) findViewById(R.id.acc_list_chooser_wrapper);
		
		ImageView separator = new ImageView(this);
		separator.setImageResource(R.drawable.divider_horizontal_dark);
		separator.setScaleType(ScaleType.FIT_XY);
		root.addView(separator, new LayoutParams(LayoutParams.FILL_PARENT, 1));
		
		LinearLayout line = new LinearLayout(this);
		line.setFocusable(true);
		line.setClickable(true);
		line.setOrientation(LinearLayout.HORIZONTAL);
		line.setGravity(Gravity.CENTER_VERTICAL);
		line.setBackgroundResource(android.R.drawable.menuitem_background);
		
		ImageView icon = new ImageView(this);
		icon.setImageDrawable(dr);
		icon.setScaleType(ScaleType.FIT_XY);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(48, 48);
		lp.setMargins(6, 6, 6, 6);
		line.addView(icon, lp);
		
		TextView tv = new TextView(this);
		tv.setText(label);
		tv.setTextAppearance(this, android.R.style.TextAppearance_Medium);
		tv.setTypeface(Typeface.DEFAULT_BOLD);
		line.addView(tv, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		line.setOnClickListener(l);
		
		root.addView(line, new LayoutParams(LayoutParams.FILL_PARENT,  sListItemHeight));
		
		
	}
	
	private int externalTotalNbrs = 0;
	private int loadedExternals = -1; // ensure not equal to total nbrs at first time
	private List<SipProfile> externalProfiles;
	
	/**
	 * Add rows for external plugins
	 */
	private void addExternalRows() {

		Map<String, String> callHandlers = CallHandler.getAvailableCallHandlers(this);
		externalTotalNbrs = callHandlers.size();
		loadedExternals = 0;
		externalProfiles = new ArrayList<SipProfile>();
		
		for(String packageName : callHandlers.keySet()) {
			Log.d(THIS_FILE, "Treating call handler... "+packageName);
			SipProfile externalProfile = new SipProfile();
			externalProfile.id = CallHandler.getAccountIdForCallHandler(this, packageName);
			
			if(Filter.isCallableNumber(externalProfile, number, database)) {
				// Transform number
				String finalNumber = Filter.rewritePhoneNumber(externalProfile, number, database);
				final SipProfile extProfile = externalProfile;
				Log.d(THIS_FILE, "Will loaded external " + packageName);
				CallHandler ch = new CallHandler(this);
				ch.loadFrom(packageName, finalNumber, new onLoadListener() {
					@Override
					public void onLoad(final CallHandler ch) {
						Log.d(THIS_FILE, "Loaded external " + ch.getIntent());
						if(ch.getIntent() != null) {
							addRow(ch.getLabel(), ch.getIconDrawable(), new OnClickListener() {
								@Override
								public void onClick(View v) {
									placePluginCall(ch);
								}
							});
							
							externalProfiles.add(extProfile);
						}
						loadedExternals ++;
						checkNumberWithSipStarted();
					}
				});
			}
		}
	}
	
	
	private void placePluginCall(CallHandler ch) {
		try {
			String nextExclude = ch.getNextExcludeTelNumber();
			if(nextExclude != null) {
				OutgoingCall.ignoreNext = nextExclude;
			}
			ch.getIntent().send();
			finishServiceIfNeeded();
			finish();
		} catch (CanceledException e) {
			Log.e(THIS_FILE, "Pending intent cancelled", e);
		}
	}
	
	
	private void checkNumberWithSipStarted() {
		// First thing to do check anyway if the one passed or already set is not valid now
		if(accountToCallTo != null) {
			checkIfMustAccountNotValid();
		}
		
		// If valid for outgoing we have to wait for service to be there
		if(prefsWrapper.isValidConnectionForOutgoing()) {
			if(service == null) {
				return;
			}
		}
		// We have to wait for all external profiles to be there
		if(loadedExternals != externalTotalNbrs) {
			return;
		}
		
		
		// Get all accounts
		List<SipProfile> accounts = new ArrayList<SipProfile>();
		// Get SIP accounts
		if(prefsWrapper.isValidConnectionForOutgoing()) {
			accounts = SipProfile.getAllProfiles(this, true);
		}
		// Add CallHandlers accounts
		for(SipProfile externalProfile : externalProfiles) {
			accounts.add(externalProfile);
		}
		
		
		SipProfile onlyAccount = null;
		int nbrOfAccounts = 0;
		// Walk all accounts (SIP + CallHandlers)
		for(SipProfile account : accounts) {
			Log.d(THIS_FILE, "Checking account "+account.id);
			if(Filter.isCallableNumber(account, number, database)) {
				Log.d(THIS_FILE, "Can call");
				if(Filter.isMustCallNumber(account, number, database)) {
					Log.d(THIS_FILE, "Must call using it");
					// Simulate that's the only one
					onlyAccount = account;
					nbrOfAccounts = 1;
					break;
				}
				onlyAccount = account;
				nbrOfAccounts ++;
				
			}
		}
		
		if (nbrOfAccounts == 1) {
			accountToCallTo = onlyAccount.id;
			checkIfMustAccountNotValid();
		}else if(nbrOfAccounts == 0) {
			// TODO : here we have no account configured to be able to manage that... 
			// We should toast user about the fact he explicitely disabled that !
		}
		
		
	}

	
	/**
	 * Flush and re-populate static list of account (static because should not exceed 3 or 4 accounts)
	 */
    private synchronized void updateList() {

    	if(checkIfMustAccountNotValid()) {
    		//We need to do nothing else
    		return;
    	}
    	
		accountsList = SipProfile.getAllProfiles(this, true);
		
		//Exclude filtered accounts
		List<SipProfile> excludedAccounts = new ArrayList<SipProfile>();
		String phoneNumber = number;
		for(SipProfile acc : accountsList) {
			if(! Filter.isCallableNumber(acc, phoneNumber, database) ) {
				excludedAccounts.add(acc);
			}
		}
		for(SipProfile acc : excludedAccounts) {
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
    		for(SipProfile acc : accountsList) {
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index " + position + " id " + id);

		SipProfile account = adapter.getItem(position);
		if (service != null) {
			SipProfileState accountInfo;
			try {
				accountInfo = service.getSipProfileState((int) account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isValidForCall()) {
				try {
					String phoneNumber = number;
					String toCall = Filter.rewritePhoneNumber(account, phoneNumber, database);

					service.makeCall("sip:" + toCall, (int) account.id);
					finish();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Unable to make the call", e);
				}
			}
			//TODO : toast for elses
		}
	}
	
	private boolean checkIfMustAccountNotValid() {
		// Check for plugins callhandlers
		if(accountToCallTo != null && accountToCallTo < SipProfile.INVALID_ID) {
			// We have a external handler as force call account
			String phoneNumber = number;
			SipProfile externalAccount = new SipProfile();
			externalAccount.id = accountToCallTo;
			String toCall = Filter.rewritePhoneNumber(externalAccount, phoneNumber, database);
			CallHandler ch = new CallHandler(this);
			ch.loadFrom(externalAccount.id, toCall, new onLoadListener() {
				@Override
				public void onLoad(final CallHandler ch) {
					Log.d(THIS_FILE, "Place external call " + ch.getIntent());
					if(ch.getIntent() != null) {
						placePluginCall(ch);
					}
				}
			});
		}
		
		// Check for sip services accounts (>0)
		if (service != null && accountToCallTo != null && accountToCallTo > 0) {
			
	    	SipProfile account = SipProfile.getProfileFromDbId(this, accountToCallTo, DBProvider.ACCOUNT_FULL_PROJECTION);
			if(account == null) {
				return false;
			}
			SipProfileState accountInfo;
			try {
				accountInfo = service.getSipProfileState( (int) account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if ( (accountInfo.getPjsuaId() >= 0 && accountInfo.getStatusCode() == SipCallSession.StatusCode.OK) ||
						TextUtils.isEmpty( accountInfo.getRegUri() ) ) {
					try {
						String phoneNumber = number;
						String toCall = Filter.rewritePhoneNumber(account, phoneNumber, database);
						
						service.makeCall("sip:" + toCall, (int) account.id);
						accountToCallTo = null;
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
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK 
	    		&& event.getRepeatCount() == 0
	    		&& !Compatibility.isCompatible(5)) {
	    	onBackPressed();
	    	
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public void onBackPressed() {
		finishServiceIfNeeded();
		finish();
	}
	
	private void finishServiceIfNeeded() {
		if(service != null) {
			if( ! prefsWrapper.isValidConnectionForIncoming()) {
				try {
					service.forceStopService();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Unable to stop service", e);
				}
			}
		}
	}

}
