/**
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

import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;

import android.os.Parcel;
import android.os.Parcelable;

public class CallInfo implements Parcelable {

	public int PrimaryKey = -1;
	private int mCallId;
	private pjsip_inv_state mCallState;
	private String mRemoteContact;

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

	public CallInfo(int call_id) {
		pjsua_call_info pj_info = new pjsua_call_info();
		pjsua.call_get_info(call_id, pj_info);
		fillFromPj(pj_info);
	}

	private void fillFromPj(pjsua_call_info pj_callinfo) {
		mCallId = pj_callinfo.getId();
		mCallState = pj_callinfo.getState();
		mRemoteContact = pj_callinfo.getRemote_info().getPtr();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(PrimaryKey);
		dest.writeInt(mCallId);
		dest.writeInt(mCallState.swigValue());
		dest.writeString(mRemoteContact);
	}

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		mCallId = in.readInt();
		mCallState = pjsip_inv_state.swigToEnum(in.readInt());
		mRemoteContact = in.readString();
	}

	// Getters / Setters
	public int getCallId() {
		return mCallId;
	}

	public void setCallId(int callId) {
		mCallId = callId;
	}

	public pjsip_inv_state getCallState() {
		return mCallState;
	}

	public void setCallState(pjsip_inv_state callState) {
		mCallState = callState;
	}

	public String getStringCallState() {
		String state = "";
		if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING)) {
			state = "CALLING";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)) {
			state = "CONFIRMED";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_CONNECTING)) {
			state = "CONNECTING";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)) {
			state = "DISCONNECTED";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY)) {
			state = "EARLY";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING)) {
			state = "INCOMING";
		} else if (mCallState.equals(pjsip_inv_state.PJSIP_INV_STATE_NULL)) {
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
		if (ci.getCallId() == mCallId) {
			return true;
		}
		return false;
	}

	public String getRemoteContact() {
		return mRemoteContact;
	}

}
