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

package com.csipsimple.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RemoteViews;

import com.csipsimple.R;
import com.csipsimple.api.SipProfileState;
import com.csipsimple.wizards.WizardUtils;
import com.csipsimple.wizards.WizardUtils.WizardInfo;

import java.util.ArrayList;

public class RegistrationNotification extends RemoteViews {

    private static final Integer[] cells = new Integer[] {
            R.id.cell1,
            R.id.cell2,
            R.id.cell3,
    };

    private static final Integer[] icons = new Integer[] {
            R.id.icon1,
            R.id.icon2,
            R.id.icon3,
    };

    private static final Integer[] texts = new Integer[] {
            R.id.account_label1,
            R.id.account_label2,
            R.id.account_label3,
    };

    public RegistrationNotification(String aPackageName) {
        super(aPackageName, R.layout.notification_registration_layout);
    }

    public RegistrationNotification(Context ctxt) {
        this(ctxt.getPackageName());
    }

    public RegistrationNotification(Context ctxt, AttributeSet attr) {
        this(ctxt.getPackageName());
    }

    public RegistrationNotification(Context ctxt, AttributeSet attr, int defStyle) {
        this(ctxt.getPackageName());
    }

    /**
     * Reset all registration info for this view, ie hide all accounts cells
     */
    public void clearRegistrations() {
        for (Integer cellId : cells) {
            setViewVisibility(cellId, View.GONE);
        }
    }

    /**
     * Apply account information to remote view
     * 
     * @param context application context for resources retrieval
     * @param activeAccountsInfos List of sip profile state to show in this
     *            notification view
     */
    public void addAccountInfos(Context context, ArrayList<SipProfileState> activeAccountsInfos) {
        int i = 0;
        for (SipProfileState accountInfo : activeAccountsInfos) {
            // Clamp to max possible notifications in remote view
            if (i < cells.length) {
                setViewVisibility(cells[i], View.VISIBLE);
                WizardInfo wizardInfos = WizardUtils.getWizardClass(accountInfo.getWizard());
                if (wizardInfos != null) {
                    CharSequence dName = accountInfo.getDisplayName();

                    setImageViewResource(icons[i], wizardInfos.icon);
                    if (!TextUtils.isEmpty(dName)) {
                        setTextViewText(texts[i], dName);
                        // setCharSequence(icons[i], "setContentDescription",
                        // dName);
                    }
                }
                i++;
            }
        }

    }

}
