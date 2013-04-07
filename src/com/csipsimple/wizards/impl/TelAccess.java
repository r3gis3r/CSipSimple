/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class TelAccess extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "telakses1.dyndns.org";
	}
	
	@Override
	protected String getDefaultName() {
		return "TelAccess";
	}
	
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
        prefs.setPreferenceStringValue(SipConfigManager.TLS_TRANSPORT_PORT, "5061");
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_QOS, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
        prefs.setPreferenceStringValue(SipConfigManager.TLS_METHOD, "1");
        prefs.setPreferenceStringValue("codec_g729_8000_fpp", "4");
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
	
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_TLS;
		account.reg_uri = "sip:telakses1.dyndns.org:5061";
		account.proxies = new String[] {"sip:telakses1.dyndns.org:5061"};
		account.reg_timeout = 120;
		account.use_zrtp = 0;
		account.use_srtp = 2;
		account.try_clean_registers = 1;
		account.rtp_enable_qos = 1;
		account.rtp_port = 10000;
		return account;
	}

}
