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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

public class AccountFilters extends ListActivity {

    private static final String THIS_FILE = "AccountFilters";
    public static final int ADD_MENU = Menu.FIRST + 1;

    private static final int MODIFY_FILTER = 0;
    private static final int ADD_FILTER = MODIFY_FILTER + 1;
    private static final int MENU_ITEM_DELETE = Menu.FIRST;

    private DBAdapter database;
    private long accountId = SipProfile.INVALID_ID;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            accountId = extras.getLong(SipProfile.FIELD_ID, -1);
        }

        if (accountId == -1) {
            Log.e(THIS_FILE, "You provide an empty account id....");
            finish();
        }

        // Custom title listing the account name for filters
        // final boolean customTitleOk =
        // requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.filters_list);
        /*
         * if(customTitleOk) {
         * getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
         * R.layout.account_filters_title); final TextView titleText =
         * (TextView) findViewById(R.id.account_filters_title); if (titleText !=
         * null) { SipProfile acct = SipProfile.getProfileFromDbId(this,
         * accountId, new String[] {SipProfile.FIELD_ID,
         * SipProfile.FIELD_DISPLAY_NAME});
         * titleText.setText(R.string.filters_for); titleText.append(" " +
         * acct.display_name); } }
         */

        database = new DBAdapter(this);
        database.open();
        // Add add row
        TextView add_row = (TextView) findViewById(R.id.add_filter);
        add_row.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editFilterActivity(-1);
            }
        });

        cursor = database.getFiltersForAccount(accountId);
        if (cursor != null) {
            startManagingCursor(cursor);
        }
        CursorAdapter adapter = new FiltersCursorAdapter(this, cursor);
        setListAdapter(adapter);

        DragnDropListView listView = (DragnDropListView) getListView();
        listView.setOnDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                // Update priorities
                CursorAdapter ad = (CursorAdapter) getListAdapter();
                int numRows = ad.getCount();

                for (int i = 0; i < numRows; i++) {
                    // Log.d(THIS_FILE, "i= "+i+" from ="+from+" to="+to);
                    if (i != from) {
                        if (from > i && i >= to) {
                            updateFilterPriority(ad.getItemId(i), i + 1);
                        } else if (from < i && i <= to) {
                            updateFilterPriority(ad.getItemId(i), i - 1);
                        } else {
                            updateFilterPriority(ad.getItemId(i), i);
                        }
                    } else {
                        updateFilterPriority(ad.getItemId(from), to);
                    }
                }
                cursor.requery();
            }
        });

        listView.setOnCreateContextMenuListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }

    private void updateFilterPriority(long filterId, int newPrio) {
        ContentValues cv = new ContentValues();
        cv.put(Filter.FIELD_PRIORITY, newPrio);
        getContentResolver()
                .update(ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, filterId), cv,
                        null, null);
    }

    class FiltersCursorAdapter extends ResourceCursorAdapter {
        public FiltersCursorAdapter(Context context, Cursor c) {
            super(context, R.layout.filters_list_item, c);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Filter filter = new Filter();
            filter.createFromDb(cursor);
            String filterDesc = filter.getRepresentation(context);
            
            TextView tv = (TextView) view.findViewById(R.id.line1);
            ImageView icon = (ImageView) view.findViewById(R.id.action_icon);
            
            tv.setText(filterDesc);
            icon.setContentDescription(filterDesc);
            switch (filter.action) {
                case Filter.ACTION_CAN_CALL:
                    icon.setImageResource(R.drawable.ic_menu_goto);
                    break;
                case Filter.ACTION_CANT_CALL:
                    icon.setImageResource(R.drawable.ic_menu_blocked_user);
                    break;
                case Filter.ACTION_REPLACE:
                    icon.setImageResource(android.R.drawable.ic_menu_edit);
                    break;
                case Filter.ACTION_DIRECTLY_CALL:
                    icon.setImageResource(R.drawable.ic_menu_answer_call);
                    break;
                case Filter.ACTION_AUTO_ANSWER:
                    icon.setImageResource(R.drawable.ic_menu_auto_answer);
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        editFilterActivity((int) id);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, ADD_MENU, Menu.NONE, R.string.add_filter).setIcon(
                android.R.drawable.ic_menu_add);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selId = item.getItemId();
        if (selId == ADD_MENU) {
            editFilterActivity(-1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Context menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_filter);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            long id = info.id;
            int selId = item.getItemId();

            if (id >= 0 && selId == MENU_ITEM_DELETE) {
                getContentResolver().delete(
                        ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, id), null, null);
                cursor.requery();
                return true;
            }
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
        }

        return super.onContextItemSelected(item);
    }

    // Edit
    private void editFilterActivity(int filterId) {
        Intent it = new Intent(this, EditFilter.class);
        it.putExtra(Intent.EXTRA_UID, filterId);
        it.putExtra(Filter.FIELD_ACCOUNT, accountId);

        startActivityForResult(it, (filterId == -1) ? ADD_FILTER : MODIFY_FILTER);
    }

}
