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

package com.csipsimple.ui.messages;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.MenuItem.OnMenuItemClickListener;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipMessage;
import com.csipsimple.service.SipNotifications;
import com.csipsimple.ui.messages.ConverstationAdapter.ConversationListItemViews;
import com.csipsimple.utils.Compatibility;

/**
 * This activity provides a list view of existing conversations.
 */
public class ConversationList extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	//private static final String THIS_FILE = "Conv List";
	
    // IDs of the main menu items.
    public static final int MENU_COMPOSE_NEW          = 0;
    public static final int MENU_DELETE_ALL           = 1;

    // IDs of the context menu items for the list of conversations.
    public static final int MENU_DELETE               = 0;
    public static final int MENU_VIEW                 = 1;
	
    private boolean mDualPane;

    private ConverstationAdapter mAdapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View v = inflater.inflate(R.layout.call_log_fragment, container, false);
        
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        // Add header
        RelativeLayout headerView = (RelativeLayout)
                inflater.inflate(R.layout.conversation_list_item, lv, false);
        ((TextView) headerView.findViewById(R.id.from) ).setText(R.string.new_message);
        ((TextView) headerView.findViewById(R.id.subject) ).setText(R.string.create_new_message);
        headerView.findViewById(R.id.quick_contact_photo).setVisibility(View.GONE);
        lv.addHeaderView(headerView, null, true);
        
        TextView tv = (TextView) v.findViewById(android.R.id.empty);
        tv.setText(R.string.status_none);
        return v;
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View management
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);

        // Adapter
        mAdapter = new ConverstationAdapter(getActivity(), null);


        // Modify list view
        ListView lv = getListView();
        lv.setVerticalFadingEdgeEnabled(true);
        // lv.setCacheColorHint(android.R.color.transparent);
        if (mDualPane) {
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.setItemsCanFocus(false);
        } else {
            lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
            lv.setItemsCanFocus(true);
        }
        
        lv.setOnCreateContextMenuListener(this);
        
     // Start out with a progress indicator.
        // setListShown(false);

        setListAdapter(mAdapter);

        // Start loading
        // getLoaderManager().initLoader(0, null, this);

        
    }
    
    @Override
    public void onResume() {
        super.onResume();
        fetchMessages();

        SipNotifications nManager = new SipNotifications(getActivity());
        nManager.cancelMessages();
    }

    public void fetchMessages() {
        getLoaderManager().restartLoader(0, null, this);
    }

    public void viewDetails(int position, String from, String fromFull) {
        Bundle b = MessageFragment.getArguments(from, fromFull);

        // TODO dual screen mode
        Intent it = new Intent(getActivity(), MessageActivity.class);
        it.putExtras(b);
        startActivity(it);
    }


    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        boolean showInActionBar = Compatibility.isCompatible(14)
                || Compatibility.isTabletScreen(getActivity());
        int ifRoomIfSplit = showInActionBar ? MenuItem.SHOW_AS_ACTION_IF_ROOM
                : MenuItem.SHOW_AS_ACTION_NEVER;
        
        
        MenuItem writeMenu = menu.add(R.string.menu_compose_new);
        writeMenu.setIcon(R.drawable.ic_menu_msg_compose_holo_dark).setShowAsAction(ifRoomIfSplit);
        writeMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                viewDetails(-1, null, null);
                return true;
            }
        });

        if (getListAdapter().getCount() > 0) {

            MenuItem deleteAllMenu = menu.add(R.string.menu_delete_all);
            deleteAllMenu.setIcon(android.R.drawable.ic_menu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            deleteAllMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    confirmDeleteThread(null);
                    return true;
                }
            });
        }
    }
    
    
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), SipMessage.THREAD_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    

    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;
        if (info.position > 0) {
            menu.add(0, MENU_VIEW, 0, R.string.menu_view);
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        
        if (info.position > 0) {
            ConversationListItemViews cri = (ConversationListItemViews) info.targetView.getTag();
            String number = cri.from;
            String fromFull = cri.fromFull;
            
            if (number.equals("SELF")) {
                number = cri.to;
            }
                        
            switch (item.getItemId()) {
            case MENU_DELETE: {
                confirmDeleteThread(number);
                break;
            }
            case MENU_VIEW: {
                viewDetails(info.position, number, fromFull);
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ConversationListItemViews cri = (ConversationListItemViews) v.getTag();
        if (cri != null) {
            viewDetails(cri.position, cri.from, cri.fromFull);
        }
    }


	/*
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();


        return super.onPrepareOptionsMenu(menu);
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_COMPOSE_NEW:
                createNewMessage();
                break;
            case MENU_DELETE_ALL:
            	confirmDeleteThread(null);
            	break;
            default:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        if (position == 0) {
            createNewMessage();
        } else {
            ConversationListItemViews tag = (ConversationListItemViews) v.getTag();
            if (tag.from.equals("SELF")) {
                openThread(tag.to, tag.fromFull);
            } else {
                openThread(tag.from, tag.fromFull);
            }
        }
    }
    */
    
    private void confirmDeleteThread(final String from) {
    	
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.confirm_dialog_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
        .setCancelable(true)
        .setPositiveButton(R.string.delete, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(TextUtils.isEmpty(from)) {
				    getActivity().getContentResolver().delete(SipMessage.MESSAGE_URI, null, null);
				}else {
				    Builder threadUriBuilder = SipMessage.THREAD_ID_URI_BASE.buildUpon();
				    threadUriBuilder.appendEncodedPath(from);
				    getActivity().getContentResolver().delete(threadUriBuilder.build(), null, null);
				}
			}
		})
        .setNegativeButton(R.string.no, null)
        .setMessage(TextUtils.isEmpty(from)
                ? R.string.confirm_delete_all_conversations
                        : R.string.confirm_delete_conversation)
        .show();
    }
    
    
	
    
}
