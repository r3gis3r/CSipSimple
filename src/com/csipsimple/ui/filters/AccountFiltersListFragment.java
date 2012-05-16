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

package com.csipsimple.ui.filters;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.ui.account.AccountsEditListAdapter;
import com.csipsimple.widgets.CSSListFragment;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

import java.util.ArrayList;

public class AccountFiltersListFragment extends CSSListFragment {
    private static final String THIS_FILE = "AccountFiltersListFragment";

    private boolean dualPane;
	private Long curCheckFilterId = SipProfile.INVALID_ID;
    private View mHeaderView;
    private AccountFiltersListAdapter mAdapter;
	private long accountId;
	
	
	private final static String CURRENT_CHOICE = "curChoice";

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	public void setAccountId(long accId) {
	    accountId = accId;
	}
	
	@Override 
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListView lv = getListView();

        //getListView().setSelector(R.drawable.transparent);
        lv.setCacheColorHint(Color.TRANSPARENT);
        
        
        // View management
        View detailsFrame = getActivity().findViewById(R.id.details);
        dualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            curCheckFilterId = savedInstanceState.getLong(CURRENT_CHOICE, SipProfile.INVALID_ID);
            //curCheckWizard = savedInstanceState.getString(CURRENT_WIZARD);
        }
        setListShown(false);
        if(mAdapter == null) {
            if(mHeaderView != null) {
                lv.addHeaderView(mHeaderView , null, true);
            }
            mAdapter = new AccountFiltersListAdapter(getActivity(), null);
            //getListView().setEmptyView(getActivity().findViewById(R.id.progress_container));
            //getActivity().findViewById(android.R.id.empty).setVisibility(View.GONE);
            setListAdapter(mAdapter);
            registerForContextMenu(lv);
    
            
            lv.setVerticalFadingEdgeEnabled(true);
        }
        
        if (dualPane) {
            // In dual-pane mode, the list view highlights the selected item.
        	Log.d("lp", "dual pane mode");
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        	//lv.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
            lv.setVerticalScrollBarEnabled(false);
            lv.setFadingEdgeLength(50);
            
            updateCheckedItem();
            // Make sure our UI is in the correct state.
            //showDetails(curCheckPosition, curCheckWizard);
        }else {
        	//getListView().setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        	lv.setVerticalScrollBarEnabled(true);
        	lv.setFadingEdgeLength(100);
        }
    }
	@Override
	public void onResume() {
	    super.onResume();
        getLoaderManager().initLoader(0, null, this);
	}
	
	// Menu stuff
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(R.string.add_filter)
                .setIcon(android.R.drawable.ic_menu_add)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onClickAddFilter();
                        return true;
                    }
                })
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM );

        menu.add(R.string.reorder).setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AccountsEditListAdapter ad = (AccountsEditListAdapter) getListAdapter();
                        ad.toggleDraggable();
                        return true;
                    }
                }).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Use custom drag and drop view -- reuse the one of accounts_edit_list
        View v = inflater.inflate(R.layout.accounts_edit_list, container, false);

        
        final DragnDropListView lv = (DragnDropListView) v.findViewById(android.R.id.list);
        
        lv.setGrabberId(R.id.grabber);
        // Setup the drop listener
        lv.setOnDropListener(new DropListener() {
            @Override
            public void drop(int from, int to) {
                Log.d(THIS_FILE, "Drop from " + from + " to " + to);
                int hvC = lv.getHeaderViewsCount();
                from = Math.max(0, from - hvC);
                to = Math.max(0, to - hvC);
                
                int i;
                // First of all, compute what we get before move
                ArrayList<Long> orderedList = new ArrayList<Long>();
                CursorAdapter ad = (CursorAdapter) getListAdapter();
                for(i=0; i < ad.getCount(); i++) {
                    orderedList.add(ad.getItemId(i));
                }
                // Then, invert in the current list the two items ids
                Long moved = orderedList.remove(from);
                orderedList.add(to, moved);
                
                // Finally save that in db
                ContentResolver cr = getActivity().getContentResolver();
                for(i=0; i<orderedList.size(); i++) {
                    Uri uri = ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, orderedList.get(i));
                    ContentValues cv = new ContentValues();
                    cv.put(Filter.FIELD_PRIORITY, i);
                    cr.update(uri, cv, null, null);
                }
            }
        });
        
        OnClickListener addClickButtonListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAddFilter();
            }
        };
        // Header view
        mHeaderView = inflater.inflate(R.layout.generic_add_header_list, container, false);
        mHeaderView.setOnClickListener(addClickButtonListener);
        ((TextView) mHeaderView.findViewById(R.id.text)).setText(R.string.add_filter);
        
        // Empty view
        Button bt = (Button) v.findViewById(android.R.id.empty);
        bt.setText(R.string.add_filter);
        bt.setOnClickListener(addClickButtonListener);
        
        return v;
    }
    
    private void updateCheckedItem() {
    	if(curCheckFilterId != SipProfile.INVALID_ID) {
	    	for(int i=0; i<getListAdapter().getCount(); i++) {
	        	long profId = getListAdapter().getItemId(i);
	        	if(profId == curCheckFilterId) {
	        		getListView().setItemChecked(i, true);
	        	}
	        }
    	}else {
    		for(int i=0; i<getListAdapter().getCount(); i++) {
    			getListView().setItemChecked(i, false);
    		}
    	}
    }
    

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CURRENT_CHOICE, curCheckFilterId);
    }
    

    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
    	
        Log.d(THIS_FILE, "Checked " + position + " et " + id);
        
        ListView lv = getListView();
        lv.setItemChecked(position, true);
        
        curCheckFilterId = id;
        showDetails(id);
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    private void showDetails(long filterId) {
        //curCheckPosition = index;

        /*
    	if (dualPane) {
            // If we are not currently showing a fragment for the new
            // position, we need to create and install a new one.
        	AccountEditFragment df = AccountEditFragment.newInstance(profileId);
            //df.setOnQuitListener(this);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df, null);
          //  ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.setTransition(FragmentTransaction.TRANSIT_NONE);
            //if(profileId != Profile.INVALID_ID) {
            //	ft.addToBackStack(null);
            //}
            ft.commit();
        } else {
        */
        	
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.

            Intent it = new Intent(getActivity(), EditFilter.class);
            it.putExtra(Intent.EXTRA_UID, filterId);
            it.putExtra(Filter.FIELD_ACCOUNT, accountId);
            startActivity(it);
        	
        	/*
        }
        */
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		 return new CursorLoader(getActivity(), SipManager.FILTER_URI, new String[] {
			BaseColumns._ID,
			Filter.FIELD_ACCOUNT,
            Filter.FIELD_ACTION,
            Filter.FIELD_MATCHES,
			Filter.FIELD_PRIORITY,
			Filter.FIELD_REPLACE
		 }, Filter.FIELD_ACCOUNT + "=?", new String[] {Long.toString(accountId)}, Filter.DEFAULT_ORDER);
		 
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
		// Select correct item if any
		updateCheckedItem();
	}


	// Context menu stuff
	// Activate / deactive menu
    // Modify the account
    public static final int MENU_ITEM_MODIFY = Menu.FIRST ;
    // Delete the account
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

    /**
     * Retrieve filter id from a given context menu info pressed
     * @param cmi The context menu info to retrieve infos from
     * @return corresponding filter id if everything goes well, -1 if not able to retrieve filter
     */
    private long filterIdFromContextMenuInfo(ContextMenuInfo cmi) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) cmi;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return -1;
        }
        return info.id;
    }
    
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final long filterId = filterIdFromContextMenuInfo(menuInfo);
        if(filterId == -1) {
            return;
        }

        menu.add(0, MENU_ITEM_MODIFY, 0, R.string.edit);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_filter);

    }

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
	    final long filterId = filterIdFromContextMenuInfo(item.getMenuInfo());
        if (filterId == -1) {
            // For some reason the requested item isn't available, do nothing
            return super.onContextItemSelected(item);
        }
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                getActivity().getContentResolver().delete(ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, filterId), null, null);
                return true;
            }
            case MENU_ITEM_MODIFY : {
                showDetails(filterId);
                return true;
            }
        }
        return super.onContextItemSelected(item);

	}
	
	private void onClickAddFilter() {
	    showDetails(-1);
	}

    @Override
    public void changeCursor(Cursor c) {
        if(mAdapter != null) {
            mAdapter.changeCursor(c);
        }
    }
	
	
}
