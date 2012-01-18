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

package com.csipsimple.utils.accessibility;

import android.content.Context;

import com.csipsimple.utils.Compatibility;

public abstract class AccessibilityWrapper {
	
	private static AccessibilityWrapper instance;
	
	public static AccessibilityWrapper getInstance() {
		if(instance == null) {
			String className =  "com.csipsimple.utils.accessibility.Accessibility";
			if(Compatibility.isCompatible(4)) {
				className += "4";
			}else {
				className += "3";
			}
			try {
                Class<? extends AccessibilityWrapper> wrappedClass = Class.forName(className).asSubclass(AccessibilityWrapper.class);
                instance = wrappedClass.newInstance();
			} catch (Exception e) {
	        	throw new IllegalStateException(e);
	        }
		}
		
		return instance;
	}
	
	protected AccessibilityWrapper() {}

	
	public abstract void init(Context context);
	public abstract boolean isEnabled();

}
