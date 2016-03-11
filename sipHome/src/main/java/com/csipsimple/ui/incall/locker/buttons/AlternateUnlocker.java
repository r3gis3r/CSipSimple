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
 * Copyright (C) 2008-2009 The Android Open Source Project
 */

package com.csipsimple.ui.incall.locker.buttons;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.ui.incall.locker.IOnLeftRightChoice;
import com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider;
import com.csipsimple.ui.incall.locker.IOnLeftRightChoice.TypeOfLock;
import com.csipsimple.ui.incall.locker.LeftRightChooserUtils;

import java.util.ArrayList;

public class AlternateUnlocker extends LinearLayout implements IOnLeftRightProvider, OnClickListener {

	private IOnLeftRightChoice onTriggerListener;

    public AlternateUnlocker(Context context) {
		this(context, null);
	}

	/**
	 * Constructor used when this widget is created from a layout file.
	 */
	public AlternateUnlocker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public AlternateUnlocker(Context context, AttributeSet attrs, int style) {
	    super(context, attrs);
	    setOrientation(HORIZONTAL);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.alternate_unlocker, this, true);
        
        
        View btn;
        btn = findViewById(R.id.takeCallButton);
        btn.setOnClickListener(this);
        btn = findViewById(R.id.dontTakeCallButton);
        btn.setOnClickListener(this);
	}

    @Override
    public void setOnLeftRightListener(IOnLeftRightChoice l) {
        onTriggerListener = l;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(onTriggerListener != null) {
            if (id == R.id.takeCallButton) {
                onTriggerListener.onLeftRightChoice(IOnLeftRightChoice.LEFT_HANDLE);
            } else if (id == R.id.dontTakeCallButton) {
                onTriggerListener.onLeftRightChoice(IOnLeftRightChoice.RIGHT_HANDLE);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider#applyTargetTitles(int)
     */
    @Override
    public void applyTargetTitles(int resArrayTitles) {
        ArrayList<String> strings = LeftRightChooserUtils.loadTargetsDescriptions(getContext(),
                resArrayTitles);
        ((TextView) findViewById(R.id.dontTakeCallButtonTxt)).setText(strings.get(0));
        ((TextView) findViewById(R.id.takeCallButtonTxt)).setText(strings.get(1));
        
    }

    /* (non-Javadoc)
     * @see com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider#setTypeOfLock(com.csipsimple.ui.incall.locker.IOnLeftRightChoice.TypeOfLock)
     */
    @Override
    public void setTypeOfLock(TypeOfLock lock) {
        if(lock == TypeOfLock.CALL) {
            applyTargetTitles(R.array.answer_choices);
        }        
    }

    /* (non-Javadoc)
     * @see com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider#getLayoutingHeight()
     */
    @Override
    public int getLayoutingHeight() {
        return LayoutParams.MATCH_PARENT;
    }

    /* (non-Javadoc)
     * @see com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider#getLayoutingHeight()
     */
    @Override
    public int getLayoutingWidth() {
        return LayoutParams.MATCH_PARENT;
    }

    /* (non-Javadoc)
     * @see com.csipsimple.ui.incall.locker.IOnLeftRightChoice.IOnLeftRightProvider#resetView()
     */
    @Override
    public void resetView() {
        // Nothing to do for this widget
    }



}
