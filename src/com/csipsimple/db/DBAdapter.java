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
 */
package com.csipsimple.db;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.CallLog;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;

public class DBAdapter {
	private final static String THIS_FILE = "SIP ACC_DB";

	private final Context context;
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;
	
	public DBAdapter(Context aContext) {
		context = aContext;
		databaseHelper = new DatabaseHelper(context);
	}

	public static class DatabaseHelper extends SQLiteOpenHelper {
		
		private static final int DATABASE_VERSION = 32;

		// Creation sql command
		private static final String TABLE_ACCOUNT_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ SipProfile.ACCOUNTS_TABLE_NAME
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
				+ SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH	+ " INTEGER," 
				
				+ SipProfile.FIELD_TRY_CLEAN_REGISTERS	+ " INTEGER,"
				
				+ SipProfile.FIELD_USE_RFC5626          + " INTEGER DEFAULT 1,"
				+ SipProfile.FIELD_RFC5626_INSTANCE_ID  + " TEXT,"
				+ SipProfile.FIELD_RFC5626_REG_ID       + " TEXT,"
				
                + SipProfile.FIELD_VID_IN_AUTO_SHOW          + " INTEGER DEFAULT -1,"
                + SipProfile.FIELD_VID_OUT_AUTO_TRANSMIT     + " INTEGER DEFAULT -1,"
                
                + SipProfile.FIELD_RTP_PORT                  + " INTEGER DEFAULT -1,"
                + SipProfile.FIELD_RTP_ENABLE_QOS            + " INTEGER DEFAULT -1,"
                + SipProfile.FIELD_RTP_QOS_DSCP              + " INTEGER DEFAULT -1,"
                + SipProfile.FIELD_RTP_BOUND_ADDR            + " TEXT,"
                + SipProfile.FIELD_RTP_PUBLIC_ADDR           + " TEXT,"
                + SipProfile.FIELD_ANDROID_GROUP             + " TEXT"
				
			+ ");";
		
		private final static String TABLE_CALLLOGS_CREATE = "CREATE TABLE IF NOT EXISTS "
			+ SipManager.CALLLOGS_TABLE_NAME
			+ " ("
				+ CallLog.Calls._ID					+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ CallLog.Calls.CACHED_NAME			+ " TEXT,"
				+ CallLog.Calls.CACHED_NUMBER_LABEL	+ " TEXT,"
				+ CallLog.Calls.CACHED_NUMBER_TYPE	+ " INTEGER,"
				+ CallLog.Calls.DATE				+ " INTEGER,"
				+ CallLog.Calls.DURATION			+ " INTEGER,"
				+ CallLog.Calls.NEW					+ " INTEGER,"
				+ CallLog.Calls.NUMBER				+ " TEXT,"
				+ CallLog.Calls.TYPE				+ " INTEGER,"
		        + SipManager.CALLLOG_PROFILE_ID_FIELD     + " INTEGER,"
		        + SipManager.CALLLOG_STATUS_CODE_FIELD    + " INTEGER,"
		        + SipManager.CALLLOG_STATUS_TEXT_FIELD    + " TEXT"
			+");";
		
		private static final String TABLE_FILTERS_CREATE =  "CREATE TABLE IF NOT EXISTS "
			+ SipManager.FILTERS_TABLE_NAME
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
			+ SipMessage.MESSAGES_TABLE_NAME
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
		
		
		
		DatabaseHelper(Context context) {
			super(context, SipManager.AUTHORITY, null, DATABASE_VERSION);
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
				db.execSQL("DROP TABLE IF EXISTS " + SipProfile.ACCOUNTS_TABLE_NAME);
			}
			if(oldVersion < 5) {
				try {
					db.execSQL("ALTER TABLE "+SipProfile.ACCOUNTS_TABLE_NAME+" ADD "+SipProfile.FIELD_KA_INTERVAL+" INTEGER");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 6) {
				db.execSQL("DROP TABLE IF EXISTS "+SipManager.FILTERS_TABLE_NAME);
			}
			if(oldVersion < 10) {
				try {
					db.execSQL("ALTER TABLE "+SipProfile.ACCOUNTS_TABLE_NAME+" ADD "+
							SipProfile.FIELD_ALLOW_CONTACT_REWRITE + " INTEGER");
					db.execSQL("ALTER TABLE "+SipProfile.ACCOUNTS_TABLE_NAME+" ADD "+
							SipProfile.FIELD_CONTACT_REWRITE_METHOD + " INTEGER");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 13) {
				try {
					db.execSQL("ALTER TABLE " + SipProfile.ACCOUNTS_TABLE_NAME + " ADD " + SipProfile.FIELD_TRANSPORT + " INTEGER");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_UDP + " WHERE prevent_tcp=1");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_TCP + " WHERE use_tcp=1");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_AUTO + " WHERE use_tcp=0 AND prevent_tcp=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
				
			}
			if(oldVersion < 17) {
				try {
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_KA_INTERVAL + "=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 18) {
				try {
					//As many users are crying... remove auto transport and force udp
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRANSPORT + "="+SipProfile.TRANSPORT_UDP +" WHERE "+ SipProfile.FIELD_TRANSPORT + "=" + SipProfile.TRANSPORT_AUTO);
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 22) {
				try {
					//Add use proxy row
					db.execSQL("ALTER TABLE " + SipProfile.ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_REG_USE_PROXY + " INTEGER");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_REG_USE_PROXY + "=3");
					//Add stack field
					db.execSQL("ALTER TABLE " + SipProfile.ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_SIP_STACK + " INTEGER");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_SIP_STACK + "=0");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 23) {
				try {
					//Add use zrtp row
					db.execSQL("ALTER TABLE " + SipProfile.ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_USE_ZRTP + " INTEGER");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_USE_ZRTP + "=0");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 24) {
				try {
					//Add voice mail row
					db.execSQL("ALTER TABLE " + SipProfile.ACCOUNTS_TABLE_NAME + " ADD "+
							SipProfile.FIELD_VOICE_MAIL_NBR + " TEXT");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_VOICE_MAIL_NBR + "=''");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 25) {
				try {
					//Add voice mail row
					db.execSQL("ALTER TABLE " + SipMessage.MESSAGES_TABLE_NAME + " ADD "+
							SipMessage.FIELD_FROM_FULL + " TEXT");
					db.execSQL("UPDATE " + SipMessage.MESSAGES_TABLE_NAME + " SET " + SipMessage.FIELD_FROM_FULL + "="+ SipMessage.FIELD_FROM);
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 26) {
				try {
					//Add reg delay before refresh row
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH, "INTEGER DEFAULT -1");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH + "=-1");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 27) {
				try {
					//Add reg delay before refresh row
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_TRY_CLEAN_REGISTERS, "INTEGER DEFAULT 0");
					db.execSQL("UPDATE " + SipProfile.ACCOUNTS_TABLE_NAME + " SET " + SipProfile.FIELD_TRY_CLEAN_REGISTERS + "=0");
					Log.d(THIS_FILE, "Upgrade done");
				}catch(SQLiteException e) {
					Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
				}
			}
			if(oldVersion < 28) {
                try {
                    // Add call log profile id
                    addColumn(db, SipManager.CALLLOGS_TABLE_NAME, SipManager.CALLLOG_PROFILE_ID_FIELD, "INTEGER");
                    // Add call log status code
                    addColumn(db, SipManager.CALLLOGS_TABLE_NAME, SipManager.CALLLOG_STATUS_CODE_FIELD, "INTEGER");
                    db.execSQL("UPDATE " + SipManager.CALLLOGS_TABLE_NAME + " SET " + SipManager.CALLLOG_STATUS_CODE_FIELD + "=200");
                    // Add call log status text
                    addColumn(db, SipManager.CALLLOGS_TABLE_NAME, SipManager.CALLLOG_STATUS_TEXT_FIELD, "TEXT");
                    Log.d(THIS_FILE, "Upgrade done");
                }catch(SQLiteException e) {
                    Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
                }
            }
			if(oldVersion < 30) {
			    try {
			      //Add reg delay before refresh row
			        addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_USE_RFC5626, "INTEGER DEFAULT 1");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RFC5626_INSTANCE_ID, "TEXT");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RFC5626_REG_ID, "TEXT");

                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_VID_IN_AUTO_SHOW, "INTEGER DEFAULT -1");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_VID_OUT_AUTO_TRANSMIT, "INTEGER DEFAULT -1");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RTP_PORT, "INTEGER DEFAULT -1");
                    

                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RTP_ENABLE_QOS, "INTEGER DEFAULT -1");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RTP_QOS_DSCP, "INTEGER DEFAULT -1");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RTP_PUBLIC_ADDR, "TEXT");
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_RTP_BOUND_ADDR, "TEXT");
			        
                    Log.d(THIS_FILE, "Upgrade done");
                }catch(SQLiteException e) {
                    Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
                }
			}
			// Nightly build bug -- restore mime type field to mime_type
			if(oldVersion == 30) {
			    try {
			        addColumn(db, SipMessage.MESSAGES_TABLE_NAME, SipMessage.FIELD_MIME_TYPE, "TEXT");
			        db.execSQL("UPDATE " + SipMessage.MESSAGES_TABLE_NAME + " SET " + SipMessage.FIELD_MIME_TYPE + "='text/plain'");
			    }catch(SQLiteException e) {
                    Log.e(THIS_FILE, "Upgrade fail... maybe a crappy rom...", e);
                }
			}

            if(oldVersion < 32) {
                try {
                    //Add android group for buddy list
                    addColumn(db, SipProfile.ACCOUNTS_TABLE_NAME, SipProfile.FIELD_ANDROID_GROUP, "TEXT");
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
	
	private static void addColumn(SQLiteDatabase db, String table, String field, String type) {
	    db.execSQL("ALTER TABLE " + table + " ADD "+ field + " " + type);
	}
	
	
	// --------
	// Filters
	// --------
	
	public Cursor getFiltersForAccount(long account_id) {
		//Log.d(THIS_FILE, "Get filters for account "+account_id);
		return db.query(SipManager.FILTERS_TABLE_NAME, Filter.FULL_PROJ, 
				Filter.FIELD_ACCOUNT+"=?", new String[]{Long.toString(account_id)}, 
				null, null, Filter.DEFAULT_ORDER);
	}
	
	public int getCountFiltersForAccount(int account_id) {
		Cursor c = db.rawQuery("SELECT COUNT(" + Filter.FIELD_ACCOUNT + ") FROM " + 
				SipManager.FILTERS_TABLE_NAME + " WHERE " + Filter.FIELD_ACCOUNT + "=?;",
				new String[]{Integer.toString(account_id)});
		int numRows = 0;
		if(c.getCount() > 0){
			c.moveToFirst();
			numRows = c.getInt(0);
			
		}
		c.close();
		return numRows;
	}

}
