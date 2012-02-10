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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.provider.CallLog;
import android.text.TextUtils;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipMessage;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.db.DBAdapter.DatabaseHelper;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DBProvider extends ContentProvider {
	
	private DatabaseHelper mOpenHelper;
	private static final String UNKNOWN_URI_LOG = "Unknown URI ";
    
	// Ids for matcher
    private static final int ACCOUNTS = 1, ACCOUNTS_ID = 2;
    private static final int ACCOUNTS_STATUS = 3, ACCOUNTS_STATUS_ID = 4;
    private static final int CALLLOGS = 5, CALLLOGS_ID = 6;
    private static final int FILTERS = 7, FILTERS_ID = 8;
    private static final int MESSAGES = 9, MESSAGES_ID = 10;
    private static final int THREADS = 11, THREADS_ID = 12;
    
    /**
     * A UriMatcher instance
     */
    private static final UriMatcher URI_MATCHER;
    static {
    	 // Create and initialize URI matcher.
    	URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipProfile.ACCOUNTS_TABLE_NAME, ACCOUNTS);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipProfile.ACCOUNTS_TABLE_NAME + "/#", ACCOUNTS_ID);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipProfile.ACCOUNTS_STATUS_TABLE_NAME, ACCOUNTS_STATUS);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipProfile.ACCOUNTS_STATUS_TABLE_NAME + "/#", ACCOUNTS_STATUS_ID);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipManager.CALLLOGS_TABLE_NAME, CALLLOGS);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipManager.CALLLOGS_TABLE_NAME + "/#", CALLLOGS_ID);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipManager.FILTERS_TABLE_NAME, FILTERS);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipManager.FILTERS_TABLE_NAME + "/#", FILTERS_ID);
    	URI_MATCHER.addURI(SipManager.AUTHORITY, SipMessage.MESSAGES_TABLE_NAME, MESSAGES);
        URI_MATCHER.addURI(SipManager.AUTHORITY, SipMessage.MESSAGES_TABLE_NAME + "/#", MESSAGES_ID);
        URI_MATCHER.addURI(SipManager.AUTHORITY, SipMessage.THREAD_ALIAS, THREADS);
        URI_MATCHER.addURI(SipManager.AUTHORITY, SipMessage.THREAD_ALIAS + "/*", THREADS_ID);
    }
	

	public final static String[] ACCOUNT_FULL_PROJECTION = {
		SipProfile.FIELD_ID,
		// Application relative fields
		SipProfile.FIELD_ACTIVE, SipProfile.FIELD_WIZARD, SipProfile.FIELD_DISPLAY_NAME,

		// Here comes pjsua_acc_config fields
		SipProfile.FIELD_PRIORITY, SipProfile.FIELD_ACC_ID, SipProfile.FIELD_REG_URI, 
		SipProfile.FIELD_MWI_ENABLED, SipProfile.FIELD_PUBLISH_ENABLED, SipProfile.FIELD_REG_TIMEOUT, SipProfile.FIELD_KA_INTERVAL, 
		SipProfile.FIELD_PIDF_TUPLE_ID,
		SipProfile.FIELD_FORCE_CONTACT, SipProfile.FIELD_ALLOW_CONTACT_REWRITE, SipProfile.FIELD_CONTACT_REWRITE_METHOD, 
		SipProfile.FIELD_CONTACT_PARAMS, SipProfile.FIELD_CONTACT_URI_PARAMS,
		SipProfile.FIELD_TRANSPORT, SipProfile.FIELD_USE_SRTP, SipProfile.FIELD_USE_ZRTP,
		SipProfile.FIELD_REG_DELAY_BEFORE_REFRESH,
		
		// RTP config
	    SipProfile.FIELD_RTP_PORT, SipProfile.FIELD_RTP_PUBLIC_ADDR, SipProfile.FIELD_RTP_BOUND_ADDR,
	    SipProfile.FIELD_RTP_ENABLE_QOS, SipProfile.FIELD_RTP_QOS_DSCP,

		// Proxy infos
		SipProfile.FIELD_PROXY, SipProfile.FIELD_REG_USE_PROXY,

		// And now cred_info since for now only one cred info can be managed
		// In future release a credential table should be created
		SipProfile.FIELD_REALM, SipProfile.FIELD_SCHEME, SipProfile.FIELD_USERNAME, SipProfile.FIELD_DATATYPE,
		SipProfile.FIELD_DATA, 
		
		// CSipSimple specific
		SipProfile.FIELD_SIP_STACK, SipProfile.FIELD_VOICE_MAIL_NBR, 
		SipProfile.FIELD_TRY_CLEAN_REGISTERS, SipProfile.FIELD_ANDROID_GROUP,
		
		// RFC 5626
		SipProfile.FIELD_USE_RFC5626, SipProfile.FIELD_RFC5626_INSTANCE_ID, SipProfile.FIELD_RFC5626_REG_ID, 
		
		// Video
		SipProfile.FIELD_VID_IN_AUTO_SHOW, SipProfile.FIELD_VID_OUT_AUTO_TRANSMIT,
		
		
	};
	public final static Class<?>[] ACCOUNT_FULL_PROJECTION_TYPES = {
		Long.class,
		
		Integer.class, String.class, String.class,
		
		Integer.class, String.class, String.class,
		Boolean.class, Integer.class, Integer.class, Integer.class, 
		String.class,
		String.class, Integer.class, Integer.class,
		String.class, String.class,
		Integer.class, Integer.class, Integer.class,
		Integer.class,
		

        // RTP config
        Integer.class, String.class, String.class,
        Integer.class, Integer.class,
		
		// Proxy infos
		String.class, Integer.class,

        // Credentials
		String.class, String.class, String.class, Integer.class,
		String.class,
		
		// CSipSimple specific
		Integer.class, String.class,
		Integer.class, String.class,

        // RFC 5626
		Integer.class, String.class, String.class,
		
		// Video
		Integer.class, Integer.class
	};


	private static final String THIS_FILE = "DBProvider";

	// Map active account id (id for sql settings database) with SipProfileState that contains stack id and other status infos
	private final Map<Long, ContentValues> profilesStatus = new HashMap<Long, ContentValues>();
	

	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
            case ACCOUNTS:
                return SipProfile.ACCOUNT_CONTENT_TYPE;
            case ACCOUNTS_ID:
                return SipProfile.ACCOUNT_CONTENT_ITEM_TYPE;
            case ACCOUNTS_STATUS:
            	return SipProfile.ACCOUNT_STATUS_CONTENT_TYPE;
            case ACCOUNTS_STATUS_ID:
            	return SipProfile.ACCOUNT_STATUS_CONTENT_ITEM_TYPE;
            case CALLLOGS :
            	return SipManager.CALLLOG_CONTENT_TYPE;
            case CALLLOGS_ID :
            	return SipManager.CALLLOG_CONTENT_ITEM_TYPE;
            case FILTERS:
            	return SipManager.FILTER_CONTENT_TYPE;
            case FILTERS_ID:
            	return SipManager.FILTER_CONTENT_ITEM_TYPE;
            case MESSAGES:
                return SipMessage.MESSAGE_CONTENT_TYPE;
            case MESSAGES_ID:
                return SipMessage.MESSAGE_CONTENT_ITEM_TYPE;
            case THREADS:
                return SipMessage.MESSAGE_CONTENT_TYPE;
            case THREADS_ID:
                return SipMessage.MESSAGE_CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }
	}
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
        // Assumes that any failures will be reported by a thrown exception.
        return true;
	}
	

    private static final String MESSAGES_THREAD_SELECTION = "("+ SipMessage.FIELD_FROM+"=? AND "+
        SipMessage.FIELD_TYPE+" IN ("+
        Integer.toString(SipMessage.MESSAGE_TYPE_INBOX)
    +") )"
    + " OR " +
    "("+ SipMessage.FIELD_TO+"=? AND "+
        SipMessage.FIELD_TYPE+" IN ("+
         Integer.toString(SipMessage.MESSAGE_TYPE_QUEUED)+", "+
         Integer.toString(SipMessage.MESSAGE_TYPE_FAILED)+", "+
         Integer.toString(SipMessage.MESSAGE_TYPE_SENT)
    +") )";

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;
        int count = 0;
        int matched = URI_MATCHER.match(uri);
        Uri regUri = uri;
        
        switch (matched) {
            case ACCOUNTS:
                count = db.delete(SipProfile.ACCOUNTS_TABLE_NAME, where, whereArgs);
                break;
            case ACCOUNTS_ID:
            	finalWhere = concatenateWhere(SipProfile.FIELD_ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(SipProfile.ACCOUNTS_TABLE_NAME, finalWhere, whereArgs);
                break;
            case CALLLOGS:
                count = db.delete(SipManager.CALLLOGS_TABLE_NAME, where, whereArgs);
            	break;
            case CALLLOGS_ID:
            	finalWhere = concatenateWhere(CallLog.Calls._ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(SipManager.CALLLOGS_TABLE_NAME, finalWhere, whereArgs);
                break;
            case FILTERS:
            	count = db.delete(SipManager.FILTERS_TABLE_NAME, where, whereArgs);
            	break;
            case FILTERS_ID:
            	finalWhere = concatenateWhere(Filter._ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(SipManager.FILTERS_TABLE_NAME, finalWhere, whereArgs);
                break;
            case MESSAGES:
                count = db.delete(SipMessage.MESSAGES_TABLE_NAME, where, whereArgs);
                break;
            case MESSAGES_ID:
                finalWhere = concatenateWhere(SipMessage.FIELD_ID + " = " + ContentUris.parseId(uri), where);
                count = db.delete(SipMessage.MESSAGES_TABLE_NAME, finalWhere, whereArgs);
                break;
            case THREADS_ID:
                String from = uri.getLastPathSegment();
                if(!TextUtils.isEmpty(from)) {
                    count = db.delete(SipMessage.MESSAGES_TABLE_NAME, MESSAGES_THREAD_SELECTION, new String[] {
                            from, from
                    });
                }else {
                    count = 0;
                }
                regUri = SipMessage.MESSAGE_URI;
                break;
            case ACCOUNTS_STATUS:
            	synchronized (profilesStatus) {
        			profilesStatus.clear();
        		}
            	break;
            case ACCOUNTS_STATUS_ID:
            	long id = ContentUris.parseId(uri);
            	synchronized (profilesStatus) {
        			profilesStatus.remove(id);
        		}
            	break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }

        getContext().getContentResolver().notifyChange(regUri, null);

        if(matched == ACCOUNTS_ID) {
        	long rowId = ContentUris.parseId(uri);
        	if(rowId >= 0) {
        		broadcastAccountChange(rowId);
        	}
        }
        
		return count;
	}


	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		int matched = URI_MATCHER.match(uri);
    	String matchedTable = null;
    	Uri baseInsertedUri = null;
    	switch (matched) {
		case ACCOUNTS:
		case ACCOUNTS_ID:
			matchedTable = SipProfile.ACCOUNTS_TABLE_NAME;
			baseInsertedUri = SipProfile.ACCOUNT_ID_URI_BASE;
			break;
		case CALLLOGS:
		case CALLLOGS_ID:
			matchedTable = SipManager.CALLLOGS_TABLE_NAME;
			baseInsertedUri = SipManager.CALLLOG_ID_URI_BASE;
			break;
		case FILTERS:
		case FILTERS_ID:
			matchedTable = SipManager.FILTERS_TABLE_NAME;
			baseInsertedUri = SipManager.FILTER_ID_URI_BASE;
			break;
		case MESSAGES:
		case MESSAGES_ID:
		    matchedTable = SipMessage.MESSAGES_TABLE_NAME;
            baseInsertedUri = SipMessage.MESSAGE_ID_URI_BASE;
		    break;
		case ACCOUNTS_STATUS_ID:
			long id = ContentUris.parseId(uri);
			synchronized (profilesStatus){
				SipProfileState ps = new SipProfileState();
				if(profilesStatus.containsKey(id)) {
					ContentValues currentValues = profilesStatus.get(id);
					ps.createFromContentValue(currentValues);
				}
				ps.createFromContentValue(initialValues);
				ContentValues cv = ps.getAsContentValue();
				cv.put(SipProfileState.ACCOUNT_ID, id);
				profilesStatus.put(id, cv);
				Log.d(THIS_FILE, "Added "+cv);
			}
            getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		default:
			break;
		}
    	
        if ( matchedTable == null ) {
            throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }

        ContentValues values;

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(matchedTable, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId >= 0) {
        	// TODO : for inserted account register it here
        	
            Uri retUri = ContentUris.withAppendedId(baseInsertedUri, rowId);
            getContext().getContentResolver().notifyChange(retUri, null);
            
            if(matched == ACCOUNTS || matched == ACCOUNTS_ID) {
            	broadcastAccountChange(rowId);
            }
            if(matched == CALLLOGS || matched == CALLLOGS_ID) {
            	db.delete(SipManager.CALLLOGS_TABLE_NAME, CallLog.Calls._ID + " IN " +
            			"(SELECT "+CallLog.Calls._ID+" FROM "+SipManager.CALLLOGS_TABLE_NAME+" ORDER BY " + 
            				CallLog.Calls.DEFAULT_SORT_ORDER + " LIMIT -1 OFFSET 500)", null);
            }
            if(matched == ACCOUNTS_STATUS || matched == ACCOUNTS_STATUS_ID) {
                broadcastRegistrationChange(rowId);
            }
            
            return retUri;
        }
        
        throw new SQLException("Failed to insert row into " + uri);
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String finalSortOrder = sortOrder;
        String[] finalSelectionArgs = selectionArgs;
        String finalGrouping = null;
        String finalHaving = null;
        int type = URI_MATCHER.match(uri);
        
        Uri regUri = uri;
        
        int remoteUid = Binder.getCallingUid();
        int selfUid = android.os.Process.myUid();
        if(remoteUid != selfUid) {
	        if (type == ACCOUNTS || type == ACCOUNTS_ID) {
				for(String proj : projection) {
	        		if(proj.toLowerCase().contains(SipProfile.FIELD_DATA) || proj.toLowerCase().contains("*")) {
	        			throw new SecurityException("Password not readable from external apps");
	        		}
	        	}
			}
        }

    	Cursor c;
    	long id;
        switch (type) {
            case ACCOUNTS:
                qb.setTables(SipProfile.ACCOUNTS_TABLE_NAME);
                if(sortOrder == null) {
                	finalSortOrder = SipProfile.FIELD_PRIORITY + " DESC";
                }
                break;
            case ACCOUNTS_ID:
                qb.setTables(SipProfile.ACCOUNTS_TABLE_NAME);
                qb.appendWhere(SipProfile.FIELD_ID + "=?");
                finalSelectionArgs = appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case CALLLOGS:
                qb.setTables(SipManager.CALLLOGS_TABLE_NAME);
                if(sortOrder == null) {
                	finalSortOrder = CallLog.Calls.DATE + " DESC";
                }
                break;
            case CALLLOGS_ID:
                qb.setTables(SipManager.CALLLOGS_TABLE_NAME);
                qb.appendWhere(CallLog.Calls._ID + "=?");
                finalSelectionArgs = appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case FILTERS:
                qb.setTables(SipManager.FILTERS_TABLE_NAME);
                if(sortOrder == null) {
                	finalSortOrder = Filter.DEFAULT_ORDER;
                }
                break;
            case FILTERS_ID:
                qb.setTables(SipManager.FILTERS_TABLE_NAME);
                qb.appendWhere(Filter._ID + "=?");
                finalSelectionArgs = appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case MESSAGES:
                qb.setTables(SipMessage.MESSAGES_TABLE_NAME);
                if(sortOrder == null) {
                    finalSortOrder = SipMessage.FIELD_DATE + " DESC";
                }
                break;
            case MESSAGES_ID:
                qb.setTables(SipMessage.MESSAGES_TABLE_NAME);
                qb.appendWhere(SipMessage.FIELD_ID + "=?");
                finalSelectionArgs = appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case THREADS:
                qb.setTables(SipMessage.MESSAGES_TABLE_NAME);
                if(sortOrder == null) {
                    finalSortOrder = SipMessage.FIELD_DATE + " DESC";
                }
                projection = new String[]{
                    "ROWID AS _id",
                    SipMessage.FIELD_FROM, 
                    SipMessage.FIELD_FROM_FULL, 
                    SipMessage.FIELD_TO, 
                    "CASE " + 
                            "WHEN " + SipMessage.FIELD_FROM + "='SELF' THEN "
                                + SipMessage.FIELD_TO + 
                            " WHEN " + SipMessage.FIELD_FROM + "!='SELF' THEN " 
                                + SipMessage.FIELD_FROM
                    + " END AS message_ordering",
                    SipMessage.FIELD_BODY, 
                    "MAX(" + SipMessage.FIELD_DATE + ") AS " + SipMessage.FIELD_DATE,
                    "MIN(" + SipMessage.FIELD_READ + ") AS " + SipMessage.FIELD_READ,
                    //SipMessage.FIELD_READ,
                    "COUNT(" + SipMessage.FIELD_DATE + ") AS counter"
                };
                qb.appendWhere(SipMessage.FIELD_TYPE + " in (" + SipMessage.MESSAGE_TYPE_INBOX
                        + "," + SipMessage.MESSAGE_TYPE_SENT + ")");
                finalGrouping = "message_ordering";
                regUri = SipMessage.MESSAGE_URI;
                break;
            case THREADS_ID:
                qb.setTables(SipMessage.MESSAGES_TABLE_NAME);
                if(sortOrder == null) {
                    finalSortOrder = SipMessage.FIELD_DATE + " DESC";
                }
                projection = new String[]{
                        "ROWID AS _id",
                        SipMessage.FIELD_FROM, 
                        SipMessage.FIELD_TO, 
                        SipMessage.FIELD_BODY, 
                        SipMessage.FIELD_DATE, 
                        SipMessage.FIELD_MIME_TYPE,
                        SipMessage.FIELD_TYPE,
                        SipMessage.FIELD_STATUS,
                        SipMessage.FIELD_FROM_FULL
                    };
                qb.appendWhere(MESSAGES_THREAD_SELECTION);
                String from = uri.getLastPathSegment();
                finalSelectionArgs = appendSelectionArgs(selectionArgs, new String[] { from, from });
                regUri = SipMessage.MESSAGE_URI;
                break;
            case ACCOUNTS_STATUS:
            	synchronized (profilesStatus) {
            		ContentValues[] cvs = new ContentValues[profilesStatus.size()];
            		int i = 0;
            		for(ContentValues  ps : profilesStatus.values()) {
            			cvs[i] = ps;
            			i++;
            		}
            		c = getCursor(cvs);
				}
            	if(c != null) {
            		c.setNotificationUri(getContext().getContentResolver(), uri);
            	}
                return c;
            case ACCOUNTS_STATUS_ID:
            	id = ContentUris.parseId(uri);
            	synchronized (profilesStatus) {
            		ContentValues cv = profilesStatus.get(id);
            		if(cv == null) {
            			return null;
            		}
            		c = getCursor(new ContentValues[] {cv});
				}
                c.setNotificationUri(getContext().getContentResolver(), uri);
                return c;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        c = qb.query(db, projection, selection, finalSelectionArgs,
                finalGrouping, finalHaving, finalSortOrder);

        c.setNotificationUri(getContext().getContentResolver(), regUri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;
        int matched = URI_MATCHER.match(uri);
        
        switch (matched) {
            case ACCOUNTS:
                count = db.update(SipProfile.ACCOUNTS_TABLE_NAME, values, where, whereArgs);
                break;
            case ACCOUNTS_ID:
                finalWhere = concatenateWhere(SipProfile.FIELD_ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(SipProfile.ACCOUNTS_TABLE_NAME, values, finalWhere, whereArgs);
                break;
            case CALLLOGS:
                count = db.update(SipManager.CALLLOGS_TABLE_NAME, values, where, whereArgs);
                break;
            case CALLLOGS_ID:
                finalWhere = concatenateWhere(CallLog.Calls._ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(SipManager.CALLLOGS_TABLE_NAME, values, finalWhere, whereArgs);
                break;
            case FILTERS:
                count = db.update(SipManager.FILTERS_TABLE_NAME, values, where, whereArgs);
                break;
            case FILTERS_ID:
                finalWhere = concatenateWhere(Filter._ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(SipManager.FILTERS_TABLE_NAME, values, finalWhere, whereArgs);
                break;
            case MESSAGES:
                count = db.update(SipMessage.MESSAGES_TABLE_NAME, values, where, whereArgs);
                break;
            case MESSAGES_ID:
                finalWhere = concatenateWhere(SipMessage.FIELD_ID + " = " + ContentUris.parseId(uri), where);
                count = db.update(SipMessage.MESSAGES_TABLE_NAME, values, where, whereArgs);
                break;
            case ACCOUNTS_STATUS_ID:
    			long id = ContentUris.parseId(uri);
    			synchronized (profilesStatus){
    				SipProfileState ps = new SipProfileState();
    				if(profilesStatus.containsKey(id)) {
    					ContentValues currentValues = profilesStatus.get(id);
    					ps.createFromContentValue(currentValues);
    				}
    				ps.createFromContentValue(values);
    				ContentValues cv = ps.getAsContentValue();
    				cv.put(SipProfileState.ACCOUNT_ID, id);
    				profilesStatus.put(id, cv);
    				Log.d(THIS_FILE, "Updated "+cv);
    			}
    			count = 1;
    			break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        long rowId = -1;
        if (matched == ACCOUNTS_ID || matched == ACCOUNTS_STATUS_ID) {
            rowId = ContentUris.parseId(uri);
        }
        if (rowId >= 0) {

            if (matched == ACCOUNTS_ID) {
                broadcastAccountChange(rowId);
            } else if (matched == ACCOUNTS_STATUS_ID) {
                broadcastRegistrationChange(rowId);
            }
        }
        
	
        return count;
	}
	
	
	
	// Internal helpers

    // Utilities from source to be backward compatible
	private static String concatenateWhere(String a, String b) {
		 if (TextUtils.isEmpty(a)) {
			 return b;
		 }
		 if (TextUtils.isEmpty(b)) {
			 return a;
		 }
			 
		 return "(" + a + ") AND (" + b + ")";
	}
	
	public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
		if (originalValues == null || originalValues.length == 0) {
			return newValues;
		}
		String[] result = new String[originalValues.length + newValues.length];
		System.arraycopy(originalValues, 0, result, 0, originalValues.length);
		System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
		return result;
	}
	
	/**
	 * Build a {@link Cursor} with a single row that contains all values
	 * provided through the given {@link ContentValues}.
	 */
	private Cursor getCursor(ContentValues[] contentValues) {
		if(contentValues.length > 0) {
	        final Set<Entry<String, Object>> valueSet = contentValues[0].valueSet();
	        int colSize = valueSet.size();
	        final String[] keys = new String[colSize];
	
	        int i = 0;
	        for (Entry<String, Object> entry : valueSet) {
	            keys[i] = entry.getKey();
	            i++;
	        }
	
	        final MatrixCursor cursor = new MatrixCursor(keys);
	        for (ContentValues cv : contentValues) {
		        final Object[] values = new Object[colSize];
		        i = 0;
		        for (Entry<String, Object> entry : cv.valueSet()) {
		            values[i] = entry.getValue();
		            i++;
		        }
	            cursor.addRow(values);
	        }
	        return cursor;
		}
		return null;
    }
	
	/**
	 * Broadcast the fact that account config has changed
	 * @param accountId
	 */
	private void broadcastAccountChange(long accountId) {
		Intent publishIntent = new Intent(SipManager.ACTION_SIP_ACCOUNT_CHANGED);
		publishIntent.putExtra(SipProfile.FIELD_ID, accountId);
		getContext().sendBroadcast(publishIntent);
	}
	
	/**
	 * Broadcast the fact that registration / adding status changed
	 * @param accountId the id of the account
	 */
	private void broadcastRegistrationChange(long accountId) {
        Intent publishIntent = new Intent(SipManager.ACTION_SIP_REGISTRATION_CHANGED);
        publishIntent.putExtra(SipProfile.FIELD_ID, accountId);
        getContext().sendBroadcast(publishIntent);
	    
	}
}
