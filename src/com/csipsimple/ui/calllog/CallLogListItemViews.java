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

import android.view.View;
import android.widget.ImageView;

import com.csipsimple.R;
import com.csipsimple.widgets.badge.QuickContactBadge;

/**
 * Simple value object containing the various views within a call log entry.
 */
public final class CallLogListItemViews {
    /** The quick contact badge for the contact. */
    public final QuickContactBadge quickContactView;
    /** The primary action view of the entry. */
    public final View primaryActionView;
    /** The secondary action button on the entry. */
    public final ImageView secondaryActionView;
    /** The divider between the primary and secondary actions. */
    public final View dividerView;
    /** The details of the phone call. */
    public final PhoneCallDetailsViews phoneCallDetailsViews;
    /** The divider to be shown below items. */
    public final View bottomDivider;

    private CallLogListItemViews(QuickContactBadge quickContactView, View primaryActionView,
            ImageView secondaryActionView, View dividerView,
            PhoneCallDetailsViews phoneCallDetailsViews,
            View bottomDivider) {
        this.quickContactView = quickContactView;
        this.primaryActionView = primaryActionView;
        this.secondaryActionView = secondaryActionView;
        this.dividerView = dividerView;
        this.phoneCallDetailsViews = phoneCallDetailsViews;
        this.bottomDivider = bottomDivider;
    }

    public static CallLogListItemViews fromView(View view) {
        return new CallLogListItemViews(
                (QuickContactBadge) view.findViewById(R.id.quick_contact_photo),
                view.findViewById(R.id.primary_action_view),
                (ImageView) view.findViewById(R.id.secondary_action_icon),
                view.findViewById(R.id.divider),
                PhoneCallDetailsViews.fromView(view),
                view.findViewById(R.id.call_log_divider));
    }
}
