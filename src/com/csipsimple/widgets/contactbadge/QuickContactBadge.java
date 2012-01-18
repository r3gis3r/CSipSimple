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

package com.csipsimple.widgets.contactbadge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

import java.lang.reflect.Constructor;

public class QuickContactBadge extends FrameLayout {

    private static final String THIS_FILE = "QuickContactBadgeCompat";
    private ContactBadgeContract badge;


    public QuickContactBadge(Context context) {
        this(context, null, 0);
    }

    public QuickContactBadge(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickContactBadge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        String className = "com.csipsimple.widgets.contactbadge.ContactBadge";
        if (Compatibility.isCompatible(5)) {
            className += "5";
        } else {
            className += "3";
        }

        try {
            Class<? extends ContactBadgeContract> wrappedClass = Class.forName(className)
                    .asSubclass(ContactBadgeContract.class);
            Constructor<? extends ContactBadgeContract> constructor = wrappedClass.getConstructor(
                    Context.class, AttributeSet.class, int.class, QuickContactBadge.class);
            badge = constructor.newInstance(context, attrs, defStyle, this);
        } catch (Exception e) {
            Log.e(THIS_FILE, "Problem when trying to load for compat mode");
        }
        if (badge != null) {
            ImageView imageView = badge.getImageView();
            LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT);
            addView(imageView, params);
        }

        setDrawable();
    }


    public ImageView getImageView() {
        if(badge != null) {
            return badge.getImageView();
        }
        return null;
    }

    public void assignContactUri(Uri uri) {
        if(badge != null) {
            badge.assignContactUri(uri);
        }
    }

    public enum ArrowPosition {
        LEFT,
        RIGHT,
        NONE
    };

    private ArrowPosition arrowPos = ArrowPosition.NONE;

    public void setPosition(ArrowPosition position) {
        arrowPos = position;
        setDrawable();
        invalidate();
    }

    private void setDrawable() {

        setWillNotDraw(arrowPos == ArrowPosition.NONE);
    }

    
    public void overlay(Canvas c, ImageView img) {
        super.onDraw(c);
        if (arrowPos != ArrowPosition.NONE) {
            
            int x_border = (arrowPos == ArrowPosition.LEFT) ? 0 : img.getWidth();
            int x_inside = x_border + ((arrowPos == ArrowPosition.LEFT) ? 1 : -1 ) * (int)(img.getWidth() * 0.2f);
            int y_top = (int) (img.getHeight() * 0.2f);
            int y_bottom = (int) (img.getHeight() * 0.6f);
            c.save();
            
            Path path = new Path();   
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(x_border, y_top);
            path.lineTo(x_inside, (y_top + y_bottom)/2);
            path.lineTo(x_border, y_bottom);
            path.lineTo(x_border, y_top);
            path.close();

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeWidth(0);
            paint.setColor(SipHome.USE_LIGHT_THEME ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);     
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setAntiAlias(true);

            
            c.drawPath(path, paint);
            c.restore();
        }
    }
    

}
