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
/**
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2011, The Android Open Source Project
 */

package com.csipsimple.ui.calllog;

import android.net.Uri;
import android.provider.CallLog.Calls;

/**
 * The details of a phone call to be shown in the UI.
 */
public class PhoneCallDetails {
    /** The number of the other party involved in the call. */
    public final CharSequence number;
    /** The formatted version of {@link #number}. */
    public final CharSequence formattedNumber;
    /**
     * The type of calls, as defined in the call log table, e.g.,
     * {@link Calls#INCOMING_TYPE}.
     * <p>
     * There might be multiple types if this represents a set of entries grouped
     * together.
     */
    public final int[] callTypes;
    /** The date of the call, in milliseconds since the epoch. */
    public final long date;
    /** The duration of the call in milliseconds, or 0 for missed calls. */
    public final long duration;
    /** The name of the contact, or the empty string. */
    public final CharSequence name;
    /** The type of phone, e.g., {@link Phone#TYPE_HOME}, 0 if not available. */
    public final int numberType;
    /**
     * The custom label associated with the phone number in the contact, or the
     * empty string.
     */
    public final CharSequence numberLabel;
    /** The URI of the contact associated with this phone call. */
    public final Uri contactUri;
    /**
     * The photo URI of the picture of the contact that is associated with this
     * phone call or null if there is none.
     * <p>
     * This is meant to store the high-res photo only.
     */
    public final Uri photoUri;
    
    public final Long accountId;
    public final int statusCode;
    public final String statusText;

    /**
     * Create the details for a call with a number not associated with a
     * contact.
     */
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber, int[] callTypes,
            long date, long duration) {
        this(number, formattedNumber, callTypes, date, duration, "", 0, "",
                null, null);
    }
    /** Create the details for a call with a number associated with a contact. */
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber,
            int[] callTypes, long date, long duration,
            CharSequence name, int numberType, CharSequence numberLabel, Uri contactUri,
            Uri photoUri) {
        this(number, formattedNumber, callTypes, date, duration, null, 200, null, name, numberType, numberLabel, contactUri, photoUri);
    }
    
    /** Create the details for a call with a number associated with a contact. */
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber,
            int[] callTypes, long date, long duration,
            Long accountId, int statusCode, String statusText,
            CharSequence name, int numberType, CharSequence numberLabel, Uri contactUri,
            Uri photoUri) {
        this.number = number;
        this.formattedNumber = formattedNumber;
        this.callTypes = callTypes;
        this.date = date;
        this.duration = duration;
        this.name = name;
        this.numberType = numberType;
        this.numberLabel = numberLabel;
        this.contactUri = contactUri;
        this.photoUri = photoUri;
        this.accountId = accountId;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
}
