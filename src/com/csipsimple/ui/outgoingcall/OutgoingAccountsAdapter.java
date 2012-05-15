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

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsLoader;
import com.csipsimple.utils.CallHandlerPlugin;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.wizards.WizardUtils;

import java.lang.reflect.Method;

public class OutgoingAccountsAdapter extends ResourceCursorAdapter {

    private final OutgoingCallListFragment fragment;
    public OutgoingAccountsAdapter(OutgoingCallListFragment aFragment, Cursor c) {
        super(aFragment.getActivity(), R.layout.outgoing_account_list_item, c, 0);
        fragment = aFragment;
    }

    
    private Integer INDEX_DISPLAY_NAME = null;
    private Integer INDEX_WIZARD = null;
    private Integer INDEX_NBR = null;
    private Integer INDEX_STATUS_FOR_OUTGOING = null;
    private Integer INDEX_STATUS_COLOR = null;
    private Integer INDEX_ID = null;
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        AccListItemViewTag tag = (AccListItemViewTag) view.getTag();
        if(tag != null) {
            initIndexes(cursor);
            long accId = cursor.getLong(INDEX_ID);
            String name = cursor.getString(INDEX_DISPLAY_NAME);
            String wizard = cursor.getString(INDEX_WIZARD);
            String nbr = cursor.getString(INDEX_NBR);
            int color = cursor.getInt(INDEX_STATUS_COLOR);
            boolean enabled = cursor.getInt(INDEX_STATUS_FOR_OUTGOING) == 1;
            
            tag.name.setText(name);
            tag.name.setTextColor(color);
            tag.status.setText(context.getString(R.string.call) + " : " + nbr);
            
            setRowViewAlpha(view, enabled ? 1.0f : 0.3f);

            boolean iconSet = false;
            AccountsLoader accLoader = fragment.getAccountLoader();
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
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = super.newView(context, cursor, parent);
        // Shortcut for the binding
        if(v.getTag() == null) {
            AccListItemViewTag tag = new AccListItemViewTag();
            tag.name = (TextView) v.findViewById(R.id.AccTextView);
            tag.status = (TextView) v.findViewById(R.id.AccTextStatusView);
            tag.icon = (ImageView) v.findViewById(R.id.wizard_icon);
            v.setTag(tag);
        }
        return v;
    }
    
    private class AccListItemViewTag {
        TextView name;
        TextView status;
        ImageView icon;
    }
    
    private void initIndexes(Cursor c) {
        if(INDEX_DISPLAY_NAME == null) {
            INDEX_ID = c.getColumnIndex(SipProfile.FIELD_ID);
            INDEX_DISPLAY_NAME = c.getColumnIndex(SipProfile.FIELD_DISPLAY_NAME);
            INDEX_WIZARD = c.getColumnIndex(SipProfile.FIELD_WIZARD);
            INDEX_NBR = c.getColumnIndex(AccountsLoader.FIELD_NBR_TO_CALL);
            INDEX_STATUS_COLOR = c.getColumnIndex(AccountsLoader.FIELD_STATUS_COLOR);
            INDEX_STATUS_FOR_OUTGOING = c.getColumnIndex(AccountsLoader.FIELD_STATUS_OUTGOING);
        }
    }
    

    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        Cursor c = (Cursor) getItem(position);
        initIndexes(c);
        return c.getInt(INDEX_STATUS_FOR_OUTGOING) == 1;
    }

    private static Method setAlphaMethod = null;
    
    private void setRowViewAlpha(View v, float alpha) {
        if(Compatibility.isCompatible(11)) {
            // In honeycomb or upper case, use the new setAlpha method
            if(setAlphaMethod == null) {
                try {
                    setAlphaMethod = View.class.getDeclaredMethod("setAlpha", float.class);
                } catch (NoSuchMethodException e) {
                    // Ignore not found set alpha class.
                }
            }
            if(setAlphaMethod != null) {
                UtilityWrapper.safelyInvokeMethod(setAlphaMethod, v, alpha);
            }
        }else {
            // Try to set alpha on each component
            TextView tv;
            tv = (TextView) v.findViewById(R.id.AccTextView);
            tv.setTextColor(tv.getTextColors().withAlpha((int)(255 * alpha)));
            tv = (TextView) v.findViewById(R.id.AccTextStatusView);
            tv.setTextColor(tv.getTextColors().withAlpha((int)(255 * alpha)));
            
            ImageView img = (ImageView) v.findViewById(R.id.wizard_icon);
            img.setAlpha((int)(255 * alpha));
        }
    }
}
