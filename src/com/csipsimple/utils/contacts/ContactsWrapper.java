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

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.Compatibility;

import java.util.ArrayList;
import java.util.List;

public abstract class ContactsWrapper {
    private static ContactsWrapper instance;
    
    public static final String FIELD_TYPE = "wrapped_type";
    public static final int TYPE_GROUP = 0;
    public static final int TYPE_CONTACT = 1;
    public static final int TYPE_CONFIGURE = 2;
    public static final String FIELD_GROUP_NAME = "title";

    public static ContactsWrapper getInstance() {
        if (instance == null) {
            if (Compatibility.isCompatible(14)) {
                instance = new com.csipsimple.utils.contacts.ContactsUtils14();
            } else if (Compatibility.isCompatible(5)) {
                instance = new com.csipsimple.utils.contacts.ContactsUtils5();
            } else {
                instance = new com.csipsimple.utils.contacts.ContactsUtils3();
            }
        }

        return instance;
    }

    protected ContactsWrapper() {
        // By default nothing to do in constructor
    }

    /**
     * Get the contact photo of a given contact
     * 
     * @param ctxt the context of the application
     * @param uri Uri of the contact
     * @param hiRes Should we try to retrieve hiRes photo
     * @param defaultResource the default resource id if no photo found
     * @return the bitmap for the contact or loaded default resource
     */
    public abstract Bitmap getContactPhoto(Context ctxt, Uri uri, boolean hiRes, Integer defaultResource);

    
    
    public static int URI_NBR = 1 << 0;
    public static int URI_IM = 1 << 1;
    public static int URI_SIP = 1 << 2;
    public static int URI_ALLS = URI_IM | URI_NBR | URI_SIP;
    /**
     * List all phone number for a given contact id
     * 
     * @param ctxt the context of the application
     * @param contactId the contact id
     * @param flag which numbers to get
     * @return a list of possible phone numbers
     */
    public abstract List<Phone> getPhoneNumbers(Context ctxt, long contactId, int flag);

    /**
     * Find contacts-phone tuple in the contact database based on an user input
     * string This method make clever search accross several field of the
     * contact
     * 
     * @param ctxt the context of the application
     * @param constraint the filtering string for the name
     * @return a cursor to the result
     */
    public abstract Cursor searchContact(Context ctxt, CharSequence constraint);

    /**
     * Transform a contact-phone entry into a sip uri
     * 
     * @param ctxt the context of the application
     * @param cursor the cursor to the contact entry
     * @return the string for calling via sip this contact-phone entry
     */
    public abstract CharSequence transformToSipUri(Context ctxt, Cursor cursor);

    /**
     * Bind to view the contact-phone tuple
     * View should be expanded from search_contact_list_item.xml
     * 
     * @param view the view to fill with infos
     * @param context the context of the application
     * @param cursor the cursor to the contact-phone tuple
     */
    public abstract void bindAutoCompleteView(View view, Context context, Cursor cursor);


    /**
     * Bind to view the contact
     * 
     * @param view the view to fill with infos
     * @param context the context of the application
     * @param cursor the cursor to the contact-phone tuple
     */
    public abstract void bindContactPhoneView(View view, Context context, Cursor cursor);

    /**
     *  Get a cursor loader on contacts entries based on contact grouping 
     * 
     * @param ctxt the context of the application
     * @return the result cursor
     */
    public abstract Cursor getContactsPhones(Context ctxt);
    
    /**
     * Retrieve list of csip: im entries in a group
     * @param groupName name of the group to search in
     * @return
     */
    public abstract List<String> getCSipPhonesByGroup(Context ctxt, String groupName);
    
    /**
     * Push back the presence status to the contact database
     * @param buddyUri the presence to update
     * @param presStatus the new presence status
     */
    public abstract void updateCSipPresence(Context ctxt, String buddyUri, SipManager.PresenceStatus presStatus, String statusText);

    /**
     * Get the column index of the column that should be used to index the list (the display name usually)
     * @param c the cursor pointing to datas
     * @return the column index
     */
    public abstract int getContactIndexableColumnIndex(Cursor c);

    /**
     * Get the intent to fire to propose user to add somebody to contacts
     * @param displayName the name to prefill
     * @param csipUri the sip uri to prefill
     * @return an android insert intent 
     */
    public abstract Intent getAddContactIntent(String displayName, String csipUri);
    
    /**
     * Get the intent to fire to display contact to a user
     * @param contactId the id of the contact to show
     * @return an android view intent
     */
    public abstract Intent getViewContactIntent(Long contactId);
    
    /**
     * Insert or update csip uri to a contact custom im csip protocol
     * @param ctxt the Context of the app
     * @param contactId the id of the contact to insert datas to
     * @param uri the uri to insert as csip im custom protocol
     * @return true if insert/update done.
     */
    public abstract boolean insertOrUpdateCSipUri(Context ctxt, long contactId, String uri);
    
    /**
     * Get a cursor loader on contacts entries based on contact grouping 
     * @param ctxt the context of the application
     * @param groupName the name of the group to filter on
     * @return the result cursor
     */
    public abstract Cursor getContactsByGroup(Context ctxt, String groupName);
    
    /**
     * Get the contact information form the cursor
     * @param context App context
     * @param cursor Cursor containing data at the correct position
     */
    public abstract ContactInfo getContactInfo(Context context, Cursor cursor);
    
    /**
     * @see android.provider.ContactsContract.StatusUpdates#getPresenceIconResourceId(int)
     * @return
     */
    public abstract int getPresenceIconResourceId(int presence);
    
    /**
     * Get list of groups.<br/>
     * @param context 
     * @return a cursor of groups. _id is the identifier, title is the name of the group
     */
    public abstract Cursor getGroups(Context context);
    
    public class ContactInfo {
        public Long contactId = null;
        public String displayName;
        public CallerInfo callerInfo = new CallerInfo();
        public boolean hasPresence = false;
        public int presence;
        public String status;
    }
    
    /**
     * Class to hold phone information
     */
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

    public void treatContactPickerPositiveResult(final Context ctxt, final Intent data,
            final OnPhoneNumberSelected l) {
        Uri contactUri = data.getData();
        List<String> list = contactUri.getPathSegments();
        String contactId = list.get(list.size() - 1);
        treatContactPickerPositiveResult(ctxt, contactId, l);
    }

    public void treatContactPickerPositiveResult(final Context ctxt, final String contactId,
            final OnPhoneNumberSelected l) {
        List<Phone> phones = getPhoneNumbers(ctxt, Long.parseLong(contactId), URI_ALLS);

        if (phones.size() == 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
            builder.setPositiveButton(R.string.ok, null);
            builder.setTitle(R.string.choose_phone);
            builder.setMessage(R.string.no_phone_found);
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (phones.size() == 1) {
            if (l != null) {
                l.onTrigger(formatNumber(phones.get(0).getNumber(), phones.get(0).getType()));
            }
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);

            ArrayList<String> entries = new ArrayList<String>();
            for (Phone phone : phones) {
                entries.add(formatNumber(phone.getNumber(), phone.getType()));
            }

            final ArrayAdapter<String> phoneChoiceAdapter = new ArrayAdapter<String>(ctxt,
                    android.R.layout.simple_dropdown_item_1line, entries);

            builder.setTitle(R.string.choose_phone);
            builder.setAdapter(phoneChoiceAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (l != null) {
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
        if (type != null && type.equals(SipManager.PROTOCOL_SIP)) {
            return "sip:" + number;
        } else {
            if (!number.startsWith("sip:")) {
                // Code from android source :
                // com/android/phone/OutgoingCallBroadcaster.java
                // so that we match exactly the same case that an outgoing call
                // from android
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
     * public static String formatNameAndNumber(String name, String number) {
     * String formattedNumber = number; if (SipUri.isPhoneNumber(number)) {
     * formattedNumber = PhoneNumberUtils.formatNumber(number); } if
     * (!TextUtils.isEmpty(name) && !name.equals(number)) { return name + " <" +
     * formattedNumber + ">"; } else { return formattedNumber; } }
     */

    /**
     * Returns true if all the characters are meaningful as digits in a phone
     * number -- letters, digits, and a few punctuation marks.
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
    
    /**
     * Get the information of a number caller
     * @param ctxt The application context
     * @param number The phone number to search
     * @return Caller information if anyone found
     */
    public abstract CallerInfo findCallerInfo(Context ctxt, String number);

    /**
     * Get the information of a sip uri caller
     * @param ctxt The application context
     * @param sipUri The sip uri to search
     * @return Caller information if anyone found
     */
    public abstract CallerInfo findCallerInfoForUri(Context ctxt, String sipUri);
    
    /**
     * Get self information based. This works better in ICS where it's formalized
     * @param ctxt The application context
     * @return Caller information of the current application user
     */
    public abstract CallerInfo findSelfInfo(Context ctxt);

}
