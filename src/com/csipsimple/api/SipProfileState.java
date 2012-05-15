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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */
package com.csipsimple.api;

import java.io.Serializable;
import java.util.Comparator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Holder for a sip profile state.<br/>
 * It allows to unpack content values from registration/activation status of a {@link SipProfile}.
 */
public class SipProfileState implements Parcelable, Serializable{

    /**
     * Primary key for serialization of the object.
     */
	private static final long serialVersionUID = -3630993161572726153L;
	public int primaryKey = -1;
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

	/**
	 * Account id.<br/>
	 * Identifier of the SIP account associated
	 * 
	 * @see SipProfile#FIELD_ID
	 * @see Integer
	 */
	public final static String ACCOUNT_ID = "account_id";
	/**
	 * Identifier for underlying sip stack. <br/>
	 * Identifier to use as this account id for the sip stack when started and account added to sip stack.
	 * Uses this identifier to call methods on sip stack.
	 * 
	 * @see Integer
	 */
	public final static String PJSUA_ID = "pjsua_id";
	/**
	 * Wizard key. <br/>
	 * Wizard identifier associated to the account. This is a shortcut to not have to query {@link SipProfile} database
	 * 
	 * @see String
	 */
	public final static String WIZARD = "wizard";
	/**
	 * Activation state.<br/>
	 * Active state of the account. This is a shortcut to not have to query {@link SipProfile} database
	 * 
	 * @see Boolean
	 */
	public final static String ACTIVE = "active";
	/**
	 * Status code of the latest registration.<br/>
	 * SIP code of latest registration.
	 * 
	 * @see Integer
	 */
	public final static String STATUS_CODE = "status_code";
	/**
	 * Status comment of latest registration.<br/>
	 * Sip comment of latest registration.
	 * 
	 * @see String
	 */
	public final static String STATUS_TEXT = "status_text";
	/**
	 * Status of sip stack adding of the account.<br/>
	 * When the application adds the account to the stack it may fails if the sip profile is invalid.
	 * 
	 * @see Integer
	 */
	public final static String ADDED_STATUS = "added_status";
	/**
	 * Latest know expires time. <br/>
	 * Expires value of latest registration. It's actually usefull to detect that it was unregister testing 0 value. 
	 * Else it's not necessarily relevant information.
	 * 
	 * @see Integer
	 */
	public final static String EXPIRES = "expires";
	/**
	 * Display name of the account.<br.>
	 * This is a shortcut to not have to query {@link SipProfile} database
	 */
	public final static String DISPLAY_NAME = "display_name";
	/**
     * Priority of the account.<br.>
     * This is a shortcut to not have to query {@link SipProfile} database
     */
	public final static String PRIORITY = "priority";
	/**
     * Registration uri of the account.<br.>
     * This is a shortcut to not have to query {@link SipProfile} database
     */
	public final static String REG_URI = "reg_uri";
	

	public static final String [] FULL_PROJECTION = new String[] {
		ACCOUNT_ID, PJSUA_ID, WIZARD, ACTIVE, STATUS_CODE, STATUS_TEXT, EXPIRES, DISPLAY_NAME, PRIORITY, REG_URI
	};
	
	
	public SipProfileState(Parcel in) {
		readFromParcel(in);
	}
	
	/**
     * Should not be used for external use of the API.
	 * Default constructor.
	 */
	public SipProfileState() {
		//Set default values
		addedStatus = -1;
		pjsuaId = -1;
		statusCode = -1;
		statusText = "";
		expires = 0;
	}
	/**
     * Should not be used for external use of the API.
	 * Constructor on the top of a sip account.
	 * 
	 * @param account The sip profile to associate this wrapper info to.
	 */
	public SipProfileState(SipProfile account) {
		this();
		
		databaseId = (int) account.id;
		wizard = account.wizard;
		active = account.active;
		displayName = account.display_name;
		priority = account.priority;
		regUri = account.reg_uri;
		
	}

    /**
     * Construct a sip state wrapper from a cursor retrieved with a
     * {@link ContentProvider} query on {@link SipProfile#ACCOUNT_STATUS_URI}.
     * 
     * @param c the cursor to unpack
     */
	public SipProfileState(Cursor c) {
		super();
		createFromDb(c);
	}

    /**
     * @see Parcelable#describeContents()
     */
	@Override
	public int describeContents() {
		return 0;
	}
	

	private final void readFromParcel(Parcel in) {
		primaryKey = in.readInt();
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

    /**
     * @see Parcelable#writeToParcel(Parcel, int)
     */
	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeInt(primaryKey);
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
	

    /**
     * Parcelable creator. So that it can be passed as an argument of the aidl
     * interface
     */
	public static final Parcelable.Creator<SipProfileState> CREATOR = new Parcelable.Creator<SipProfileState>() {
		public SipProfileState createFromParcel(Parcel in) {
			return new SipProfileState(in);
		}

		public SipProfileState[] newArray(int size) {
			return new SipProfileState[size];
		}
	};
	
	

	/** 
	 * Fill account state object from cursor.
	 * @param c cursor on the database queried from {@link SipProfile#ACCOUNT_STATUS_URI}
	 */
	public final void createFromDb(Cursor c) {
		ContentValues args = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, args);
		createFromContentValue(args);
	}

    /** 
     * Fill account state object from content values.
     * @param args content values to wrap.
     */
	public final void createFromContentValue(ContentValues args) {
		Integer tmp_i;
		String tmp_s;
		Boolean tmp_b;
		
		tmp_i = args.getAsInteger(ACCOUNT_ID);
		if(tmp_i != null) {
			databaseId = tmp_i;
		}
		tmp_i = args.getAsInteger(PJSUA_ID);
		if(tmp_i != null) {
			pjsuaId = tmp_i;
		}
		tmp_s = args.getAsString(WIZARD);
		if(tmp_s != null) {
			wizard = tmp_s;
		}
		tmp_b = args.getAsBoolean(ACTIVE);
		if(tmp_b != null) {
			active = tmp_b;
		}
		tmp_i = args.getAsInteger(STATUS_CODE);
		if(tmp_i != null) {
			statusCode = tmp_i;
		}
		tmp_s = args.getAsString(STATUS_TEXT);
		if(tmp_s != null) {
			statusText = tmp_s;
		}
		tmp_i = args.getAsInteger(ADDED_STATUS);
		if(tmp_i != null) {
			addedStatus = tmp_i;
		}
		tmp_i = args.getAsInteger(EXPIRES);
		if(tmp_i != null) {
			expires = tmp_i;
		}
		tmp_s = args.getAsString(DISPLAY_NAME);
		if(tmp_s != null) {
			displayName = tmp_s;
		}
		tmp_s = args.getAsString(REG_URI);
		if(tmp_s != null) {
			regUri = tmp_s;
		}
		tmp_i = args.getAsInteger(PRIORITY);
		if(tmp_i != null) {
			priority = tmp_i;
		}
		
	}

    /**
     * Should not be used for external use of the API.
     * Produce content value from the wrapper.
     * 
     * @return Complete content values from the current wrapper around sip
     *         profile state.
     */
	public ContentValues getAsContentValue() {
		ContentValues cv = new ContentValues();
		cv.put(ACCOUNT_ID, databaseId);
		cv.put(ACTIVE, active);
		cv.put(ADDED_STATUS, addedStatus);
		cv.put(DISPLAY_NAME, displayName);
		cv.put(EXPIRES, expires);
		cv.put(PJSUA_ID, pjsuaId);
		cv.put(PRIORITY, priority);
		cv.put(REG_URI, regUri);
		cv.put(STATUS_CODE, statusCode);
		cv.put(STATUS_TEXT, statusText);
		cv.put(WIZARD, wizard);
		return cv;
	}

	/**
	 * @param databaseId the databaseId to set
	 */
	public void setDatabaseId(int databaseId) {
		this.databaseId = databaseId;
	}

	/**
     * Should not be used for external use of the API.
	 * @return the databaseId
	 */
	public int getDatabaseId() {
		return databaseId;
	}

	/**
     * Should not be used for external use of the API.
	 * @param pjsuaId the pjsuaId to set
	 */
	public void setPjsuaId(int pjsuaId) {
		this.pjsuaId = pjsuaId;
	}

	/**
	 * @return the pjsuaId {@link #PJSUA_ID}
	 */
	public int getPjsuaId() {
		return pjsuaId;
	}

	/**
     * Should not be used for external use of the API.
	 * @param wizard the wizard to set
	 */
	public void setWizard(String wizard) {
		this.wizard = wizard;
	}

	/**
	 * @return the wizard {@link #WIZARD}
	 */
	public String getWizard() {
		return wizard;
	}

	/**
     * Should not be used for external use of the API.
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the active {@link #ACTIVE}
	 */
	public boolean isActive() {
		return active;
	}

	/**
     * Should not be used for external use of the API.
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the statusCode {@link #STATUS_TEXT}
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
     * Should not be used for external use of the API.
	 * @param statusText the statusText to set
	 */
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	/**
	 * @return the statusText {@link #STATUS_TEXT}
	 */
	public String getStatusText() {
		return statusText;
	}


	/**
     * Should not be used for external use of the API.
	 * @param addedStatus the addedStatus to set
	 */
	public void setAddedStatus(int addedStatus) {
		this.addedStatus = addedStatus;
	}


	/**
	 * @return the addedStatus {@link #ADDED_STATUS}
	 */
	public int getAddedStatus() {
		return addedStatus;
	}


	/**
     * Should not be used for external use of the API.
	 * @param expires the expires to set
	 */
	public void setExpires(int expires) {
		this.expires = expires;
	}


	/**
	 * @return the expires {@link #EXPIRES}
	 */
	public int getExpires() {
		return expires;
	}
    
    /**
     * @return the display name {@link #DISPLAY_NAME}
     */
	public CharSequence getDisplayName() {
		return displayName;
	}

	/**
	 * @return the priority {@link #PRIORITY}
	 */
	public int getPriority() {
		return priority;
	}
	/**
     * Should not be used for external use of the API.
	 * @param priority
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
	

	/**
     * Should not be used for external use of the API.
	 * @param regUri the regUri to set
	 */
	public void setRegUri(String regUri) {
		this.regUri = regUri;
	}


	/**
	 * @return the regUri {@link #REG_URI}
	 */
	public String getRegUri() {
		return regUri;
	}

	/**
	 * Is the account added to sip stack yet?
	 * @return true if the account has been added to sip stack and has a sip stack id.
	 */
	public boolean isAddedToStack() {
		return pjsuaId != -1;
	}
	
	/**
	 * Is the account valid for sip calls?
	 * @return true if it should be possible to make a call using the associated account.
	 */
	public boolean isValidForCall() {
		if(active) {
			if(TextUtils.isEmpty(getRegUri())) {
				return true;
			}
			return (getPjsuaId() >= 0 && getStatusCode() == SipCallSession.StatusCode.OK && getExpires() > 0);
		}
		return false;
	}
	
	/**
	 * Compare accounts profile states.
	 * @return a comparator instance to compare profile states by priorities.
	 */
	public final static Comparator<SipProfileState> getComparator(){
		return ACC_INFO_COMPARATOR;
	}


	private static final Comparator<SipProfileState> ACC_INFO_COMPARATOR = new Comparator<SipProfileState>() {
		@Override
		public int compare(SipProfileState infos1,SipProfileState infos2) {
			if (infos1 != null && infos2 != null) {
				
				int c1 = infos1.getPriority();
				int c2 = infos2.getPriority();
				
				if (c1 > c2) {
					return 1;
				}
				if (c1 < c2) {
					return -1;
				}
			}

			return 0;
		}
	};
}
