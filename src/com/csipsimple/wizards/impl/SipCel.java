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

import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class SipCel extends SimpleImplementation {

	ListPreference accountState;
	CheckBoxPreference useSafePort;

	@Override
	protected String getDomain() {
		return "sip.sipcel.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "SipCel";
	}
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);
		

		CharSequence[] states = new CharSequence[] {"com", "eu", "mobi", "tel"};
		
        accountState = new ListPreference(parent);
        useSafePort = new CheckBoxPreference(parent);
        
        accountState.setEntries(states);
        accountState.setEntryValues(states);
        accountState.setKey("server");
        accountState.setDialogTitle(R.string.w_common_server);
        accountState.setTitle(R.string.w_common_server);
        accountState.setDefaultValue("com");
        
        String domain = account.reg_uri;
        boolean useSafe = false;
        if( domain != null ) {
	        for(CharSequence state : states) {
	        	String currentComp = "sip:sip.sipcel."+state;
	        	if( domain.startsWith(currentComp) ) {
	        		accountState.setValue((String) state);
	        		break;
	        	}
	        }
	        if(domain.endsWith(":443")) {
	        	useSafe = true;
	        }
        }
        
        useSafePort.setChecked(useSafe);
        useSafePort.setTitle("Use alternate port");
        useSafePort.setSummary("Connect to port 443 instead of 5060");
        

        addPreference(accountState);
        addPreference(useSafePort);
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		String remoteServerUri = "sip:sip.sipcel.";
		String ext = "com";
		String port = "";
		
		if(!TextUtils.isEmpty(accountState.getValue())){
			ext = accountState.getValue();
		}
		
		if(useSafePort.isChecked()) {
			port = ":443";
		}
		
		remoteServerUri += ext + port;
		acc.reg_uri = remoteServerUri;
		acc.proxies = new String[] { remoteServerUri };
		acc.publish_enabled = 1;
		acc.reg_timeout = 120;
		acc.transport = SipProfile.TRANSPORT_AUTO;
		
		return acc;
	}
	

	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
		prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, Integer.toString(SipConfigManager.DTMF_MODE_AUTO));
		
		
		
		//Only g729, gsm, ilbc, ulaw 
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"244");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"241");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"242");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "243");
		
		//On NB ulaw, ilbc, gsm,  g729
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"242");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"243");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "241");
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
