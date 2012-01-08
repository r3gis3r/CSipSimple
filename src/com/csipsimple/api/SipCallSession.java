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
 *  
 *  This file and this file only is released under dual Apache license
 */

package com.csipsimple.api;

import android.os.Parcel;
import android.os.Parcelable;

public final class SipCallSession implements Parcelable {

    /**
     * Describe the state of a call
     */
    public static class InvState {
        public static final int INVALID = -1;
        public static final int NULL = 0;
        public static final int CALLING = 1;
        public static final int INCOMING = 2;
        public static final int EARLY = 3;
        public static final int CONNECTING = 4;
        public static final int CONFIRMED = 5;
        public static final int DISCONNECTED = 6;
    }

    public static class MediaState {
        public static final int NONE = 0;
        public static final int ACTIVE = 1;
        public static final int LOCAL_HOLD = 2;
        public static final int REMOTE_HOLD = 3;
        public static final int ERROR = 4;
    }

    public static class StatusCode {
        public static final int TRYING = 100;
        public static final int RINGING = 180;
        public static final int CALL_BEING_FORWARDED = 181;
        public static final int QUEUED = 182;
        public static final int PROGRESS = 183;
        public static final int OK = 200;
        public static final int ACCEPTED = 202;
        public static final int MULTIPLE_CHOICES = 300;
        public static final int MOVED_PERMANENTLY = 301;
        public static final int MOVED_TEMPORARILY = 302;
        public static final int USE_PROXY = 305;
        public static final int ALTERNATIVE_SERVICE = 380;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int PAYMENT_REQUIRED = 402;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int METHOD_NOT_ALLOWED = 405;
        public static final int NOT_ACCEPTABLE = 406;
        public static final int INTERVAL_TOO_BRIEF = 423;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int DECLINE = 603;
        /*
         * PJSIP_SC_PROXY_AUTHENTICATION_REQUIRED = 407,
         * PJSIP_SC_REQUEST_TIMEOUT = 408, PJSIP_SC_GONE = 410,
         * PJSIP_SC_REQUEST_ENTITY_TOO_LARGE = 413,
         * PJSIP_SC_REQUEST_URI_TOO_LONG = 414, PJSIP_SC_UNSUPPORTED_MEDIA_TYPE
         * = 415, PJSIP_SC_UNSUPPORTED_URI_SCHEME = 416, PJSIP_SC_BAD_EXTENSION
         * = 420, PJSIP_SC_EXTENSION_REQUIRED = 421,
         * PJSIP_SC_SESSION_TIMER_TOO_SMALL = 422,
         * PJSIP_SC_TEMPORARILY_UNAVAILABLE = 480,
         * PJSIP_SC_CALL_TSX_DOES_NOT_EXIST = 481, PJSIP_SC_LOOP_DETECTED = 482,
         * PJSIP_SC_TOO_MANY_HOPS = 483, PJSIP_SC_ADDRESS_INCOMPLETE = 484,
         * PJSIP_AC_AMBIGUOUS = 485, PJSIP_SC_BUSY_HERE = 486,
         * PJSIP_SC_REQUEST_TERMINATED = 487, PJSIP_SC_NOT_ACCEPTABLE_HERE =
         * 488, PJSIP_SC_BAD_EVENT = 489, PJSIP_SC_REQUEST_UPDATED = 490,
         * PJSIP_SC_REQUEST_PENDING = 491, PJSIP_SC_UNDECIPHERABLE = 493,
         * PJSIP_SC_INTERNAL_SERVER_ERROR = 500, PJSIP_SC_NOT_IMPLEMENTED = 501,
         * PJSIP_SC_BAD_GATEWAY = 502, PJSIP_SC_SERVICE_UNAVAILABLE = 503,
         * PJSIP_SC_SERVER_TIMEOUT = 504, PJSIP_SC_VERSION_NOT_SUPPORTED = 505,
         * PJSIP_SC_MESSAGE_TOO_LARGE = 513, PJSIP_SC_PRECONDITION_FAILURE =
         * 580, PJSIP_SC_BUSY_EVERYWHERE = 600, PJSIP_SC_DOES_NOT_EXIST_ANYWHERE
         * = 604, PJSIP_SC_NOT_ACCEPTABLE_ANYWHERE = 606,
         */
    }

    public static final int INVALID_CALL_ID = -1;

    public int primaryKey = -1;
    private int callId = INVALID_CALL_ID;
    private int callState = InvState.INVALID;
    private String remoteContact;
    private boolean isIncoming;
    private int confPort = -1;
    private long accId = SipProfile.INVALID_ID;

    public long callStart = 0;
    private int mediaStatus = MediaState.NONE;
    private boolean mediaSecure = false;
    private boolean mediaHasVideoStream = false;
    private long connectStart = 0;
    private int lastStatusCode = 0;
    private String lastStatusComment = "";
    private String mediaSecureInfo = "";
    
    public SipCallSession(Parcel in) {
        readFromParcel(in);
    }

    public SipCallSession() {
        // Nothing to do in default constructor
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(primaryKey);
        dest.writeInt(callId);
        dest.writeInt(callState);
        dest.writeInt(mediaStatus);
        dest.writeString(remoteContact);
        dest.writeInt(isIncoming() ? 1 : 0);
        dest.writeInt(confPort);
        dest.writeInt((int) accId);
        dest.writeInt(lastStatusCode);
        dest.writeString(mediaSecureInfo);
        dest.writeLong(connectStart);
        dest.writeInt(mediaSecure ? 1 : 0);
        dest.writeString(getLastStatusComment());
        dest.writeInt(mediaHasVideo() ? 1 : 0);
    }

    public void readFromParcel(Parcel in) {
        primaryKey = in.readInt();
        callId = in.readInt();
        callState = in.readInt();
        mediaStatus = in.readInt();
        remoteContact = in.readString();
        setIncoming((in.readInt() == 1));
        confPort = in.readInt();
        accId = in.readInt();
        lastStatusCode = in.readInt();
        mediaSecureInfo = in.readString();
        connectStart = in.readLong();
        mediaSecure = (in.readInt() == 1);
        setLastStatusComment(in.readString());
        setMediaHasVideo((in.readInt() == 1));
    }

    public static final Parcelable.Creator<SipCallSession> CREATOR = new Parcelable.Creator<SipCallSession>() {
        public SipCallSession createFromParcel(Parcel in) {
            return new SipCallSession(in);
        }

        public SipCallSession[] newArray(int size) {
            return new SipCallSession[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SipCallSession)) {
            return false;
        }
        SipCallSession ci = (SipCallSession) o;
        if (ci.getCallId() == callId) {
            return true;
        }
        return false;
    }

    // Getters / Setters
    /**
     * Get the call id of this call info
     * 
     * @return id of this call
     */
    public int getCallId() {
        return callId;
    }

    public void setCallId(int callId2) {
        callId = callId2;
    }

    /**
     * Get the call state of this call info
     * 
     * @return the invitation state
     */
    public int getCallState() {
        return callState;
    }

    public void setCallState(int callState2) {
        callState = callState2;
    }

    public int getMediaStatus() {
        return mediaStatus;
    }

    public void setMediaStatus(int mediaStatus2) {
        mediaStatus = mediaStatus2;
    }

    /**
     * Get the remote contact for this call info
     * 
     * @return string representing the remote contact
     */
    public String getRemoteContact() {
        return remoteContact;
    }

    public void setRemoteContact(String remoteContact2) {
        remoteContact = remoteContact2;
    }

    /**
     * @param isIncoming the isIncoming to set
     */
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }

    /**
     * @return the isIncoming
     */
    public boolean isIncoming() {
        return isIncoming;
    }

    /**
     * Get duration of the call right now
     * 
     * @return duration in seconds
     */
    public long getConnectStart() {
        return connectStart;
    }

    public void setConnectStart(long connectStart2) {
        connectStart = connectStart2;
    }

    /**
     * Check if the specific call info indicate it is an active call in
     * progress.
     */
    public boolean isActive() {
        return (callState == InvState.INCOMING || callState == InvState.EARLY ||
                callState == InvState.CALLING || callState == InvState.CONFIRMED || callState == InvState.CONNECTING);
    }

    public int getConfPort() {
        return confPort;
    }

    public void setConfPort(int confPort2) {
        confPort = confPort2;
    }

    public long getAccId() {
        return accId;
    }

    public void setAccId(long accId2) {
        accId = accId2;
    }

    public boolean isSecure() {
        return mediaSecure;
    }

    public void setMediaSecure(boolean mediaSecure2) {
        mediaSecure = mediaSecure2;
    }

    public void setMediaSecureInfo(String aInfo) {
        mediaSecureInfo = aInfo;
    }

    public String getMediaSecureInfo() {
        return mediaSecureInfo;
    }

    public boolean isLocalHeld() {
        return mediaStatus == SipCallSession.MediaState.LOCAL_HOLD;
    }

    public boolean isRemoteHeld() {
        return (mediaStatus == SipCallSession.MediaState.NONE && isActive() && !isBeforeConfirmed());
    }

    public boolean isBeforeConfirmed() {
        return (callState == InvState.CALLING || callState == InvState.INCOMING
                || callState == InvState.EARLY || callState == InvState.CONNECTING);
    }

    public boolean isAfterEnded() {
        return (callState == InvState.DISCONNECTED || callState == InvState.INVALID || callState == InvState.NULL);
    }

    public void setLastStatusCode(int status_code) {
        lastStatusCode = status_code;
    }

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    /**
     * @return the lastStatusComment
     */
    public String getLastStatusComment() {
        return lastStatusComment;
    }

    /**
     * @param lastStatusComment the lastStatusComment to set
     */
    public void setLastStatusComment(String lastStatusComment) {
        this.lastStatusComment = lastStatusComment;
    }

    /**
     * @return the mediaHasVideoStream
     */
    public boolean mediaHasVideo() {
        return mediaHasVideoStream;
    }

    /**
     * @param mediaHasVideoStream the mediaHasVideoStream to set
     */
    public void setMediaHasVideo(boolean mediaHasVideo) {
        this.mediaHasVideoStream = mediaHasVideo;
    }
}
