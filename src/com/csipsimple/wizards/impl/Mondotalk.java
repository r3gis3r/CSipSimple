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
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;

public class Mondotalk extends SimpleImplementation {

    private LinearLayout customWizard;
    private TextView customWizardText;
    
	@Override
	protected String getDomain() {
		return "sip99.mondotalk.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "Mondotalk";
	}
	
    private int CREATE_ACCOUNT;
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);

		accountUsername.setTitle(R.string.w_common_phone_number);
		accountUsername.setDialogTitle(R.string.w_common_phone_number);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
        //Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
        
        CREATE_ACCOUNT = parent.getFreeSubActivityCode();
		
        updateAccountInfos(account);
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_UDP;
		account.reg_timeout = 180;
		return account;
	}
	
	@Override
	protected boolean canTcp() {
		return false;
	}
	
	
	

    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            // Not yet account balance helper
            //accountBalanceHelper.launchRequest(acc);
        } else {
            // add a row to link 
            customWizardText.setText(R.string.create_account);
            customWizard.setVisibility(View.VISIBLE);
            customWizard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    parent.startActivityForResult(new Intent(parent, MondotalkCreate.class), CREATE_ACCOUNT);
                }
            });
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
                }
            }
        }
    }
    
}
