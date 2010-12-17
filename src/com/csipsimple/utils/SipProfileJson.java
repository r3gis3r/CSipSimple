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


package com.csipsimple.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;

import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Filter;

public class SipProfileJson {
	
	private static final String THIS_FILE = "SipProfileJson";
	private static String FILTER_KEY = "filters";
	
	private static JSONObject serializeBaseSipProfile(SipProfile profile) {
		
		ContentValues cv = profile.getDbContentValues();
		Columns cols = new Columns(SipProfile.full_projection, SipProfile.full_projection_types);
		
		return cols.contentValueToJSON(cv);
	}
	
	
	private static JSONObject serializeBaseFilter(Filter filter) {
		
		ContentValues cv = filter.getDbContentValues();
		Columns cols = new Columns(Filter.full_projection, Filter.full_projection_types);
		
		return cols.contentValueToJSON(cv);
	}
	
	public static JSONObject serializeSipProfile(SipProfile profile, DBAdapter db) {
		JSONObject jsonProfile = serializeBaseSipProfile(profile);
		JSONArray jsonFilters = new JSONArray();
		
		Cursor c = db.getFiltersForAccount(profile.id);
		int numRows = c.getCount();
		c.moveToFirst();
		for (int i = 0; i < numRows; ++i) {
			Filter f = new Filter();
			f.createFromDb(c);
			
			try {
				jsonFilters.put(i, serializeBaseFilter(f));
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Impossible to add fitler", e);
				e.printStackTrace();
			}
		}
		c.close();
		
		try {
			jsonProfile.put(FILTER_KEY, jsonFilters);
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Impossible to add fitlers", e);
		}
		
		return jsonProfile;
	}
	
	public static JSONArray serializeSipProfiles(Context ctxt) {
		JSONArray jsonSipProfiles = new JSONArray();
		DBAdapter db = new DBAdapter(ctxt);
		db.open();
		List<SipProfile> accounts = db.getListAccounts();
		for(int i = 0; i<accounts.size(); i++) {
			JSONObject p = serializeSipProfile(accounts.get(i), db);
			try {
				jsonSipProfiles.put(i, p);
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Impossible to add profile", e);
			}
		}
		
		db.close();
		
		return jsonSipProfiles;
	}
	
	private static String KEY_ACCOUNTS = "accounts";
	
	
	public static boolean saveSipConfiguration(Context ctxt) {
		File dir = PreferencesWrapper.getConfigFolder();
		if( dir != null) {
			Date d = new Date();
			File file = new File(dir.getAbsoluteFile() + File.separator + "backup_"+DateFormat.format("MM-dd-yy_kkmmss", d)+".json");
			Log.d(THIS_FILE, "Out dir " + file.getAbsolutePath());
			
			
			JSONObject configChain = new JSONObject();
			try {
				configChain.put(KEY_ACCOUNTS, serializeSipProfiles(ctxt) );
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Impossible to add profiles", e);
			}
			
			try {
				// Create file
				FileWriter fstream = new FileWriter(file.getAbsoluteFile());
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(configChain.toString(2));
				// Close the output stream
				out.close();
				return true;
			} catch (Exception e) {// Catch exception if any
				Log.e(THIS_FILE, "Impossible to save config to disk", e);
				return false;
			}
		}
		return false;
	}
}
