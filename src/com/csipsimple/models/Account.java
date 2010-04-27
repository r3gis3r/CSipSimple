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
package com.csipsimple.models;

import org.pjsip.pjsua.pjsua;
import org.pjsip.pjsua.pjsuaConstants;
import org.pjsip.pjsua.pjsua_acc_config;
import org.pjsip.pjsua.pjsua_acc_info;

import com.csipsimple.service.SipService;

public class Account {
	//For now everything is public, easiest to manage
	public String display_name;
	public String wizard;
	public boolean active;
	public pjsua_acc_config cfg;
	public Integer id;
	
	public Account() {
		display_name = "";
		wizard = "EXPERT";
		active = true;
		cfg = new pjsua_acc_config();
		pjsua.acc_config_default(cfg);
		
		
	}
	
	public pjsua_acc_info getPjAccountInfo() {
		pjsua_acc_info acc_info = new pjsua_acc_info();
		//TODO : try catch since if no stack available.... it fails
		int success = pjsua.acc_get_info(SipService.active_acc_map.get(id), acc_info);
		if(success != pjsuaConstants.PJ_SUCCESS) {
			return null;
		}
		return acc_info;
	}
	
}
