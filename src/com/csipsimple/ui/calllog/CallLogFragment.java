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
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SupportActivity;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.MenuItem.OnMenuItemClickListener;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.ui.calllog.CallLogAdapter.OnCallLogAction;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

/**
 * Displays a list of call log entries.
 */
public class CallLogFragment extends ListFragment implements ViewPagerVisibilityListener,
        CallLogAdapter.CallFetcher, LoaderManager.LoaderCallbacks<Cursor>, OnCallLogAction {

    private static final String THIS_FILE = "CallLogFragment";

    private boolean mShowOptionsMenu;
    private CallLogAdapter mAdapter;

    private boolean mDualPane;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
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

        // Adapter
        mAdapter = new CallLogAdapter(getActivity(), this);
        mAdapter.setOnCallLogActionListener(this);

        setListAdapter(mAdapter);

        // Start loading
        // getLoaderManager().initLoader(0, null, this);

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

        // Start out with a progress indicator.
        // setListShown(false);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        fetchCalls();
    }

    @Override
    public void fetchCalls() {
        getLoaderManager().restartLoader(0, null, this);

    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        if (mShowOptionsMenu != visible) {
            mShowOptionsMenu = visible;
            // Invalidate the options menu since we are changing the list of
            // options shown in it.
            SupportActivity activity = getSupportActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
            }
        }
        if (visible && isResumed()) {
            getLoaderManager().restartLoader(0, null, this);
        }
        if (visible) {
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
    }

    // Options
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        boolean showInActionBar = Compatibility.isCompatible(14)
                || Compatibility.isTabletScreen(getActivity());
        int ifRoomIfSplit = showInActionBar ? MenuItem.SHOW_AS_ACTION_IF_ROOM
                : MenuItem.SHOW_AS_ACTION_NEVER;

        MenuItem delMenu = menu.add(R.string.callLog_delete_all);
        delMenu.setIcon(android.R.drawable.ic_menu_delete).setShowAsAction(ifRoomIfSplit);
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
        alertDialog.setButton(getString(R.string.callLog_delDialog_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().getContentResolver().delete(SipManager.CALLLOG_URI, null,
                                null);
                    }
                });
        alertDialog.setButton2(getString(R.string.callLog_delDialog_no),
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

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.changeCursor(data);

        // The list should now be shown.
        /*
         * if (isResumed()) { setListShown(true); } else {
         * setListShownNoAnimation(true); }
         */
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.changeCursor(null);
    }

    @Override
    public void viewDetails(int position, long[] callIds) {
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
            it.setData(Uri.fromParts("csip", SipUri.getCanonicalSipContact(number, false), null));
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if(accId != null) {
                it.putExtra(SipProfile.FIELD_ACC_ID, accId);
            }
            startActivity(it);
        }
    }

}
