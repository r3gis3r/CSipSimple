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

import org.pjsip.pjsua.pj_str_t;

import android.text.InputType;

import com.csipsimple.R;
import com.csipsimple.models.Account;

public class UkrTelecom extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "sip.ukrtel.net";
	}
	
	@Override
	protected String getDefaultName() {
		return "Ukrtelecom";
	}

	
	//Customization
	@Override
	public void fillLayout(Account account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_common_phone_number);
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
	}
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_common_phone_number_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	
	
	public Account buildAccount(Account account) {
		account = super.buildAccount(account);
		account.cfg.setProxy_cnt(0);
		pj_str_t[] proxies = account.cfg.getProxy();
		account.cfg.setProxy(proxies);
		account.transport = 1;
		return account;
	}
}
