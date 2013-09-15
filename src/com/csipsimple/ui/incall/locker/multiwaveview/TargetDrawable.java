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
 * Copyright (C) 2012 The Android Open Source Project
 */

package com.csipsimple.ui.incall.locker.multiwaveview;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TargetDrawable {

    public static final int[] STATE_ACTIVE =
            { android.R.attr.state_enabled, android.R.attr.state_active };
    public static final int[] STATE_INACTIVE =
            { android.R.attr.state_enabled, -android.R.attr.state_active };
    public static final int[] STATE_FOCUSED =
            { android.R.attr.state_enabled, -android.R.attr.state_active,
                android.R.attr.state_focused };

    private float mTranslationX = 0.0f;
    private float mTranslationY = 0.0f;
    private float mPositionX = 0.0f;
    private float mPositionY = 0.0f;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mAlpha = 1.0f;
    private Drawable mDrawable;
    private boolean mEnabled = true;
    private final int mResourceId;

    /* package */ static class DrawableWithAlpha extends Drawable {
        private float mAlpha = 1.0f;
        private Drawable mRealDrawable;
        public DrawableWithAlpha(Drawable realDrawable) {
            mRealDrawable = realDrawable;
        }
        public void setAlpha(float alpha) {
            mAlpha = alpha;
        }
        public float getAlpha() {
            return mAlpha;
        }
        public void draw(Canvas canvas) {
            mRealDrawable.setAlpha((int) Math.round(mAlpha * 255f));
            mRealDrawable.draw(canvas);
        }
        @Override
        public void setAlpha(int alpha) {
            mRealDrawable.setAlpha(alpha);
        }
        @Override
        public void setColorFilter(ColorFilter cf) {
            mRealDrawable.setColorFilter(cf);
        }
        @Override
        public int getOpacity() {
            return mRealDrawable.getOpacity();
        }
    }

    public TargetDrawable(Resources res, int resId) {
        mResourceId = resId;
        setDrawable(res, resId);
    }

    public void setDrawable(Resources res, int resId) {
        // Note we explicitly don't set mResourceId to resId since we allow the drawable to be
        // swapped at runtime and want to re-use the existing resource id for identification.
        Drawable drawable = resId == 0 ? null : res.getDrawable(resId);
        // Mutate the drawable so we can animate shared drawable properties.
        mDrawable = drawable != null ? drawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public TargetDrawable(TargetDrawable other) {
        mResourceId = other.mResourceId;
        // Mutate the drawable so we can animate shared drawable properties.
        mDrawable = other.mDrawable != null ? other.mDrawable.mutate() : null;
        resizeDrawables();
        setState(STATE_INACTIVE);
    }

    public void setState(int [] state) {
        if (mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) mDrawable;
            d.setState(state);
        }
    }

    public boolean hasState(int [] state) {
        if (mDrawable instanceof StateListDrawable) {
            //StateListDrawable d = (StateListDrawable) mDrawable;
            // TODO: this doesn't seem to work
            //return d.getStateDrawableIndex(state) != -1;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the drawable is a StateListDrawable and is in the focused state.
     *
     * @return
     */
    public boolean isActive() {
        if (mDrawable instanceof StateListDrawable) {
            StateListDrawable d = (StateListDrawable) mDrawable;
            int[] states = d.getState();
            for (int i = 0; i < states.length; i++) {
                if (states[i] == android.R.attr.state_focused) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this target is enabled. Typically an enabled target contains a valid
     * drawable in a valid state. Currently all targets with valid drawables are valid.
     *
     * @return
     */
    public boolean isEnabled() {
        return mDrawable != null && mEnabled;
    }

    /**
     * Makes drawables in a StateListDrawable all the same dimensions.
     * If not a StateListDrawable, then justs sets the bounds to the intrinsic size of the
     * drawable.
     */
    private void resizeDrawables() {
        
        if (mDrawable instanceof StateListDrawable) {
            List<int[]> possiblesStates = new ArrayList<int[]>();
            possiblesStates.add(STATE_ACTIVE);
            possiblesStates.add(STATE_INACTIVE);
            possiblesStates.add(STATE_FOCUSED);
            
            StateListDrawable d = (StateListDrawable) mDrawable;
            int maxWidth = 0;
            int maxHeight = 0;
            for (int[] possState : possiblesStates) {
                d.setState(possState);
                Drawable childDrawable = d.getCurrent();
                if(childDrawable != null) {
                    maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                    maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
                }
            }
            d.setBounds(0, 0, maxWidth, maxHeight);
            for (int[] possState : possiblesStates) {
                d.setState(possState);
                Drawable childDrawable = d.getCurrent();
                if(childDrawable != null) {
                    maxWidth = Math.max(maxWidth, childDrawable.getIntrinsicWidth());
                    maxHeight = Math.max(maxHeight, childDrawable.getIntrinsicHeight());
                }
            }
        } else if (mDrawable != null) {
            mDrawable.setBounds(0, 0,
                    mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
        }
    }

    public void setX(float x) {
        mTranslationX = x;
    }

    public void setY(float y) {
        mTranslationY = y;
    }

    public void setScaleX(float x) {
        mScaleX = x;
    }

    public void setScaleY(float y) {
        mScaleY = y;
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    public float getX() {
        return mTranslationX;
    }

    public float getY() {
        return mTranslationY;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public float getAlpha() {
        return mAlpha;
    }

    public void setPositionX(float x) {
        mPositionX = x;
    }

    public void setPositionY(float y) {
        mPositionY = y;
    }

    public float getPositionX() {
        return mPositionX;
    }

    public float getPositionY() {
        return mPositionY;
    }

    public int getWidth() {
        return mDrawable != null ? mDrawable.getIntrinsicWidth() : 0;
    }

    public int getHeight() {
        return mDrawable != null ? mDrawable.getIntrinsicHeight() : 0;
    }

    public void draw(Canvas canvas) {
        if (mDrawable == null || !mEnabled) {
            return;
        }
        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.scale(mScaleX, mScaleY, mPositionX, mPositionY);
        canvas.translate(mTranslationX + mPositionX, mTranslationY + mPositionY);
        canvas.translate(-0.5f * getWidth(), -0.5f * getHeight());
        mDrawable.setAlpha((int) Math.round(mAlpha * 255f));
        mDrawable.draw(canvas);
        canvas.restore();
    }

    public void setEnabled(boolean enabled) {
        mEnabled  = enabled;
    }

    public int getResourceId() {
        return mResourceId;
    }
}
