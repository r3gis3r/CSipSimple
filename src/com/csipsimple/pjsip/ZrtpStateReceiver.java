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

package com.csipsimple.pjsip;

import android.content.Intent;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Log;

import org.pjsip.pjsua.ZrtpCallback;
import org.pjsip.pjsua.pj_str_t;

public class ZrtpStateReceiver extends ZrtpCallback {
    

    private static final String THIS_FILE = "ZrtpStateReceiver";
    private PjSipService pjService;
    
    ZrtpStateReceiver(PjSipService service) {
        pjService = service;
    }

    @Override
    public void on_zrtp_show_sas(int callId, pj_str_t sas, int verified) {
        String sasString = PjSipService.pjStrToString(sas);
        Log.d(THIS_FILE, "ZRTP show SAS " + sasString + " verified : " + verified);
        if(verified != 1) {
            SipCallSession callInfo = pjService.getPublicCallInfo(callId);
            Intent zrtpIntent = new Intent(SipManager.ACTION_ZRTP_SHOW_SAS);
            zrtpIntent.putExtra(Intent.EXTRA_SUBJECT, sasString);
            zrtpIntent.putExtra(SipManager.EXTRA_CALL_INFO, callInfo);
            pjService.service.sendBroadcast(zrtpIntent, SipManager.PERMISSION_USE_SIP);
        }else{
            updateZrtpInfos(callId);
        }
    }
    

    @Override
    public void on_zrtp_update_transport(int callId) {
        updateZrtpInfos(callId);
    }

    public void updateZrtpInfos(final int callId) {
        pjService.refreshCallMediaState(callId);
    }
}
