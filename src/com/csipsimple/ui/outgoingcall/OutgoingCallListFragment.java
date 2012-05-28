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

package com.csipsimple.ui.outgoingcall;

import android.app.PendingIntent.CanceledException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsLoader;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.CSSListFragment;

public class OutgoingCallListFragment extends CSSListFragment {
    
    private static final String THIS_FILE = "OutgoingCallListFragment";
    private OutgoingAccountsAdapter mAdapter;
    private AccountsLoader accLoader;
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        attachAdapter();
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void attachAdapter() {
        if(getListAdapter() == null) {
            if(mAdapter == null) {
                mAdapter = new OutgoingAccountsAdapter(this, null);
            }
            setListAdapter(mAdapter);
        }
    }
    

    @Override
    public Loader<Cursor> onCreateLoader(int loader, Bundle args) {
        OutgoingCallChooser superActivity = ((OutgoingCallChooser) getActivity());
        accLoader = new AccountsLoader(getActivity(), superActivity.getPhoneNumber(), superActivity.shouldIgnoreRewritingRules());
        return accLoader;
        
    }
    
    /**
     * Place the call for a given cursor positionned at right index in list
     * @param c The cursor pointing the entry we'd like to call
     * @return true if call performed, false else
     */
    private boolean placeCall(Cursor c) {
        OutgoingCallChooser superActivity = ((OutgoingCallChooser)getActivity());
    
        ISipService service = superActivity.getConnectedService();
        long accountId = c.getLong(c.getColumnIndex(SipProfile.FIELD_ID));
        if(accountId > SipProfile.INVALID_ID) {
            // Extra check for the account id.
            if(service == null) {
                return false;
            }
            boolean canCall = c.getInt(c.getColumnIndex(AccountsLoader.FIELD_STATUS_OUTGOING)) == 1;
            if(!canCall) {
                return false;
            }
            try {
                String toCall = c.getString(c.getColumnIndex(AccountsLoader.FIELD_NBR_TO_CALL));
                service.makeCall(toCall, (int) accountId);
                superActivity.finishServiceIfNeeded(true);
                return true;
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Unable to make the call", e);
            }
        }else if(accountId < SipProfile.INVALID_ID) {
            // This is a plugin row.
            if(accLoader != null) {
                CallHandlerPlugin ch = accLoader.getCallHandlerWithAccountId(accountId);
                if(ch == null) {
                    Log.w(THIS_FILE, "Call handler not anymore available in loader... something gone wrong");
                    return false;
                }
                try {
                    String nextExclude = ch.getNextExcludeTelNumber();
                    if (nextExclude != null && service != null) {
                        try {
                            service.ignoreNextOutgoingCallFor(nextExclude);
                        } catch (RemoteException e) {
                            Log.e(THIS_FILE, "Ignore next outgoing number failed");
                        }
                    }
                    ch.getIntent().send();
                    superActivity.finishServiceIfNeeded(false);
                    return true;
                } catch (CanceledException e) {
                    Log.e(THIS_FILE, "Pending intent cancelled", e);
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void changeCursor(Cursor c) {
        if(c != null) {
            OutgoingCallChooser superActivity = ((OutgoingCallChooser)getActivity());
            Long accountToCall = superActivity.getAccountToCallTo();
            // Move to first to search in this cursor
            c.moveToFirst();
            // First of all, if only one is available... try call with it
            if(c.getCount() == 1) {
                if(placeCall(c)) {
                    c.close();
                    return;
                }
            }else {
                // Now lets search for one in for call mode if service is ready
                do {
                    if(c.getInt(c.getColumnIndex(AccountsLoader.FIELD_FORCE_CALL)) == 1) {
                        if(placeCall(c)) {
                            c.close();
                            return;
                        }
                    }
                    if(accountToCall != SipProfile.INVALID_ID) {
                        if(accountToCall == c.getLong(c.getColumnIndex(SipProfile.FIELD_ID))) {
                            if(placeCall(c)) {
                                c.close();
                                return;
                            }
                        }
                    }
                } while(c.moveToNext());
            }
            
        }

        // Set adapter content if nothing to force was found
        if(mAdapter != null) {
            mAdapter.changeCursor(c);
        }
    }
    
    @Override
    public synchronized void onListItemClick(ListView l, View v, int position, long id) {
        if(mAdapter != null) {
            placeCall((Cursor) mAdapter.getItem(position));
        }
    }

    public AccountsLoader getAccountLoader() {
        return accLoader;
    }


}
