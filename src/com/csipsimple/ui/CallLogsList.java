/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2009 The Android Open Source Project
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.utils.Log;

public class CallLogsList extends ListActivity {

	private DBAdapter database;

	private Drawable drawableIncoming;
	private Drawable drawableOutgoing;
	private Drawable drawableMissed;

	private Activity contextToBindTo = this;

	private static final String THIS_FILE = "Call log list";
	
	public static final int MENU_ITEM_DELETE_CALL = Menu.FIRST;
	public static final int MENU_ITEM_DELETE_ALL = Menu.FIRST+1;
	

	public static final class RecentCallsListItemViews {
		TextView line1View;
		TextView labelView;
		TextView numberView;
		TextView dateView;
		ImageView iconView;
		View callView;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Bind to the service
		if (getParent() != null) {
			contextToBindTo  = getParent();
		}
		
		setContentView(R.layout.recent_calls);
		registerForContextMenu(getListView());

		// Ressources
		drawableIncoming = getResources().getDrawable(R.drawable.ic_call_log_list_incoming_call);
		drawableOutgoing = getResources().getDrawable(R.drawable.ic_call_log_list_outgoing_call);
		drawableMissed = getResources().getDrawable(R.drawable.ic_call_log_list_missed_call);

		// Db
		if (database == null) {
			database = new DBAdapter(this);
		}
		database.open();

		Cursor cursor = database.getAllCallLogs();
		startManagingCursor(cursor);

		CallLogsCursorAdapter cad = new CallLogsCursorAdapter(this, cursor);

		setListAdapter(cad);
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	class CallLogsCursorAdapter extends ResourceCursorAdapter implements OnClickListener {

		public CallLogsCursorAdapter(Context context, Cursor c) {
			super(context, R.layout.recent_calls_list_item, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final RecentCallsListItemViews tagView = (RecentCallsListItemViews) view.getTag();
			String number = cursor.getString(DBAdapter.NUMBER_COLUMN_INDEX);
			String cachedName = cursor.getString(DBAdapter.CALLER_NAME_COLUMN_INDEX);

			String remoteContact = number;
			String phoneNumber = number;

			// Store away the number so we can call it directly if you click on
			// the call icon
			tagView.callView.setTag(number);

			if(TextUtils.isEmpty(cachedName)) {
				// Reformat number
				Pattern sipUriSpliter = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*)@[^>]*(?:>)?");
				Matcher m = sipUriSpliter.matcher(number);
				if (m.matches()) {
					if (!TextUtils.isEmpty(m.group(2))) {
						remoteContact = m.group(2);
					} else if (!TextUtils.isEmpty(m.group(1))) {
						remoteContact = m.group(1);
					}
				}
			} else {
				remoteContact = cachedName;
			}

			tagView.line1View.setText(remoteContact);
			tagView.numberView.setText(phoneNumber);
			// tagView.line1View.setText(number);
			// tagView.numberView.setVisibility(View.GONE);
			// tagView.labelView.setVisibility(View.GONE);

			int type = cursor.getInt(DBAdapter.CALL_TYPE_COLUMN_INDEX);
			long date = cursor.getLong(DBAdapter.DATE_COLUMN_INDEX);

			// Set the date/time field by mixing relative and absolute times.
			int flags = DateUtils.FORMAT_ABBREV_RELATIVE;

			tagView.dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags));

			// Set the icon
			switch (type) {
			case Calls.INCOMING_TYPE:
				tagView.iconView.setImageDrawable(drawableIncoming);
				break;

			case Calls.OUTGOING_TYPE:
				tagView.iconView.setImageDrawable(drawableOutgoing);
				break;

			case Calls.MISSED_TYPE:
				tagView.iconView.setImageDrawable(drawableMissed);
				break;
			}

			// TODO : async get contact info after first draw
			// and fill it if possible

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = super.newView(context, cursor, parent);
			RecentCallsListItemViews tagView = new RecentCallsListItemViews();
			tagView.line1View = (TextView) view.findViewById(R.id.line1);
			tagView.labelView = (TextView) view.findViewById(R.id.label);
			tagView.numberView = (TextView) view.findViewById(R.id.number);
			tagView.dateView = (TextView) view.findViewById(R.id.date);
			tagView.iconView = (ImageView) view.findViewById(R.id.call_type_icon);
			tagView.callView = view.findViewById(R.id.call_icon);
			tagView.callView.setOnClickListener(this);

			view.setTag(tagView);

			return view;
		}

		@Override
		public void onClick(View view) {
			placeCallLogCall((String) view.getTag());
		}
	}
	

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(THIS_FILE, "Clicked : " + position + " and " + id);
		Intent intent = new Intent(this, CallLog.class);
		intent.putExtra(Calls._ID, (int) id);
		startActivity(intent);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        menu.add(0, MENU_ITEM_DELETE_CALL, 0, R.string.callLog_delete_entry);
        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.callLog_delete_all);
	}
	
	
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return false;
        }
        
        int calllogId = cursor.getInt(DBAdapter.ID_COLUMN_INDEX);
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE_CALL: {
        		database.deleteOneCallLog(calllogId);
        		CallLogsCursorAdapter cad = (CallLogsCursorAdapter) getListAdapter();
        		cad.getCursor().requery();
        		//cad.notifyDataSetChanged();	// Apparently not needed
                return true;
            }
            case MENU_ITEM_DELETE_ALL: {
        		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        	    alertDialog.setTitle(R.string.callLog_delDialog_title);
        	    alertDialog.setMessage(getString(R.string.callLog_delDialog_message));
        	    alertDialog.setButton(getString(R.string.callLog_delDialog_yes), new DialogInterface.OnClickListener() {	// DEPRECATED
        	      public void onClick(DialogInterface dialog, int which) {
        	    	database.deleteAllCallLogs();
            		CallLogsCursorAdapter cad = (CallLogsCursorAdapter) getListAdapter();
            		cad.getCursor().requery();
        	        return;
        	    } }); 
        	    alertDialog.setButton2(getString(R.string.callLog_delDialog_no), (DialogInterface.OnClickListener)null);	// DEPRECATED
        	    try {
        	    	alertDialog.show();
        	    }catch(Exception e) {
        	    	Log.e(THIS_FILE, "error while trying to show deletion yes/no dialog");
        	    }
            }
        }
        return false;
    }
    
    

	private void placeCallLogCall(String number) {
		Matcher m = getSipMatcher(number);
		if (m.matches()) {
			if ( !TextUtils.isEmpty(m.group(2)) && !TextUtils.isEmpty(m.group(3)) ) {
				Intent intent = new Intent(Intent.ACTION_CALL);
				intent.setData( Uri.fromParts("sip",  m.group(2) + "@" + m.group(3), null));
				startActivity(intent);
				return;
			}else {
				Log.e(THIS_FILE, "Failed to place call"+number);
			}
		}
	}
	
	private Matcher getSipMatcher(String number) {
		Pattern sipUriSpliter = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?sip(?:s)?:([^@]*)@([^>]*)(?:>)?");
		return sipUriSpliter.matcher(number);
	}
	
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK : {
			onBackPressed();
			//return true;
			break;
		}
		}

		return super.onKeyUp(keyCode, event);
	}
	
	public void onBackPressed() {
		if(contextToBindTo != null && contextToBindTo instanceof SipHome) {
			((SipHome) contextToBindTo).onBackPressed();
		}else {
			finish();
		}
	}
	
}
