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
 * This file contains relicensed code from som Apache copyright of 
 * Copyright (C) 2011, The Android Open Source Project
 */

package com.csipsimple.ui.calllog;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipUri;

/**
 * Helper class to fill in the views in {@link PhoneCallDetailsViews}.
 */
public class PhoneCallDetailsHelper {
    /**
     * The maximum number of icons will be shown to represent the call types in
     * a group.
     */
    private static final int MAX_CALL_TYPE_ICONS = 3;

    private final Resources mResources;
    /**
     * The injected current time in milliseconds since the epoch. Used only by
     * tests.
     */
    private Long mCurrentTimeMillisForTest;

    /**
     * Creates a new instance of the helper.
     * <p>
     * Generally you should have a single instance of this helper in any
     * context.
     * 
     * @param resources used to look up strings
     */
    public PhoneCallDetailsHelper(Resources resources) {
        mResources = resources;
    }

    /** Fills the call details views with content. */
    public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details) {
        // Display up to a given number of icons.
        views.callTypeIcons.clear();
        int count = details.callTypes.length;
        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
            views.callTypeIcons.add(details.callTypes[index]);
        }
        views.callTypeIcons.setVisibility(View.VISIBLE);

        // Show the total call count only if there are more than the maximum
        // number of icons.
        Integer callCount;
        if (count > MAX_CALL_TYPE_ICONS) {
            callCount = count;
        } else {
            callCount = null;
        }

        // The date of this call, relative to the current time.
        CharSequence dateText =
                DateUtils.getRelativeTimeSpanString(details.date,
                        getCurrentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);

        // Set the call count and date.
        setCallCountAndDate(views, callCount, dateText);

        // Display number and display name
        CharSequence displayName;
        if (!TextUtils.isEmpty(details.name)) {
            displayName = details.name;
        } else {
            // Try to fallback on number treat
            if (!TextUtils.isEmpty(details.number)) {
                displayName = SipUri.getDisplayedSimpleContact(details.number.toString());
                // SipUri.getCanonicalSipContact(details.number.toString(),
                // false);
            } else {
                displayName = mResources.getString(R.string.unknown);
            }

            if (!TextUtils.isEmpty(details.numberLabel)) {
                SpannableString text = new SpannableString(details.numberLabel + " " + displayName);
                text.setSpan(new StyleSpan(Typeface.BOLD), 0, details.numberLabel.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                displayName = text;
            }
        }

        views.nameView.setText(displayName);
        if (!TextUtils.isEmpty(details.formattedNumber)) {
            views.numberView.setText(details.formattedNumber);
        } else if (!TextUtils.isEmpty(details.number)) {
            views.numberView.setText(details.number);
        } else {
            // In this case we can assume that display name was set to unknown
            views.numberView.setText(displayName);
        }
    }

    /** Sets the text of the header view for the details page of a phone call. */
    public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details) {
        CharSequence nameText;
        final CharSequence displayNumber = details.number;
        if (TextUtils.isEmpty(details.name)) {
            nameText = displayNumber;
        } else {
            nameText = details.name;
        }

        nameView.setText(nameText);
    }

    public void setCurrentTimeForTest(long currentTimeMillis) {
        mCurrentTimeMillisForTest = currentTimeMillis;
    }

    /**
     * Returns the current time in milliseconds since the epoch.
     * <p>
     * It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
     */
    private long getCurrentTimeMillis() {
        if (mCurrentTimeMillisForTest == null) {
            return System.currentTimeMillis();
        } else {
            return mCurrentTimeMillisForTest;
        }
    }

    /** Sets the call count and date. */
    private void setCallCountAndDate(PhoneCallDetailsViews views, Integer callCount,
            CharSequence dateText) {
        // Combine the count (if present) and the date.
        CharSequence text;
        if (callCount != null) {
            text = mResources.getString(
                    R.string.call_log_item_count_and_date, callCount.intValue(), dateText);
        } else {
            text = dateText;
        }

        views.callTypeAndDate.setText(text);
    }

}
