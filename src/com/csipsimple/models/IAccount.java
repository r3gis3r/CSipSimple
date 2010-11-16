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
	public Integer transport = 0;
	public boolean active = true;
	public int priority = 100;
	public String acc_id = null;
	public String reg_uri = null;
	public int published_enabled = -1;
	public int reg_timeout = -1;
	public int ka_interval = -1;
	public String pidf_tuple_id = null;
	public String force_contact = null;
	public boolean allow_contact_rewrite = true;
	public int contact_rewrite_method = 2;
	public String proxy = null;
	public String realm = null;
	public String username = null;
	public int datatype = 0;
	public String data = null;
	public int use_srtp = -1;
	
	

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
		dest.writeInt(transport);
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
		dest.writeInt(use_srtp);
		dest.writeInt(allow_contact_rewrite?1:0);
		dest.writeInt(contact_rewrite_method);
	}

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		id = in.readInt();
		display_name = in.readString();
		wizard = in.readString();
		transport = in.readInt();
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
		use_srtp = in.readInt();
		allow_contact_rewrite = (in.readInt()!=0);
		contact_rewrite_method = in.readInt();
	}
}
