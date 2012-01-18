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
/**
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 */

package com.csipsimple.utils.contacts;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;

/**
 * This adapter is used to filter contacts on both name and number.
 */
public class ContactsSearchAdapter extends CursorAdapter {

    private final Context mContext;
    private long currentAccId = SipProfile.INVALID_ID;
    
    public ContactsSearchAdapter(Context context) {
        // Note that the RecipientsAdapter doesn't support auto-requeries. If we
        // want to respond to changes in the contacts we're displaying in the drop-down,
        // code using this adapter would have to add a line such as:
        //   mRecipientsAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        // See MessageFragment for an example.
        super(context, null, false /* no auto-requery */);
        mContext = context;
    }
    
    public final void setSelectedAccount(long accId) {
    	currentAccId = accId;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.search_contact_list_item, parent, false);
    }
    
    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
    	ContactsWrapper.getInstance().bindAutoCompleteView(view, context, cursor);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return ContactsWrapper.getInstance().searchContact(mContext, constraint);
    }
    
    @Override
    public final CharSequence convertToString(Cursor cursor) {
    	CharSequence number = ContactsWrapper.getInstance().transformToSipUri(mContext, cursor);
    	if(number != null) {
    		SipProfile account = new SipProfile();
    		// We only need the id
    		account.id = currentAccId;
			DBAdapter db = new DBAdapter(mContext);
			String rewritten = Filter.rewritePhoneNumber(account, number.toString(), db);
			return rewritten;
    	}
    	return number;
    }

    
    
}

