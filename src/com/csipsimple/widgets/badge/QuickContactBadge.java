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
package com.csipsimple.widgets.badge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.csipsimple.R;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

public class QuickContactBadge extends FrameLayout {
	
	private static final String THIS_FILE = "QuickContactBadgeCompat";
    private static final float CORNER_OFFSET = 1;
	private static Method assignContactUriMethod;
	private static Class<? extends ImageView> quickContactBadgeClass;
	
	private ImageView imageView;
	private Constructor<? extends ImageView> quickContactBadgeConstructor;
    private float mDensity;
	

	public QuickContactBadge(Context context) {
		this(context, null, 0);
	}
	
	public QuickContactBadge(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public QuickContactBadge(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		if(Compatibility.isCompatible(5)) {
	        try {
				initReflexion();
				imageView = quickContactBadgeConstructor.newInstance(context, attrs, defStyle);
			} catch (Exception e) {
				Log.e(THIS_FILE, "Can't create quick contact badge", e);
			} 
		}
		if(imageView == null){
			imageView = new ImageView(context, attrs, defStyle);
		}
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		addView(imageView, params);
		
		
		
        Resources r = getContext().getResources();
        mDensity = r.getDisplayMetrics().density;
        setDrawable();
	}



	private void initReflexion() throws NoSuchMethodException, ClassNotFoundException {
		if(quickContactBadgeClass == null) {
			quickContactBadgeClass = Class.forName("android.widget.QuickContactBadge").asSubclass(ImageView.class);
		}
		
		if(quickContactBadgeConstructor == null && quickContactBadgeClass != null) {
			quickContactBadgeConstructor = quickContactBadgeClass.getConstructor(Context.class, AttributeSet.class, int.class);
		}
		
		if(assignContactUriMethod == null && quickContactBadgeClass != null) {
			assignContactUriMethod = quickContactBadgeClass.getMethod("assignContactUri", Uri.class);
		}
	}

	public ImageView getImageView() {
		return imageView;
	}

	public void assignContactUri(Uri uri) {
		if(assignContactUriMethod != null) {
			try {
				assignContactUriMethod.invoke(imageView, uri);
			} catch (Exception e) {
				Log.e(THIS_FILE, "Error while calling reflexion on quick contact badge", e);
			}
		}
	}
	
	public enum ArrowPosition {
	    LEFT_UPPER,
        LEFT_MIDDLE,
        LEFT_LOWER,
        RIGHT_UPPER,
        RIGHT_MIDDLE,
        RIGHT_LOWER,
        BOTTOM_MIDDLE,
        NONE
	};
	
	private ArrowPosition arrowPos = ArrowPosition.NONE;
    private Drawable mDrawable = null;
    private int mDrawableIntrinsicWidth;
    private int mDrawableIntrinsicHeight;
	
    public void setPosition(ArrowPosition position) {
        arrowPos = position;
        setDrawable();
        invalidate();
    }
    

    private void setDrawable() {
        Resources r = getContext().getResources();

        switch (arrowPos) {
            case LEFT_UPPER:
            case LEFT_MIDDLE:
            case LEFT_LOWER:
                mDrawable  = r.getDrawable(R.drawable.msg_bubble_right);
                break;

            case RIGHT_UPPER:
            case RIGHT_MIDDLE:
            case RIGHT_LOWER:
                mDrawable = r.getDrawable(R.drawable.msg_bubble_left);
                break;
        }
        if(mDrawable != null) {
            mDrawableIntrinsicWidth = mDrawable.getIntrinsicWidth();
            mDrawableIntrinsicHeight = mDrawable.getIntrinsicHeight();
        }
        setWillNotDraw(mDrawable == null);
    }

    
    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if(mDrawable != null) {
            c.save();
            computeBounds(c);
            mDrawable.draw(c);
            c.restore();
        }
    }
    
    

    public float getCloseOffset() {
        return CORNER_OFFSET * mDensity; // multiply by density to get pixels
    }
    

    private void computeBounds(Canvas c) {
        final int left = 0;
        final int top = 0;
        final int right = getWidth();
        final int middle = right / 2;
        final int bottom = getHeight();

        final int cornerOffset = (int) getCloseOffset();

        switch (arrowPos) {
            case RIGHT_UPPER:
                mDrawable.setBounds(
                        right - mDrawableIntrinsicWidth,
                        top + cornerOffset,
                        right,
                        top + cornerOffset + mDrawableIntrinsicHeight);
                break;

            case LEFT_UPPER:
                mDrawable.setBounds(
                        left,
                        top + cornerOffset,
                        left + mDrawableIntrinsicWidth,
                        top + cornerOffset + mDrawableIntrinsicHeight);
                break;

            case BOTTOM_MIDDLE:
                int halfWidth = mDrawableIntrinsicWidth / 2;
                mDrawable.setBounds(
                        (int)(middle - halfWidth),
                        (int)(bottom - mDrawableIntrinsicHeight),
                        (int)(middle + halfWidth),
                        (int)(bottom));

                break;
        }
    }
    
}
