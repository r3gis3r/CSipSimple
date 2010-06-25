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
package com.csipsimple.ui.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.pjsip.pjsua.pj_str_t;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.SimpleAdapter;

import com.csipsimple.R;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

public class Codecs extends ListActivity {

	protected static final String THIS_FILE = "Codecs";
	
	private static final String CODEC_NAME = "codec_name";
	private static final String CODEC_ID = "codec_id";
	private static final String CODEC_PRIORITY = "codec_priority";
	
	
	private SimpleAdapter adapter;
	ArrayList<HashMap<String, Object>> codecs;

	private PreferencesWrapper prefsWrapper;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.codec_picker_activity);
		
		prefsWrapper = new PreferencesWrapper(this);
		initDatas();
		
		
		adapter = new SimpleAdapter(this, codecs, R.layout.codec_pref_line, new String []{
			CODEC_NAME
		}, new int[] {
			R.id.line1
		});
		
		setListAdapter(adapter);
		
		DragnDropListView listView = (DragnDropListView) getListView();
		listView.setOnDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				
				HashMap<String, Object> item = (HashMap<String, Object>) getListAdapter().getItem(from);
				
				Log.d(THIS_FILE, "Dropped "+item.get(CODEC_NAME)+" -> "+to);
				
				codecs.remove(from);
				codecs.add(to, item);
				
				//Update priorities
				short currentPriority = 130;
				for(HashMap<String, Object> codec : codecs) {
					if(currentPriority != (Short) codec.get(CODEC_PRIORITY)) {
						prefsWrapper.setCodecPriority((String)codec.get(CODEC_ID), Short.toString(currentPriority));
						codec.put(CODEC_PRIORITY, currentPriority);
					}
					//Log.d(THIS_FILE, "Reorder : "+codec.toString());
					currentPriority --;
				}
				
				
				//Log.d(THIS_FILE, "Data set "+codecs.toString());
				((SimpleAdapter) adapter).notifyDataSetChanged();
			}
		});
		
	}
	
	
	private void initDatas() {
		codecs = new ArrayList<HashMap<String, Object>>();
		
		if(SipService.codecs == null) {
			Log.w(THIS_FILE, "Codecs not initialized in service !!! ");
			return;
		}
		
		int current_prio = 130;
		for(String codecName : SipService.codecs) {
			Log.d(THIS_FILE, "Fill codec "+codecName);
			String[] codecParts = codecName.split("/");
			if(codecParts.length >=2 ) {
				HashMap<String, Object> codecInfo = new HashMap<String, Object>();
				codecInfo.put(CODEC_ID, codecName);
				codecInfo.put(CODEC_NAME, codecParts[0]+" "+codecParts[1].substring(0, codecParts[1].length()-3)+" kHz");
				codecInfo.put(CODEC_PRIORITY, prefsWrapper.getCodecPriority(codecName, Integer.toString(current_prio)));
				codecs.add(codecInfo);
				current_prio --;
			}
		}
		
		Collections.sort(codecs, codecsComparator);
	}
	
	private Comparator<HashMap<String, Object>> codecsComparator = new Comparator<HashMap<String, Object>>() {
		@Override
		public int compare(HashMap<String, Object> infos1, HashMap<String, Object> infos2) {
			
			if (infos1 != null && infos2 != null) {
				if ((Short)infos1.get(CODEC_PRIORITY) > (Short)infos2.get(CODEC_PRIORITY)) {
					return -1;
				}
				if ((Short)infos1.get(CODEC_PRIORITY) < (Short)infos2.get(CODEC_PRIORITY)) {
					return 1;
				}
			}

			return 0;
		}
	};
}
