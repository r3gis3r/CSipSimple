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
package com.csipsimple.utils.accessibility;

import android.content.Context;

import com.csipsimple.service.MediaManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;

public abstract class AccessibilityWrapper {
	
	private static AccessibilityWrapper instance;
	
	public static AccessibilityWrapper getInstance() {
		if(instance == null) {
			String className = CustomDistribution.getRootPackage() + ".utils.accessibility.Accessibility";
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

	
	public abstract void init(Context context, MediaManager manager);
	public abstract boolean isEnabled();

}
