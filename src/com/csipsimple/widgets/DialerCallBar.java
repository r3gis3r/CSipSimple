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

package com.csipsimple.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import com.csipsimple.R;

public class DialerCallBar extends LinearLayout implements OnClickListener, OnLongClickListener {

    public interface OnDialActionListener {
        /**
         * The make call button has been pressed
         */
        void placeCall();

        /**
         * The voice mail button has been pressed
         */
        void placeVMCall();
        /**
         * The delete button has been pressed
         */
        void deleteChar();
        /**
         * The delete button has been long pressed
         */
        void deleteAll();
    }

    private OnDialActionListener actionListener;

    public DialerCallBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.dialer_call_bar, this, true);
        findViewById(R.id.vmButton).setOnClickListener(this);
        findViewById(R.id.dialButton).setOnClickListener(this);
        findViewById(R.id.deleteButton).setOnClickListener(this);
        findViewById(R.id.deleteButton).setOnLongClickListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    /**
     * Set a listener for this widget actions
     * @param l the listener called back when some user action is done on this widget
     */
    public void setOnDialActionListener(OnDialActionListener l) {
        actionListener = l;
    }
    
    /**
     * Set the action buttons enabled or not
     */
    public void setEnabled(boolean enabled) {
        findViewById(R.id.dialButton).setEnabled(enabled);
        findViewById(R.id.deleteButton).setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        if (actionListener != null) {
            int viewId = v.getId();
            if (viewId == R.id.vmButton) {
                actionListener.placeVMCall();
            }else if(viewId == R.id.dialButton) {
                actionListener.placeCall();
            }else if(viewId == R.id.deleteButton) {
                actionListener.deleteChar();
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (actionListener != null) {
            int viewId = v.getId();
            if(viewId == R.id.deleteButton) {
                actionListener.deleteAll();
                v.setPressed(false);
                return true;
            }
        }
        return false;
    }
}
