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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */
package com.csipsimple.api;

import android.net.Uri;
import android.os.RemoteException;

public final class SipManager {
	// -------
	// Static constants
	// PERMISSION
	public static final String PERMISSION_USE_SIP = "android.permission.USE_SIP";
	public static final String PERMISSION_CONFIGURE_SIP = "android.permission.CONFIGURE_SIP";
	
	// SERVICE intents

	public static final String INTENT_SIP_CONFIGURATION = "com.csipsimple.service.SipConfiguration";
	public static final String INTENT_SIP_SERVICE = "com.csipsimple.service.SipService";
	public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "com.csipsimple.accounts.activate";
	public static final Object INTENT_GET_ACCOUNTS_LIST = "com.csipsimple.accounts.list";
	
	// -------
	// ACTIONS
	public static final String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
	public static final String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
	public static final String ACTION_SIP_CALLLOG = "com.csipsimple.phone.action.CALLLOG";
	public static final String ACTION_SIP_MESSAGES = "com.csipsimple.phone.action.MESSAGES";
	
	// SERVICE BROADCASTS
	public static final String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
    public static final String ACTION_SIP_ACCOUNT_CHANGED = "com.csipsimple.service.ACCOUNT_CHANGED";
	public static final String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
	public static final String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
	public static final String ACTION_SIP_CAN_BE_STOPPED = "com.csipsimple.service.ACTION_SIP_CAN_BE_STOPPED";
	public static final String ACTION_SIP_REQUEST_RESTART = "com.csipsimple.service.ACTION_SIP_REQUEST_RESTART";
	
	public static final String ACTION_ZRTP_SHOW_SAS = "com.csipsimple.service.SHOW_SAS";
	
	public static final String ACTION_SIP_MESSAGE_RECEIVED = "com.csipsimple.service.MESSAGE_RECEIVED";
	//TODO : message sent?
	public static final String ACTION_SIP_MESSAGE_STATUS = "com.csipsimple.service.MESSAGE_STATUS";
	public static final String ACTION_GET_DRAWABLES = "com.csipsimple.themes.GET_DRAWABLES";
	public static final String ACTION_GET_PHONE_HANDLERS = "com.csipsimple.phone.action.HANDLE_CALL";
	public static final String ACTION_GET_EXTRA_CODECS = "com.csipsimple.codecs.action.REGISTER_CODEC";
	public static final String ACTION_GET_VIDEO_PLUGIN = "com.csipsimple.plugins.action.REGISTER_VIDEO";
	
	public static final String META_LIB_NAME = "lib_name";
	public static final String META_LIB_INIT_FACTORY = "init_factory";
	
	// Content provider
	public static final String AUTHORITY = "com.csipsimple.db";
	public final static String BASE_DIR_TYPE = "vnd.android.cursor.dir/vnd.csipsimple";
	public final static String BASE_ITEM_TYPE = "vnd.android.cursor.item/vnd.csipsimple";
	public final static String CONTENT_SCHEME = "content://";
	
	// Content Provider - call logs
	public final static String CALLLOGS_TABLE_NAME = "calllogs";
	public final static String CALLLOG_CONTENT_TYPE = BASE_DIR_TYPE + ".calllog";
	public final static String CALLLOG_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + ".calllog";
	public final static Uri CALLLOG_URI =  Uri.parse(CONTENT_SCHEME + AUTHORITY + "/" + CALLLOGS_TABLE_NAME);
	public final static Uri CALLLOG_ID_URI_BASE = Uri.parse(CONTENT_SCHEME + AUTHORITY + "/" + CALLLOGS_TABLE_NAME + "/");
	// -- Extra fields for call logs
	/**
	 * The account used for this call
	 */
	public final static String CALLLOG_PROFILE_ID_FIELD = "account_id";
	/**
	 * The final latest status code for this call 
	 */
	public final static String CALLLOG_STATUS_CODE_FIELD = "status_code";
	/**
     * The final latest status text for this call 
     */
    public final static String CALLLOG_STATUS_TEXT_FIELD = "status_text";
	
	
	// Content Provider - filter
	public static final String FILTERS_TABLE_NAME = "outgoing_filters";
	public final static String FILTER_CONTENT_TYPE = BASE_DIR_TYPE + ".filter";
	public final static String FILTER_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + ".filter";
	public final static Uri FILTER_URI =  Uri.parse(CONTENT_SCHEME + AUTHORITY + "/" + FILTERS_TABLE_NAME);
	public final static Uri FILTER_ID_URI_BASE = Uri.parse(CONTENT_SCHEME + AUTHORITY + "/" + FILTERS_TABLE_NAME + "/");
	
	
	// EXTRAS
	public static final String EXTRA_CALL_INFO = "call_info";
	public static final String EXTRA_PROFILES = "profiles";
	
	
	// Constants
	public static final int SUCCESS = 0;
	public static final int ERROR_CURRENT_NETWORK = 10;
	
	public static final int CURRENT_API = 1003;
    
	
	public static boolean isApiCompatible(ISipService service) {
		if(service != null) {
			try {
				int version = service.getVersion();
				return (Math.floor(version / 1000) == Math.floor(CURRENT_API % 1000));
			} catch (RemoteException e) {
				// We consider this is a bad api version that does not have versionning at all
				return false;
			}
		}
		
		return false;
	}
}
