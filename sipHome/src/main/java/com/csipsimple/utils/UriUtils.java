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
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2011 The Android Open Source Project
 */

package com.csipsimple.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;

import com.csipsimple.api.SipManager;

/**
 * Utility methods for dealing with URIs.
 */
public class UriUtils {
    /** Static helper, not instantiable. */
    private UriUtils() {}

    /** Checks whether two URI are equal, taking care of the case where either is null. */
    public static boolean areEqual(Uri uri1, Uri uri2) {
        if (uri1 == null && uri2 == null) {
            return true;
        }
        if (uri1 == null || uri2 == null) {
            return false;
        }
        return uri1.equals(uri2);
    }

    /** Parses a string into a URI and returns null if the given string is null. */
    public static Uri parseUriOrNull(String uriString) {
        if (uriString == null) {
            return null;
        }
        return Uri.parse(uriString);
    }

    /** Converts a URI into a string, returns null if the given URI is null. */
    public static String uriToString(Uri uri) {
        return uri == null ? null : uri.toString();
    }
    
    /**
     * Detect if phone number is a uri
     * @param number The number to detect
     * @return true if look like a URI instead of a phone number 
     */
    public static boolean isUriNumber(String number) {
        // Note we allow either "@" or "%40" to indicate a URI, in case
        // the passed-in string is URI-escaped. (Neither "@" nor "%40"
        // will ever be found in a legal PSTN number.)
        return number != null && (number.contains("@") || number.contains("%40"));
    }

    private final static String SCHEME_IMTO = "imto";
    private final static String SCHEME_TEL = "tel";
    private final static String SCHEME_SMSTO = "smsto";
    private final static String SCHEME_SMS = "sms";
    private final static String AUTHORITY_CSIP = SipManager.PROTOCOL_CSIP;
    private final static String AUTHORITY_SIP = SipManager.PROTOCOL_SIP;
    private final static String AUTHORITY_SKYPE = "skype";

    public static String extractNumberFromIntent(Intent it, Context ctxt) {
        if(it == null) {
            return null;
        }
        String phoneNumber = null;
        String action = it.getAction();
        Uri data = it.getData();
        if(data != null && action != null) {
            phoneNumber = PhoneNumberUtils.getNumberFromIntent(it, ctxt);
        }
        
        if(phoneNumber == null) {
            if (action != null && data != null) {
                String scheme = data.getScheme();
                if(scheme != null) {
                    scheme = scheme.toLowerCase();
                }
                
                if (action.equalsIgnoreCase(Intent.ACTION_SENDTO)) {
                    // Send to action -- could be im or sms
                    if (SCHEME_IMTO.equals(scheme)) {
                        // Im sent
                        String auth = data.getAuthority();
                        if (AUTHORITY_CSIP.equals(auth) ||
                                AUTHORITY_SIP.equals(auth) ||
                                AUTHORITY_SKYPE.equals(auth) ) {
                            phoneNumber = data.getLastPathSegment();
                        }
                    }else if (SCHEME_SMSTO.equals(scheme) || SCHEME_SMS.equals(scheme)) {
                        phoneNumber = PhoneNumberUtils.stripSeparators(data.getSchemeSpecificPart());
                    }
                } else {
                    // Simple call intent
                   phoneNumber = data.getSchemeSpecificPart();
                } 
            }
        }else {
            if (action != null && data != null) {
                String scheme = data.getScheme();
                if(scheme != null) {
                    scheme = scheme.toLowerCase();
                    if(SCHEME_SMSTO.equals(scheme) || SCHEME_SMS.equals(scheme) || SCHEME_TEL.equals(scheme)) {
                        phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
                    }
                }
            }
        }
        
        return phoneNumber;
    }

}
