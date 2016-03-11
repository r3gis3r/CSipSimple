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

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.csipsimple.R;

/**
 * Encapsulates the views that are used to display the details of a phone call
 * in the call log.
 */
public final class PhoneCallDetailsViews {
    public final TextView nameView;
    public final View callTypeView;
    public final CallTypeIconsView callTypeIcons;
    public final TextView callTypeAndDate;
    public final TextView numberView;

    private PhoneCallDetailsViews(TextView nameView, View callTypeView,
            CallTypeIconsView callTypeIcons, TextView callTypeAndDate, TextView numberView) {
        this.nameView = nameView;
        this.callTypeView = callTypeView;
        this.callTypeIcons = callTypeIcons;
        this.callTypeAndDate = callTypeAndDate;
        this.numberView = numberView;
    }

    /**
     * Create a new instance by extracting the elements from the given view.
     * <p>
     * The view should contain three text views with identifiers
     * {@code R.id.name}, {@code R.id.date}, and {@code R.id.number}, and a
     * linear layout with identifier {@code R.id.call_types}.
     */
    public static PhoneCallDetailsViews fromView(View view) {
        return new PhoneCallDetailsViews((TextView) view.findViewById(R.id.name),
                view.findViewById(R.id.call_type),
                (CallTypeIconsView) view.findViewById(R.id.call_type_icons),
                (TextView) view.findViewById(R.id.call_count_and_date),
                (TextView) view.findViewById(R.id.number));
    }

    public static PhoneCallDetailsViews createForTest(Context context) {
        return new PhoneCallDetailsViews(
                new TextView(context),
                new View(context),
                new CallTypeIconsView(context),
                new TextView(context),
                new TextView(context));
    }
}
