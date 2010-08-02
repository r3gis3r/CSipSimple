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
package com.csipsimple.db;

import java.util.ArrayList;
import java.util.List;

import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsuaConstants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.RemoteException;
import android.provider.CallLog;

import com.csipsimple.models.Account;
import com.csipsimple.models.AccountInfo;
import com.csipsimple.models.Filter;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

public class DBAdapter {
	static String THIS_FILE = "SIP ACC_DB";

	private static final String DATABASE_NAME = "com.csipsimple.db";
	private static final int DATABASE_VERSION = 7;
	private static final String ACCOUNTS_TABLE_NAME = "accounts";
	private static final String CALLLOGS_TABLE_NAME = "calllogs";
	private static final String FILTERS_TABLE_NAME = "outgoing_filters";
	

	
	

	// Creation sql command
	private static final String TABLE_ACCOUNT_CREATE = "CREATE TABLE IF NOT EXISTS "
		+ ACCOUNTS_TABLE_NAME
		+ " ("
			+ Account.FIELD_ID+ 				" INTEGER PRIMARY KEY AUTOINCREMENT,"

			// Application relative fields
			+ Account.FIELD_ACTIVE				+ " INTEGER,"
			+ Account.FIELD_WIZARD				+ " TEXT,"
			+ Account.FIELD_DISPLAY_NAME		+ " TEXT,"
			+ Account.FIELD_USE_TCP				+ " BOOLEAN,"

			// Here comes pjsua_acc_config fields
			+ Account.FIELD_PRIORITY 			+ " INTEGER," 
			+ Account.FIELD_ACC_ID 				+ " TEXT NOT NULL,"
			+ Account.FIELD_REG_URI				+ " TEXT," 
			+ Account.FIELD_MWI_ENABLED 		+ " BOOLEAN,"
			+ Account.FIELD_PUBLISH_ENABLED 	+ " INTEGER," 
			+ Account.FIELD_REG_TIMEOUT 		+ " INTEGER," 
			+ Account.FIELD_KA_INTERVAL 		+ " INTEGER," 
			+ Account.FIELD_PIDF_TUPLE_ID 		+ " TEXT,"
			+ Account.FIELD_FORCE_CONTACT 		+ " TEXT,"
			+ Account.FIELD_CONTACT_PARAMS 		+ " TEXT,"
			+ Account.FIELD_CONTACT_URI_PARAMS	+ " TEXT,"

			// Proxy infos
			+ Account.FIELD_PROXY				+ " TEXT,"

			// And now cred_info since for now only one cred info can be managed
			// In future release a credential table should be created
			+ Account.FIELD_REALM 				+ " TEXT," 
			+ Account.FIELD_SCHEME 				+ " TEXT," 
			+ Account.FIELD_USERNAME			+ " TEXT," 
			+ Account.FIELD_DATATYPE 			+ " INTEGER," 
			+ Account.FIELD_DATA 				+ " TEXT"
		+ ");";
	
	private final static String TABLE_CALLLOGS_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ CALLLOGS_TABLE_NAME
			+ " ("
			+ CallLog.Calls._ID					+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ CallLog.Calls.CACHED_NAME			+ " TEXT,"
			+ CallLog.Calls.CACHED_NUMBER_LABEL	+ " TEXT,"
			+ CallLog.Calls.CACHED_NUMBER_TYPE	+ " INTEGER,"
			+ CallLog.Calls.DATE				+ " INTEGER,"
			+ CallLog.Calls.DURATION			+ " INTEGER,"
			+ CallLog.Calls.NEW					+ " INTEGER,"
			+ CallLog.Calls.NUMBER				+ " TEXT,"
			+ CallLog.Calls.TYPE				+ " INTEGER"
			+");";
	
	private static final String TABLE_FILTERS_CREATE =  "CREATE TABLE IF NOT EXISTS "
		+ FILTERS_TABLE_NAME
		+ " ("
			+ Filter._ID						+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ Filter.FIELD_PRIORITY 			+ " INTEGER," 
	
			// Foreign key to account
			+ Filter.FIELD_ACCOUNT				+ " INTEGER,"
			// Match/replace
			+ Filter.FIELD_MATCHES				+ " TEXT,"
			+ Filter.FIELD_REPLACE				+ " TEXT,"
	
			+ Filter.FIELD_ACTION 				+ " INTEGER" 
		+ ");";
	

	private final Context context;
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context aContext) {
		context = aContext;
		databaseHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(TABLE_ACCOUNT_CREATE);
			db.execSQL(TABLE_CALLLOGS_CREATE);
			db.execSQL(TABLE_FILTERS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(THIS_FILE, "Upgrading database from version "
							+ oldVersion + " to " + newVersion);
			if(oldVersion < 1) {
				db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE_NAME);
			} else if(oldVersion < 7) {
				if(oldVersion < 5) {
					db.execSQL("ALTER TABLE "+ACCOUNTS_TABLE_NAME+" ADD "+Account.FIELD_KA_INTERVAL+" INTEGER");
				}
				if(oldVersion < 6) {
					db.execSQL("DROP TABLE IF EXISTS "+FILTERS_TABLE_NAME);
				}
				db.execSQL("ALTER TABLE "+ACCOUNTS_TABLE_NAME+" ADD "+Account.FIELD_USE_TCP+" BOOLEAN");
			}
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
		db = databaseHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close database
	 */
	public void close() {
		databaseHelper.close();
	}
	
	
	// --------
	// Accounts
	// --------
	
	/**
	 * Delete account
	 * 
	 * @param account account to delete into the database 
	 * You have to be sure this account exists before deleting it
	 * @return true if succeed
	 */
	public boolean deleteAccount(Account account) {
		return db.delete(ACCOUNTS_TABLE_NAME, Account.FIELD_ID + "=" + account.id, null) > 0;
	}

	/**
	 * Update an account with new values
	 * 
	 * @param account account to update into the database
	 * You have to be sure this account exists before update it 
	 * @return true if succeed
	 */
	public boolean updateAccount(Account account) {
		return db.update(ACCOUNTS_TABLE_NAME, account.getDbContentValues(),
				Account.FIELD_ID + "=" + account.id, null) > 0;
	}
	
	/**
	 * Insert a new account into the database
	 * @param account account to add into the database
	 * @return the id of inserted row into database
	 */
	public long insertAccount(Account account){
		return db.insert(ACCOUNTS_TABLE_NAME, null, account.getDbContentValues());
	}

	/**
	 * Get the list of all saved account
	 * @return the list of accounts
	 */
	public List<Account> getListAccounts() {
		return getListAccounts(true, null);
	}
	
	/**
	 * Get the list of saved accounts, optionally including the unusable ones
	 * @param includeUnusable true if list should contain accounts that can NOT presently be used for calls
	 * @param service Ignored if includeUsable is false. If non-null, full usability test is done, else only disabled accounts are skipped.
	 * @return the list of accounts
	 */
	public List<Account> getListAccounts(boolean includeUnusable, ISipService service) {
		ArrayList<Account> ret = new ArrayList<Account>();
		if(SipService.hasSipStack) {
			try {
				Cursor c = db.query(ACCOUNTS_TABLE_NAME, Account.common_projection,
						null, null, null, null, Account.FIELD_PRIORITY
								+ " ASC");
				int numRows = c.getCount();
				c.moveToFirst();
				for (int i = 0; i < numRows; ++i) {
					Account acc = new Account();
					acc.createFromDb(c);
					if (includeUnusable) {
						ret.add(acc);
					} else {
						if (service == null) {
							// No service, just skip inactive accounts
							if (acc.active) {
								ret.add(acc);
							}
						} else {
							// Service available, skip accounts that can't be used for calling
							
							// Maybe this is not really a good idea to do it here (should be only db related...).
							try {
								AccountInfo accountInfo = service.getAccountInfo(acc.id);
								if(accountInfo != null) {
									pjsip_status_code stat = accountInfo.getStatusCode();
									if (accountInfo != null && accountInfo.isActive() &&
											accountInfo.getAddedStatus() == pjsuaConstants.PJ_SUCCESS &&
											(stat == pjsip_status_code.PJSIP_SC_OK ||	// If trying/registering, include. May be OK shortly.
												 stat == pjsip_status_code.PJSIP_SC_PROGRESS || 
												 stat == pjsip_status_code.PJSIP_SC_TRYING)) {
										ret.add(acc);
									}
								}
							} catch (RemoteException e) { 
								Log.w(THIS_FILE, "Sip service not available", e);
							}
						}
					}
					c.moveToNext();
				}
				c.close();
			} catch (SQLException e) {
				Log.e("Exception on query", e.toString());
			}
		}

		return ret;
	}
	
	/**
	 * Get the corresponding account for a give id (id is database id)
	 * If account_id is < 0 a new account is created and returned
	 * If account_id is not found null is returned
	 * @param accountId id of the account in the sqlite database
	 * @return The corresponding account
	 */
	public Account getAccount(long accountId){
		
		if(accountId <0){
			return new Account();
		}
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, Account.common_projection,
					Account.FIELD_ID + "=" + accountId, null, null, null, null);
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				
				Account account = new Account();
				account.createFromDb(c);
				c.close();
				return account;
				
			}
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		
		return null;
	}
	
	/**
	 * Count the number of account saved in database
	 * @return the number of account
	 */
	public int getNbrOfAccount() {
		Cursor c = db.query(ACCOUNTS_TABLE_NAME, new String[] {
			Account.FIELD_ID	
		}, null, null, null, null, null);
		int numRows = c.getCount();
		c.close();
		return numRows;
	}
	
	public int countAvailableAccountsForNumber(String number) {
		
		return 0;
	}
	
	
	// -------------
	// Call logs
	// -------------
	
	private final static String[] logs_projection = {
		CallLog.Calls._ID,
		CallLog.Calls.CACHED_NAME,
		CallLog.Calls.CACHED_NUMBER_LABEL,
		CallLog.Calls.CACHED_NUMBER_TYPE,
		CallLog.Calls.DURATION,
		CallLog.Calls.DATE,
		CallLog.Calls.NEW,
		CallLog.Calls.NUMBER,
		CallLog.Calls.TYPE
	};
	
	public static final int ID_COLUMN_INDEX = 0;
	// Cached columns, not yet supported
	public static final int CALLER_NAME_COLUMN_INDEX = 1;
	public static final int CALLER_NUMBERLABEL_COLUMN_INDEX = 2;
	public static final int CALLER_NUMBERTYPE_COLUMN_INDEX = 3;
	// Sure this columns are filled
	public static final int DURATION_COLUMN_INDEX = 4;
	public static final int DATE_COLUMN_INDEX = 5;
	public static final int NEW_COLUMN_INDEX = 6;
	public static final int NUMBER_COLUMN_INDEX = 7;
	public static final int CALL_TYPE_COLUMN_INDEX = 8;
	
	
	/**
	 * Insert a new calllog into the database
	 * @param values calllogs values
	 * @return the id of inserted row into database
	 */
	public long insertCallLog(ContentValues args){
		return db.insert(CALLLOGS_TABLE_NAME, null, args);
	}
	
	/**
	 * Count the number of account saved in database
	 * @return the number of account
	 */
	public int getNbrOfCallLogs() {
		Cursor c = db.rawQuery("SELECT COUNT("+CallLog.Calls._ID+");", null);
		int numRows = 0;
		if(c.getCount() > 0){
			c.moveToFirst();
			numRows = c.getInt(0);
			
		}
		c.close();
		return numRows;
	}
	
	
	
	public Cursor getAllCallLogs() {
		return db.query(CALLLOGS_TABLE_NAME, logs_projection, null, null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
	}

	public Cursor getCallLog(int logId) {
		if(logId <0){
			return null;
		}
		try {
			return db.query(CALLLOGS_TABLE_NAME, logs_projection,
					CallLog.Calls._ID + "=" + logId, null, null, null, null);
		} catch (SQLException e) {
			Log.e(THIS_FILE, "Exception on query", e);
		}
		
		return null;
	}
	
	public boolean deleteCallLog(int calllogId) {
		return db.delete(CALLLOGS_TABLE_NAME, CallLog.Calls._ID  + "=" + calllogId, null) > 0;
	}

	
	// --------
	// Filters
	// --------
	
	
	/**
	 * Insert a new filter into the database
	 * @param values filter values
	 * @return the id of inserted row into database
	 */
	public long insertFilter(ContentValues args){
		return db.insert(FILTERS_TABLE_NAME, null, args);
	}
	
	
	public Cursor getFiltersForAccount(int account_id) {
		return db.query(FILTERS_TABLE_NAME, Filter.common_projection, 
				Filter.FIELD_ACCOUNT+"=?", new String[]{Integer.toString(account_id)}, 
				null, null, Filter.DEFAULT_ORDER);
	}

	public Filter getFilter(int filterId) {

		if(filterId <0){
			return new Filter();
		}
		try {
			Cursor c = db.query(FILTERS_TABLE_NAME, Filter.common_projection,
					CallLog.Calls._ID + "=" + filterId, null, null, null, null);
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				Filter filter = new Filter();
				filter.createFromDb(c);
				c.close();
				return filter;
				
			}
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		
		return null;
	}
	/**
	 * Delete account
	 * 
	 * @param account account to delete into the database 
	 * You have to be sure this account exists before deleting it
	 * @return true if succeed
	 */
	public boolean deleteFilter(Filter filter) {
		return db.delete(FILTERS_TABLE_NAME, Filter._ID + "=" + filter.id, null) > 0;
	}

	/**
	 * Update an account with new values
	 * 
	 * @param account account to update into the database
	 * You have to be sure this account exists before update it 
	 * @return true if succeed
	 */
	public boolean updateFilter(Filter filter) {
		return db.update(FILTERS_TABLE_NAME, filter.getDbContentValues(),
				Filter._ID + "=" + filter.id, null) > 0;
	}
	
	/**
	 * Insert a new account into the database
	 * @param account account to add into the database
	 * @return the id of inserted row into database
	 */
	public long insertFilter(Filter filter){
		return db.insert(FILTERS_TABLE_NAME, null, filter.getDbContentValues());
	}

	
	
}
