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

import com.csipsimple.R;
import com.csipsimple.models.Account;
import com.csipsimple.utils.PreferencesWrapper;

public class Sipgate extends AlternateServerImplementation {
	
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		//Override titles
		accountDisplayName.setTitle(R.string.w_sipgate_display_name);
		accountDisplayName.setDialogTitle(R.string.w_sipgate_display_name);
		accountServer.setTitle(R.string.w_common_server);
		accountServer.setDialogTitle(R.string.w_common_server);
		accountUsername.setTitle(R.string.w_sipgate_username);
		accountUsername.setDialogTitle(R.string.w_sipgate_username);
		accountPassword.setTitle(R.string.w_sipgate_password);
		accountPassword.setDialogTitle(R.string.w_sipgate_password);
	}
	
	

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(PreferencesWrapper.ENABLE_STUN, true);
		prefs.addStunServer("stun.sipgate.net:10000");
	}
	

	@Override
	protected String getDefaultName() {
		return "Sipgate";
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
	
}
