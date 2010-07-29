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

import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_acc_config;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;

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
	public static final String FIELD_MWI_ENABLED = "mwi_enabled";
	public static final String FIELD_PUBLISH_ENABLED = "publish_enabled";
	public static final String FIELD_REG_TIMEOUT = "reg_timeout";
	public static final String FIELD_KA_INTERVAL = "ka_interval";
	public static final String FIELD_PIDF_TUPLE_ID = "pidf_tuple_id";
	public static final String FIELD_FORCE_CONTACT = "force_contact";
	public static final String FIELD_CONTACT_PARAMS = "contact_params";
	public static final String FIELD_CONTACT_URI_PARAMS = "contact_uri_params";
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
		FIELD_PRIORITY, FIELD_ACC_ID, FIELD_REG_URI, FIELD_USE_TCP,	FIELD_MWI_ENABLED,
		FIELD_PUBLISH_ENABLED, FIELD_REG_TIMEOUT, FIELD_KA_INTERVAL, FIELD_PIDF_TUPLE_ID,
		FIELD_FORCE_CONTACT, FIELD_CONTACT_PARAMS, FIELD_CONTACT_URI_PARAMS,

		// Proxy infos
		FIELD_PROXY,

		// And now cred_info since for now only one cred info can be managed
		// In future release a credential table should be created
		FIELD_REALM, FIELD_SCHEME, FIELD_USERNAME, FIELD_DATATYPE,
		FIELD_DATA };
	
	
	//For now everything is public, easiest to manage
	public String display_name;
	public String wizard;
	public boolean active;
	public pjsua_acc_config cfg;
	public Integer id;
	public boolean use_tcp;
	
	public Account() {
		display_name = "";
		wizard = "EXPERT";
		active = true;
		use_tcp = false;
		
		cfg = new pjsua_acc_config();
		pjsua.acc_config_default(cfg);
		// Change the default ka interval to 40s
		cfg.setKa_interval(40);
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
		tmp_i = args.getAsInteger(FIELD_USE_TCP);	// Why doesn't getAsBoolean() work? The value is 1
		if (tmp_i != null) {
			use_tcp =(tmp_i != 0);
		} else {
			use_tcp = false;
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
		// Proxy
		tmp_s = args.getAsString(FIELD_PROXY);
		if (tmp_s != null) {
			cfg.setProxy_cnt(1);
			cfg.setProxy(pjsua.pj_str_copy(tmp_s));
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
		
		args.put(FIELD_PRIORITY, cfg.getPriority());
		args.put(FIELD_ACC_ID, cfg.getId().getPtr());
		args.put(FIELD_REG_URI, cfg.getReg_uri().getPtr());

		// MWI not yet in JNI

		args.put(FIELD_PUBLISH_ENABLED, cfg.getPublish_enabled());
		args.put(FIELD_REG_TIMEOUT, cfg.getReg_timeout());
		args.put(FIELD_KA_INTERVAL, cfg.getKa_interval());
		args.put(FIELD_PIDF_TUPLE_ID, cfg.getPidf_tuple_id().getPtr());
		args.put(FIELD_FORCE_CONTACT, cfg.getForce_contact().getPtr());

		// CONTACT_PARAM and CONTACT_PARAM_URI not yet in JNI

		// Assume we have an unique proxy
		if (cfg.getProxy_cnt() > 0) {
			args.put(FIELD_PROXY, cfg.getProxy().getPtr());
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
	
	
	
}
