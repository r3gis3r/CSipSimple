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
package com.csipsimple.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.csipsimple.utils.Log;

public class SipUri {

	private static final String THIS_FILE = "SipUri";
	
	private static String digitNumberPatter = "^[0-9\\-#\\+\\*\\(\\)]+$";
	
	
	//Contact related

    public static class ParsedSipContactInfos {
    	public String displayName = "";
    	public String userName = "";
    	public String domain = "";
    	public String scheme = "sip";
    }
    private static Pattern sipContactSpliter = Pattern.compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?(sip(?:s)?):([^@]*)@([^>]*)(?:>)?");
	
	/**
	 * Parse a sip contact
	 * @param sipUri string sip contact
	 * @return a ParsedSipContactInfos which contains uri parts. If not match return the object with blank fields
	 */
	 public static ParsedSipContactInfos parseSipContact(String sipUri) {
    	ParsedSipContactInfos parsedInfos = new ParsedSipContactInfos();
    	
    	if(!TextUtils.isEmpty(sipUri)) {
	    	Log.d(THIS_FILE, "Parsing " + sipUri);
			Matcher m = sipContactSpliter.matcher(sipUri);
			if (m.matches()) {
				parsedInfos.displayName = m.group(1).trim();
				parsedInfos.domain = m.group(4);
				parsedInfos.userName =  m.group(3);
				parsedInfos.scheme = m.group(2);
			}
    	}
    	
    	return parsedInfos;
    }
    
    
	/**
	 * Return what should be display as caller id for this sip uri
	 * This is the merged and fancy way fallback to uri or user name if needed
	 * @param uri the uri to display
	 * @return the simple display
	 */
	public static String getDisplayedSimpleContact(String uri) {
		// Reformat number
		String remoteContact = uri;
		ParsedSipContactInfos parsedInfos = parseSipContact(uri);
		
		if (!TextUtils.isEmpty(parsedInfos.displayName)) {
			//If available prefer the display name
			remoteContact = parsedInfos.displayName;
		} else if (!TextUtils.isEmpty(parsedInfos.userName) ) {
			//Else, if available choose the username 
			remoteContact = parsedInfos.userName;
		}
		return remoteContact;
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
     * @param sipContact full sip uri
     * @return simplified sip uri
     */
    public static String getCanonicalSipContact(String sipContact) {
    	String result = sipContact;
    	Matcher m = sipContactSpliter.matcher(sipContact);
		if (m.matches()) {
			result = m.group(2)+":"+m.group(3)+"@"+m.group(4);
		}
		return result;
    }
    
    
    //Uri related
    public static class ParsedSipUriInfos {
    	public String domain = "";
    	public String scheme = "sip";
    	public int port = 5060;
    }
    
    private static Pattern sipUriSpliter = Pattern.compile("^(sip(?:s)?):([^:]*)(?::(\\d))?$", Pattern.CASE_INSENSITIVE);
	
    /**
     * Parse an uri
     * @param sipUri the uri to parse
     * @return parsed object
     */
	public static ParsedSipUriInfos parseSipUri(String sipUri) {
		ParsedSipUriInfos parsedInfos = new ParsedSipUriInfos();

		if (!TextUtils.isEmpty(sipUri)) {
			Log.d(THIS_FILE, "Parsing " + sipUri);
			Matcher m = sipUriSpliter.matcher(sipUri);
			if (m.matches()) {
				parsedInfos.scheme = m.group(1);
				parsedInfos.domain = m.group(2);
				if(m.group(3) != null) {
					try{
						parsedInfos.port = Integer.parseInt(m.group(3));
					}catch(NumberFormatException e) {
						Log.e(THIS_FILE, "Unable to parse port number");
					}
				}
			}
		}

		return parsedInfos;
	}
    
}
