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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.models.SipMessage;
import com.csipsimple.utils.Log;

public class DBAdapter {
	static String THIS_FILE = "SIP ACC_DB";

	private static final String DATABASE_NAME = "com.csipsimple.db";
	private static final int DATABASE_VERSION = 26;
	private static final String ACCOUNTS_TABLE_NAME = "accounts";
	private static final String CALLLOGS_TABLE_NAME = "calllogs";
	private static final String FILTERS_TABLE_NAME = "outgoing_filters";
	private static final String MESSAGES_TABLE_NAME = "messages";
	

	
	

	// Creation sql command
	private static final String TABLE_ACCOUNT_CREATE = "CREATE TABLE IF NOT EXISTS "
		+ ACCOUNTS_TABLE_NAME
		+ " ("
			+ SipProfile.FIELD_ID+ 				" INTEGER PRIMARY KEY AUTOINCREMENT,"

			// Application relative fields
			+ SipProfile.FIELD_ACTIVE				+ " INTEGER,"
			+ SipProfile.FIELD_WIZARD				+ " TEXT,"
			+ SipProfile.FIELD_DISPLAY_NAME		+ " TEXT,"

			// Here comes pjsua_acc_config fields
			+ SipProfile.FIELD_PRIORITY 			+ " INTEGER," 
			+ SipProfile.FIELD_ACC_ID 				+ " TEXT NOT NULL,"
			+ SipProfile.FIELD_REG_URI				+ " TEXT," 
			+ SipProfile.FIELD_MWI_ENABLED 		+ " BOOLEAN,"
			+ SipProfile.FIELD_PUBLISH_ENABLED 	+ " INTEGER," 
			+ SipProfile.FIELD_REG_TIMEOUT 		+ " INTEGER," 
			+ SipProfile.FIELD_KA_INTERVAL 		+ " INTEGER," 
			+ SipProfile.FIELD_PIDF_TUPLE_ID 		+ " TEXT,"
			+ SipProfile.FIELD_FORCE_CONTACT 		+ " TEXT,"
			+ SipProfile.FIELD_ALLOW_CONTACT_REWRITE + " INTEGER,"
			+ SipProfile.FIELD_CONTACT_REWRITE_METHOD + " INTEGER,"
			+ SipProfile.FIELD_CONTACT_PARAMS 		+ " TEXT,"
			+ SipProfile.FIELD_CONTACT_URI_PARAMS	+ " TEXT,"
			+ SipProfile.FIELD_TRANSPORT	 		+ " INTEGER," 
			+ SipProfile.FIELD_USE_SRTP	 			+ " INTEGER," 
			+ SipProfile.FIELD_USE_ZRTP	 			+ " INTEGER," 

			// Proxy infos
			+ SipProfile.FIELD_PROXY				+ " TEXT,"
			+ SipProfile.FIELD_REG_USE_PROXY		+ " INTEGER,"

			// And now cred_info since for now only one cred info can be managed
			// In future release a credential table should be created
			+ SipProfile.FIELD_REALM 				+ " TEXT," 
			+ SipProfile.FIELD_SCHEME 				+ " TEXT," 
			+ SipProfile.FIELD_USERNAME				+ " TEXT," 
			+ SipProfile.FIELD_DATATYPE 			+ " INTEGER," 
			+ SipProfile.FIELD_DATA 				+ " TEXT,"
			
			
			+ SipProfile.FIELD_SIP_STACK 			+ " INTEGER," 
			+ SipProfile.FIELD_VOICE_MAIL_NBR		+ " TEXT,"
			+ SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH	+ " INTEGER" 
			
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
	
	private final static String TABLE_MESSAGES_CREATE = "CREATE TABLE IF NOT EXISTS "
		+ MESSAGES_TABLE_NAME
		+ " ("
			+ SipMessage.FIELD_ID					+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SipMessage.FIELD_FROM				+ " TEXT,"
			+ SipMessage.FIELD_TO					+ " TEXT,"
			+ SipMessage.FIELD_CONTACT				+ " TEXT,"
			+ SipMessage.FIELD_BODY				+ " TEXT,"
			+ SipMessage.FIELD_MIME_TYPE			+ " TEXT,"
			+ SipMessage.FIELD_TYPE				+ " INTEGER,"
			+ SipMessage.FIELD_DATE				+ " INTEGER,"
			+ SipMessage.FIELD_STATUS			+ " INTEGER,"
			+ SipMessage.FIELD_READ				+ " BOOLEAN,"
			+ SipMessage.FIELD_FROM_FULL		+ " TEXT"
		+");";
	

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
			db.execSQL(TABLE_MESSAGES_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(THIS_FILE, "Upgrading database from version "
							+ oldVersion + " to " + newVersion);
			if(oldVersion < 1) {
				db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE_NAME);
			}
			if(oldVersion < 5) {
				try {
					db.execSQL("ALTER TABLE "+ACCOUNTS_TABLE_NAME+" ADD "+SipProfile.FIELD_KA_INTERVAL+" INTEGER");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 6) {
				db.execSQL("DROP TABLE IF EXISTS "+FILTERS_TABLE_NAME);
			}
			if(oldVersion < 10) {
				try {
					db.execSQL("ALTER TABLE "+ACCOUNTS_TABLE_NAME+" ADD "+
							SipProfile.FIELD_ALLOW_CONTACT_REWRITE + " INTEGER");
					db.execSQL("ALTER TABLE "+ACCOUNTS_TABLE_NAME+" ADD "+
							SipProfile.FIELD_CONTACT_REWRITE_METHOD + " INTEGER");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 13) {
				try {
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD " + SipProfile.FIELD_TRANSPORT + " INTEGER");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_UDP + " WHERE prevent_tcp=1");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_TCP + " WHERE use_tcp=1");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_AUTO + " WHERE use_tcp=0 AND prevent_tcp=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
				
			}
			if(oldVersion < 17) {
				try {
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_KA_INTERVAL + "=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 18) {
				try {
					//As many users are crying... remove auto transport and force udp
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "="+SipProfile.TRANSPORT_UDP +" WHERE "+ SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_AUTO);
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 22) {
				try {
					//Add use proxy row
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_REG_USE_PROXY + " INTEGER");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_REG_USE_PROXY + "=3");
					//Add stack field
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_SIP_STACK + " INTEGER");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_SIP_STACK + "=0");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 23) {
				try {
					//Add use zrtp row
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_USE_ZRTP + " INTEGER");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_USE_ZRTP + "=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 24) {
				try {
					//Add voice mail row
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_VOICE_MAIL_NBR + " TEXT");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_VOICE_MAIL_NBR + "=''");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 25) {
				try {
					//Add voice mail row
					db.execSQL("ALTER TABLE " + MESSAGES_TABLE_NAME + " ADD "+
							SipMessage.FIELD_FROM_FULL + " TEXT");
					db.execSQL("UPDATE " + MESSAGES_TABLE_NAME + " SET " + SipMessage.FIELD_FROM_FULL + "="+ SipMessage.FIELD_FROM);
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 26) {
				try {
					//Add reg delay before refresh row
					db.execSQL("ALTER TABLE " + ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH + " INTEGER");
					db.execSQL("UPDATE " + ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH + "=-1");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			
			onCreate(db);
		}
	}

	private boolean opened = false;
	/**
	 * Open database
	 * 
	 * @return database adapter
	 * @throws SQLException
	 */
	public DBAdapter open() throws SQLException {
		db = databaseHelper.getWritableDatabase();
		opened = true;
		return this;
	}

	/**
	 * Close database
	 */
	public void close() {
		databaseHelper.close();
		opened = false;
	}
	
	public boolean isOpen() {
		return opened;
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
	public boolean deleteAccount(SipProfile account) {
		return db.delete(ACCOUNTS_TABLE_NAME, SipProfile.FIELD_ID + "=" + account.id, null) > 0;
	}

	/**
	 * Update an account with new values
	 * 
	 * @param account account to update into the database
	 * You have to be sure this account exists before update it 
	 * @return true if succeed
	 */
	public boolean updateAccount(SipProfile account) {
		return db.update(ACCOUNTS_TABLE_NAME, account.getDbContentValues(),
				SipProfile.FIELD_ID + "=" + account.id, null) > 0;
	}
	

	public boolean updateAccountPriority(long accId, int currentPriority) {
		ContentValues args = new ContentValues();
		args.put(SipProfile.FIELD_PRIORITY, currentPriority);
		return db.update(ACCOUNTS_TABLE_NAME, args,
				SipProfile.FIELD_ID + "=" + accId, null) > 0;
	}
	
	/**
	 * Insert a new account into the database
	 * @param account account to add into the database
	 * @return the id of inserted row into database
	 */
	public long insertAccount(SipProfile account){
		return db.insert(ACCOUNTS_TABLE_NAME, null, account.getDbContentValues());
	}

	/**
	 * Get the list of all saved account
	 * @return the list of accounts
	 */
	public List<SipProfile> getListAccounts() {
		return getListAccounts(false/*, null*/);
	}
	
	/**
	 * Get the list of saved accounts, optionally including the unusable ones
	 * @param includeUnusable true if list should contain accounts that can NOT presently be used for calls
	 * @return the list of accounts
	 */
	public List<SipProfile> getListAccounts(boolean activesOnly) {
		ArrayList<SipProfile> ret = new ArrayList<SipProfile>();

		try {
			String whereClause = null;
			String[] whereArgs = null;
			if (activesOnly) {
				whereClause = SipProfile.FIELD_ACTIVE+"=?";
				whereArgs = new String[] {"1"};
			}
			
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, SipProfile.full_projection,
					whereClause, whereArgs, null, null, SipProfile.FIELD_PRIORITY
							+ " DESC");
			int numRows = c.getCount();
			c.moveToFirst();
			for (int i = 0; i < numRows; ++i) {
				SipProfile acc = new SipProfile();
				acc.createFromDb(c);
				ret.add(acc);
				c.moveToNext();
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
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
	public SipProfile getAccount(long accountId){
		
		if(accountId <0){
			return new SipProfile();
		}
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, SipProfile.full_projection,
					SipProfile.FIELD_ID + "=" + accountId, null, null, null, null);
			
			
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				SipProfile account = new SipProfile();
				account.createFromDb(c);
				c.close();
				return account;
				
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		
		return null;
	}
	


	public SipProfile getAccountForWizard(String wizardId) {
		if(wizardId  == null){
			return new SipProfile();
		}
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, SipProfile.full_projection,
					SipProfile.FIELD_WIZARD + "=?", new String[] {wizardId}, null, null, null);
			
			
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				SipProfile account = new SipProfile();
				account.createFromDb(c);
				c.close();
				return account;
				
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		SipProfile acc = new SipProfile();
		acc.wizard = wizardId;
		return acc;
	}
	
	public ContentValues getAccountValues(long accountId){
		if(accountId <0){
			return null;
		}
		try {
			Cursor c = db.query(ACCOUNTS_TABLE_NAME, SipProfile.full_projection,
					SipProfile.FIELD_ID + "=" + accountId, null, null, null, null);
			
			
			int numRows = c.getCount();
			if(numRows > 0){
				c.moveToFirst();
				ContentValues args = new ContentValues();
				DatabaseUtils.cursorRowToContentValues(c, args);
				c.close();
				return args;
				
			}
			c.close();
		} catch (SQLException e) {
			Log.e("Exception on query", e.toString());
		}
		
		return null;
	}
	
	
	public boolean setAccountActive(long accountId, boolean active) {
		ContentValues cv = new ContentValues();
		cv.put(SipProfile.FIELD_ACTIVE, active?1:0);
		boolean result = db.update(ACCOUNTS_TABLE_NAME, cv,
				SipProfile.FIELD_ID + "=" + accountId, null) > 0;
		
		if(result) {
			Intent publishIntent = new Intent(SipManager.ACTION_SIP_ACCOUNT_ACTIVE_CHANGED);
			publishIntent.putExtra(SipManager.EXTRA_ACCOUNT_ID, accountId);
			publishIntent.putExtra(SipManager.EXTRA_ACTIVATE, active);
			context.sendBroadcast(publishIntent);
		}
		return result;
	}
	

	public boolean setAccountWizard(int accountId, String wizardId) {
		ContentValues cv = new ContentValues();
		cv.put(SipProfile.FIELD_WIZARD, wizardId);
		boolean result = db.update(ACCOUNTS_TABLE_NAME, cv,
				SipProfile.FIELD_ID + "=" + accountId, null) > 0;
		return result;
	}
	
	
	public int getNbrOfAccount() {
		return getNbrOfAccount(false);
	}
	
	/**
	 * Count the number of account saved in database
	 * @return the number of account
	 */
	public int getNbrOfAccount(boolean activesOnly) {
		String whereClause = null;
		String[] whereArgs = null;
		if (!activesOnly) {
			whereClause = SipProfile.FIELD_ACTIVE+"=?";
			whereArgs = new String[] {"1"};
		}
		Cursor c = db.query(ACCOUNTS_TABLE_NAME, new String[] {
				SipProfile.FIELD_ID	
		}, whereClause, whereArgs, null, null, null);
		int numRows = c.getCount();
		c.close();
		return numRows;
	}
	

	public void removeAllAccounts() {
		db.delete(FILTERS_TABLE_NAME, "1", null);
		db.delete(ACCOUNTS_TABLE_NAME, "1", null);
		
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
	public long insertCallLog(ContentValues args) {
		long result = db.insert(CALLLOGS_TABLE_NAME, null, args);
		removeCallLogExpiredEntries();
		return result;
	}
	
	/**
	 * Count the number of account saved in database
	 * @return the number of account
	 */
	public int getNbrOfCallLogs() {
		Cursor c = db.rawQuery("SELECT COUNT("+CallLog.Calls._ID+") FROM "+CALLLOGS_TABLE_NAME+";", null);
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
	
	public boolean deleteOneCallLog(int calllogId) {
		return db.delete(CALLLOGS_TABLE_NAME, CallLog.Calls._ID  + "=" + calllogId, null) > 0;
	}

	public boolean deleteAllCallLogs() {
		return db.delete(CALLLOGS_TABLE_NAME, null, null) > 0;
	}
	
	
	private void removeCallLogExpiredEntries() {
		db.delete(CALLLOGS_TABLE_NAME, CallLog.Calls._ID + " IN " +
			"(SELECT "+CallLog.Calls._ID+" FROM "+CALLLOGS_TABLE_NAME+" ORDER BY " + 
				CallLog.Calls.DEFAULT_SORT_ORDER + " LIMIT -1 OFFSET 500)", null);
	}
	
	// --------
	// Filters
	// --------
	
	
	/**
	 * Insert a new filter into the database
	 * @param Filter Filter to add into the database
	 * @return the id of inserted row into database
	 */
	public long insertFilter(Filter filter){
		return db.insert(FILTERS_TABLE_NAME, null, filter.getDbContentValues());
	}
	
	public Cursor getFiltersForAccount(int account_id) {
		//Log.d(THIS_FILE, "Get filters for account "+account_id);
		return db.query(FILTERS_TABLE_NAME, Filter.full_projection, 
				Filter.FIELD_ACCOUNT+"=?", new String[]{Integer.toString(account_id)}, 
				null, null, Filter.DEFAULT_ORDER);
	}
	
	public int getCountFiltersForAccount(int account_id) {
		Cursor c = db.rawQuery("SELECT COUNT(" + Filter.FIELD_ACCOUNT + ") FROM " + 
									FILTERS_TABLE_NAME + " WHERE " + Filter.FIELD_ACCOUNT + "=?;",
				new String[]{Integer.toString(account_id)});
		int numRows = 0;
		if(c.getCount() > 0){
			c.moveToFirst();
			numRows = c.getInt(0);
			
		}
		c.close();
		return numRows;
	}
	
	

	public Filter getFilter(int filterId) {

		if(filterId <0){
			return new Filter();
		}
		try {
			Cursor c = db.query(FILTERS_TABLE_NAME, Filter.full_projection,
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
		return deleteFilter(filter.id);
	}
	
	public boolean deleteFilter(int id) {
		return db.delete(FILTERS_TABLE_NAME, Filter._ID + "=?" , new String[] { Integer.toString(id) }) > 0;
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
	
	public boolean updateFilterPriority(long filterId, int newPriority) {
		ContentValues args = new ContentValues();
		args.put(Filter.FIELD_PRIORITY, newPriority);
		return db.update(FILTERS_TABLE_NAME, args,
				Filter._ID + "=" + filterId, null) > 0;
	}
	

	// --------
	// MESSAGES
	// --------

	/**
	 * Insert a new message into the database
	 * @param message SipMessage to add into the database
	 * @return the id of inserted row into database
	 */
	public long insertMessage(SipMessage message){
		return db.insert(MESSAGES_TABLE_NAME, null, message.getContentValues());
	}

	public Cursor getAllConversations() {
		return db.query(MESSAGES_TABLE_NAME, 
				new String[]{
					"ROWID AS _id",
					SipMessage.FIELD_FROM, 
					SipMessage.FIELD_FROM_FULL, 
					SipMessage.FIELD_TO, 
					"CASE WHEN "+SipMessage.FIELD_FROM+"='SELF' THEN "+SipMessage.FIELD_TO+" WHEN "+SipMessage.FIELD_FROM+"!='SELF' THEN "+SipMessage.FIELD_FROM+" END AS message_ordering",
					SipMessage.FIELD_BODY, 
					"MAX(" + SipMessage.FIELD_DATE + ") AS " + SipMessage.FIELD_DATE,
					"MIN(" + SipMessage.FIELD_READ + ") AS " + SipMessage.FIELD_READ,
					//SipMessage.FIELD_READ,
					"COUNT(" + SipMessage.FIELD_DATE + ") AS counter"
				}, 
				SipMessage.FIELD_TYPE+" in ("+SipMessage.MESSAGE_TYPE_INBOX+","+SipMessage.MESSAGE_TYPE_SENT+")", null, 
				"message_ordering", null, 
				SipMessage.FIELD_DATE+" DESC");
	}
	
	public Cursor getConversation(String remoteFrom) {
		return db.query(MESSAGES_TABLE_NAME, 
				new String[]{
					"ROWID AS _id",
					SipMessage.FIELD_FROM, 
					SipMessage.FIELD_BODY, 
					SipMessage.FIELD_DATE, 
					SipMessage.FIELD_MIME_TYPE,
					SipMessage.FIELD_TYPE,
					SipMessage.FIELD_STATUS,
					SipMessage.FIELD_FROM_FULL
				}, SipMessage.THREAD_SELECTION,
				new String[] {
					remoteFrom,
					remoteFrom
				}, 
				null, null, 
				SipMessage.FIELD_DATE+" ASC");
	}
	
	public boolean deleteConversation(String remoteFrom) {
		return db.delete(MESSAGES_TABLE_NAME, SipMessage.THREAD_SELECTION, new String[] {
					remoteFrom,
					remoteFrom
				}) > 0;
	}
	
	public boolean deleteAllConversation() {
		return db.delete(MESSAGES_TABLE_NAME, null, null) > 0;
	}
	
	public boolean markConversationAsRead(String remoteFrom) {
		ContentValues args = new ContentValues();
		args.put(SipMessage.FIELD_READ, true);
		return db.update(MESSAGES_TABLE_NAME, args,
				SipMessage.FIELD_FROM + "=?", new String[] {remoteFrom}) > 0;
	}

	public boolean updateMessageStatus(String sTo, String body, int messageType, int status, String reason) {
		ContentValues args = new ContentValues();
		args.put(SipMessage.FIELD_TYPE, messageType);
		args.put(SipMessage.FIELD_STATUS, status);
		if(status != SipCallSession.StatusCode.OK 
			&& status != SipCallSession.StatusCode.ACCEPTED ) {
			args.put(SipMessage.FIELD_BODY, body + " // " + reason);
		}
		return db.update(MESSAGES_TABLE_NAME, args,
				SipMessage.FIELD_TO + "=? AND "+
				SipMessage.FIELD_BODY+ "=? AND "+
				SipMessage.FIELD_TYPE+ "="+SipMessage.MESSAGE_TYPE_QUEUED, 
				new String[] {sTo, body}) > 0;
	}

}
