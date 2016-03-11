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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseIntArray;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.bluetooth.BluetoothWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final int MATCHER_BLUETOOTH = 8;
    public static final int MATCHER_CALLINFO_AUTOREPLY = 9;
	
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
	
	
	public static final String DEFAULT_ORDER = FIELD_PRIORITY + " asc";
	
	private static final String BLUETOOTH_MATCHER_KEY = "###BLUETOOTH###";
    private static final String CALLINFO_AUTOREPLY_MATCHER_KEY = "###CALLINFO_AUTOREPLY###";
	
	
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
		if(m.type != MATCHER_BLUETOOTH &&
		        m.type != MATCHER_CALLINFO_AUTOREPLY &&
		        m.type != MATCHER_ALL) {
    		reprBuf.append(' ');
    		reprBuf.append(m.fieldContent);
		}
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
	
	private boolean patternMatches(Context ctxt, String number, Bundle extraHdr, boolean defaultValue) {
	    if(CALLINFO_AUTOREPLY_MATCHER_KEY.equals(matchPattern)) {
	        if(extraHdr != null &&
                extraHdr.containsKey("Call-Info")) {
                String hdrValue = extraHdr.getString("Call-Info");
                if(hdrValue != null) {
                    hdrValue = hdrValue.trim();
                }
                if(!TextUtils.isEmpty(hdrValue) &&
                        "answer-after=0".equalsIgnoreCase(hdrValue)){
                    return true;
                }
            }
	    }else if(BLUETOOTH_MATCHER_KEY.equals(matchPattern)) {
            return BluetoothWrapper.getInstance(ctxt).isBTHeadsetConnected();
        }else {
            try {
                return Pattern.matches(matchPattern, number);
            }catch(PatternSyntaxException e) {
                logInvalidPattern(e);
            }
        }
	    return defaultValue;
	}
	
	/**
	 * Does the filter allows to call ?
	 * @param ctxt Application context
	 * @param number number to test
	 * @return true if we can call this number
	 */
	public boolean canCall(Context ctxt, String number) {
		if(action == ACTION_CANT_CALL) {
		    return !patternMatches(ctxt, number, null, false);
			
		}
		return true;
	}
	
	/**
	 * Does the filter force to call ?
	 * @param ctxt Application context
	 * @param number number to test
	 * @return true if we must call this number
	 */
	public boolean mustCall(Context ctxt, String number) {
		if(action == ACTION_DIRECTLY_CALL) {
		    return patternMatches(ctxt, number, null, false);
			
		}
		return false;
	}
	
	/**
	 * Should the filter avoid next filters ?
	 * @param ctxt Application context
	 * @param number number to test
	 * @return true if we should not process next filters
	 */
	public boolean stopProcessing(Context ctxt, String number) {
		if(action == ACTION_CAN_CALL || action == ACTION_DIRECTLY_CALL) {
			return patternMatches(ctxt, number, null, false);
		}
		return false;
	}
	
	/**
	 * Does the filter auto answer a call ?
	 * @param ctxt Application context
	 * @param number number to test
	 * @return true if the call should be auto-answered
	 */
    public boolean autoAnswer(Context ctxt, String number, Bundle extraHdr) {
        if(action == ACTION_AUTO_ANSWER) {
            return patternMatches(ctxt, number, extraHdr, false);
        }
        return false;
    }
	
    /**
     * Rewrite the number with this filter rule
     * @param number the number to rewrite
     * @return the rewritten number
     */
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
	
	
	//Utilities functions
	private static int getForPosition(SparseIntArray positions, Integer key) {
		return positions.get(key);
	}
	
	private static int getPositionFor(SparseIntArray positions, Integer value) {
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
	private final static SparseIntArray FILTER_ACTION_POS = new SparseIntArray();
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
	private final static SparseIntArray MATCHER_TYPE_POS = new SparseIntArray();
	
	static {
		MATCHER_TYPE_POS.put(0, MATCHER_STARTS);
		MATCHER_TYPE_POS.put(1, MATCHER_ENDS);
		MATCHER_TYPE_POS.put(2, MATCHER_CONTAINS);
		MATCHER_TYPE_POS.put(3, MATCHER_ALL);
		MATCHER_TYPE_POS.put(4, MATCHER_HAS_N_DIGIT);
		MATCHER_TYPE_POS.put(5, MATCHER_HAS_MORE_N_DIGIT);
		MATCHER_TYPE_POS.put(6, MATCHER_IS_EXACTLY);
		MATCHER_TYPE_POS.put(7, MATCHER_REGEXP);
        MATCHER_TYPE_POS.put(8, MATCHER_BLUETOOTH);
        MATCHER_TYPE_POS.put(9, MATCHER_CALLINFO_AUTOREPLY);
	};
	
	public static int getMatcherForPosition(Integer selectedItemPosition) {
		return getForPosition(MATCHER_TYPE_POS, selectedItemPosition);
	}

	public static int getPositionForMatcher(Integer selectedAction) {
		return getPositionFor(MATCHER_TYPE_POS, selectedAction);
	}
	
	private final static SparseIntArray REPLACE_TYPE_POS = new SparseIntArray();
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
		case MATCHER_BLUETOOTH:
		    matchPattern = BLUETOOTH_MATCHER_KEY;
		    break;
		case MATCHER_CALLINFO_AUTOREPLY:
		    matchPattern = CALLINFO_AUTOREPLY_MATCHER_KEY;
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
			repr.type = matchType = MATCHER_STARTS;
			repr.fieldContent = "";
			return repr;
		}else {
			repr.fieldContent = matchPattern;
			if( TextUtils.isEmpty(repr.fieldContent) ) {
				repr.type = matchType = MATCHER_STARTS;
				return repr;
			}
		}
		
		if(matchPattern.equals(BLUETOOTH_MATCHER_KEY)) {
		    repr.type = matchType = MATCHER_BLUETOOTH;
		}else if(matchPattern.equalsIgnoreCase(CALLINFO_AUTOREPLY_MATCHER_KEY)) {
		    repr.type = matchType = MATCHER_CALLINFO_AUTOREPLY;
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
			if(action != null && action == ACTION_AUTO_ANSWER) {
			    repr.fieldContent = replacePattern;
			}
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

    public static boolean isCallableNumber(Context ctxt, long accountId, String number) {
        boolean canCall = true;
        List<Filter> filterList = getFiltersForAccount(ctxt, accountId);
        for (Filter f : filterList) {
            Log.d(THIS_FILE, "Test filter " + f.matchPattern);
            canCall &= f.canCall(ctxt, number);

            // Stop processing & rewrite
            if (f.stopProcessing(ctxt, number)) {
                return canCall;
            }
            number = f.rewrite(number);
        }
        return canCall;
    }

	public static boolean isMustCallNumber(Context ctxt, long accountId, String number) {
	    List<Filter> filterList = getFiltersForAccount(ctxt, accountId);
        for (Filter f : filterList) {
			if(f.mustCall(ctxt, number)) {
				return true;
			}
			//Stop processing & rewrite
			if(f.stopProcessing(ctxt, number)) {
				return false;
			}
			number = f.rewrite(number);
		}
		return false;
	}
	
	/**
	 * Rewrite a phone number for use with an account.
	 * 
	 * @param ctxt The application context.
	 * @param accountId The account id to use for outgoing call
	 * @param number The number to rewrite
	 * @return Rewritten number
	 */
	public static String rewritePhoneNumber(Context ctxt, long accountId, String number) {
        List<Filter> filterList = getFiltersForAccount(ctxt, accountId);
        for (Filter f : filterList) {
			//Log.d(THIS_FILE, "RW > Test filter "+f.matches);
			number = f.rewrite(number);
			if(f.stopProcessing(ctxt, number)) {
				return number;
			}
		}
		return number;
	}
	
	public static int isAutoAnswerNumber(Context ctxt, long accountId, String number, Bundle extraHdr) {
        List<Filter> filterList = getFiltersForAccount(ctxt, accountId);
        for (Filter f : filterList) {
            if (f.autoAnswer(ctxt, number, extraHdr)) {
                if (TextUtils.isEmpty(f.replacePattern)) {
                    return 200;
                }
                try {
                    return Integer.parseInt(f.replacePattern);
                } catch (NumberFormatException e) {
                    Log.e(THIS_FILE, "Invalid autoanswer code : " + f.replacePattern);
                }
                return 200;
            }
            // Stop processing & rewrite
            if (f.stopProcessing(ctxt, number)) {
                return 0;
            }
            number = f.rewrite(number);
        }
        return 0;
    }
	
	

	// Helpers static factory
	public static Filter getFilterFromDbId(Context ctxt, long filterId, String[] projection) {
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
	
	private static Map<Long, List<Filter>> FILTERS_PER_ACCOUNT = new HashMap<Long, List<Filter>>();
	
	private static List<Filter> getFiltersForAccount(Context ctxt, long accountId){
        if (!FILTERS_PER_ACCOUNT.containsKey(accountId)) {
            ArrayList<Filter> aList = new ArrayList<Filter>();
            Cursor c = getFiltersCursorForAccount(ctxt, accountId);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        do {
                            aList.add(new Filter(c));
                        } while (c.moveToNext());
                    }
                } catch (Exception e) {
                    Log.e(THIS_FILE, "Error on looping over sip profiles", e);
                } finally {
                    c.close();
                }
            }
            FILTERS_PER_ACCOUNT.put(accountId, aList);
        }
        return FILTERS_PER_ACCOUNT.get(accountId);
	}
	
	public static void resetCache() {
	    FILTERS_PER_ACCOUNT = new HashMap<Long, List<Filter>>();
	}
	
	public static Cursor getFiltersCursorForAccount(Context ctxt, long accountId) {
	    return ctxt.getContentResolver().query(SipManager.FILTER_URI, FULL_PROJ, FIELD_ACCOUNT+"=?", new String[]{Long.toString(accountId)}, DEFAULT_ORDER);
	}
}
