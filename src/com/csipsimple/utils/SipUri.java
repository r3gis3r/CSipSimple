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
package com.csipsimple.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.text.TextUtils;

public class SipUri {

	private static final String THIS_FILE = "SipUri";
	
	private static Pattern sipUriSpliter = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?(sip(?:s)?):([^@]*)@([^>]*)(?:>)?");
	private static String digitNumberPatter = "^[0-9\\-#\\+\\*\\(\\)]+$";
	
	public static String getDisplayedSimpleUri(String uri) {
		// Reformat number
		Matcher m = sipUriSpliter.matcher(uri);
		String remoteContact = uri;
		if (m.matches()) {
			if (!TextUtils.isEmpty(m.group(3))) {
				remoteContact = m.group(3);
			} else if (!TextUtils.isEmpty(m.group(1))) {
				remoteContact = m.group(1);
			}
		}
		
		return remoteContact;
	}
	
	/**
	 * Parse a sip uri
	 * @param sipUri string sip uri
	 * @return a ParsedSipUriInfos which contains uri parts
	 */
	 public static ParsedSipUriInfos parseSipUri(String sipUri) {
    	ParsedSipUriInfos parsedInfos = new ParsedSipUriInfos();
    	
    	if(!TextUtils.isEmpty(sipUri)) {
	    	Log.d(THIS_FILE, "Parsing " + sipUri);
			Matcher m = sipUriSpliter.matcher(sipUri);
			if (m.matches()) {
				parsedInfos.displayName = m.group(1).trim();
				parsedInfos.userName =  m.group(3);
				Log.d(THIS_FILE, "Found contact login =" + parsedInfos.displayName+" display name = "+parsedInfos.userName);
			}
    	}
    	
    	return parsedInfos;
    }
    
    public static class ParsedSipUriInfos {
    	public String displayName;
    	public String userName;
    }
    
    /**
     * Check if username is an phone tel
     * @param phone username to check
     * @return true if look like a phone number
     */
    public static boolean isPhoneNumber(String phone) {
    	 return (!TextUtils.isEmpty(phone) && Pattern.matches(digitNumberPatter, phone));
    }
    
    /**
     * Transform sip uri into something that doesn't depend on remote display name
     * @param sipUri full sip uri
     * @return simplified sip uri
     */
    public static String getCanonicalSipUri(String sipUri) {
    	String result = sipUri;
    	Matcher m = sipUriSpliter.matcher(sipUri);
		if (m.matches()) {
			result = m.group(2)+":"+m.group(3)+"@"+m.group(4);
		}
		return result;
    }
}
