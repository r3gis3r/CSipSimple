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

import com.csipsimple.service.MediaManager;

public class BluetoothUtils3 extends BluetoothWrapper {


	public boolean canBluetooth() {
		return false;
	}

	public void setBluetoothOn(boolean on) {
		// Do nothing
	}

	public boolean isBluetoothOn() {
		return false;
	}

	@Override
	public void setContext(Context context) {
		// Do nothing
	}

    public void setMediaManager(MediaManager aManager) {
     // Do nothing
    }

	@Override
	public void register() {
		//Do nothing
	}

	@Override
	public void unregister() {
		// Do nothing
	}

    @Override
	public boolean isBTHeadsetConnected() {
	    return false;
	}
}
