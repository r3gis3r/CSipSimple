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

import android.os.Parcel;
import android.os.Parcelable;

public class IAccount implements Parcelable {
	public int PrimaryKey = -1;


	public int id = -1;
	public String display_name = "";
	public String wizard = "EXPERT";
	public boolean use_tcp = false;
	public boolean prevent_tcp = false;
	public boolean active = true;
	public int priority = 100;
	public String acc_id = null;
	public String reg_uri = null;
	public int published_enabled = -1;
	public int reg_timeout = -1;
	public int ka_interval = -1;
	public String pidf_tuple_id = null;
	public String force_contact = null;
	public String proxy = null;
	public String realm = null;
	public String username = null;
	public int datatype = 0;
	public String data = null;
	
	

	public static final Parcelable.Creator<IAccount> CREATOR = new Parcelable.Creator<IAccount>() {
		public IAccount createFromParcel(Parcel in) {
			return new IAccount(in);
		}

		public IAccount[] newArray(int size) {
			return new IAccount[size];
		}
	};
	
	public IAccount() {
		
	}
	
	public IAccount(Parcel in) {
		readFromParcel(in);
	}
	
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	private String getWriteParcelableString(String str) {
		return (str == null)?"null":str;
	}
	
	private String getReadParcelableString(String str) {
		return str.equalsIgnoreCase("null")?null:str;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(PrimaryKey);
		dest.writeInt(id);
		dest.writeString(display_name);
		dest.writeString(wizard);
		dest.writeInt(use_tcp?1:0);
		dest.writeInt(prevent_tcp?1:0);
		dest.writeInt(active?1:0);
		dest.writeInt(priority);
		dest.writeString(getWriteParcelableString(acc_id));
		dest.writeString(getWriteParcelableString(reg_uri));
		dest.writeInt(published_enabled);
		dest.writeInt(reg_timeout);
		dest.writeInt(ka_interval);
		dest.writeString(getWriteParcelableString(pidf_tuple_id));
		dest.writeString(getWriteParcelableString(force_contact));
		dest.writeString(getWriteParcelableString(proxy));
		dest.writeString(getWriteParcelableString(realm));
		dest.writeString(getWriteParcelableString(username));
		dest.writeInt(datatype);
		dest.writeString(getWriteParcelableString(data));
	}

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		id = in.readInt();
		display_name = in.readString();
		wizard = in.readString();
		use_tcp = (in.readInt()!=0)?true:false;
		prevent_tcp = (in.readInt()!=0)?true:false;
		active = (in.readInt()!=0)?true:false;
		priority = in.readInt();
		acc_id = getReadParcelableString(in.readString());
		reg_uri = getReadParcelableString(in.readString());
		published_enabled = in.readInt();
		reg_timeout = in.readInt();
		ka_interval = in.readInt();
		pidf_tuple_id = getReadParcelableString(in.readString());
		force_contact = getReadParcelableString(in.readString());
		proxy = getReadParcelableString(in.readString());
		realm = getReadParcelableString(in.readString());
		username = getReadParcelableString(in.readString());
		datatype = in.readInt();
		data = getReadParcelableString(in.readString());
	}
}
