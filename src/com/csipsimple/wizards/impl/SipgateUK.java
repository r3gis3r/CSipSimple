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

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Base64;
import com.csipsimple.utils.Log;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SipgateUK extends SimpleImplementation {

    private static final String THIS_FILE = "Sipgate UK";
    private LinearLayout customWizard;
    private TextView customWizardText;

    @Override
    protected String getDomain() {
        return "sipgate.co.uk";
    }
    
    @Override
    protected String getDefaultName() {
        return "Sipgate";
    }

    
    //Customization
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        //Override titles
        accountDisplayName.setTitle(R.string.w_sipgate_display_name);
        accountDisplayName.setDialogTitle(R.string.w_sipgate_display_name);
        accountUsername.setTitle(R.string.w_sipgate_username);
        accountUsername.setDialogTitle(R.string.w_sipgate_username);
        accountPassword.setTitle(R.string.w_sipgate_password);
        accountPassword.setDialogTitle(R.string.w_sipgate_password);
        
        //Get wizard specific row for balance
        // Does not work currently
//        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
//        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
//        updateAccountInfos(account);
        
    }
    
    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        account.sip_stun_use = 0;
        account.media_stun_use = 0;
        account.allow_contact_rewrite = false;
        account.allow_via_rewrite = false;
        account.mwi_enabled = false;
        account.reg_timeout = 600;
        return account;
    }
	
	// Balance consulting
	private void updateAccountInfos(final SipProfile acc) {
		if (acc != null && acc.id != SipProfile.INVALID_ID) {
			customWizard.setVisibility(View.GONE);
			accountBalanceHelper.launchRequest(acc);
		} else {
			// add a row to link 
			customWizard.setVisibility(View.GONE);
			
		}
	}
	
    private AccountBalanceHelper accountBalanceHelper= new AccountBalance(this);
    
    private static class AccountBalance extends AccountBalanceHelper {
        
        WeakReference<SipgateUK> w;
        
        AccountBalance(SipgateUK wizard){
            w = new WeakReference<SipgateUK>(wizard);
        }

        Pattern p = Pattern.compile("^.*TotalIncludingVat</name><value><double>(.*)</double>.*$");

        /**
         * {@inheritDoc}
         */
        @Override
        public String parseResponseLine(String line) {
            Matcher matcher = p.matcher(line);
            if(matcher.matches()) {
                String strValue = matcher.group(1).trim();
                try {
                    float value = Float.parseFloat(strValue.trim());
                    if(value >= 0) {
                        strValue = Double.toString( Math.round(value * 100.0)/100.0 );
                    }
                }catch(NumberFormatException e) {
                    Log.d(THIS_FILE, "Can't parse float value in credit "+ strValue);
                }
                return "Creditos : " + strValue + " euros";
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc)  throws IOException {

            String requestURL = "https://samurai.sipgate.net/RPC2";
            HttpPost httpPost = new HttpPost(requestURL);
            // TODO : this is wrong ... we should use acc user/password instead of SIP ones, but we don't have it
            String userpassword = acc.username + ":" + acc.data;
            String encodedAuthorization = Base64.encodeBytes( userpassword.getBytes() );
            httpPost.addHeader("Authorization", "Basic " + encodedAuthorization);
            httpPost.addHeader("Content-Type", "text/xml");
            
            // prepare POST body
            String body = "<?xml version='1.0'?><methodCall><methodName>samurai.BalanceGet</methodName></methodCall>";

            // set POST body
            HttpEntity entity = new StringEntity(body);
            httpPost.setEntity(entity);
            return httpPost;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultError() {
            SipgateUK wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultSuccess(String balanceText) {
            SipgateUK wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
    };
	
}
