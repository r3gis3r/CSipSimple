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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.widgets.DragnDropListView;
import com.csipsimple.widgets.DragnDropListView.DropListener;

public class Codecs extends ListActivity implements OnClickListener {

	protected static final String THIS_FILE = "Codecs";
	
	private static final String CODEC_NAME = "codec_name";
	private static final String CODEC_ID = "codec_id";
	private static final String CODEC_PRIORITY = "codec_priority";
	
	public static final int MENU_ITEM_ACTIVATE = Menu.FIRST;
	
	private String bandtype = SipConfigManager.CODEC_WB; 
	
	private SimpleAdapter adapter;
	ArrayList<HashMap<String, Object>> codecs;

	private PreferencesWrapper prefsWrapper;

	private ToggleButton nbToggle;
	private ToggleButton wbToggle;
	
	private static final HashMap<String, String> nonFreeCodecs = new HashMap<String, String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put("G729/8000/1", "http://www.synapseglobal.com/g729_codec_license.html");
	}};
	
	@Override
	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.codecs_list);
		
		prefsWrapper = new PreferencesWrapper(this);
		
		nbToggle = (ToggleButton) findViewById(R.id.tg_narrow_band);
		wbToggle = (ToggleButton) findViewById(R.id.tg_wide_band);
		
		nbToggle.setOnClickListener(this);
		wbToggle.setOnClickListener(this);
		initDatas();
		
		
		adapter = new SimpleAdapter(this, codecs, R.layout.codecs_list_item, new String []{
			CODEC_NAME,
			CODEC_PRIORITY
		}, new int[] {
			R.id.line1,
			R.id.entiere_line
		});
		adapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				if(view.getId() == R.id.entiere_line) {
					Log.d(THIS_FILE, "Entiere line is binded ");
					TextView tv = (TextView) view.findViewById(R.id.line1);
					ImageView grabber = (ImageView) view.findViewById(R.id.icon);
					if((Short) data == 0) {
						tv.setTextColor(Color.GRAY);
						grabber.setImageResource(R.drawable.ic_mp_disabled);
					}else {
						tv.setTextColor(Color.WHITE);
						grabber.setImageResource(R.drawable.ic_mp_move);
					}
					return true;
				}
				return false;
			}
			
		});
		
		setListAdapter(adapter);
		
		
		DragnDropListView listView = (DragnDropListView) getListView();
		listView.setOnDropListener(new DropListener() {
			@Override
			public void drop(int from, int to) {
				
				HashMap<String, Object> item = (HashMap<String, Object>) getListAdapter().getItem(from);
				
				Log.d(THIS_FILE, "Dropped "+item.get(CODEC_NAME)+" -> "+to);
				
				//Prevent disabled codecs to be reordered
				if((Short) item.get(CODEC_PRIORITY) <= 0 ) {
					return ;
				}
				
				codecs.remove(from);
				codecs.add(to, item);
				
				//Update priorities
				short currentPriority = 130;
				for(HashMap<String, Object> codec : codecs) {
					if((Short) codec.get(CODEC_PRIORITY) > 0) {
						if(currentPriority != (Short) codec.get(CODEC_PRIORITY)) {
							prefsWrapper.setCodecPriority((String)codec.get(CODEC_ID), bandtype, Short.toString(currentPriority));
							codec.put(CODEC_PRIORITY, currentPriority);
						}
						//Log.d(THIS_FILE, "Reorder : "+codec.toString());
						currentPriority --;
					}
				}
				
				
				//Log.d(THIS_FILE, "Data set "+codecs.toString());
				((SimpleAdapter) adapter).notifyDataSetChanged();
			}
		});
		
		listView.setOnCreateContextMenuListener(this);
		
	}
	
	
	private void initDatas() {
		if(codecs == null) {
			codecs = new ArrayList<HashMap<String, Object>>();
		}else {
			codecs.clear();
		}
		
		String[] codecNames = prefsWrapper.getCodecList();
		
		int current_prio = 130;
		for(String codecName : codecNames) {
			Log.d(THIS_FILE, "Fill codec "+codecName+" for "+bandtype);
			String[] codecParts = codecName.split("/");
			if(codecParts.length >=2 ) {
				HashMap<String, Object> codecInfo = new HashMap<String, Object>();
				codecInfo.put(CODEC_ID, codecName);
				codecInfo.put(CODEC_NAME, codecParts[0]+" "+codecParts[1].substring(0, codecParts[1].length()-3)+" kHz");
				codecInfo.put(CODEC_PRIORITY, prefsWrapper.getCodecPriority(codecName, bandtype, Integer.toString(current_prio)));
				codecs.add(codecInfo);
				current_prio --;
				Log.d(THIS_FILE, "Found priority is "+codecInfo.get(CODEC_PRIORITY));
			}
			
		}
		
		Collections.sort(codecs, codecsComparator);
	}
	
	private Comparator<HashMap<String, Object>> codecsComparator = new Comparator<HashMap<String, Object>>() {
		@Override
		public int compare(HashMap<String, Object> infos1, HashMap<String, Object> infos2) {
			if (infos1 != null && infos2 != null) {
				short c1 = (Short)infos1.get(CODEC_PRIORITY);
				short c2 = (Short)infos2.get(CODEC_PRIORITY);
				if (c1 > c2) {
					return -1;
				}
				if (c1 < c2) {
					return 1;
				}
			}

			return 0;
		}
	};
	
	

	
	@Override
	@SuppressWarnings("unchecked")
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return;
        }

        HashMap<String, Object> codec = (HashMap<String, Object>) adapter.getItem(info.position);
        if (codec == null) {
            // If for some reason the requested item isn't available, do nothing
            return;
        }
        
        boolean isDisabled = ((Short)codec.get(CODEC_PRIORITY) == 0);
        menu.add(0, MENU_ITEM_ACTIVATE, 0, isDisabled?"Activate":"Deactivate");
        
	}
	
	@SuppressWarnings("unchecked")
	public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        
        HashMap<String, Object> codec = null;
        codec = (HashMap<String, Object>) adapter.getItem(info.position);
        
        if (codec == null) {
            // If for some reason the requested item isn't available, do nothing
            return false;
        }
        
        switch (item.getItemId()) {
		case MENU_ITEM_ACTIVATE: {
			boolean isDisabled = ((Short) codec.get(CODEC_PRIORITY) == 0);
			
			final short newPrio = isDisabled ? (short) 1 : (short) 0;
			
			if(nonFreeCodecs.containsKey(codec.get(CODEC_ID)) && isDisabled) {
				final HashMap<String, Object> fCodec = codec;

				final TextView message = new TextView(this);
				final SpannableString s = new SpannableString(getString(R.string.this_codec_is_not_free) + nonFreeCodecs.get(codec.get(CODEC_ID)));
				Linkify.addLinks(s, Linkify.WEB_URLS);
				message.setText(s);
				message.setMovementMethod(LinkMovementMethod.getInstance());
				message.setPadding(10, 10, 10, 10);
				  
				
				//Alert user that we will disable for all incoming calls as he want to quit
				new AlertDialog.Builder(this)
					.setTitle(R.string.warning)
					.setView(message)
					.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							setCodecActivated(fCodec, newPrio);
						}
					})
					.setNegativeButton(R.string.cancel, null)
					.show();
			}else {
				setCodecActivated(codec, newPrio);
			}
			
			return true;
		}
		}
        return false;
    }
	
	private void setCodecActivated(HashMap<String, Object> codec, short newPrio) {
		codec.put(CODEC_PRIORITY, newPrio);
		prefsWrapper.setCodecPriority((String) codec.get(CODEC_ID), bandtype, Short.toString(newPrio));
		
		Collections.sort(codecs, codecsComparator);

		((SimpleAdapter) adapter).notifyDataSetChanged();
	}


	@Override
	public void onClick(View v) {
		
		
		if (v.getId() == R.id.tg_narrow_band) {
			wbToggle.setChecked(!nbToggle.isChecked());
			bandtype = wbToggle.isChecked() ? SipConfigManager.CODEC_WB : SipConfigManager.CODEC_NB;
		}

		// If we are clicking on wide band, set nbToggle check to correct value
		if (v.getId() == R.id.tg_wide_band) {
			nbToggle.setChecked(!wbToggle.isChecked());
			bandtype = nbToggle.isChecked() ? SipConfigManager.CODEC_NB : SipConfigManager.CODEC_WB;
		}
		Log.d(THIS_FILE, "We are now setting at codecs for "+bandtype);
		initDatas();
		((SimpleAdapter) adapter).notifyDataSetChanged();
	}
	
}
