/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.csipsimple.ui.calllog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.CallLog.Calls;
import android.util.AttributeSet;
import android.view.View;

import com.csipsimple.R;

import java.util.ArrayList;
import java.util.List;

/**
 * View that draws one or more symbols for different types of calls (missed
 * calls, outgoing etc). The symbols are set up horizontally. As this view
 * doesn't create subviews, it is better suited for ListView-recycling that a
 * regular LinearLayout using ImageViews.
 */
public class CallTypeIconsView extends View {
    private final List<Integer> mCallTypes;
    private final Resources mResources;
    private int mWidth;
    private int mHeight;

    public CallTypeIconsView(Context context) {
        this(context, null);
    }

    public CallTypeIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResources = new Resources(context);
        mCallTypes = new ArrayList<Integer>();
    }

    public void clear() {
        mCallTypes.clear();
        mWidth = 0;
        mHeight = 0;
        invalidate();
    }

    public void add(int callType) {
        mCallTypes.add(callType);

        final Drawable drawable = getCallTypeDrawable(callType);
        mWidth += drawable.getIntrinsicWidth() + mResources.iconMargin;
        mHeight = Math.max(mHeight, drawable.getIntrinsicHeight());
        invalidate();
    }

    public int getCount() {
        return mCallTypes.size();
    }

    public int getCallType(int index) {
        return mCallTypes.get(index);
    }

    private Drawable getCallTypeDrawable(int callType) {
        switch (callType) {
            case Calls.INCOMING_TYPE:
                return mResources.incoming;
            case Calls.OUTGOING_TYPE:
                return mResources.outgoing;
            case Calls.MISSED_TYPE:
                return mResources.missed;
            default:
                throw new IllegalArgumentException("invalid call type: " + callType);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int left = 0;
        for (Integer callType : mCallTypes) {
            final Drawable drawable = getCallTypeDrawable(callType);
            final int right = left + drawable.getIntrinsicWidth();
            drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            left = right + mResources.iconMargin;
        }
    }

    private static class Resources {
        public final Drawable incoming;
        public final Drawable outgoing;
        public final Drawable missed;
        public final int iconMargin;

        public Resources(Context context) {
            final android.content.res.Resources r = context.getResources();
            incoming = r.getDrawable(R.drawable.ic_call_incoming_holo_dark);
            outgoing = r.getDrawable(R.drawable.ic_call_outgoing_holo_dark);
            missed = r.getDrawable(R.drawable.ic_call_missed_holo_dark);
            iconMargin = r.getDimensionPixelSize(R.dimen.call_log_icon_margin);
        }
    }
}
