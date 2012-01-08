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
/**
 * The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.csipsimple.widgets;

import com.csipsimple.utils.Log;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	private static final String THIS_FILE = "SeekBarPrefs";

	private SeekBar seekBar;
	private TextView valueText;
	private final Context context;
	private final float defaultValue, max;
	private final String dialogMessage, suffix;
	
	private float value = 0.0f;

	public SeekBarPreference(Context aContext, AttributeSet attrs) {
		super(aContext, attrs);
		context = aContext;

		dialogMessage = attrs.getAttributeValue(ANDROID_NS, "dialogMessage");
		suffix = attrs.getAttributeValue(ANDROID_NS, "text");
		defaultValue = attrs.getAttributeFloatValue(ANDROID_NS, "defaultValue", 0.0f);
		max = attrs.getAttributeIntValue(ANDROID_NS, "max", 10);

	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		TextView splashText = new TextView(context);
		if (dialogMessage != null) {
			splashText.setText(dialogMessage);
		}
		layout.addView(splashText);

		valueText = new TextView(context);
		valueText.setGravity(Gravity.CENTER_HORIZONTAL);
		valueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(valueText, params);

		seekBar = new SeekBar(context);
		seekBar.setOnSeekBarChangeListener(this);
		layout.addView(seekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist()) {
			value = getPersistedFloat(defaultValue);
		}
		
		seekBar.setMax( (int)(max*10) );
		seekBar.setProgress( (int)(10*value) );
		
		return layout;
	}

	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		seekBar.setMax( (int) (10*max) );
		seekBar.setProgress( (int) (10 * value) );
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object aDefaultValue) {
		super.onSetInitialValue(restore, aDefaultValue);
		if (restore) {
			value = shouldPersist() ? getPersistedFloat(defaultValue) : 0;
		} else {
			value = (Float) aDefaultValue;
		}
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		Log.d(THIS_FILE, "Dialog is closing..."+positiveResult+" et "+shouldPersist());
		if(positiveResult && shouldPersist()) {
			Log.d(THIS_FILE, "Save : "+value);
			persistFloat(value);
		}
	}

	public void onProgressChanged(SeekBar seek, int aValue, boolean fromTouch) {
		String t = String.valueOf(aValue/10.0);
		valueText.setText(suffix == null ? t : t.concat(suffix));
		value = (float) (aValue / 10.0);
		callChangeListener(new Float(value));
	}

	public void onStartTrackingTouch(SeekBar seek) {
		// Interface unused implementation
	}

	public void onStopTrackingTouch(SeekBar seek) {
		// Interface unused implementation
	}
	
}
