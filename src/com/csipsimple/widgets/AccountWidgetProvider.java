/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.view.View;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.wizards.WizardUtils;

public class AccountWidgetProvider extends AppWidgetProvider {

	private static final String THIS_FILE = "Widget provider";
	
    static ComponentName THIS_APPWIDGET = null;

	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each requested appWidgetId
        for (int widgetId : appWidgetIds) {
        	RemoteViews view = buildUpdate(context, widgetId);
            appWidgetManager.updateAppWidget(widgetId, view);
        }
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int widgetId : appWidgetIds) {
			AccountWidgetConfigure.deleteWidget(context, widgetId);
		}
		super.onDeleted(context, appWidgetIds);
	}
	
	 /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        String act = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(act)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else if (SipManager.ACTION_SIP_REGISTRATION_CHANGED.equals(act) || SipManager.ACTION_SIP_ACCOUNT_CHANGED.equals(act)) {
				//Thread t = new Thread() {
				//	public void run() {
						updateWidget(context);
				//	};
				//};
				//t.start();
		}
		super.onReceive(context, intent);
		
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if(THIS_APPWIDGET == null) {
            String pkg = PreferencesProviderWrapper.getCurrentPackageInfos(context).applicationInfo.packageName;
            THIS_APPWIDGET = new ComponentName(pkg,
                            "com.csipsimple.widgets.AccountWidgetProvider");
        }
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
		if(accId != SipProfile.INVALID_ID) {
		    ContentResolver cr = context.getContentResolver();
			Cursor c = cr.query(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, accId), 
					new String[] {
				SipProfile.FIELD_WIZARD,
				SipProfile.FIELD_ACTIVE,
				SipProfile.FIELD_ID,
				SipProfile.FIELD_DISPLAY_NAME
			}, null, null, null);
			
			if(c != null) {
				try {
					if(c.getCount() > 0) {
						c.moveToFirst();
						ContentValues acc = new ContentValues();
						DatabaseUtils.cursorRowToContentValues(c, acc);
						
						views.setImageViewResource(R.id.img_account, WizardUtils.getWizardIconRes(acc.getAsString(SipProfile.FIELD_WIZARD)));
						boolean active = (acc.getAsInteger(SipProfile.FIELD_ACTIVE) == 1);
						
						views.setImageViewResource(R.id.ind_account, active ? R.drawable.appwidget_settings_ind_on : R.drawable.appwidget_settings_ind_off);
						
						
						views.setTextViewText(R.id.txt_account, acc.getAsString(SipProfile.FIELD_DISPLAY_NAME));
						views.setOnClickPendingIntent(R.id.btn_account, getLaunchPendingIntent(context, accId, !active));
						
						// In case of active account, we have to check status of the account
                        boolean showStatus = false;
						if(active) {
						    AccountStatusDisplay accountStatusDisplay = AccountListUtils.getAccountDisplay(context, accId);
						    Integer drawable = null;
						    
						    if(accountStatusDisplay.checkBoxIndicator == R.drawable.ic_indicator_red) {
						        drawable = R.drawable.appwidget_settings_ind_red;
						        showStatus = true;
						    }else if (accountStatusDisplay.checkBoxIndicator == R.drawable.ic_indicator_yellow) {
						        drawable = R.drawable.appwidget_settings_ind_yellow;
						    }
						    if(showStatus) {
						        views.setTextViewText(R.id.txt_status, accountStatusDisplay.statusLabel);
						    }
						    if(drawable != null) {
						        views.setImageViewResource(R.id.ind_account, drawable);
						    }
				            
						}
                        views.setViewVisibility(R.id.txt_status, showStatus ? View.VISIBLE : View.GONE);
                        
					}
				}catch(Exception e) {
					Log.e(THIS_FILE, "Something went wrong while retrieving the account", e);
				} finally {
					c.close();
				}
			}
			
			
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
        Intent launchIntent = new Intent(SipManager.INTENT_SIP_ACCOUNT_ACTIVATE);
        launchIntent.putExtra(SipProfile.FIELD_ID, accId);
        launchIntent.putExtra(SipProfile.FIELD_ACTIVE, activate);
        Log.d(THIS_FILE, "Create intent "+activate);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int)accId,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
	
}
