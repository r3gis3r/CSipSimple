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

import com.csipsimple.R;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class PasswordPreference extends EditTextPreference implements OnClickListener {

	private static final String THIS_FILE = "PasswordPreference";
	private CheckBox showPwdCheckbox;



	public PasswordPreference(Context context) {
		super(context, null);
	}

	public PasswordPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(THIS_FILE, "Create me....");
		
		showPwdCheckbox = new CheckBox(context);
		showPwdCheckbox.setText(R.string.show_password);
		showPwdCheckbox.setOnClickListener(this);
	}
	
	
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		Log.d(THIS_FILE, ">>> BINDING TO VIEW !!!");
		try {
			CheckBox checkbox = showPwdCheckbox;
			ViewParent oldParent = checkbox.getParent();
			if (oldParent != view) {
				if (oldParent != null) {
					((ViewGroup) oldParent).removeView(checkbox);
				}
			}
			
			ViewGroup container = (ViewGroup) view;
			if(Compatibility.isCompatible(8)) {
				container = (ViewGroup) container.getChildAt(0);
			}
			if (container != null) {
				container.addView(checkbox, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			}
		}catch(Exception e) {
			// Just do nothing in case weird ROM in use
			Log.w(THIS_FILE, "Unsupported device for enhanced password", e);
		}
	}

	@Override
	public void onClick(View view) {
		getEditText().setInputType(
				InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ? 
				InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
				InputType.TYPE_TEXT_VARIATION_PASSWORD));
	}
	
}
