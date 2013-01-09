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
import android.text.TextUtils;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.PreferencesWrapper;

public class Babytel extends Advanced {

    protected String getDomain() {
        return "sip.babytel.ca";
    }

    protected String getDefaultName() {
        return "Babytel";
    }

    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        accountUserName.setDialogTitle(R.string.w_common_phone_number_desc);
        accountUserName.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        accountCallerId.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        hidePreference(null, FIELD_TCP);
        hidePreference(null, FIELD_PROXY);
        hidePreference(null, FIELD_SERVER);
        hidePreference(null, FIELD_AUTH_ID);
        if(TextUtils.isEmpty(account.display_name)) {
            accountDisplayName.setText(getDefaultName());
        }
    }

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if (fieldName.equals(FIELD_USERNAME)) {
            return parent.getString(R.string.w_common_phone_number_desc);
        } 
        return super.getDefaultFieldSummary(fieldName);
    }

    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        String accId = "";
         if(!TextUtils.isEmpty(accountCallerId.getText().trim())) {
             accId += accountCallerId.getText().trim() +" ";
         }
         accId += "<sip:" + SipUri.encodeUser(accountUserName.getText().trim()) + "@" + getDomain() + ">";
         account.acc_id = accId;
        String regUri = "sip:" + getDomain();
        account.reg_uri = regUri;
        account.proxies = new String[]{regUri};
        account.proxies = new String[] {"sip:nat.babytel.ca:5065"};
        account.transport = SipProfile.TRANSPORT_UDP;
        account.reg_timeout = 900;
        account.ice_cfg_use = 1;
        account.ice_cfg_enable = 1;
        return account;
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
    }

    @Override
    public boolean needRestart() {
        return true;
    }

    @Override
    public boolean canSave() {
        boolean isValid = true;
        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountUserName, isEmpty(accountUserName) || accountUserName.getText().trim().length() != 11);
        isValid &= checkField(accountPassword, isEmpty(accountPassword));
        return isValid;
    }
}
