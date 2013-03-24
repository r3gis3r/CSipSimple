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

package com.csipsimple.service.impl;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipProfile;

public class SipCallSessionImpl extends SipCallSession {

    /**
     * Set the call id of this serializable holder
     * 
     * @param callId2 the call id to setup
     */
    public void setCallId(int callId2) {
        callId = callId2;
    }

    /**
     * @param callStart the callStart to set
     */
    public void setCallStart(long callStart) {
        this.callStart = callStart;
    }

    /**
     * @param callState the new invitation state
     * @see InvState
     */
    public void setCallState(int callState) {
        this.callState = callState;
        ;
    }

    /**
     * Set the account id for this call of this serializable holder
     * 
     * @param accId2 The {@link SipProfile#id} of the account use for this call
     * @see #getAccId()
     */
    public void setAccId(long accId2) {
        accId = accId2;
    }
    
    /**
     * Set the signaling secure transport level.
     * Value should be one of {@link SipCallSession#TRANSPORT_SECURE_NONE}, {@link SipCallSession#TRANSPORT_SECURE_TO_SERVER}, {@link SipCallSession#TRANSPORT_SECURE_FULL}
     * @param transportSecure2
     */
    public void setSignalisationSecure(int transportSecure2) {
        transportSecure = transportSecure2;
    }

    /**
     * Set the media security level for this call of this serializable holder
     * 
     * @param mediaSecure2 true if the call has a <b>media</b> encrypted
     * @see #isMediaSecure()
     */
    public void setMediaSecure(boolean mediaSecure2) {
        mediaSecure = mediaSecure2;
    }

    /**
     * Set the media security info for this call of this serializable holder
     * 
     * @param aInfo the information about the <b>media</b> security
     * @see #getMediaSecureInfo()
     */
    public void setMediaSecureInfo(String aInfo) {
        mediaSecureInfo = aInfo;
    }

    /**
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
     * Set the last status comment for this call
     * 
     * @param lastStatusComment the lastStatusComment to set
     */
    public void setLastStatusComment(String lastStatusComment) {
        this.lastStatusComment = lastStatusComment;
    }

    /**
     * Set the remote contact of this serializable holder
     * 
     * @param remoteContact2 the new remote contact representation string
     * @see #getRemoteContact()
     */
    public void setRemoteContact(String remoteContact2) {
        remoteContact = remoteContact2;
    }

    /**
     * Set the fact that this call was initiated by the remote party
     * 
     * @param isIncoming the isIncoming to set
     * @see #isIncoming()
     */
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }

    /**
     * Set the time of the beginning of the call as a connected call
     * 
     * @param connectStart2 the new connected start time for this call
     * @see #getConnectStart()
     */
    public void setConnectStart(long connectStart2) {
        connectStart = connectStart2;
    }

    /**
     * Set the conf port of this serializable holder
     * 
     * @param confPort2
     * @see #getConfPort()
     */
    public void setConfPort(int confPort2) {
        confPort = confPort2;
    }

    /**
     * Set the media video stream flag <br/>
     * 
     * @param mediaHasVideo pass true if the media of the underlying call has a
     *            video stream
     */
    public void setMediaHasVideo(boolean mediaHasVideo) {
        this.mediaHasVideoStream = mediaHasVideo;
    }

    /**
     * Set the can record flag <br/>
     * 
     * @param canRecord pass true if the audio can be recorded
     */
    public void setCanRecord(boolean canRecord) {
        this.canRecord = canRecord;
    }
    
    /**
     * Set the is record flag <br/>
     * 
     * @param isRecording pass true if the audio is currently recording
     */
    public void setIsRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }

    /**
     * @param zrtpSASVerified the zrtpSASVerified to set
     */
    public void setZrtpSASVerified(boolean zrtpSASVerified) {
        this.zrtpSASVerified = zrtpSASVerified;
    }

    /**
     * @param hasZrtp the hasZrtp to set
     */
    public void setHasZrtp(boolean hasZrtp) {
        this.hasZrtp = hasZrtp;
    }

    /**
     * Set the sip media state of this serializable holder
     * 
     * @param mediaStatus2 the new media status
     */
    public void setMediaStatus(int mediaStatus2) {
        mediaStatus = mediaStatus2;
    }

    public void applyDisconnect() {
        isIncoming = false;
        mediaStatus = MediaState.NONE;
        mediaSecure = false;
        mediaHasVideoStream = false;
        callStart = 0;
        mediaSecureInfo = "";
        canRecord = false;
        isRecording = false;
        zrtpSASVerified = false;
        hasZrtp = false;
    }
}
