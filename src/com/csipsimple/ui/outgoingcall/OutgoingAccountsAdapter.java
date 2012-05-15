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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.ui.account.AccountsLoader;
import com.csipsimple.wizards.WizardUtils;

public class OutgoingAccountsAdapter extends ResourceCursorAdapter {

    public OutgoingAccountsAdapter(Context context, Cursor c) {
        super(context, R.layout.outgoing_account_list_item, c, 0);
    }

    
    private Integer INDEX_DISPLAY_NAME = null;
    private Integer INDEX_WIZARD = null;
    private Integer INDEX_ICON = null;
    private Integer INDEX_NBR = null;
    private Integer INDEX_STATUS_FOR_OUTGOING = null;
    private Integer INDEX_STATUS_COLOR = null;
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        AccListItemViewTag tag = (AccListItemViewTag) view.getTag();
        if(tag != null) {
            initIndexes(cursor);
            String name = cursor.getString(INDEX_DISPLAY_NAME);
            String wizard = cursor.getString(INDEX_WIZARD);
            String nbr = cursor.getString(INDEX_NBR);
            int color = cursor.getInt(INDEX_STATUS_COLOR);
            boolean enabled = cursor.getInt(INDEX_STATUS_FOR_OUTGOING) == 1;
            
            tag.name.setText(name);
            tag.name.setTextColor(color);
            tag.status.setText(context.getString(R.string.call) + " : " + nbr);
            
            // TODO : set alpha cross 
            //view.setAlpha(enabled ? 1.0f : 0.3f);
            
            byte[] iconBlob = cursor.getBlob(INDEX_ICON);
            if(iconBlob.length > 0) {
                Bitmap bmp = BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
                tag.icon.setImageBitmap(bmp);
            }else {
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
            INDEX_DISPLAY_NAME = c.getColumnIndex(SipProfile.FIELD_DISPLAY_NAME);
            INDEX_WIZARD = c.getColumnIndex(SipProfile.FIELD_WIZARD);
            INDEX_ICON = c.getColumnIndex(AccountsLoader.FIELD_ICON);
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

}
