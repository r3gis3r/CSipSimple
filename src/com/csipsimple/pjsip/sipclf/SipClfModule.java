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

package com.csipsimple.pjsip.sipclf;

import android.content.Context;

import com.csipsimple.api.SipProfile;
import com.csipsimple.pjsip.PjSipService.PjsipModule;
import com.csipsimple.utils.Log;

import org.pjsip.pjsua.pjsua;

public class SipClfModule implements PjsipModule {

    private static final String THIS_FILE = "SipClfModule";

    public SipClfModule() {
    }

    @Override
    public void setContext(Context ctxt) {
    }

    @Override
    public void onBeforeStartPjsip() {
        int status = pjsua.sipclf_mod_init();
        Log.d(THIS_FILE, "SipClfModule module added with status " + status);
    }

    @Override
    public void onBeforeAccountStartRegistration(int pjId, SipProfile acc) {
    }

}
