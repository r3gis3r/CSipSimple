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
/**
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2011, The Android Open Source Project
 */

package com.csipsimple.widgets;

import com.csipsimple.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * An entry in the call log.
 */
public class CheckableListItemView extends LinearLayout implements Checkable {
    public CheckableListItemView(Context context) {
        super(context);
    }

    public CheckableListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
//
//    public CallLogListItemView(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//    }

    @Override
    public void requestLayout() {
        // We will assume that once measured this will not need to resize
        // itself, so there is no need to pass the layout request to the parent
        // view (ListView).
        forceLayout();
    }

    
    // Checkable behavior
	private boolean checked = false;
    
	@Override
	public boolean isChecked() {
		return checked;
	}
	
	@Override
	public void setChecked(boolean aChecked) {
		if(checked == aChecked) {
			return;
		}
		checked = aChecked;
		setBackgroundResource(checked? R.drawable.abs__list_longpressed_holo : R.drawable.transparent);
	}
	
	@Override
	public void toggle() {
		setChecked(!checked);
	}
}
