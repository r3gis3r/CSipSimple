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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class SipCel extends SimpleImplementation {

	private static final String KEY_SERVER_PORT = "server_port";
    private ListPreference accountPort;
    private boolean accountPortAdded = false;

    @Override
	protected String getDomain() {
		return "sip.sipcel.mobi";
	}
	
	@Override
	protected String getDefaultName() {
		return "SipCel";
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#updateDescriptions()
	 */
	@Override
	public void updateDescriptions() {
	    super.updateDescriptions();
	    setListFieldSummary(KEY_SERVER_PORT);
	    if(accountUseTcp.isChecked()) {
	        if (accountPortAdded) {
	            hidePreference(null, KEY_SERVER_PORT);
	            accountPortAdded = false;
	        }
	    }else {
	        if(!accountPortAdded) {
	            addPreference(accountPort);
	            accountPortAdded = true;
	        }
	    }
	    
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#fillLayout(com.csipsimple.api.SipProfile)
	 */
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);

        CharSequence[] ports = new CharSequence[] {"5060", "443"};
        
        boolean recycle = true;
        accountPort = (ListPreference) findPreference(KEY_SERVER_PORT);
        if(accountPort == null) {
            recycle = false;
            accountPort = new ListPreference(parent);
            accountPort.setTitle("Server port");
            accountPort.setSummary("Connect to port 443 instead of 5060");
        }
        
        accountPort.setEntries(ports);
        accountPort.setEntryValues(ports);
        accountPort.setKey(KEY_SERVER_PORT);
        accountPort.setDefaultValue("5060");
        
        if (account.proxies != null) {
            for (String proxy : account.proxies) {
                if(proxy.endsWith(":443")) {
                    accountPort.setValue("443");
                }
            }
        }
        
        if(!recycle && account.transport != SipProfile.TRANSPORT_TCP) {
            addPreference(accountPort);
            accountPortAdded = true;
        }
	}
	
	@Override
	protected boolean canTcp() {
	    return true;
	}

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		String remoteServerUri = getDomain();
		
        String proxyPort = "";
		if(acc.transport == SipProfile.TRANSPORT_UDP) {
		    proxyPort = ":" + accountPort.getValue();
		}
		
		acc.proxies = new String[] { "sip:" + remoteServerUri + proxyPort };
		acc.publish_enabled = 1;
		acc.sip_stun_use = 1;
		acc.media_stun_use = 1;
		
		return acc;
	}
	

	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		
		prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_QOS, true);
        prefs.setPreferenceStringValue(SipConfigManager.SND_CLOCK_RATE, "8000");
		prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, Integer.toString(SipConfigManager.DTMF_MODE_AUTO));

        prefs.setPreferenceStringValue(SipConfigManager.KEEP_ALIVE_INTERVAL_MOBILE, "800");
        prefs.setPreferenceStringValue(SipConfigManager.KEEP_ALIVE_INTERVAL_WIFI, "1200");
        prefs.setPreferenceStringValue(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_MOBILE, "800");
        prefs.setPreferenceStringValue(SipConfigManager.TCP_KEEP_ALIVE_INTERVAL_WIFI, "1200");

		//For Wifi: Speex 32Khz, speex 16, g729, gsm.
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"242");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"100");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"243");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"244");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "241");
		
		//For 3G: G729, GSM 8Khz, Ilbc 8Khz, speex 8Khz.
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"100");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"242");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"241");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "243");
	}
	
	@Override
	public List<Filter> getDefaultFilters(SipProfile acc) {
	    ArrayList<Filter> filters = new ArrayList<Filter>();
		
		Filter f = new Filter();
		f.account = (int) acc.id;
		f.action = Filter.ACTION_REPLACE;
		f.matchPattern = "^"+Pattern.quote("+")+"(.*)$";
		f.replacePattern = "00$1";
		f.matchType = Filter.MATCHER_STARTS;
		filters.add(f);
		
		f = new Filter();
		f.account = (int) acc.id;
		f.action = Filter.ACTION_REPLACE;
		f.matchPattern = "^"+Pattern.quote("011")+"(.*)$";
		f.replacePattern = "00$1";
		f.matchType = Filter.MATCHER_STARTS;
		filters.add(f);
		
		return filters;
	}
	
}
