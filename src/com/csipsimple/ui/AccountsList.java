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

import java.util.List;

import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_info;

import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.service.SipService;
import com.csipsimple.service.UAStateReceiver;
import com.csipsimple.wizards.AddAccountWizard;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.csipsimple.R;
import com.csipsimple.service.ISipService;

public class AccountsList extends ListActivity {
	
	private DBAdapter db;
	private CheckAdapter ad;
	
	private List<Account> accounts_list;
	
	private static final String THIS_FILE = "SIP AccountList";
	
	
	public static final int MENU_ITEM_DELETE = Menu.FIRST;

	
	private static final int REQUEST_ADD = 0;
	private static final int REQUEST_MODIFY = REQUEST_ADD+1;
	
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
			/*
			int call_id = -1;
			Bundle extras = intent.getExtras();
	        if(extras != null){
	        	call_id = extras.getInt("call_id",-1);
	        }
	        */
			updateList();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(THIS_FILE, "Starting");

		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.account_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

		// Fill accounts with currently avalaible accounts
		db = new DBAdapter(this);
		db.open();
		accounts_list = db.getListAccounts();
		db.close();
		// And set as adapter
		ad = new CheckAdapter(this, accounts_list);
		setListAdapter(ad);

		// Inform the list we provide context menus for items
		getListView().setOnCreateContextMenuListener(this);

		LinearLayout add_row = (LinearLayout) findViewById(R.id.add_row);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(AccountsList.this, AddAccountWizard.class), REQUEST_ADD);
			}
		});
		bindService(new Intent(this, SipService.class), m_connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(UAStateReceiver.UA_REG_STATE_CHANGED));
	}
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(m_connection);
		unregisterReceiver(regStateReceiver);
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return;
        }

        Account cAcc = (Account) getListAdapter().getItem(info.position);
        if (cAcc == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cAcc.display_name);

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_DELETE, 0, "Delete");
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        Account cAcc = (Account) getListAdapter().getItem(info.position);
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
            	db.open();
        		db.deleteAccount((int) cAcc.id);
        		accounts_list = db.getListAccounts();
        		ad.clear();
        		for(Account acc : accounts_list){
        			ad.add(acc);
        		}
        		db.close();
        		
        		ad.notifyDataSetChanged();

                return true;
            }
        }
        return false;
    }
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index "+position+" id "+id);
		
		Account cAcc = ad.getItem(position);
		Class<?> selected_class = WizardUtils.getWizardClass(cAcc.wizard);
		if(selected_class != null){
			
			Intent it = new Intent(this, selected_class);
			it.putExtra(Intent.EXTRA_UID,  (int) cAcc.id);
			
			startActivityForResult(it, REQUEST_MODIFY);
		}
	}

	
	/**
	 * FOr now appears when we come back from a add/modify 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		Log.d(THIS_FILE, "Well this is done i get a result....");
		
		switch(requestCode){
		case REQUEST_ADD:
			if(resultCode == RESULT_OK){
				updateList();
			}
		case REQUEST_MODIFY:
			if(resultCode == RESULT_OK){
				updateList();
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
	
	
	class CheckAdapter extends ArrayAdapter<Account> {

		CheckAdapter(Activity context, List<Account> list) {
			super(context, R.layout.account_row, list);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			
			//Log.d(THIS_FILE, "try to do convertView :: "+position+" / "+getCount());
			//View v = super.getView(position, convertView, parent);
			View v = convertView;
			boolean should_attach_cb_listener = false;
            if (v == null) {
            	should_attach_cb_listener = true;
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.account_row, parent, false);
            }
            
            CheckBox cbLu = (CheckBox)v.findViewById(R.id.AccCheckBoxActive);
	        
	        Account acc = getItem(position);
	        //Log.d(THIS_FILE, "has account");
	        if (acc != null){
	            TextView tvObjet = (TextView)v.findViewById(R.id.AccTextView);
	            TextView tvsObjet = (TextView)v.findViewById(R.id.AccTextStatusView);
	            ImageView icoObjet = (ImageView)v.findViewById(R.id.wizard_icon);
	            
	            cbLu.setTag(acc.id);
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
		            		pjsua_acc_info acc_info = new pjsua_acc_info();
		            		pjsua.acc_get_info(SipService.active_acc_map.get(acc.id), acc_info);
	            			status = acc_info.getStatus_text().getPtr();
	            			pjsip_status_code status_code = acc_info.getStatus();
	            			
	            			if( status_code == pjsip_status_code.PJSIP_SC_OK ){

			            		if(acc_info.getExpires() > 0){
			            			color = Color.argb(255, 63, 255, 0);
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
	            	}else{
	            		status = "Unable to register ! Check your configuration";
	            		color = 0xFF0000FF;
	            		color = Color.argb(255, 255, 15, 0);
	            	}
	            	
	            }
	            
	            //Update status label and color
	            tvsObjet.setText(status);
	            tvObjet.setTextColor(color);
	            
	            //Update checkbox selection - Note : this will fire the onCheckClick listener
	            cbLu.setChecked( acc.active );
	            //Update account image
	            WizardInfo wizard_infos = WizardUtils.getWizardClassInfos(acc.wizard);
	            icoObjet.setImageResource(wizard_infos.icon);
	        }
	        
			if (should_attach_cb_listener) {
				cbLu.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// Log.d(THIS_FILE,
						// "Checked : "+isChecked+" tag : "+buttonView.getTag());
						db.open();
						Account acc = db.getAccount((Integer) buttonView.getTag());

						// This test is required to make sure when list is
						// updated / or just initialized
						// that service is not restarted
						if (acc.active != isChecked) {
							acc.active = isChecked;
							db.updateAccount(acc);
							db.close();
							// TODO : we should maybe do something more clever
							// than
							// remove and re-add all accounts
							Thread t = new Thread() {
								public void run() {
									if (m_service != null) {
										try {
											m_service.removeAllAccounts();
											m_service.addAllAccounts();
										} catch (RemoteException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								};
							};
							t.start();

						} else {
							db.close();
						}
					}
				});
			}
	        
	        return v;
	    }

	}

}
