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

import com.actionbarsherlock.internal.utils.UtilityWrapper;
import com.csipsimple.R;
import com.csipsimple.utils.Log;

import java.lang.reflect.Method;

/**
 * EditText which suppresses IME show up.
 */
public class DigitsEditText extends EditText {

    private static final String THIS_FILE = "DigitsEditText";
    private Boolean isDigit = null;
    // private int baseInputType = InputType.TYPE_NULL;
    private Method showSoftInputOnFocus;

    public DigitsEditText(Context context) {
        this(context, null);
    }
    
    public DigitsEditText(Context context, AttributeSet attrs, int style) {
        this(context, attrs);
    }
    
    public DigitsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        try {
            showSoftInputOnFocus = UtilityWrapper.safelyGetSuperclassMethod(getClass(), "setShowSoftInputOnFocus", boolean.class);
        }catch(RuntimeException e) {
            Log.e(THIS_FILE, "Cannot find show soft input on focus method ", e);
        }
        setIsDigit(true, false);
    }
    
    public synchronized void setIsDigit(boolean isDigit, boolean autofocus) {
        if(this.isDigit == null || this.isDigit != isDigit) {
            this.isDigit = isDigit;
            reflexSetShowSoftInputOnFocus(!isDigit);
            if (isDigit) {
                setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.dialpad_digits_text_size));
            } else {
                setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
        }
        applyKeyboardShowHide(autofocus);
    }

    /*
    // @Override
    public boolean isTextSelectable() {
        if(isDigit == null) {
            return false;
        }
        return !isDigit;
    }
    */

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if(focused) {
            applyKeyboardShowHide(false);
        }else {
            final InputMethodManager imm = ((InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE));
            if(imm != null && imm.isActive(this)) {
                imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean ret = super.onTouchEvent(event);
        // Must be done after super.onTouchEvent()
        applyKeyboardShowHide(false);
        return ret;
    }
    
/*
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        boolean ret = false;
        if(!isDigit) {
            ret = super.requestFocus(direction, previouslyFocusedRect);
        }
        applyKeyboardShowHide(false);
        return ret;
    }
*/
    
    private void applyKeyboardShowHide(boolean autofocus) {
        final InputMethodManager imm = ((InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE));
        if (imm != null) {
            if(isDigit) {
                if(imm.isActive(this)) {
                    imm.hideSoftInputFromWindow(getApplicationWindowToken(), 0);
                }
            }else if(autofocus) {
                imm.showSoftInput(this, 0);
            }
        }
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
        applyKeyboardShowHide(false);
    }
    
    private void reflexSetShowSoftInputOnFocus(boolean show) {
        if(showSoftInputOnFocus != null) {
            UtilityWrapper.safelyInvokeMethod(showSoftInputOnFocus, this, show);
        }
    }
}
