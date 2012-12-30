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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationWebview;
import com.csipsimple.wizards.utils.AccountCreationWebview.OnAccountCreationDoneListener;

public class VoipTel extends SimpleImplementation  implements OnAccountCreationDoneListener {

    private static final String webCreationPage = "http://212.4.110.135:8080/subscriber/newSubscriberFree/alta?execution=e2s1";
    

    private LinearLayout customWizard;
    private TextView customWizardText;
    private AccountCreationWebview extAccCreator;

	@Override
	protected String getDomain() {
		return "voip.voiptel.ie";
	}
	
	@Override
	protected String getDefaultName() {
		return "Voiptel Mobile";
	}
	
	@Override
	protected boolean canTcp() {
		return false;
	}
	
	@Override
	public void fillLayout(SipProfile account) {
		super.fillLayout(account);

		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
        //Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
        extAccCreator = new AccountCreationWebview(parent, webCreationPage, this);
        
        updateAccountInfos(account);
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

		prefs.setCodecPriority("g729/8000/1", SipConfigManager.CODEC_NB, "240");
		prefs.setCodecPriority("g729/8000/1", SipConfigManager.CODEC_WB, "240");
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
	


    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
        } else {
            // add a row to link 
            customWizardText.setText(R.string.create_account);
            customWizard.setVisibility(View.VISIBLE);
            customWizard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    extAccCreator.show();
                }
            });
        }
    }

    @Override
    public void onAccountCreationDone(String username, String password) {
        setUsername(username);
        setPassword(password);
    }
    

    @Override
    public boolean saveAndQuit() {
        if(canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }
}
