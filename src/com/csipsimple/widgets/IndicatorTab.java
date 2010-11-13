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
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

public class IndicatorTab extends LinearLayout {

	
	private static final String THIS_FILE = "IcTAB";
	private Context context;
	private ImageView icon;
	private TextView label;
	private String labelResource;
	private int iconResource;
	private int unselectedIconResource;

	public IndicatorTab(Context aContext, AttributeSet attrs) {
		super(aContext, attrs);
		context = aContext;
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.home_tab, this, true);
	//	setFocusable(true);


		icon = (ImageView) findViewById(R.id.photo);
		label = (TextView) findViewById(R.id.name);
		
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		setResources(labelResource, iconResource, unselectedIconResource);
		
	}
	
	@Override
	protected void drawableStateChanged() {
		if(icon != null) {
			int[] state = getDrawableState();
			if(StateSet.stateSetMatches(View.SELECTED_STATE_SET, state)) {
				icon.setImageResource(iconResource);
			}else {
				icon.setImageResource(unselectedIconResource);
			}
		}
		super.drawableStateChanged();
	}
	
	
	
	public void setResources(String aLabelResource, int aIconResource, int aUnselectedIconResource) {
		labelResource = aLabelResource;
		iconResource = aIconResource;
		unselectedIconResource = aUnselectedIconResource;
		if(label != null && icon != null) {
			label.setText(labelResource);
			icon.setImageResource(iconResource);
		}else {
			Log.e(THIS_FILE, "not found !!!");
		}
		drawableStateChanged();
	}
	
	
}
