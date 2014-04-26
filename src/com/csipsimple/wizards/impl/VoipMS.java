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


public class VoipMS extends SimpleImplementation {
	
	
	@Override
	protected String getDefaultName() {
		return "VoIP.ms";
	}
	
	ListPreference sipServer; 
	static SortedMap<String, String> providers = new TreeMap<String, String>(){
		private static final long serialVersionUID = -2561302247222706262L;
	{
		put("Atlanta, GA", "atlanta.voip.ms");
        put("Atlanta 2, GA", "atlanta2.voip.ms");
		put("Chicago, IL", "chicago.voip.ms");
        put("Chicago 2, IL", "chicago2.voip.ms");
        put("Chicago 3, IL", "chicago3.voip.ms");
        put("Chicago 4, IL", "chicago4.voip.ms");
		put("Dallas, TX", "dallas.voip.ms");
        put("Denver, Colorado", "denver.voip.ms");
        put("Denver 2, Colorado", "denver2.voip.ms");
		put("Houston, TX", "houston.voip.ms");
		put("Los Angeles, CA", "losangeles.voip.ms");
        put("Los Angeles 2, CA", "losangeles2.voip.ms");
		put("New York, NY", "newyork.voip.ms");
        put("New York 2, NY", "newyork2.voip.ms");
        put("New York 3, NY", "newyork3.voip.ms");
        put("New York 4, NY", "newyork4.voip.ms");
		put("Seattle, WA", "seattle.voip.ms");
        put("Seattle 2, WA", "seattle2.voip.ms");
        put("Seattle 3, WA", "seattle3.voip.ms");
		put("Tampa, FL", "tampa.voip.ms");
        put("Montreal,QC", "montreal.voip.ms");
		put("Montreal 2,QC", "montreal2.voip.ms");
        put("Montreal 3,QC", "montreal3.voip.ms");
        put("Montreal 4,QC", "montreal4.voip.ms");
        put("Toronto, ON", "toronto.voip.ms");
		put("Toronto 2, ON", "toronto2.voip.ms");
        put("Toronto 3, ON", "toronto3.voip.ms");
        put("Toronto 4, ON", "toronto4.voip.ms");
        put("Washington, DC", "washington.voip.ms");
        put("Washington 2, DC", "washington2.voip.ms");
		put("London, UK", "london.voip.ms");
        put("London, UK", "vancouver..voip.ms");
        put("London, UK", "vancouver2.voip.ms");
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
        sipServer.setDefaultValue("atlanta.voip.ms");
        
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
	
	/* (non-Javadoc)
	 * @see com.csipsimple.wizards.impl.SimpleImplementation#buildAccount(com.csipsimple.api.SipProfile)
	 */
	@Override
	public SipProfile buildAccount(SipProfile account) {
	    SipProfile acc = super.buildAccount(account);
	    acc.vm_nbr = "*97";
	    return acc;
	}
}
