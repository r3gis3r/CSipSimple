/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.csipsimple.utils.bluetooth;


import java.util.Set;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothClass.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.csipsimple.service.MediaManager;
import com.csipsimple.utils.Log;

@TargetApi(8)
public class BluetoothUtils8 extends BluetoothWrapper {

	private static final String THIS_FILE = "BT8";
	private AudioManager audioManager;
	
	private boolean isBluetoothConnected = false;
	
	private BroadcastReceiver mediaStateReceiver = new BroadcastReceiver() {
		
        @Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(THIS_FILE, ">>> Bluetooth SCO state changed !!! ");
			if(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED.equals(action)) {
				int status = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR );
				Log.d(THIS_FILE, "Bluetooth sco state changed : " + status);
				if(status == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
					isBluetoothConnected = true;
				}else if(status == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
					isBluetoothConnected = false;
				}
				manager.setSoftwareVolume();
				manager.broadcastMediaChanged();
			}
		}
	};
	private Context context;
	private MediaManager manager;
	private BluetoothAdapter bluetoothAdapter;

	public void init(Context aContext, MediaManager aManager) {
		context = aContext;
		manager = aManager;
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		register();
		try {
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}catch(RuntimeException e) {
			Log.w(THIS_FILE, "Cant get default bluetooth adapter ", e);
		}
	}
	
	public boolean canBluetooth() {
		// Detect if any bluetooth a device is available for call
		if (bluetoothAdapter == null) {
		    // Device does not support Bluetooth
			return false;
		}
		boolean hasConnectedDevice = false;
		//If bluetooth is on
		if(bluetoothAdapter.isEnabled()) {
			
			//We get all bounded bluetooth devices
			// bounded is not enough, should search for connected devices....
			Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
			for(BluetoothDevice device : pairedDevices) {
				BluetoothClass bluetoothClass = device.getBluetoothClass();
                if (bluetoothClass != null) {
                	int deviceClass = bluetoothClass.getDeviceClass();
                	if(bluetoothClass.hasService(Service.RENDER) ||
                		deviceClass == Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                		deviceClass == Device.AUDIO_VIDEO_CAR_AUDIO ||
                		deviceClass == Device.AUDIO_VIDEO_HANDSFREE ) {
	                    	//And if any can be used as a audio handset
	                    	hasConnectedDevice = true;
	                    	break;
                	}
				}
			}
		}
		boolean retVal = hasConnectedDevice && audioManager.isBluetoothScoAvailableOffCall();
		Log.d(THIS_FILE, "Can I do BT ? "+retVal);
		return retVal;
	}

	public void setBluetoothOn(boolean on) {
		Log.d(THIS_FILE, "Ask for "+on+" vs "+audioManager.isBluetoothScoOn());
		if(on != /* audioManager.isBluetoothScoOn()*/ isBluetoothConnected) {
			if(on) {
				Log.d(THIS_FILE, "BT on >>>");
				audioManager.setBluetoothScoOn(true);
				audioManager.startBluetoothSco();
			}else {
				Log.d(THIS_FILE, "BT off >>>");
				audioManager.stopBluetoothSco();
                audioManager.setBluetoothScoOn(false);
			}
		}
	}
	
	public boolean isBluetoothOn() {
		return isBluetoothConnected;
	}
	
	public void register() {
		Log.d(THIS_FILE, "Register BT media receiver");
		context.registerReceiver(mediaStateReceiver , new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
	}

	public void unregister() {
		try {
			Log.d(THIS_FILE, "Unregister BT media receiver");
			context.unregisterReceiver(mediaStateReceiver);
		}catch(Exception e) {
			Log.w(THIS_FILE, "Failed to unregister media state receiver",e);
		}
		bluetoothAdapter = null;
	}


}
