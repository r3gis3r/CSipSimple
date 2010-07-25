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

import com.csipsimple.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.text.TextUtils;

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
	
	public static final String[] common_projection = {
		_ID,
		FIELD_PRIORITY,
		FIELD_MATCHES,
		FIELD_REPLACE,
		FIELD_ACTION
	};
	public static final String DEFAULT_ORDER = FIELD_PRIORITY+" desc"; //TODO : should be a os constant... just find it
	
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
		repr += choices[action];
		repr += " "+matches;
		if(!TextUtils.isEmpty(replace) && action == ACTION_REPLACE) {
			repr += " > "+replace;
		}
		return repr;
	}

}
