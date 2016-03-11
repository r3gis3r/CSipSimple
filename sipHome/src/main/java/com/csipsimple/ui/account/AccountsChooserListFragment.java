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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.widgets.CSSListFragment;
import com.csipsimple.wizards.WizardUtils;

public class AccountsChooserListFragment extends CSSListFragment {
    
    private AccountsChooserAdapter mAdapter;
    private AccountsLoader accLoader;

    private Integer INDEX_DISPLAY_NAME = null;
    private Integer INDEX_WIZARD = null;
    private Integer INDEX_ID = null;
    

    private void initIndexes(Cursor c) {
        if(INDEX_DISPLAY_NAME == null) {
            INDEX_ID = c.getColumnIndex(SipProfile.FIELD_ID);
            INDEX_DISPLAY_NAME = c.getColumnIndex(SipProfile.FIELD_DISPLAY_NAME);
            INDEX_WIZARD = c.getColumnIndex(SipProfile.FIELD_WIZARD);
        }
    }
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        attachAdapter();
        getLoaderManager().initLoader(0, getArguments(), this);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        return inflater.inflate(R.layout.accounts_chooser_fragment, container, false);
    }

    private void attachAdapter() {
        if(getListAdapter() == null) {
            if(mAdapter == null) {
                mAdapter = new AccountsChooserAdapter(getActivity(), null);
            }
            setListAdapter(mAdapter);
        }
    }
    

    @Override
    public Loader<Cursor> onCreateLoader(int loader, Bundle args) {
        accLoader = new AccountsLoader(getActivity(), false, showExternal);
        return accLoader;
        
    }
    

    @Override
    public void changeCursor(Cursor c) {

        // Set adapter content if nothing to force was found
        if(mAdapter != null) {
            mAdapter.changeCursor(c);
        }
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(mAdapter != null && accListener != null) {
            Cursor c = (Cursor) mAdapter.getItem(position);
            initIndexes(c);
            long accId = c.getLong(INDEX_ID);
            String displayName = c.getString(INDEX_DISPLAY_NAME);
            String wizard = c.getString(INDEX_WIZARD);
            accListener.onAccountClicked(accId, displayName, wizard);
        }
    }
    
    public interface OnAccountClickListener {
        /**
         * Fired when an account row is cliked
         * @param accountId the id of the clicked account
         * @param name the display name of the clicked account
         * @param wizard the wizard of the clicked account
         */
        public void onAccountClicked(long accountId, String name, String wizard); 
    }
    private OnAccountClickListener accListener = null;
    public void setOnAccountClickListener(OnAccountClickListener l) {
        accListener = l;
    }
    
    private class AccountsChooserAdapter extends ResourceCursorAdapter {

        public AccountsChooserAdapter(Context context, Cursor c) {
            super(context, R.layout.accounts_chooser_list_item, c, 0);
        }
        

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            // Shortcut for the binding
            if(v.getTag() == null) {
                AccListItemViewTag tag = new AccListItemViewTag();
                tag.name = (TextView) v.findViewById(R.id.AccTextView);
                tag.icon = (ImageView) v.findViewById(R.id.wizard_icon);
                v.setTag(tag);
            }
            return v;
        }
        
        private class AccListItemViewTag {
            TextView name;
            ImageView icon;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            AccListItemViewTag tag = (AccListItemViewTag) view.getTag();
            if(tag != null) {
                initIndexes(cursor);
                Long accId = cursor.getLong(INDEX_ID);
                String name = cursor.getString(INDEX_DISPLAY_NAME);
                String wizard = cursor.getString(INDEX_WIZARD);
                
                tag.name.setText(name);
                
                boolean iconSet = false;
                if(accLoader != null) {
                   CallHandlerPlugin ch = accLoader.getCallHandlerWithAccountId(accId);
                   if(ch != null) {
                       tag.icon.setImageBitmap(ch.getIcon());
                       iconSet = true;
                   }
                }
                
                if(!iconSet){
                    tag.icon.setImageResource(WizardUtils.getWizardIconRes(wizard));
                }
            }
        }
    }
    
    private boolean showExternal;

    public void setShowCallHandlerPlugins(boolean showInternalAccounts) {
        showExternal = showInternalAccounts;
    }

}
