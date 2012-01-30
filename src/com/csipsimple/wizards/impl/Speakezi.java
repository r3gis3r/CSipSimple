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

import android.preference.ListPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.SortedMap;
import java.util.TreeMap;

public class Speakezi extends SimpleImplementation {

    ListPreference sipServer;
    static SortedMap<String, String[]> providers = new TreeMap<String, String[]>() {
        private static final long serialVersionUID = -2561302247222706262L;
        {
            put("SpeakEzi", new String[] {
                "sip.easivoice.co.za"
            });
            put("SpeakEzi Office", new String[] {
                "41.221.5.172"
            });
        }
    };

    @Override
    protected String getDefaultName() {
        return "Speakezi";
    }

    @Override
    protected String getDomain() {
        return "sip.easivoice.co.za";
    }

    private static final String PROVIDER_LIST_KEY = "provider_list";

    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);

        boolean recycle = true;
        sipServer = (ListPreference) parent.findPreference(PROVIDER_LIST_KEY);
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
            v[i] = ((String[]) providers.get(pv))[0];
            i++;
        }

        sipServer.setEntries(e);
        sipServer.setEntryValues(v);
        sipServer.setDialogTitle(R.string.w_common_server);
        sipServer.setTitle(R.string.w_common_server);
        sipServer.setDefaultValue("sip.easivoice.co.za");

        String domain = account.reg_uri;
        if (domain != null) {
            for (CharSequence state : v) {
                String currentComp = "sip:" + state;
                if (currentComp.equalsIgnoreCase(domain)) {
                    sipServer.setValue((String) state);
                    break;
                }
            }
        }

        if (!recycle) {
            parent.getPreferenceScreen().addPreference(sipServer);
        }
    }

    @Override
    public boolean needRestart() {
        return true;
    }
    
    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        // Use registrar and proxy to be the selected server
        // Keep user domain on old domain
        String provider = sipServer.getValue();
        if(!TextUtils.isEmpty(provider)) {
            acc.reg_uri = "sip:" + provider;
            acc.proxies = new String[] {"sip:" + provider};
        }
        
        return acc;
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);

        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "245");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "244");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "243");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "242");

        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "245");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "243");
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "242");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "241");
    }

    @Override
    public void updateDescriptions() {
        super.updateDescriptions();
        setStringFieldSummary(PROVIDER_LIST_KEY);
    }

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(fieldName == PROVIDER_LIST_KEY) {
            if(sipServer != null) {
                return sipServer.getEntry().toString();
            }
        }
        
        return super.getDefaultFieldSummary(fieldName);
    }
}
