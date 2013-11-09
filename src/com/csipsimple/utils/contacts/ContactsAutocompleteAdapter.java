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
import android.text.TextUtils;
import android.view.View;
import android.widget.ResourceCursorAdapter;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;

/**
 * This adapter is used to filter contacts on both name and number.
 */
public class ContactsAutocompleteAdapter extends ResourceCursorAdapter {

    private final Context mContext;
    private long currentAccId = SipProfile.INVALID_ID;
    
    public ContactsAutocompleteAdapter(Context context) {
        // Note that the RecipientsAdapter doesn't support auto-requeries. If we
        // want to respond to changes in the contacts we're displaying in the drop-down,
        // code using this adapter would have to add a line such as:
        //   mRecipientsAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        // See MessageFragment for an example.
        super(context, R.layout.recipient_filter_item, null, false /* no auto-requery */);
        mContext = context;
    }
    
    public final void setSelectedAccount(long accId) {
    	currentAccId = accId;
    }

    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
    	ContactsWrapper.getInstance().bindContactPhoneView(view, context, cursor);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return ContactsWrapper.getInstance().getContactsPhones(mContext, constraint);
    }
    
    @Override
    public final CharSequence convertToString(Cursor cursor) {
    	CharSequence number = ContactsWrapper.getInstance().transformToSipUri(mContext, cursor);
        boolean isExternalPhone = ContactsWrapper.getInstance().isExternalPhoneNumber(mContext, cursor);
    	if(!TextUtils.isEmpty(number) && isExternalPhone) {
			return Filter.rewritePhoneNumber(mContext, currentAccId, number.toString());
    	}
    	return number;
    }
    
}

