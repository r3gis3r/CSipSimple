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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
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

public class Mobex extends SimpleImplementation {
    private static String THIS_FILE = "MobexW";


    private LinearLayout customWizard;
    private TextView customWizardText;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDomain() {
        return "200.152.124.172";
    }

    /**
     * {@inheritDoc}
     */
	@Override
	protected String getDefaultName() {
		return "Mobex";
	}
	
	private final static String USUAL_PREFIX = "12";
	
	/**
	 * {@inheritDoc}
	 */
    @Override
    public void fillLayout(SipProfile account) {
        super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        
        if(TextUtils.isEmpty(account.username)){
            accountUsername.setText(USUAL_PREFIX);
        }

        //Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
        

        updateAccountInfos(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canSave() {
        boolean ok = super.canSave();
        ok &= checkField(accountUsername, accountUsername.getText().trim().equalsIgnoreCase(USUAL_PREFIX));
        return ok;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        acc.proxies = null;
        String encodedUser = SipUri.encodeUser(accountUsername.getText().trim());
        account.acc_id = "0"+encodedUser+" <sip:"
                + encodedUser + "@"+getDomain()+">";
        return acc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {

        prefs.setPreferenceBooleanValue(SipConfigManager.ECHO_CANCELLATION, true);
        
        // G729 8 KHz, PCMA 8 KHz, PCMU 8 KHz and GSM 8 KHz
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"220");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"240");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "210");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_WB, "0");

        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"220");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"230");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"240");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "210");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_NB, "0");
        
    }
    
    // Account balance
    
    
    /**
     * Update view regarding account information.
     * For now only get the account balance.
     * @param acc The current SipProfile of this account.
     */
    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            accountBalanceHelper.launchRequest(acc);
        } else {
            customWizard.setVisibility(View.GONE);
        }
    }
    
    
    private AccountBalanceHelper accountBalanceHelper = new AccountBalance(this);
    
    private static class AccountBalance extends AccountBalanceHelper {
        
        private WeakReference<Mobex> w;

        private Pattern p = Pattern.compile("^.*<return xsi:type=\"xsd:string\">(.*)</return>.*$");
        

        AccountBalance(Mobex wizard){
            w = new WeakReference<Mobex>(wizard);
        }
        
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
                Log.d(THIS_FILE, "We parse " + "Creditos : " + strValue + " R$");
                //return "Creditos : " + strValue + " R$";
                return null;
            }
            return null;
        }
        
        @Override
        public HttpRequestBase getRequest(SipProfile acc)  throws IOException {

            String requestURL = "http://200.152.124.172/billing/webservice/Server.php";
            
            HttpPost httpPost = new HttpPost(requestURL);
            httpPost.addHeader("SOAPAction", "\"mostra_creditos\"");
            httpPost.addHeader("Content-Type", "text/xml");

            // prepare POST body
            String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope " +
                    "SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                    "xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
                    "xmlns:xsi=\"http://www.w3.org/1999/XMLSchema-instance\" " +
                    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/1999/XMLSchema\"" +
                    "><SOAP-ENV:Body><mostra_creditos SOAP-ENC:root=\"1\">" +
                    "<chave xsi:type=\"xsd:string\">" +
                    acc.data +
                    "</chave><username xsi:type=\"xsd:string\">" +
                    acc.username.replaceAll("^12", "") +
                    "</username></mostra_creditos></SOAP-ENV:Body></SOAP-ENV:Envelope>";
            Log.d(THIS_FILE, "Sending request for user " + acc.username.replaceAll("^12", ""));
            // set POST body
            HttpEntity entity = new StringEntity(body);
            httpPost.setEntity(entity);
            return httpPost;
        }

        @Override
        public void applyResultError() {
            Mobex wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        @Override
        public void applyResultSuccess(String balanceText) {
            Mobex wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
    };
}
