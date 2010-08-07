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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

@SuppressWarnings("serial")
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
	
	public static final int MATCHER_START = 0;
	public static final int MATCHER_HAS_N_DIGIT = 1;
	public static final int MATCHER_HAS_MORE_N_DIGIT = 2;
	public static final int MATCHER_IS_EXACTLY = 3;
	public static final int MATCHER_REGEXP = 4;
	
	public static final int REPLACE_PREFIX = 0;
	public static final int REPLACE_MATCH_TO = 1;
	public static final int REPLACE_TRANSFORM = 2;
	public static final int REPLACE_REGEXP = 3;
	
	
	public static final String[] common_projection = {
		_ID,
		FIELD_PRIORITY,
		FIELD_MATCHES,
		FIELD_REPLACE,
		FIELD_ACTION
	};
	
	public static final String DEFAULT_ORDER = FIELD_PRIORITY+" desc"; //TODO : should be a os constant... just find it
	private static final String THIS_FILE = "Filter";
	
	public Integer id;
	public Integer priority;
	public Integer account;
	public String matches;
	public String replace;
	public Integer action;
	
	
	
	public void createFromDb(Cursor c) {
		ContentValues args = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, args);
		
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
			matches = tmp_s;
		}
		tmp_s = args.getAsString(FIELD_REPLACE);
		if (tmp_s != null) {
			replace = tmp_s;
		}
	}



	public ContentValues getDbContentValues() {
		ContentValues args = new ContentValues();
		
		if(id != null){
			args.put(_ID, id);
		}
		args.put(FIELD_ACCOUNT, account);
		args.put(FIELD_MATCHES, matches);
		args.put(FIELD_REPLACE, replace);
		args.put(FIELD_ACTION, action);
		args.put(FIELD_PRIORITY, priority);
		return args;
	}



	public String getRepresentation(Context context) {
		String[] choices = context.getResources().getStringArray(R.array.filters_action);
		String repr = "";
		repr += choices[getPositionForAction(action)];
		repr += " "+matches;
		if(!TextUtils.isEmpty(replace) && action == ACTION_REPLACE) {
			repr += " > "+replace;
		}
		return repr;
	}
	
	public boolean canCall(String number) {
		Log.d(THIS_FILE, "Check if filter is valid for "+number+" >> "+action+" and "+matches);
		if(action == ACTION_CANT_CALL) {
			try {
				return !Pattern.matches(matches, number);
			}catch(PatternSyntaxException e) {
				Log.e(THIS_FILE, "Invalid pattern ", e);
			}
			
		}
		return true;
	}
	
	public boolean stopProcessing(String number) {
		Log.d(THIS_FILE, "Should stop processing "+number+" ? ");
		if(action == ACTION_CAN_CALL) {
			try {
				return Pattern.matches(matches, number);
			}catch(PatternSyntaxException e) {
				Log.e(THIS_FILE, "Invalid pattern ", e);
			}
		}
		Log.d(THIS_FILE, "Response : false");
		return false;
	}
	
	public String rewrite(String number) {
		if(action == ACTION_REPLACE) {
			try {
				Pattern pattern = Pattern.compile(matches);
				Matcher matcher = pattern.matcher(number);
				return matcher.replaceAll(replace); 
			}catch(PatternSyntaxException e) {
				Log.e(THIS_FILE, "Invalid pattern ", e);
			}
		}
		return number;
	}

	
	
	//Utilities functions
	private static int getForPosition(HashMap<Integer, Integer> positions, Integer key) {
		return positions.get(key);
	}
	private static int getPositionFor(HashMap<Integer, Integer> positions, Integer value) {
		if(value != null) {
			for (Entry<Integer, Integer> entry : positions.entrySet()) {
				if (entry.getValue().equals(value)) {
					return entry.getKey();
				}
			}
		}
		return 0;
	}
	

	/**
	 * Available actions
	 */
	private static HashMap<Integer, Integer> filterActionPositions = new HashMap<Integer, Integer>() {{
		put(0, ACTION_CANT_CALL);
		put(1, ACTION_REPLACE);
		put(2, ACTION_CAN_CALL);
	}};
	
	public static int getActionForPosition(Integer selectedItemPosition) {
		return getForPosition(filterActionPositions, selectedItemPosition);
	}

	public static int getPositionForAction(Integer selectedAction) {
		return getPositionFor(filterActionPositions, selectedAction);
	}
	
	/**
	 * Available matches patterns
	 */
	private static HashMap<Integer, Integer> matcherTypePositions = new HashMap<Integer, Integer>() {{
		put(0, MATCHER_START);
		put(1, MATCHER_HAS_N_DIGIT);
		put(2, MATCHER_HAS_MORE_N_DIGIT);
		put(3, MATCHER_IS_EXACTLY);
		put(4, MATCHER_REGEXP);
	}};
	
	public static int getMatcherForPosition(Integer selectedItemPosition) {
		return getForPosition(matcherTypePositions, selectedItemPosition);
	}

	public static int getPositionForMatcher(Integer selectedAction) {
		return getPositionFor(matcherTypePositions, selectedAction);
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
		switch(representation.type) {
		case MATCHER_START:
			matches = "^"+Pattern.quote(representation.fieldContent)+"(.*)$";
			break;
		case MATCHER_HAS_N_DIGIT:
			//TODO: is dot the best char?
			//TODO ... we should probably test the fieldContent type to ensure it's well digits...
			matches = "^(.{"+representation.fieldContent+"})$";
			break;
		case MATCHER_HAS_MORE_N_DIGIT:
			//TODO ... we should probably test the fieldContent type to ensure it's well digits...
			matches = "^(.{"+representation.fieldContent+",})$";
			break;
		case MATCHER_IS_EXACTLY:
			matches = "^("+Pattern.quote(representation.fieldContent)+")$";
			break;
		case MATCHER_REGEXP:
		default:
			matches = representation.fieldContent;
			break;
		}
	}
	
	/**
	 * Get the represantation for current matcher
	 * @return RegExpReprestation object with type of matcher and content for matcher 
	 * (content that should be shown in a text field for user)
	 */
	public RegExpRepresentation getRepresentationForMatcher() {
		RegExpRepresentation repr = new RegExpRepresentation();
		repr.type = MATCHER_REGEXP;
		if(matches == null) {
			repr.fieldContent = "";
			return repr;
		}else {
			repr.fieldContent = matches;
		}
		
		Matcher matcher = null;
		
		//Well... here we are... Some awful regexp matcher to test a regexp... Isn't it nice?
		matcher = Pattern.compile("^\\^\\\\Q(.+)\\\\E\\(\\.\\*\\)\\$$").matcher(matches);
		if(matcher.matches()) {
			repr.type = MATCHER_START;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		matcher = Pattern.compile("^\\^\\(\\.\\{([0-9]+)\\}\\)\\$$").matcher(matches);
		if(matcher.matches()) {
			repr.type = MATCHER_HAS_N_DIGIT;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		matcher = Pattern.compile("^\\^\\(\\.\\{([0-9]+),\\}\\)\\$$").matcher(matches);
		if(matcher.matches()) {
			repr.type = MATCHER_HAS_MORE_N_DIGIT;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		matcher = Pattern.compile("^\\^\\(\\\\Q(.+)\\\\E\\)\\$$").matcher(matches);
		if(matcher.matches()) {
			repr.type = MATCHER_IS_EXACTLY;
			repr.fieldContent = matcher.group(1);
			return repr;
		}
		
		
		return repr;
	}
	
	
	public void setReplaceRepresentation(RegExpRepresentation representation){
		switch(representation.type) {
		
		}
	}

}
