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

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.utils.PreferencesWrapper;

public class PlanetPhone extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "sip.planetphone.net";
	}
	
	@Override
	protected String getDefaultName() {
		return "PlanetPhone";
	}

	//Customization
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		accountUsername.setTitle(R.string.w_common_username);
		accountUsername.setDialogTitle(R.string.w_common_username);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
	}
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_VAD, true);
	}
	
	
	@Override
	public boolean needRestart() {
		return true;
	}
}
