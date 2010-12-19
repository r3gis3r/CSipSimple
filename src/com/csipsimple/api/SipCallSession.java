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


import android.os.Parcel;
import android.os.Parcelable;

public final class SipCallSession implements Parcelable {
	
	/**
	 * Describe the state of a call
	 * 
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
	
	
	public int PrimaryKey = -1;
	private int callId;
	private int callState = InvState.INVALID;
	private String remoteContact;
	private boolean isIncoming;
	private int confPort = -1;
	private int accId = SipProfile.INVALID_ID;
	
	public long callStart = 0;
	private int mediaStatus = MediaState.NONE;
	private boolean mediaSecure = false;
	private long connectStart = 0;
	

	public SipCallSession(Parcel in) {
		readFromParcel(in);
	}

	public SipCallSession() {
		
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(PrimaryKey);
		dest.writeInt(callId);
		dest.writeInt(callState);
		dest.writeInt(mediaStatus);
		dest.writeString(remoteContact);
		dest.writeInt(isIncoming()?1:0);
		dest.writeInt(confPort);
		dest.writeInt(accId);
	}

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		callId = in.readInt();
		callState = in.readInt();
		mediaStatus = in.readInt();
		remoteContact = in.readString();
		setIncoming((in.readInt() == 1));
		confPort = in.readInt();
		accId = in.readInt();
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
	 * @return string representing the remote contact
	 */
	public String getRemoteContact() {
		return remoteContact;
	}
	public void setRemoteContact( String remoteContact2 ) {
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
	 * @return duration in seconds
	 */
	public long getConnectStart() {
		return connectStart;
	}
	
	public void setConnectStart(long connectStart2) {
		connectStart = connectStart2;
	}
	
	/**
	 * Check if the specific call info indicate it is an active
	 * call in progress.
	 */
	public boolean isActive() {
		switch (callState) {
		case InvState.INCOMING:
		case InvState.EARLY:
		case InvState.CALLING:
		case InvState.CONFIRMED:
		case InvState.CONNECTING:
			return true;
			
		case InvState.DISCONNECTED:
		case InvState.NULL:
			break;
		}
		return false;
	}
	
	
	public int getConfPort() {
		return confPort;
	}
	public void setConfPort(int confPort2) {
		confPort = confPort2;
	}
	
	public int getAccId() {
		return accId;
	}

	public void setAccId(int accId2) {
		accId = accId2;
	}
	
	public boolean isSecure() {
		return mediaSecure;
	}


	public void setMediaSecure(boolean mediaSecure2) {
		mediaSecure = mediaSecure2;
	}


}
