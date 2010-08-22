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

import java.io.Serializable;

import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua_acc_info;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class AccountInfo implements Parcelable, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3630993161572726153L;
	public int PrimaryKey = -1;
	private int databaseId;
	private int pjsuaId;
	private String wizard;
	private boolean active;
	private pjsip_status_code statusCode;
	private String statusText;
	private int addedStatus;
	private int expires;
	
	
	
	public AccountInfo(Parcel in) {
		readFromParcel(in);
	}
	
	
	public AccountInfo(Account account) {
		databaseId = account.id;
		wizard = account.wizard;
		active = account.active;
		
		//Set default values
		addedStatus = -1;
		pjsuaId = -1;
		statusCode = pjsip_status_code.PJSIP_SC_NOT_FOUND;
		statusText = "";
		expires = 0;
	}
	
	
	public void fillWithPjInfo(pjsua_acc_info pjInfo) {
		try {
			statusCode = pjInfo.getStatus();
		}catch (IllegalArgumentException e) {
			//TODO : find a better default?
			statusCode = pjsip_status_code.PJSIP_SC_INTERNAL_SERVER_ERROR;
		}
		statusText = pjInfo.getStatus_text().getPtr();
		expires = pjInfo.getExpires();
	}

	@Override
	public int describeContents() {
		return 0;
	}
	

	public void readFromParcel(Parcel in) {
		PrimaryKey = in.readInt();
		databaseId = in.readInt();
		pjsuaId = in.readInt();
		wizard = in.readString();
		active = (in.readInt() == 1);
		statusCode = pjsip_status_code.swigToEnum(in.readInt());
		statusText = in.readString();
		addedStatus = in.readInt();
		expires = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeInt(PrimaryKey);
		out.writeInt(databaseId);
		out.writeInt(pjsuaId);
		out.writeString(wizard);
		out.writeInt( (active?1:0) );
		out.writeInt(statusCode.swigValue());
		out.writeString(statusText);
		out.writeInt(addedStatus);
		out.writeInt(expires);
	}
	

	public static final Parcelable.Creator<AccountInfo> CREATOR = new Parcelable.Creator<AccountInfo>() {
		public AccountInfo createFromParcel(Parcel in) {
			return new AccountInfo(in);
		}

		public AccountInfo[] newArray(int size) {
			return new AccountInfo[size];
		}
	};


	/**
	 * @param databaseId the databaseId to set
	 */
	public void setDatabaseId(int databaseId) {
		this.databaseId = databaseId;
	}

	/**
	 * @return the databaseId
	 */
	public int getDatabaseId() {
		return databaseId;
	}

	/**
	 * @param pjsuaId the pjsuaId to set
	 */
	public void setPjsuaId(int pjsuaId) {
		this.pjsuaId = pjsuaId;
	}

	/**
	 * @return the pjsuaId
	 */
	public int getPjsuaId() {
		return pjsuaId;
	}

	/**
	 * @param wizard the wizard to set
	 */
	public void setWizard(String wizard) {
		this.wizard = wizard;
	}

	/**
	 * @return the wizard
	 */
	public String getWizard() {
		return wizard;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(pjsip_status_code statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the statusCode
	 */
	public pjsip_status_code getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusText the statusText to set
	 */
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	/**
	 * @return the statusText
	 */
	public String getStatusText() {
		return statusText;
	}


	/**
	 * @param addedStatus the addedStatus to set
	 */
	public void setAddedStatus(int addedStatus) {
		this.addedStatus = addedStatus;
	}


	/**
	 * @return the addedStatus
	 */
	public int getAddedStatus() {
		return addedStatus;
	}


	/**
	 * @param expires the expires to set
	 */
	public void setExpires(int expires) {
		this.expires = expires;
	}


	/**
	 * @return the expires
	 */
	public int getExpires() {
		return expires;
	}

	
}
