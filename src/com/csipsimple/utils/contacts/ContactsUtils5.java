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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.utils.Compatibility;

public class ContactsUtils5 extends ContactsWrapper {

    public static final int CONTACT_ID_INDEX = 1;
    public static final int TYPE_INDEX       = 2;
    public static final int NUMBER_INDEX     = 3;
    public static final int LABEL_INDEX      = 4;
    public static final int NAME_INDEX       = 5;

    private static final String[] PROJECTION_PHONE = {
    	ContactsContract.CommonDataKinds.Phone._ID,                  // 0
    	ContactsContract.CommonDataKinds.Phone.CONTACT_ID,           // 1
    	ContactsContract.CommonDataKinds.Phone.TYPE,                 // 2
    	ContactsContract.CommonDataKinds.Phone.NUMBER,               // 3
    	ContactsContract.CommonDataKinds.Phone.LABEL,                // 4
    	ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,         // 5
    };

    private static final String SORT_ORDER = Contacts.TIMES_CONTACTED + " DESC,"
            + Contacts.DISPLAY_NAME + "," + ContactsContract.CommonDataKinds.Phone.TYPE;
	private static final String GINGER_SIP_TYPE = "vnd.android.cursor.item/sip_address";
    

	
	
	public Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource) {
		Bitmap img = null;
		
		InputStream s = ContactsContract.Contacts.openContactPhotoInputStream(ctxt.getContentResolver(), uri);
		img = BitmapFactory.decodeStream(s);
		
		if(img == null){
			BitmapDrawable drawableBitmap = ((BitmapDrawable) ctxt.getResources().getDrawable(defaultResource));
			if(drawableBitmap != null) {
				img = drawableBitmap.getBitmap();
			}
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
				if ("sip".equalsIgnoreCase(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL)))) {
                    phones.add(new Phone(pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)), "sip"));
                }
            }
                
        } 
        pCur.close();
        
        // Add any SIP uri if android 9
        if(Compatibility.isCompatible(9)) {
        	 pCur = ctxt.getContentResolver().query(
                     ContactsContract.Data.CONTENT_URI, 
                     null, 
                     ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                     new String[]{id, GINGER_SIP_TYPE}, null);
             while (pCur.moveToNext()) {
                 // Could also use some other IM type but may be confusing. Are there phones with no 'custom' IM type?
            	 phones.add(new Phone(pCur.getString(pCur.getColumnIndex(ContactsContract.Data.DATA1)), "sip"));
             }
             pCur.close();
        }

 		return(phones);
 	}
 	
 	
 	
 	@Override
	public Cursor searchContact(Context ctxt, CharSequence constraint) {
		String phone = "";
        String cons = null;

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

        // TODO : filter more complex, see getAllContactsAdapter
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(cons));
        /*
         * if we decide to filter based on phone types use a selection
         * like this.
        String selection = String.format("%s=%s OR %s=%s OR %s=%s",
                Phone.TYPE,
                Phone.TYPE_MOBILE,
                Phone.TYPE,
                Phone.TYPE_WORK_MOBILE,
                Phone.TYPE,
                Phone.TYPE_MMS);
         */
        Cursor phoneCursor =
            ctxt.getContentResolver().query(uri,
                    PROJECTION_PHONE,
                    null, //selection,
                    null,
                    SORT_ORDER);

        if (phone.length() > 0) {

            MatrixCursor translated = new MatrixCursor(PROJECTION_PHONE, 1 /*2*/);
            
            RowBuilder result = translated.newRow();
            result.add(Integer.valueOf(-1));                    // ID
            result.add(Long.valueOf(-1));                       // CONTACT_ID
            result.add(Integer.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM));     // TYPE
            result.add(cons);                                  // NUMBER

            result.add("\u00A0");                               // LABEL
            result.add(cons);                                   // NAME
            
            //Rewriten as phone number
            /*
            result = translated.newRow();
            result.add(Integer.valueOf(-1));                    // ID
            result.add(Long.valueOf(-1));                       // CONTACT_ID
            result.add(Integer.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM));     // TYPE
            result.add(phone);                                  // NUMBER
			*/
            /*
             * The "\u00A0" keeps Phone.getDisplayLabel() from deciding
             * to display the default label ("Home") next to the transformation
             * of the letters into numbers.
             */
            /*
            result.add("\u00A0");                               // LABEL
            result.add(cons);                                   // NAME
			*/

            //return new MergeCursor(new Cursor[] { translated, phoneCursor });
            return phoneCursor;
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
        /*
        String name = cursor.getString(NAME_INDEX);
        int type = cursor.getInt(TYPE_INDEX);

        String label = cursor.getString(LABEL_INDEX);
        
        CharSequence displayLabel = android.provider.Contacts.Phones.getDisplayLabel(ctxt, type, label);

        if (name == null) {
            name = "";
        } else {
            // Names with commas are the bane of the recipient editor's existence.
            // We've worked around them by using spans, but there are edge cases
            // where the spans get deleted. Furthermore, having commas in names
            // can be confusing to the user since commas are used as separators
            // between recipients. The best solution is to simply remove commas
            // from names.
            name = name.replace(", ", " ")
                       .replace(",", " ");  // Make sure we leave a space between parts of names.
        }

        String nameAndNumber = ContactsWrapper.formatNameAndNumber(name, number);

        SpannableString out = new SpannableString(nameAndNumber);
        int len = out.length();

        if (!TextUtils.isEmpty(name)) {
            out.setSpan(new Annotation("name", name), 0, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            out.setSpan(new Annotation("name", number), 0, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        String person_id = cursor.getString(CONTACT_ID_INDEX);
        out.setSpan(new Annotation("person_id", person_id), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new Annotation("label", displayLabel.toString()), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new Annotation("number", number), 0, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

         */
        return number;
        //return out;
    }
	


	@SuppressWarnings("deprecation")
	@Override
	public void bindAutoCompleteView(View view, Context context, Cursor cursor) {
		TextView name = (TextView) view.findViewById(R.id.name);
		TextView label = (TextView) view.findViewById(R.id.label);
		TextView number = (TextView) view.findViewById(R.id.number);
		
		name.setText(cursor.getString(NAME_INDEX));
		
		int type = cursor.getInt(TYPE_INDEX);
		CharSequence labelText = android.provider.Contacts.Phones.getDisplayLabel(context, type, cursor.getString(LABEL_INDEX));
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

		number.setText(cursor.getString(NUMBER_INDEX));
	}


	@Override
	public SimpleCursorAdapter getAllContactsAdapter(Activity ctxt, int layout, int[] holders) {
		//TODO : throw error if holders length != correct length
		
		Uri uri = Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, "");
		
		String query = Contacts.DISPLAY_NAME + " IS NOT NULL "
				+" AND "
				+ "("
					// Has phone number
					+ "("+ ContactsContract.Data.MIMETYPE+ "='"+ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE+"' AND "+ContactsContract.CommonDataKinds.Phone.NUMBER+" IS NOT NULL ) "
					// Sip uri
					+ (Compatibility.isCompatible(9)? " OR " + ContactsContract.Data.MIMETYPE+ "='"+GINGER_SIP_TYPE+"'":"")
					//Sip IM custo
					+ " OR ("
						
							+ ContactsContract.Data.MIMETYPE+ "='"+ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE+"' "
							+" AND "+ ContactsContract.CommonDataKinds.Im.PROTOCOL + "=" + ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
							+" AND "+ ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + "='sip'" 
					+")"
				+ ")";
		if(!Compatibility.isCompatible(14)) {
			query += ") GROUP BY ( "+RawContacts.CONTACT_ID;
		}else {
			//query += ")) GROUP BY (( "+RawContacts.CONTACT_ID;
		}
		
		Cursor resCursor = ctxt.managedQuery(uri, 
				new String[] {
					ContactsContract.Data._ID,
			    	RawContacts.CONTACT_ID,
			        Contacts.DISPLAY_NAME
			    }, query, 
				null, 
				Contacts.DISPLAY_NAME + " ASC");
		return new ContactCursorAdapter(ctxt, 
				R.layout.contact_list_item, 
				resCursor, 
				new String[] { Contacts.DISPLAY_NAME }, 
				new int[] {R.id.contact_name });
	}
	
	
	private class ContactCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {

		private AlphabetIndexer alphaIndexer;

		public ContactCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			alphaIndexer=new AlphabetIndexer(c, c.getColumnIndex(Contacts.DISPLAY_NAME), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			Long contactId = cursor.getLong(cursor.getColumnIndex(RawContacts.CONTACT_ID));
			view.setTag(contactId);
		    ImageView imageView = (ImageView) view.findViewById(R.id.contact_picture);
		    
		    Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		    Bitmap bitmap = getContactPhoto(context, uri, R.drawable.picture_unknown);
		 
		    imageView.setImageBitmap(bitmap);
			
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			v.setTag(cursor.getInt(cursor.getColumnIndex(RawContacts.CONTACT_ID)));
			return v;
		}

		@Override
		public int getPositionForSection(int arg0) {
			return alphaIndexer.getPositionForSection(arg0);
		}

		@Override
		public int getSectionForPosition(int arg0) {
			return alphaIndexer.getSectionForPosition(arg0);
		}

		@Override
		public Object[] getSections() {
			return alphaIndexer.getSections();
		}
		
	}

}
