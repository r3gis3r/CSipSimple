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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsChooserListActivity;
import com.csipsimple.utils.Log;

public class AccountWidgetConfigure extends AccountsChooserListActivity {

	private static final String WIDGET_PREFS = "widget_prefs";
	private static final String THIS_FILE = "Widget config";
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
		    appWidgetId = extras.getInt(
		            AppWidgetManager.EXTRA_APPWIDGET_ID, 
		            AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		
		//Result to cancel in case application is quit by user
		Intent cancelResultValue = new Intent();
        cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId);
        setResult(RESULT_CANCELED, cancelResultValue);
		
	}
	

	private static String getPrefsKey(int widgetId) {
		return "widget" + widgetId + "_account";
	}
	
	public static long getAccountForWidget(Context ctx, int widgetId) {
		SharedPreferences prefs = ctx.getSharedPreferences(WIDGET_PREFS, 0);
        return prefs.getLong(getPrefsKey(widgetId), SipProfile.INVALID_ID);
        
	}
	
	public static void deleteWidget(Context ctx, int widgetId) {
		SharedPreferences prefs = ctx.getSharedPreferences(WIDGET_PREFS, 0);
        SharedPreferences.Editor edit = prefs.edit();
		edit.remove(getPrefsKey(widgetId));
		edit.commit();
	}


    @Override
    public void onAccountClicked(long accountId, String d, String w) {

        if(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            SharedPreferences prefs = getSharedPreferences(WIDGET_PREFS, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putLong(getPrefsKey(appWidgetId), accountId);
            edit.commit();
            
            
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId);
            setResult(RESULT_OK, resultValue);
            
            AccountWidgetProvider.updateWidget(this);
            
            finish();
        }else {
            Log.w(THIS_FILE, "Invalid widget ID here...");
        }
    }
	
}
