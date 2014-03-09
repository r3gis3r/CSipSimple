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
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipUri;
import com.csipsimple.models.CallerInfo;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@TargetApi(5)
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

    private static final String DISPLAY_NAME_ORDER = Contacts.DISPLAY_NAME + " COLLATE LOCALIZED";
    private static final String SORT_ORDER = Contacts.TIMES_CONTACTED + " DESC,"
            +  DISPLAY_NAME_ORDER + "," + CommonDataKinds.Phone.TYPE;
    private static final String THIS_FILE = "ContactsUtils5";

    public Bitmap getContactPhoto(Context ctxt, Uri uri, boolean hiRes, Integer defaultResource) {
        Bitmap img = null;
        InputStream s = ContactsContract.Contacts.openContactPhotoInputStream(
                ctxt.getContentResolver(), uri);
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

    public List<Phone> getPhoneNumbers(Context ctxt, long contactId, int flag) {
        String id = Long.toString(contactId);
        ArrayList<Phone> phones = new ArrayList<Phone>();
        Cursor pCur;
        
        if ((flag & ContactsWrapper.URI_NBR) > 0) {
            pCur = ctxt.getContentResolver().query(
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
        }

        // Add any custom IM named 'sip' and set its type to 'sip'
        if ((flag & ContactsWrapper.URI_IM) > 0) {
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
                    String proto = pCur.getString(pCur
                            .getColumnIndex(CommonDataKinds.Im.CUSTOM_PROTOCOL));
                    if (SipManager.PROTOCOL_SIP.equalsIgnoreCase(proto) || SipManager.PROTOCOL_CSIP.equalsIgnoreCase(proto)) {
                        phones.add(new Phone(pCur.getString(pCur
                                .getColumnIndex(CommonDataKinds.Im.DATA)), SipManager.PROTOCOL_SIP));
                    }
                }
    
            }
            pCur.close();
        }
        
        // Add any SIP uri if android 9
        if (Compatibility.isCompatible(9) && ((flag & ContactsWrapper.URI_SIP) > 0)) {
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
                        .getColumnIndex(ContactsContract.Data.DATA1)), SipManager.PROTOCOL_SIP));
            }
            pCur.close();
        }

        return (phones);
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
                    PhoneLookup.CUSTOM_RINGTONE,
                    PhoneLookup.PHOTO_URI
            };
        } else {
            projection = new String[] {
                    PhoneLookup._ID,
                    PhoneLookup.DISPLAY_NAME,
                    PhoneLookup.TYPE,
                    PhoneLookup.LABEL,
                    PhoneLookup.NUMBER,
                    PhoneLookup.CUSTOM_RINGTONE,
                    PhoneLookup.LOOKUP_KEY
            };
        }
        Cursor cursor = null;
        try {
            cursor = ctxt.getContentResolver().query(searchUri, projection, null, null, null);
        }catch(SQLException e) {
            Log.e(THIS_FILE, "Stock contact app is not able to resolve contacts", e);
        }
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
                        callerInfo.contactContentUri = ContentUris.withAppendedId(
                                Contacts.CONTENT_URI, callerInfo.personId);
                    }

                    if (cv.containsKey(PhoneLookup.CUSTOM_RINGTONE)) {
                        String cRt = cv.getAsString(PhoneLookup.CUSTOM_RINGTONE);
                        if(!TextUtils.isEmpty(cRt)) {
                            callerInfo.contactRingtoneUri = Uri.parse(cRt);
                        }
                    }

                    if (cv.containsKey(PhoneLookup.PHOTO_ID) && cv.getAsLong(PhoneLookup.PHOTO_ID) != null) {
                        callerInfo.photoId = cv.getAsLong(PhoneLookup.PHOTO_ID);
                    }

                    if (cv.containsKey(PhoneLookup.PHOTO_URI)) {
                        String cPu = cv.getAsString(PhoneLookup.PHOTO_URI);
                        if(!TextUtils.isEmpty(cPu)) {
                            callerInfo.photoUri = Uri.parse(cPu);
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
        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection = new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.CUSTOM_RINGTONE,
                    Data.LOOKUP_KEY,
                    Data.PHOTO_URI
            };
        } else {
            projection = new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.CUSTOM_RINGTONE,
                    Data.LOOKUP_KEY
            };
        }
        

        Uri uri = Data.CONTENT_URI;

        // Has phone number
        String whereSipUriClause = "(" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='"+SipManager.PROTOCOL_SIP+"'" + ")";

        // CSip IM custo
        whereSipUriClause += " OR (" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='"+SipManager.PROTOCOL_CSIP+"'" + ")";

        // Has sip uri
        if (Compatibility.isCompatible(9)) {
            whereSipUriClause += " OR " + Data.MIMETYPE + "='"
                    + CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE + "'";
        }
        
        String query = Contacts.DISPLAY_NAME + " IS NOT NULL "
                + " AND (" + whereSipUriClause + ") AND " + Data.DATA1 + "=?";
        

        Cursor cursor = ctxt.getContentResolver().query(uri,
                projection, query,
                new String[] {sipUri}, 
                Data.DISPLAY_NAME + " ASC");
        
        
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();

                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, cv);
                    callerInfo.contactExists = true;
                    if (cv.containsKey(Data.DISPLAY_NAME)) {
                        callerInfo.name = cv.getAsString(Data.DISPLAY_NAME);
                    }

                    callerInfo.phoneNumber = sipUri;

                    callerInfo.numberLabel = "sip";
                    callerInfo.phoneLabel = "sip";

                    if (cv.containsKey(Data.CONTACT_ID)) {
                        callerInfo.personId = cv.getAsLong(Data.CONTACT_ID);
                        
                        callerInfo.contactContentUri = ContentUris.withAppendedId(
                                Contacts.CONTENT_URI, callerInfo.personId);
                    }

                    if (cv.containsKey(Data.CUSTOM_RINGTONE)) {
                        String cRt = cv.getAsString(Data.CUSTOM_RINGTONE);
                        if(!TextUtils.isEmpty(cRt)) {
                            callerInfo.contactRingtoneUri = Uri.parse(cRt);
                        }
                    }

                    if (cv.containsKey(Data.PHOTO_ID) && cv.getAsLong(Data.PHOTO_ID) != null) {
                        callerInfo.photoId = cv.getAsLong(Data.PHOTO_ID);
                    }

                    if (cv.containsKey(Data.PHOTO_URI)) {
                        String cPu = cv.getAsString(Data.PHOTO_URI);
                        if(!TextUtils.isEmpty(cPu)) {
                            callerInfo.photoUri = Uri.parse(cPu);
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
        
        
        return callerInfo;
    }

    @Override
    public CallerInfo findSelfInfo(Context ctxt) {
        CallerInfo callerInfo = new CallerInfo();
        return callerInfo;
    }

    @Override
    public Cursor getContactsPhones(Context ctxt, CharSequence constraint) {

        Uri uri = Data.CONTENT_URI;

        // Has phone number
        String isPhoneType = "(" + Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                + "' AND " + CommonDataKinds.Phone.NUMBER + " IS NOT NULL ) ";

        // Has sip uri
        if (Compatibility.isCompatible(9)) {
            isPhoneType += " OR (" + Data.MIMETYPE + "='"
                    + CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE + "')";
        }
        // Sip: IM custo
        isPhoneType += " OR (" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='"+SipManager.PROTOCOL_SIP+"'" + ")";

        // CSip IM custo
        isPhoneType += " OR (" + Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND " + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "='"+SipManager.PROTOCOL_CSIP+"'" + ")";

        String query = Contacts.DISPLAY_NAME + " IS NOT NULL "
                + " AND (" + isPhoneType + ")";

        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection = new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.LOOKUP_KEY,
                    Data.PHOTO_URI,
                    Data.MIMETYPE,
                    Data.DATA2,
                    Data.DATA3,
                    Data.DATA5,
                    Data.DATA6
            };
        } else {
            projection = new String[] {
                    Data._ID,
                    Data.CONTACT_ID,
                    Data.DATA1,
                    Data.DISPLAY_NAME,
                    Data.PHOTO_ID,
                    Data.LOOKUP_KEY,
                    Data.MIMETYPE,
                    Data.DATA2,
                    Data.DATA3,
                    Data.DATA5,
                    Data.DATA6
            };
        }
        // Treat constraint
        String[] selectionArgs = null;
        if (!TextUtils.isEmpty(constraint)) {
            String phoneConstraint = null;
            boolean isDigitOnly = constraint.toString().matches("^[0-9\\-\\(\\)+]+$");
            if (usefulAsDigits(constraint)) {
                phoneConstraint = PhoneNumberUtils.convertKeypadLettersToDigits(constraint.toString());
                if (!phoneConstraint.equals(constraint.toString())) {
                    phoneConstraint = phoneConstraint.trim();
                }
            }
            
            // Start filter condition
            ArrayList<String> selectionArgsArray = new ArrayList<String>();
            query += " AND (";
            // Filter the data (aka phone number or sip uri)
            query += String.format("%s LIKE ?", Data.DATA1);
            selectionArgsArray.add(constraint + "%");
            if(!TextUtils.isEmpty(phoneConstraint) && !phoneConstraint.equals(constraint)) {
                query += String.format(" OR %s LIKE ?", Data.DATA1);
                selectionArgsArray.add(phoneConstraint + "%");
            }
            // Filter the contact based on other data such as name
            if(!TextUtils.isEmpty(constraint) && !isDigitOnly) {
                query += " OR " + Data.RAW_CONTACT_ID + " IN " +
                    "(SELECT name_data." + Data.RAW_CONTACT_ID +
                        " FROM view_data AS name_data"+
                        " WHERE name_data." + Data.MIMETYPE + "='" + CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'" + 
                        " AND (" + 
                              "name_data."+CommonDataKinds.StructuredName.FAMILY_NAME + " LIKE ? OR " +
                              "name_data."+CommonDataKinds.StructuredName.GIVEN_NAME + " LIKE ?" +
                        ")"+
                    ")";
                selectionArgsArray.add(constraint + "%");
                selectionArgsArray.add(constraint + "%");
            }
            query += ")";
            selectionArgs = selectionArgsArray.toArray(new String[selectionArgsArray.size()]);
            
        }

        Cursor resCursor = ctxt.getContentResolver().query(uri,
                projection, query, selectionArgs, Data.DISPLAY_NAME + " ASC");

        return resCursor;
    }
    
    @Override
    public CharSequence transformToSipUri(Context ctxt, Cursor cursor) {
        String value = cursor.getString(cursor.getColumnIndex(Data.DATA1));
        if (value == null) {
            return "";
        }
        value = value.trim();
        return value;
    }
    /* (non-Javadoc)
     * @see com.csipsimple.utils.contacts.ContactsWrapper#isExternalPhoneNumber(android.content.Context, android.database.Cursor)
     */
    @Override
    public boolean isExternalPhoneNumber(Context context, Cursor cursor) {
        String mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
        return CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equalsIgnoreCase(mimeType);
    }

    /* (non-Javadoc)
     * @see com.csipsimple.utils.contacts.ContactsWrapper#bindContactPhoneView(android.view.View, android.content.Context, android.database.Cursor)
     */
    @Override
    public void bindContactPhoneView(View view, Context context, Cursor cursor) {
        // Get values
        String value = cursor.getString(cursor.getColumnIndex(Data.DATA1));
        String displayName = cursor.getString(cursor.getColumnIndex(Data.DISPLAY_NAME));
        Long contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Bitmap bitmap = getContactPhoto(context, uri, false, R.drawable.ic_contact_picture_holo_dark);
        CharSequence labelName = "";
        String mimeType = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
        if(CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equalsIgnoreCase(mimeType)) {
            int typeColumnIdx = cursor.getColumnIndex(CommonDataKinds.Phone.TYPE);
            int labelColumnIdx = cursor.getColumnIndex(CommonDataKinds.Phone.LABEL);
            if(labelColumnIdx != -1 && typeColumnIdx != -1) {
                String labelField = cursor.getString(labelColumnIdx);
                int typeField = cursor.getInt(typeColumnIdx);
                labelName = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), typeField, labelField);
            }
        }else if(CommonDataKinds.Im.CONTENT_ITEM_TYPE.equalsIgnoreCase(mimeType)) {
            int typeColumnIdx = cursor.getColumnIndex(CommonDataKinds.Im.PROTOCOL);
            int labelColumnIdx = cursor.getColumnIndex(CommonDataKinds.Im.CUSTOM_PROTOCOL);
            if(typeColumnIdx != -1 && labelColumnIdx != -1) {
                String labelField = cursor.getString(labelColumnIdx);
                int typeField = cursor.getInt(typeColumnIdx);
                labelName = ContactsContract.CommonDataKinds.Im.getProtocolLabel(context.getResources(), typeField, labelField);
            }
        }
        
        // Get views
        TextView tv = (TextView) view.findViewById(R.id.name);
        TextView label = (TextView) view.findViewById(R.id.label);
        TextView sub = (TextView) view.findViewById(R.id.number);
        ImageView imageView = (ImageView) view.findViewById(R.id.contact_photo);
        

        // Bind
        view.setTag(value);
        tv.setText(displayName);
        sub.setText(value);
        // When there's no label, getDisplayLabel() returns a CharSequence of  length==1 containing a unicode non-breaking space.
        // Need to check for that and consider that as "no label".
        if (TextUtils.isEmpty(labelName) || (labelName.length() == 1 && labelName.charAt(0) == '\u00A0')) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(labelName);
            label.setVisibility(View.VISIBLE);
        }
        if(imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
//        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, cursor.getLong(CONTACT_ID_INDEX));
//        ContactsAsyncHelper.updateImageViewWithContactAsync(context, imageView, uri, R.drawable.ic_contact_picture_holo_dark);
    }

    @Override
    public int getContactIndexableColumnIndex(Cursor c) {
        return c.getColumnIndex(Contacts.DISPLAY_NAME);
    }

    @Override
    public Cursor getContactsByGroup(Context ctxt, String groupName) {

        if (TextUtils.isEmpty(groupName)) {
            return null;
        }

        String[] projection;
        if (Compatibility.isCompatible(11)) {
            projection = new String[] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                    Contacts.PHOTO_ID,
                    Contacts.CONTACT_STATUS_ICON,
                    Contacts.CONTACT_STATUS,
                    Contacts.CONTACT_PRESENCE,
                    Contacts.PHOTO_URI
            };
        } else {
            projection = new String[] {
                    Contacts._ID,
                    Contacts.DISPLAY_NAME,
                    Contacts.PHOTO_ID,
                    Contacts.CONTACT_STATUS,
                    Contacts.CONTACT_PRESENCE
            };
        }

        Uri searchUri = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, Uri.encode(groupName));
        
        
        Cursor c = null;
        try {
            c = ctxt.getContentResolver().query(searchUri, projection, null, null,
                    Contacts.DISPLAY_NAME + " ASC");
        } catch(Exception e) {
            Log.e(THIS_FILE, "Error while retrieving group", e);
        }
        return c;
    }

    @Override
    public List<String> getCSipPhonesByGroup(Context ctxt, String groupName) {

        Cursor contacts = getContactsByGroup(ctxt, groupName);
        ArrayList<String> results = new ArrayList<String>();
        if (contacts != null) {
            try {
                while (contacts.moveToNext()) {
                    List<String> res = getCSipPhonesContact(ctxt, contacts.getLong(contacts
                            .getColumnIndex(Contacts._ID)));
                    results.addAll(res);
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error while looping on contacts", e);
            } finally {
                contacts.close();
            }
        }
        return results;
    }

    @Override
    public List<String> getCSipPhonesContact(Context ctxt, Long contactId) {
        ArrayList<String> results = new ArrayList<String>();
        Uri dataUri = Data.CONTENT_URI;
        String dataQuery = Data.MIMETYPE + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' "
                + " AND "
                + CommonDataKinds.Im.PROTOCOL + "=" + CommonDataKinds.Im.PROTOCOL_CUSTOM
                + " AND "
                + " LOWER(" + CommonDataKinds.Im.CUSTOM_PROTOCOL + ")='"+SipManager.PROTOCOL_CSIP+"'";
        // get csip data
        Cursor dataCursor = ctxt.getContentResolver()
                .query(dataUri,
                        new String[] {
                                CommonDataKinds.Im._ID,
                                CommonDataKinds.Im.DATA,
                        },
                        dataQuery + " AND " + CommonDataKinds.Im.CONTACT_ID + "=?",
                        new String[] {
                            Long.toString(contactId)
                        }, null);

        try {
            if (dataCursor != null && dataCursor.getCount() > 0) {
                dataCursor.moveToFirst();
                String val = dataCursor.getString(dataCursor
                        .getColumnIndex(CommonDataKinds.Im.DATA));
                if (!TextUtils.isEmpty(val)) {
                    results.add(val);
                }
            }
        } catch (Exception e) {
            Log.e(THIS_FILE, "Error while looping on data", e);
        } finally {
            dataCursor.close();
        }
        
        return results;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    @Override
    public void updateCSipPresence(Context ctxt, String buddyUri,
            SipManager.PresenceStatus presStatus, String statusText) {

        if (Compatibility.isCompatible(8)) {
            // if(csipDatasId.containsKey(buddyUri)) {
            // long dataId = csipDatasId.get(buddyUri);
            int presence = StatusUpdates.OFFLINE;
            String correspondingPresence = "";
            switch (presStatus) {
                case ONLINE:
                    presence = StatusUpdates.AVAILABLE;
                    correspondingPresence = ctxt.getString(R.string.online);
                    break;
                case OFFLINE:
                    presence = StatusUpdates.INVISIBLE;
                    correspondingPresence = ctxt.getString(R.string.offline);
                    break;
                case AWAY:
                    presence = StatusUpdates.AWAY;
                    correspondingPresence = ctxt.getString(R.string.away);
                    break;
                case BUSY:
                    presence = StatusUpdates.DO_NOT_DISTURB;
                    correspondingPresence = ctxt.getString(R.string.busy);
                    break;
                default:
                    break;
            }
            if(TextUtils.isEmpty(statusText)) {
                statusText = correspondingPresence;
            }

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newInsert(StatusUpdates.CONTENT_URI);
            // builder.withValue(StatusUpdates.DATA_ID, dataId);
            builder.withValue(StatusUpdates.CUSTOM_PROTOCOL, SipManager.PROTOCOL_CSIP);
            builder.withValue(StatusUpdates.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
            builder.withValue(StatusUpdates.IM_HANDLE, buddyUri);
            builder.withValue(StatusUpdates.STATUS, statusText);
            builder.withValue(StatusUpdates.PRESENCE, presence);

            if (Compatibility.isCompatible(11)) {
                builder.withValue(StatusUpdates.CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_VOICE);
            }

            String pkg = PreferencesProviderWrapper.getCurrentPackageInfos(ctxt).applicationInfo.packageName;
            builder.withValue(StatusUpdates.STATUS_RES_PACKAGE, pkg);
            builder.withValue(StatusUpdates.STATUS_LABEL, R.string.app_name);
            builder.withValue(StatusUpdates.STATUS_ICON, R.drawable.ic_launcher_phone);
            builder.withValue(StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis());
            operationList.add(builder.build());
            /*
             * builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
             * builder.withSelection(Data._ID + " = '" + dataId + "'", null);
             * builder.withValue(CommonDataKinds.Im.PRESENCE, presence);
             * operationList.add(builder.build());
             */
            try {
                ctxt.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Can't update status", e);
            } catch (OperationApplicationException e) {
                Log.e(THIS_FILE, "Can't update status", e);
            }

            // }

            /*
             * Uri dataUri = Data.CONTENT_URI; String dataQuery = Data.MIMETYPE
             * + "='" + CommonDataKinds.Im.CONTENT_ITEM_TYPE + "' " + " AND " +
             * CommonDataKinds.Im.PROTOCOL + "=" +
             * CommonDataKinds.Im.PROTOCOL_CUSTOM + " AND " +
             * CommonDataKinds.Im.CUSTOM_PROTOCOL + "='csip'" + " AND " +
             * CommonDataKinds.Im.DATA + "=?"; ContentValues presenceValues =
             * new ContentValues(); int val = CommonDataKinds.Im.OFFLINE; switch
             * (presStatus) { case ONLINE: val = CommonDataKinds.Im.AVAILABLE;
             * break; case OFFLINE: // TODO -- is that good? val =
             * CommonDataKinds.Im.AWAY; break; default: break; }
             * presenceValues.put(CommonDataKinds.Im.PRESENCE_STATUS, val);
             * ctxt.getContentResolver().update(dataUri, presenceValues,
             * dataQuery, new String[] {buddyUri});
             */
        }
    }

    @Override
    public ContactInfo getContactInfo(Context context, Cursor cursor) {
        ContactInfo ci = new ContactInfo();
        // Get values
        ci.displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
        ci.contactId = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
        ci.callerInfo.contactContentUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, ci.contactId);
        ci.callerInfo.photoId = cursor.getLong(cursor.getColumnIndex(Contacts.PHOTO_ID));
        int photoUriColIndex = cursor.getColumnIndex(Contacts.PHOTO_ID);
        ci.status = cursor.getString(cursor.getColumnIndex(Contacts.CONTACT_STATUS));
        ci.presence = cursor.getInt(cursor.getColumnIndex(Contacts.CONTACT_PRESENCE));

        if (photoUriColIndex >= 0) {
            String photoUri = cursor.getString(photoUriColIndex);
            if (!TextUtils.isEmpty(photoUri)) {
                ci.callerInfo.photoUri = Uri.parse(photoUri);
            }
        }
        ci.hasPresence = !TextUtils.isEmpty(ci.status);
        return ci;
    }
    
    public int getPresenceIconResourceId(int presence) {
        return StatusUpdates.getPresenceIconResourceId(presence);
    }

    @Override
    public Intent getAddContactIntent(String displayName, String csipUri) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT, Contacts.CONTENT_URI);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);

        if (!TextUtils.isEmpty(displayName)) {
            intent.putExtra(Insert.NAME, displayName);
        }

        if (!TextUtils.isEmpty(csipUri)) {
            ArrayList<ContentValues> data = new ArrayList<ContentValues>();
            ContentValues csipProto = new ContentValues();
            csipProto.put(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE);
            csipProto.put(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
            csipProto.put(CommonDataKinds.Im.CUSTOM_PROTOCOL, SipManager.PROTOCOL_CSIP);
            csipProto.put(CommonDataKinds.Im.DATA, SipUri.getCanonicalSipContact(csipUri, false));
            data.add(csipProto);

            intent.putParcelableArrayListExtra(Insert.DATA, data);
        }

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
        Uri searchUri = Groups.CONTENT_URI;
        String[] projection = new String[] {
                Groups._ID,
                Groups.TITLE /* No need of as title, since already title */
        };
        
        return context.getContentResolver().query(searchUri, projection, null, null,
                Groups.TITLE + " ASC");
    }

    @Override
    public boolean insertOrUpdateCSipUri(Context ctxt, long contactId, String uri) {

        ContentResolver cr = ctxt.getContentResolver();
        long rawContactId = -1;
        Cursor c = cr.query(RawContacts.CONTENT_URI,
                new String[]{RawContacts._ID},
                RawContacts.CONTACT_ID + "=?",
                new String[]{String.valueOf(contactId)}, null);
        try {
            if(c.moveToNext()) {
                rawContactId = c.getLong(c.getColumnIndex(RawContacts._ID));
            }
        } catch (Exception e) {
            Log.e(THIS_FILE, "Error while looping on contacts", e);
        } finally {
            c.close();
        }
        
        if(rawContactId != -1) {
            String csipUri = SipUri.getCanonicalSipContact(uri, false);
            // First try update
            ContentValues cv = new ContentValues();
            cv.put(CommonDataKinds.Im.DATA, csipUri);
            Cursor cs = cr.query(Data.CONTENT_URI, new String[] {CommonDataKinds.Im._ID},
                    CommonDataKinds.Im.MIMETYPE + "=?"
                    + " AND " + CommonDataKinds.Im.PROTOCOL + "=?" 
                    + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "=?" 
                    + " AND " + CommonDataKinds.Im.RAW_CONTACT_ID + "=?", new String [] {
                CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                Integer.toString(CommonDataKinds.Im.PROTOCOL_CUSTOM),
                SipManager.PROTOCOL_CSIP,
                Long.toString(rawContactId)
            }, null);
            if(cs != null) {
                int count = cs.getCount();
                cs.close();
                
                
                if(count > 0) {
                    int updated = cr.update(Data.CONTENT_URI, cv, 
                        CommonDataKinds.Im.MIMETYPE + "=?"
                        + " AND " + CommonDataKinds.Im.PROTOCOL + "=?" 
                        + " AND " + CommonDataKinds.Im.CUSTOM_PROTOCOL + "=?" 
                        + " AND " + CommonDataKinds.Im.RAW_CONTACT_ID + "=?", new String [] {
                    CommonDataKinds.Im.CONTENT_ITEM_TYPE,
                    Integer.toString(CommonDataKinds.Im.PROTOCOL_CUSTOM),
                    SipManager.PROTOCOL_CSIP,
                    Long.toString(rawContactId)
                    });
                    Log.d(THIS_FILE, "Updated : " + updated);
                }else {
                    cv.put(CommonDataKinds.Im.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE);
                    cv.put(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
                    cv.put(CommonDataKinds.Im.CUSTOM_PROTOCOL, SipManager.PROTOCOL_CSIP);
                    cv.put(CommonDataKinds.Im.RAW_CONTACT_ID, rawContactId);
                    Uri insertedUri = cr.insert(Data.CONTENT_URI, cv);
                    if(insertedUri == null) {
                        return false;
                    }
                    Log.d(THIS_FILE, "Inserted : " + insertedUri.toString());
                }
                
                return true;
            }
        }
        return false;
    }



}
