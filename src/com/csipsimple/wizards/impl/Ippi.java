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

import android.text.InputType;

import com.csipsimple.models.Account;
import com.csipsimple.utils.PreferencesWrapper;

public class Ippi extends SimpleImplementation {


	@Override
	protected String getDomain() {
		return "ippi.fr";
	}
	
	@Override
	protected String getDefaultName() {
		return "ippi";
	}

	
	//Customization
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_STUN, true);
		prefs.addStunServer("stun.ippi.fr");
	}
	
	@Override
	protected boolean canTcp() {
		return true;
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
}
