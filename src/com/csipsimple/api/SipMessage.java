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


import android.content.ContentValues;
import android.net.Uri;

public class SipMessage {

	public static final String FIELD_ID = "id";
	public static final String FIELD_FROM = "sender";
	public static final String FIELD_TO = "receiver";
	public static final String FIELD_CONTACT = "contact";
	public static final String FIELD_BODY = "body";
	public static final String FIELD_MIME_TYPE = "mimeType";
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_DATE = "date";
	public static final String FIELD_STATUS = "status";
	public static final String FIELD_READ = "read";
	public static final String FIELD_FROM_FULL = "full_sender";
	
	
    public static final int MESSAGE_TYPE_INBOX  = 1;
    public static final int MESSAGE_TYPE_SENT   = 2;
    public static final int MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
    public static final int MESSAGE_TYPE_QUEUED = 6; // for messages to send later
    

    // Content Provider - account
    public static final String MESSAGES_TABLE_NAME = "messages";
    public final static String MESSAGE_CONTENT_TYPE = SipManager.BASE_DIR_TYPE + ".message";
    public final static String MESSAGE_CONTENT_ITEM_TYPE = SipManager.BASE_ITEM_TYPE + ".message";
    public final static Uri MESSAGE_URI = Uri.parse(SipManager.CONTENT_SCHEME
            + SipManager.AUTHORITY + "/" + MESSAGES_TABLE_NAME);
    public final static Uri MESSAGE_ID_URI_BASE = Uri.parse(SipManager.CONTENT_SCHEME
            + SipManager.AUTHORITY + "/" + MESSAGES_TABLE_NAME + "/");
    public final static String THREAD_ALIAS = "thread";
    public final static Uri THREAD_URI = Uri.parse(SipManager.CONTENT_SCHEME
            + SipManager.AUTHORITY + "/" + THREAD_ALIAS);
    public final static Uri THREAD_ID_URI_BASE = Uri.parse(SipManager.CONTENT_SCHEME
            + SipManager.AUTHORITY + "/" + THREAD_ALIAS + "/");
    
    
    public static final int STATUS_NONE = -1;
	public static final String SELF = "SELF";
	
	
	private String from;
	private String fullFrom;
	private String to;
	private String contact;
	private String body;
	private String mimeType;
	private long date;
	private int type;
	private int status = STATUS_NONE;
	private boolean read = false;
	
	public SipMessage(String aForm, String aTo, String aContact, String aBody, String aMimeType, long aDate, int aType, String aFullFrom) {
		from = aForm;
		to = aTo;
		contact = aContact;
		body = aBody;
		mimeType = aMimeType;
		date = aDate;
		type = aType;
		fullFrom = aFullFrom;
	}
	
	public SipMessage(ContentValues cv) {
		from = cv.getAsString(FIELD_FROM);
		to = cv.getAsString(FIELD_TO);
		contact = cv.getAsString(FIELD_CONTACT);
		body = cv.getAsString(FIELD_BODY);
		mimeType = cv.getAsString(FIELD_MIME_TYPE);
		date = cv.getAsLong(FIELD_DATE);
		type = cv.getAsInteger(FIELD_TYPE);
		status = cv.getAsInteger(FIELD_STATUS);
		read = cv.getAsBoolean(FIELD_READ);
		fullFrom = cv.getAsString(FIELD_FROM_FULL);
	}
	
	
	public ContentValues getContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(FIELD_FROM, from);
		cv.put(FIELD_TO, to);
		cv.put(FIELD_CONTACT, contact);
		cv.put(FIELD_BODY, body);
		cv.put(FIELD_MIME_TYPE, mimeType);
		cv.put(FIELD_TYPE, type);
		cv.put(FIELD_DATE, date);
		cv.put(FIELD_STATUS, status);
		cv.put(FIELD_READ, read);
		cv.put(FIELD_FROM_FULL, fullFrom);
		return cv;
	}

	public String getFrom() {
		return from;
	}

	public String getBody() {
		return body;
	}

	public String getTo() {
		return to;
	}

	public void setRead(boolean b) {
		read = b;
		
	}
	
	public String getDisplayName() {
		return SipUri.getDisplayedSimpleContact(fullFrom);
	}
	
}
