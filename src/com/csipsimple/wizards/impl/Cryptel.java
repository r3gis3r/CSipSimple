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
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationFirstView;
import com.csipsimple.wizards.utils.AccountCreationFirstView.OnAccountCreationFirstViewListener;
import com.csipsimple.wizards.utils.AccountCreationWebview;
import com.csipsimple.wizards.utils.AccountCreationWebview.OnAccountCreationDoneListener;


public class Cryptel extends SimpleImplementation implements OnAccountCreationDoneListener, OnAccountCreationFirstViewListener {

    
    private static final String webCreationPage = "http://50.28.50.63/index.php";

    private AccountCreationWebview extAccCreator;

    private ViewGroup settingsContainer;
    private ViewGroup validationBar;

    private AccountCreationFirstView firstView;
    
	@Override
	protected String getDomain() {
		return "sip.cryptelcore.net:5061";
	}

	@Override
	protected String getDefaultName() {
		return "Via Cryptel";
	}
	
	@Override
	public void fillLayout(SipProfile account) {
	    super.fillLayout(account);

        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
        accountPassword.setTitle(R.string.w_cryptel_password);
        accountPassword.setDialogTitle(R.string.w_cryptel_password);

        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);
        
        extAccCreator = new AccountCreationWebview(parent, webCreationPage, this);
        
        updateAccountInfos(account);
	}
	
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
	    acc.use_zrtp = 1;
	    acc.transport = SipProfile.TRANSPORT_TLS;
	    return acc;
	}

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);

        //Only G711a/u and g722 on WB
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"235");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"245");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "242");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "244");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_WB, "236");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_WB, "200");
        
        //On NB set for gsm high priority
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"245");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "244");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "239");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_NB, "236");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_NB, "200");
        
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
    }
    
    @Override
    public boolean needRestart() {
        return true;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccountCreationDone(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccountCreationDone(String username, String password, String extra) {
        onAccountCreationDone(username, password);
    }

    @Override
    public boolean saveAndQuit() {
        if(canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }

    @Override
    public void onCreateAccountRequested() {
        setFirstViewVisibility(false);
        extAccCreator.show();
    }

    @Override
    public void onEditAccountRequested() {
        setFirstViewVisibility(false);
    }

}
