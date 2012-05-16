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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */

package com.csipsimple.api;

import android.net.Uri;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for Sip uri manipulation in java space.
 * Allows to parse sip uris and check it.
 *
 */
public final class SipUri {

    private SipUri() {
        // Singleton
    }

    private final static String SIP_SCHEME_RULE = "sip(?:s)?|tel";
    private final static String DIGIT_NBR_RULE = "^[0-9\\-#\\+\\*\\(\\)]+$";
    private final static Pattern SIP_CONTACT_ADDRESS_PATTERN = Pattern
            .compile("^([^@:]+)@([^@:]+)$");
    private final static Pattern SIP_CONTACT_PATTERN = Pattern
            .compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?("+SIP_SCHEME_RULE+"):([^@]+)@([^>]+)(?:>)?$");
    private final static Pattern SIP_HOST_PATTERN = Pattern
            .compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?("+SIP_SCHEME_RULE+"):([^@>]+)(?:>)?$");

    // Contact related
    /**
     * Holder for parsed sip contact information.<br/>
     * Basically wrap AoR.
     * We should have something like "{@link ParsedSipContactInfos#displayName} <{@link ParsedSipContactInfos#scheme}:{@link ParsedSipContactInfos#userName}@{@link ParsedSipContactInfos#domain}>
     */
    public static class ParsedSipContactInfos {
        /**
         * Contact display name.
         */
        public String displayName = "";
        /**
         * User name of AoR
         */
        public String userName = "";
        /**
         * Domaine name
         */
        public String domain = "";
        /**
         * Scheme of the protocol
         */
        public String scheme = "";
        

        @Override
        public String toString() {
            return toString(true);
        }
        
        public String toString(boolean includeDisplayName) {

            StringBuffer buildString = new StringBuffer();
            if(!TextUtils.isEmpty(scheme)) {
                buildString.append("<sip:");
            }else {
                buildString.append("<" + scheme + ":");
            }
            if(!TextUtils.isEmpty(userName)) {
                buildString.append(Uri.encode(userName) + "@");
            }
            buildString.append(domain + ">");
            
            // Append display name at beggining if necessary
            if (includeDisplayName && !TextUtils.isEmpty(displayName)) {
                // Prepend with space
                buildString.insert(0, " ");
                // Start with display name
                buildString.insert(0, Uri.encode(displayName));
            }
            return buildString.toString();
        }
    }

    /**
     * Parse a sip contact
     * 
     * @param sipUri string sip contact
     * @return a ParsedSipContactInfos which contains uri parts. If not match
     *         return the object with blank fields
     */
    public static ParsedSipContactInfos parseSipContact(String sipUri) {
        ParsedSipContactInfos parsedInfos = new ParsedSipContactInfos();

        if (!TextUtils.isEmpty(sipUri)) {
            Matcher m = SIP_CONTACT_PATTERN.matcher(sipUri);
            if (m.matches()) {
                parsedInfos.displayName = Uri.decode(m.group(1).trim());
                parsedInfos.domain = m.group(4);
                parsedInfos.userName = Uri.decode(m.group(3));
                parsedInfos.scheme = m.group(2);
            }else {
                // Try to consider that as host
                m = SIP_HOST_PATTERN.matcher(sipUri);
                if(m.matches()) {
                    parsedInfos.displayName = Uri.decode(m.group(1).trim());
                    parsedInfos.domain = m.group(3);
                    parsedInfos.scheme = m.group(2);
                }else {
                    m = SIP_CONTACT_ADDRESS_PATTERN.matcher(sipUri);
                    if(m.matches()) {
                        parsedInfos.userName = Uri.decode(m.group(1));
                        parsedInfos.domain = m.group(2);
                    }else {
                        // Final fallback, we have only a username given
                        parsedInfos.userName = sipUri;
                    }
                }
            }
        }

        return parsedInfos;
    }

    /**
     * Return what should be display as caller id for this sip uri This is the
     * merged and fancy way fallback to uri or user name if needed
     * 
     * @param uri the uri to display
     * @return the simple display
     */
    public static String getDisplayedSimpleContact(CharSequence uri) {
        // Reformat number
        if (uri != null) {
            String remoteContact = uri.toString();
            ParsedSipContactInfos parsedInfos = parseSipContact(remoteContact);

            if (!TextUtils.isEmpty(parsedInfos.displayName)) {
                // If available prefer the display name
                remoteContact = parsedInfos.displayName;
            } else if (!TextUtils.isEmpty(parsedInfos.userName)) {
                // Else, if available choose the username
                remoteContact = parsedInfos.userName;
            }
            return remoteContact;
        }
        return "";
    }

    /**
     * Check if username is an phone tel
     * 
     * @param phone username to check
     * @return true if look like a phone number
     */
    public static boolean isPhoneNumber(String phone) {
        return (!TextUtils.isEmpty(phone) && Pattern.matches(DIGIT_NBR_RULE, phone));
    }

    /**
     * Transform sip uri into something that doesn't depend on remote display
     * name
     * For example, if you give "Display Name" <sip:user@domain.com>
     * It will return sip:user@domain.com
     * 
     * @param sipContact full sip uri
     * @return simplified sip uri
     */
    public static String getCanonicalSipContact(String sipContact) {
        return getCanonicalSipContact(sipContact, true);
    }

    /**
     * Transform sip uri into something that doesn't depend on remote display
     * name
     * 
     * @param sipContact full sip uri
     * @param includeScheme whether to include scheme
     * @return the canonical sip contact <br/>
     *         Example sip:user@domain.com <br/>
     *         or user@domain.com (if include scheme is false)
     */
    public static String getCanonicalSipContact(String sipContact, boolean includeScheme) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(sipContact)) {
            Matcher m = SIP_CONTACT_PATTERN.matcher(sipContact);
            boolean hasUsername = false;

            if (m.matches()) {
                hasUsername = true;
            } else {
                m = SIP_HOST_PATTERN.matcher(sipContact);

            }

            if (m.matches()) {
                if (includeScheme) {
                    sb.append(m.group(2));
                    sb.append(":");
                }
                sb.append(m.group(3));
                if (hasUsername) {
                    sb.append("@");
                    sb.append(m.group(4));
                }
            } else {
                m = SIP_CONTACT_ADDRESS_PATTERN.matcher(sipContact);
                if(m.matches()) {
                    if(includeScheme) {
                        sb.append("sip:");
                    }
                    sb.append(sipContact);
                }else {
                    sb.append(sipContact);
                }
            }
        }

        return sb.toString();
    }

    // Uri related
    /**
     * Holder for parsed sip uri information.<br/>
     * We should have something like "{@link ParsedSipUriInfos#scheme}:{@link ParsedSipUriInfos#domain}:{@link ParsedSipUriInfos#port}"
     */
    public static class ParsedSipUriInfos {
        /**
         * Domain name/ip
         */
        public String domain = "";
        /**
         * Scheme of the protocol
         */
        public String scheme = "sip";
        /**
         * Port number
         */
        public int port = 5060;
    }

    private final static Pattern SIP_URI_PATTERN = Pattern.compile(
            "^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse an uri
     * 
     * @param sipUri the uri to parse
     * @return parsed object
     */
    public static ParsedSipUriInfos parseSipUri(String sipUri) {
        ParsedSipUriInfos parsedInfos = new ParsedSipUriInfos();

        if (!TextUtils.isEmpty(sipUri)) {
            Matcher m = SIP_URI_PATTERN.matcher(sipUri);
            if (m.matches()) {
                parsedInfos.scheme = m.group(1);
                parsedInfos.domain = m.group(2);
                if (m.group(3) != null) {
                    try {
                        parsedInfos.port = Integer.parseInt(m.group(3));
                    } catch (NumberFormatException e) {
                        // Log.e(THIS_FILE, "Unable to parse port number");
                    }
                }
            }
        }

        return parsedInfos;
    }
    
    public static Uri forgeSipUri(String scheme, String contact) {
        return Uri.fromParts(scheme, contact, null);
    }

}
