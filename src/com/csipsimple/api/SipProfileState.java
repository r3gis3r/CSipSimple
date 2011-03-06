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
 *  
 *  This file and this file only is released under dual Apache license
 */
package com.csipsimple.api;

import java.io.Serializable;
import java.util.Comparator;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class SipProfileState implements Parcelable, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3630993161572726153L;
	public int PrimaryKey = -1;
	private int databaseId;
	private int pjsuaId;
	private String wizard;
	private boolean active;
	private int statusCode;
	private String statusText;
	private int addedStatus;
	private int expires;
	private String displayName;
	private int priority;
	private String regUri = "";


	public SipProfileState(Parcel in) {
		readFromParcel(in);
	}
	
	
	public SipProfileState(SipProfile account) {
		databaseId = account.id;
		wizard = account.wizard;
		active = account.active;
		displayName = account.display_name;
		priority = account.priority;
		regUri = account.reg_uri;
		
		//Set default values
		addedStatus = -1;
		pjsuaId = -1;
		statusCode = SipCallSession.StatusCode.NOT_FOUND;
		statusText = "";
		expires = 0;
		
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
		statusCode = in.readInt();
		statusText = in.readString();
		addedStatus = in.readInt();
		expires = in.readInt();
		displayName = in.readString();
		priority = in.readInt();
		regUri = in.readString();
	}

	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeInt(PrimaryKey);
		out.writeInt(databaseId);
		out.writeInt(pjsuaId);
		out.writeString(wizard);
		out.writeInt( (active?1:0) );
		out.writeInt(statusCode);
		out.writeString(statusText);
		out.writeInt(addedStatus);
		out.writeInt(expires);
		out.writeString(displayName);
		out.writeInt(priority);
		out.writeString(regUri);
	}
	

	public static final Parcelable.Creator<SipProfileState> CREATOR = new Parcelable.Creator<SipProfileState>() {
		public SipProfileState createFromParcel(Parcel in) {
			return new SipProfileState(in);
		}

		public SipProfileState[] newArray(int size) {
			return new SipProfileState[size];
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
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
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


	public CharSequence getDisplayName() {
		return displayName;
	}

	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	

	/**
	 * @param regUri the regUri to set
	 */
	public void setRegUri(String regUri) {
		this.regUri = regUri;
	}


	/**
	 * @return the regUri
	 */
	public String getRegUri() {
		return regUri;
	}

	
	
	
	public boolean isValidForCall() {
		if(active) {
			if(TextUtils.isEmpty(getRegUri())) {
				return true;
			}
			return (getPjsuaId() >= 0 && getStatusCode() == SipCallSession.StatusCode.OK && getExpires() > 0);
		}
		return false;
	}
	
	public final static Comparator<SipProfileState> getComparator(){
		return accountInfoComparator;
	}


	private static final Comparator<SipProfileState> accountInfoComparator = new Comparator<SipProfileState>() {
		@Override
		public int compare(SipProfileState infos1,SipProfileState infos2) {
			if (infos1 != null && infos2 != null) {
				
				int c1 = infos1.getPriority();
				int c2 = infos2.getPriority();
				
				if (c1 > c2) {
					return -1;
				}
				if (c1 < c2) {
					return 1;
				}
			}

			return 0;
		}
	};
}
