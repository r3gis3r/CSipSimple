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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_acc_config;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;

import com.csipsimple.db.DBAdapter;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;

public class Account {
	// Fields for table accounts
	public static final String FIELD_ID = "id";
	public static final String FIELD_ACTIVE = "active";
	public static final String FIELD_WIZARD = "wizard";
	public static final String FIELD_DISPLAY_NAME = "display_name";

	public static final String FIELD_PRIORITY = "priority";
	public static final String FIELD_ACC_ID = "acc_id";
	public static final String FIELD_REG_URI = "reg_uri";
	public static final String FIELD_USE_TCP = "use_tcp";
	public static final String FIELD_PREVENT_TCP = "prevent_tcp";
	public static final String FIELD_MWI_ENABLED = "mwi_enabled";
	public static final String FIELD_PUBLISH_ENABLED = "publish_enabled";
	public static final String FIELD_REG_TIMEOUT = "reg_timeout";
	public static final String FIELD_KA_INTERVAL = "ka_interval";
	public static final String FIELD_PIDF_TUPLE_ID = "pidf_tuple_id";
	public static final String FIELD_FORCE_CONTACT = "force_contact";
	public static final String FIELD_CONTACT_PARAMS = "contact_params";
	public static final String FIELD_CONTACT_URI_PARAMS = "contact_uri_params";
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
		FIELD_USE_TCP, FIELD_PREVENT_TCP,
		FIELD_MWI_ENABLED, FIELD_PUBLISH_ENABLED, FIELD_REG_TIMEOUT, FIELD_KA_INTERVAL, FIELD_PIDF_TUPLE_ID,
		FIELD_FORCE_CONTACT, FIELD_CONTACT_PARAMS, FIELD_CONTACT_URI_PARAMS,
		FIELD_USE_SRTP,

		// Proxy infos
		FIELD_PROXY,

		// And now cred_info since for now only one cred info can be managed
		// In future release a credential table should be created
		FIELD_REALM, FIELD_SCHEME, FIELD_USERNAME, FIELD_DATATYPE,
		FIELD_DATA };
	private static final String THIS_FILE = "AccountModel";
	
	
	//For now everything is public, easiest to manage
	public String display_name;
	public String wizard;
	public boolean active;
	public pjsua_acc_config cfg;
	public Integer id;
	public boolean use_tcp;
	public boolean prevent_tcp;
	
	public Account() {
		display_name = "";
		wizard = "EXPERT";
		use_tcp = false;
		prevent_tcp = false;
		active = true;
		
		
		cfg = new pjsua_acc_config();
		if (!SipService.creating) {
			pjsua.acc_config_default(cfg);
		}
		// Change the default ka interval to 40s
		cfg.setKa_interval(40);
	}
	
	public Account(IAccount parcelable) {
		if(parcelable.id != -1) {
			id = parcelable.id;
		}
		display_name = parcelable.display_name;
		wizard = parcelable.wizard;
		use_tcp = parcelable.use_tcp;
		prevent_tcp = parcelable.prevent_tcp;
		active = parcelable.active;
		

		cfg = new pjsua_acc_config();
		pjsua.acc_config_default(cfg);
		
		cfg.setPriority(parcelable.priority);
		if(parcelable.acc_id != null) {
			cfg.setId(pjsua.pj_str_copy(parcelable.acc_id));
		}
		if(parcelable.reg_uri != null) {
			cfg.setReg_uri(pjsua.pj_str_copy(parcelable.reg_uri));
		}
		if(parcelable.published_enabled != -1) {
			cfg.setPublish_enabled(parcelable.published_enabled);
		}
		if(parcelable.reg_timeout != -1) {
			cfg.setReg_timeout(parcelable.reg_timeout);
		}
		if(parcelable.ka_interval != -1) {
			cfg.setKa_interval(parcelable.ka_interval);
		}
		if(parcelable.pidf_tuple_id != null) {
			cfg.setPidf_tuple_id(pjsua.pj_str_copy(parcelable.pidf_tuple_id));
		}
		if(parcelable.force_contact != null) {
			cfg.setForce_contact(pjsua.pj_str_copy(parcelable.force_contact));
		}
		if(parcelable.use_srtp != -1) {
			cfg.setUse_srtp(pjmedia_srtp_use.swigToEnum(parcelable.use_srtp));
			cfg.setSrtp_secure_signaling(0);
		}
		
		if(parcelable.proxy != null) {
			cfg.setProxy_cnt(1);
			pj_str_t[] proxies = cfg.getProxy();
			proxies[0] = pjsua.pj_str_copy(parcelable.proxy);
			cfg.setProxy(proxies);
		}

		cfg.setCred_count(1);
		pjsip_cred_info cred_info = cfg.getCred_info();
		
		if(parcelable.realm != null) {
			cred_info.setRealm(pjsua.pj_str_copy(parcelable.realm));
		}
		if(parcelable.username != null) {
			cred_info.setUsername(pjsua.pj_str_copy(parcelable.username));
		}
		if(parcelable.datatype != -1) {
			cred_info.setData_type(parcelable.datatype);
		}
		if(parcelable.data != null) {
			cred_info.setData(pjsua.pj_str_copy(parcelable.data));
		}
	}
	
	
	public IAccount getIAccount() {
		IAccount parcelable = new IAccount();
		if(id != null) {
			parcelable.id = id;
		}
		parcelable.display_name = display_name;
		parcelable.wizard = wizard ;
		parcelable.use_tcp = use_tcp;
		parcelable.prevent_tcp = prevent_tcp;
		parcelable.active = active ;
		
		parcelable.priority = cfg.getPriority();
		parcelable.acc_id = cfg.getId().getPtr();
		parcelable.reg_uri = cfg.getReg_uri().getPtr();
		parcelable.published_enabled = cfg.getPublish_enabled();
		parcelable.reg_timeout = (int) cfg.getReg_timeout();
		parcelable.ka_interval = (int) cfg.getKa_interval();
		parcelable.pidf_tuple_id = cfg.getPidf_tuple_id().getPtr();
		parcelable.force_contact = cfg.getForce_contact().getPtr();
		parcelable.proxy = (cfg.getProxy_cnt()>0)? cfg.getProxy()[0].getPtr():null;
		parcelable.use_srtp = cfg.getUse_srtp().swigValue();
		
		pjsip_cred_info cred_info = cfg.getCred_info();
		if(cfg.getCred_count()>0) {
			parcelable.realm = cred_info.getRealm().getPtr();
			parcelable.username = cred_info.getUsername().getPtr();
			parcelable.datatype = cred_info.getData_type();
			parcelable.data = cred_info.getData().getPtr();
		}
		return parcelable;
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
		tmp_i = args.getAsInteger(FIELD_USE_TCP);
		if (tmp_i != null) {
			use_tcp =(tmp_i != 0);
		} else {
			use_tcp = false;
		}
		tmp_i = args.getAsInteger(FIELD_PREVENT_TCP);
		if (tmp_i != null) {
			prevent_tcp =(tmp_i != 0);
		} else {
			prevent_tcp = false;
		}
		tmp_i = args.getAsInteger(FIELD_ACTIVE);
		if (tmp_i != null) {
			active = (tmp_i != 0);
		} else {
			active = true;
		}

		// Credentials
		cfg.setCred_count(1);

		// General account settings
		tmp_i = args.getAsInteger(FIELD_PRIORITY);
		if (tmp_i != null) {
			cfg.setPriority((int) tmp_i);
		}
		tmp_s = args.getAsString(FIELD_ACC_ID);
		if (tmp_s != null) {
			cfg.setId(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_REG_URI);
		if (tmp_s != null) {
			cfg.setReg_uri(pjsua.pj_str_copy(tmp_s));
		}
		tmp_i = args.getAsInteger(FIELD_PUBLISH_ENABLED);
		if (tmp_i != null) {
			cfg.setPublish_enabled(tmp_i);
		}
		tmp_i = args.getAsInteger(FIELD_REG_TIMEOUT);
		if (tmp_i != null) {
			cfg.setReg_timeout(tmp_i);
		}
		tmp_i = args.getAsInteger(FIELD_KA_INTERVAL);
		if (tmp_i != null) {
			cfg.setKa_interval(tmp_i);
		}
		tmp_s = args.getAsString(FIELD_PIDF_TUPLE_ID);
		if (tmp_s != null) {
			cfg.setPidf_tuple_id(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_FORCE_CONTACT);
		if (tmp_s != null) {
			cfg.setForce_contact(pjsua.pj_str_copy(tmp_s));
		}
		tmp_i = args.getAsInteger(FIELD_USE_SRTP);
		if (tmp_i != null) {
			cfg.setUse_srtp(pjmedia_srtp_use.swigToEnum(tmp_i));
			cfg.setSrtp_secure_signaling(0);
		}
		
		// Proxy
		tmp_s = args.getAsString(FIELD_PROXY);
		if (tmp_s != null) {
			cfg.setProxy_cnt(1);
			pj_str_t[] proxies = cfg.getProxy();
			proxies[0] = pjsua.pj_str_copy(tmp_s);
			cfg.setProxy(proxies);
		}

		pjsip_cred_info cred_info = cfg.getCred_info();

		tmp_s = args.getAsString(FIELD_REALM);
		if (tmp_s != null) {
			cred_info.setRealm(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_SCHEME);
		if (tmp_s != null) {
			cred_info.setScheme(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_USERNAME);
		if (tmp_s != null) {
			cred_info.setUsername(pjsua.pj_str_copy(tmp_s));
		}
		tmp_i = args.getAsInteger(FIELD_DATATYPE);
		if (tmp_i != null) {
			cred_info.setData_type(tmp_i);
		}
		tmp_s = args.getAsString(FIELD_DATA);
		if (tmp_s != null) {
			cred_info.setData(pjsua.pj_str_copy(tmp_s));
		}
	}
	
	/**
	 * Transform pjsua_acc_config into ContentValues that can be insert into database
	 */
	public ContentValues getDbContentValues() {
		ContentValues args = new ContentValues();
		
		if(id != null){
			args.put(FIELD_ID, id);
		}
		args.put(FIELD_ACTIVE, active?"1":"0");
		args.put(FIELD_WIZARD, wizard);
		args.put(FIELD_DISPLAY_NAME, display_name);
		args.put(FIELD_USE_TCP, use_tcp);
		args.put(FIELD_PREVENT_TCP, prevent_tcp);
		
		args.put(FIELD_PRIORITY, cfg.getPriority());
		args.put(FIELD_ACC_ID, cfg.getId().getPtr());
		args.put(FIELD_REG_URI, cfg.getReg_uri().getPtr());

		// MWI not yet in JNI

		args.put(FIELD_PUBLISH_ENABLED, cfg.getPublish_enabled());
		args.put(FIELD_REG_TIMEOUT, cfg.getReg_timeout());
		args.put(FIELD_KA_INTERVAL, cfg.getKa_interval());
		args.put(FIELD_PIDF_TUPLE_ID, cfg.getPidf_tuple_id().getPtr());
		args.put(FIELD_FORCE_CONTACT, cfg.getForce_contact().getPtr());
		args.put(FIELD_USE_SRTP, cfg.getUse_srtp().swigValue());

		// CONTACT_PARAM and CONTACT_PARAM_URI not yet in JNI

		// Assume we have an unique proxy
		if (cfg.getProxy_cnt() > 0) {
			args.put(FIELD_PROXY, cfg.getProxy()[0].getPtr());
		}else {
			args.put(FIELD_PROXY, (String) null);
		}
		
		// Assume we have an unique credential
		pjsip_cred_info cred_info = cfg.getCred_info();
		args.put(FIELD_REALM, cred_info.getRealm().getPtr());
		args.put(FIELD_SCHEME, cred_info.getScheme().getPtr());
		args.put(FIELD_USERNAME, cred_info.getUsername().getPtr());
		args.put(FIELD_DATATYPE, cred_info.getData_type());
		args.put(FIELD_DATA, cred_info.getData().getPtr());

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

	public void applyExtraParams() {
		
		//TODO : should NOT be done here !!! 
		
		String reg_uri = "";
		if (use_tcp) {
			reg_uri = cfg.getReg_uri().getPtr();
			pj_str_t[] proxies = cfg.getProxy();
			
			String proposed_server = reg_uri + ";transport=tcp";
			cfg.setReg_uri(pjsua.pj_str_copy(proposed_server));
			
			if (cfg.getProxy_cnt() == 0 || proxies[0].getPtr() == null || proxies[0].getPtr() == "") {
				proxies[0] = pjsua.pj_str_copy(proposed_server);
				cfg.setProxy(proxies);
			} else {
				proxies[0] = pjsua.pj_str_copy(proxies[0].getPtr() + ";transport=tcp");
				cfg.setProxy(proxies);
			}
			Log.w(THIS_FILE, "We are using TCP !!!");
		}
		
	}
	
	public String getDefaultDomain() {
		String regUri = cfg.getReg_uri().getPtr();
		if(regUri == null) {
			return null;
		}
		
		Pattern p = Pattern.compile("^sip(s)?:([^@]*)$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(regUri);
		Log.v(THIS_FILE, "Try to find into "+regUri);
		if(!m.matches()) {
			Log.e(THIS_FILE, "Default domain can't be guessed from regUri of this account");
			return null;
		}
		return m.group(2);
	}

	public int getIconResource() {
		return WizardUtils.getWizardIconRes(wizard);
	}
	
	
	
	@Override
	public boolean equals(Object o) {
		if(o != null && o.getClass() == Account.class) {
			Account oAccount = (Account) o;
			return oAccount.id == id;
		}
		return super.equals(o);
	}
}
