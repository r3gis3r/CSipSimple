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
package com.csipsimple.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SlidingDrawer;


public class SlidingPanel extends SlidingDrawer {
	

	public SlidingPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		//Relayout handle
		final int width = r - l;
		final int height = b - t;
		
		
		final View handle = getHandle();
		
		int childWidth = handle.getMeasuredWidth();
		int childHeight = handle.getMeasuredHeight();
		
		int childLeft;
		int childTop;
		
		
		float density = getContent().getResources().getDisplayMetrics().density;
		
		childLeft = isOpened() ? 0 : width - childWidth;
		childTop = (height - childHeight - (int) (12 *  density) );
		
		handle.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
		
		
	}
}
