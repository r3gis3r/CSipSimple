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

import java.lang.reflect.Field;

import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.text.TextUtils;

/**
 * Looks up caller information for the given phone number.
 */
@SuppressWarnings("deprecation")
public class CallerInfo {

	private static final String THIS_FILE = "CallerInfo";

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
	public Uri contactContentUri;

    public CallerInfo() {
       
    }

    /**
     * getCallerInfo given a phone number, look up in the People database
     * for the matching caller id info.
     * @param context the context used to get the ContentResolver
     * @param number the phone number used to lookup caller
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied. If
     * a matching number is not found, then a generic caller info is returned,
     * with all relevant fields empty or null.
     */
    public static CallerInfo getCallerInfo(Context context, String number) {

        Uri contactUri = null;		// [sentinel]
        Uri contentUri = null;		// [sentinel]

        CallerInfo info = new CallerInfo();
        info.phoneLabel = null;
        info.numberType = 0;
        info.numberLabel = null;
        info.contactExists = false;


        if (TextUtils.isEmpty(number)) {
            return null;
        }
        
        // Must try V5+ ContactsContract first, as the old API fails on 
        // newer OS levels.
        if(Compatibility.isCompatible(5) ) {
        	Log.d(THIS_FILE, "Trying Api 5 for PhoneLookup");
        	try {
        		Class<?> contactClass = Class.forName("android.provider.ContactsContract$PhoneLookup");
        		Field uriField = contactClass.getField("CONTENT_FILTER_URI");
        		Uri u = (Uri) uriField.get(null);
        		contactUri = Uri.withAppendedPath(u, Uri.encode(number));
    	        Log.d(THIS_FILE, "Api 5 succeeded, uri=" + contactUri.toString());
        	} catch (Exception e) {
        		Log.e(THIS_FILE, "Api compatible 5 but uri not available", e);
        	}
	        
        }   // TODO Should be simply 'else'?
        if (contactUri == null) {
        	contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));
        	Log.d(THIS_FILE, "Old Api lookup, phone uri=" + contactUri.toString());
        }
        
    	Cursor cursor = context.getContentResolver().query(contactUri, null, null, null, null);
    	
    	// TODO - Strictly speaking the column indexes below should be old/new API switched
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
                if(Compatibility.isCompatible(5)) {
                	//Not really relevent for api 5
                	columnIndex = cursor.getColumnIndex(Contacts.Phones._ID);	// REGIS - NOTE THIS CHANGE (rbd)
                }else {
                	columnIndex = cursor.getColumnIndex(Contacts.Phones.PERSON_ID);
                }
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

                //
                // Get the content Uri for this person
                if(Compatibility.isCompatible(5) ) {
                	Log.d(THIS_FILE, "Trying Api 5 for Contacts");
                	try {
                		Class<?> contactClass = Class.forName("android.provider.ContactsContract$Contacts");
                		Field uriField = contactClass.getField("CONTENT_URI");
                		Uri u = (Uri) uriField.get(null);
                		contentUri = ContentUris.withAppendedId(u, info.personId);
            	        Log.d(THIS_FILE, "Api 5 succeeded, uri=" + contentUri.toString());
                	} catch (Exception e) {
                		Log.e(THIS_FILE, "Api compatible 5 but uri not available", e);
                	}
        	        
                }   // TODO Should be simply 'else'?
                if (contentUri == null) {
                	contentUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, info.personId);
                	Log.d(THIS_FILE, "Old Api lookup, uri=" + contentUri.toString());
                }
                
                info.contactExists = true;
            }
            cursor.close();
        }

        info.name = normalize(info.name);
        info.contactRefUri = contactUri;
        info.contactContentUri = contentUri;

        // if no query results were returned with a viable number,
        // fill in the original number value we used to query with.
        if (TextUtils.isEmpty(info.phoneNumber)) {
            info.phoneNumber = number;
        }

        return info;
    }

    private static String normalize(String s) {
        if (s == null || s.length() > 0) {
            return s;
        } else {
            return null;
        }
    }
}
