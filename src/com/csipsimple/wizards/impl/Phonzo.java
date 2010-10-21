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

import com.csipsimple.models.Account;

public class Phonzo extends AuthorizationImplementation {
	

	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		hidePreference(null, SERVER);
	}
	
	@Override
	public void updateDescriptions() {
		setStringFieldSummary(DISPLAY_NAME);
		setStringFieldSummary(USER_NAME);
		setPasswordFieldSummary(PASSWORD);
		setPasswordFieldSummary(AUTH_NAME);
	}
	
	@Override
	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
		isValid &= checkField(accountUsername, isEmpty(accountUsername));
		isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
		isValid &= checkField(accountPassword, isEmpty(accountPassword));

		return isValid;
	}

	@Override
	protected String getDefaultName() {
		return "Phonzo";
	}

	
}
