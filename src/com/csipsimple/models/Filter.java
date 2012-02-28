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
package com.csipsimple.models;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.text.TextUtils;
import android.util.SparseArray;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.utils.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Filter {
	public static final String _ID = "_id";
	public static final String FIELD_PRIORITY = "priority";
	public static final String FIELD_ACCOUNT = "account";
	public static final String FIELD_MATCHES = "matches";
	public static final String FIELD_REPLACE = "replace";
	public static final String FIELD_ACTION = "action";
	
	public static final int ACTION_CAN_CALL = 0;
	public static final int ACTION_CANT_CALL = 1;
	public static final int ACTION_REPLACE = 2;
	public static final int ACTION_DIRECTLY_CALL = 3;
	public static final int ACTION_AUTO_ANSWER = 4;
	
	public static final int MATCHER_STARTS = 0;
	public static final int MATCHER_HAS_N_DIGIT = 1;
	public static final int MATCHER_HAS_MORE_N_DIGIT = 2;
	public static final int MATCHER_IS_EXACTLY = 3;
	public static final int MATCHER_REGEXP = 4;
	public static final int MATCHER_ENDS = 5;
	public static final int MATCHER_ALL = 6;
	public static final int MATCHER_CONTAINS = 7;
	
	public static final int REPLACE_PREFIX = 0;
	public static final int REPLACE_MATCH_BY = 1;
	public static final int REPLACE_ALL_BY = 2;
	public static final int REPLACE_REGEXP = 3;
	public static final int REPLACE_SUFFIX = 4;
	
	
	public static final String[] FULL_PROJ = {
		_ID,
		FIELD_PRIORITY,
		FIELD_MATCHES,
		FIELD_REPLACE,
		FIELD_ACTION
	};
	
	public static final Class<?>[]  FULL_PROJ_TYPES = {
		Integer.class,
		Integer.class,
		String.class,
		String.class,
		Integer.class
	};
	
	
	public static final String DEFAULT_ORDER = FIELD_PRIORITY+" asc"; //TODO : should be a os constant... just find it
	private static final String THIS_FILE = "Filter";
	
	public Integer id;
	public Integer priority;
	public Integer account;
	public String matchPattern;
	public Integer matchType;
	public String replacePattern;
	public Integer action;
	
	public Filter() {
		// Nothing to do
	}
	
	public Filter(Cursor c) {
		super();
		createFromDb(c);
	}

	public void createFromDb(Cursor c) {
		ContentValues args = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, args);
		
		createFromContentValue(args);
	}

	public void createFromContentValue(ContentValues args) {
		Integer tmp_i;
		String tmp_s;
		
		tmp_i = args.getAsInteger(_ID);
		if (tmp_i != null) {
			id = tmp_i;
		}
		tmp_i = args.getAsInteger(FIELD_PRIORITY);
		if (tmp_i != null) {
			priority = tmp_i;
		}
		tmp_i = args.getAsInteger(FIELD_ACTION);
		if (tmp_i != null) {
			action = tmp_i;
		}
		
		
		tmp_s = args.getAsString(FIELD_MATCHES);
		if (tmp_s != null) {
			matchPattern = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_REPLACE);
		if (tmp_s != null) {
			replacePattern = tmp_s;
		}
		
		tmp_i = args.getAsInteger(FIELD_ACCOUNT);
		if(tmp_i != null) {
			account = tmp_i;
		}
	}

	public ContentValues getDbContentValues() {
		ContentValues args = new ContentValues();
		
		if(id != null){
			args.put(_ID, id);
		}
		args.put(FIELD_ACCOUNT, account);
		args.put(FIELD_MATCHES, matchPattern);
		args.put(FIELD_REPLACE, replacePattern);
		args.put(FIELD_ACTION, action);
		args.put(FIELD_PRIORITY, priority);
		return args;
	}



	public String getRepresentation(Context context) {
		String[] matches_array = context.getResources().getStringArray(R.array.filters_type);
		String[] replace_array = context.getResources().getStringArray(R.array.replace_type);
		RegExpRepresentation m = getRepresentationForMatcher();
		StringBuffer reprBuf = new StringBuffer();
		reprBuf.append(matches_array[getPositionForMatcher(m.type)]);
		reprBuf.append(' ');
		reprBuf.append(m.fieldContent);
		if(!TextUtils.isEmpty(replacePattern) && action == ACTION_REPLACE) {
			m = getRepresentationForReplace();
			reprBuf.append('\n');
			reprBuf.append(replace_array[getPositionForReplace(m.type)]);
			reprBuf.append(' ');
			reprBuf.append(m.fieldContent);
		}
		return reprBuf.toString();
	}
	
	private void logInvalidPattern(PatternSyntaxException e) {
		Log.e(THIS_FILE, "Invalid pattern ", e);
	}
	
	public boolean canCall(String number) {
		//Log.d(THIS_FILE, "Check if filter is valid for "+number+" >> "+action+" and "+matchPattern);
		if(action == ACTION_CANT_CALL) {
			try {
				return !Pattern.matches(matchPattern, number);
			}catch(PatternSyntaxException e) {
				logInvalidPattern(e);
			}
			
		}
		return true;
	}
	
	public boolean mustCall(String number) {
		if(action == ACTION_DIRECTLY_CALL) {
			try {
				return Pattern.matches(matchPattern, number);
			}catch(PatternSyntaxException e) {
				logInvalidPattern(e);
			}
			
		}
		return false;
	}
	
	public boolean stopProcessing(String number) {
		//Log.d(THIS_FILE, "Should stop processing "+number+" ? ");
		if(action == ACTION_CAN_CALL || action == ACTION_DIRECTLY_CALL) {
			try {
				return Pattern.matches(matchPattern, number);
			}catch(PatternSyntaxException e) {
				logInvalidPattern(e);
			}
		}
		//Log.d(THIS_FILE, "Response : false");
		return false;
	}
	
	public String rewrite(String number) {
		if(action == ACTION_REPLACE) {
			try {
				Pattern pattern = Pattern.compile(matchPattern);
				Matcher matcher = pattern.matcher(number);
				return matcher.replaceAll(replacePattern); 
			}catch(PatternSyntaxException e) {
				logInvalidPattern(e);
			}catch(ArrayIndexOutOfBoundsException e) {
				Log.e(THIS_FILE, "Out of bounds ", e);
			}
		}
		return number;
	}

	public boolean autoAnswer(String number) {
		if(action == ACTION_AUTO_ANSWER) {
			try {
				//TODO : get contact part
				return Pattern.matches(matchPattern, number);
			}catch(PatternSyntaxException e) {
				logInvalidPattern(e);
			}
		}
		return false;
	}
	
	
	//Utilities functions
	private static int getForPosition(SparseArray<Integer> positions, Integer key) {
		return positions.get(key);
	}
	
	private static int getPositionFor(SparseArray<Integer> positions, Integer value) {
		if(value != null) {
		    int pos = positions.indexOfValue(value);
		    if(pos >= 0) {
		        return pos;
		    }
		}
		return 0;
	}
	

	/**
	 * Available actions
	 */
	private final static SparseArray<Integer> FILTER_ACTION_POS = new SparseArray<Integer>();
	static {
		FILTER_ACTION_POS.put(0, ACTION_CANT_CALL);
		FILTER_ACTION_POS.put(1, ACTION_REPLACE);
		FILTER_ACTION_POS.put(2, ACTION_CAN_CALL);
		FILTER_ACTION_POS.put(3, ACTION_DIRECTLY_CALL);
		FILTER_ACTION_POS.put(4, ACTION_AUTO_ANSWER);
	};
	
	public static int getActionForPosition(Integer selectedItemPosition) {
		return getForPosition(FILTER_ACTION_POS, selectedItemPosition);
	}

	public static int getPositionForAction(Integer selectedAction) {
		return getPositionFor(FILTER_ACTION_POS, selectedAction);
	}
	
	/**
	 * Available matches patterns
	 */
	private final static SparseArray <Integer> MATCHER_TYPE_POS = new SparseArray<Integer>();
	
	static {
		MATCHER_TYPE_POS.put(0, MATCHER_STARTS);
		MATCHER_TYPE_POS.put(1, MATCHER_ENDS);
		MATCHER_TYPE_POS.put(2, MATCHER_CONTAINS);
		MATCHER_TYPE_POS.put(3, MATCHER_ALL);
		MATCHER_TYPE_POS.put(4, MATCHER_HAS_N_DIGIT);
		MATCHER_TYPE_POS.put(5, MATCHER_HAS_MORE_N_DIGIT);
		MATCHER_TYPE_POS.put(6, MATCHER_IS_EXACTLY);
		MATCHER_TYPE_POS.put(7, MATCHER_REGEXP);
	};
	
	public static int getMatcherForPosition(Integer selectedItemPosition) {
		return getForPosition(MATCHER_TYPE_POS, selectedItemPosition);
	}

	public static int getPositionForMatcher(Integer selectedAction) {
		return getPositionFor(MATCHER_TYPE_POS, selectedAction);
	}
	
	private final static SparseArray<Integer> REPLACE_TYPE_POS = new SparseArray<Integer>();
	static {
		REPLACE_TYPE_POS.put(0, REPLACE_PREFIX);
		REPLACE_TYPE_POS.put(1, REPLACE_SUFFIX);
		REPLACE_TYPE_POS.put(2, REPLACE_MATCH_BY);
		REPLACE_TYPE_POS.put(3, REPLACE_ALL_BY);
		REPLACE_TYPE_POS.put(4, REPLACE_REGEXP);
	};
	
	public static int getReplaceForPosition(Integer selectedItemPosition) {
		return getForPosition(REPLACE_TYPE_POS, selectedItemPosition);
	}

	public static int getPositionForReplace(Integer selectedAction) {
		return getPositionFor(REPLACE_TYPE_POS, selectedAction);
	}
	
	
	/**
	 * Represent a typed regexp
	 * Utility for visualisation of regexp (typed, for example start with, number of digit etc etc) 
	 * @author r3gis3r
	 *
	 */
	public static final class RegExpRepresentation {
		public Integer type;
		public String fieldContent;
	}
	
	/**
	 * Set matches field according to a RegExpRepresentation (for UI display)
	 * @param representation the regexp representation
	 */
	public void setMatcherRepresentation(RegExpRepresentation representation) {
	    matchType = representation.type;
		switch(representation.type) {
		case MATCHER_STARTS:
			matchPattern = "^"+Pattern.quote(representation.fieldContent)+"(.*)$";
			break;
		case MATCHER_ENDS:
			matchPattern = "^(.*)"+Pattern.quote(representation.fieldContent)+"$";
			break;
        case MATCHER_CONTAINS:
            matchPattern = "^(.*)"+Pattern.quote(representation.fieldContent)+"(.*)$";
            break;
		case MATCHER_ALL:
			matchPattern = "^(.*)$";
			break;
		case MATCHER_HAS_N_DIGIT:
			//TODO ... we should probably test the fieldContent type to ensure it's well digits...
			matchPattern = "^(\\d{"+representation.fieldContent+"})$";
			break;
		case MATCHER_HAS_MORE_N_DIGIT:
			//TODO ... we should probably test the fieldContent type to ensure it's well digits...
			matchPattern = "^(\\d{"+representation.fieldContent+",})$";
			break;
		case MATCHER_IS_EXACTLY:
			matchPattern = "^("+Pattern.quote(representation.fieldContent)+")$";
			break;
		case MATCHER_REGEXP:
		default:
		    matchType = MATCHER_REGEXP;        // In case hit default:
			matchPattern = representation.fieldContent;
			break;
		}
	}
	
	/**
	 * Get the representation for current matcher
	 * @return RegExpReprestation object with type of matcher and content for matcher 
	 * (content that should be shown in a text field for user)
	 */
	public RegExpRepresentation getRepresentationForMatcher() {
		RegExpRepresentation repr = new RegExpRepresentation();
		repr.type = matchType =  MATCHER_REGEXP;
		if(matchPattern == null) {
			repr.type = MATCHER_STARTS;
			repr.fieldContent = "";
			return repr;
		}else {
			repr.fieldContent = matchPattern;
			if( TextUtils.isEmpty(repr.fieldContent) ) {
				repr.type = matchType = MATCHER_STARTS;
				return repr;
			}
		}
		
		Matcher matcher = null;
		
		//Well... here we are... Some awful regexp matcher to test a regexp... Isn't it nice?
		matcher = Pattern.compile("^\\^\\\\Q(.+)\\\\E\\(\\.\\*\\)\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_STARTS;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		matcher = Pattern.compile("^\\^\\(\\.\\*\\)\\\\Q(.+)\\\\E\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_ENDS;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
        matcher = Pattern.compile("^\\^\\(\\.\\*\\)\\\\Q(.+)\\\\E\\(\\.\\*\\)\\$$").matcher(matchPattern);
        if(matcher.matches()) {
            repr.type = matchType = MATCHER_CONTAINS;
            repr.fieldContent = matcher.group(1);
            return repr;
        }
		
		matcher = Pattern.compile("^\\^\\(\\.\\*\\)\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_ALL;
			repr.fieldContent = "";
			return repr;
		}
		
		matcher = Pattern.compile("^\\^\\(\\\\d\\{([0-9]+)\\}\\)\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_HAS_N_DIGIT;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		matcher = Pattern.compile("^\\^\\(\\\\d\\{([0-9]+),\\}\\)\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_HAS_MORE_N_DIGIT;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		matcher = Pattern.compile("^\\^\\(\\\\Q(.+)\\\\E\\)\\$$").matcher(matchPattern);
		if(matcher.matches()) {
			repr.type = matchType = MATCHER_IS_EXACTLY;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		
		return repr;
	}
	
	
	public void setReplaceRepresentation(RegExpRepresentation representation){
		switch(representation.type) {
		case REPLACE_PREFIX:
			replacePattern = representation.fieldContent+"$0";
			break;
		case REPLACE_SUFFIX:
			replacePattern = "$0"+representation.fieldContent;
			break;
		case REPLACE_MATCH_BY:
		    switch (matchType)
		    {
		        case MATCHER_STARTS:
		            replacePattern = representation.fieldContent+"$1";
		            break;
                case MATCHER_ENDS:
                    replacePattern = "$1"+representation.fieldContent;
                    break;
                case MATCHER_CONTAINS:
                    replacePattern = "$1"+representation.fieldContent+"$2";
                    break;
                default:
                    // Other types match the entire input
                    replacePattern = representation.fieldContent;
                    break;
		    }
			break;
		case REPLACE_ALL_BY:
			//If $ is inside... well, next time will be considered as a regexp
			replacePattern = representation.fieldContent;
			break;
		case REPLACE_REGEXP:
		default:
			replacePattern = representation.fieldContent;
			break;
		}
	}
	
	
	public RegExpRepresentation getRepresentationForReplace() {
		RegExpRepresentation repr = new RegExpRepresentation();
		repr.type = REPLACE_REGEXP;
		if(replacePattern == null) {
			repr.type = REPLACE_MATCH_BY;
			repr.fieldContent = "";
			return repr;
		}else {
			repr.fieldContent = replacePattern;
			if( TextUtils.isEmpty(repr.fieldContent) ) {
				repr.type = REPLACE_MATCH_BY;
				return repr;
			}
		}
		
		Matcher matcher = null;
		
		
		matcher = Pattern.compile("^(.+)\\$0$").matcher(replacePattern);
		if(matcher.matches()) {
			repr.type = REPLACE_PREFIX;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		matcher = Pattern.compile("^\\$0(.+)$").matcher(replacePattern);
		if(matcher.matches()) {
			repr.type = REPLACE_SUFFIX;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
        switch (matchType)
        {
            case MATCHER_STARTS:
                matcher = Pattern.compile("^(.*)\\$1$").matcher(replacePattern);
                break;
            case MATCHER_ENDS:
                matcher = Pattern.compile("^\\$1(.*)$").matcher(replacePattern);
                break;
            case MATCHER_CONTAINS:
                matcher = Pattern.compile("^\\$1(.*)\\$2$").matcher(replacePattern);
                break;
            default:
                // Other types match the entire input
                matcher = Pattern.compile("^(.*)$").matcher(replacePattern);
                break;
        }
		if(matcher.matches()) {
			repr.type = REPLACE_MATCH_BY;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		matcher = Pattern.compile("^([^\\$]+)$").matcher(replacePattern);
		if(matcher.matches()) {
			repr.type = REPLACE_ALL_BY;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		return repr;
	}

	
	//Static utility method

	public static boolean isCallableNumber(SipProfile account, String number, DBAdapter db) {
		boolean canCall = true;
		db.open();
		Cursor c = db.getFiltersForAccount(account.id);
		int numRows = c.getCount();
		//Log.d(THIS_FILE, "F > This account has "+numRows+" filters");
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			Log.d(THIS_FILE, "Test filter "+f.matchPattern);
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
	

	public static boolean isMustCallNumber(SipProfile account, String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(account.id);
		int numRows = c.getCount();
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			//Log.d(THIS_FILE, "Test filter "+f.match_pattern);
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
	
	public static String rewritePhoneNumber(SipProfile account, String number, Context ctxt) {
		DBAdapter db = new DBAdapter(ctxt);
		return rewritePhoneNumber(account, number, db);
	}
	
	public static String rewritePhoneNumber(SipProfile account, String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(account.id);
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
	
	public static boolean isAutoAnswerNumber(SipProfile account, String number, DBAdapter db) {
		db.open();
		Cursor c = db.getFiltersForAccount(account.id);
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
	
	

	// Helpers static factory
	public static Filter getProfileFromDbId(Context ctxt, long filterId, String[] projection) {
		Filter filter = new Filter();
		if(filterId >= 0) {
			Cursor c = ctxt.getContentResolver().query(ContentUris.withAppendedId(SipManager.FILTER_ID_URI_BASE, filterId), 
					projection, null, null, null);
			
			if(c != null) {
				try {
					if(c.getCount() > 0) {
						c.moveToFirst();
						filter = new Filter(c);
					}
				}catch(Exception e) {
					Log.e(THIS_FILE, "Something went wrong while retrieving the account", e);
				} finally {
					c.close();
				}
			}
		}
		return filter;
	}
	
}
