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


public class MediaState implements Parcelable {
	
	
	public int PrimaryKey = -1;
	public boolean isMicrophoneMute = false;
	public boolean isSpeakerphoneOn = false;
	public boolean isBluetoothScoOn = false;
	public boolean canMicrophoneMute = true;
	public boolean canSpeakerphoneOn = true;
	public boolean canBluetoothSco = false;
	
	@Override
	public boolean equals(Object o) {
		
		if(o != null && o.getClass() == MediaState.class) {
			MediaState oState = (MediaState) o;
			if(oState.isBluetoothScoOn == isBluetoothScoOn &&
					oState.isMicrophoneMute == isMicrophoneMute &&
					oState.isSpeakerphoneOn == isSpeakerphoneOn &&
					oState.canBluetoothSco == canBluetoothSco &&
					oState.canSpeakerphoneOn == canSpeakerphoneOn &&
					oState.canMicrophoneMute == canMicrophoneMute) {
				return true;
			}else {
				return false;
			}
			
		}
		return super.equals(o);
	}
	
	public MediaState() {
		
	}
	
	public MediaState(Parcel in) {
		PrimaryKey = in.readInt();
		isMicrophoneMute = (in.readInt() == 1);
		isSpeakerphoneOn = (in.readInt() == 1);
		isBluetoothScoOn = (in.readInt() == 1);
		canMicrophoneMute = (in.readInt() == 1);
		canSpeakerphoneOn = (in.readInt() == 1);
		canBluetoothSco = (in.readInt() == 1);
	}


	public static final Parcelable.Creator<MediaState> CREATOR = new Parcelable.Creator<MediaState>() {
		public MediaState createFromParcel(Parcel in) {
			return new MediaState(in);
		}

		public MediaState[] newArray(int size) {
			return new MediaState[size];
		}
	};



	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(PrimaryKey);
		dest.writeInt(isMicrophoneMute?1:0);
		dest.writeInt(isSpeakerphoneOn?1:0);
		dest.writeInt(isBluetoothScoOn?1:0);
		dest.writeInt(canMicrophoneMute?1:0);
		dest.writeInt(canSpeakerphoneOn?1:0);
		dest.writeInt(canBluetoothSco?1:0);
	}
}