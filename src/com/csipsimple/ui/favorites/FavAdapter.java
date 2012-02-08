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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.contacts.ContactsWrapper;
import com.csipsimple.wizards.WizardUtils;

public class FavAdapter extends ResourceCursorAdapter {

    //private static final String THIS_FILE = "FavAdapter";

    public FavAdapter(Context context, Cursor c) {
        super(context, R.layout.fav_list_item, c, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ContentValues cv = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, cv);
        
        int type = ContactsWrapper.TYPE_CONTACT;
        if(cv.containsKey(ContactsWrapper.FIELD_TYPE)) {
            type = cv.getAsInteger(ContactsWrapper.FIELD_TYPE);
        }
        
        if(type == ContactsWrapper.TYPE_GROUP) {
            showViewForHeader(view, true);
            TextView tv = (TextView) view.findViewById(R.id.header_text);
            ImageView icon = (ImageView) view.findViewById(R.id.header_icon);
            PresenceStatusSpinner presSpinner = (PresenceStatusSpinner) view.findViewById(R.id.header_presence_spinner);
            
            tv.setText(cv.getAsString(SipProfile.FIELD_DISPLAY_NAME));
            icon.setImageResource(WizardUtils.getWizardIconRes(cv.getAsString(SipProfile.FIELD_WIZARD)));
            presSpinner.setProfileId(cv.getAsLong(BaseColumns._ID));
            
        }else {
            showViewForHeader(view, false);
            ContactsWrapper.getInstance().bindContactView(view, context, cursor);
            
        }
    }
    
    private void showViewForHeader(View view, boolean isHeader) {

        view.findViewById(R.id.header_view).setVisibility(isHeader ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.contact_view).setVisibility(isHeader ? View.GONE : View.VISIBLE );
    }

}

