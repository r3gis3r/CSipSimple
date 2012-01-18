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


package com.csipsimple.ui.messages;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipUri;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.widgets.contactbadge.QuickContactBadge;

public class ConverstationAdapter extends SimpleCursorAdapter {

    public ConverstationAdapter(Context context, Cursor c) {
        super(context, R.layout.conversation_list_item, c, new String[] {
                SipMessage.FIELD_BODY
        },
                new int[] {
                        R.id.subject
                }, 0);
    }

    public static final class ConversationListItemViews {
        TextView fromView;
        TextView dateView;
        QuickContactBadge quickContactView;
        int position;
        String to;
        String from;
        String fromFull;
        
        String getRemoteNumber() {
            String number = from;
            if (SipMessage.SELF.equals(number)) {
                number = to;
            }
            return number;
        }
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);
        ConversationListItemViews tagView = new ConversationListItemViews();
        tagView.fromView = (TextView) view.findViewById(R.id.from);
        tagView.dateView = (TextView) view.findViewById(R.id.date);
        tagView.quickContactView = (QuickContactBadge) view.findViewById(R.id.quick_contact_photo);
        view.setTag(tagView);
        //view.setOnClickListener(mPrimaryActionListener);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        final ConversationListItemViews tagView = (ConversationListItemViews) view.getTag();
        String nbr = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM));
        String fromFull = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM_FULL));
        String to_number = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_TO));
        
        //int read = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_READ));
        long date = cursor.getLong(cursor.getColumnIndex(SipMessage.FIELD_DATE));
        
        
        tagView.fromFull = fromFull;
        tagView.to = to_number;
        tagView.from = nbr;
        tagView.position = cursor.getPosition();
        
        
        /*
        Drawable background = (read == 0)?
                context.getResources().getDrawable(R.drawable.conversation_item_background_unread) :
                context.getResources().getDrawable(R.drawable.conversation_item_background_read);
        
        view.setBackgroundDrawable(background);
         */
        String number = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM_FULL));
        CallerInfo info = CallerInfo.getCallerInfoFromSipUri(mContext, number);
        
        /*
        final Uri lookupUri = info.contactContentUri;
        final String name = info.name;
        final int ntype = info.numberType;
        final String label = info.phoneLabel;
        CharSequence formattedNumber = SipUri.getCanonicalSipContact(number, false);
        */
        
        
        // Photo
        tagView.quickContactView.assignContactUri(info.contactContentUri);
        ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(mContext, 
                tagView.quickContactView.getImageView(),
                info,
                SipHome.USE_LIGHT_THEME ? R.drawable.ic_contact_picture_holo_light
                        : R.drawable.ic_contact_picture_holo_dark);

        // From
        tagView.fromView.setText(formatMessage(cursor));

        //Date
        // Set the date/time field by mixing relative and absolute times.
        int flags = DateUtils.FORMAT_ABBREV_RELATIVE;
        tagView.dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags));
    }
    
    
    

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    private CharSequence formatMessage(Cursor cursor) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        /*
        String remoteContact = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM));
        if (remoteContact.equals("SELF")) {
            remoteContact = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_TO));
            buf.append("To: ");
        }
        */
        String remoteContactFull = cursor.getString(cursor.getColumnIndex(SipMessage.FIELD_FROM_FULL));
        buf.append(SipUri.getDisplayedSimpleContact(remoteContactFull));
        
        int counter = cursor.getInt(cursor.getColumnIndex("counter"));
        if (counter > 1) {
            buf.append(" (" + counter + ") ");
        }
       

        int read = cursor.getInt(cursor.getColumnIndex(SipMessage.FIELD_READ));
        // Unread messages are shown in bold
        if (read == 0) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return buf;
    }
}
