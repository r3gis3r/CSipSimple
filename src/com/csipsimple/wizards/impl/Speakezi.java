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

import com.csipsimple.utils.PreferencesWrapper;

public class Speakezi extends SimpleImplementation {

	@Override
	protected String getDomain() {
		return "sip.easivoice.co.za";
	}

	@Override
	protected String getDefaultName() {
		return "Speakezi";
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

		prefs.setCodecPriority("speex/8000/1", "240");
		prefs.setCodecPriority("GSM/8000/1", "239");
		prefs.setCodecPriority("PCMA/8000/1", "238");
		prefs.setCodecPriority("PCMU/8000/1", "237");
	}

}
