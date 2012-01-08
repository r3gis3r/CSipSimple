/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * 
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

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.media.ToneGenerator;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.csipsimple.R;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.Theme;

public class Dialpad extends LinearLayout implements OnClickListener {

	private OnDialKeyListener onDialKeyListener;
	private final static String THIS_FILE = "Dialpad";
	
	private static final Map<Integer, int[]> DIGITS_BTNS = new HashMap<Integer, int[]>();
	
	static {
		DIGITS_BTNS.put(R.id.button0, new int[] {ToneGenerator.TONE_DTMF_0, KeyEvent.KEYCODE_0});
		DIGITS_BTNS.put(R.id.button1, new int[] {ToneGenerator.TONE_DTMF_1, KeyEvent.KEYCODE_1});
		DIGITS_BTNS.put(R.id.button2, new int[] {ToneGenerator.TONE_DTMF_2, KeyEvent.KEYCODE_2});
		DIGITS_BTNS.put(R.id.button3, new int[] {ToneGenerator.TONE_DTMF_3, KeyEvent.KEYCODE_3});
		DIGITS_BTNS.put(R.id.button4, new int[] {ToneGenerator.TONE_DTMF_4, KeyEvent.KEYCODE_4});
		DIGITS_BTNS.put(R.id.button5, new int[] {ToneGenerator.TONE_DTMF_5, KeyEvent.KEYCODE_5});
		DIGITS_BTNS.put(R.id.button6, new int[] {ToneGenerator.TONE_DTMF_6, KeyEvent.KEYCODE_6});
		DIGITS_BTNS.put(R.id.button7, new int[] {ToneGenerator.TONE_DTMF_7, KeyEvent.KEYCODE_7});
		DIGITS_BTNS.put(R.id.button8, new int[] {ToneGenerator.TONE_DTMF_8, KeyEvent.KEYCODE_8});
		DIGITS_BTNS.put(R.id.button9, new int[] {ToneGenerator.TONE_DTMF_9, KeyEvent.KEYCODE_9});
		DIGITS_BTNS.put(R.id.buttonpound, new int[] {ToneGenerator.TONE_DTMF_P, KeyEvent.KEYCODE_POUND});
		DIGITS_BTNS.put(R.id.buttonstar, new int[] {ToneGenerator.TONE_DTMF_S, KeyEvent.KEYCODE_STAR});
	};
	
	private static final Map<Integer, String> DIGITS_NAMES = new HashMap<Integer, String>();
	static {
		DIGITS_NAMES.put(R.id.button0, "0");
		DIGITS_NAMES.put(R.id.button1, "1");
		DIGITS_NAMES.put(R.id.button2, "2");
		DIGITS_NAMES.put(R.id.button3, "3");
		DIGITS_NAMES.put(R.id.button4, "4");
		DIGITS_NAMES.put(R.id.button5, "5");
		DIGITS_NAMES.put(R.id.button6, "6");
		DIGITS_NAMES.put(R.id.button7, "7");
		DIGITS_NAMES.put(R.id.button8, "8");
		DIGITS_NAMES.put(R.id.button9, "9");
		DIGITS_NAMES.put(R.id.buttonpound, "pound");
		DIGITS_NAMES.put(R.id.buttonstar, "star");
	};
	
	/**
	 * Interface definition for a callback to be invoked when a tab is triggered
	 * by moving it beyond a target zone.
	 */
	public interface OnDialKeyListener {
		
		/**
		 * Called when the user make an action
		 * 
		 * @param keyCode keyCode pressed
		 * @param dialTone corresponding dialtone
		 */
		void onTrigger(int keyCode, int dialTone);
	}
	
	public Dialpad(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.dialpad, this, true);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		for(int buttonId : DIGITS_BTNS.keySet()) {
			ImageButton button = (ImageButton) findViewById(buttonId);
			button.setOnClickListener(this);
		}
		
	}
	
	
	/**
	 * Registers a callback to be invoked when the user triggers an event.
	 * 
	 * @param listener
	 *            the OnTriggerListener to attach to this view
	 */
	public void setOnDialKeyListener(OnDialKeyListener listener) {
		onDialKeyListener = listener;
	}

	private void dispatchDialKeyEvent(int buttonId) {
		if (onDialKeyListener != null && DIGITS_BTNS.containsKey(buttonId)) {
			int[] datas = DIGITS_BTNS.get(buttonId);
			onDialKeyListener.onTrigger(datas[1], datas[0]);
		}
	}

	@Override
	public void onClick(View v) {
		int view_id = v.getId();
		dispatchDialKeyEvent(view_id);
		
	}
	

	public void applyTheme(Theme t) {
		Log.d(THIS_FILE, "Theming in progress");
		for(int buttonId : DIGITS_BTNS.keySet()) {
			
			ImageButton b = (ImageButton) findViewById(buttonId);
			
			// We have to reload for each button because std share state else
			Drawable pressed = t.getDrawableResource("btn_dial_pressed");
			Drawable focused = t.getDrawableResource("btn_dial_selected");
			if(focused == null) {
				focused = pressed;
			}
			Drawable normal = t.getDrawableResource("btn_dial_normal");
			StateListDrawable std = null;
			if(pressed != null && focused != null && normal != null) {
				std = new StateListDrawable();
				std.addState(new int[] {android.R.attr.state_pressed}, pressed);
				std.addState(new int[] {android.R.attr.state_focused}, focused);
				std.addState(new int[] {}, normal);
			}
			
			
			if(std != null) {
				b.setBackgroundDrawable(std);
			}
			Drawable src = t.getDrawableResource("dial_num_"+DIGITS_NAMES.get(buttonId));
			if(src != null) {
				b.setImageDrawable(src);
			}
		}
		
	}

}
