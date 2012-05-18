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

package com.csipsimple.ui.calllog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.ui.calllog.CallLogAdapter.OnCallLogAction;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.CSSListFragment;

import java.util.ArrayList;

/**
 * Displays a list of call log entries.
 */
public class CallLogListFragment extends CSSListFragment implements ViewPagerVisibilityListener,
        CallLogAdapter.CallFetcher, OnCallLogAction {

    private static final String THIS_FILE = "CallLogFragment";

    private boolean mShowOptionsMenu;
    private CallLogAdapter mAdapter;

    private boolean mDualPane;

    private ActionMode mMode;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void attachAdapter() {
        if(getListAdapter() == null) {
            if(mAdapter == null) {
                Log.d(THIS_FILE, "Attach call log adapter now");
                // Adapter
                mAdapter = new CallLogAdapter(getActivity(), this);
                mAdapter.setOnCallLogActionListener(this);
            }
            setListAdapter(mAdapter);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.call_log_fragment, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // View management
        mDualPane = getResources().getBoolean(R.bool.use_dual_panes);


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
        
        // Map long press
        lv.setLongClickable(true);
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> ad, View v, int pos, long id) {
                turnOnActionMode();
                getListView().setItemChecked(pos, true);
                mMode.invalidate();
                return true;
            }
        });
    }
    
    /*
    @Override
    public void onResume() {
        super.onResume();
        fetchCalls();
    }
    */

    @Override
    public void fetchCalls() {
        attachAdapter();
        if(isResumed()) {
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    boolean alreadyLoaded = false;
    
    @Override
    public void onVisibilityChanged(boolean visible) {
        if (mShowOptionsMenu != visible) {
            mShowOptionsMenu = visible;
            // Invalidate the options menu since we are changing the list of
            // options shown in it.
            SherlockFragmentActivity activity = getSherlockActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
        

        if(visible) {
            attachAdapter();
            // Start loading
            if(!alreadyLoaded) {
                getLoaderManager().initLoader(0, null, this);
                alreadyLoaded = true;
            }
        }
        
        
        if (visible && isResumed()) {
            //getLoaderManager().restartLoader(0, null, this);
            ListView lv = getListView();
            if (lv != null && mAdapter != null) {
                final int checkedPos = lv.getCheckedItemPosition();
                if (checkedPos >= 0) {
                    // TODO post instead
                    Thread t = new Thread() {
                        public void run() {
                            final long[] selectedIds = mAdapter.getCallIdsAtPosition(checkedPos);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    viewDetails(checkedPos, selectedIds);  
                                }
                            });
                        };
                    };
                    t.start();
                }
            }
        }
        
        
        if(!visible && mMode != null) {
            mMode.finish();
        }
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        MenuItem delMenu = menu.add(R.string.callLog_delete_all);
        delMenu.setIcon(R.drawable.ic_ab_trash_dark).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        delMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                deleteAllCalls();
                return true;
            }
        });
    }

    private void deleteAllCalls() {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(R.string.callLog_delDialog_title);
        alertDialog.setMessage(getString(R.string.callLog_delDialog_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.callLog_delDialog_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().getContentResolver().delete(SipManager.CALLLOG_URI, null,
                                null);
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.callLog_delDialog_no),
                (DialogInterface.OnClickListener) null);
        try {
            alertDialog.show();
        } catch (Exception e) {
            Log.e(THIS_FILE, "error while trying to show deletion yes/no dialog");
        }
    }

    // Loader
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(getActivity(), SipManager.CALLLOG_URI, new String[] {
                CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.CACHED_NUMBER_LABEL,
                CallLog.Calls.CACHED_NUMBER_TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE,
                CallLog.Calls.NEW, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                SipManager.CALLLOG_PROFILE_ID_FIELD
        },
                null, null,
                Calls.DEFAULT_SORT_ORDER);
    }


    @Override
    public void viewDetails(int position, long[] callIds) {
        ListView lv = getListView();
        if(mMode != null) {
            lv.setItemChecked(position, !lv.isItemChecked(position));
            mMode.invalidate();
            // Don't see details in this case
            return;
        }
        
        if (mDualPane) {
            // If we are not currently showing a fragment for the new
            // position, we need to create and install a new one.
            CallLogDetailsFragment df = new CallLogDetailsFragment();
            Bundle bundle = new Bundle();
            bundle.putLongArray(CallLogDetailsFragment.EXTRA_CALL_LOG_IDS, callIds);
            df.setArguments(bundle);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df, null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();

            getListView().setItemChecked(position, true);
        } else {
            Intent it = new Intent(getActivity(), CallLogDetailsActivity.class);
            it.putExtra(CallLogDetailsFragment.EXTRA_CALL_LOG_IDS, callIds);
            getActivity().startActivity(it);
        }
    }

    @Override
    public void placeCall(String number, Long accId) {
        if(!TextUtils.isEmpty(number)) {
            Intent it = new Intent(Intent.ACTION_CALL);
            it.setData(SipUri.forgeSipUri("csip", SipUri.getCanonicalSipContact(number, false)));
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(accId != null) {
                it.putExtra(SipProfile.FIELD_ACC_ID, accId);
            }
            getActivity().startActivity(it);
        }
    }

    
    // Action mode
    
    private void turnOnActionMode() {
        Log.d(THIS_FILE, "Long press");
        mMode = getSherlockActivity().startActionMode(new CallLogActionMode());
        ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
    }
    
    private class CallLogActionMode  implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.d(THIS_FILE, "onCreateActionMode");
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.call_log_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.d(THIS_FILE, "onPrepareActionMode");
            ListView lv = getListView();
            int nbrCheckedItem = 0;

            for (int i = 0; i < lv.getCount(); i++) {
                if (lv.isItemChecked(i)) {
                    nbrCheckedItem++;
                }
            }
            menu.findItem(R.id.delete).setVisible(nbrCheckedItem > 0);
            menu.findItem(R.id.dialpad).setVisible(nbrCheckedItem == 1);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if(itemId == R.id.delete) {
                actionModeDelete();
                return true;
            }else if(itemId == R.id.invert_selection) {
                actionModeInvertSelection();
                return true;
            }else if(itemId == R.id.dialpad) {
                actionModeDialpad();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(THIS_FILE, "onDestroyActionMode");

            ListView lv = getListView();
            // Uncheck all
            int count = lv.getAdapter().getCount();
            for (int i = 0; i < count; i++) {
                lv.setItemChecked(i, false);
            }
            mMode = null;
        }
        
    }
    
    private void actionModeDelete() {
        ListView lv = getListView();
        
        ArrayList<Long> checkedIds = new ArrayList<Long>();
        
        for(int i = 0; i < lv.getCount(); i++) {
            if(lv.isItemChecked(i)) {
                long[] selectedIds = mAdapter.getCallIdsAtPosition(i);
                
                for(long id : selectedIds) {
                    checkedIds.add(id);
                }
                
            }
        }
        if(checkedIds.size() > 0) {
            String strCheckedIds = TextUtils.join(", ", checkedIds);
            Log.d(THIS_FILE, "Checked positions ("+ strCheckedIds +")");
            getActivity().getContentResolver().delete(SipManager.CALLLOG_URI, CallLog.Calls._ID + " IN ("+strCheckedIds+")", null);
            mMode.finish();
        }
    }
    
    private void actionModeInvertSelection() {
        ListView lv = getListView();

        for(int i = 0; i < lv.getCount(); i++) {
            lv.setItemChecked(i, !lv.isItemChecked(i));
        }
        mMode.invalidate();
    }
    
    private void actionModeDialpad() {
        
        ListView lv = getListView();

        for(int i = 0; i < lv.getCount(); i++) {
            if(lv.isItemChecked(i)) {
                mAdapter.getItem(i);
                String number = mAdapter.getCallRemoteAtPostion(i);
                if(!TextUtils.isEmpty(number)) {
                    Intent it = new Intent(Intent.ACTION_DIAL);
                    it.setData(SipUri.forgeSipUri("sip", number));
                    startActivity(it);
                }
                break;
            }
        }
        mMode.invalidate();
        
    }
    
    @Override
    public void changeCursor(Cursor c) {
        mAdapter.changeCursor(c);
    }
    
}
