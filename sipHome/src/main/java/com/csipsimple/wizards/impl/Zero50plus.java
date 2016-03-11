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

import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Zero50plus extends Advanced {
	
	protected String getDefaultName() {
		return "050Plus";
	}
	
    @Override
    protected String getServer() {
        return "050plus.com";
    }
	
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);
	    if(TextUtils.isEmpty(accountDisplayName.getText())){
	        accountDisplayName.setText(getDefaultName());
	    }
	    
	    if(account.proxies != null && account.proxies.length > 0) {
	        String p = account.proxies[0];
	        if(!TextUtils.isEmpty(p)) {
	            String strippedP = p.replace(":5061", "");
                strippedP = strippedP.replace("sip:", "");
	            strippedP = strippedP.replace(".050plus.com", "");
	            accountProxy.setText(strippedP);
	        }
	    }

	    accountUserName.setTitle("nicNm");
	    accountAuthId.setTitle("SipID");
	    accountPassword.setTitle("sipPwd");
	    
	    hidePreference(null, FIELD_SERVER);
	    hidePreference(null, FIELD_TCP);
	    hidePreference(null, FIELD_CALLER_ID);
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
	    if(fieldName.equals(FIELD_PROXY)) {
	        return "tranGwAd-payTranGwPNm";
	    }
	    return super.getDefaultFieldSummary(fieldName);
	}
	
	@Override
	public boolean canSave() {
	       boolean isValid = true;
	        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
	        isValid &= checkField(accountUserName, isEmpty(accountUserName));
	        isValid &= checkField(accountPassword, isEmpty(accountPassword));
	        return isValid;
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		
		// Shall be sips: ?
		String pTxt = accountProxy.getText();
		if(!pTxt.contains(".050plus.com")) {
		    pTxt += ".050plus.com";
		}
        String sipServerUri = "sip:" + pTxt + ":5061";
        account.reg_uri = sipServerUri;
        account.proxies = new String[] {sipServerUri};
		
		account.reg_timeout = 3600;
		account.mwi_enabled = false;
		account.allow_contact_rewrite = false;
		account.publish_enabled = 0;
		account.try_clean_registers = 0;
		account.use_srtp = 2;
		account.transport = SipProfile.TRANSPORT_TLS;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
		
	}

    @Override
    public boolean needRestart() {
        return true;
    }
}
