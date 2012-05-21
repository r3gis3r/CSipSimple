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

package com.csipsimple.ui.account;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.provider.BaseColumns;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.CallHandlerPlugin.OnLoadListener;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AccountsLoader extends AsyncTaskLoader<Cursor> {

    public static final String FIELD_FORCE_CALL = "force_call";
    public static final String FIELD_NBR_TO_CALL = "nbr_to_call";
    public static final String FIELD_STATUS_OUTGOING = "status_for_outgoing";
    public static final String FIELD_STATUS_COLOR = "status_color";
    

    private static final String THIS_FILE = "OutgoingAccountsLoader";

    private Cursor currentResult;

    private final String numberToCall;
    private final boolean ignoreRewritting;
    private final boolean loadStatus;
    private final boolean onlyActive;
    private final boolean loadCallHandlerPlugins;
    
    /**
     * Constructor for loader for outgoing call context. <br/>
     * This one will care of rewriting number and keep track of accounts status.
     * @param context Your app context
     * @param number Phone number for outgoing call
     * @param ignoreRewrittingRules Should we ignore rewriting rules.
     */
    public AccountsLoader(Context context, String number, boolean ignoreRewrittingRules) {
        super(context);
        numberToCall = number;
        ignoreRewritting = ignoreRewrittingRules;
        loadStatus = true;
        onlyActive = true;
        loadCallHandlerPlugins = true;
    }
    

    public AccountsLoader(Context context, boolean onlyActiveAccounts, boolean withCallHandlerPlugins) {
        super(context);
        numberToCall = "";
        ignoreRewritting = true;
        loadStatus = false;
        onlyActive = onlyActiveAccounts;
        loadCallHandlerPlugins = withCallHandlerPlugins;
        
    }

    private ContentObserver loaderObserver = new ForceLoadContentObserver();
    private ArrayList<FilteredProfile> finalAccounts;
    
    
    @Override
    public Cursor loadInBackground() {
        // First register for status updates
        if(loadStatus) {
            getContext().getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI,
                true, loaderObserver);
        }
        
        ArrayList<FilteredProfile> prefinalAccounts = new ArrayList<FilteredProfile>();
        
        // Get all sip profiles
        ArrayList<SipProfile> accounts = SipProfile.getAllProfiles(getContext(), onlyActive,
                new String[] {
                        SipProfile.FIELD_ID,
                        SipProfile.FIELD_ACC_ID,
                        SipProfile.FIELD_ACTIVE,
                        SipProfile.FIELD_DISPLAY_NAME,
                        SipProfile.FIELD_WIZARD
                });
        // And all external call handlers
        Map<String, String> externalHandlers;
        if(loadCallHandlerPlugins) {
            externalHandlers = CallHandlerPlugin.getAvailableCallHandlers(getContext());
        }else {
            externalHandlers = new HashMap<String, String>();
        }
        if(TextUtils.isEmpty(numberToCall)) {
            // In case of empty number to call, just add everything without any other question
            for(SipProfile acc : accounts) {
                prefinalAccounts.add(new FilteredProfile(acc, false));
            }
            for(Entry<String, String> extEnt : externalHandlers.entrySet() ) {
                prefinalAccounts.add(new FilteredProfile(extEnt.getKey(), false));
                
            }
        }else {
            // If there is a number to call, add only those callable, and flag must call entries
            // If one must call entry is found per group, just stop looping, and don't add other from the group.
            // Note that we keep processing external call handlers voluntarily cause we may encounter a sip account that doesn't register
            // But is in force call mode
            for(SipProfile acc : accounts) {
                if(Filter.isCallableNumber(getContext(), acc.id, numberToCall)) {
                    boolean forceCall = Filter.isMustCallNumber(getContext(), acc.id, numberToCall);
                    prefinalAccounts.add(new FilteredProfile(acc, forceCall));
                    if(forceCall) {
                        break;
                    }
                }
            }
            for(Entry<String, String> extEnt : externalHandlers.entrySet() ) {
                long accId = CallHandlerPlugin.getAccountIdForCallHandler(getContext(), extEnt.getKey());
                if(Filter.isCallableNumber(getContext(), accId, numberToCall)) {
                    boolean forceCall = Filter.isMustCallNumber(getContext(), accId, numberToCall);
                    prefinalAccounts.add(new FilteredProfile(extEnt.getKey(), forceCall));
                    if(forceCall) {
                        break;
                    }
                }
            }
            
        }
        
        
        // Build final cursor based on final filtered accounts
        Cursor[] cursorsToMerge = new Cursor[prefinalAccounts.size()];
        int i = 0;
        for (FilteredProfile acc : prefinalAccounts) {
            cursorsToMerge[i++] = createCursorForAccount(acc);
        }

        
        if(cursorsToMerge.length > 0) {
            MergeCursor mg = new MergeCursor(cursorsToMerge);
            mg.registerContentObserver(loaderObserver);
            finalAccounts = prefinalAccounts;
            return mg;
        }else {
            finalAccounts = prefinalAccounts;
            return null;
        }
    }
    

    /**
     * Class to hold information about a possible call handler entry.
     * This could be either a sip profile or a call handler plugin
     */
    private class FilteredProfile {
        /**
         * Sip profile constructor.
         * To use when input is a sip profile
         * @param acc The corresponding sip profile
         * @param forceCall The force call flag in current context.
         */
        public FilteredProfile(SipProfile acc, boolean forceCall) {
            account = acc;
            isForceCall = forceCall;
            AccountStatusDisplay displayState = AccountListUtils.getAccountDisplay(getContext(), acc.id);
            statusColor = displayState.statusColor;
            statusForOutgoing = displayState.availableForCalls;
            callHandlerPlugin = null;
        }
        
        /**
         * Call handler plugin constructor.
         * To use when input is a call handler plugin.
         * @param componentName The component name of the plugin
         * @param forceCall The force call flag in current context.
         */
        public FilteredProfile(String componentName, boolean forceCall) {
            account = new SipProfile();
            long accId = CallHandlerPlugin.getAccountIdForCallHandler(getContext(), componentName);
            account.id = accId;
            account.wizard = "EXPERT";
            CallHandlerPlugin ch = new CallHandlerPlugin(getContext());
            final Semaphore semaphore = new Semaphore(0);
            
            String toCall = numberToCall;
            if(!ignoreRewritting) {
                toCall = Filter.rewritePhoneNumber(getContext(), accId, numberToCall);
            }
            ch.loadFrom(accId, toCall, new OnLoadListener() {
                
                @Override
                public void onLoad(CallHandlerPlugin ch) {
                    semaphore.release();
                }
            });
            
            try {
                semaphore.tryAcquire(3L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.e(THIS_FILE, "Not possible to bind callhandler plugin");
            }
            account.display_name = ch.getLabel();
            account.icon = ch.getIcon();
            
            isForceCall = forceCall;
            statusColor = getContext().getResources().getColor(android.R.color.white);
            statusForOutgoing = true;
            callHandlerPlugin = ch;
        }
        
        final SipProfile account;
        final boolean isForceCall;
        final private boolean statusForOutgoing;
        final private int statusColor;
        final CallHandlerPlugin callHandlerPlugin;
        
        /**
         * Rewrite a number for this calling entry
         * @param number The number to rewrite
         * @return Rewritten number.
         */
        public String rewriteNumber(String number) {
            if(ignoreRewritting) {
                return number;
            }else {
                return Filter.rewritePhoneNumber(getContext(), account.id, number);
            }
        }
        
        /**
         * Is the account available for outgoing calls
         * @return True if a call can be made using this calling entry
         */
        public boolean getStatusForOutgoing() {
            return statusForOutgoing;
        }
        
        /**
         * The color representing the calling entry status. green for registered
         * sip accounts, red for invalid sip accounts, orange for sip accounts
         * with ongoing registration, white for call handler plugins
         * 
         * @return the color for this entry status.
         */
        public int getStatusColor() {
            return statusColor;
        }
        
        /**
         * Get the eventual associated call handler plugin object.
         * 
         * @return The call handler plugin object if any associated to this
         *         calling entry. Null if representing a sip account.
         */
        public CallHandlerPlugin getCallHandlerPlugin() {
            return callHandlerPlugin;
        }
        
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation here just adds a
     * little more logic.
     */
    @Override
    public void deliverResult(Cursor c) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We
            // don't need the result.
            if (currentResult != null) {
                onReleaseResources(currentResult);
            }
        }
        
        currentResult = c;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(c);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (currentResult != null && !takeContentChanged()) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(currentResult);
        }else {
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(Cursor c) {
        super.onCanceled(c);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(c);
        
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (currentResult != null) {
            onReleaseResources(currentResult);
            currentResult = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set.
     */
    protected void onReleaseResources(Cursor c) {
        if(c != null) {
            c.unregisterContentObserver(loaderObserver);
            c.close();
        }
        if(loadStatus) {
            getContext().getContentResolver().unregisterContentObserver(loaderObserver);
        }
    }

    
    private static String[] COLUMN_HEADERS = new String[] {
            BaseColumns._ID,
            SipProfile.FIELD_ID,
            SipProfile.FIELD_DISPLAY_NAME,
            SipProfile.FIELD_WIZARD,
            FIELD_FORCE_CALL,
            FIELD_NBR_TO_CALL,
            FIELD_STATUS_OUTGOING,
            FIELD_STATUS_COLOR
    };

    /**
     * Creates a cursor that contains a single row and maps the section to the
     * given value.
     */
    private Cursor createCursorForAccount(FilteredProfile fa) {
        MatrixCursor matrixCursor = new MatrixCursor(COLUMN_HEADERS);
        
        
        matrixCursor.addRow(new Object[] {
                fa.account.id,
                fa.account.id,
                fa.account.display_name,
                fa.account.wizard,
                fa.isForceCall ? 1 : 0,
                fa.rewriteNumber(numberToCall),
                fa.getStatusForOutgoing() ? 1 : 0,
                fa.getStatusColor()
        });
        return matrixCursor;
    }
    
    /**
     * Get the cached call handler plugin loaded for a given position
     * @param position The position to search at
     * @return The call handler plugin if any for this position
     */
    public CallHandlerPlugin getCallHandlerWithAccountId(long accId) {
        for(FilteredProfile filteredAcc :finalAccounts) {
            if(filteredAcc.account.id == accId)
            return filteredAcc.getCallHandlerPlugin();
        }
        return null;
    }


}
