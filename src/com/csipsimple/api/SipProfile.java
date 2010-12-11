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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class SipProfile implements Parcelable {
	
	//Constants
	public final static int INVALID_ID = -1;
	
	//Transport choices
	public final static int TRANSPORT_AUTO = 0;
	public final static int TRANSPORT_UDP = 1;
	public final static int TRANSPORT_TCP = 2;
	public final static int TRANSPORT_TLS = 3;
	
	//Stack choices
	public static final int PJSIP_STACK = 0;
	public static final int GOOGLE_STACK = 1;
	
	//Password type choices
	public static final int CRED_DATA_PLAIN_PASSWD = 0;
	public static final int CRED_DATA_DIGEST = 1;
	public static final int CRED_CRED_DATA_EXT_AKA = 2;
	
	public static final String PROXIES_SEPARATOR = "|";
	
	// Fields for table accounts
	public static final String FIELD_ID = "id";
	public static final String FIELD_ACTIVE = "active";
	public static final String FIELD_WIZARD = "wizard";
	public static final String FIELD_DISPLAY_NAME = "display_name";

	public static final String FIELD_PRIORITY = "priority";
	public static final String FIELD_ACC_ID = "acc_id";
	public static final String FIELD_REG_URI = "reg_uri";
	public static final String FIELD_MWI_ENABLED = "mwi_enabled";
	public static final String FIELD_PUBLISH_ENABLED = "publish_enabled";
	public static final String FIELD_REG_TIMEOUT = "reg_timeout";
	public static final String FIELD_KA_INTERVAL = "ka_interval";
	public static final String FIELD_PIDF_TUPLE_ID = "pidf_tuple_id";
	public static final String FIELD_FORCE_CONTACT = "force_contact";
	
	public static final String FIELD_ALLOW_CONTACT_REWRITE = "allow_contact_rewrite";
	public static final String FIELD_CONTACT_REWRITE_METHOD = "contact_rewrite_method";
	
	public static final String FIELD_CONTACT_PARAMS = "contact_params";
	public static final String FIELD_CONTACT_URI_PARAMS = "contact_uri_params";
	public static final String FIELD_TRANSPORT = "transport";
	public static final String FIELD_USE_SRTP = "use_srtp";
	
	// For now, assume unique proxy
	public static final String FIELD_PROXY = "proxy";
	// For now, assume unique credential
	public static final String FIELD_REALM = "realm";
	public static final String FIELD_SCHEME = "scheme";
	public static final String FIELD_USERNAME = "username";
	public static final String FIELD_DATATYPE = "datatype";
	public static final String FIELD_DATA = "data";
	
	public final static String[] common_projection = {
		FIELD_ID,
		// Application relative fields
		FIELD_ACTIVE, FIELD_WIZARD, FIELD_DISPLAY_NAME,

		// Here comes pjsua_acc_config fields
		FIELD_PRIORITY, FIELD_ACC_ID, FIELD_REG_URI, 
		FIELD_MWI_ENABLED, FIELD_PUBLISH_ENABLED, FIELD_REG_TIMEOUT, FIELD_KA_INTERVAL, FIELD_PIDF_TUPLE_ID,
		FIELD_FORCE_CONTACT, FIELD_ALLOW_CONTACT_REWRITE, FIELD_CONTACT_REWRITE_METHOD, 
		FIELD_CONTACT_PARAMS, FIELD_CONTACT_URI_PARAMS,
		FIELD_TRANSPORT, FIELD_USE_SRTP,

		// Proxy infos
		FIELD_PROXY,

		// And now cred_info since for now only one cred info can be managed
		// In future release a credential table should be created
		FIELD_REALM, FIELD_SCHEME, FIELD_USERNAME, FIELD_DATATYPE,
		FIELD_DATA };
	
	
	//Properties
	public int PrimaryKey = -1;
	public int id = INVALID_ID;
	public String display_name = "";
	public String wizard = "EXPERT";
	public Integer transport = 0;
	public boolean active = true;
	public int priority = 100;
	public String acc_id = null;
	public String reg_uri = null;
	public int publish_enabled = -1;
	public int reg_timeout = -1;
	public int ka_interval = -1;
	public String pidf_tuple_id = null;
	public String force_contact = null;
	public boolean allow_contact_rewrite = true;
	public int contact_rewrite_method = 2;
	public String[] proxies = null;
	public String realm = null;
	public String username = null;
	public String scheme = null;
	public int datatype = 0;
	public String data = null;
	public int use_srtp = -1;
	public int sip_stack = PJSIP_STACK;
	
	
	public SipProfile() {
		display_name = "";
		wizard = "EXPERT";
		active = true;
	}
	
	public SipProfile(Parcel in) {
		PrimaryKey = in.readInt();
		id = in.readInt();
		display_name = in.readString();
		wizard = in.readString();
		transport = in.readInt();
		active = (in.readInt()!=0)?true:false;
		priority = in.readInt();
		acc_id = getReadParcelableString(in.readString());
		reg_uri = getReadParcelableString(in.readString());
		publish_enabled = in.readInt();
		reg_timeout = in.readInt();
		ka_interval = in.readInt();
		pidf_tuple_id = getReadParcelableString(in.readString());
		force_contact = getReadParcelableString(in.readString());
		proxies = TextUtils.split(getReadParcelableString(in.readString()),  Pattern.quote(PROXIES_SEPARATOR) );
		realm = getReadParcelableString(in.readString());
		username = getReadParcelableString(in.readString());
		datatype = in.readInt();
		data = getReadParcelableString(in.readString());
		use_srtp = in.readInt();
		allow_contact_rewrite = (in.readInt()!=0);
		contact_rewrite_method = in.readInt();
		sip_stack = in.readInt();
	}

	public static final Parcelable.Creator<SipProfile> CREATOR = new Parcelable.Creator<SipProfile>() {
		public SipProfile createFromParcel(Parcel in) {
			return new SipProfile(in);
		}

		public SipProfile[] newArray(int size) {
			return new SipProfile[size];
		}
	};

	private static final String THIS_FILE = "SipProfile";


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
		dest.writeInt(transport);
		dest.writeInt(active?1:0);
		dest.writeInt(priority);
		dest.writeString(getWriteParcelableString(acc_id));
		dest.writeString(getWriteParcelableString(reg_uri));
		dest.writeInt(publish_enabled);
		dest.writeInt(reg_timeout);
		dest.writeInt(ka_interval);
		dest.writeString(getWriteParcelableString(pidf_tuple_id));
		dest.writeString(getWriteParcelableString(force_contact));
		dest.writeString(getWriteParcelableString(TextUtils.join(PROXIES_SEPARATOR, proxies)));
		dest.writeString(getWriteParcelableString(realm));
		dest.writeString(getWriteParcelableString(username));
		dest.writeInt(datatype);
		dest.writeString(getWriteParcelableString(data));
		dest.writeInt(use_srtp);
		dest.writeInt(allow_contact_rewrite?1:0);
		dest.writeInt(contact_rewrite_method);
		dest.writeInt(sip_stack);
	}
	
	private String getWriteParcelableString(String str) {
		return (str == null)?"null":str;
	}
	
	private String getReadParcelableString(String str) {
		return str.equalsIgnoreCase("null")?null:str;
	}
	
	
	/** 
	 * Fill account object from cursor
	 * @param c cursor on the database 
	 */
	public void createFromDb(Cursor c) {
		ContentValues args = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, args);
		Integer tmp_i;
		String tmp_s;

		// Application specific settings
		tmp_i = args.getAsInteger(FIELD_ID);
		if (tmp_i != null) {
			id = tmp_i;
		}
		tmp_s = args.getAsString(FIELD_DISPLAY_NAME);
		if (tmp_s != null) {
			display_name = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_WIZARD);
		if (tmp_s != null) {
			wizard = tmp_s;
		}
		tmp_i = args.getAsInteger(FIELD_TRANSPORT);
		if (tmp_i != null) {
			transport = tmp_i;
		}
		
		tmp_i = args.getAsInteger(FIELD_ACTIVE);
		if (tmp_i != null) {
			active = (tmp_i != 0);
		} else {
			active = true;
		}
		
		// General account settings
		tmp_i = args.getAsInteger(FIELD_PRIORITY);
		if (tmp_i != null) {
			priority = tmp_i;
		}
		tmp_s = args.getAsString(FIELD_ACC_ID);
		if (tmp_s != null) {
			acc_id = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_REG_URI);
		if (tmp_s != null) {
			reg_uri = tmp_s;
		}
		tmp_i = args.getAsInteger(FIELD_PUBLISH_ENABLED);
		if (tmp_i != null) {
			publish_enabled = tmp_i;
		}
		tmp_i = args.getAsInteger(FIELD_REG_TIMEOUT);
		if (tmp_i != null) {
			reg_timeout = tmp_i;
		}
		tmp_i = args.getAsInteger(FIELD_KA_INTERVAL);
		if (tmp_i != null) {
			ka_interval = tmp_i;
		}
		tmp_s = args.getAsString(FIELD_PIDF_TUPLE_ID);
		if (tmp_s != null) {
			pidf_tuple_id = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_FORCE_CONTACT);
		if (tmp_s != null) {
			force_contact = tmp_s;
		}
		tmp_i = args.getAsInteger(FIELD_ALLOW_CONTACT_REWRITE);
		if (tmp_i != null) {
			allow_contact_rewrite = (tmp_i == 1);
		}
		tmp_i = args.getAsInteger(FIELD_CONTACT_REWRITE_METHOD);
		if (tmp_i != null) {
			contact_rewrite_method = tmp_i;
		}
		
		tmp_i = args.getAsInteger(FIELD_USE_SRTP);
		if (tmp_i != null) {
			use_srtp = tmp_i;
		}
		
		// Proxy
		tmp_s = args.getAsString(FIELD_PROXY);
		if (tmp_s != null) {
			proxies = TextUtils.split(tmp_s,  Pattern.quote(PROXIES_SEPARATOR) );
		}
		
		tmp_s = args.getAsString(FIELD_REALM);
		if (tmp_s != null) {
			realm = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_SCHEME);
		if (tmp_s != null) {
			scheme = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_USERNAME);
		if (tmp_s != null) {
			username = tmp_s;
		}
		tmp_i = args.getAsInteger(FIELD_DATATYPE);
		if (tmp_i != null) {
			datatype = tmp_i;
		}
		tmp_s = args.getAsString(FIELD_DATA);
		if (tmp_s != null) {
			data = tmp_s;
		}
	}
	
	

	/**
	 * Transform pjsua_acc_config into ContentValues that can be insert into database
	 */
	public ContentValues getDbContentValues() {
		ContentValues args = new ContentValues();
		
		if(id != INVALID_ID){
			args.put(FIELD_ID, id);
		}
		//TODO : ensure of non nullity of some params
		
		args.put(FIELD_ACTIVE, active?"1":"0");
		args.put(FIELD_WIZARD, wizard);
		args.put(FIELD_DISPLAY_NAME, display_name);
		args.put(FIELD_TRANSPORT, transport);
		
		args.put(FIELD_PRIORITY, priority);
		args.put(FIELD_ACC_ID, acc_id);
		args.put(FIELD_REG_URI, reg_uri);

		// MWI not yet in JNI

		args.put(FIELD_PUBLISH_ENABLED, publish_enabled);
		args.put(FIELD_REG_TIMEOUT, reg_timeout);
		args.put(FIELD_KA_INTERVAL, ka_interval);
		args.put(FIELD_PIDF_TUPLE_ID, pidf_tuple_id);
		args.put(FIELD_FORCE_CONTACT, force_contact);
		args.put(FIELD_ALLOW_CONTACT_REWRITE, allow_contact_rewrite);
		args.put(FIELD_CONTACT_REWRITE_METHOD, contact_rewrite_method);
		args.put(FIELD_USE_SRTP, use_srtp);

		// CONTACT_PARAM and CONTACT_PARAM_URI not yet in JNI

		if(proxies != null) {
			args.put(FIELD_PROXY, TextUtils.join(PROXIES_SEPARATOR, proxies));
		}else {
			args.put(FIELD_PROXY, "");
		}
		// Assume we have an unique credential
		args.put(FIELD_REALM, realm);
		args.put(FIELD_SCHEME, scheme);
		args.put(FIELD_USERNAME, username);
		args.put(FIELD_DATATYPE, datatype);
		args.put(FIELD_DATA, data);

		return args;
	}
	
	

	public boolean isCallableNumber(String number, DBAdapter db) {
		boolean canCall = true;
		db.open();
		Cursor c = db.getFiltersForAccount(id);
		int numRows = c.getCount();
		Log.d(THIS_FILE, "F > This account has "+numRows+" filters");
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			Log.d(THIS_FILE, "Test filter "+f.matches);
			canCall &= f.canCall(number);
			
			//Stop processing & rewrite
			if(f.stopProcessing(number)) {
				c.close();
				db.close();
				return canCall;
			}
			number = f.rewrite(number);
			//Move to next
			c.moveToNext();
		}
		c.close();
		db.close();
		return canCall;
	}
	

	public boolean isMustCallNumber(String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(id);
		int numRows = c.getCount();
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			Log.d(THIS_FILE, "Test filter "+f.matches);
			if(f.mustCall(number)) {
				c.close();
				db.close();
				return true;
			}
			//Stop processing & rewrite
			if(f.stopProcessing(number)) {
				c.close();
				db.close();
				return false;
			}
			number = f.rewrite(number);
			//Move to next
			c.moveToNext();
		}
		c.close();
		db.close();
		return false;
	}
	
	
	public String rewritePhoneNumber(String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(id);
		int numRows = c.getCount();
		//Log.d(THIS_FILE, "RW > This account has "+numRows+" filters");
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			//Log.d(THIS_FILE, "RW > Test filter "+f.matches);
			number = f.rewrite(number);
			if(f.stopProcessing(number)) {
				c.close();
				db.close();
				return number;
			}
			c.moveToNext();
		}
		c.close();
		db.close();
		return number;
	}
	
	public boolean isAutoAnswerNumber(String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(id);
		int numRows = c.getCount();
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			if( f.autoAnswer(number) ) {
				return true;
			}
			//Stop processing & rewrite
			if(f.stopProcessing(number)) {
				c.close();
				db.close();
				return false;
			}
			number = f.rewrite(number);
			//Move to next
			c.moveToNext();
		}
		c.close();
		db.close();
		return false;
	}
	
	
	public String getDefaultDomain() {
		String regUri = reg_uri;
		if(regUri == null) {
			return null;
		}
		
		Pattern p = Pattern.compile("^(<)?sip(s)?:([^@]*)(>)?$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(regUri);
		Log.v(THIS_FILE, "Try to find into "+regUri);
		if(!m.matches()) {
			Log.e(THIS_FILE, "Default domain can't be guessed from regUri of this account");
			return null;
		}
		return m.group(3);
	}
	

	public int getIconResource() {
		return WizardUtils.getWizardIconRes(wizard);
	}
	
}
