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

package com.csipsimple.widgets;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.actionbarsherlock.app.SherlockListFragment;
import com.csipsimple.R;

/**
 * Helper class for list fragments.<br/>
 * This takes in charge of cursor callbacks by forwarding to {@link #changeCursor(Cursor)}.<br/>
 * It also takes in charge to retrieve and update progress indicator. Custom views for this must contains : 
 * <ul>
 * <li>{@link R.id.listContainer} to wrap list</li>
 * <li>{@link R.id.progressContainer} for progress indicator</li>
 * </ul> 
 * @author r3gis3r
 *
 */
public abstract class CSSListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    // Override set list shown
    

    private View mListContainer = null;
    private View mProgressContainer = null;
    private boolean mListShown = false;
    @Override
    public void setListShown(boolean shown) {
        setListShown(shown, true);
    }
    
    @Override
    public void setListShownNoAnimation(boolean shown) {
        setListShown(shown, false);
    }
    
    private void setListShown(boolean shown, boolean animate) {
        ensureCustomList();
        if(mListShown == shown) {
            return;
        }
        mListShown = shown;
        if(mListContainer != null && mProgressContainer != null) {
            if(shown) {
                if(animate) {
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                }
                mListContainer.setVisibility(View.VISIBLE);
                mProgressContainer.setVisibility(View.GONE);
            }else {
                if(animate) {
                    mListContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
                }
                mListContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.VISIBLE);
            }
        }
    }
    
    /**
     * Make sure our private reference to views are correct. 
     */
    private void ensureCustomList() {
        if(mListContainer != null) {
            return;
        }
        mListContainer = getView().findViewById(R.id.listContainer);
        mProgressContainer = getView().findViewById(R.id.progressContainer);
    }

    public abstract Loader<Cursor> onCreateLoader(int loader, Bundle args);
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        changeCursor(data);
        if(isResumed()) {
            setListShown(true);
        }else {
            setListShownNoAnimation(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        changeCursor(null);
    }
    
    /**
     * Request a cursor change to the adapter. <br/>
     * To be implemented by extenders.
     * @param c the new cursor to replace the old one
     */
    public abstract void changeCursor(Cursor c);
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // When we will recycle this view, the stored shown and list containers becomes invalid
        mListShown = false;
        mListContainer = null;
        super.onActivityCreated(savedInstanceState);
    }
}
