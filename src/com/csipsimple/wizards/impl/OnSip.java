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

import java.util.HashMap;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;


public class OnSip extends AuthorizationImplementation {

	@Override
	protected String getDefaultName() {
		return "OnSIP";
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		//TODO : when codec per network feature will be there set that for wifi
		prefs.setCodecPriority("G722/16000/1", "240");
		prefs.setCodecPriority("PCMU/8000/1", "239");
		prefs.setCodecPriority("GSM/8000/1", "238");
		
		
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		//Override titles so they are consistent with the how our support docs refer to them
		accountUsername.setTitle(R.string.w_onsip_username);
		accountUsername.setDialogTitle(R.string.w_onsip_username_desc);
	
		accountAuthorization.setTitle(R.string.w_onsip_authentication_name);
		accountAuthorization.setDialogTitle(R.string.w_onsip_authentication_name_desc);
		
		accountPassword.setTitle(R.string.w_onsip_password);
		accountPassword.setDialogTitle(R.string.w_onsip_password_desc);
		
		accountServer.setTitle(R.string.w_onsip_server);
		accountServer.setDialogTitle(R.string.w_onsip_server_desc);
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.proxies = new String[]{"sip:sip.onsip.com"};
		return account;
	}

	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = -5743705263738203615L;

	{
		put(DISPLAY_NAME, R.string.w_common_display_name_desc);
		put(USER_NAME, R.string.w_onsip_username_desc);
		put(AUTH_NAME, R.string.w_onsip_authentication_name_desc);
		put(PASSWORD, R.string.w_onsip_password_desc);
		put(SERVER, R.string.w_onsip_server_desc);
	}};
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}
	
}
