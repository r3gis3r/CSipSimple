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

import android.preference.ListPreference;

import com.csipsimple.R;
import com.csipsimple.api.SipProfile;

import java.util.SortedMap;
import java.util.TreeMap;


public class SuperCel extends SimpleImplementation {

    ListPreference sipServer; 
    static SortedMap<String, String> providers = new TreeMap<String, String>(){
    /**
         * 
         */
        private static final long serialVersionUID = 2291298928765427732L;

    {
        put("SuperCel", "67.222.131.147");
        put("SuperCel-2", "sip2.supersip.tk");
        put("SuperCel-3", "sip3.supersip.tk");
        put("SuperCel-4", "sip4.supersip.tk");
    }
    };

    private static final String PROVIDER_LIST_KEY = "provider_list";

	@Override
	protected String getDomain() {
        String provider = sipServer.getValue();
        if(provider != null) {
            return provider;
        }
		return "67.222.131.147";
	}
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#fillLayout(com.csipsimple.api.SipProfile)
	 */
	@Override
	public void fillLayout(SipProfile account) {
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
        sipServer.setDefaultValue("67.222.131.147");

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
	
	@Override
	protected String getDefaultName() {
		return "SuperCel";
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
}
