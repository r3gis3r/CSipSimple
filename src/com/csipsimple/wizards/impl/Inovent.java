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

import android.text.InputType;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Inovent extends SimpleImplementation {
    protected static String REALM = "realm";

    @Override
    protected String getDomain() {
        return "inovent.bg";
    }

    // Customization
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
        if (!TextUtils.isEmpty(account.username) && !TextUtils.isEmpty(account.realm)) {
            accountUsername.setText(account.username + "@" + account.realm);
        }
        accountUsername
                .setDialogMessage("Username and realm in form username@realm, for example admin@inovent.bg");
    }

    @Override
    protected String getDefaultName() {
        return "Inovent";
    }

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if (fieldName.equals(USER_NAME)) {
            return "Username in the form username@realm";
        }
        return super.getDefaultFieldSummary(fieldName);
    }

    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        String[] emailParts = getText(accountUsername).trim().split("@");
        account.acc_id = "<sip:" + getText(accountUsername).trim() + ">";
        if (emailParts.length == 2) {
            account.username = emailParts[0];
            account.realm = emailParts[1];
        }
        else {
            account.username = "invalid username";
            account.realm = "invalid realm";
        }
        account.data = getText(accountPassword);
        account.transport = SipProfile.TRANSPORT_TLS;
        account.reg_uri = "sips:inovent.bg";
        account.proxies = new String[] {
            "sips:inovent.bg:5061"
        };
        account.reg_timeout = 900;
        account.use_zrtp = 0;
        account.use_srtp = 2;
        account.try_clean_registers = 1;
        account.rtp_enable_qos = 0;
        account.rtp_port = -1;
        account.sip_stun_use = 0;
        account.turn_cfg_use = 0;
        
        // TODO : r3gis : should we set
        // account.default_uri_scheme ? 
        return account;
    }

    @Override
    public boolean needRestart() {
        return true;
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
        // TODO : r3gis : is it mandatory that the app locally bind 5061 for TLS transport, 
        // usually better to take random port instead for local binding : more secure to prevent unwanted direct calls + avoid conflicts with other apps
        prefs.setPreferenceStringValue(SipConfigManager.TLS_TRANSPORT_PORT, "5061");
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_QOS, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
        prefs.setPreferenceStringValue(SipConfigManager.TLS_METHOD, "1");

        // Prefer opus,silk
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_WB, "245");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "240");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");

        // Prefer opus,silk
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_NB, "245");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "240");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
    }

}
