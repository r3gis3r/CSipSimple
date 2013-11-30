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

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.text.TextUtils;

import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.Log;

import java.io.InputStream;

@TargetApi(14)
public class ContactsUtils14 extends ContactsUtils5 {

    private static final String THIS_FILE = "ContactsUtils14";

    @Override
    public Bitmap getContactPhoto(Context ctxt, Uri uri, boolean hiRes, Integer defaultResource) {
        Bitmap img = null;
        InputStream s = ContactsContract.Contacts.openContactPhotoInputStream(
                ctxt.getContentResolver(), uri, hiRes);
        img = BitmapFactory.decodeStream(s);

        if (img == null && defaultResource != null) {
            BitmapDrawable drawableBitmap = ((BitmapDrawable) ctxt.getResources().getDrawable(
                    defaultResource));
            if (drawableBitmap != null) {
                img = drawableBitmap.getBitmap();
            }
        }
        return img;
    }

    @Override
    public CallerInfo findSelfInfo(Context ctxt) {
        
        
        CallerInfo callerInfo = new CallerInfo();

        String[] projection = new String[] {
                    Profile._ID,
                    Profile.DISPLAY_NAME,
                    Profile.PHOTO_ID,
                    Profile.PHOTO_URI
            };
        Cursor cursor = ctxt.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, projection, null, null, null);
        if(cursor != null) {
            try {
                if(cursor.getCount() > 0) {
                    Log.d(THIS_FILE, "Found " + cursor.getCount() + "self info ");
                    cursor.moveToFirst();
                    
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    callerInfo.contactExists = true;
                    if(cv.containsKey(Profile.DISPLAY_NAME) ) {
                        callerInfo.name = cv.getAsString(Profile.DISPLAY_NAME);
                    }
                    

                    if(cv.containsKey(Profile._ID) ) {
                        callerInfo.personId = cv.getAsLong(Profile._ID);
                        callerInfo.contactContentUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, callerInfo.personId);
                    }
                    
                    if(cv.containsKey(Profile.PHOTO_ID)) {
                        Long photoId = cv.getAsLong(Profile.PHOTO_ID);
                        if(photoId != null) {
                            callerInfo.photoId = photoId;
                        }
                    }
                    
                    if(cv.containsKey(Profile.PHOTO_URI)) {
                        String photoUri = cv.getAsString(Profile.PHOTO_URI);
                        if(!TextUtils.isEmpty(photoUri)) {
                            callerInfo.photoUri = Uri.parse(photoUri);
                        }
                    }
    
                    if(callerInfo.name != null && callerInfo.name.length() == 0) {
                        callerInfo.name = null;
                    }
                    
                }
            }catch(Exception e) {
                Log.e(THIS_FILE, "Exception while retrieving cursor infos", e);
            }finally {
                cursor.close();
            }
        }
        
        
        return callerInfo;
    }
}
