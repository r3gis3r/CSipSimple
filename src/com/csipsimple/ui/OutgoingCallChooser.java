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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
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
import com.csipsimple.models.Filter;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

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
			
			/*
			 * Disabled since we don't want to force user to do a pstn call if sip should have been applied.
			if (adapter.isEmpty()) {
				Log.d(THIS_FILE, "No usable accounts for SIP, skip the chooser, -> PSTN call");
				placePstnCall();
				return;
			}
			*/
			
			checkNumberWithSipStarted();

			
			//This need to be done after setContentView call
			w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
					android.R.drawable.ic_menu_call);
			//TODO : internationalisation should be %s form
			String phoneNumber = number;
			setTitle(getString(R.string.outgoing_call_chooser_call_text) + " " + phoneNumber);

			// Inform the list we provide context menus for items
			//	getListView().setOnCreateContextMenuListener(this);
			

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

		super.onCreate(savedInstanceState);
		
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
		int shouldCallId = getIntent().getIntExtra(SipProfile.FIELD_ACC_ID, SipProfile.INVALID_ID);
		if(shouldCallId != SipProfile.INVALID_ID) {
			accountToCallTo = shouldCallId;
		}
		
		/*else {
			Log.e(THIS_FILE, "This action : "+getIntent().getAction()+" is not supported by this view");
			return;
		}
		*/
		
		if(number == null) {
			Log.e(THIS_FILE, "No number detected for : "+getIntent().getAction());
			finish();
			return;
		}
		
		Log.d(THIS_FILE, "Choose to call : " + number);
		
		

		// Build minimal activity window
		w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);

		
	    //	Log.d(THIS_FILE, "We are updating the list");
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
		// Need full selector, finish layout
		setContentView(R.layout.outgoing_account_list);

		// Start service and bind it. Finish selector in onServiceConnected
		Intent sipService = new Intent(this, SipService.class);
		bindService(sipService, connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		
		
		bindAddedRows();
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
		Log.d(THIS_FILE, "Append ROW "+label+ " et ");
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
	
	
	
	private void bindAddedRows() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> callers = Compatibility.getIntentsForCall(this);
		
		int index = 1; 
		for(final ResolveInfo caller : callers) {
			// Add row if possible
			// Exclude GSM
			SipProfile gsmProfile = new SipProfile();
			gsmProfile.id = SipProfile.INVALID_ID - index;
			
			if(Filter.isCallableNumber(gsmProfile, number, database)) {
				Log.d(THIS_FILE, caller.resolvePackageName+" : "+caller.activityInfo.name);
				final SipProfile acc = gsmProfile;
				addRow(caller.loadLabel(pm), caller.loadIcon(pm), new OnClickListener() {
					@Override
					public void onClick(View v) {
						placeInternalCall(acc, caller);
					}
				});
			}
			index ++;
		}
	}
	
	
	private void placeInternalCall(SipProfile acc, ResolveInfo caller) {
		
		Intent i = null;
		String phoneNumber = Filter.rewritePhoneNumber(acc, number, database);
		//Case default dialer
		if(caller.activityInfo.packageName.startsWith("com.android")) {
			i = new Intent(Intent.ACTION_CALL);
			i.setData(Uri.fromParts("tel", phoneNumber, null));
			
			//This will call again the outdial
			OutgoingCall.ignoreNext = number;
		}else if(caller.activityInfo.packageName.startsWith("com.skype")) {
			i = new Intent();
			i.setComponent(new ComponentName(caller.activityInfo.packageName, "com.skype.raider.contactsync.ContactSkypeOutCallStartActivity"));
			i.setData(Uri.fromParts("tel", phoneNumber, null));
		}
		
		if(i != null) {
			startActivity(i);
			finishServiceIfNeeded();
			finish();
		}else {

	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle(R.string.warning)
	            .setIcon(android.R.drawable.ic_dialog_alert)
	        .setNeutralButton(R.string.ok, null)
	        .setMessage("This application is not yet supported ("+caller.activityInfo.packageName+") :\nIt is possible that this application doesn't provide a way for other applications to do it! If so CRY on the project website")
	        .show();
		}
	}
	
	private void checkNumberWithSipStarted() {
		
		
		database.open();
		List<SipProfile> accounts = database.getListAccounts(true);
		database.close();
		
		if(accountToCallTo != null) {
			checkIfMustAccountNotValid();
		}
		
		//DB SIP
		if (isCallableNumber(number, accounts, database)) {
			Log.d(THIS_FILE, "Number OK for SIP, have live connection, show the call selector");
			
			SipProfile mustCallAccount = isMustCallableNumber(number, accounts, database);
			if(mustCallAccount != null) {
				accountToCallTo = mustCallAccount.id;
				checkIfMustAccountNotValid();
			}
		}
		//Internal intents
		List<ResolveInfo> callers = Compatibility.getIntentsForCall(this);
		SipProfile mustAcc = null;
		ResolveInfo resInfo = null;
		int index = 1;
		for(ResolveInfo caller : callers) {
			SipProfile acc = new SipProfile();
			acc.id = SipProfile.INVALID_ID - index;
			if(Filter.isMustCallNumber(acc, number, database)) {
				resInfo = caller;
				mustAcc = acc;
				break;
			}
			index ++;
		}
		
		if(mustAcc != null && resInfo != null) {
			placeInternalCall(mustAcc, resInfo);
		}
		
		
	}

	/**
	 * Check whether a number can be call using sip
	 * Should check if not matches preferences of excluded patterns
	 * @param number the number to test
	 * @param accounts 
	 * @return true if we should handle this number using SIP
	 */
	private boolean isCallableNumber(String number, List<SipProfile> accounts, DBAdapter db  ) {
		boolean canCall = false;
		
		for(SipProfile account : accounts) {
			Log.d(THIS_FILE, "Checking if number valid for account "+account.display_name);
			if(Filter.isCallableNumber(account, number, db)) {
				Log.d(THIS_FILE, ">> Response is YES");
				return true;
			}
		}
		return canCall;
	}
	
	private SipProfile isMustCallableNumber(String number, List<SipProfile> accounts, DBAdapter db ) {
		for(SipProfile account : accounts) {
			Log.d(THIS_FILE, "Checking if number must be call for account "+account.display_name);
			if(Filter.isMustCallNumber(account, number, db)) {
				Log.d(THIS_FILE, ">> Response is YES");
				return account;
			}
		}
		return null;
	}

	
	/**
	 * Flush and re-populate static list of account (static because should not exceed 3 or 4 accounts)
	 */
    private synchronized void updateList() {

    	
    	if(checkIfMustAccountNotValid()) {
    		//We need to do nothing else
    		return;
    	}
    	
    	database.open();
		accountsList = database.getListAccounts(true/*, service*/);
		database.close();
		
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
				accountInfo = service.getSipProfileState(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isValidForCall()) {
				try {
					String phoneNumber = number;
					String toCall = Filter.rewritePhoneNumber(account, phoneNumber, database);
					
					service.makeCall("sip:"+toCall, account.id);
					finish();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Unable to make the call", e);
				}
			}
			//TODO : toast for elses
		}
	}
	
	private boolean checkIfMustAccountNotValid() {
		
		if (service != null && accountToCallTo != null) {
			
			
	    	database.open();
	    	SipProfile account = database.getAccount(accountToCallTo);
			database.close();
			if(account == null) {
				return false;
			}
			SipProfileState accountInfo;
			try {
				accountInfo = service.getSipProfileState(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if ( (accountInfo.getPjsuaId() >= 0 && accountInfo.getStatusCode() == SipCallSession.StatusCode.OK) ||
						accountInfo.getWizard().equalsIgnoreCase("LOCAL") ) {
					try {
						String phoneNumber = number;
						String toCall = Filter.rewritePhoneNumber(account, phoneNumber, database);
						
						service.makeCall("sip:"+toCall, account.id);
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
			PreferencesWrapper prefsWrapper = new PreferencesWrapper(this);
		
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
