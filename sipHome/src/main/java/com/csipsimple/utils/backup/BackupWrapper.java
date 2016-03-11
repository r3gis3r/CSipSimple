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

package com.csipsimple.utils.backup;

import android.content.Context;

import com.csipsimple.utils.Compatibility;

public abstract class BackupWrapper {
	private static BackupWrapper instance;
    protected Context context;
	
	public static BackupWrapper getInstance(Context context) {
		if(instance == null) {
		    if(Compatibility.isCompatible(8)) {
                instance = new com.csipsimple.utils.backup.BackupUtils8();
			}else {
                instance = new com.csipsimple.utils.backup.BackupUtils3();
			}
		    if(instance != null) {
		        instance.setContext(context);
		    }
		}
		
		return instance;
	}
	
	protected BackupWrapper() {}

	protected void setContext(Context ctxt) {
	    context = ctxt;
	}
	/**
	 * Notifies the Android backup system that your application wishes to back up new changes to its data. 
	 * A backup operation using your application's BackupAgent subclass will be scheduled when you call this method. 
	 */
	public abstract void dataChanged();
}
