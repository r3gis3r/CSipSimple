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
package com.csipsimple.ui;

import java.util.List;

import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.service.UAStateReceiver;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class OutgoingCallChooser extends ListActivity {
	
	private DBAdapter db;
	private AccAdapter ad;
	
	String number;
	
	private List<Account> accounts_list;
	
	private static final String THIS_FILE = "SIP OUTChoose";
	
	private ISipService m_service;
	private ServiceConnection m_connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			m_service = ISipService.Stub.asInterface(arg1);
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    };
    
    
    
   	private BroadcastReceiver regStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			updateList();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "Starting");
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			number = extras.getString("number");
		}
		
		
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outgoing_account_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

		// Fill accounts with currently avalaible accounts
		db = new DBAdapter(this);
		db.open();
		accounts_list = db.getListAccounts();
		db.close();
		// And set as adapter
		ad = new AccAdapter(this, accounts_list);
		setListAdapter(ad);

		// Inform the list we provide context menus for items
		getListView().setOnCreateContextMenuListener(this);

		LinearLayout add_row = (LinearLayout) findViewById(R.id.use_pstn_row);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(THIS_FILE, "Choosen : pstn");
				Intent intentMakePstnCall = new Intent(Intent.ACTION_CALL);
				intentMakePstnCall.setData(Uri.parse("tel:"+number+"#PSTN"));
				startActivity(intentMakePstnCall);
				finish();
			}
		});
		
		Intent sipServ = new Intent(this, SipService.class);
		
		//Start service and bind it
		startService(sipServ);
		bindService(sipServ, m_connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(UAStateReceiver.UA_REG_STATE_CHANGED));
	}
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(m_connection);
		unregisterReceiver(regStateReceiver);
	}


    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index "+position+" id "+id);
		
		Account cAcc = ad.getItem(position);
		Class<?> selected_class = WizardUtils.getWizardClass(cAcc.wizard);
		if(selected_class != null){
			
			
			if(m_service != null) {
				if(SipService.active_acc_map.containsKey(cAcc.id)) {
					pjsua_acc_info acc_info =  cAcc.getPjAccountInfo();
            		if(acc_info != null) {
            			pjsip_status_code status_code;
            			status_code = acc_info.getStatus();
            			if( status_code == pjsip_status_code.PJSIP_SC_OK ){
							try {
								m_service.makeCall(number);
								finish();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
            			}
            		}
				}
			}
		}
	}
	

	
	
	/**
	 * Flush and re-populate static list of account (static because should not exceed 3 or 4 accounts)
	 */
	private void updateList(){
		db.open();
		accounts_list = db.getListAccounts();
		ad.clear();
		for(Account acc : accounts_list){
			ad.add(acc);
		}
		db.close();
		ad.notifyDataSetChanged();
	}
	
	
	class AccAdapter extends ArrayAdapter<Account> {

		AccAdapter(Activity context, List<Account> list) {
			super(context, R.layout.choose_account_row, list);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			
			//Log.d(THIS_FILE, "try to do convertView :: "+position+" / "+getCount());
			//View v = super.getView(position, convertView, parent);
			View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.choose_account_row, parent, false);
            }
            
            v.setClickable(true);
            
            
	        Account acc = getItem(position);
	        //Log.d(THIS_FILE, "has account");
	        if (acc != null){
	            TextView tvObjet = (TextView)v.findViewById(R.id.AccTextView);
	            TextView tvsObjet = (TextView)v.findViewById(R.id.AccTextStatusView);
	            ImageView icoObjet = (ImageView)v.findViewById(R.id.wizard_icon);
	            
	            Log.d(THIS_FILE, "Is rendering "+acc.display_name);
	            tvObjet.setText(acc.display_name);
	            
	            int color = Color.argb(255, 100, 100, 100); //Default color for not added account
	            String status = "Not added";
	            //Set status according to what we can get from service
	            if(SipService.status_acc_map.containsKey(acc.id)){
	            	Log.d(THIS_FILE, "Has adding status");
	            	if(SipService.status_acc_map.get(acc.id) == pjsuaConstants.PJ_SUCCESS){
	            		
	            		status = "Not yet registered";
	            		color = Color.argb(255, 255, 255, 255);
	            		
	            		if(SipService.active_acc_map.containsKey(acc.id)){
		            		pjsua_acc_info acc_info =  acc.getPjAccountInfo();
		            		if(acc_info != null) {
		            			status = acc_info.getStatus_text().getPtr();
		            			pjsip_status_code status_code;
		            			status_code = acc_info.getStatus();
		            			if( status_code == pjsip_status_code.PJSIP_SC_OK ){
	
				            		if(acc_info.getExpires() > 0){
				            			//Green, account is available
				            			color = Color.argb(255, 63, 255, 0);
				            			v.setClickable(false);
				            		}else{
				            			color = Color.argb(255, 100, 100, 100); //Default color for not added account
				        	            status = "Unregistred";
				            		}
		            			}else{
		            				Log.d(THIS_FILE, "Status is "+status_code);
		            				if(status_code == pjsip_status_code.PJSIP_SC_PROGRESS ||
		            						status_code == pjsip_status_code.PJSIP_SC_TRYING){
		            					color = Color.argb(255, 255, 194, 0);
		            				}else{
		            					color = Color.argb(255, 255, 0, 0);
		            				}
			            		}
		            		}
	            		}
	            	}else{
	            		status = "Unable to register ! Check your configuration";
	            		color = 0xFF0000FF;
	            		color = Color.argb(255, 255, 15, 0);
	            	}
	            	
	            }
	            
	            //Update status label and color
	            tvsObjet.setText(status);
	            tvObjet.setTextColor(color);
	            
	            //Update account image
	            WizardInfo wizard_infos = WizardUtils.getWizardClassInfos(acc.wizard);
	            icoObjet.setImageResource(wizard_infos.icon);
	        }
	        
	        
	        return v;
	    }

	}

}
