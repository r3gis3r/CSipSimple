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


	private int id;
	private String display_name;
	private String wizard;
	private boolean use_tcp;
	private boolean prevent_tcp;
	private boolean active;
	private int priority;
	private String acc_id;
	private String reg_uri;
	private int published_enabled;
	private int reg_timeout;
	private int ka_interval;
	private String pidf_tuple_id;
	private String force_contact;
	private String proxy;
	private String realm;
	private String username;
	private int datatype;
	private String data;
	
	

	public static final Parcelable.Creator<IAccount> CREATOR = new Parcelable.Creator<IAccount>() {
		public IAccount createFromParcel(Parcel in) {
			return new IAccount(in);
		}

		public IAccount[] newArray(int size) {
			return new IAccount[size];
		}
	};
	
	public IAccount(Parcel in) {
		readFromParcel(in);
	}
	
	
	@Override
	public int describeContents() {
		return 0;
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
		dest.writeString(acc_id);
		dest.writeString(reg_uri);
		dest.writeInt(published_enabled);
		dest.writeInt(reg_timeout);
		dest.writeInt(ka_interval);
		dest.writeString(pidf_tuple_id);
		dest.writeString(force_contact);
		dest.writeString(proxy);
		dest.writeString(realm);
		dest.writeString(username);
		dest.writeInt(datatype);
		dest.writeString(data);
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
		acc_id = in.readString();
		reg_uri = in.readString();
		published_enabled = in.readInt();
		reg_timeout = in.readInt();
		ka_interval = in.readInt();
		pidf_tuple_id = in.readString();
		force_contact = in.readString();
		proxy = in.readString();
		realm = in.readString();
		username = in.readString();
		datatype = in.readInt();
		data = in.readString();
	}
}
