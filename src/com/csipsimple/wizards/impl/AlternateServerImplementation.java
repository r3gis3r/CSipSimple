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

import android.preference.EditTextPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;

public abstract class AlternateServerImplementation extends SimpleImplementation {
	

	protected static String SERVER = "server";
	protected EditTextPreference accountServer;
	
	@Override
	protected void bindFields() {
		super.bindFields();
		accountServer = (EditTextPreference) parent.findPreference(SERVER);
	}
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);
		accountServer.setText(account.getSipDomain());
	}
	
	@Override
	protected String getDomain() {
		return accountServer.getText();
	}
	
	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_alternate_server_preferences;
	}
	
	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= super.canSave();
		isValid &= checkField(accountServer, isEmpty(accountServer));

		return isValid;
	}

	@Override
	public void updateDescriptions() {
		super.updateDescriptions();
		setStringFieldSummary(SERVER);
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(SERVER)) {
			return parent.getString(R.string.w_common_server_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
}
