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
 *  
 *  This file and this file only is released under dual Apache license
 */
package com.csipsimple.api;

public final class SipManager {
	// -------
	// Static constants
	// -------
	// ACTIONS
	public static final String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
	public static final String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
	public static final String ACTION_SIP_CALLLOG = "com.csipsimple.phone.action.CALLLOG";
	public static final String ACTION_SIP_MESSAGES = "com.csipsimple.phone.action.MESSAGES";
	
	// SERVICE BROADCASTS
	public static final String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
	public static final String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	public static final String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
	public static final String ACTION_SIP_ACCOUNT_ACTIVE_CHANGED = "com.csipsimple.service.ACCOUNT_ACTIVE_CHANGED";
	public static final String ACTION_SIP_MESSAGE_RECEIVED = "com.csipsimple.service.MESSAGE_RECEIVED";
	//TODO : message sent?
	public static final String ACTION_SIP_MESSAGE_STATUS = "com.csipsimple.service.MESSAGE_STATUS";
	
	
	// EXTRAS
	public static final String EXTRA_CALL_INFO = "call_info";
	public static final String EXTRA_ACCOUNT_ID = "acc_id";
	public static final String EXTRA_ACTIVATE = "activate";
	
	
	
}
