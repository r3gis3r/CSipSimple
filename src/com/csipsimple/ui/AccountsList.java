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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.api.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.SipProfileJson;
import com.csipsimple.wizards.BasePrefsWizard;
import com.csipsimple.wizards.WizardChooser;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

public class AccountsList extends Activity implements OnItemClickListener {
	
	private DBAdapter database;
	private AccountAdapter adapter;
	
	private List<SipProfile> accountsList;
	private ListView accountsListView;
	private GestureDetector gestureDetector;
	
	private static final String THIS_FILE = "SIP AccountList";
	
	public static final int MENU_ITEM_ACTIVATE = Menu.FIRST;
	public static final int MENU_ITEM_MODIFY = Menu.FIRST+1;
	public static final int MENU_ITEM_DELETE = Menu.FIRST+2;
	public static final int MENU_ITEM_WIZARD = Menu.FIRST+3;
	

	
	private static final int CHOOSE_WIZARD = 0;
	private static final int REQUEST_MODIFY = CHOOSE_WIZARD + 1;
	private static final int CHANGE_WIZARD =  REQUEST_MODIFY + 1;
	
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
				clickAddAccount();
			}
		});
		
		
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
	protected void onStart() {
		super.onStart();
		Log.d(THIS_FILE, "Bind to service");
		//Bind to sip service
		bindService(new Intent(this, SipService.class), connection, Context.BIND_AUTO_CREATE);
		//And register to ua state events
		registerReceiver(registrationStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(THIS_FILE, "Unbind from service");
		try {
			unbindService(connection);
		}catch(Exception e) {
			//Just ignore that
		}
		service = null;
		try {
			unregisterReceiver(registrationStateReceiver);
		}catch(Exception e) {
			//Just ignore that
		}
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

        SipProfile account = (SipProfile) adapter.getItem(info.position);
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
        menu.add(0, MENU_ITEM_WIZARD, 0, R.string.choose_wizard);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(THIS_FILE, "bad menuInfo", e);
            return false;
        }
        SipProfile account = (SipProfile) adapter.getItem(info.position);
        
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
            	database.open();
        		database.deleteAccount(account);
        		database.close();
				reloadAsyncAccounts(account.id, 0);
                return true;
            }
            case MENU_ITEM_MODIFY : {
        			Intent it = new Intent(this, BasePrefsWizard.class);
        			it.putExtra(Intent.EXTRA_UID,  (int) account.id);
        			it.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
        			startActivityForResult(it, REQUEST_MODIFY);
        		return true;
            }
            case MENU_ITEM_ACTIVATE: {
            	account.active = ! account.active;
            	database.open();
            	database.updateAccount(account);
            	database.close();
				reloadAsyncAccounts(account.id, account.active?1:0);
				return true;
            }
            case MENU_ITEM_WIZARD:{
            	Intent it = new Intent(this, WizardChooser.class);
            	it.putExtra(Intent.EXTRA_UID, (int) account.id);
            	startActivityForResult(it, CHANGE_WIZARD);
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
    		for(SipProfile acc : accountsList){
    			adapter.add(acc);
    		}
    		adapter.notifyDataSetChanged();
    	}
    }
    
    
    private void clickAddAccount() {
    	startActivityForResult(new Intent(AccountsList.this, WizardChooser.class), CHOOSE_WIZARD);
    }
	

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		SipProfile account = adapter.getItem(position);
		Intent intent = new Intent(this, BasePrefsWizard.class);
		if(account.id != SipProfile.INVALID_ID) {
			intent.putExtra(Intent.EXTRA_UID,  (int) account.id);
		}
		intent.putExtra(SipProfile.FIELD_WIZARD, account.wizard);
		
		startActivityForResult(intent, REQUEST_MODIFY);
		
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
				if(data != null) {
					String wizardId = data.getStringExtra(WizardUtils.ID);
					if(wizardId != null) {
						Intent intent = new Intent(this, BasePrefsWizard.class);
						intent.putExtra(SipProfile.FIELD_WIZARD, wizardId);
						startActivityForResult(intent, REQUEST_MODIFY);
					}
				}
			}
			break;
		case REQUEST_MODIFY:
			if(resultCode == RESULT_OK){
				handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
				/*
				int accId = data.getIntExtra(Intent.EXTRA_UID, -1);
				if(accId != -1) {
					reloadAsyncAccounts(accId, 1);
				}else {
					reloadAsyncAccounts(null, 1);
				}
				*/
			}
			break;
		case CHANGE_WIZARD:
			if(resultCode == RESULT_OK) {
				if(data != null && data.getExtras() != null) {
					String wizardId = data.getStringExtra(WizardUtils.ID);
					int accountId = data.getIntExtra(Intent.EXTRA_UID, SipProfile.INVALID_ID);
					if(wizardId != null && accountId != SipProfile.INVALID_ID) {
						database.open();
						database.setAccountWizard(accountId, wizardId);
						database.close();
						handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
					}
				}
			}
			break;
		}
	}
	

	private abstract class OnServiceConnect{
		protected abstract void serviceConnected(); 
	}
	private OnServiceConnect onServiceConnect = null;
	private void reloadAsyncAccounts(final Integer accountId, final Integer renew) {
		//Force reflush accounts
		Log.d(THIS_FILE, "Reload async accounts "+accountId+" renew : "+renew);
		onServiceConnect = new OnServiceConnect() {
			@Override
			protected void serviceConnected() {
				if (service != null) {
					Log.d(THIS_FILE, "Will reload all accounts !");
					try {
						//Ensure sip service is started
						service.sipStart();
						
						if(accountId == null) {
							service.reAddAllAccounts();
						}else {
							service.setAccountRegistration(accountId, renew);
						}
					} catch (RemoteException e) {
						Log.e(THIS_FILE, "Impossible to reload accounts", e);
					}finally {
						Log.d(THIS_FILE, "> Need to update list !");
						handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
					}
				}
			}
		};
		
		Thread t = new Thread() {
			@Override
			public void run() {
				Log.d(THIS_FILE, "Would like to reload all accounts");
				if(service != null) {
					onServiceConnect.serviceConnected();
					onServiceConnect = null;
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
	
	
	class AccountAdapter extends ArrayAdapter<SipProfile> implements OnClickListener {
		Activity context;
		private HashMap<Integer, AccountStatusDisplay> cacheStatusDisplay;
		
		AccountAdapter(Activity context, List<SipProfile> list) {
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
	        final SipProfile account = getItem(position);
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
            if(wizardInfos != null) {
            	tagView.activeCheckbox.setBackgroundResource(wizardInfos.icon);
            }
		}


		@Override
		public void onClick(View view) {
			AccountListItemViews tagView = (AccountListItemViews) ((View)view.getTag()).getTag();
			
			final SipProfile account = getItem(tagView.accountPosition);
			if(account == null) {
				return;
			}
			tagView.activeCheckbox.toggle();
			
			
			boolean isActive = tagView.activeCheckbox.isChecked();
			
			//Update database and reload accounts
			database.open();
			database.setAccountActive(account.id, isActive);
			database.close();
		//	reloadAsyncAccounts(account.id, account.active?1:0);
			
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
			if(onServiceConnect != null) {
				Thread t = new Thread() {
					public void run() {
						onServiceConnect.serviceConnected();
						onServiceConnect = null;
					};
				};
				t.start();
			}
			handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
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
	public static final int ADD_MENU = Menu.FIRST + 1;
	public static final int REORDER_MENU = Menu.FIRST + 2;
	public static final int BACKUP_MENU = Menu.FIRST + 3;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, ADD_MENU, Menu.NONE, R.string.add_account).setIcon(android.R.drawable.ic_menu_add);
		menu.add(Menu.NONE, REORDER_MENU, Menu.NONE, R.string.reorder).setIcon(android.R.drawable.ic_menu_sort_by_size);
		menu.add(Menu.NONE, BACKUP_MENU, Menu.NONE, R.string.backup_restore).setIcon(android.R.drawable.ic_menu_save);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ADD_MENU:
			clickAddAccount();
			return true;
		case REORDER_MENU:
			startActivityForResult(new Intent(this, ReorderAccountsList.class), REQUEST_MODIFY);
			return true;
		case BACKUP_MENU:
			
			//Populate choice list
			List<String> items = new ArrayList<String>();
			items.add(getResources().getString(R.string.backup));
			final File backupDir = PreferencesWrapper.getConfigFolder();
			if(backupDir != null) {
				String[] filesNames = backupDir.list();
				for(String fileName : filesNames) {
					items.add(fileName);
				}
			}
			
			final String[] fItems = (String[]) items.toArray(new String[0]);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.backup_restore);
			builder.setItems(fItems, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	if(item == 0) {
			    		SipProfileJson.saveSipConfiguration(AccountsList.this);
			    	}else {
						File fileToRestore = new File(backupDir + File.separator + fItems[item]);
			    		SipProfileJson.restoreSipConfiguration(AccountsList.this, fileToRestore);
			    		reloadAsyncAccounts(null, null);
			    		handler.sendMessage(handler.obtainMessage(NEED_LIST_UPDATE));
			    	}
			    }
			});
			builder.setCancelable(true);
			AlertDialog backupDialog = builder.create();
			backupDialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
