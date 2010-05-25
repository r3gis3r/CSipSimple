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
import java.util.List;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

public class MainPrefs extends ListActivity {
	
	private static final String THIS_FILE = "Main prefs";
	private PrefGroupAdapter adapter;
	private ServiceConnection restartServiceConnection;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		List<PrefGroup> prefs_list = new ArrayList<PrefGroup>();
		prefs_list.add(new PrefGroup(R.string.prefs_fast, R.string.prefs_fast_desc, 
				R.drawable.ic_prefs_fast, new Intent(this, PrefsFast.class)));
		prefs_list.add(new PrefGroup(R.string.prefs_network, R.string.prefs_network_desc, 
				R.drawable.ic_prefs_network, new Intent(this, PrefsNetwork.class)));
		prefs_list.add(new PrefGroup(R.string.prefs_media, R.string.prefs_media_desc, 
				R.drawable.ic_prefs_media, new Intent(this, PrefsMedia.class)));		
		prefs_list.add(new PrefGroup(R.string.prefs_ui, R.string.prefs_ui_desc, 
				R.drawable.ic_prefs_ui, new Intent(this, PrefsUI.class)));
		
		
		adapter = new PrefGroupAdapter(this, prefs_list);
		setListAdapter(adapter);
		
		getListView().setOnCreateContextMenuListener(this);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		//Restart SIP service -> Bind and when bound just restart
		restartServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				ISipService sipservice = ISipService.Stub.asInterface(service);
				try {
					sipservice.sipStop();
					sipservice.sipStart();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				unbindService(restartServiceConnection);
			}
		};
		Intent serviceIntent =  new Intent(this, SipService.class);
		bindService(serviceIntent, restartServiceConnection, 0);
		startService(serviceIntent);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.w(THIS_FILE, "Click at index "+position+" id "+id);
		super.onListItemClick(l, v, position, id);
		
		PrefGroup pref_gp = adapter.getItem(position);
		startActivity(pref_gp.intent);
		
	}
	
	class PrefGroup {
		public String title;
		public int icon;
		public String summary;
		public Intent intent;
		
		public PrefGroup(String title, String summary, int icon, Intent intent) {
			this.title = title;
			this.summary = summary;
			this.icon = icon;
			this.intent = intent;
		}
		
		public PrefGroup(int title_res, int summary_res, int icon, Intent intent) {
			this.title = getString(title_res);
			this.summary = getString(summary_res);
			this.icon = icon;
			this.intent = intent;
		}
	}
	
	class PrefGroupAdapter extends ArrayAdapter<PrefGroup>{

		public PrefGroupAdapter(Context context, List<PrefGroup> objects) {
			super(context, R.layout.icon_preference_screen, objects);
		}
		
	    public View getView(int position, View convertView, ViewGroup parent) {
			
			View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.icon_preference_screen, parent, false);
            }
            
            PrefGroup pref_gp = adapter.getItem(position);
            ImageView icon_view = (ImageView)v.findViewById(R.id.icon);
            
            TextView title_view = (TextView)v.findViewById(android.R.id.title);
            TextView summary_view = (TextView)v.findViewById(android.R.id.summary);
            icon_view.setImageResource(pref_gp.icon);
            title_view.setText(pref_gp.title);
            summary_view.setText(pref_gp.summary);
            
            return v;
	    }
		
	}
	
	
	
	
}
