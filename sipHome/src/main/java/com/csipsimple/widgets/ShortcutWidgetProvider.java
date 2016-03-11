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
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.ShortcutWidgetConfigure.Shortcut;

public class ShortcutWidgetProvider extends AppWidgetProvider {

	
    private static final String THIS_FILE = "ShortcutWidgetProvider";
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
			ShortcutWidgetConfigure.deleteWidget(context, widgetId);
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
            THIS_APPWIDGET = new ComponentName(context, ShortcutWidgetProvider.class);
        }
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        for (int widgetId : appWidgetIds) {
        	RemoteViews view = buildUpdate(context, widgetId);
            appWidgetManager.updateAppWidget(widgetId, view);
        }
    }
	
    
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_shortcut);
        
        
        int action = ShortcutWidgetConfigure.getActionForWidget(context, appWidgetId);
        if(action >= 0 && action < ShortcutWidgetConfigure.SHORTCUTS.length) {
            Shortcut sh = ShortcutWidgetConfigure.SHORTCUTS[action];
    		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, sh.intent, 0);
            views.setOnClickPendingIntent(R.id.btn_shortcut, pendingIntent);
            views.setImageViewResource(R.id.img_account, sh.iconRes);
            views.setTextViewText(R.id.txt_account, context.getText(sh.nameRes));
        }else {
            Log.w(THIS_FILE, "Invalid action id " + action);
        }
        return views;
    }
    
	
}
