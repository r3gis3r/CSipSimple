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

package com.csipsimple.ui.warnings;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.ui.warnings.WarningUtils.WarningBlockView;

public class WarningPrivilegedIntent extends WarningBlockView  {

    public WarningPrivilegedIntent(Context context) {
        this(context, null, 0);
    }
    public WarningPrivilegedIntent(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public WarningPrivilegedIntent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected int getLayout() {
        return R.layout.warning_privileged_intent;
    }
    
    @Override
    protected void bindView() {
        super.bindView();
        findViewById(R.id.warn_priv_int_register).setOnClickListener(this);
        
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.warn_priv_int_register) {
            SipConfigManager.setPreferenceBooleanValue(getContext(), SipConfigManager.INTEGRATE_TEL_PRIVILEGED, true);
            SipConfigManager.setPreferenceBooleanValue(getContext(), SipConfigManager.INTEGRATE_WITH_DIALER, false);
            if(onWarnChangedListener != null) {
                onWarnChangedListener.onWarningRemoved(getWarningKey());
            }
            getContext().sendBroadcast(new Intent(SipManager.ACTION_SIP_REQUEST_RESTART));
        }
    }
    @Override
    protected String getWarningKey() {
        return WarningUtils.WARNING_PRIVILEGED_INTENT;
    }

}
