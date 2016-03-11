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
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationFirstView;
import com.csipsimple.wizards.utils.AccountCreationFirstView.OnAccountCreationFirstViewListener;
import com.csipsimple.wizards.utils.AccountCreationWebview;
import com.csipsimple.wizards.utils.AccountCreationWebview.OnAccountCreationDoneListener;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;

public class Ippi extends SimpleImplementation implements OnAccountCreationDoneListener, OnAccountCreationFirstViewListener {


	protected static final String THIS_FILE = "IppiW";
	

	private LinearLayout customWizard;
	private TextView customWizardText;
    private AccountCreationWebview extAccCreator;

    private ViewGroup validationBar;
    private ViewGroup settingsContainer;


    private AccountCreationFirstView firstView;
	
	@Override
	protected String getDomain() {
		return "ippi.fr";
	}
	
	@Override
	protected String getDefaultName() {
		return "ippi";
	}
	
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

		//Get wizard specific row
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);

        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);
		
		updateAccountInfos(account);

        extAccCreator = new AccountCreationWebview(parent, "https://m.ippi.fr/subscribe/android.php", this);
	}
	
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, false); /* Seems to produce problems with TCP ? -- specific? */
		prefs.setPreferenceBooleanValue(SipConfigManager.USE_COMPACT_FORM, true);
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
			customWizard.setVisibility(View.GONE);
			accountBalanceHelper.launchRequest(acc);
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
	

    private AccountBalanceHelper accountBalanceHelper = new AccountBalance(this);
    
    private static class AccountBalance extends AccountBalanceHelper {
        
        WeakReference<Ippi> w;
        
        AccountBalance(Ippi wizard){
            w = new WeakReference<Ippi>(wizard);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {

            String requestURL = "https://soap.ippi.fr/credit/check_credit.php?"
                    + "login=" + acc.username
                    + "&code=" + MD5.MD5Hash(acc.data + DateFormat.format("yyyyMMdd", new Date()));

            return new HttpGet(requestURL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parseResponseLine(String line) {
            try {
                float value = Float.parseFloat(line.trim());
                if (value >= 0) {
                    return "Credit : " + Math.round(value * 100.0) / 100.0 + " euros";
                }
            } catch (NumberFormatException e) {
                Log.e(THIS_FILE, "Can't get value for line");
            }
            return null;
        }

        @Override
        public void applyResultError() {
            Ippi wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        @Override
        public void applyResultSuccess(String balanceText) {
            Ippi wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
        
    };
	
	
	@Override
	protected boolean canTcp() {
		return true;
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		//Proxy useless....?????
		//account.proxies = null;
		account.vm_nbr = "*1234";
        account.sip_stun_use = 0;
        account.media_stun_use = 0;
        account.ice_cfg_enable = 1;
        account.ice_cfg_use = 0;
		return account;
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
