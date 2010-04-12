/**
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
package com.csipsimple.db;

import java.util.ArrayList;
import java.util.List;

import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import com.csipsimple.models.Account;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.csipsimple.utils.Log;

public class DBAdapter {
	static String THIS_FILE = "SIP ACC_DB";

	private static final String DATABASE_NAME = "com.csipsimple.db";
	private static final int DATABASE_VERSION = 2;
	private static final String ACCOUNTS_TABLE_NAME = "accounts";

	
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

	// Creation sql command
	private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ ACCOUNTS_TABLE_NAME
			+ " ("
			+ FIELD_ID+ 				" INTEGER PRIMARY KEY AUTOINCREMENT,"

			// Application relative fields
			+ FIELD_ACTIVE				+ " INTEGER,"
			+ FIELD_WIZARD				+ " TEXT,"
			+ FIELD_DISPLAY_NAME		+ " TEXT,"

			// Here comes pjsua_acc_config fields
			+ FIELD_PRIORITY 			+ " INTEGER," 
			+ FIELD_ACC_ID 				+ " TEXT NOT NULL,"
			+ FIELD_REG_URI				+ " TEXT," 
			+ FIELD_MWI_ENABLED 		+ " BOOLEAN,"
			+ FIELD_PUBLISH_ENABLED 	+ " INTEGER," 
			+ FIELD_REG_TIMEOUT 		+ " INTEGER," 
			+ FIELD_PIDF_TUPLE_ID 		+ " TEXT,"
			+ FIELD_FORCE_CONTACT 		+ " TEXT,"
			+ FIELD_CONTACT_PARAMS 		+ " TEXT,"
			+ FIELD_CONTACT_URI_PARAMS	+ " TEXT,"

			// Proxy infos
			+ FIELD_PROXY				+ " TEXT,"

			// And now cred_info since for now only one cred info can be managed
			// In future release a credential table should be created
			+ FIELD_REALM 				+ " TEXT," 
			+ FIELD_SCHEME 				+ " TEXT," 
			+ FIELD_USERNAME			+ " TEXT," 
			+ FIELD_DATATYPE 			+ " INTEGER," 
			+ FIELD_DATA 				+ " TEXT"
			+ ");";

	private final static String[] common_projection = {
			FIELD_ID,
			// Application relative fields
			FIELD_ACTIVE, FIELD_WIZARD, FIELD_DISPLAY_NAME,

			// Here comes pjsua_acc_config fields
			FIELD_PRIORITY, FIELD_ACC_ID, FIELD_REG_URI, FIELD_MWI_ENABLED,
			FIELD_PUBLISH_ENABLED, FIELD_REG_TIMEOUT, FIELD_PIDF_TUPLE_ID,
			FIELD_FORCE_CONTACT, FIELD_CONTACT_PARAMS, FIELD_CONTACT_URI_PARAMS,

			// Proxy infos
			FIELD_PROXY,

			// And now cred_info since for now only one cred info can be managed
			// In future release a credential table should be created
			FIELD_REALM, FIELD_SCHEME, FIELD_USERNAME, FIELD_DATATYPE,
			FIELD_DATA };

	private final Context context;

	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(THIS_FILE, "Upgrading database from version "
							+ oldVersion + " to " + newVersion
							+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Open database
	 * 
	 * @return database adapter
	 * @throws SQLException
	 */
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close database
	 */
	public void close() {
		DBHelper.close();
	}

	// TODO : deleteAccount, getAllAccounts, updateAccount, createAccount

	// Transform pjsua_acc_config into ContentValues that can be insert into
	// database
	private ContentValues accountToContentValues(Account acc) {
		ContentValues args = new ContentValues();
		
		if(acc.id != null){
			args.put(FIELD_ACTIVE, acc.id);
		}
		args.put(FIELD_ACTIVE, acc.active?"1":"0");
		args.put(FIELD_WIZARD, acc.wizard);
		args.put(FIELD_DISPLAY_NAME, acc.display_name);

		args.put(FIELD_PRIORITY, acc.cfg.getPriority());
		args.put(FIELD_ACC_ID, acc.cfg.getId().getPtr());
		args.put(FIELD_REG_URI, acc.cfg.getReg_uri().getPtr());

		// MWI not yet in JNI

		args.put(FIELD_PUBLISH_ENABLED, acc.cfg.getPublish_enabled());
		args.put(FIELD_REG_TIMEOUT, acc.cfg.getReg_timeout());
		args.put(FIELD_PIDF_TUPLE_ID, acc.cfg.getPidf_tuple_id().getPtr());
		args.put(FIELD_FORCE_CONTACT, acc.cfg.getForce_contact().getPtr());

		// CONTACT_PARAM and CONTACT_PARAM_URI not yet in JNI

		// Assume we have an unique proxy
		args.put(FIELD_PROXY, acc.cfg.getProxy().getPtr());

		// Assume we have an unique credential
		pjsip_cred_info cred_info = acc.cfg.getCred_info();
		args.put(FIELD_REALM, cred_info.getRealm().getPtr());
		args.put(FIELD_SCHEME, cred_info.getScheme().getPtr());
		args.put(FIELD_USERNAME, cred_info.getUsername().getPtr());
		args.put(FIELD_DATATYPE, cred_info.getData_type());
		args.put(FIELD_DATA, cred_info.getData().getPtr());

		return args;
	}

	// Transform a ContentValues into a pjsua_acc_config object
	// use cursorRowToContentValues from dbHelper to transform cursor into
	// ContentValues
	private Account contentValuesToAccount(ContentValues args) {
		Account acc = new Account();
		

		Integer tmp_i;
		String tmp_s;
		
		//Application specific settings
		tmp_i = args.getAsInteger(FIELD_ID);
		if (tmp_i != null) {
			acc.id = tmp_i;
		}
		tmp_s = args.getAsString(FIELD_DISPLAY_NAME);
		if (tmp_s != null) {
			acc.display_name = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_WIZARD);
		if (tmp_s != null) {
			acc.wizard = tmp_s;
		}
		
		tmp_i = args.getAsInteger(FIELD_ACTIVE);
		if (tmp_i != null) {
			acc.active = (tmp_i != 0);
		}else{
			acc.active = true;
		}
		
		// Credentials
		acc.cfg.setCred_count(1);

		// General account settings
		tmp_i = args.getAsInteger(FIELD_PRIORITY);
		if (tmp_i != null) {
			acc.cfg.setPriority((int) tmp_i);
		}
		tmp_s = args.getAsString(FIELD_ACC_ID);
		if (tmp_s != null) {
			acc.cfg.setId(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_REG_URI);
		if (tmp_s != null) {
			acc.cfg.setReg_uri(pjsua.pj_str_copy(tmp_s));
		}
		tmp_i = args.getAsInteger(FIELD_PUBLISH_ENABLED);
		if (tmp_i != null) {
			acc.cfg.setPublish_enabled(tmp_i);
		}
		tmp_i = args.getAsInteger(FIELD_REG_TIMEOUT);
		if (tmp_i != null) {
			acc.cfg.setReg_timeout(tmp_i);
		}
		tmp_s = args.getAsString(FIELD_PIDF_TUPLE_ID);
		if (tmp_s != null) {
			acc.cfg.setPidf_tuple_id(pjsua.pj_str_copy(tmp_s));
		}
		tmp_s = args.getAsString(FIELD_FORCE_CONTACT);
		if (tmp_s != null) {
			acc.cfg.setForce_contact(pjsua.pj_str_copy(tmp_s));
		}
		// Proxy
		tmp_s = args.getAsString(FIELD_PROXY);
		if (tmp_s != null) {
			acc.cfg.setProxy_cnt(1);
			acc.cfg.setProxy(pjsua.pj_str_copy(tmp_s));
		}
		
		pjsip_cred_info cred_info = acc.cfg.getCred_info();

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
		
		
		return acc;
	}

	/**
	 * Delete account
	 * 
	 * @param account_id
	 *            account identifier (primary key in database)
	 * @return true if succeed
	 */
	public boolean deleteAccount(int account_id) {
		return db
				.delete(ACCOUNTS_TABLE_NAME, FIELD_ID + "=" + account_id, null) > 0;
	}

	/**
	 * Update an account with new values
	 * 
	 * @param account_id
	 *            account identifier (primary key in database)
	 * @param cfg
	 *            account pjsip configuration structure
	 * @param active
	 *            whether account is active
	 * @return true if succeed
	 */
	public boolean updateAccount(Account acc) {
		ContentValues args = accountToContentValues(acc);

		return db.update(ACCOUNTS_TABLE_NAME, args,
				FIELD_ID + "=" + acc.id, null) > 0;
	}
	
	
	public long insertAccount(Account acc){
		ContentValues args = accountToContentValues(acc);
		
		return db.insert(ACCOUNTS_TABLE_NAME, null, args);
	}

	public List<Account> getListAccounts() {
		ArrayList<Account> ret = new ArrayList<Account>();
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, common_projection,
					null, null, null, null, FIELD_PRIORITY
							+ " ASC");
			int numRows = c.getCount();
			Log.i(THIS_FILE, "Found rows : "+numRows);
			c.moveToFirst();
			for (int i = 0; i < numRows; ++i) {
				
				ContentValues args = new ContentValues();
				DatabaseUtils.cursorRowToContentValues(c, args);
				//Commented since password can be print and other app could get back this info
				//Log.i(THIS_FILE, "Content values extracted : "+args.toString());
				
				Account acc = contentValuesToAccount(args);
				
				ret.add(acc);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}

		return ret;
	}
	
	
	public Account getAccount(long account_id){
		
		if(account_id <0){
			return new Account();
		}
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, common_projection,
					FIELD_ID + "=" + account_id, null, null, null, null);
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				
				ContentValues args = new ContentValues();
				DatabaseUtils.cursorRowToContentValues(c, args);
				c.close();
				return contentValuesToAccount(args);
				
			}
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		
		return null;
		
	}
	
	public int getNbrOfAccount() {
		Cursor c = db.query(ACCOUNTS_TABLE_NAME, new String[] {
			FIELD_ID	
		}, null, null, null, null, null);
		int numRows = c.getCount();
		c.close();
		return numRows;
	}

}
