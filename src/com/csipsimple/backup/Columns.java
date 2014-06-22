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
/**
 * This file contains relicensed code from Apache copyright of 
 * Copyright 2010 Google Inc.
 */

package com.csipsimple.backup;

import android.content.ContentValues;
import android.database.Cursor;

import com.csipsimple.utils.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class Columns {

	private ArrayList<String> names;

	private enum Type {
		STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN
	}

	private ArrayList<Type> types;

	public Columns(String[] names, Class<?>[] classes) {
		this.names = new ArrayList<String>(Arrays.asList(names));
		types = new ArrayList<Type>(names.length);
		for (int i = 0; i < names.length; i++) {

			if (classes[i] == String.class) {
				types.add(i, Type.STRING);
			} else if (classes[i] == Integer.TYPE || classes[i] == Integer.class) {
				types.add(i, Type.INT);
			} else if (classes[i] == Long.TYPE || classes[i] == Long.class) {
				types.add(i, Type.LONG);
			} else if (classes[i] == Float.TYPE || classes[i] == Float.class) {
				types.add(i, Type.FLOAT);
			} else if (classes[i] == Double.TYPE || classes[i] == Double.class) {
				types.add(i, Type.DOUBLE);
			} else if (classes[i] == Boolean.TYPE || classes[i] == Boolean.class) {
				types.add(i, Type.BOOLEAN);
			}
		}
	}
	
    public boolean removeColumn(String columnName) {
            int index = names.indexOf(columnName);
            if(index < 0) {
                return false;
            }
            names.remove(index);
            types.remove(index);
            return true;
    }

	public boolean hasField(Cursor c, String name) {
		int i = c.getColumnIndex(name);
		return ((i != -1) && !c.isNull(i));
	}

	public JSONObject contentValueToJSON(ContentValues cv) {
		JSONObject json = new JSONObject();
		for (int i = 0; i < names.size(); i++) {
			if (!cv.containsKey(names.get(i))) {
				continue;
			}
			try {
			    String name = names.get(i);
				switch (types.get(i)) {
				case STRING:
					json.put(name, cv.getAsString(name));
					break;
				case INT:
					json.put(name, cv.getAsInteger(name));
					break;
				case LONG:
					json.put(name, cv.getAsLong(name));
					break;
				case FLOAT:
					json.put(name, cv.getAsFloat(name));
					break;
				case DOUBLE:
					json.put(name, cv.getAsDouble(name));
					break;
				case BOOLEAN:
					json.put(name, cv.getAsBoolean(name));
					break;
				default:
					Log.w("Col", "Invalid type, can't unserialize " + types.get(i));
				}
			} catch (JSONException e) {
				Log.e("Col", "Invalid type, can't unserialize ", e);
			}
		}

		return json;
	}

	public ContentValues jsonToContentValues(JSONObject j) {
		ContentValues cv = new ContentValues();
		for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
			switch (types.get(i)) {
			case STRING:
				j2cvString(j, cv, name);
				break;
			case INT:
				j2cvInt(j, cv, name);
				break;
			case LONG:
				j2cvLong(j, cv, name);
				break;
			case FLOAT:
				j2cvFloat(j, cv, name);
				break;
			case DOUBLE:
				j2cvDouble(j, cv, name);
				break;
			case BOOLEAN:
				j2cvBoolean(j, cv, name);
			}
		}

		return cv;
	}

	private static void j2cvInt(JSONObject j, ContentValues cv, String key) {
		try {
			int v = j.getInt(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}

	private static void j2cvLong(JSONObject j, ContentValues cv, String key) {
		try {
			long v = j.getLong(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}

	private static void j2cvString(JSONObject j, ContentValues cv, String key) {
		try {
			String v = j.getString(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}

	private static void j2cvFloat(JSONObject j, ContentValues cv, String key) {
		try {
			float v = (float) j.getDouble(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}

	private static void j2cvDouble(JSONObject j, ContentValues cv, String key) {
		try {
			double v = j.getDouble(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}

	private static void j2cvBoolean(JSONObject j, ContentValues cv, String key) {
		try {
			boolean v = j.getBoolean(key);
			cv.put(key, v);
		} catch (JSONException e) {
		}
	}
	
}
