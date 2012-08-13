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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.Intents;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.Groups;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
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
            People._ID,
            People.DISPLAY_NAME
    };

    private static final String[] PROJECTION_PHONE = {
            Phones._ID, // 0
            Phones.PERSON_ID, // 1
            Phones.TYPE, // 2
            Phones.NUMBER, // 3
            Phones.LABEL, // 4
            Phones.DISPLAY_NAME, // 5
    };

    private static final String SORT_ORDER = Phones.DISPLAY_NAME + " ASC,"
            + Phones.TYPE;
    private static final String THIS_FILE = "ContactsUtils3";

    @Override
    public Bitmap getContactPhoto(Context ctxt, Uri uri, boolean hiRes, Integer defaultResource) {
        Bitmap img = null;
        try {
            img = People.loadContactPhoto(ctxt, uri, defaultResource, null);
        } catch (IllegalArgumentException e) {
            Log.w("Contact3", "Failed to find contact photo");
        }
        return img;
    }

    public List<Phone> getPhoneNumbers(Context ctxt, long contactId, int flags) {
        ArrayList<Phone> phones = new ArrayList<Phone>();
        if ((flags & ContactsWrapper.URI_NBR) > 0) {
            Cursor pCur = ctxt.getContentResolver().query(
                    Phones.CONTENT_URI,
                    null,
                    Phones.PERSON_ID + " = ?",
                    new String[] {
                        Long.toString(contactId)
                    }, null);
            while (pCur.moveToNext()) {
                phones.add(new Phone(
                        pCur.getString(pCur.getColumnIndex(Phones.NUMBER))
                        , pCur.getString(pCur.getColumnIndex(Phones.TYPE))
                        ));
            }
            pCur.close();
        }
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

        Uri uri = Phones.CONTENT_URI;
        /*
         * if we decide to filter based on phone types use a selection like
         * this. String selection = String.format("%s=%s OR %s=%s OR %s=%s",
         * Phone.TYPE, Phone.TYPE_MOBILE, Phone.TYPE, Phone.TYPE_WORK_MOBILE,
         * Phone.TYPE, Phone.TYPE_MMS);
         */
        String selection = String.format("%s LIKE ? OR %s LIKE ?", Phones.NUMBER,
                Phones.DISPLAY_NAME);
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
            result.add(Integer.valueOf(Phones.TYPE_CUSTOM)); // TYPE
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
        CharSequence labelText = Phones.getDisplayLabel(context, type,
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
    public CallerInfo findCallerInfo(Context ctxt, String number) {
        Uri searchUri = Uri
                .withAppendedPath(Phones.CONTENT_FILTER_URL, Uri.encode(number));

        CallerInfo callerInfo = new CallerInfo();

        Cursor cursor = ctxt.getContentResolver().query(searchUri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    callerInfo.contactExists = true;
                    if (cv.containsKey(Phones.DISPLAY_NAME)) {
                        callerInfo.name = cv.getAsString(Phones.DISPLAY_NAME);
                    }

                    callerInfo.phoneNumber = cv.getAsString(Phones.NUMBER);

                    if (cv.containsKey(Phones.TYPE)
                            && cv.containsKey(Phones.LABEL)) {
                        callerInfo.numberType = cv.getAsInteger(Phones.TYPE);
                        callerInfo.numberLabel = cv.getAsString(Phones.LABEL);
                        callerInfo.phoneLabel = Phones.getDisplayLabel(ctxt,
                                callerInfo.numberType, callerInfo.numberLabel)
                                .toString();
                    }

                    if (cv.containsKey(Phones.PERSON_ID)) {
                        callerInfo.personId = cv.getAsLong(Phones.PERSON_ID);
                        callerInfo.contactContentUri = ContentUris.withAppendedId(
                                People.CONTENT_URI, callerInfo.personId);
                    }

                    if (cv.containsKey(Phones.CUSTOM_RINGTONE)) {
                        String ringtoneUriString = cv.getAsString(Phones.CUSTOM_RINGTONE);
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
    public CallerInfo findCallerInfoForUri(Context ctxt, String sipUri) {

        CallerInfo callerInfo = new CallerInfo();
        callerInfo.phoneNumber = sipUri;
        
        return callerInfo;
    }
    
    @Override
    public CallerInfo findSelfInfo(Context ctxt) {
        CallerInfo callerInfo = new CallerInfo();
        return callerInfo;
    }


    @Override
    public Cursor getContactsPhones(Context ctxt) {
        Uri uri = Phones.CONTENT_URI;
        Cursor resCursor = ctxt.getContentResolver().query(uri, PROJECTION_PHONE, null, null,
                Phones.DISPLAY_NAME + " ASC");
        return resCursor;
    }

    @Override
    public void bindContactPhoneView(View view, Context context, Cursor cursor) {
        
        // Get values
        String value = cursor.getString(cursor.getColumnIndex(Phones.NUMBER));
        String displayName = cursor.getString(cursor.getColumnIndex(Phones.DISPLAY_NAME));
        Long peopleId = cursor.getLong(cursor.getColumnIndex(Phones.PERSON_ID));
        Uri uri = ContentUris.withAppendedId(People.CONTENT_URI, peopleId);
        Bitmap bitmap = getContactPhoto(context, uri, false, R.drawable.ic_contact_picture_holo_dark);
        
        // Get views
        TextView tv = (TextView) view.findViewById(R.id.contact_name);
        TextView sub = (TextView) view.findViewById(R.id.subject);
        ImageView imageView = (ImageView) view.findViewById(R.id.contact_picture);
        
        // Bind
        view.setTag(value);
        tv.setText(displayName);
        sub.setText(value);
        imageView.setImageBitmap(bitmap);        
    }

    @Override
    public int getContactIndexableColumnIndex(Cursor c) {
        return c.getColumnIndex(People.DISPLAY_NAME);
    }
    

    
    @Override
    public Cursor getContactsByGroup(Context ctxt, String groupName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ContactInfo getContactInfo(Context context, Cursor cursor) {
        ContactInfo ci = new ContactInfo();
        
        return ci;
    }
    
    public int getPresenceIconResourceId(int presence) {
        return R.drawable.emo_im_wtf;
    }

    @Override
    public List<String> getCSipPhonesByGroup(Context ctxt, String groupName) {
        return new ArrayList<String>();
    }

    @Override
    public void updateCSipPresence(Context ctxt, String buddyUri, SipManager.PresenceStatus presStatus, String statusText) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Intent getAddContactIntent(String displayName, String csipUri) {

        Intent intent = new Intent(Intents.Insert.ACTION);

        if(!TextUtils.isEmpty(displayName)) {
            intent.putExtra(Intents.Insert.NAME, displayName);
        }
        intent.putExtra(Intents.Insert.IM_HANDLE, csipUri);
        intent.putExtra(Intents.Insert.IM_PROTOCOL, SipManager.PROTOCOL_CSIP);
        return intent;
    }

    @Override
    public Intent getViewContactIntent(Long contactId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId));
        return intent;
    }

    @Override
    public Cursor getGroups(Context context) {
        Uri searchUri = android.provider.Contacts.Groups.CONTENT_URI;
        String[] projection = new String[] {
                Groups._ID,
                Groups.NAME + " AS '"+FIELD_GROUP_NAME+"'"
        };
        
        return context.getContentResolver().query(searchUri, projection, null, null,
                Groups.NAME + " ASC");
    }

    @Override
    public boolean insertOrUpdateCSipUri(Context ctxt, long contactId, String uri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getCSipPhonesContact(Context ctxt, Long contactId) {
        // TODO Auto-generated method stub
        return new ArrayList<String>();
    }
}
