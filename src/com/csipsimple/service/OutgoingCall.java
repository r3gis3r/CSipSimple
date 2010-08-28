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
package com.csipsimple.service;

import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class OutgoingCall extends BroadcastReceiver {

	private static final String THIS_FILE = "Outgoing RCV";
	private Context context;
	private PreferencesWrapper prefsWrapper;
	
	public static String ignoreNext = "";

	@Override
	public void onReceive(Context aContext, Intent intent) {
		context = aContext;
		prefsWrapper = new PreferencesWrapper(context);
		String action = intent.getAction();
		String number = getResultData();
		String full_number = intent.getStringExtra("android.phone.extra.ORIGINAL_URI");

		Log.d(THIS_FILE, "act=" + action + " num=" + number + " fnum=" + full_number + " ignx=" + ignoreNext);	
		
		if (number == null) return;
		
		//Log.d(THIS_FILE, "We are trying to call " + full_number);
		if (!prefsWrapper.useIntegrateDialer() || ignoreNext.equalsIgnoreCase(number)) {
			Log.d(THIS_FILE, "Our selector disabled, or Mobile chosen in our selector, send to tel");
			//Log.d(THIS_FILE, "we will force it ");
			ignoreNext = "";
			setResultData(number);
			return;
		}
		
		// If this is an outgoing call with a valid number
		if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {					// Filter should assure this!
			//Log.d(THIS_FILE, "This is a work for super outgoing call handler....");
			DBAdapter db = new DBAdapter(context);
			db.open();
			List<Account> accounts = db.getListAccounts(true);
			db.close();
			
			if (isCallableNumber(number, accounts, db) && prefsWrapper.isValidConnectionForOutgoing()) {
				Log.d(THIS_FILE, "Number OK for SIP, have live connection, show the call selector");
				// Launch activity to choose what to do with this call
				Intent outgoingCallChooserIntent = new Intent(Intent.ACTION_CALL);
				outgoingCallChooserIntent.setData(Uri.fromParts("sip", number, null));
				outgoingCallChooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				
				Account mustCallAccount = isMustCallableNumber(number, accounts, db);
				if(mustCallAccount != null) {
					outgoingCallChooserIntent.putExtra(Account.FIELD_ACC_ID, mustCallAccount.id);
				}
				context.startActivity(outgoingCallChooserIntent);
				
				// We will treat this by ourselves
				setResultData(null);
				return;
			} else {
				Log.d(THIS_FILE, "Can't use SIP, pass number along");
				// Pass the call to pstn handle
				setResultData(number);
				return;
			}
		}
	}

	/**
	 * Check whether a number can be call using sip
	 * Should check if not matches preferences of excluded patterns
	 * @param number the number to test
	 * @param accounts 
	 * @return true if we should handle this number using SIP
	 */
	private boolean isCallableNumber(String number, List<Account> accounts, DBAdapter db  ) {
		boolean canCall = false;
		
		for(Account account : accounts) {
			Log.d(THIS_FILE, "Checking if number valid for account "+account.display_name);
			if(account.isCallableNumber(number, db)) {
				Log.d(THIS_FILE, ">> Response is YES");
				return true;
			}
		}
		return canCall;
	}
	
	private Account isMustCallableNumber(String number, List<Account> accounts, DBAdapter db ) {
		for(Account account : accounts) {
			Log.d(THIS_FILE, "Checking if number must be call for account "+account.display_name);
			if(account.isMustCallNumber(number, db)) {
				Log.d(THIS_FILE, ">> Response is YES");
				return account;
			}
		}
		return null;
	}

}
