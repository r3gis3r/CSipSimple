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
package com.csipsimple.utils.bluetooth;


import java.util.Set;

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

public class BluetoothUtils8 {

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
				manager.broadcastMediaChanged();
			}
		}
	};
	private Context context;
	private MediaManager manager;

	public BluetoothUtils8(Context aContext, MediaManager aManager) {
		context = aContext;
		manager = aManager;
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		context.registerReceiver(mediaStateReceiver , new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
	}
	
	public boolean canBluetooth() {
		// Detect if any bluetooth a device is available for call
		BluetoothAdapter mBluetoothAdapter = null;
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}catch(RuntimeException e) {
			Log.w(THIS_FILE, "Cant get default bluetooth adapter ", e);
		}
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			return false;
		}
		boolean hasConnectedDevice = false;
		//If bluetooth is on
		if(mBluetoothAdapter.isEnabled()) {
			
			//We get all bounded bluetooth devices
			// bounded is not enough, should search for connected devices....
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
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

		
		return hasConnectedDevice && audioManager.isBluetoothScoAvailableOffCall();
	}

	public void setBluetoothOn(boolean on) {
		if(on != audioManager.isBluetoothScoOn()) {
			if(on) {
				Log.d(THIS_FILE, "BT on >>>");
				audioManager.setBluetoothScoOn(true);
				audioManager.startBluetoothSco();
			}else {
				Log.d(THIS_FILE, "BT off >>>");
				audioManager.setBluetoothScoOn(false);
				audioManager.stopBluetoothSco();
			}
		}
	}
	
	public boolean isBluetoothOn() {
		return isBluetoothConnected;
	}

	public void destroy() {
		try {
			context.unregisterReceiver(mediaStateReceiver);
		}catch(Exception e) {
			Log.w(THIS_FILE, "Failed to unregister media state receiver",e);
		}
	}

}
