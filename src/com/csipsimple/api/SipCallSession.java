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

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

/**
 * Represents state of a call session<br/>
 * This class helps to serialize/deserialize the state of the media layer <br/>
 * <b>Changing these fields has no effect on the sip call session </b>: it's
 * only a structured holder for datas <br/>
 */
public final class SipCallSession implements Parcelable {

    /**
     * Describe the control state of a call <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/group__PJSIP__INV.htm#ga083ffd9c75c406c41f113479cc1ebc1c"
     * >Pjsip documentation</a>
     */
    public static class InvState {
        /**
         * The call is in an invalid state not syncrhonized with sip stack
         */
        public static final int INVALID = -1;
        /**
         * Before INVITE is sent or received
         */
        public static final int NULL = 0;
        /**
         * After INVITE is sent
         */
        public static final int CALLING = 1;
        /**
         * After INVITE is received.
         */
        public static final int INCOMING = 2;
        /**
         * After response with To tag.
         */
        public static final int EARLY = 3;
        /**
         * After 2xx is sent/received.
         */
        public static final int CONNECTING = 4;
        /**
         * After ACK is sent/received.
         */
        public static final int CONFIRMED = 5;
        /**
         * Session is terminated.
         */
        public static final int DISCONNECTED = 6;

        // Should not be constructed, just an older for int values
        // Not an enum because easier to pass to Parcelable
        private InvState() {
        }
    }

    /**
     * Describe the media state of the call <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/group__PJSUA__LIB__CALL.htm#ga0608027241a5462d9f2736e3a6b8e3f4"
     * >Pjsip documentation</a>
     */
    public static class MediaState {
        /**
         * Call currently has no media
         */
        public static final int NONE = 0;
        /**
         * The media is active
         */
        public static final int ACTIVE = 1;
        /**
         * The media is currently put on hold by local endpoint
         */
        public static final int LOCAL_HOLD = 2;
        /**
         * The media is currently put on hold by remote endpoint
         */
        public static final int REMOTE_HOLD = 3;
        /**
         * The media has reported error (e.g. ICE negotiation)
         */
        public static final int ERROR = 4;

        // Should not be constructed, just an older for int values
        // Not an enum because easier to pass to Parcelable
        private MediaState() {
        }
    }

    /**
     * Status code of the sip call dialog Actually just shortcuts to SIP codes<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/group__PJSIP__MSG__LINE.htm#gaf6d60351ee68ca0c87358db2e59b9376"
     * >Pjsip documentation</a>
     */
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

    /**
     * Id of an invalid or not existant call
     */
    public static final int INVALID_CALL_ID = -1;

    /**
     * Primary key for the parcelable object
     */
    public int primaryKey = -1;
    /**
     * The starting time oof the call
     */
    public long callStart = 0;

    private int callId = INVALID_CALL_ID;
    private int callState = InvState.INVALID;
    private String remoteContact;
    private boolean isIncoming;
    private int confPort = -1;
    private long accId = SipProfile.INVALID_ID;
    private int mediaStatus = MediaState.NONE;
    private boolean mediaSecure = false;
    private boolean mediaHasVideoStream = false;
    private long connectStart = 0;
    private int lastStatusCode = 0;
    private String lastStatusComment = "";
    private String mediaSecureInfo = "";

    /**
     * Construct from parcelable <br/>
     * Only used by {@link #CREATOR}
     * 
     * @param in parcelable to build from
     */
    private SipCallSession(Parcel in) {
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

    /**
     * Constructor for a sip call session state object <br/>
     * It will contains default values for all flags This class as no
     * setter/getter for members flags <br/>
     * It's aim is to allow to serialize/deserialize easily the state of a sip
     * call, <n>not to modify it</b>
     */
    public SipCallSession() {
        // Nothing to do in default constructor
    }

    /**
     * @see Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @see Parcelable#writeToParcel(Parcel, int)
     */
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

    /**
     * Parcelable creator. So that it can be passed as an argument of the aidl
     * interface
     */
    public static final Parcelable.Creator<SipCallSession> CREATOR = new Parcelable.Creator<SipCallSession>() {
        public SipCallSession createFromParcel(Parcel in) {
            return new SipCallSession(in);
        }

        public SipCallSession[] newArray(int size) {
            return new SipCallSession[size];
        }
    };

    /**
     * A sip call session is equal to another if both means the same callId
     */
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

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the call id of this serializable holder
     * 
     * @param callId2 the call id to setup
     */
    public void setCallId(int callId2) {
        callId = callId2;
    }

    /**
     * Get the call state of this call info
     * 
     * @return the invitation state
     * @see InvState
     */
    public int getCallState() {
        return callState;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the call state of this serializable holder
     * 
     * @param callState2 the new invitation state
     * @see InvState
     */
    public void setCallState(int callState2) {
        callState = callState2;
    }

    public int getMediaStatus() {
        return mediaStatus;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the sip media state of this serializable holder
     * 
     * @param mediaStatus2 the new media status
     */
    public void setMediaStatus(int mediaStatus2) {
        mediaStatus = mediaStatus2;
    }

    /**
     * Get the remote Contact for this call info
     * 
     * @return string representing the remote contact
     */
    public String getRemoteContact() {
        return remoteContact;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the remote contact of this serializable holder
     * 
     * @param remoteContact2 the new remote contact representation string
     * @see #getRemoteContact()
     */
    public void setRemoteContact(String remoteContact2) {
        remoteContact = remoteContact2;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the fact that this call was initiated by the remote party
     * 
     * @param isIncoming the isIncoming to set
     * @see #isIncoming()
     */
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }

    /**
     * Get the call way
     * 
     * @return true if the remote party was the caller
     */
    public boolean isIncoming() {
        return isIncoming;
    }

    /**
     * Get the start time of the connection of the call
     * 
     * @return duration in milliseconds
     * @see SystemClock#elapsedRealtime()
     */
    public long getConnectStart() {
        return connectStart;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the time of the beginning of the call as a connected call
     * 
     * @param connectStart2 the new connected start time for this call
     * @see #getConnectStart()
     */
    public void setConnectStart(long connectStart2) {
        connectStart = connectStart2;
    }

    /**
     * Check if the specific call info indicates that it is an active call in
     * progress (incoming or early or calling or confirmed or connecting)
     * 
     * @return true if the call can be considered as in progress/active
     */
    public boolean isActive() {
        return (callState == InvState.INCOMING || callState == InvState.EARLY ||
                callState == InvState.CALLING || callState == InvState.CONFIRMED || callState == InvState.CONNECTING);
    }

    /**
     * Get the sounds conference board port <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/group__PJSUA__LIB__BASE.htm#gaf5d44947e4e62dc31dfde88884534385"
     * >Pjsip documentation</a>
     * 
     * @return the conf port of the audio media of this call
     */
    public int getConfPort() {
        return confPort;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the conf port of this serializable holder
     * 
     * @param confPort2
     * @see #getConfPort()
     */
    public void setConfPort(int confPort2) {
        confPort = confPort2;
    }

    /**
     * Get the identifier of the account corresponding to this call <br/>
     * This identifier is the one you have in {@link SipProfile#id} <br/>
     * It may return {@link SipProfile#INVALID_ID} if no account detected for
     * this call. <i>Example, case of peer to peer call</i>
     * 
     * @return The {@link SipProfile#id} of the account use for this call
     */
    public long getAccId() {
        return accId;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the account id for this call of this serializable holder
     * 
     * @param accId2 The {@link SipProfile#id} of the account use for this call
     * @see #getAccId()
     */
    public void setAccId(long accId2) {
        accId = accId2;
    }

    /**
     * Get the secure level of the call
     * 
     * @return true if the call has a <b>media</b> encrypted
     */
    public boolean isSecure() {
        return mediaSecure;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the media security level for this call of this serializable holder
     * 
     * @param mediaSecure2 true if the call has a <b>media</b> encrypted
     * @see #isSecure()
     */
    public void setMediaSecure(boolean mediaSecure2) {
        mediaSecure = mediaSecure2;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the media security info for this call of this serializable holder
     * 
     * @param aInfo the information about the <b>media</b> security
     * @see #getMediaSecureInfo()
     */
    public void setMediaSecureInfo(String aInfo) {
        mediaSecureInfo = aInfo;
    }

    /**
     * Get the information about the <b>media</b> security of this call
     * 
     * @return the information about the <b>media</b> security
     */
    public String getMediaSecureInfo() {
        return mediaSecureInfo;
    }

    /**
     * Get the information about local held state of this call
     * 
     * @return the information about local held state of media
     */
    public boolean isLocalHeld() {
        return mediaStatus == SipCallSession.MediaState.LOCAL_HOLD;
    }

    /**
     * Get the information about remote held state of this call
     * 
     * @return the information about remote held state of media
     */
    public boolean isRemoteHeld() {
        return (mediaStatus == SipCallSession.MediaState.NONE && isActive() && !isBeforeConfirmed());
    }

    /**
     * Check if the specific call info indicates that it is a call that has not yet been confirmed by both ends.<br/>
     * In other worlds if the call is in state, calling, incoming early or connecting.
     * 
     * @return true if the call can be considered not yet been confirmed
     */
    public boolean isBeforeConfirmed() {
        return (callState == InvState.CALLING || callState == InvState.INCOMING
                || callState == InvState.EARLY || callState == InvState.CONNECTING);
    }


    /**
     * Check if the specific call info indicates that it is a call that has been ended<br/>
     * In other worlds if the call is in state, disconnected, invalid or null
     * 
     * @return true if the call can be considered as already ended
     */
    public boolean isAfterEnded() {
        return (callState == InvState.DISCONNECTED || callState == InvState.INVALID || callState == InvState.NULL);
    }

    /**
     * Get the latest status code of the sip dialog corresponding to this call
     * call
     * 
     * @return the status code
     * @see SipCallSession.StatusCode
     */
    public int getLastStatusCode() {
        return lastStatusCode;
    }
    
    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the latest status code for this call of this serializable holder
     * 
     * @param status_code The code of the latest known sip dialog
     * @see #getLastStatusCode()
     * @see SipCallSession.StatusCode
     */
    public void setLastStatusCode(int status_code) {
        lastStatusCode = status_code;
    }

    /**
     * Get the last status comment of the sip dialog corresponding to this call
     * 
     * @return the last status comment string from server
     */
    public String getLastStatusComment() {
        return lastStatusComment;
    }

    /**
     * This method should be only used by CSipSimple service <br/>
     * Set the last status comment for this call 
     * 
     * @param lastStatusComment the lastStatusComment to set
     */
    public void setLastStatusComment(String lastStatusComment) {
        this.lastStatusComment = lastStatusComment;
    }

    /**
     * Get whether the call has a video media stream connected
     * 
     * @return true if the call has a video media stream
     */
    public boolean mediaHasVideo() {
        return mediaHasVideoStream;
    }

    /**
     * Set the media video stream flag <br/>
     * This method should be only used by CSipSimple service
     * 
     * @param mediaHasVideo pass true if the media of the underlying call has a
     *            video stream
     */
    public void setMediaHasVideo(boolean mediaHasVideo) {
        this.mediaHasVideoStream = mediaHasVideo;
    }
}
