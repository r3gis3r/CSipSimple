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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;


public class Vitelity extends SimpleImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "Vitelity";
	}
	
	ListPreference sipServer; 
	static SortedMap<String, String> providers = new TreeMap<String, String>(){
		private static final long serialVersionUID = -2561302247222706262L;
	{
		put("sip1", "sip1.vitelity.net");
        put("sip2", "sip2.vitelity.net");
        put("sip3", "sip3.vitelity.net");
        put("sip4", "sip4.vitelity.net");
        put("sip5", "sip5.vitelity.net");
        put("sip6", "sip6.vitelity.net");
        put("sip7", "sip7.vitelity.net");
        put("sip8", "sip8.vitelity.net");
        put("sip9", "sip9.vitelity.net");
	}
	};

	private static final String PROVIDER_LIST_KEY = "provider_list";
	
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		
		
		boolean recycle = true;
		sipServer = (ListPreference) findPreference(PROVIDER_LIST_KEY);
		if(sipServer == null) {
			sipServer = new ListPreference(parent);
			sipServer.setKey(PROVIDER_LIST_KEY);
			recycle = false;
		}
		
		CharSequence[] e = new CharSequence[providers.size()];
		CharSequence[] v = new CharSequence[providers.size()];
        int i = 0;
        for(String pv : providers.keySet()) {
        	e[i] = pv;
        	v[i] = providers.get(pv);
        	i++;
        }
		
		sipServer.setEntries(e);
		sipServer.setEntryValues(v);
		sipServer.setDialogTitle(R.string.w_common_server);
		sipServer.setTitle(R.string.w_common_server);
		sipServer.setDefaultValue("sip1.vitelity.net");

        if(!recycle) {
            addPreference(sipServer);
        }
        
        String domain = account.reg_uri;
        if( domain != null ) {
	        for(CharSequence state : v) {
                String currentComp = "sip:" + state;
	        	if( currentComp.equalsIgnoreCase(domain) ) {
                    sipServer.setValue(state.toString());
	        		break;
	        	}
	        }
        }
        
   }


	protected String getDomain() {
		String provider = sipServer.getValue();
		if(provider != null) {
			return provider;
		}
		return "";
	}
	
	@Override
	public void updateDescriptions() {
		super.updateDescriptions();
		setStringFieldSummary(PROVIDER_LIST_KEY);
	}

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		if(fieldName == PROVIDER_LIST_KEY) {
			if(sipServer != null) {
				return sipServer.getEntry().toString();
			}
		}
		
		return super.getDefaultFieldSummary(fieldName);
	}
	

    @Override
    public List<Filter> getDefaultFilters(SipProfile acc) {
        // For US and Canada resident, auto add 10 digits => prefix with 1 rewriting rule 
        String country = Locale.getDefault().getCountry();
        if (Locale.CANADA.getCountry().equals(country) || Locale.US.getCountry().equals(country)) {
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
