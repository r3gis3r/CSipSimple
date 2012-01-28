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

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.StatusUpdates;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.service.PresenceManager.PresenceStatus;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.ContactsAsyncHelper;
import com.csipsimple.utils.Log;
import com.csipsimple.widgets.contactbadge.QuickContactBadge;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContactsUtils5 extends ContactsWrapper {

    public static final int CONTACT_ID_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int NUMBER_INDEX = 3;
    public static final int LABEL_INDEX = 4;
    public static final int NAME_INDEX = 5;

    private static final String[] PROJECTION_PHONE = {
            CommonDataKinds.Phone._ID, // 0
            CommonDataKinds.Phone.CONTACT_ID, // 1
            CommonDataKinds.Phone.TYPE, // 2
            CommonDataKinds.Phone.NUMBER, // 3
            CommonDataKinds.Phone.LABEL, // 4
            CommonDataKinds.Phone.DISPLAY_NAME, // 5
    };

    private static final String SORT_ORDER = Contacts.TIMES_CONTACTED + " DESC,"
            + Contacts.DISPLAY_NAME + "," + CommonDataKinds.Phone.TYPE;
    private static final String THIS_FILE = "ContactsUtils5";

    public Bitmap getContactPhoto(Context ctxt, Uri uri, Integer defaultResource) {
        Bitmap img = null;

        InputStream s = ContactsContract.Contacts.openContactPhotoInputStream(
                ctxt.getContentResolver(), uri);
        img = BitmapFactory.decodeStream(s);

        if (img == null) {
            BitmapDrawable drawableBitmap = ((BitmapDrawable) ctxt.getResources().getDrawable(
                    defaultResource));
            if (drawableBitmap != null) {
                img = drawableBitmap.getBitmap();
            }
        }
        return img;
    }

    public List<Phone> getPhoneNumbers(Context ctxt, String id) {
        ArrayList<Phone> phones = new ArrayList<Phone>();

        Cursor pCur = ctxt.getContentResolver().query(
                CommonDataKinds.Phone.CONTENT_URI,
                null,
                CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[] {
                    id
                }, null);
        while (pCur.moveToNext()) {
            phones.add(new Phone(
                    pCur.getString(pCur
                            .getColumnIndex(CommonDataKinds.Phone.NUMBER)),
                    pCur.getString(pCur.getColumnIndex(CommonDataKinds.Phone.TYPE))
                    ));

        }
        pCur.close();

        // Add any custom IM named 'sip' and set its type to 'sip'
        pCur = ctxt.getContentResolver().query(
                Data.CONTENT_URI,
                null,
                Data.CONTACT_ID + " = ? AND " + Data.MIMETYPE
                        + " = ?",
                new String[] {
                        id, CommonDataKinds.Im.CONTENT_ITEM_TYPE
                }, null);
        while (pCur.moveToNext()) {
            // Could also use some other IM type but may be confusing. Are there
            // phones with no 'custom' IM type?
            if (pCur.getInt(pCur.getColumnIndex(CommonDataKinds.Im.PROTOCOL)) == CommonDataKinds.Im.PROTOCOL_CUSTOM) {
                if ("sip".equalsIgnoreCase(pCur.getString(pCur
                        .getColumnIndex(CommonDataKinds.Im.CUSTOM_PROTOCOL)))) {
                    phones.add(new Phone(pCur.getString(pCur
                            .getColumnIndex(CommonDataKinds.Im.DATA)), "sip"));
                }
            }

        }
        pCur.close();

        // Add any SIP uri if android 9
        if (Compatibility.isCompatible(9)) {
            pCur = ctxt.getContentResolver().query(
                    Data.CONTENT_URI,
                    null,
                    Data.CONTACT_ID + " = ? AND " + Data.MIMETYPE
                            + " = ?",
                    new String[] {
                            id, CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE
                    }, null);
            while (pCur.moveToNext()) {
                // Could also use some other IM type but may be confusing. Are
                // there phones with no 'custom' IM type?
                phones.add(new Phone(pCur.getString(pCur
                        .getColumnIndex(ContactsContract.Data.DATA1)), "sip"));
            }
            pCur.close();
        }

        return (phones);
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
        Uri uri = Uri.withAppendedPath(CommonDataKinds.Phone.CONTENT_FILTER_URI,
                Uri.encode(cons));
        /*
         * if we decide to filter based on phone types use a selection like
         * this. String selection = String.format("%s=%s OR %s=%s OR %s=%s",
         * Phone.TYPE, Phone.TYPE_MOBILE, Phone.TYPE, Phone.TYPE_WORK_MOBILE,
         * Phone.TYPE, Phone.TYPE_MMS);
         */
        Cursor phoneCursor =
                ctxt.getContentResolver().query(uri,
                        PROJECTION_PHONE,
                        null, // selection,
                        null,
                        SORT_ORDER);

        if (phone.length() > 0) {

            MatrixCursor translated = new MatrixCursor(PROJECTION_PHONE, 1 /* 2 */);

            RowBuilder result = translated.newRow();
            result.add(Integer.valueOf(-1)); // ID
            result.add(Long.valueOf(-1)); // CONTACT_ID
            result.add(Integer.valueOf(CommonDataKinds.Phone.TYPE_CUSTOM)); // TYPE
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

            // return new MergeCursor(new Cursor[] { translated, phoneCursor });
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
         * String name = cursor.getString(NAME_INDEX); int type =
         * cursor.getInt(TYPE_INDEX); String label =
         * cursor.getString(LABEL_INDEX); CharSequence displayLabel =
         * android.provider.Contacts.Phones.getDisplayLabel(ctxt, type, label);
         * if (name == null) { name = ""; } else { // Names with commas are the
         * bane of the recipient editor's existence. // We've worked around them
         * by using spans, but there are edge cases // where the spans get
         * deleted. Furthermore, having commas in names // can be confusing to
         * the user since commas are used as separators // between recipients.
         * The best solution is to simply remove commas // from names. name =
         * name.replace(", ", " ") .replace(",", " "); // Make sure we leave a
         * space between parts of names. } String nameAndNumber =
         * ContactsWrapper.formatNameAndNumber(name, number); SpannableString
         * out = new SpannableString(nameAndNumber); int len = out.length(); if
         * (!TextUtils.isEmpty(name)) { out.setSpan(new Annotation("name",
         * name), 0, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); } else {
         * out.setSpan(new Annotation("name", number), 0, len,
         * Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); } String person_id =
         * cursor.getString(CONTACT_ID_INDEX); out.setSpan(new
         * Annotation("person_id", person_id), 0, len,
         * Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); out.setSpan(new
         * Annotation("label", displayLabel.toString()), 0, len,
         * Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); out.setSpan(new
         * Annotation("number", number), 0, len,
         * Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
         */
        return number;
        // return out;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void bindAutoCompleteView(View view, Context context, Cursor cursor) {
        TextView name = (TextView) view.findViewById(R.id.name);
        TextView label = (TextView) view.findViewById(R.id.label);
        TextView number = (TextView) view.findViewById(R.id.number);

        name.setText(cursor.getString(NAME_INDEX));

        int type = cursor.getInt(TYPE_INDEX);
        CharSequence labelText = CommonDataKinds.Phone
                .getTypeLabel(context.getResources(), type, cursor.getString(LABEL_INDEX));
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
    public CallerInfo findCallerInfo(Context ctxt, String number) {
        Uri searchUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        CallerInfo callerInfo = new CallerInfo();

        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection = new String[] {
                    PhoneLookup._ID,
                    PhoneLookup.DISPLAY_NAME,
                    PhoneLookup.TYPE,
                    PhoneLookup.LABEL,
                    PhoneLookup.NUMBER,
                    PhoneLookup.PHOTO_ID,
                    PhoneLookup.LOOKUP_KEY,
                    PhoneLookup.PHOTO_URI
            };
        } else {
            projection = new String[] {
                    PhoneLookup._ID,
                    PhoneLookup.DISPLAY_NAME,
                    PhoneLookup.TYPE,
                    PhoneLookup.LABEL,
                    PhoneLookup.NUMBER,
                    PhoneLookup.LOOKUP_KEY
            };
        }

        Cursor cursor = ctxt.getContentResolver().query(searchUri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    callerInfo.contactExists = true;
                    if (cv.containsKey(PhoneLookup.DISPLAY_NAME)) {
                        callerInfo.name = cv.getAsString(PhoneLookup.DISPLAY_NAME);
                    }

                    callerInfo.phoneNumber = cv.getAsString(PhoneLookup.NUMBER);

                    if (cv.containsKey(PhoneLookup.TYPE) && cv.containsKey(PhoneLookup.LABEL)) {
                        callerInfo.numberType = cv.getAsInteger(PhoneLookup.TYPE);
                        callerInfo.numberLabel = cv.getAsString(PhoneLookup.LABEL);
                        callerInfo.phoneLabel = (String) android.provider.ContactsContract.CommonDataKinds.Phone
                                .getTypeLabel(ctxt.getResources(), callerInfo.numberType,
                                        callerInfo.numberLabel);
                    }

                    if (cv.containsKey(PhoneLookup._ID)) {
                        callerInfo.personId = cv.getAsLong(PhoneLookup._ID);

                        /*
                         * if(cv.containsKey(PhoneLookup.LOOKUP_KEY) ) {
                         * callerInfo.contactContentUri =
                         * Contacts.getLookupUri(callerInfo.personId ,
                         * cv.getAsString(PhoneLookup.LOOKUP_KEY)); }
                         */

                        callerInfo.contactContentUri = ContentUris.withAppendedId(
                                Contacts.CONTENT_URI, callerInfo.personId);
                    }

                    if (cv.containsKey(PhoneLookup.CUSTOM_RINGTONE)) {
                        callerInfo.contactRingtoneUri = Uri.parse(cv
                                .getAsString(PhoneLookup.CUSTOM_RINGTONE));
                    }

                    if (cv.containsKey(PhoneLookup.PHOTO_ID)) {
                        callerInfo.photoId = cv.getAsLong(PhoneLookup.PHOTO_ID);
                    }

                    if (cv.containsKey(PhoneLookup.PHOTO_URI)) {
                        callerInfo.photoUri = Uri.parse(cv.getAsString(PhoneLookup.PHOTO_URI));
                    }

                    if (callerInfo.name != null && callerInfo.name.length() == 0) {
                        callerInfo.name = null;
                    }

                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Exception while retrieving cursor infos");
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
    public Cursor getContactsPhones(Context ctxt) {

        Uri uri = ContactsContract.Data.CONTENT_URI;

        // Has phone number
        String isPhoneType = "(" + Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                + "' AND " + CommonDataKinds.Phone.NUMBER + " IS NOT NULL ) ";

        // Has sip uri
        if (Compatibility.isCompatible(9)) {
            isPhoneType += " OR " + Data.MIMETYPE + "='" + CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE;
        }
        // Sip: IM custo
        isPhoneType += " OR (" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='sip'" + ")";

        // CSip IM custo
        isPhoneType += " OR (" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='csip'" + ")";

        String query = Contacts.DISPLAY_NAME + " IS NOT NULL "
                + " AND (" + isPhoneType + ")";


        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection =  new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.LOOKUP_KEY,
                    Data.PHOTO_URI
            };
        } else {
            projection =  new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.LOOKUP_KEY
            };
        }
        
        Cursor resCursor = ctxt.getContentResolver().query(uri, 
                projection, query,
                null, Data.DISPLAY_NAME + " ASC");
        
        return resCursor;
    }
    

    @Override
    public void bindContactPhoneView(View view, Context context, Cursor cursor) {

        // Get values
        String value = cursor.getString(cursor.getColumnIndex(Data.DATA1));
        String displayName = cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME));
        Long contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Bitmap bitmap = getContactPhoto(context, uri, R.drawable.picture_unknown);

        
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
        return c.getColumnIndex(Contacts.DISPLAY_NAME);
    }

    @Override
    public Cursor getContactsByGroup(Context ctxt, String groupName) {

        if(TextUtils.isEmpty(groupName)) {
            return null;
        }
        
        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection = new String[] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                    Contacts.PHOTO_ID,
                    Contacts.CONTACT_STATUS_ICON,
                    Contacts.CONTACT_STATUS_LABEL,
                    Contacts.CONTACT_STATUS_RES_PACKAGE,
                    Contacts.PHOTO_URI
            };
        } else {
            projection = new String[] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                    Contacts.PHOTO_ID,
                    Contacts.CONTACT_STATUS_ICON,
                    Contacts.CONTACT_STATUS_LABEL,
                    Contacts.CONTACT_STATUS_RES_PACKAGE,
            };
        }

        Uri searchUri = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, groupName);
        
        return ctxt.getContentResolver().query(searchUri, projection, null, null, Contacts.DISPLAY_NAME + " ASC");
    }
    
    
    //private HashMap<String, Long> csipDatasId = new HashMap<String, Long>();

    @Override
    public List<String> getCSipPhonesByGroup(Context ctxt, String groupName) {
        Uri dataUri = Data.CONTENT_URI;
        String dataQuery = Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " 
                + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM 
                + " AND " 
                + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='csip'";
        

        Cursor contacts = getContactsByGroup(ctxt, groupName);
        ArrayList<String> results = new ArrayList<String>();
        if(contacts != null) {
            try {
                while (contacts.moveToNext()) {
                    // get csip data
                    Cursor dataCursor = ctxt.getContentResolver()
                            .query(dataUri,
                                    new String[] {
                                        CommonDataKinds.Im._ID,
                                        CommonDataKinds.Im.DATA,
                                    },
                                    dataQuery + " AND " + Data.CONTACT_ID + "=?",
                                    new String[] {
                                        Long.toString(contacts.getLong(contacts
                                                .getColumnIndex(Contacts._ID)))
                                    }, null);
                    if(dataCursor != null && dataCursor.getCount() > 0) {
                        dataCursor.moveToFirst();
                        String val = dataCursor.getString(dataCursor.getColumnIndex(CommonDataKinds.Im.DATA));
                        //long id = dataCursor.getLong(dataCursor.getColumnIndex(CommonDataKinds.Im._ID));
                        if(!TextUtils.isEmpty(val)) {
                            results.add(val);
                            //csipDatasId.put(val, id);
                        }
                    }
                    dataCursor.close();
                }
            }catch(Exception e){
                Log.e(THIS_FILE, "Error while looping on contacts", e);
            }finally {
                contacts.close();
            }
        }
        return results;
    }
    

    @Override
    public void updateCSipPresence(Context ctxt, String buddyUri, PresenceStatus presStatus) {
        
        if(Compatibility.isCompatible(8)) {
//            if(csipDatasId.containsKey(buddyUri)) {
//                long dataId = csipDatasId.get(buddyUri);
                String status = "Unknown";
                int presence = StatusUpdates.OFFLINE;
                switch (presStatus) {
                    case ONLINE:
                        status = "Online";
                        presence = StatusUpdates.AVAILABLE;
                        break;
                    case OFFLINE:
                        status = "Offline";
                        presence = StatusUpdates.INVISIBLE;
                        break;
                    default:
                        break;
                }
                
                ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(StatusUpdates.CONTENT_URI);
                //builder.withValue(StatusUpdates.DATA_ID, dataId);
                builder.withValue(StatusUpdates.CUSTOM_PROTOCOL, "csip");
                builder.withValue(StatusUpdates.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
                builder.withValue(StatusUpdates.IM_HANDLE, buddyUri);
                builder.withValue(StatusUpdates.STATUS, status);
                builder.withValue(StatusUpdates.PRESENCE, presence);
                
                if(Compatibility.isCompatible(11)) {
                    builder.withValue(StatusUpdates.CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_VOICE );
                }
                builder.withValue(StatusUpdates.STATUS_RES_PACKAGE, "com.csipsimple");
                builder.withValue(StatusUpdates.STATUS_LABEL, R.string.app_name);
                builder.withValue(StatusUpdates.STATUS_ICON, R.drawable.ic_launcher_phone);
                builder.withValue(StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis());
                operationList.add(builder.build());
                /*
                builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
                builder.withSelection(Data._ID + " = '" + dataId + "'", null);
                builder.withValue(CommonDataKinds.Im.PRESENCE, presence);
                operationList.add(builder.build());
                */
                try {
                    ctxt.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
                } catch (RemoteException e) {
                    Log.e(THIS_FILE, "Can't update status", e);
                } catch (OperationApplicationException e) {
                    Log.e(THIS_FILE, "Can't update status", e);
                }

 //           }
            
            /*
            
            Uri dataUri = Data.CONTENT_URI;
            String dataQuery = Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                    + " AND " 
                    + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM 
                    + " AND " 
                    + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='csip'"
                    + " AND "
                    + CommonDataKinds.Im.DATA + "=?";
            
            ContentValues presenceValues = new ContentValues();
            int val = CommonDataKinds.Im.OFFLINE;
            switch (presStatus) {
                case ONLINE:
                    val = CommonDataKinds.Im.AVAILABLE;
                    break;
                case OFFLINE:
                    // TODO -- is that good?
                    val = CommonDataKinds.Im.AWAY;
                    break;
                default:
                    break;
            }
            presenceValues.put(CommonDataKinds.Im.PRESENCE_STATUS, val);
            ctxt.getContentResolver().update(dataUri, presenceValues, dataQuery, new String[] {buddyUri});
            */
        }
    }

    @Override
    public void bindContactView(View view, Context context, Cursor cursor) {

        // Get values
        String displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
        Long contactId = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
        CallerInfo ci = new CallerInfo();
        ci.contactContentUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        ci.photoId = cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID));
        int photoUriColIndex = cursor.getColumnIndex(Contacts.PHOTO_ID);

        if (photoUriColIndex >= 0) {
            String photoUri = cursor.getString(photoUriColIndex);
            if(!TextUtils.isEmpty(photoUri)) {
                ci.photoUri = Uri.parse(photoUri);
            }
        }
        
        // Get views
        TextView tv = (TextView) view.findViewById(R.id.contact_name);
        QuickContactBadge badge = (QuickContactBadge) view.findViewById(R.id.quick_contact_photo);

        // Bind
        tv.setText(displayName);

        badge.assignContactUri(ci.contactContentUri);
        ContactsAsyncHelper.updateImageViewWithContactPhotoAsync(context, badge.getImageView(),
                ci,
                SipHome.USE_LIGHT_THEME ? R.drawable.ic_contact_picture_holo_light
                        : R.drawable.ic_contact_picture_holo_dark);
    }





}
