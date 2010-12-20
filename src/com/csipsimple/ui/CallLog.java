/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CallLog.Calls;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.db.AccountAdapter;
import com.csipsimple.db.DBAdapter;
import com.csipsimple.service.ISipService;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.Log;

public class CallLog extends ListActivity {
    private static final String THIS_FILE = "CallDetail";

    private TextView viewCallType;
    private ImageView viewCallTypeIcon;
    private TextView viewCallTime;
    private TextView viewCallDuration;
	private TextView viewCallBackNumber;
	private TextView viewCallBackType;

    private String number = null;

    LayoutInflater mInflater;
    Resources mResources;

	private DBAdapter database;

	private List<SipProfile> accountsList;

	private AccountAdapter adapter;
	

	private ISipService service;
	private ServiceConnection connection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			service = ISipService.Stub.asInterface(arg1);
			if(adapter != null) {
				adapter.updateService(service);
			}
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
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.call_detail);

        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        viewCallType = (TextView) findViewById(R.id.type);
        viewCallTypeIcon = (ImageView) findViewById(R.id.icon);
        viewCallTime = (TextView) findViewById(R.id.time);
        viewCallDuration = (TextView) findViewById(R.id.duration);
        viewCallBackType = (TextView) findViewById(R.id.callBackType);
        viewCallBackNumber = (TextView) findViewById(R.id.callBackNumber);
        
        Log.d(THIS_FILE, "We are created with "+getIntent().getExtras().getInt(Calls._ID));

        
		Intent sipService = new Intent(this, SipService.class);
		//Bind the service
		bindService(sipService, connection, Context.BIND_AUTO_CREATE);
		registerReceiver(regStateReceiver, new IntentFilter(SipManager.ACTION_SIP_REGISTRATION_CHANGED));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData(getIntent().getExtras().getInt(Calls._ID));
    }
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(connection);
		}catch(Exception e) {}
		try {
			unregisterReceiver(regStateReceiver);
		}catch(Exception e) {}
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                Log.d(THIS_FILE, "To be implemented");
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Update user interface with details of given call.
     */
    private void updateData(int logId) {
    	if(database == null) {
    		database = new DBAdapter(this);
    	}
    	
    	database.open();
    	
    	Log.i(THIS_FILE, "Getting info from "+logId);
    	
    	Cursor logCursor = database.getCallLog(logId);
        
        if (logCursor != null && logCursor.moveToFirst()) {
            // Read call log specifics
            number = logCursor.getString(DBAdapter.NUMBER_COLUMN_INDEX);
            long date = logCursor.getLong(DBAdapter.DATE_COLUMN_INDEX);
            long duration = logCursor.getLong(DBAdapter.DURATION_COLUMN_INDEX);
            int callType = logCursor.getInt(DBAdapter.CALL_TYPE_COLUMN_INDEX);

            // Pull out string in format [relative], [date]
            CharSequence dateClause = DateUtils.formatDateRange(this, date, date,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
            viewCallTime.setText(dateClause);

            // Set the duration
            if (callType == Calls.MISSED_TYPE) {
                viewCallDuration.setVisibility(View.GONE);
            } else {
                viewCallDuration.setVisibility(View.VISIBLE);
                viewCallDuration.setText(formatDuration(duration));
            }

            // Set the call type icon and caption
            String callText = null;
            switch (callType) {
                case Calls.INCOMING_TYPE:
                    viewCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_incoming_call);
                    viewCallType.setText(R.string.type_incoming);
                    callText = getString(R.string.callBack);
                    break;

                case Calls.OUTGOING_TYPE:
                    viewCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_outgoing_call);
                    viewCallType.setText(R.string.type_outgoing);
                    callText = getString(R.string.callAgain);
                    break;

                case Calls.MISSED_TYPE:
                    viewCallTypeIcon.setImageResource(R.drawable.ic_call_log_header_missed_call);
                    viewCallType.setText(R.string.type_missed);
                    callText = getString(R.string.returnCall);
                    break;
            }
            
            viewCallBackType.setText(callText);
            viewCallBackNumber.setText(number);
        }
        if(logCursor != null) {
        	logCursor.close();
        }
        database.close();
        
        updateList();
    }
    
    
    private synchronized void updateList() {
        //	Log.d(THIS_FILE, "We are updating the list");
        	if(database == null) {
        		database = new DBAdapter(this);
        	}
        	
        	database.open();
    		accountsList = database.getListAccounts(true);
    		database.close();
        	
        	if(adapter == null) {
        		adapter = new AccountAdapter(this, accountsList);
        		adapter.setNotifyOnChange(false);
        		setListAdapter(adapter);
        		if(service != null) {
        			adapter.updateService(service);
        		}
        	}else {
        		adapter.clear();
        		for(SipProfile acc : accountsList){
        			adapter.add(acc);
        		}
        		adapter.notifyDataSetChanged();
        	}
        }

    private String formatDuration(long elapsedSeconds) {
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;

        return getString(R.string.callDetailsDurationFormat, minutes, seconds);
    }



    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
		Log.d(THIS_FILE, "Click at index " + position + " id " + id);

		SipProfile account = adapter.getItem(position);
		if (service != null) {
			SipProfileState accountInfo;
			try {
				accountInfo = service.getSipProfileState(account.id);
			} catch (RemoteException e) {
				accountInfo = null;
			}
			if ( accountInfo != null && accountInfo.isValidForCall() ) {
				try {
					service.makeCall(number, account.id);
					finish();
				} catch (RemoteException e) {
					Log.e(THIS_FILE, "Unable to make the call", e);
				}
			}
			//TODO : toast for elses
		}
    }
}

