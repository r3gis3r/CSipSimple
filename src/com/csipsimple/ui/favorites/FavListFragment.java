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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.csipsimple.R;
import com.csipsimple.ui.SipHome.ViewPagerVisibilityListener;
import com.csipsimple.utils.contacts.ContactsWrapper;

public class FavListFragment extends ListFragment implements ViewPagerVisibilityListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    

    private FavAdapter mAdapter;
    private boolean mDualPane;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Adapter
        mAdapter = new FavAdapter(getActivity(), null);
        setListAdapter(mAdapter);
        
        // Start loading
        getLoaderManager().initLoader(0, null, this);
        
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

        // Start out with a progress indicator.
        // setListShown(false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        
        return null;//ContactsWrapper.getInstance().getContactByGroupCursorLoader(getActivity(), "Contacts");
    }

    @Override
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

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.changeCursor(null);
        
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        // TODO Auto-generated method stub
        
    }

}
