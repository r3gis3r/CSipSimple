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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

public class AccountFilters extends ListActivity {


	private static final String THIS_FILE = "AccountFilters";
	public static final int ADD_MENU = Menu.FIRST + 1;
	
	private static final int MODIFY_FILTER = 0;
	private static final int ADD_FILTER = MODIFY_FILTER+1;
	
	private DBAdapter database;
	private int accountId = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			accountId = extras.getInt("account_id");
		}
		
		if (accountId == -1) {
			Log.e(THIS_FILE, "You provide an empty account id....");
			finish();
		}
		
		setContentView(R.layout.filters_list);
		
		//Add add row
		LinearLayout add_row = (LinearLayout) findViewById(R.id.add_filter);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editFilterActivity(-1);
			}
		});
		
		
		database = new DBAdapter(this);
		database.open();
		Cursor cursor = database.getFiltersForAccount(accountId);
		startManagingCursor(cursor);
		CursorAdapter adapter = new FiltersCursorAdapter(this, cursor);
		setListAdapter(adapter);
		
		DragnDropListView listView = (DragnDropListView) getListView();
		listView.setOnDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				//TODO ...
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}
	
	
	
	class FiltersCursorAdapter extends ResourceCursorAdapter {
		public FiltersCursorAdapter(Context context, Cursor c) {
			super(context, R.layout.filters_list_item, c);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Filter filter = new Filter();
			filter.createFromDb(cursor);

			
			TextView tv = (TextView) view.findViewById(R.id.line1);
			tv.setText(filter.getRepresentation(context));
			ImageView icon = (ImageView) view.findViewById(R.id.action_icon);
			switch (filter.action) {
			case Filter.ACTION_CAN_CALL:
				icon.setImageResource(android.R.drawable.ic_menu_call);
				break;
			case Filter.ACTION_CANT_CALL:
				icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
				break;
			case Filter.ACTION_REPLACE:
				icon.setImageResource(android.R.drawable.ic_menu_edit);
				break;
			}
			
		}

	}
	

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		editFilterActivity((int)id);
	}
	
	// Context Menu
	//TODO
	
	// Menu
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, ADD_MENU, Menu.NONE, R.string.add_filter).setIcon(
				android.R.drawable.ic_menu_add);
		return super.onCreateOptionsMenu(menu);
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ADD_MENU:
			editFilterActivity(-1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    
    private void editFilterActivity(int filterId) {
    	Intent it = new Intent(this, EditFilter.class);
		it.putExtra(Intent.EXTRA_UID, filterId);
		it.putExtra(Filter.FIELD_ACCOUNT, accountId);
		
		startActivityForResult(it, (filterId == -1)?ADD_FILTER:MODIFY_FILTER);
    }
    
    
}
