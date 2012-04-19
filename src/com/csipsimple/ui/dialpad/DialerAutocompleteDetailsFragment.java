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

package com.csipsimple.ui.dialpad;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.contacts.ContactsSearchAdapter;

public class DialerAutocompleteDetailsFragment extends SherlockListFragment {
    private ContactsSearchAdapter autoCompleteAdapter;
    private CharSequence constraint = "";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        autoCompleteAdapter = new ContactsSearchAdapter(getActivity());
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setListAdapter(autoCompleteAdapter);
        proposeRestoreFromBundle(savedInstanceState);
        proposeRestoreFromBundle(getArguments());
    }
    
    private void proposeRestoreFromBundle(Bundle b) {
        if(b != null && b.containsKey(EXTRA_FILTER_CONSTRAINT)) {
            filter(b.getCharSequence(EXTRA_FILTER_CONSTRAINT));
        }
    }
    
    public final static String EXTRA_FILTER_CONSTRAINT = "constraint";
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(EXTRA_FILTER_CONSTRAINT, constraint);
        super.onSaveInstanceState(outState);
    }
    

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Object selectedItem = autoCompleteAdapter.getItem(position);
        if (selectedItem != null) {

            // Well that a little bit too direct it should be more a listener
            // But not sure how fragments will behaves on restore for now
            Activity superAct = getActivity();
            if (superAct instanceof SipHome) {
                Fragment frag = ((SipHome) superAct).getCurrentFragment();
                if (frag != null && frag instanceof DialerFragment) {
                    ((DialerFragment) frag).setTextFieldValue(autoCompleteAdapter.getFilter()
                            .convertResultToString(selectedItem));
                }
            }
        }
    }

    /**
     * Filter the query to some filter string
     * 
     * @param constraint the string to filter on
     */
    public void filter(CharSequence constraint) {
        autoCompleteAdapter.getFilter().filter(constraint);
        this.constraint = constraint;
    }
}
