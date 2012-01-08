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

package com.csipsimple.ui.dialpad;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.csipsimple.utils.Log;

/**
 * EditText which suppresses IME show up.
 */
public class DigitsEditText extends EditText {

    private boolean isDigit = true;

    // private int baseInputType = InputType.TYPE_NULL;

    public DigitsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        // baseInputType = getInputType();
        setIsDigit(isDigit, false);
    }

    public void setIsDigit(boolean isDigit, boolean autofocus) {
        this.isDigit = isDigit;
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));

        if (isDigit) {
            setInputType(InputType.TYPE_NULL /*
                                              * | InputType.
                                              * TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                              */);
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            if (imm != null && imm.isActive(this)) {
                imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
            }
        } else {
            setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            if (imm != null && autofocus) {
                imm.showSoftInput(this, 0);
            }
        }

    }

    // @Override
    public boolean isTextSelectable() {
        return !isDigit;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        Log.d("DigitsEditText", ">> " + imm.isActive(this));
        if (imm != null && imm.isActive(this) && isDigit) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null && imm.isActive(this) && isDigit) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean ret = super.onTouchEvent(event);
        // Must be done after super.onTouchEvent()
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null && imm.isActive(this) && isDigit) {
            imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
        }
        return ret;
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            // Since we're replacing the text every time we add or remove a
            // character, only read the difference. (issue 5337550)
            final int added = event.getAddedCount();
            final int removed = event.getRemovedCount();
            final int length = event.getBeforeText().length();
            if (added > removed) {
                event.setRemovedCount(0);
                event.setAddedCount(1);
                event.setFromIndex(length);
            } else if (removed > added) {
                event.setRemovedCount(1);
                event.setAddedCount(0);
                event.setFromIndex(length - 1);
            } else {
                return;
            }
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            // The parent EditText class lets tts read "edit box" when this View
            // has a focus, which
            // confuses users on app launch (issue 5275935).
            return;
        }
        super.sendAccessibilityEventUnchecked(event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Here we ensure that we hide the keyboard
        // Since this will be fired when virtual keyboard this will probably
        // blink but for now no better way were found to hide keyboard for sure
        setIsDigit(isDigit, false);
    }
}
