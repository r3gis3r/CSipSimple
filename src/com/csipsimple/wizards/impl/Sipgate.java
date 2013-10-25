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

import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Base64;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sipgate extends AlternateServerImplementation {

    protected static final String THIS_FILE = "SipgateW";
    
	private static final String PROXY_KEY = "proxy_server";

    private EditTextPreference accountProxy;

    private LinearLayout customWizard;
    private TextView customWizardText;


    @Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		//Override titles
		accountDisplayName.setTitle(R.string.w_sipgate_display_name);
		accountDisplayName.setDialogTitle(R.string.w_sipgate_display_name);
		accountServer.setTitle(R.string.w_common_server);
		accountServer.setDialogTitle(R.string.w_common_server);
		accountUsername.setTitle(R.string.w_sipgate_username);
		accountUsername.setDialogTitle(R.string.w_sipgate_username);
		accountPassword.setTitle(R.string.w_sipgate_password);
		accountPassword.setDialogTitle(R.string.w_sipgate_password);
		
		// Add optional proxy
        boolean recycle = true;
        accountProxy = (EditTextPreference) findPreference(PROXY_KEY);
        if(accountProxy == null) {
            accountProxy = new EditTextPreference(parent);
            accountProxy.setKey(PROXY_KEY);
            accountProxy.setTitle(R.string.w_advanced_proxy);
            accountProxy.setSummary(R.string.w_advanced_proxy_desc);
            accountProxy.setDialogMessage(R.string.w_advanced_proxy_desc);
            recycle = false;
        }

        if(!recycle) {
            addPreference(accountProxy);
        }
        
        String currentProxy = account.getProxyAddress();
        String currentServer = account.getSipDomain();
        if(!TextUtils.isEmpty(currentProxy) && !TextUtils.isEmpty(currentServer)
                && !currentProxy.equalsIgnoreCase(currentServer)) {
            accountProxy.setText(currentProxy);
        }

		
        if(TextUtils.isEmpty(account.getSipDomain())) {
            accountServer.setText("sipgate.de");
        }

		//Get wizard specific row for balance
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
		updateAccountInfos(account);
	}
	

	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		String nproxy = getText(accountProxy);
		if(!TextUtils.isEmpty(nproxy)) {
		    account.proxies = new String[] {"sip:"+nproxy};
		}
		account.transport = SipProfile.TRANSPORT_UDP;
		account.allow_contact_rewrite = false;
		account.allow_via_rewrite = false;
		return account;
	}

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		// Add stun server
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
		prefs.addStunServer("stun.sipgate.net:10000");
	}
	

	@Override
	protected String getDefaultName() {
		return "Sipgate";
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}
	

    @Override
    public String getDefaultFieldSummary(String fieldName) {
        if(PROXY_KEY.equals(fieldName)) {
            return parent.getString(R.string.w_advanced_proxy_desc);
        }
        return super.getDefaultFieldSummary(fieldName);
    }
	
    @Override
    public void updateDescriptions() {
        super.updateDescriptions();
        setStringFieldSummary(PROXY_KEY);
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
        
        WeakReference<Sipgate> w;
        
        AccountBalance(Sipgate wizard){
            w = new WeakReference<Sipgate>(wizard);
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
            Sipgate wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultSuccess(String balanceText) {
            Sipgate wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
    };
	
}
