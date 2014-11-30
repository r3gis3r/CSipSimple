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

import android.app.PendingIntent;
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
    private long startDate;
    private boolean callMade = false;
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        callMade = false;
        attachAdapter();
        getLoaderManager().initLoader(0, null, this);
        startDate = System.currentTimeMillis();
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
    
    final long MOBILE_CALL_DELAY_MS = 600;
    
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
                if(superActivity != null) {
                    superActivity.finishServiceIfNeeded(true);
                }
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
                String nextExclude = ch.getNextExcludeTelNumber();
                long delay = 0;
                if (nextExclude != null && service != null) {
                    try {
                        service.ignoreNextOutgoingCallFor(nextExclude);
                    } catch (RemoteException e) {
                        Log.e(THIS_FILE, "Ignore next outgoing number failed");
                    }
                    delay = MOBILE_CALL_DELAY_MS - (System.currentTimeMillis() - startDate);
                }
                
                if(ch.getIntent() != null) {
                    PluginCallRunnable pendingTask = new PluginCallRunnable(ch.getIntent(), delay);
                    Log.d(THIS_FILE, "Deferring call task of " + delay);
                    pendingTask.start();
                }
                return true;
            }
        }
        return false;
    }
    
    private class PluginCallRunnable extends Thread {
        private PendingIntent pendingIntent;
        private long delay;
        public PluginCallRunnable(PendingIntent pi, long d) {
            pendingIntent = pi;
            delay = d;
        }
        
        @Override
        public void run() {
            if(delay > 0) {
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    Log.e(THIS_FILE, "Thread that fires outgoing call has been interrupted");
                }
            }
            OutgoingCallChooser superActivity = ((OutgoingCallChooser)getActivity());
            try {
                pendingIntent.send();
            } catch (CanceledException e) {
                Log.e(THIS_FILE, "Pending intent cancelled", e);
            }
            if(superActivity != null) {
                superActivity.finishServiceIfNeeded(false);
            }
        }
    }

    @Override
    public synchronized void changeCursor(Cursor c) {
        if(c != null && callMade == false) {
            OutgoingCallChooser superActivity = ((OutgoingCallChooser)getActivity());
            Long accountToCall = superActivity.getAccountToCallTo();
            boolean canCallOtherAccounts = superActivity.canCallAutomatically();
            // Move to first to search in this cursor
            c.moveToFirst();
            
            // If only one is available.
            // We don't use this filter if account were specified.
            if(c.getCount() == 1 && canCallOtherAccounts) {
                if(placeCall(c)) {
                    c.close();
                    callMade = true;
                    return;
                }
            }else {
                do {
                    if(accountToCall != SipProfile.INVALID_ID) {
                        // It's account that was asked to call
                        if(accountToCall == c.getLong(c.getColumnIndex(SipProfile.FIELD_ID))) {
                            if(placeCall(c)) {
                                c.close();
                                callMade = true;
                                return;
                            }
                        }
                    } else if((c.getInt(c.getColumnIndex(AccountsLoader.FIELD_FORCE_CALL)) == 1) && canCallOtherAccounts) {
                        // If one with forceCall flag
                        // We don't use this filter if account were specified.
                        if(placeCall(c)) {
                            c.close();
                            callMade = true;
                            return;
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
