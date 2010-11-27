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

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.People;

import com.csipsimple.utils.Log;

@SuppressWarnings("deprecation")
public class ContactsUtils3 extends ContactsWrapper {

	@Override
	public Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource) {
		Bitmap img = null;
		try {
    		img = People.loadContactPhoto(ctxt, uri, defaultResource, null);
    	} catch(IllegalArgumentException e) {
    		Log.w("Contact3", "Failed to find contact photo");
    	}
    	return img;
	}
	
 	public ArrayList<Phone> getPhoneNumbers(Context ctxt, String id) {
 		ArrayList<Phone> phones = new ArrayList<Phone>();
 		
 		Cursor pCur = ctxt.getContentResolver().query(
 				Contacts.Phones.CONTENT_URI, 
 				null, 
 				Contacts.Phones.PERSON_ID +" = ?", 
 				new String[]{id}, null);
 		while (pCur.moveToNext()) {
 			phones.add(new Phone(
 					pCur.getString(pCur.getColumnIndex(Contacts.Phones.NUMBER))
 					, pCur.getString(pCur.getColumnIndex(Contacts.Phones.TYPE))
 			));
 		} 
 		pCur.close();
 		return(phones);
 	}

}
