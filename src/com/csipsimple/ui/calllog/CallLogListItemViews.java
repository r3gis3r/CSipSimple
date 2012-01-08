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
