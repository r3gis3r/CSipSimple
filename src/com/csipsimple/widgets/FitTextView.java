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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class FitTextView extends TextView {

    public FitTextView(Context context) {
        super(context);
        initialise();
    }

    public FitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    private void initialise() {
        testPaint = new Paint();
        testPaint.set(this.getPaint());
        //max size defaults to the intially specified text size unless it is too small
        maxTextSize = this.getTextSize();
        if (maxTextSize < 9) {
            maxTextSize = 23;
        }
        minTextSize = 8;
    }

    /* Re size the font so the specified text fits in the text box
     * assuming the text box is the specified width.
     */
	private void refitText(String text, int textWidth) {
		if (textWidth > 0) {
			int availableWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
			int trySize = (int) maxTextSize;
			int increment = ~(trySize - (int) minTextSize) / 2;

			testPaint.setTextSize(trySize);
			while ((trySize > minTextSize) && (testPaint.measureText(text) > availableWidth)) {
				trySize += increment;
				increment = (increment == 0) ? -1 : ~increment / 2;
				if (trySize <= minTextSize) {
					trySize = (int) minTextSize;
					break;
				}
				testPaint.setTextSize(trySize);
			}

			this.setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize);
		}
	}

    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
      //  int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        refitText(this.getText().toString(), parentWidth);
      //  this.setMeasuredDimension(parentWidth, parentHeight);
    }



    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        refitText(text.toString(), this.getWidth());
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        if (w != oldw) {
            refitText(this.getText().toString(), w);
        }
    }

    //Getters and Setters
    public float getMinTextSize() {
        return minTextSize;
    }

    public void setMinTextSize(int minTextSize) {
        this.minTextSize = minTextSize;
    }

    public float getMaxTextSize() {
        return maxTextSize;
    }

    public void setMaxTextSize(int minTextSize) {
        this.maxTextSize = minTextSize;
    }

    //Attributes
    private Paint testPaint;
    private float minTextSize;
    private float maxTextSize;

}
