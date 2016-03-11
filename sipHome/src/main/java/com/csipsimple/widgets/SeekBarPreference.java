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
 * This file contains relicensed code from Apache copyright of 
 * Copyright (C) 2010 Matthew Wiggins 
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
	private static final String DB_SUFFIX = "dB";
    private double subdivision = 5;

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
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(valueText, params);

		seekBar = new SeekBar(context);
		seekBar.setOnSeekBarChangeListener(this);
		layout.addView(seekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist()) {
			value = getPersistedFloat(defaultValue);
		}
		
		applySeekBarValues();
		
		return layout;
	}
	
	
	private void applySeekBarValues() {
        if(DB_SUFFIX.equals(suffix)) {
            seekBar.setMax( (int) (2 *  max  * subdivision) );
        }else {
            seekBar.setMax(valueToProgressUnit(max));
	    }
        seekBar.setProgress(valueToProgressUnit(value));
	}

	
	private int valueToProgressUnit(float val) {
	    if(DB_SUFFIX.equals(suffix)) {
	        Log.d(THIS_FILE, "Value is " + val);
	        double dB = (10.0f * Math.log10(val));
	        return (int) ( (dB + max) * subdivision);
	    }
	    return (int)(val * subdivision);
	}
	
	private float progressUnitToValue(int pVal) {
        if(DB_SUFFIX.equals(suffix)) {
            Log.d(THIS_FILE, "Progress is " + pVal);
            double dB = pVal / subdivision - max;
            return (float) Math.pow(10, dB / 10.0f);
        }
	    
	    return (float) (pVal / subdivision);
	}
	
	private String progressUnitToDisplay(int pVal) {
        if(DB_SUFFIX.equals(suffix)) {
            return Float.toString((float) (pVal / subdivision - max));
        }
        return Float.toString((float) (pVal / subdivision));
	}
	
	@Override
	protected void onBindDialogView(View v) {
		super.onBindDialogView(v);
		applySeekBarValues();
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
		String t = progressUnitToDisplay(aValue);
		valueText.setText(suffix == null ? t : t.concat(suffix));
		if(fromTouch) {
    		value = progressUnitToValue(aValue);
    		Log.d(THIS_FILE, "Set ratio value " + value);
    		callChangeListener(Float.valueOf(value));
		}
	}

	public void onStartTrackingTouch(SeekBar seek) {
		// Interface unused implementation
	}

	public void onStopTrackingTouch(SeekBar seek) {
		// Interface unused implementation
	}
	
}
