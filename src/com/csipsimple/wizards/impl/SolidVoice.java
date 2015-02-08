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
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.PreferencesWrapper;

public class SolidVoice extends SimpleImplementation {

    @Override
    protected String getDomain() {
        return "solid-voice.net";
    }

    @Override
    protected String getDefaultName() {
        return "solidvoice";
    }
    
    /* (non-Javadoc)
     * @see com.csipsimple.wizards.impl.SimpleImplementation#buildAccount(com.csipsimple.api.SipProfile)
     */
    @Override
    public SipProfile buildAccount(SipProfile account) {
        
        SipProfile acc = super.buildAccount(account);
        account.acc_id = "<sip:" + SipUri.encodeUser(accountUsername.getText().trim()) + "@sip.solid-voice.net>";
        return acc;
    }

    /*
     * (non-Javadoc)
     * @see com.csipsimple.wizards.impl.BaseImplementation#setDefaultParams(com.
     * csipsimple.utils.PreferencesWrapper)
     */
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
    }
    
    /* (non-Javadoc)
     * @see com.csipsimple.wizards.impl.SimpleImplementation#needRestart()
     */
    @Override
    public boolean needRestart() {
        return true;
    }

}
