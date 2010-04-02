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

import org.pjsip.pjsua.pjsip_inv_state;
import org.pjsip.pjsua.pjsip_status_code;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsua_call_info;

import com.csipsimple.service.SipService;
import com.csipsimple.service.UAStateReceiver;

import com.csipsimple.R;
import com.csipsimple.service.ISipService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class CallHandler extends Activity {
	private static String THIS_FILE = "SIP CALL HANDLER";
	
	
	private boolean prevent_from_ale_hack = true;
	/**
	 * Service bind flag
	 */
	private boolean m_servicedBind = false;
	
	private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			int call_id = -1;
			Bundle extras = intent.getExtras();
	        if(extras != null){
	        	call_id = extras.getInt("call_id",-1);
	        }
	        
	        Log.d(THIS_FILE, "BC recieve");
	        
	        if(call_id == mCall_id){
	        	updateUIFromCall();
	        }
		}
	};
	
	
    /**
     * The service connection inteface with our binded service
     * {@link http://code.google.com/android/reference/android/content/ServiceConnection.html}
     */
    private ServiceConnection m_connection = new ServiceConnection(){


		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			ISipService.Stub.asInterface(arg1);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			
		}
    	
    };
    
    private int mCall_id = -1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.callhandler);
        Log.d(THIS_FILE,"Creating call handler.....");
        m_servicedBind = bindService(new Intent(this, SipService.class), 
    			m_connection, Context.BIND_AUTO_CREATE);
        
        
        
        Bundle extras = getIntent().getExtras();
        if(extras != null){
        	mCall_id = extras.getInt("call_id",-1);
        	prevent_from_ale_hack = extras.getBoolean("prevent_ale_hack", true);
        }
        

        Log.d(THIS_FILE,"Creating call handler for "+mCall_id);
        
        registerReceiver(callStateReceiver, new IntentFilter(UAStateReceiver.UA_CALL_STATE_CHANGED));
        
        Button bt;
        bt = (Button) findViewById(R.id.take_call);
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pjsua.call_answer(mCall_id, pjsip_status_code.PJSIP_SC_OK.swigValue(), null, null);
			}
		});
		

		bt = (Button) findViewById(R.id.hangup);
		bt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pjsua.call_hangup(mCall_id, 0, null, null);
				
			}
		});
		
		updateUIFromCall();
		
    }
	
	private void updateUIFromCall(){
		pjsua_call_info info = new pjsua_call_info();
		pjsua.call_get_info(mCall_id, info);
		pjsip_inv_state call_state = info.getState();
		
		Log.d(THIS_FILE, "Update ui from call");
		
		Button bt;
		if(call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_INCOMING) ||
				call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_EARLY)
		){
			bt = (Button) findViewById(R.id.take_call);
			bt.setVisibility(View.VISIBLE);
			bt = (Button) findViewById(R.id.hangup);
			bt.setVisibility(View.VISIBLE);
			bt.setText("Decline");
			
		}else if(call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) || 
				call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_CALLING) || 
				call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_CONNECTING) ){
			bt = (Button) findViewById(R.id.take_call);
			bt.setVisibility(View.GONE);
			bt = (Button) findViewById(R.id.hangup);
			bt.setVisibility(View.VISIBLE);
			bt.setText("Hang up");
			
			//ALE specific
			if(!prevent_from_ale_hack){
				finish();
			}
			
			
		}else if(call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_NULL) || 
				call_state.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)){
			Log.i(THIS_FILE, "Disconnected here !!!");
			finish();
		}
	}
	
	
	@Override
	protected void onDestroy() {
		if(m_servicedBind){
			unbindService(m_connection);
		}
		
		unregisterReceiver(callStateReceiver);
		
		super.onDestroy();
	}
}
