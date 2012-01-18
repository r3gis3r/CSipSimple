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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;

public class QuickActionItem extends LinearLayout  {
   
    public QuickActionItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @see android.widget.ImageView#setImageDrawable(Drawable)
     */
    public void setImageDrawable(Drawable drawable) {
        ImageView im = ((ImageView)findViewById(R.id.quickaction_icon));
    	im.setImageDrawable(drawable);
    }
    
    /**
     * @see android.widget.ImageView#setImageResource(int)
     */
    public void setImageResource(int resId) {
        ImageView im = ((ImageView)findViewById(R.id.quickaction_icon));
        im.setImageResource(resId);
    }
    
    /**
     * @see android.widget.TextView#setText(CharSequence)
     */
    public void setText(CharSequence text) {
        ImageView im = ((ImageView)findViewById(R.id.quickaction_icon));
        TextView tv = ((TextView)findViewById(R.id.quickaction_text)); 
    	tv.setText(text);
    	im.setContentDescription(text);
    }
    
}