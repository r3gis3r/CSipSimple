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

import android.content.Context;

import com.csipsimple.utils.Compatibility;

public abstract class BluetoothWrapper {
	
    public interface BluetoothChangeListener {
        void onBluetoothStateChanged(int status);
    }
    
    
	private static BluetoothWrapper instance;
    protected Context context;
    
    protected BluetoothChangeListener btChangesListener;
	
	public static BluetoothWrapper getInstance(Context context) {
		if(instance == null) {
		    if(Compatibility.isCompatible(14)) {
		        instance = new com.csipsimple.utils.bluetooth.BluetoothUtils14();
		    }else if(Compatibility.isCompatible(8)) {
                instance = new com.csipsimple.utils.bluetooth.BluetoothUtils8();
			}else {
                instance = new com.csipsimple.utils.bluetooth.BluetoothUtils3();
			}
		    if(instance != null) {
		        instance.setContext(context);
		    }
		}
		
		return instance;
	}
	
	protected BluetoothWrapper() {}

	protected void setContext(Context ctxt) {
	    context = ctxt;
	}
	
	public void setBluetoothChangeListener(BluetoothChangeListener l) {
	    btChangesListener = l;
	}
	
	public abstract boolean canBluetooth();
	public abstract void setBluetoothOn(boolean on);
	public abstract boolean isBluetoothOn();
	public abstract void register();
	public abstract void unregister();
	public abstract boolean isBTHeadsetConnected();
}
