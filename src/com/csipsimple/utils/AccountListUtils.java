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
package com.csipsimple.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;


public class AccountListUtils {

	public static final class AccountStatusDisplay {
		public String statusLabel;
		public int statusColor;
		public int checkBoxIndicator;
		public boolean availableForCalls;
	}


	private static final String THIS_FILE = "AccountListUtils";
	
	
	public static AccountStatusDisplay getAccountDisplay(Context context, long accountId) {
		AccountStatusDisplay accountDisplay = new AccountStatusDisplay();
		accountDisplay.statusLabel = context.getString(R.string.acct_inactive);
		final Resources resources = context.getResources();
		accountDisplay.statusColor = resources.getColor(R.color.account_inactive); 
		accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
		accountDisplay.availableForCalls = false;
		

		SipProfileState accountInfo = null;
        Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(SipProfile.ACCOUNT_STATUS_ID_URI_BASE, accountId), 
        		null, null, null, null);
		if (c != null) {
			try {
				if(c.getCount() > 0) {
					c.moveToFirst();
					accountInfo = new SipProfileState(c);
				}
			} catch (Exception e) {
				Log.e(THIS_FILE, "Error on looping over sip profiles states", e);
			} finally {
				c.close();
			}
		}
		
		if (accountInfo != null && accountInfo.isActive()) {
			if (accountInfo.getAddedStatus() >= SipManager.SUCCESS) {

				accountDisplay.statusLabel = context.getString(R.string.acct_unregistered);
				accountDisplay.statusColor = resources.getColor(R.color.account_unregistered);
				accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
				if( TextUtils.isEmpty( accountInfo.getRegUri()) ) {
					// Green
					accountDisplay.statusColor = resources.getColor(R.color.account_valid);
					accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_on;
					accountDisplay.statusLabel = context.getString(R.string.acct_registered);
					accountDisplay.availableForCalls = true;
				}else if (accountInfo.getPjsuaId() >= 0) {
					String pjStat = accountInfo.getStatusText();	// Used only on error status message
					int statusCode = accountInfo.getStatusCode();
					if (statusCode == SipCallSession.StatusCode.OK) {
						// Log.d(THIS_FILE,
						// "Now account "+account.display_name+" has expires "+accountInfo.getExpires());
						if (accountInfo.getExpires() > 0) {
							// Green
							accountDisplay.statusColor = resources.getColor(R.color.account_valid);
							accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_on;
							accountDisplay.statusLabel = context.getString(R.string.acct_registered);
							accountDisplay.availableForCalls = true;
						} else {
							// Yellow unregistered
							accountDisplay.statusColor = resources.getColor(R.color.account_unregistered);
							accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
							accountDisplay.statusLabel = context.getString(R.string.acct_unregistered);
						}
					} else if(statusCode != -1 ){
						if (statusCode == SipCallSession.StatusCode.PROGRESS || statusCode == SipCallSession.StatusCode.TRYING) {
							// Yellow progressing ...
							accountDisplay.statusColor = resources.getColor(R.color.account_unregistered);
							accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
							accountDisplay.statusLabel = context.getString(R.string.acct_registering);
						} else {
							//TODO : treat 403 with special message
							// Red : error
							accountDisplay.statusColor = resources.getColor(R.color.account_error);
							accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_red;
							accountDisplay.statusLabel = context.getString(R.string.acct_regerror) + " - " + pjStat;	// Why can't ' - ' be in resource?
						}
					}else {
						// Account is currently registering (added to pjsua but not replies yet from pjsua registration)
						accountDisplay.statusColor = resources.getColor(R.color.account_inactive);
						accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
						accountDisplay.statusLabel = context.getString(R.string.acct_registering);
					}
				}
			} else {
				if(accountInfo.isAddedToStack()) {
					accountDisplay.statusLabel = context.getString(R.string.acct_regfailed);
					accountDisplay.statusColor = resources.getColor(R.color.account_error);
				}else {
					accountDisplay.statusColor = resources.getColor(R.color.account_inactive);
					accountDisplay.checkBoxIndicator = R.drawable.ic_indicator_yellow;
					accountDisplay.statusLabel = context.getString(R.string.acct_registering);
					
				}
			}
		}
		return accountDisplay;
	}
	
	
	
}
