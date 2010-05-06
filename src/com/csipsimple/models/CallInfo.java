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
package com.csipsimple.models;

import org.pjsip.pjsua.SWIGTYPE_p_pj_time_val;
import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;

import android.os.Parcel;
import android.os.Parcelable;

public class CallInfo implements Parcelable {

	public int PrimaryKey = -1;
	private int callId;
	private pjsip_inv_state callState;
	private String remoteContact;

	public static final Parcelable.Creator<CallInfo> CREATOR = new Parcelable.Creator<CallInfo>() {
		public CallInfo createFromParcel(Parcel in) {
			return new CallInfo(in);
		}

		public CallInfo[] newArray(int size) {
			return new CallInfo[size];
		}
	};

	public CallInfo(Parcel in) {
		readFromParcel(in);
	}

	public CallInfo(pjsua_call_info pj_callinfo) {
		fillFromPj(pj_callinfo);
	}

	public CallInfo(int aCallId) {
		pjsua_call_info pj_info = new pjsua_call_info();
		pjsua.call_get_info(aCallId, pj_info);
		fillFromPj(pj_info);
	}

	private void fillFromPj(pjsua_call_info pjCallInfo) {
		callId = pjCallInfo.getId();
		callState = pjCallInfo.getState();
		remoteContact = pjCallInfo.getRemote_info().getPtr();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(PrimaryKey);
		dest.writeInt(callId);
		dest.writeInt(callState.swigValue());
		dest.writeString(remoteContact);
	}

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		callId = in.readInt();
		callState = pjsip_inv_state.swigToEnum(in.readInt());
		remoteContact = in.readString();
	}

	// Getters / Setters
	/**
	 * Get the call id of this call info
	 * @return id of this call
	 */
	public int getCallId() {
		return callId;
	}

	/**
	 * Get the call state of this call info
	 * @return the pjsip invitation state
	 */
	public pjsip_inv_state getCallState() {
		return callState;
	}
	

	/**
	 * Get the corresponding string for a given state
	 * Can be used to translate or debug current state
	 * @return the string reprensenting this call info state
	 */
	public String getStringCallState() {
		String state = "";
		if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING)) {
			state = "CALLING";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)) {
			state = "CONFIRMED";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_CONNECTING)) {
			state = "CONNECTING";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)) {
			state = "DISCONNECTED";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY)) {
			state = "EARLY";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING)) {
			state = "INCOMING";
		} else if (callState.equals(pjsip_inv_state.PJSIP_INV_STATE_NULL)) {
			state = "NULL";
		}
		return state;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof CallInfo)) {
			return false;
		}
		CallInfo ci = (CallInfo) o;
		if (ci.getCallId() == callId) {
			return true;
		}
		return false;
	}

	/**
	 * Get the remote contact for this call info
	 * @return string representing the remote contact
	 */
	public String getRemoteContact() {
		return remoteContact;
	}

	//TODO : implement this (could be usefull to get from the native stack instead of managing it in java
//	public long getDuration() {
//		pjsua_call_info pjInfo = new pjsua_call_info();
//		pjsua.call_get_info(callId, pjInfo);
//		SWIGTYPE_p_pj_time_val pjDuration = pjInfo.getConnect_duration();
//		return ;
//	}

}
