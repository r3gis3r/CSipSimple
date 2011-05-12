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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.text.TextUtils;
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
			c.moveToNext();
		}
		c.close();
		
		try {
			jsonProfile.put(FILTER_KEY, jsonFilters);
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Impossible to add fitlers", e);
		}
		
		return jsonProfile;
	}
	
	private static final JSONArray serializeSipProfiles(Context ctxt) {
		JSONArray jsonSipProfiles = new JSONArray();
		DBAdapter db = new DBAdapter(ctxt);
		db.open();
		List<SipProfile> accounts = db.getListAccounts();
		int i = 0;
		for(i = 0; i<accounts.size(); i++) {
			JSONObject p = serializeSipProfile(accounts.get(i), db);
			try {
				jsonSipProfiles.put(jsonSipProfiles.length(), p);
			} catch (JSONException e) {
				Log.e(THIS_FILE, "Impossible to add profile", e);
			}
		}
		
		
		
		// Add negative fake accounts
		List<ResolveInfo> callers = Compatibility.getIntentsForCall(ctxt);
		if(callers != null) {
			for(int index = 1; index < callers.size()+1; index++) {
				SipProfile gsmProfile = new SipProfile();
				gsmProfile.id = SipProfile.INVALID_ID - index;
				JSONObject p = serializeSipProfile(gsmProfile, db);
				try {
					jsonSipProfiles.put( jsonSipProfiles.length(), p);
				} catch (JSONException e) {
					Log.e(THIS_FILE, "Impossible to add profile", e);
				}
			}
		}
		

		db.close();
		return jsonSipProfiles;
	}
	
	
	private static final JSONObject serializeSipSettings(Context ctxt) {
		PreferencesWrapper prefs = new PreferencesWrapper(ctxt);
		return prefs.serializeSipSettings();
	}
	
	private static String KEY_ACCOUNTS = "accounts";
	private static String KEY_SETTINGS = "settings";
	
	/**
	 * Save current sip configuration
	 * @param ctxt
	 * @return
	 */
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
				configChain.put(KEY_SETTINGS, serializeSipSettings(ctxt) );
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
			} catch (Exception e) {
				// Catch exception if any
				Log.e(THIS_FILE, "Impossible to save config to disk", e);
				return false;
			}
		}
		return false;
	}
	
	
	// --- RESTORE PART --- //
	private static boolean restoreSipProfile(Context ctxt, JSONObject jsonObj, DBAdapter db) {
		//Restore accounts
		Columns cols;
		ContentValues cv;
		
		
		cols = new Columns(SipProfile.full_projection, SipProfile.full_projection_types);
		cv = cols.jsonToContentValues(jsonObj);
		
		SipProfile profile = new SipProfile();
		profile.createFromContentValue(cv);
		if(profile.id >= 0) {
			db.insertAccount(profile);
		}
		
		//Restore filters
		cols = new Columns(Filter.full_projection, Filter.full_projection_types);
		try {
			JSONArray filtersObj = jsonObj.getJSONArray(FILTER_KEY);
			Log.d(THIS_FILE, "We have filters for " + profile.id + " > "+filtersObj.length());
			for(int i = 0; i < filtersObj.length(); i++) {
				JSONObject filterObj = filtersObj.getJSONObject(i);
				//Log.d(THIS_FILE, "restoring "+filterObj.toString(4));
				cv = cols.jsonToContentValues(filterObj);
				cv.put(Filter.FIELD_ACCOUNT, profile.id);
				Filter filter = new Filter();
				filter.createFromContentValue(cv);
				db.insertFilter(filter);
			}
		} catch (JSONException e) {
			Log.e(THIS_FILE, "Error while restoring filters", e);
		}
		
		return false;
	}
	
	private static void restoreSipSettings(Context ctxt, JSONObject settingsObj) {
		PreferencesWrapper prefs = new PreferencesWrapper(ctxt);
		prefs.restoreSipSettings(settingsObj);
	}
	
	
	/**
	 * Restore a sip configuration
	 * @param ctxt
	 * @param fileToRestore
	 * @return
	 */
	public static boolean restoreSipConfiguration(Context ctxt, File fileToRestore) {
		if(fileToRestore != null && fileToRestore.isFile()) {
			
			String content = "";
			
			try {
				BufferedReader buf;
				String line;
				buf = new BufferedReader(new FileReader(fileToRestore));
				while( (line = buf.readLine()) != null ) {
					content += line;
				}
			} catch (FileNotFoundException e) {
				Log.e(THIS_FILE, "Error while restoring", e);
			} catch (IOException e) {
	            Log.e(THIS_FILE, "Error while restoring", e);
	        }

			if(!TextUtils.isEmpty(content)) {
				DBAdapter db = new DBAdapter(ctxt);
				db.open();
				try {
					JSONObject mainJSONObject = new JSONObject(content);
					
					// Manage accounts
					JSONArray accounts = mainJSONObject.getJSONArray(KEY_ACCOUNTS);
					if( accounts.length() > 0 ) {
						db.removeAllAccounts();
					}
					for(int i=0; i<accounts.length(); i++) {
						try {
							JSONObject account = accounts.getJSONObject(i);
							restoreSipProfile(ctxt, account, db);
						}catch(JSONException e) {
							Log.e(THIS_FILE,"Unable to parse item "+i , e);
						}
					}

					db.close();
					
					// Manage settings
					JSONObject settings = mainJSONObject.getJSONObject(KEY_SETTINGS);
					if(settings != null) {
						restoreSipSettings(ctxt, settings);
					}
					
					return true;
				} catch (JSONException e) {
					Log.e(THIS_FILE, "Error while parsing saved file", e);
				}
				db.close();
			}
		}
		
		return false;
	}
}
