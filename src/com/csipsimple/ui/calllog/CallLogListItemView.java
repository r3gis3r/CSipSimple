/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.csipsimple.ui.calllog;

import com.csipsimple.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * An entry in the call log.
 */
public class CallLogListItemView extends LinearLayout implements Checkable {
    public CallLogListItemView(Context context) {
        super(context);
    }

    public CallLogListItemView(Context context, AttributeSet attrs) {
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
