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
import com.csipsimple.utils.PreferencesWrapper;


public class Fayn extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "sip3.fayn.cz";
	}
	
	@Override
	protected String getDefaultName() {
		return "Fayn";
	}

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "245");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("g729/8000/1", SipConfigManager.CODEC_NB, "243");
		
		prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "243");
		prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"244");
		prefs.setCodecPriority("g729/8000/1", SipConfigManager.CODEC_NB, "245");
	}
	
}
