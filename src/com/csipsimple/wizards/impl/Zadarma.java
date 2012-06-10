/**
 * Copyright (C) 2011 Dmytro Tokar
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
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;
import com.csipsimple.wizards.impl.AccountCreationWebview.OnAccountCreationDoneListener;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class Zadarma extends SimpleImplementation implements OnAccountCreationDoneListener {

    protected static final String THIS_FILE = "ZadarmaW";

    private static final String webCreationPage = "https://ss.zadarma.com/android/registration/";

    private LinearLayout customWizard;
    private TextView customWizardText;
    private AccountCreationWebview extAccCreator;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDomain() {
        return "Zadarma.com";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultName() {
        return "Zadarma";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

        // Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);

        updateAccountInfos(account);

        extAccCreator = new AccountCreationWebview(parent, webCreationPage, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needRestart() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
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
    public boolean saveAndQuit() {
        if (canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }

    // Balance consult

    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            accountBalanceHelper.launchRequest(acc);
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

    private AccountBalanceHelper accountBalanceHelper = new AccountBalanceHelper() {

        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {

            String requestURL = "https://ss.zadarma.com/android/getbalance/?"
                    + "login=" + acc.username
                    + "&code=" + MD5.MD5Hash(acc.data);

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
                    return "Balance : " + Math.round(value * 100.0) / 100.0 + " USD";
                }
            } catch (NumberFormatException e) {
                Log.e(THIS_FILE, "Can't get value for line");
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultError() {
            customWizard.setVisibility(View.GONE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultSuccess(String balanceText) {
            customWizardText.setText(balanceText);
            customWizard.setVisibility(View.VISIBLE);
        }

    };
}
