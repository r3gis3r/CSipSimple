/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.pjsip;

import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjmedia_srtp_use;
import org.pjsip.pjsua.pjsip_cred_info;
import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_config;

import android.content.Context;
import android.text.TextUtils;

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class PjSipAccount {
	
	//private static final String THIS_FILE = "PjSipAcc";
	
	
	//For now everything is public, easiest to manage
	public String display_name;
	public String wizard;
	public boolean active;
	public pjsua_acc_config cfg;
	public Integer id;
	public Integer transport = 0;
	
	

	
	
	public PjSipAccount() {
		cfg = new pjsua_acc_config();
		
		pjsua.acc_config_default(cfg);
		// By default keep alive interval is now 0 since it's managed globally at android level
		cfg.setKa_interval(0);
	}
	
	public PjSipAccount(SipProfile profile) {
		if(profile.id != SipProfile.INVALID_ID) {
			id = profile.id;
		}
		display_name = profile.display_name;
		wizard = profile.wizard;
		transport = profile.transport;
		active = profile.active;
		transport = profile.transport;

		cfg = new pjsua_acc_config();
		pjsua.acc_config_default(cfg);
		
		cfg.setPriority(profile.priority);
		if(profile.acc_id != null) {
			cfg.setId(pjsua.pj_str_copy(profile.acc_id));
		}
		if(profile.reg_uri != null) {
			cfg.setReg_uri(pjsua.pj_str_copy(profile.reg_uri));
		}
		if(profile.publish_enabled != -1) {
			cfg.setPublish_enabled(profile.publish_enabled);
		}
		if(profile.reg_timeout != -1) {
			cfg.setReg_timeout(profile.reg_timeout);
		}
		if(profile.ka_interval != -1) {
			cfg.setKa_interval(profile.ka_interval);
		}
		if(profile.pidf_tuple_id != null) {
			cfg.setPidf_tuple_id(pjsua.pj_str_copy(profile.pidf_tuple_id));
		}
		if(profile.force_contact != null) {
			cfg.setForce_contact(pjsua.pj_str_copy(profile.force_contact));
		}
		
		cfg.setAllow_contact_rewrite(profile.allow_contact_rewrite ? pjsuaConstants.PJ_TRUE : pjsuaConstants.PJ_FALSE);
		cfg.setContact_rewrite_method(profile.contact_rewrite_method);
		
		
		if(profile.use_srtp != -1) {
			cfg.setUse_srtp(pjmedia_srtp_use.swigToEnum(profile.use_srtp));
			cfg.setSrtp_secure_signaling(0);
		}
		
		if(profile.proxies != null) {
			Log.d("PjSipAccount", "Create proxy "+profile.proxies.length);
			cfg.setProxy_cnt(profile.proxies.length);
			pj_str_t[] proxies = cfg.getProxy();
			int i = 0;
			for(String proxy : profile.proxies) {
				Log.d("PjSipAccount", "Add proxy "+proxy);
				proxies[i] = pjsua.pj_str_copy(proxy);
				i += 1;
			}
			cfg.setProxy(proxies);
		}else {
			cfg.setProxy_cnt(0);
		}

		if(profile.username != null || profile.data != null) {
			cfg.setCred_count(1);
			pjsip_cred_info cred_info = cfg.getCred_info();
			
			if(profile.realm != null) {
				cred_info.setRealm(pjsua.pj_str_copy(profile.realm));
			}
			if(profile.username != null) {
				cred_info.setUsername(pjsua.pj_str_copy(profile.username));
			}
			if(profile.datatype != -1) {
				cred_info.setData_type(profile.datatype);
			}
			if(profile.data != null) {
				cred_info.setData(pjsua.pj_str_copy(profile.data));
			}
		}else {
			cfg.setCred_count(0);
		}
	}
	
	


	public void applyExtraParams(Context ctxt) {
		
		// Transport
		String regUri = "";
		String argument = "";
		switch (transport) {
		case SipProfile.TRANSPORT_UDP:
			argument = ";lr;transport=UDP";
			break;
		case SipProfile.TRANSPORT_TCP:
			argument = ";lr;transport=TCP";
			break;
		case SipProfile.TRANSPORT_TLS:
			//TODO : differentiate ssl/tls ?
			argument = ";lr;transport=TLS";
			break;
		default:
			break;
		}
		
		if (!TextUtils.isEmpty(argument)) {
			regUri = cfg.getReg_uri().getPtr();
			if(!TextUtils.isEmpty(regUri)) {
				long initialProxyCnt = cfg.getProxy_cnt();
				pj_str_t[] proxies = cfg.getProxy();
				
				//TODO : remove lr and transport from uri
		//		cfg.setReg_uri(pjsua.pj_str_copy(proposed_server));
				
				if (initialProxyCnt == 0 || TextUtils.isEmpty(proxies[0].getPtr())) {
					cfg.setReg_uri(pjsua.pj_str_copy(regUri + argument));
					cfg.setProxy_cnt(0);
				} else {
					proxies[0] = pjsua.pj_str_copy(proxies[0].getPtr() + argument);
					cfg.setProxy(proxies);
				}
//				} else {
//					proxies[0] = pjsua.pj_str_copy(proxies[0].getPtr() + argument);
//					cfg.setProxy(proxies);
//				}
			}
		}
		
		//Caller id
		PreferencesWrapper prefs = new PreferencesWrapper(ctxt);
		String defaultCallerid = prefs.getPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID);
		
		
		// If one default caller is set 
		if (!TextUtils.isEmpty(defaultCallerid)) {
			String accId = cfg.getId().getPtr();
			ParsedSipContactInfos parsedInfos = SipUri.parseSipContact(accId);
			if (TextUtils.isEmpty(parsedInfos.displayName)) {
				// Apply new display name
				parsedInfos.displayName = defaultCallerid;
				cfg.setId(pjsua.pj_str_copy(parsedInfos.toString()));
			}
		}
	}
	
	
	@Override
	public boolean equals(Object o) {
		if(o != null && o.getClass() == PjSipAccount.class) {
			PjSipAccount oAccount = (PjSipAccount) o;
			return oAccount.id == id;
		}
		return super.equals(o);
	}
}
