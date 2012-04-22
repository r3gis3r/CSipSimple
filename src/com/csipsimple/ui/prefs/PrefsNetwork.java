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

package com.csipsimple.ui.prefs;

import android.telephony.TelephonyManager;
import android.view.MenuItem;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.PreferencesWrapper;


public class PrefsNetwork extends GenericPrefs {
	private static final String NAT_TRAVERSAL_KEY = "nat_traversal";
	private static final String TRANSPORT_KEY = "transport";
	
	@Override
	protected int getXmlPreferences() {
		return R.xml.prefs_network;
		
	}
	/*
	@Override
	protected void beforeBuildPrefs() {
		super.beforeBuildPrefs();
		
		ActionBar ab = getActionBar();
		if(ab != null) {
			ab.setDisplayHomeAsUpEnabled(true);
		}
		
	}
	*/
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int selId = item.getItemId();
		if(selId == Compatibility.getHomeMenuId()) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void afterBuildPrefs() {
		super.afterBuildPrefs();
		TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
		
		if (telephonyManager.getPhoneType() == 2 /*TelephonyManager.PHONE_TYPE_CDMA*/) {
			hidePreference("for_incoming", "use_gprs_in");
			hidePreference("for_outgoing", "use_gprs_out");
			hidePreference("for_incoming", "use_edge_in");
			hidePreference("for_outgoing", "use_edge_out");
		}
		PreferencesWrapper pfw = new PreferencesWrapper(this);
		

		if(!Compatibility.isCompatible(9)) {
			hidePreference("perfs", SipConfigManager.LOCK_WIFI_PERFS);
		}
		
		if(!pfw.isAdvancedUser()) {
			hidePreference(null, "perfs");
			
			hidePreference(NAT_TRAVERSAL_KEY, SipConfigManager.ENABLE_TURN);
			hidePreference(NAT_TRAVERSAL_KEY, SipConfigManager.TURN_SERVER);
			hidePreference(NAT_TRAVERSAL_KEY, SipConfigManager.TURN_USERNAME);
			hidePreference(NAT_TRAVERSAL_KEY, SipConfigManager.TURN_PASSWORD);
			
			hidePreference(TRANSPORT_KEY, SipConfigManager.ENABLE_TCP);
			hidePreference(TRANSPORT_KEY, SipConfigManager.ENABLE_UDP);
			hidePreference(TRANSPORT_KEY, SipConfigManager.TCP_TRANSPORT_PORT);
			hidePreference(TRANSPORT_KEY, SipConfigManager.UDP_TRANSPORT_PORT);
			hidePreference(TRANSPORT_KEY, SipConfigManager.RTP_PORT);
			hidePreference(TRANSPORT_KEY, SipConfigManager.USE_IPV6);
			hidePreference(TRANSPORT_KEY, SipConfigManager.OVERRIDE_NAMESERVER);
			hidePreference(TRANSPORT_KEY, SipConfigManager.FORCE_NO_UPDATE);
			
			hidePreference(TRANSPORT_KEY, SipConfigManager.ENABLE_QOS);
			hidePreference(TRANSPORT_KEY, SipConfigManager.DSCP_VAL);
			hidePreference(TRANSPORT_KEY, SipConfigManager.USER_AGENT);
			
			hidePreference(TRANSPORT_KEY, SipConfigManager.TIMER_MIN_SE);
			hidePreference(TRANSPORT_KEY, SipConfigManager.TIMER_SESS_EXPIRES);
			

            hidePreference("for_incoming", "use_anyway_in");
            hidePreference("for_outgoing", "use_anyway_out");
            
            hidePreference(null, "sip_protocol");
		}
		
		boolean canUseTLS = pfw.getLibCapability(PreferencesWrapper.LIB_CAP_TLS);
        if(!canUseTLS) {
            hidePreference(null, "tls");
            hidePreference("secure_media", SipConfigManager.USE_ZRTP);
        }
        
	}

	@Override
	protected void updateDescriptions() {
		setStringFieldSummary(SipConfigManager.STUN_SERVER);
	}
	
}
