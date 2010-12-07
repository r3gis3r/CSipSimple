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
import android.view.accessibility.AccessibilityManager;

import com.csipsimple.service.MediaManager;

public class Accessibility4 extends AccessibilityWrapper {

	private AccessibilityManager accessibilityManager;
	
	@Override
	public void init(Context context, MediaManager manager) {

		accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
	}

	@Override
	public boolean isEnabled() {
		return accessibilityManager.isEnabled();
	}

}
