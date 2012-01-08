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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.contacts.ContactsWrapper;

/**
 * Looks up caller information for the given phone number.
 */
public class CallerInfo {

    private static final String THIS_FILE = "CallerInfo";

    public static final CallerInfo EMPTY = new CallerInfo();

    public boolean contactExists;

    public long personId;
    public String name;

    public String phoneNumber;
    public String phoneLabel;
    public int numberType;
    public String numberLabel;
    /** The photo for the contact, if available. */
    public long photoId;
    /** The high-res photo for the contact, if available. */
    public Uri photoUri;

    // fields to hold individual contact preference data,
    // including the send to voicemail flag and the ringtone
    // uri reference.
    public Uri contactRingtoneUri;
    public Uri contactContentUri;

    /**
     * Build and retrieve caller infos from contacts based on the caller sip uri
     * 
     * @param context Current application context
     * @param sipUri The remote contact sip uri
     * @return The caller info as CallerInfo object
     */
    public static CallerInfo getCallerInfoFromSipUri(Context context, String sipUri) {
        if (TextUtils.isEmpty(sipUri)) {
            return new CallerInfo();
        }

        CallerInfo callerInfo = new CallerInfo();
        if (!TextUtils.isEmpty(sipUri)) {

            ParsedSipContactInfos uriInfos = SipUri.parseSipContact(sipUri);
            if (SipUri.isPhoneNumber(uriInfos.userName)) {
                Log.d(THIS_FILE, "Number looks usable, try People lookup");
                callerInfo = ContactsWrapper.getInstance().findCallerInfo(context,
                        uriInfos.userName);
            }
            // If contact uid doesn't match; we can try with contact display
            // name
            if (callerInfo == null && SipUri.isPhoneNumber(uriInfos.displayName)) {
                Log.d(THIS_FILE, "Display name looks usable, try People lookup");
                callerInfo = ContactsWrapper.getInstance().findCallerInfo(context,
                        uriInfos.displayName);
            }
        }

        return callerInfo;
    }

}
