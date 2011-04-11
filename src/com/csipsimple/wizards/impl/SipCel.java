/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
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


public class SipCel extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "sip.sipcel.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "SIPCEL";
	}

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		acc.reg_uri = "sip.sipcel.com:445";
		acc.proxies = new String[] { "sip.sipcel.com:445" };
		acc.publish_enabled = 1;
		acc.reg_timeout = 120;
		acc.transport = SipProfile.TRANSPORT_AUTO;
		
		return acc;
	}
	

	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
		prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, PreferencesWrapper.DTMF_MODE_AUTO);
		
		
		
		//Only g729, ulaw, ilbc, gsm
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"244");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"243");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"242");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "241");
		
		//On NB g729, ulaw, ilbc, gsm
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"243");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"242");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "241");
	}
	
}
