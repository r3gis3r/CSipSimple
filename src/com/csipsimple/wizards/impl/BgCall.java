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
import com.csipsimple.utils.PreferencesWrapper;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class BgCall extends SimpleImplementation {

    private LinearLayout customWizard;
    private TextView customWizardText;
    
	@Override
	protected String getDomain() {
		return "bg-call.com";
	}

	@Override
	protected String getDefaultName() {
		return "BG-call";
	}

    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
        

        //Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
        
        updateAccountInfos(account);
	};
	
    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            //accountBalanceHelper.launchRequest(acc);
        }
    }
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);

        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"210");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"220");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"200");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_WB, "0");
        
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"200");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"210");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"220");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-16/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-24/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-32/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G726-40/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("opus/48000/1", SipConfigManager.CODEC_NB, "0");
	}
	

    @Override
    public boolean needRestart() {
        return true;
    }

    private AccountBalanceHelper accountBalanceHelper = new AccountBalance(this);
    
    private static class AccountBalance extends AccountBalanceHelper {
        
        WeakReference<BgCall> w;
        
        AccountBalance(BgCall wizard){
            w = new WeakReference<BgCall>(wizard);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {
            String requestURL = "http://bg-call.com/mv/csipsimple.mv?"
                    + "username=" + acc.username
                    + "&password=" + acc.data;

            return new HttpGet(requestURL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parseResponseLine(String line) {
            if(!TextUtils.isEmpty(line)) {
                return line;
            }
            return null;
        }

        @Override
        public void applyResultError() {
            BgCall wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        @Override
        public void applyResultSuccess(String balanceText) {
            BgCall wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
        
    };
}
