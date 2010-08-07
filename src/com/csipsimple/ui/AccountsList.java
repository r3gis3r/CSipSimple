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

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.csipsimple.R;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.models.Account;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.wizards.WizardChooser;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class AccountsList extends Activity implements OnItemClickListener {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	private List<Account> accountsList;
	private ListView accountsListView;
	private GestureDetector gestureDetector;
	
	private static final String THIS_FILE = "SIP AccountList";
	
	public static final int MENU_ITEM_ACTIVATE = Menu.FIRST;
	public static final int MENU_ITEM_MODIFY = Menu.FIRST+1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST+2;
	

	
	private static final int CHOOSE_WIZARD = 0;
	private static final int REQUEST_MODIFY = CHOOSE_WIZARD+1;
	
	private static final int NEED_LIST_UPDATE = 1;
	private static final int UPDATE_LINE = 2;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Build window
		Window w = getWindow();
		w.requestFeature(Window.FEATURE_LEFT_ICON);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts_list);
		w.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_list_accounts);

		
		// Fill accounts with currently avalaible accounts
		updateList();
		
		accountsListView = (ListView) findViewById(R.id.account_list);
		
		accountsListView.setAdapter(adapter);
		accountsListView.setOnItemClickListener(this);
		accountsListView.setOnCreateContextMenuListener(this);
		

		//Add add row
		LinearLayout add_row = (LinearLayout) findViewById(R.id.add_account);
		add_row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(AccountsList.this, WizardChooser.class), CHOOSE_WIZARD);
			}
		});
		//Bind to sip service
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		//And register to ua state events
		registerReceiver(registrationStateReceiver, new IntentFilter(SipService.ACTION_SIP_REGISTRATION_CHANGED));
		
		//Add gesture detector
		gestureDetector = new GestureDetector(this, new BackGestureDetector());
		accountsListView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
	}
	
	
	 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(connection);
		unregisterReceiver(registrationStateReceiver);
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

        Account account = (Account) adapter.getItem(info.position);
        if (account == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        
        WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);

        // Setup the menu header
        menu.setHeaderTitle(account.display_name);
        menu.setHeaderIcon(wizardInfos.icon);

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_ACTIVATE, 0, account.active?R.string.deactivate_account:R.string.activate_account);
        menu.add(0, MENU_ITEM_MODIFY, 0, R.string.modify_account);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.delete_account);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        Account account = (Account) adapter.getItem(info.position);
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
            	database.open();
        		database.deleteAccount(account);
        		database.close();
				reloadAsyncAccounts();
                return true;
            }
            case MENU_ITEM_MODIFY : {
            	WizardInfo wizard = WizardUtils.getWizardClass(account.wizard);
        		if(wizard != null){
        			
        			Intent it = new Intent(this, wizard.implementation);
        			it.putExtra(Intent.EXTRA_UID,  (int) account.id);
        			
        			startActivityForResult(it, REQUEST_MODIFY);
        		}
        		return true;
            }
            case MENU_ITEM_ACTIVATE: {
            	account.active = ! account.active;
            	database.open();
            	database.updateAccount(account);
            	database.close();
				reloadAsyncAccounts();
				return true;
            }
        }
        return false;
    }
    
    
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
    
    
    
	

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		Account account = adapter.getItem(position);
		WizardInfo wizard = WizardUtils.getWizardClass(account.wizard);
		if(wizard != null){
			
			Intent intent = new Intent(this, wizard.implementation);
			intent.putExtra(Intent.EXTRA_UID,  (int) account.id);
			
			startActivityForResult(intent, REQUEST_MODIFY);
		}
		
	}

	
	/**
	 * FOr now appears when we come back from a add/modify 
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode){
		case CHOOSE_WIZARD:
			if(resultCode == RESULT_OK) {
				if(data != null && data.getExtras() != null) {
					String wizard_id = data.getExtras().getString(WizardUtils.ID);
					if(wizard_id != null) {
						WizardInfo wizard = WizardUtils.getWizardClass(wizard_id);
						startActivityForResult(new Intent(this, wizard.implementation), REQUEST_MODIFY);
					}
				}
			}
			break;
		case REQUEST_MODIFY:
			if(resultCode == RESULT_OK){
				reloadAsyncAccounts();
			}
			break;
		}
	}
	

	
	private void reloadAsyncAccounts() {
		//Force reflush accounts
		Thread t = new Thread() {
			@Override
			public void run() {
				if (service != null) {
					try {
						service.reAddAllAccounts();
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to reload accounts", e);
					}finally {
						handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
					}
				}
			};
		};
		t.start();
	}
	
	
	private static final class AccountListItemViews {
		TextView labelView;
		TextView statusView;
		View indicator;
		CheckBox activeCheckbox;
		ImageView barOnOff;
		int accountPosition;
	}
	
	
	class AccountAdapter extends ArrayAdapter<Account> implements OnClickListener {
		Activity context;
		private HashMap<Integer, AccountStatusDisplay> cacheStatusDisplay;
		
		AccountAdapter(Activity context, List<Account> list) {
			super(context, R.layout.accounts_list_item, list);
			this.context = context;
			cacheStatusDisplay = new HashMap<Integer, AccountStatusDisplay>();
		}
		
		@Override
		public void notifyDataSetChanged() {
			cacheStatusDisplay.clear();
			super.notifyDataSetChanged();
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			//Create view if not existant
			View view = convertView;
            if (view == null) {
                LayoutInflater viewInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = viewInflater.inflate(R.layout.accounts_list_item, parent, false);
                
                AccountListItemViews tagView = new AccountListItemViews();
                tagView.labelView = (TextView)view.findViewById(R.id.AccTextView);
                tagView.indicator = view.findViewById(R.id.indicator);
                tagView.activeCheckbox = (CheckBox)view.findViewById(R.id.AccCheckBoxActive);
                tagView.statusView =  (TextView) view.findViewById(R.id.AccTextStatusView);
                tagView.barOnOff = (ImageView) tagView.indicator.findViewById(R.id.bar_onoff);
                
                view.setTag(tagView);
                
                tagView.indicator.setOnClickListener(this);
                tagView.indicator.setTag(view);
            }
            
            
            bindView(view, position);
           
	        return view;
	        
	    }
		
		
		public void bindView(View view, int position) {
			AccountListItemViews tagView = (AccountListItemViews) view.getTag();
			tagView.accountPosition = position;
			view.setTag(tagView);
			
			
			// Get the view object and account object for the row
	        final Account account = getItem(position);
	        if (account == null){
	        	return;
	        }
	        AccountStatusDisplay accountStatusDisplay = null;
			accountStatusDisplay = (AccountStatusDisplay) cacheStatusDisplay.get(position);
			if(accountStatusDisplay == null) {
				//In an ideal world, should be threaded
				accountStatusDisplay = AccountListUtils.getAccountDisplay(context, service, account.id);
				cacheStatusDisplay.put(position, accountStatusDisplay);
			}
			
			tagView.labelView.setText(account.display_name);
            
            //Update status label and color
			tagView.statusView.setText(accountStatusDisplay.statusLabel);
			tagView.labelView.setTextColor(accountStatusDisplay.statusColor);
            
            //Update checkbox selection
			tagView.activeCheckbox.setChecked( account.active );
			tagView.barOnOff.setImageResource( account.active ? accountStatusDisplay.checkBoxIndicator : R.drawable.ic_indicator_off );
            
            //Update account image
            final WizardInfo wizardInfos = WizardUtils.getWizardClass(account.wizard);
            tagView.activeCheckbox.setBackgroundResource(wizardInfos.icon);
	        
		}


		@Override
		public void onClick(View view) {
			AccountListItemViews tagView = (AccountListItemViews) ((View)view.getTag()).getTag();
			
			final Account account = getItem(tagView.accountPosition);
			if(account == null) {
				return;
			}
			tagView.activeCheckbox.toggle();
			boolean isActive = tagView.activeCheckbox.isChecked();
			
			//Update database and reload accounts
			database.open();
			account.active = isActive;
			database.updateAccount(account);
			database.close();
			reloadAsyncAccounts();
			
			//Update visual
			tagView.barOnOff.setImageResource(account.active?R.drawable.ic_indicator_on : R.drawable.ic_indicator_off);
			
			
		}

	}
	
	

	// Service connection
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
    
   	private BroadcastReceiver registrationStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			//Log.d(THIS_FILE, "Received a registration update");
			updateList();
		}
	};
	
	// Ui handler
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NEED_LIST_UPDATE:
				updateList();
				break;
			case UPDATE_LINE:
				
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	// Gesture detector
	private class BackGestureDetector extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if(e1 == null || e2 == null) {
				return false;
			}
			float deltaX = e2.getX() - e1.getX();
			float deltaY = e2.getY() - e1.getY();
			
			if(deltaX > 0 && deltaX > Math.abs(deltaY * 3) ) {
				finish();
				return true;
			}
			return false;
		}
	}

}
