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
package com.csipsimple.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

public class AccountWidgetProvider extends AppWidgetProvider {

	private static final String THIS_FILE = "Widget provider";
	
    static final ComponentName THIS_APPWIDGET =
        new ComponentName("com.csipsimple",
                "com.csipsimple.widgets.AccountWidgetProvider");

	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each requested appWidgetId
        for (int widgetId : appWidgetIds) {
        	RemoteViews view = buildUpdate(context, widgetId);
            appWidgetManager.updateAppWidget(widgetId, view);
        }
	}
	
	
	 /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(SipService.ACTION_SIP_REGISTRATION_CHANGED.equals(intent.getAction()) ||
        		SipService.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED.equals(intent.getAction())	) {
        	updateWidget(context);
        }
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        for (int widgetId : appWidgetIds) {
        	RemoteViews view = buildUpdate(context, widgetId);
            appWidgetManager.updateAppWidget(widgetId, view);
        }
    }
	
    
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        
        
        long accId = AccountWidgetConfigure.getAccountForWidget(context, appWidgetId);
		Log.d(THIS_FILE, "Updating wiget " + appWidgetId + " for account " + accId);
		if(accId != -1) {
			
			
			DBAdapter db = new DBAdapter(context);
			db.open();
			ContentValues acc = db.getAccountValues(accId);
		//	Log.d(THIS_FILE, "Found for " + accId + " : " + acc);
			if (acc != null) {
				views.setImageViewResource(R.id.img_account, WizardUtils.getWizardIconRes(acc.getAsString(Account.FIELD_WIZARD)));
				boolean active = (acc.getAsInteger(Account.FIELD_ACTIVE) == 1);
				if(active) {
				//	accountStatusDisplay = AccountListUtils.getAccountDisplay(context, service, account.id);
					views.setImageViewResource(R.id.ind_account, R.drawable.appwidget_settings_ind_on);
				}else {
					views.setImageViewResource(R.id.ind_account, R.drawable.appwidget_settings_ind_off);
				}
				views.setTextViewText(R.id.txt_account, acc.getAsString(Account.FIELD_DISPLAY_NAME));
				views.setOnClickPendingIntent(R.id.btn_account, getLaunchPendingIntent(context, accId, !active));
			}
			
			db.close();
		}
        
        return views;
    }
    
	
    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @param accId
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context, long accId, boolean activate ) {
        Intent launchIntent = new Intent(SipService.INTENT_SIP_ACCOUNT_ACTIVATE);
        launchIntent.putExtra(SipService.EXTRA_ACCOUNT_ID, accId);
        launchIntent.putExtra(SipService.EXTRA_ACTIVATE, activate);
        Log.d(THIS_FILE, "Create intent "+activate);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int)accId,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
	
}
