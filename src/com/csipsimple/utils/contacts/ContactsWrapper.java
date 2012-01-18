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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;

import com.csipsimple.R;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.Compatibility;

public abstract class ContactsWrapper {
	private static ContactsWrapper instance;
	
	public static ContactsWrapper getInstance() {
		if(instance == null) {
			String className = "com.csipsimple.utils.contacts.ContactsUtils";
			if(Compatibility.isCompatible(5)) {
				className += "5";
			}else {
				className += "3";
			}
			try {
                Class<? extends ContactsWrapper> wrappedClass = Class.forName(className).asSubclass(ContactsWrapper.class);
                instance = wrappedClass.newInstance();
	        } catch (Exception e) {
	        	throw new IllegalStateException(e);
	        }
		}
		
		return instance;
	}
	
	protected ContactsWrapper() {
		// By default nothing to do in constructor
	}
	
	public abstract Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource);
	public abstract List<Phone> getPhoneNumbers(Context ctxt, String id);
	public abstract Cursor searchContact(Context ctxt, CharSequence constraint);
	public abstract CharSequence transformToSipUri(Context ctxt, Cursor cursor);
	public abstract void bindAutoCompleteView(View view, Context context, Cursor cursor);
	public abstract SimpleCursorAdapter getAllContactsAdapter(Activity ctxt, int layout, int[] holders);
	
	public class Phone {
		private String number;
		private String type;

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public Phone(String n, String t) {
			this.number = n;
			this.type = t;
		}
	}
	
	public void treatContactPickerPositiveResult(final Context ctxt, final Intent data, final OnPhoneNumberSelected l) {
		Uri contactUri = data.getData();
        List<String> list = contactUri.getPathSegments();
        String contactId = list.get(list.size() - 1);
        treatContactPickerPositiveResult(ctxt, contactId, l);
	}
	

	public void treatContactPickerPositiveResult(final Context ctxt, final String contactId, final OnPhoneNumberSelected l) {
		List<Phone> phones = getPhoneNumbers(ctxt, contactId);
        
        if(phones.size() == 0) {
	        final AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
        	builder.setPositiveButton(R.string.ok, null);
        	builder.setTitle(R.string.choose_phone);
        	builder.setMessage(R.string.no_phone_found);
        	AlertDialog dialog = builder.create();
        	dialog.show();
        }else if(phones.size() == 1) {
        	if(l != null) {
        		l.onTrigger(formatNumber(phones.get(0).getNumber(), phones.get(0).getType()));
        	}
        }else {
	        final AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
			
			ArrayList<String> entries = new ArrayList<String>();
			for (Phone phone : phones) {
				entries.add(formatNumber(phone.getNumber(), phone.getType()));
			}
			
			final ArrayAdapter<String> phoneChoiceAdapter = new ArrayAdapter<String>(ctxt, android.R.layout.simple_dropdown_item_1line, entries );
			
			builder.setTitle(R.string.choose_phone);
	        builder.setAdapter(phoneChoiceAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(l != null) {
						l.onTrigger(phoneChoiceAdapter.getItem(which));
					}
				}
			});
	        builder.setCancelable(true);
	        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Nothing to do
					dialog.dismiss();
				}
			});
	        AlertDialog dialog = builder.create();
	        dialog.show();
        }
	}
	
	private String formatNumber(String number, String type) {
	    if (type != null && type.equals("sip")) {
	        return "sip:" + number;
	    } else {
	    	if (!number.startsWith("sip:")) {
	    		//Code from android source : com/android/phone/OutgoingCallBroadcaster.java 
                // so that we match exactly the same case that an outgoing call from android
				String rNumber = PhoneNumberUtils.convertKeypadLettersToDigits(number);
	            return PhoneNumberUtils.stripSeparators(rNumber);
	    	}
	        return number;
	    }
	}
	
	public interface OnPhoneNumberSelected {
		void onTrigger(String number);
	}

/*
    public static String formatNameAndNumber(String name, String number) {
        String formattedNumber = number;
        if (SipUri.isPhoneNumber(number)) {
            formattedNumber = PhoneNumberUtils.formatNumber(number);
        }

        if (!TextUtils.isEmpty(name) && !name.equals(number)) {
            return name + " <" + formattedNumber + ">";
        } else {
            return formattedNumber;
        }
    }
    */
	
	

    /**
     * Returns true if all the characters are meaningful as digits
     * in a phone number -- letters, digits, and a few punctuation marks.
     */
    protected boolean usefulAsDigits(CharSequence cons) {
        int len = cons.length();

        for (int i = 0; i < len; i++) {
            char c = cons.charAt(i);

            if ((c >= '0') && (c <= '9')) {
                continue;
            }
            if ((c == ' ') || (c == '-') || (c == '(') || (c == ')') || (c == '.') || (c == '+')
                    || (c == '#') || (c == '*')) {
                continue;
            }
            if ((c >= 'A') && (c <= 'Z')) {
                continue;
            }
            if ((c >= 'a') && (c <= 'z')) {
                continue;
            }

            return false;
        }

        return true;
    }

    public abstract CallerInfo findCallerInfo(Context ctxt, String number);
}
