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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

/**
 * Holder for a sip message.<br/>
 * It allows to prepare / unpack content values of a SIP message.
 */
public class SipMessage {

    /**
     * Primary key id.
     * 
     * @see Long
     */
    public static final String FIELD_ID = "id";
    /**
     * From / sender.
     * 
     * @see String
     */
    public static final String FIELD_FROM = "sender";
    /**
     * To / receiver.
     * 
     * @see String
     */
    public static final String FIELD_TO = "receiver";
    /**
     * Contact of the sip message.
     */
    public static final String FIELD_CONTACT = "contact";
    /**
     * Body / content of the sip message.
     * 
     * @see String
     */
    public static final String FIELD_BODY = "body";
    /**
     * Mime type of the sip message.
     * 
     * @see String
     */
    public static final String FIELD_MIME_TYPE = "mime_type";
    /**
     * Way type of the message.
     * 
     * @see Integer
     * @see #MESSAGE_TYPE_INBOX
     * @see #MESSAGE_TYPE_FAILED
     * @see #MESSAGE_TYPE_QUEUED
     * @see #MESSAGE_TYPE_SENT
     */
    public static final String FIELD_TYPE = "type";
    /**
     * Reception date of the message.
     * 
     * @see Long
     */
    public static final String FIELD_DATE = "date";
    /**
     * Latest pager status.
     * 
     * @see Integer
     */
    public static final String FIELD_STATUS = "status";
    /**
     * Read status of the message.
     * 
     * @see Boolean
     */
    public static final String FIELD_READ = "read";
    /**
     * Non canonical sip from
     * 
     * @see String
     */
    public static final String FIELD_FROM_FULL = "full_sender";

    /**
     * Message received type.
     */
    public static final int MESSAGE_TYPE_INBOX = 1;
    /**
     * Message sent type.
     */
    public static final int MESSAGE_TYPE_SENT = 2;
    /**
     * Failed outgoing message.
     */
    public static final int MESSAGE_TYPE_FAILED = 5;
    /**
     * Message to send later.
     */
    public static final int MESSAGE_TYPE_QUEUED = 6;

    // Content Provider - account
    /**
     * Table for sip message.
     */
    public static final String MESSAGES_TABLE_NAME = "messages";
    /**
     * Content type for sip message.
     */
    public static final String MESSAGE_CONTENT_TYPE = SipManager.BASE_DIR_TYPE + ".message";
    /**
     * Item type for a sip message.
     */
    public static final String MESSAGE_CONTENT_ITEM_TYPE = SipManager.BASE_ITEM_TYPE + ".message";
    /**
     * Uri for content provider of sip message
     */
    public static final Uri MESSAGE_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + MESSAGES_TABLE_NAME);
    /**
     * Base uri for sip message content provider.<br/>
     * To append with {@link #FIELD_ID}
     * 
     * @see ContentUris#appendId(android.net.Uri.Builder, long)
     */
    public static final Uri MESSAGE_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + MESSAGES_TABLE_NAME + "/");
    /**
     * Table for threads. <br/>
     * It's an alias.
     */
    public static final String THREAD_ALIAS = "thread";
    /**
     * Uri for content provider of threads view.
     */
    public static final Uri THREAD_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + THREAD_ALIAS);
    /**
     * Base uri for thread views.
     */
    public static final Uri THREAD_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + THREAD_ALIAS + "/");

    /**
     * Status unknown for a message.
     */
    public static final int STATUS_NONE = -1;
    /**
     * Constant to represent self as sender or receiver of the message.
     */
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

    /**
     * Construct from raw datas.
     * 
     * @param aForm {@link #FIELD_FROM}
     * @param aTo {@link #FIELD_TO}
     * @param aContact {@link #FIELD_CONTACT}
     * @param aBody {@link #FIELD_BODY}
     * @param aMimeType {@link #FIELD_MIME_TYPE}
     * @param aDate {@link #FIELD_DATE}
     * @param aType {@link #FIELD_TYPE}
     * @param aFullFrom {@link #FIELD_FROM_FULL}
     */
    public SipMessage(String aForm, String aTo, String aContact, String aBody, String aMimeType,
            long aDate, int aType, String aFullFrom) {
        from = aForm;
        to = aTo;
        contact = aContact;
        body = aBody;
        mimeType = aMimeType;
        date = aDate;
        type = aType;
        fullFrom = aFullFrom;
    }

    /**
     * Construct a sip message wrapper from a cursor retrieved with a
     * {@link ContentProvider} query on {@link #MESSAGES_TABLE_NAME}.
     * 
     * @param c the cursor to unpack
     */
    public SipMessage(Cursor c) {
        ContentValues args = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, args);
        createFromContentValue(args);
    }

    /**
     * Pack the object content value to store
     * 
     * @return The content value representing the message
     */
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
    
    public final void createFromContentValue(ContentValues args) {
        Integer tmp_i;
        String tmp_s;
        Long tmp_l;
        Boolean tmp_b;
        
        tmp_s = args.getAsString(FIELD_FROM);
        if(tmp_s != null) {
            from = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_TO);
        if(tmp_s != null) {
            to = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_CONTACT);
        if(tmp_s != null) {
            contact = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_BODY);
        if(tmp_s != null) {
            body = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_MIME_TYPE);
        if(tmp_s != null) {
            mimeType = tmp_s;
        }
        tmp_l = args.getAsLong(FIELD_DATE);
        if(tmp_l != null) {
            date = tmp_l;
        }
        tmp_i = args.getAsInteger(FIELD_TYPE);
        if(tmp_i != null) {
            type = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_STATUS);
        if(tmp_i != null) {
            status = tmp_i;
        }
        tmp_b = args.getAsBoolean(FIELD_READ);
        if(tmp_b != null) {
            read = tmp_b;
        }

        tmp_s = args.getAsString(FIELD_FROM_FULL);
        if(tmp_s != null) {
            fullFrom = tmp_s;
        }
    }

    /**
     * Get the from of the message.
     * 
     * @return the From of the sip message
     */
    public String getFrom() {
        return from;
    }

    /**
     * Get the body of the message.
     * 
     * @return the Body of the message
     */
    public String getBody() {
        return body;
    }

    /**
     * Get to of the message.
     * 
     * @return the To of the sip message
     */
    public String getTo() {
        return to;
    }

    /**
     * Set the message as read or unread.
     * 
     * @param b true when read.
     */
    public void setRead(boolean b) {
        read = b;

    }

    /**
     * Get the display name of remote party.
     * 
     * @return The remote display name
     */
    public String getDisplayName() {
        return SipUri.getDisplayedSimpleContact(fullFrom);
    }
    
    /**
     * Get the number of the remote party.
     * 
     * @return The remote party number
     */
    public String getRemoteNumber() {
        if (SipMessage.SELF.equalsIgnoreCase(from)) {
            return to;
        }else {
            return from;
        }
    }
    
    /**
     * Get the content of the body without error tag
     * @return The body of the sip message
     */
    public String getBodyContent() {
        int splitIndex = body.indexOf(" // ");
        if(splitIndex != -1) {
            return body.substring(0, splitIndex);
        }
        return body;
    }
    /**
     * Get optional error of the sip message
     *  
     * @return The error string repr if any, null otherwise.
     */
    public String getErrorContent() {
        if (status == SipMessage.STATUS_NONE
                || status == SipCallSession.StatusCode.OK
                || status == SipCallSession.StatusCode.ACCEPTED) {
            return null;
        }
        
        int splitIndex = body.indexOf(" // ");
        if(splitIndex != -1) {
            return body.substring(splitIndex + 4, body.length());
        }
        return null;
    }
    
    /**
     * Get the way of the message is send by the user of the application
     * 
     * @return true if message is sent by the user of the application
     */
    public boolean isOutgoing() {
        if (SipMessage.SELF.equalsIgnoreCase(from)) {
            return true;
        }else {
            return false;
        }
    }
    
    /**
     * Get the send/receive date of the message.
     * @return the date of the send of the message for outgoing, of receive for incoming.
     */
    public long getDate() {
        return date;
    }
    
    /**
     * Get the complete remote contact from which the message comes.<br/>
     * This includes display name.
     *  
     * @return the sip uri of remote contact as announced when sending this message.
     */
    public String getFullFrom() {
        return fullFrom;
    }

    /**
     * Get the type of the message.
     * 
     * @return Message type
     * @see #MESSAGE_TYPE_FAILED
     * @see #MESSAGE_TYPE_INBOX
     * @see #MESSAGE_TYPE_QUEUED
     * @see #MESSAGE_TYPE_SENT
     */
    public int getType() {
        return type;
    }

    /**
     * Get the mime type of the message.
     * 
     * @return the message mime type sent by remote party.
     */
    public String getMimeType() {
        return mimeType;
    }
}
