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

package com.csipsimple.utils.contacts;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.support.v4.content.Loader;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ContactsUtils3 extends ContactsWrapper {

    public static final int CONTACT_ID_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int NUMBER_INDEX = 3;
    public static final int LABEL_INDEX = 4;
    public static final int NAME_INDEX = 5;

    protected static final String PROJECTION_CONTACTS[] = {
            Contacts.People._ID,
            Contacts.People.DISPLAY_NAME
    };

    private static final String[] PROJECTION_PHONE = {
            Contacts.Phones._ID, // 0
            Contacts.Phones.PERSON_ID, // 1
            Contacts.Phones.TYPE, // 2
            Contacts.Phones.NUMBER, // 3
            Contacts.Phones.LABEL, // 4
            Contacts.Phones.DISPLAY_NAME, // 5
    };

    private static final String SORT_ORDER = Contacts.Phones.DISPLAY_NAME + " ASC,"
            + Contacts.Phones.TYPE;
    private static final String THIS_FILE = "ContactsUtils3";

    @Override
    public Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource) {
        Bitmap img = null;
        try {
            img = People.loadContactPhoto(ctxt, uri, defaultResource, null);
        } catch (IllegalArgumentException e) {
            Log.w("Contact3", "Failed to find contact photo");
        }
        return img;
    }

    public List<Phone> getPhoneNumbers(Context ctxt, String id) {
        ArrayList<Phone> phones = new ArrayList<Phone>();

        Cursor pCur = ctxt.getContentResolver().query(
                Contacts.Phones.CONTENT_URI,
                null,
                Contacts.Phones.PERSON_ID + " = ?",
                new String[] {
                    id
                }, null);
        while (pCur.moveToNext()) {
            phones.add(new Phone(
                    pCur.getString(pCur.getColumnIndex(Contacts.Phones.NUMBER))
                    , pCur.getString(pCur.getColumnIndex(Contacts.Phones.TYPE))
                    ));
        }
        pCur.close();
        return (phones);
    }

    @Override
    public Cursor searchContact(Context ctxt, CharSequence constraint) {
        String phone = "";
        String cons = "";

        if (constraint != null) {
            cons = constraint.toString();

            if (usefulAsDigits(cons)) {
                phone = PhoneNumberUtils.convertKeypadLettersToDigits(cons);
                if (phone.equals(cons)) {
                    phone = "";
                } else {
                    phone = phone.trim();
                }
            }
        }

        Uri uri = Uri.withAppendedPath(Contacts.Phones.CONTENT_URI, "");
        /*
         * if we decide to filter based on phone types use a selection like
         * this. String selection = String.format("%s=%s OR %s=%s OR %s=%s",
         * Phone.TYPE, Phone.TYPE_MOBILE, Phone.TYPE, Phone.TYPE_WORK_MOBILE,
         * Phone.TYPE, Phone.TYPE_MMS);
         */
        String selection = String.format("%s LIKE ? OR %s LIKE ?", Contacts.Phones.NUMBER,
                Contacts.Phones.DISPLAY_NAME);
        Cursor phoneCursor =
                ctxt.getContentResolver().query(uri,
                        PROJECTION_PHONE,
                        selection,
                        new String[] {
                                cons + "%", "%" + cons + "%"
                        },
                        SORT_ORDER);

        if (phone.length() > 0) {

            MatrixCursor translated = new MatrixCursor(PROJECTION_PHONE, 1 /* 2 */);

            RowBuilder result = translated.newRow();
            result.add(Integer.valueOf(-1)); // ID
            result.add(Long.valueOf(-1)); // CONTACT_ID
            result.add(Integer.valueOf(Contacts.Phones.TYPE_CUSTOM)); // TYPE
            result.add(cons); // NUMBER

            result.add("\u00A0"); // LABEL
            result.add(cons); // NAME

            // Rewriten as phone number
            /*
             * result = translated.newRow(); result.add(Integer.valueOf(-1)); //
             * ID result.add(Long.valueOf(-1)); // CONTACT_ID
             * result.add(Integer.
             * valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)); //
             * TYPE result.add(phone); // NUMBER
             */
            /*
             * The "\u00A0" keeps Phone.getDisplayLabel() from deciding to
             * display the default label ("Home") next to the transformation of
             * the letters into numbers.
             */
            /*
             * result.add("\u00A0"); // LABEL result.add(cons); // NAME
             */

            return new MergeCursor(new Cursor[] {
                    translated, phoneCursor
            });
        } else {
            return phoneCursor;
        }
    }

    @Override
    public CharSequence transformToSipUri(Context ctxt, Cursor cursor) {
        String number = cursor.getString(NUMBER_INDEX);
        if (number == null) {
            return "";
        }
        number = number.trim();
        return number;
    }

    @Override
    public void bindAutoCompleteView(View view, Context context, Cursor cursor) {
        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(cursor.getString(NAME_INDEX));

        TextView label = (TextView) view.findViewById(R.id.label);
        int type = cursor.getInt(TYPE_INDEX);
        CharSequence labelText = android.provider.Contacts.Phones.getDisplayLabel(context, type,
                cursor.getString(LABEL_INDEX));
        // When there's no label, getDisplayLabel() returns a CharSequence of
        // length==1 containing
        // a unicode non-breaking space. Need to check for that and consider
        // that as "no label".
        if (labelText.length() == 0 || (labelText.length() == 1 && labelText.charAt(0) == '\u00A0')) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(labelText);
            label.setVisibility(View.VISIBLE);
        }

        TextView number = (TextView) view.findViewById(R.id.number);
        number.setText(cursor.getString(NUMBER_INDEX));
    }

    @Override
    public SimpleCursorAdapter getAllContactsAdapter(Activity ctxt, int layout, int[] holders) {
        Uri uri = Uri.withAppendedPath(Contacts.People.CONTENT_URI, "");
        Cursor resCursor = ctxt.managedQuery(uri, PROJECTION_CONTACTS, null, null,
                Contacts.People.DISPLAY_NAME + " ASC");
        return new ContactCursorAdapter(ctxt,
                R.layout.contact_list_item,
                resCursor,
                new String[] {
                        Contacts.People.DISPLAY_NAME
                },
                new int[] {
                        R.id.contact_name
                });
    }

    private class ContactCursorAdapter extends SimpleCursorAdapter {

        public ContactCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            Long contactId = cursor.getLong(cursor.getColumnIndex(Contacts.People._ID));
            view.setTag(contactId);
            ImageView imageView = (ImageView) view.findViewById(R.id.contact_picture);

            Uri uri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, contactId);
            Bitmap bitmap = getContactPhoto(context, uri, R.drawable.picture_unknown);

            imageView.setImageBitmap(bitmap);

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            v.setTag(cursor.getInt(cursor.getColumnIndex(Contacts.People._ID)));
            return v;
        }

    }

    @Override
    public CallerInfo findCallerInfo(Context ctxt, String number) {
        Uri searchUri = Uri
                .withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));

        CallerInfo callerInfo = new CallerInfo();

        Cursor cursor = ctxt.getContentResolver().query(searchUri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    callerInfo.contactExists = true;
                    if (cv.containsKey(Contacts.Phones.DISPLAY_NAME)) {
                        callerInfo.name = cv.getAsString(Contacts.Phones.DISPLAY_NAME);
                    }

                    callerInfo.phoneNumber = cv.getAsString(Contacts.Phones.NUMBER);

                    if (cv.containsKey(Contacts.Phones.TYPE)
                            && cv.containsKey(Contacts.Phones.LABEL)) {
                        callerInfo.numberType = cv.getAsInteger(Contacts.Phones.TYPE);
                        callerInfo.numberLabel = cv.getAsString(Contacts.Phones.LABEL);
                        callerInfo.phoneLabel = Contacts.Phones.getDisplayLabel(ctxt,
                                callerInfo.numberType, callerInfo.numberLabel)
                                .toString();
                    }

                    if (cv.containsKey(Contacts.Phones.PERSON_ID)) {
                        callerInfo.personId = cv.getAsLong(Contacts.Phones.PERSON_ID);
                        callerInfo.contactContentUri = ContentUris.withAppendedId(
                                Contacts.People.CONTENT_URI, callerInfo.personId);
                    }

                    if (cv.containsKey(Contacts.Phones.CUSTOM_RINGTONE)) {
                        String ringtoneUriString = cv.getAsString(Contacts.Phones.CUSTOM_RINGTONE);
                        if (!TextUtils.isEmpty(ringtoneUriString)) {
                            callerInfo.contactRingtoneUri = Uri.parse(ringtoneUriString);
                        }
                    }

                    if (callerInfo.name != null && callerInfo.name.length() == 0) {
                        callerInfo.name = null;
                    }

                }

            } catch (Exception e) {
                Log.e(THIS_FILE, "Exception while retrieving cursor infos", e);
            } finally {
                cursor.close();
            }

        }

        // if no query results were returned with a viable number,
        // fill in the original number value we used to query with.
        if (TextUtils.isEmpty(callerInfo.phoneNumber)) {
            callerInfo.phoneNumber = number;
        }

        return callerInfo;
    }

    
    @Override
    public CallerInfo findSelfInfo(Context ctxt) {
        CallerInfo callerInfo = new CallerInfo();
        return callerInfo;
    }

    @Override
    public Loader<Cursor> getContactByGroupCursorLoader(Context ctxt, String groupName) {
        // TODO Auto-generated method stub
        return null;
    }
}
