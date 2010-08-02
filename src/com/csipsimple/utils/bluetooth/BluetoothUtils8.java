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


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;

public class BluetoothUtils8 {

//	private static final String THIS_FILE = "BT 8";
	private AudioManager audioManager;

	public BluetoothUtils8(Context context) {
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	
	public boolean canBluetooth() {
		// Detect if any bluetooth a device is available for call
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
			return false;
		}
		boolean hasBoundedDevice = false;
		//If bluetooth is on
		if(mBluetoothAdapter.isEnabled()) {
			/*
			//We get all bounded bluetooth devices
			// bounded is not enough, should search for connected devices....
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			for(BluetoothDevice device : pairedDevices) {
				Log.d(THIS_FILE, "BT : "+ device.getBondState()+" - "+device.getAddress()+" - "+device.getName());
				Log.d(THIS_FILE, "Can telephony : "+ device.getBluetoothClass().hasService(BluetoothClass.Service.TELEPHONY));
				Log.d(THIS_FILE, "Can audio : "+device.getBluetoothClass().hasService( BluetoothClass.Service.AUDIO));
				if( //device.getBondState() == BluetoothDevice.BOND_BONDED &&  // Inutil since getBoundedDevices... 
						device.getBluetoothClass().hasService(BluetoothClass.Service.TELEPHONY)) {
					//And if any can be used as a audio handset
					hasBoundedDevice = true;
				//	break;
				}
			}
			*/
			hasBoundedDevice = true;
		}

		
		return hasBoundedDevice && audioManager.isBluetoothScoAvailableOffCall();
	}

	public void setBluetoothOn(boolean on) {
		if(on) {
			audioManager.setBluetoothScoOn(true);
			audioManager.startBluetoothSco();
		}else {
			audioManager.stopBluetoothSco();
			audioManager.setBluetoothScoOn(false);
		}
	}

}
