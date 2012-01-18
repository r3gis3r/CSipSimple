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

public class FreephoneLineCa extends SimpleImplementation {
	


	@Override
	protected String getDomain() {
		return "voip.freephoneline.ca";
	}
	
	@Override
	protected String getDefaultName() {
		return "Freephoneline.ca";
	}
	

	
	@Override
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.reg_timeout = 3600;
		account.allow_contact_rewrite = false;
		
		return account;
	}
	
	@Override
	protected boolean canTcp() {
		return false;
	}
	
	
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

		
		//Only G711u on WB
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"245");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
		
		//On NB set for g729 high priority
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
		prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
		prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "245");
	}

	@Override
	public boolean needRestart() {
		return true;
	}
}
