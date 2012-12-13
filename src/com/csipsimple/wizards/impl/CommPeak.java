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

import java.util.SortedMap;
import java.util.TreeMap;

import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class CommPeak extends SimpleImplementation {
    @Override
    protected String getDefaultName() {
        return "CommPeak";
    }

    ListPreference sipServer;
    static SortedMap<String, String> providers = new TreeMap<String, String>() {
        private static final long serialVersionUID = 5937536588407734205L;
        {
            put("sip.commpeak.com", "sip.commpeak.com");
            put("US Virginia", "useast.sip.commpeak.com");
            put("US Oregon", "uswest.sip.commpeak.com");
            put("EU Ireland", "ireland.sip.commpeak.com");
            put("S. America Brazil", "brazil.sip.commpeak.com");
            put("Asia Singapore", "singapore.sip.commpeak.com");
            put("Asia Tokyo", "tokyo.sip.commpeak.com");
        }
    };

    private static final String PROVIDER_LIST_KEY = "provider_list";

    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);

        boolean recycle = true;
        sipServer = (ListPreference) findPreference(PROVIDER_LIST_KEY);
        if (sipServer == null) {
            sipServer = new ListPreference(parent);
            sipServer.setKey(PROVIDER_LIST_KEY);
            recycle = false;
        }

        CharSequence[] e = new CharSequence[providers.size()];
        CharSequence[] v = new CharSequence[providers.size()];
        int i = 0;
        for (String pv : providers.keySet()) {
            e[i] = pv;
            v[i] = providers.get(pv);
            i++;
        }

        sipServer.setEntries(e);
        sipServer.setEntryValues(v);
        sipServer.setDialogTitle(R.string.w_common_server);
        sipServer.setTitle(R.string.w_common_server);
        sipServer.setDefaultValue("sip.commpeak.com");

        if (!recycle) {
            addPreference(sipServer);
        }

        String domain = account.reg_uri;
        if (domain != null) {
            for (CharSequence state : v) {
                String currentComp = "sip:" + state;
                if (currentComp.equalsIgnoreCase(domain)) {
                    sipServer.setValue(state.toString());
                    break;
                }
            }
        }
    }

    protected String getDomain() {
        String provider = sipServer.getValue();
        if (provider != null) {
            return provider;
        }
        return "";
    }

    @Override
    public void updateDescriptions() {
        super.updateDescriptions();
        setStringFieldSummary(PROVIDER_LIST_KEY);
    }

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if (fieldName == PROVIDER_LIST_KEY) {
            if (sipServer != null) {
                return sipServer.getEntry().toString();
            }
        }

        return super.getDefaultFieldSummary(fieldName);
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        // Prefer silk, g729 (not possible due to license, and keep g711
        // fallback)
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "245");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");

        // Prefer silk, g729 (not possible due to license, and keep g711
        // fallback)
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "245");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
    }

    @Override
    public boolean needRestart() {
        return true;
    }
}
