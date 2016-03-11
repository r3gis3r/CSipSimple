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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockListActivity;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortcutWidgetConfigure extends SherlockListActivity {

	private static final String WIDGET_PREFS = "widget_shortcut_prefs";
    private static final String THIS_FILE = "ShortcutWidgetConfigure";
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	private static String KEY_TITLE = "title";
    private static String KEY_INDEX = "index";
	
	public static class Shortcut {
	    int nameRes;
	    int iconRes;
	    Intent intent;
	    
	    public Shortcut(int aName, int anIcon, Intent anIntent) {
            nameRes = aName;
            iconRes = anIcon;
            intent = anIntent;
        }
	}
	
	public static Shortcut[] SHORTCUTS = new Shortcut[] {
	    new Shortcut(R.string.dial_tab_name_text, R.drawable.ic_ab_dialer_holo_dark, new Intent(SipManager.ACTION_SIP_DIALER)),
	    new Shortcut(R.string.calllog_tab_name_text, R.drawable.ic_ab_history_holo_dark, new Intent(SipManager.ACTION_SIP_CALLLOG)),
	    new Shortcut(R.string.favorites_tab_name_text, R.drawable.ic_ab_favourites_holo_dark, new Intent(SipManager.ACTION_SIP_FAVORITES)),
	    new Shortcut(R.string.messages_tab_name_text, R.drawable.ic_ab_text_holo_dark, new Intent(SipManager.ACTION_SIP_MESSAGES)),
        new Shortcut(R.string.prefs_fast, R.drawable.ic_prefs_fast, new Intent(SipManager.ACTION_UI_PREFS_FAST)),
        new Shortcut(R.string.prefs, android.R.drawable.ic_menu_manage, new Intent(SipManager.ACTION_UI_PREFS_GLOBAL)),
	};
	
	
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
		
        Resources r = getResources();
        List<Map<String, ?>> datas = new ArrayList<Map<String,?>>();
        for( int i = 0; i < SHORTCUTS.length ; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(KEY_INDEX, i);
            map.put(KEY_TITLE, r.getString(SHORTCUTS[i].nameRes));
            datas.add(map);
        }
        setListAdapter(new SimpleAdapter(this, datas,
                android.R.layout.simple_list_item_1, new String[] { KEY_TITLE },
                new int[] { android.R.id.text1 }));
	}
	

	private static String getPrefsKey(int widgetId) {
		return "shortcut" + widgetId + "_action";
	}
	
	public static int getActionForWidget(Context ctx, int widgetId) {
		SharedPreferences prefs = ctx.getSharedPreferences(WIDGET_PREFS, 0);
        return prefs.getInt(getPrefsKey(widgetId), -1);
        
	}
	
	public static void deleteWidget(Context ctx, int widgetId) {
		SharedPreferences prefs = ctx.getSharedPreferences(WIDGET_PREFS, 0);
        SharedPreferences.Editor edit = prefs.edit();
		edit.remove(getPrefsKey(widgetId));
		edit.commit();
	}

	@Override
    @SuppressWarnings("unchecked")
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, Object> map = (Map<String, Object>)l.getItemAtPosition(position);

        Integer index = (Integer) map.get(KEY_INDEX);

        if(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            SharedPreferences prefs = getSharedPreferences(WIDGET_PREFS, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt(getPrefsKey(appWidgetId), index);
            edit.commit();
            
            
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId);
            setResult(RESULT_OK, resultValue);
            
            ShortcutWidgetProvider.updateWidget(this);
            
            finish();
        }else {
            Log.w(THIS_FILE, "Invalid widget ID here...");
        }
    }
}
