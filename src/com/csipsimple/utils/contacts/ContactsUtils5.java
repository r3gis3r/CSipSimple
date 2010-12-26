/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
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
package com.csipsimple.utils.contacts;

import java.io.InputStream;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import com.csipsimple.utils.Log;

public class ContactsUtils5 extends ContactsWrapper {

	public Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource) {
		Bitmap img = null;
		
		InputStream s = ContactsContract.Contacts.openContactPhotoInputStream(ctxt.getContentResolver(), uri);
		img = BitmapFactory.decodeStream(s);
		if (img != null) {
			Log.d("Contacts5", "Success! Photo Bitmap ready to load");
		}
		return img;
	}
	
	
 	public ArrayList<Phone> getPhoneNumbers(Context ctxt, String id) {
 		ArrayList<Phone> phones = new ArrayList<Phone>();
 		
 		Cursor pCur = ctxt.getContentResolver().query(
 				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
 				null, 
 				ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", 
 				new String[]{id}, null);
 		while (pCur.moveToNext()) {
 			phones.add(new Phone(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)), 
 					pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
 			));
 
 		} 
 		pCur.close();

 		// Add any custom IM named 'sip' and set its type to 'sip'
        pCur = ctxt.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI, 
                null, 
                ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                new String[]{id, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}, null);
        while (pCur.moveToNext()) {
            // Could also use some other IM type but may be confusing. Are there phones with no 'custom' IM type?
            if (pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL)) == ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM) {
                if (pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)).equalsIgnoreCase("sip")) {
                    phones.add(new Phone(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)), "sip"));
                }
            }
                
        } 
        pCur.close();

 		return(phones);
 	}
	
}
