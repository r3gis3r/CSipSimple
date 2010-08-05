/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2006 The Android Open Source Project
 *  
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.models;


import com.csipsimple.utils.Log;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.TextUtils;

/**
 * Looks up caller information for the given phone number.
 */
@SuppressWarnings("deprecation")
public class CallerInfo {


    public boolean contactExists;
    
    public long personId;
    public String name;
    
    public String phoneNumber;
    public String phoneLabel;
    public int    numberType;
    public String numberLabel;

    // fields to hold individual contact preference data,
    // including the send to voicemail flag and the ringtone
    // uri reference.
    public Uri contactRingtoneUri;
	public Uri contactRefUri;



    public CallerInfo() {
       
    }

    /**
     * getCallerInfo given a Cursor.
     * @param context the context used to retrieve string constants
     * @param contactRef the URI to attach to this CallerInfo object
     * @param cursor the first object in the cursor is used to build the CallerInfo object.
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef, Cursor cursor) {

        CallerInfo info = new CallerInfo();
        info.phoneLabel = null;
        info.numberType = 0;
        info.numberLabel = null;
        info.contactExists = false;


        if (cursor != null) {
            if (cursor.moveToFirst()) {

                int columnIndex;

                // Look for the name
                columnIndex = cursor.getColumnIndex(Contacts.People.DISPLAY_NAME);
                if (columnIndex != -1) {
                    info.name = cursor.getString(columnIndex);
                }

                // Look for the number
                columnIndex = cursor.getColumnIndex(Contacts.Phones.NUMBER);
                if (columnIndex != -1) {
                    info.phoneNumber = cursor.getString(columnIndex);
                }

                // Look for the label/type combo
                columnIndex = cursor.getColumnIndex(Contacts.Phones.LABEL);
                if (columnIndex != -1) {
                    int typeColumnIndex = cursor.getColumnIndex(Contacts.Phones.TYPE);
                    if (typeColumnIndex != -1) {
                        info.numberType = cursor.getInt(typeColumnIndex);
                        info.numberLabel = cursor.getString(columnIndex);
                        info.phoneLabel = Contacts.Phones.getDisplayLabel(context,
                                info.numberType, info.numberLabel)
                                .toString();
                    }
                }

                // Look for the person ID
                columnIndex = cursor.getColumnIndex(Contacts.Phones.PERSON_ID);
                if (columnIndex != -1) {
                    info.personId = cursor.getLong(columnIndex);
                }

                // look for the custom ringtone, create from the string stored
                // in the database.
                columnIndex = cursor.getColumnIndex(Contacts.Phones.CUSTOM_RINGTONE);
                if ((columnIndex != -1) && (cursor.getString(columnIndex) != null)) {
                    info.contactRingtoneUri = Uri.parse(cursor.getString(columnIndex));
                } else {
                    info.contactRingtoneUri = null;
                }

                info.contactExists = true;
            }
            cursor.close();
        }

        info.name = normalize(info.name);
        info.contactRefUri = contactRef;

        return info;
    }

    /**
     * getCallerInfo given a URI, look up in the People database
     * for the uri unique key.
     * @param context the context used to get the ContentResolver
     * @param contactRef the URI used to lookup caller id (differs before and after API level 5!)
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef) {

        return getCallerInfo(context, contactRef,
                context.getContentResolver().query(contactRef, null, null, null, null));
    }

    /**
     * getCallerInfo given a phone number, look up in the People database
     * for the matching caller id info.
     * @param context the context used to get the ContentResolver
     * @param number the phone number used to lookup caller id
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied. If
     * a matching number is not found, then a generic caller info is returned,
     * with all relevant fields empty or null.
     */
    public static CallerInfo getCallerInfo(Context context, String number) {
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        // Must try V5+ ContactsContract first, as the old API fails on 
        // newer OS levels.
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        CallerInfo info = CallerInfo.getCallerInfo(context, contactUri);
        if (info == null) {
        	contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));
			info = CallerInfo.getCallerInfo(context, contactUri);
        }

        // if no query results were returned with a viable number,
        // fill in the original number value we used to query with.
        if (TextUtils.isEmpty(info.phoneNumber)) {
            info.phoneNumber = number;
        }
        
        return info;
    }

    /**
     * getCallerId: a convenience method to get the caller id for a given
     * number.
     *
     * @param context the context used to get the ContentResolver.
     * @param number a phone number.
     * @return if the number belongs to a contact, the contact's name is
     * returned; otherwise, the number itself is returned.
     *
     * TODO NOTE: This MAY need to refer to the Asynchronous Query API
     * [startQuery()], instead of getCallerInfo, but since it looks like
     * it is only being used by the provider calls in the messaging app:
     *   1. android.provider.Telephony.Mms.getDisplayAddress()
     *   2. android.provider.Telephony.Sms.getDisplayAddress()
     * We may not need to make the change.
     */
    public static String getCallerId(Context context, String number) {
        CallerInfo info = getCallerInfo(context, number);
        String callerID = null;

        if (info != null) {
            String name = info.name;

            if (!TextUtils.isEmpty(name)) {
                callerID = name;
            } else {
                callerID = number;
            }
        }
        return callerID;
    }

    private static String normalize(String s) {
        if (s == null || s.length() > 0) {
            return s;
        } else {
            return null;
        }
    }
}
