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
import com.csipsimple.models.AccountInfo;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.OutgoingCall;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class OutgoingCallChooser extends ListActivity {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	String number;
	
	public final static int AUTO_CHOOSE_TIME = 8000;
	private List<Account> accountsList;

	private static final String THIS_FILE = "SIP OUTChoose";
	
	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
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
		updateList();
		setListAdapter(adapter);
		

		// Inform the list we provide context menus for items
	//	getListView().setOnCreateContextMenuListener(this);

		LinearLayout add_row = (LinearLayout) findViewById(R.id.use_pstn_row);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(THIS_FILE, "Choosen : pstn");
				OutgoingCall.ignoreNext = number;
				Intent intentMakePstnCall = new Intent(Intent.ACTION_CALL);
				intentMakePstnCall.setData(Uri.parse("tel:"+number));
				startActivity(intentMakePstnCall);
				finish();
			}
		});
		
		Intent sipService = new Intent(this, SipService.class);
		
		//Start service and bind it
		startService(sipService);
		bindService(sipService, connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(SipService.ACTION_SIP_REGISTRATION_CHANGED));
	}
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
		unregisterReceiver(regStateReceiver);
	}


    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index " + position + " id " + id);

		Account account = adapter.getItem(position);
		Class<?> selected_class = WizardUtils.getWizardClass(account.wizard);
		if (selected_class != null && service != null) {
			AccountInfo accountInfo;
			try {
				accountInfo = service.getAccountInfo(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if (accountInfo != null && accountInfo.isActive()) {
				if (accountInfo.getPjsuaId() >= 0 && accountInfo.getStatusCode() == pjsip_status_code.PJSIP_SC_OK) {
					try {
						service.makeCall(number, accountInfo.getPjsuaId());
						finish();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Unable to make the call", e);
					}
				}
			}
			//TODO : toast for elses
		}
	}
	

	
	
	
	/**
	 * Flush and re-populate static list of account (static because should not exceed 3 or 4 accounts)
	 */
    
    private synchronized void updateList() {
    //	Log.d(THIS_FILE, "We are updating the list");
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
    	database.open();
		accountsList = database.getListAccounts();
		database.close();
    	
    	if(adapter == null) {
    		adapter = new AccountAdapter(this, accountsList);
    		adapter.setNotifyOnChange(false);
    	}else {
    		adapter.clear();
    		for(Account acc : accountsList){
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }
    
	
	
	class AccountAdapter extends ArrayAdapter<Account> {

		AccountAdapter(Activity context, List<Account> list) {
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

            TextView labelView = (TextView)v.findViewById(R.id.AccTextView);
            TextView statusView = (TextView)v.findViewById(R.id.AccTextStatusView);
            ImageView iconImage = (ImageView)v.findViewById(R.id.wizard_icon);
            
	        Account account = getItem(position);
	        //Log.d(THIS_FILE, "has account");
	        if (account != null){
	            labelView.setText(account.display_name);
	            int color = Color.argb(255, 100, 100, 100); //Default color for not added account
	            String status = "Not added";
	            
				if (service != null) {
					AccountInfo accountInfo;
					try {
						accountInfo = service.getAccountInfo(account.id);
					} catch (RemoteException e) {
						accountInfo = null;
					}
					if (accountInfo != null && accountInfo.isActive()) {
						if (accountInfo.getAddedStatus() == pjsuaConstants.PJ_SUCCESS) {

							status = "Not yet registered";
							color = Color.argb(255, 255, 255, 255);

							if (accountInfo.getPjsuaId() >= 0) {
								status = accountInfo.getStatusText();
								pjsip_status_code statusCode = accountInfo.getStatusCode();
								if (statusCode == pjsip_status_code.PJSIP_SC_OK) {

									if (accountInfo.getExpires() > 0) {
										// Green, account is available
										color = Color.argb(255, 63, 255, 0);
										v.setClickable(false);
									} else {
										// Default color for  not added ccount
										color = Color.argb(255, 100, 100, 100);
										status = "Unregistred";
									}
								} else {
									if (statusCode == pjsip_status_code.PJSIP_SC_PROGRESS || statusCode == pjsip_status_code.PJSIP_SC_TRYING) {
										color = Color.argb(255, 255, 194, 0);
									} else {
										color = Color.argb(255, 255, 0, 0);
									}
								}
							}
						}
					} else {
						status = "Unable to register ! Check your configuration";
						color = 0xFF0000FF;
						color = Color.argb(255, 255, 15, 0);
					}
				}
	            
	            //Update status label and color
	            statusView.setText(status);
	            labelView.setTextColor(color);
	            
	            //Update account image
	            WizardInfo wizard_infos = WizardUtils.getWizardClassInfos(account.wizard);
	            iconImage.setImageResource(wizard_infos.icon);
	        }
	        
	        
	        return v;
	    }

	}

}
