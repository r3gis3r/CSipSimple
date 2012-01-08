/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.csipsimple.utils;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

public class Columns {

	private String[] names;

	private enum Type {
		STRING, INT, LONG, FLOAT, DOUBLE, BOOLEAN
	}

	private Type[] types;

	public Columns(String[] names, Class<?>[] classes) {
		this.names = names;
		types = new Type[names.length];
		for (int i = 0; i < names.length; i++) {

			if (classes[i] == String.class) {
				types[i] = Type.STRING;
			} else if (classes[i] == Integer.TYPE || classes[i] == Integer.class) {
				types[i] = Type.INT;
			} else if (classes[i] == Long.TYPE || classes[i] == Long.class) {
				types[i] = Type.LONG;
			} else if (classes[i] == Float.TYPE || classes[i] == Float.class) {
				types[i] = Type.FLOAT;
			} else if (classes[i] == Double.TYPE || classes[i] == Double.class) {
				types[i] = Type.DOUBLE;
			} else if (classes[i] == Boolean.TYPE || classes[i] == Boolean.class) {
				types[i] = Type.BOOLEAN;
			}
		}
	}

	public boolean hasField(Cursor c, String name) {
		int i = c.getColumnIndex(name);
		return ((i != -1) && !c.isNull(i));
	}

	public JSONObject contentValueToJSON(ContentValues cv) {
		JSONObject json = new JSONObject();
		for (int i = 0; i < names.length; i++) {
			if (!cv.containsKey(names[i])) {
				continue;
			}
			try {
				switch (types[i]) {
				case STRING:
					json.put(names[i], cv.getAsString(names[i]));
					break;
				case INT:
					json.put(names[i], cv.getAsInteger(names[i]));
					break;
				case LONG:
					json.put(names[i], cv.getAsLong(names[i]));
					break;
				case FLOAT:
					json.put(names[i], cv.getAsFloat(names[i]));
					break;
				case DOUBLE:
					json.put(names[i], cv.getAsDouble(names[i]));
					break;
				case BOOLEAN:
					json.put(names[i], cv.getAsBoolean(names[i]));
					break;
				default:
					Log.w("Col", "Invalid type, can't unserialize " + types[i]);
				}
			} catch (JSONException e) {
				Log.e("Col", "Invalid type, can't unserialize ", e);
			}
		}

		return json;
	}

	public ContentValues jsonToContentValues(JSONObject j) {
		ContentValues cv = new ContentValues();
		for (int i = 0; i < names.length; i++) {
			switch (types[i]) {
			case STRING:
				j2cvString(j, cv, names[i]);
				break;
			case INT:
				j2cvInt(j, cv, names[i]);
				break;
			case LONG:
				j2cvLong(j, cv, names[i]);
				break;
			case FLOAT:
				j2cvFloat(j, cv, names[i]);
				break;
			case DOUBLE:
				j2cvDouble(j, cv, names[i]);
				break;
			case BOOLEAN:
				j2cvBoolean(j, cv, names[i]);
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
