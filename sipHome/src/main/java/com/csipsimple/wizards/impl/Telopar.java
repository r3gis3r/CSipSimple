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

package com.csipsimple.wizards.impl;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Telopar extends SimpleImplementation {

    @Override
    protected String getDomain() {
        return "telopar.us";
    }

    @Override
    protected String getDefaultName() {
        return "Novatrope";
    }

    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        // Contact rewrite not needed for them.
        // Besides stun will be enabled.
        account.contact_rewrite_method = 1;
        account.try_clean_registers = 0;
        account.allow_contact_rewrite = false;
        return account;
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);

        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
        // Add stun server
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);

        for (String bandwidth : new String[] {
                SipConfigManager.CODEC_WB, SipConfigManager.CODEC_NB
        }) {
            prefs.setCodecPriority("PCMU/8000/1", bandwidth, "0");
            prefs.setCodecPriority("PCMA/8000/1", bandwidth, "0");
            prefs.setCodecPriority("G722/16000/1", bandwidth, "0");
            prefs.setCodecPriority("iLBC/8000/1", bandwidth, "0");
            prefs.setCodecPriority("speex/8000/1", bandwidth, "0");
            prefs.setCodecPriority("speex/16000/1", bandwidth, "0");
            prefs.setCodecPriority("speex/32000/1", bandwidth, "0");
            prefs.setCodecPriority("SILK/8000/1", bandwidth, "0");
            prefs.setCodecPriority("SILK/12000/1", bandwidth, "0");
            prefs.setCodecPriority("SILK/16000/1", bandwidth, "0");
            prefs.setCodecPriority("SILK/24000/1", bandwidth, "0");
            prefs.setCodecPriority("GSM/8000/1", bandwidth, "240");
        }

        prefs.addStunServer("stun.telopar.net");
    }

    @Override
    public boolean needRestart() {
        return true;
    }
}
