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

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.csipsimple.R;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.CallInfo;
import com.csipsimple.widgets.RegistrationNotification;

public class SipNotifications {

	private NotificationManager notificationManager;
	private RegistrationNotification contentView;
	private Notification inCallNotification;
	private Context context;

	public static final int REGISTER_NOTIF_ID = 1;
	public static final int CALL_NOTIF_ID = REGISTER_NOTIF_ID + 1;
	
	public SipNotifications(Context aContext) {
		context = aContext;
		notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	//Announces

	//Register
	public void notifyRegisteredAccounts(ArrayList<AccountInfo> activeAccountsInfos) {
		int icon = R.drawable.sipok;
		CharSequence tickerText = context.getString(R.string.service_ticker_registered_text);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Intent notificationIntent = new Intent(SipService.ACTION_SIP_DIALER);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (contentView == null) {
			contentView = new RegistrationNotification(context.getPackageName());
		}
		contentView.clearRegistrations();
		contentView.addAccountInfos(context, activeAccountsInfos);

		// notification.setLatestEventInfo(context, contentTitle,
		// contentText, contentIntent);
		notification.contentIntent = contentIntent;
		notification.contentView = contentView;
		notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		// notification.flags = Notification.FLAG_FOREGROUND_SERVICE;

		notificationManager.notify(REGISTER_NOTIF_ID, notification);
	}

	// Calls
	public void showNotificationForCall(CallInfo currentCallInfo2) {
		// This is the pending call notification
		//int icon = R.drawable.ic_incall_ongoing;
		int icon = android.R.drawable.stat_sys_phone_call;
		CharSequence tickerText =  context.getText(R.string.ongoing_call);
		long when = System.currentTimeMillis();
		
		if(inCallNotification == null) {
			inCallNotification = new Notification(icon, tickerText, when);
			inCallNotification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
			// notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
		}

		Intent notificationIntent = new Intent(SipService.ACTION_SIP_CALL_UI);
		notificationIntent.putExtra("call_info", currentCallInfo2);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		
		inCallNotification.setLatestEventInfo(context, context.getText(R.string.ongoing_call) /*+" / "+currentCallInfo2.getCallId()*/, 
				currentCallInfo2.getRemoteContact(), contentIntent);

		notificationManager.notify(CALL_NOTIF_ID, inCallNotification);
	}
	
	public void showNotificationForMissedCall() {
		int icon = android.R.drawable.stat_notify_missed_call;
		CharSequence tickerText =  context.getText(R.string.missed_call);
		long when = System.currentTimeMillis();
		
		
	}
	
	// Cancels
	public void cancelRegisters() {
		notificationManager.cancel(REGISTER_NOTIF_ID);
	}
	
	public void cancelCalls() {
		notificationManager.cancel(CALL_NOTIF_ID);
	}
	
	public void cancelAll() {
		notificationManager.cancelAll();
	}
}
