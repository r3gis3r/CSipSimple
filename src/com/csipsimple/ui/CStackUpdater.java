/**
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
package com.csipsimple.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import com.csipsimple.utils.Log;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.service.SipService;
import com.csipsimple.widgets.DownloadBubbleView;

public class CStackUpdater extends Activity {

	protected static final String THIS_FILE = "CStackUpdater";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cstackupdater);

		

		getUpdates();
	}
	
	
	public class Update extends Object {
		public String text;
		public String description;
		public String version;
		public String changelog;
		public URI url;
	}

	private void getUpdates() {
		HttpClient updateHttpClient = new DefaultHttpClient();

		URI updateServerUri = URI.create("http://10.0.2.2/android/update.json");
		HttpUriRequest updateReq = new HttpGet(updateServerUri);
		updateReq.addHeader("Cache-Control", "no-cache");

		try {
			HttpResponse updateResponse;
			HttpEntity updateResponseEntity = null;

			updateResponse = updateHttpClient.execute(updateReq);
			int updateServerResponse = updateResponse.getStatusLine()
					.getStatusCode();
			if (updateServerResponse != HttpStatus.SC_OK) {
				Log.e(THIS_FILE, "can't get updates from site");
				return; // TODO : should throw something
			}

			updateResponseEntity = updateResponse.getEntity();
			BufferedReader upLineReader = new BufferedReader(
					new InputStreamReader(updateResponseEntity.getContent()),
					2 * 1024);
			StringBuffer upBuf = new StringBuffer();
			String upLine;
			while ((upLine = upLineReader.readLine()) != null) {
				upBuf.append(upLine);
			}
			upLineReader.close();

			try {
				JSONObject mainJSONObject = new JSONObject(upBuf.toString());

				JSONArray coreJSONArray = mainJSONObject
						.getJSONArray("sip_core");
				
				JSONObject stack = getCompatibleStack(coreJSONArray);
				if(stack != null){
					Update up = getLatestUpdate(stack);
					createButtonForUpdate(up);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private  Update getLatestUpdate(JSONObject stack){
		Update up = new Update();
		JSONObject latestUpJSON;
		try {
			latestUpJSON = stack.getJSONArray("versions").getJSONObject(0);
			up.changelog = latestUpJSON.getString("changelog");
			up.version = latestUpJSON.getString("version");
			up.url = URI.create(latestUpJSON.getString("download_url"));
			up.description = stack.getString("description");
			up.text = stack.getString("label");
			return up;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private JSONObject getCompatibleStack(JSONArray availableStacks ){
		int core_count = availableStacks.length();
		for ( int i=0; i< core_count; i++) {
			JSONObject plateform_stack;
			try{
				plateform_stack = availableStacks.getJSONObject(i);
				if(isCompatibleStack(plateform_stack.getJSONObject("filters"))){
					Log.d(THIS_FILE, "Found : "+plateform_stack.getString("id"));
					return plateform_stack;
				}else{
					Log.d(THIS_FILE, "NOT VALID : "+plateform_stack.getString("id"));
				}
			}catch(Exception e){
				Log.w(THIS_FILE, "INVALID FILTER FOR");
				e.printStackTrace();
			}
			
		}
		return null;
	}
	
	
	
	
	private boolean isCompatibleStack(JSONObject filter) throws SecurityException, NoSuchFieldException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, JSONException {
		
		//For each filter keys, we check if the filter is not invalid
		Iterator<?> iter = filter.keys();
		while(iter.hasNext()){
			//Each filter key correspond to a android class which values has to be checked
			String class_filter = (String) iter.next();
			//Get this class
			Class<?> cls = Class.forName(class_filter);
			
			//Then for this class, we have to check if each static field matches defined regexp rule
			Iterator<?> cls_iter = filter.getJSONObject(class_filter).keys();
			
			while(cls_iter.hasNext()){
				String field_name = (String) cls_iter.next();
				Field field = cls.getField(field_name);
				//Get the current value on the system
				String current_value = field.get(null).toString();
				//Get the filter for this value
				String regexp_filter = filter.getJSONObject(class_filter).getString(field_name);
				
				//Check if matches
				if(! Pattern.matches(regexp_filter, current_value)){
					Log.d(THIS_FILE, "Regexp not match : "+current_value+" matches /"+regexp_filter+"/");
					return false;
				}
			}
		}
		return true;
	}
	
	private void createButtonForUpdate(Update up){
		//Environment.getExternalStorageDirectory()
		File out_file = getApplicationContext().getFileStreamPath(SipService.STACK_FILE_NAME);
		Log.d(THIS_FILE, "Create for : "+out_file.getAbsolutePath());
		DownloadBubbleView nv = new DownloadBubbleView(this,
				up.text, up.description, up.url, out_file);
		
		ViewGroup pv = (ViewGroup) findViewById(R.id.master_layout);
		pv.addView(nv);
	}
}
