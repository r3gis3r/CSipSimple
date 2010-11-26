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

import android.content.Context;

import com.csipsimple.service.MediaManager;
import com.csipsimple.utils.Compatibility;

public class BluetoothWrapper {
	private BluetoothUtils8 butils8;
	private BluetoothUtils7 butils7;

	/* class initialization fails when this throws an exception */
	static {
		try {
			Class.forName("com.csipsimple.utils.bluetooth.BluetoothUtils8");
		} catch (Exception ex) {
			try {
				Class.forName("com.csipsimple.utils.bluetooth.BluetoothUtils7");
			}catch (Exception ex2) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	public BluetoothWrapper(Context context, MediaManager manager) {
		if(Compatibility.isCompatible(8)) {
			butils8 = new BluetoothUtils8(context, manager);
		}else if(Compatibility.isCompatible(7)) {
			butils7 = new BluetoothUtils7(context, manager);
		}
	}

	/* calling here forces class initialization */
	public static void checkAvailable() {}

	public boolean canBluetooth() {
		if(butils8 != null) {
			return butils8.canBluetooth();
		}else if(butils7 != null) {
			return butils7.canBluetooth();
		}
		return false;
	}

	public void setBluetoothOn(boolean on) {
		if(butils8 != null) {
			butils8.setBluetoothOn(on);
		}else if(butils7 != null) {
			butils7.setBluetoothOn(on);
		}
	}
	
	public boolean isBluetoothOn() {
		if(butils8 != null) {
			return butils8.isBluetoothOn();
		}else if(butils7 != null) {
			return butils7.isBluetoothOn();
		}
		return false;
	}

	public void register() {
		if(butils8 != null) {
			butils8.register();
		}
	}
	
	
	public void unregister() {
		if(butils8 != null) {
			butils8.unregister();
		}
	}
}
