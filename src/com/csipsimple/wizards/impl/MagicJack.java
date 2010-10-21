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
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;

import com.csipsimple.models.Account;

public class MagicJack extends AlternateServerImplementation {
	

	public Account buildAccount(Account account) {
		account = super.buildAccount(account);
		String port = "5070";
		
		account.cfg.setReg_uri(pjsua.pj_str_copy("sip:"+getDomain()+":"+port));

		pjsip_cred_info ci = account.cfg.getCred_info();

		account.cfg.setCred_count(1);
		ci.setRealm(pjsua.pj_str_copy("*"));
		ci.setUsername(getPjText(accountUsername));
		ci.setData(getPjText(accountPassword));
		ci.setScheme(pjsua.pj_str_copy("Digest"));
		ci.setData_type(8); // 8 is MJ digest auth

		account.cfg.setProxy_cnt(1);
		pj_str_t[] proxies = account.cfg.getProxy();
		proxies[0] = pjsua.pj_str_copy("sip:"+getDomain()+":"+port);
		account.cfg.setProxy(proxies);
		return account;
	}

	@Override
	protected String getDefaultName() {
		return "MagicJack";
	}
}
