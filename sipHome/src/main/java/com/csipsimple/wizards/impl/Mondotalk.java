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

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.wizards.utils.AccountCreationFirstView;
import com.csipsimple.wizards.utils.AccountCreationFirstView.OnAccountCreationFirstViewListener;

import java.util.Locale;

public class Mondotalk extends SimpleImplementation implements OnAccountCreationFirstViewListener {
    
	@Override
	protected String getDomain() {
		return "sip99.mondotalk.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "Mondotalk";
	}
	
    private int CREATE_ACCOUNT;
    private ViewGroup settingsContainer;
    private ViewGroup validationBar;
    private AccountCreationFirstView firstView;
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);

		accountUsername.setTitle(R.string.w_common_phone_number);
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);

        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);
        
        CREATE_ACCOUNT = parent.getFreeSubActivityCode();
		
        updateAccountInfos(account);
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_UDP;
		account.reg_timeout = 180;
		
		// Use port 80 for blocked countries
		String currentCountry = Locale.getDefault().getCountry();
		if(!TextUtils.isEmpty(currentCountry)) {
            if ("AE".equalsIgnoreCase(currentCountry) || 
                    "CN".equalsIgnoreCase(currentCountry) ||
                    "PK".equalsIgnoreCase(currentCountry)) {
                account.proxies = new String[] {
                        "sip99.mondotalk.com:80"
                };
            }
		}
		
		return account;
	}
	
	@Override
	protected boolean canTcp() {
		return false;
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CREATE_ACCOUNT) {
            if(resultCode == Activity.RESULT_OK) {
                String uname = data.getStringExtra(SipProfile.FIELD_USERNAME);
                String pwd = data.getStringExtra(SipProfile.FIELD_DATA);
                if(!TextUtils.isEmpty(uname) && !TextUtils.isEmpty(pwd)) {
                    setUsername(uname);
                    setPassword(pwd);
                    if(canSave()) {
                        parent.saveAndFinish();
                    }
                }
            }
        }
    }
    

    @Override
    public void onCreateAccountRequested() {
        setFirstViewVisibility(false);
        parent.startActivityForResult(new Intent(parent, MondotalkCreate.class), CREATE_ACCOUNT);
    }

    @Override
    public void onEditAccountRequested() {
        setFirstViewVisibility(false);
    }
    
}
