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

package com.csipsimple.ui.favorites;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.content.AsyncTaskLoader;

import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.contacts.ContactsWrapper;

import java.util.ArrayList;

public class FavLoader extends AsyncTaskLoader<Cursor> {

    private Cursor currentResult;

    public FavLoader(Context context) {
        super(context);
    }
    
    Handler mHandler = new Handler();

    @Override
    public Cursor loadInBackground() {
        // First of all, get all active accounts
        ArrayList<SipProfile> accounts = SipProfile.getAllProfiles(getContext(), true);

        Cursor[] cursorsToMerge = new Cursor[2 * accounts.size()];
        int i = 0;
        for (SipProfile acc : accounts) {
            cursorsToMerge[i++] = createHeaderCursorFor(acc);
            cursorsToMerge[i++] = createContentCursorFor(acc);
            
        }
        if(cursorsToMerge.length > 0) {
            MergeCursor mg = new MergeCursor(cursorsToMerge);
            mg.registerContentObserver(new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    onContentChanged();
                }
            });
            return mg;
        }else {
            return null;
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
        if (currentResult != null) {
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
        c.close();
    }

    /**
     * Creates a cursor that contains a single row and maps the section to the
     * given value.
     */
    private Cursor createHeaderCursorFor(SipProfile account) {
        MatrixCursor matrixCursor =
                new MatrixCursor(new String[] {
                        BaseColumns._ID, 
                        ContactsWrapper.FIELD_TYPE,
                        SipProfile.FIELD_DISPLAY_NAME,
                        SipProfile.FIELD_WIZARD
                });
        matrixCursor.addRow(new Object[] {
                account.id,
                ContactsWrapper.TYPE_GROUP,
                account.display_name,
                account.wizard
        });
        return matrixCursor;
    }

    /**
     * Creates a cursor that contains contacts group corresponding to an sip
     * account.
     */
    private Cursor createContentCursorFor(SipProfile account) {
        return ContactsWrapper.getInstance().getContactsByGroup(getContext(), account.display_name);
    }

}
