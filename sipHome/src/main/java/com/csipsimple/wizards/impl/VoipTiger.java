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

public class VoipTiger extends SimpleImplementation {
	
    private final static boolean CUSTOM_DISTRIBUTION = false;

	@Override
	protected String getDomain() {
		return "www.voiptiger.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "VoipTiger";
	}

	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);
	    if(CUSTOM_DISTRIBUTION) {
	        hidePreference(null, DISPLAY_NAME);
	    }
	}
	
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
	    if(CUSTOM_DISTRIBUTION) {
	        acc.display_name = acc.username;
	    }
	    return acc;
	}

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        //  Ilbc, G729, G722,  G711
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"220");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB, "230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"220");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"240");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
        
        // 
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"220");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"220");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"240");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        
        prefs.setPreferenceFloatValue(SipConfigManager.SND_SPEAKER_LEVEL, 1.2f);
    }
    
	
}
