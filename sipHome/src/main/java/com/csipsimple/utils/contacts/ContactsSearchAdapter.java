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
import android.database.CursorIndexOutOfBoundsException;
import android.support.v4.widget.CursorAdapter;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;

/**
 * This adapter is used to filter contacts on both name and number.
 */
public class ContactsSearchAdapter extends CursorAdapter implements SectionIndexer {

    private final Context mContext;
    private long currentAccId = SipProfile.INVALID_ID;
    AlphabetIndexer alphaIndexer;
    private String currentFilter = "";
    private CharacterStyle boldStyle = new StyleSpan(android.graphics.Typeface.BOLD);
    private CharacterStyle highlightStyle = new ForegroundColorSpan(0xFF33B5E5);
    
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
    public final void setSelectedText(String txt) {
        if(!TextUtils.isEmpty(txt)) {
            currentFilter  = txt.toLowerCase();            
        }else {
            currentFilter = "";
        }
        if (TextUtils.isEmpty(txt) || txt.length() >= 2) {
            getFilter().filter(txt);
        }else {
            currentFilter = "";
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.search_contact_list_item, parent, false);
    }
    
    private boolean highlightTextViewSearch(TextView tv) {
        if(currentFilter.length() > 0) {
            String value = tv.getText().toString();
            int foundIdx = value.toLowerCase().indexOf(currentFilter);
            if(foundIdx >= 0) {
                SpannableString spn = new SpannableString(value);
                spn.setSpan(boldStyle, foundIdx, foundIdx + currentFilter.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spn.setSpan(highlightStyle, foundIdx, foundIdx + currentFilter.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                tv.setText(spn);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
    	ContactsWrapper.getInstance().bindContactPhoneView(view, context, cursor);
    	highlightTextViewSearch((TextView) view.findViewById(R.id.name));
        highlightTextViewSearch((TextView) view.findViewById(R.id.number));
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        Cursor c = ContactsWrapper.getInstance().getContactsPhones(mContext, constraint);
        
        if(alphaIndexer == null) {
            alphaIndexer = new AlphabetIndexer(c, ContactsWrapper.getInstance().getContactIndexableColumnIndex(c),
                    " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }else {
            alphaIndexer.setCursor(c);
        }
        return c;
    }
    
    @Override
    public final CharSequence convertToString(Cursor cursor) {
    	CharSequence number = ContactsWrapper.getInstance().transformToSipUri(mContext, cursor);
        boolean isExternalPhone = ContactsWrapper.getInstance().isExternalPhoneNumber(mContext, cursor);
    	if(!TextUtils.isEmpty(number) && isExternalPhone) {
    	    String stripNbr = PhoneNumberUtils.stripSeparators(number.toString());
			return Filter.rewritePhoneNumber(mContext, currentAccId, stripNbr);
    	}
    	return number;
    }

    @Override
    public int getPositionForSection(int section) {
        if(alphaIndexer != null) {
            try {
                return alphaIndexer.getPositionForSection(section);
            }catch(CursorIndexOutOfBoundsException e) {
                // Not a problem we are just not yet init
            }
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        if(alphaIndexer != null) {
            try {
                return alphaIndexer.getSectionForPosition(position);
            }catch(CursorIndexOutOfBoundsException e) {
                // Not a problem we are just not yet init
            }
        }
        return 0;
    }

    @Override
    public Object[] getSections() {
        if(alphaIndexer != null) {
            return alphaIndexer.getSections();
        }
        return null;
    }
    
}

