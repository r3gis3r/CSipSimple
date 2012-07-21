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

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Callcentric extends SimpleImplementation {
	
	@Override
	protected String getDomain() {
		return "callcentric.com";
	}
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		accountUsername.setTitle(R.string.w_callcentric_phone_number);
		accountUsername.setDialogTitle(R.string.w_callcentric_phone_number);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
		accountPassword.setTitle(R.string.w_callcentric_password);
		accountPassword.setDialogTitle(R.string.w_callcentric_password);
	}

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_callcentric_phone_number_desc);
		}else if(fieldName.equals(PASSWORD)) {
			return parent.getString(R.string.w_callcentric_password_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}

	@Override
	protected String getDefaultName() {
		return "Callcentric";
	}
	

	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.contact_rewrite_method = 1;
		account.mwi_enabled = false;
		return account;
	}
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
	    super.setDefaultParams(prefs);
	    prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_DNS_SRV, true);
	    prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_VAD, false);
	}
	@Override
	public boolean needRestart() {
	    return true;
	}
	
	@Override
	public List<Filter> getDefaultFilters(SipProfile acc) {
	    // For US and Canada resident, auto add 10 digits => prefix with 1 rewriting rule 
	    if(Locale.CANADA.getCountry().equals(Locale.getDefault().getCountry()) || Locale.US.getCountry().equals(Locale.getDefault().getCountry())) {
	        ArrayList<Filter> filters = new ArrayList<Filter>();
            
            Filter f = new Filter();
            f.account = (int) acc.id;
            f.action = Filter.ACTION_REPLACE;
            f.matchPattern = "^(\\d{10})$";
            f.replacePattern = "1$0";
            f.matchType = Filter.MATCHER_HAS_N_DIGIT;
            filters.add(f);
            
            return filters;
	    }
	    return null;
	}
}
