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

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationFirstView;
import com.csipsimple.wizards.utils.AccountCreationFirstView.OnAccountCreationFirstViewListener;

public class MobileWiFi extends SimpleImplementation implements OnAccountCreationFirstViewListener {

    
    private static final String webCreationPage = "http://mobile-wi.fi";
    
    private ViewGroup validationBar;
    private ViewGroup settingsContainer;

    private AccountCreationFirstView firstView;

    @Override
    protected String getDefaultName() {
        return "Mobile-Wi.Fi";
    }

    @Override
    protected String getDomain() {
        return "sip.mobile-wi.fi";
    }
    
    private boolean useIpv6() {
        return false;
    }

    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        String domain = getDomain();
        String scheme = "sips";
        
        // For TLS
        account.reg_uri = scheme +":" + domain;
        account.transport = SipProfile.TRANSPORT_TLS;
        account.vm_nbr = "1000";
        
        // For ipv6 -- future use... not yet a button two switch (and anyway will not be done this way as we should autodetect)
        account.proxies = new String[] {
                scheme + ":" + (useIpv6() ? "[2001:470:9ff3::1]" : "csipsimple.mobile-wi.fi")
        };
        if(useIpv6()) {
            account.sip_stun_use = 0;
            account.media_stun_use = 0;
            account.ice_cfg_enable = 1;
            account.ice_cfg_use = 0;
        } else {
            account.sip_stun_use = -1;
            account.media_stun_use = -1;
            account.ice_cfg_enable = 0;
        }
        
        
        return account;
    }

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
    }

    @Override
    public boolean needRestart() {
        return true;
    }
    @Override
    public void fillLayout(SipProfile account) {
        super.fillLayout(account);

        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);

        updateAccountInfos(account);
    }

    private void setFirstViewVisibility(boolean visible) {
        if(firstView != null) {
            firstView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        validationBar.setVisibility(visible ? View.GONE : View.VISIBLE);
        settingsContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
    }
    

    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            setFirstViewVisibility(false);
        } else {
            if(firstView == null) {
                firstView = new AccountCreationFirstView(parent);
                ViewGroup globalContainer = (ViewGroup) settingsContainer.getParent();
                firstView.setOnAccountCreationFirstViewListener(this);
                globalContainer.addView(firstView);
            }
            setFirstViewVisibility(true);
        }
    }

    @Override
    public void onCreateAccountRequested() {
        setFirstViewVisibility(false);
        parent.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webCreationPage)));
    }

    @Override
    public void onEditAccountRequested() {
        setFirstViewVisibility(false);
    }
}
