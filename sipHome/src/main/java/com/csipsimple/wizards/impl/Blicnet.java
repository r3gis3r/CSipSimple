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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Blicnet extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "sip.blic.net";
	}
	
	@Override
	protected String getDefaultName() {
		return "Blicnet";
	}

    private final static String USUAL_PREFIX = "200044";
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
        accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);

        if(TextUtils.isEmpty(account.username)){
            accountUsername.setText(USUAL_PREFIX);
        }
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

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.proxies = null;
        account.allow_contact_rewrite = false;
        account.contact_rewrite_method = 1;
        account.try_clean_registers = 0;
        account.allow_via_rewrite = false;
        account.mwi_enabled = false;
        account.transport = SipProfile.TRANSPORT_UDP;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
        prefs.setPreferenceStringValue(SipConfigManager.DTMF_MODE, Integer.toString(SipConfigManager.DTMF_MODE_INFO));
        
        // g729 and PCMA as fallback
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB,"199");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB,"200");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB,"245");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB,"0");
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
        
        //On NB g729 and PCMA as fallback
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB,"199");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB,"200");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB,"0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB,"245");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB,"0");
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
	

    @Override
    public List<Filter> getDefaultFilters(SipProfile acc) {
        ArrayList<Filter> filters = new ArrayList<Filter>();
        
        Filter f = new Filter();
        f.account = (int) acc.id;
        f.action = Filter.ACTION_REPLACE;
        f.matchPattern = "^" + Pattern.quote("+") + "(.*)$";
        f.replacePattern = "00$1";
        f.matchType = Filter.MATCHER_STARTS;
        filters.add(f);
        
        return filters;
    }
}
