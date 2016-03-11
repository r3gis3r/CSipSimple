/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2011 JuanJo Ciarlante (aka jjo)
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
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Mobile4U extends SimpleImplementation {
	

	@Override
	protected String getDomain() {
		return "sip.mobile4u.hu";
	}
	
	@Override
	protected String getDefaultName() {
		return "Mobile4U";
	}

	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);

        accountUsername.setTitle(R.string.w_common_phone_number);
        accountUsername.setDialogTitle(R.string.w_common_phone_number);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_PHONE);
		
	}
	
	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName.equals(USER_NAME)) {
			return parent.getString(R.string.w_common_phone_number_desc);
		}
		return super.getDefaultFieldSummary(fieldName);
	}
	/*
	@Override
	public SipProfile buildAccount(SipProfile account) {
		SipProfile acc = super.buildAccount(account);
		return acc;
	}
	*/
	@Override
	public List<Filter> getDefaultFilters(SipProfile acc) {
        // Filter1: Rewrite >> Starts with "+" >> Replace match by "00"
        ArrayList<Filter> filters = new ArrayList<Filter>();

        Filter f = new Filter();
        f.account = (int) acc.id;
        f.action = Filter.ACTION_REPLACE;
        f.matchPattern = "^" + Pattern.quote("+") + "(.*)$";
        f.replacePattern = "00$1";
        f.matchType = Filter.MATCHER_STARTS;
        filters.add(f);

        // Filter2: Rewrite >> Starts with "06" >> Replace match by "0036"
        f = new Filter();
        f.account = (int) acc.id;
        f.action = Filter.ACTION_REPLACE;
        f.matchPattern = "^" + Pattern.quote("06") + "(.*)$";
        f.replacePattern = "0036$1";
        f.matchType = Filter.MATCHER_STARTS;
        filters.add(f);

        return filters;
	}
	
}
