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

import android.content.ContentResolver;
import android.net.Uri;
import android.os.RemoteException;

/**
 * Manage SIP application globally <br/>
 * Define intent, action, broadcast, extra constants <br/>
 * It also define authority and uris for some content holds by the internal
 * database
 */
public final class SipManager {
    // -------
    // Static constants
    // PERMISSION
    /**
     * Permission that allows to use sip : place call, control call etc.
     */
    public static final String PERMISSION_USE_SIP = "android.permission.USE_SIP";
    /**
     * Permission that allows to configure sip engine : preferences, accounts.
     */
    public static final String PERMISSION_CONFIGURE_SIP = "android.permission.CONFIGURE_SIP";

    // SERVICE intents
    /**
     * Used to bind sip service to configure it.<br/>
     * This method has been deprected and should not be used anymore. <br/>
     * Use content provider approach instead
     * 
     * @see SipConfigManager
     */
    public static final String INTENT_SIP_CONFIGURATION = "com.csipsimple.service.SipConfiguration";
    /**
     * Bind sip service to control calls.
     * 
     * @see ISipService
     */
    public static final String INTENT_SIP_SERVICE = "com.csipsimple.service.SipService";
    /**
     * Shortcut to turn on / off a sip account.
     * <p>
     * Expected Extras :
     * <ul>
     * <li>{@link SipProfile#FIELD_ID} as Long to choose the account to
     * activate/deactivate</li>
     * <li><i>{@link SipProfile#FIELD_ACTIVE} - optional </i> as boolean to
     * choose if should be activated or deactivated</li>
     * </ul>
     * </p>
     */
    public static final String INTENT_SIP_ACCOUNT_ACTIVATE = "com.csipsimple.accounts.activate";

    // -------
    // ACTIONS
    /**
     * Action launched when a sip call is ongoing.
     * <p>
     * Provided extras :
     * <ul>
     * <li>{@link #EXTRA_CALL_INFO} a {@link SipCallSession} containing infos of
     * the call</li>
     * </ul>
     * </p>
     */
    public static final String ACTION_SIP_CALL_UI = "com.csipsimple.phone.action.INCALL";
    /**
     * Action launched when the status icon clicked.<br/>
     * Should raise the dialer.
     */
    public static final String ACTION_SIP_DIALER = "com.csipsimple.phone.action.DIALER";
    /**
     * Action launched when a missed call notification entry is clicked.<br/>
     * Should raise call logs list.
     */
    public static final String ACTION_SIP_CALLLOG = "com.csipsimple.phone.action.CALLLOG";
    /**
     * Action launched when a sip message notification entry is clicked.<br/>
     * Should raise the sip message list.
     */
    public static final String ACTION_SIP_MESSAGES = "com.csipsimple.phone.action.MESSAGES";

    // SERVICE BROADCASTS
    /**
     * Broadcast sent when call state has changed.
     * <p>
     * Provided extras :
     * <ul>
     * <li>{@link #EXTRA_CALL_INFO} a {@link SipCallSession} containing infos of
     * the call</li>
     * </ul>
     * </p>
     */
    public static final String ACTION_SIP_CALL_CHANGED = "com.csipsimple.service.CALL_CHANGED";
    /**
     * Broadcast sent when sip account has been changed.
     * <p>
     * Provided extras :
     * <ul>
     * <li>{@link SipProfile#FIELD_ID} the long id of the account</li>
     * </ul>
     * </p>
     */
    public static final String ACTION_SIP_ACCOUNT_CHANGED = "com.csipsimple.service.ACCOUNT_CHANGED";
    /**
     * Broadcast sent when sip account registration has changed.
     * <p>
     * Provided extras :
     * <ul>
     * <li>{@link SipProfile#FIELD_ID} the long id of the account</li>
     * </ul>
     * </p>
     */
    public static final String ACTION_SIP_REGISTRATION_CHANGED = "com.csipsimple.service.REGISTRATION_CHANGED";
    /**
     * Broadcast sent when the state of device media has been changed.
     */
    public static final String ACTION_SIP_MEDIA_CHANGED = "com.csipsimple.service.MEDIA_CHANGED";
    /**
     * Broadcast sent when a ZRTP SAS
     */
    public static final String ACTION_ZRTP_SHOW_SAS = "com.csipsimple.service.SHOW_SAS";

    public static final String ACTION_SIP_MESSAGE_RECEIVED = "com.csipsimple.service.MESSAGE_RECEIVED";

    // REGISTERED BROADCASTS
    /**
     * Broadcast to send when the sip service can be stopped.
     */
    public static final String ACTION_SIP_CAN_BE_STOPPED = "com.csipsimple.service.ACTION_SIP_CAN_BE_STOPPED";
    /**
     * Broadcast to send when the sip service should be restarted.
     */
    public static final String ACTION_SIP_REQUEST_RESTART = "com.csipsimple.service.ACTION_SIP_REQUEST_RESTART";

    // PLUGINS BROADCASTS
    /**
     * Plugin action for themes.
     */
    public static final String ACTION_GET_DRAWABLES = "com.csipsimple.themes.GET_DRAWABLES";
    /**
     * Plugin action for call handlers.
     */
    public static final String ACTION_GET_PHONE_HANDLERS = "com.csipsimple.phone.action.HANDLE_CALL";
    /**
     * Plugin action for audio codec.
     */
    public static final String ACTION_GET_EXTRA_CODECS = "com.csipsimple.codecs.action.REGISTER_CODEC";
    /**
     * Plugin action for video.
     */
    public static final String ACTION_GET_VIDEO_PLUGIN = "com.csipsimple.plugins.action.REGISTER_VIDEO";
    /**
     * Meta constant name for library name.
     */
    public static final String META_LIB_NAME = "lib_name";
    /**
     * Meta constant name for the factory name.
     */
    public static final String META_LIB_INIT_FACTORY = "init_factory";

    // Content provider
    /**
     * Authority for regular database of the application.
     */
    public static final String AUTHORITY = "com.csipsimple.db";
    /**
     * Base content type for csipsimple objects.
     */
    public static final String BASE_DIR_TYPE = "vnd.android.cursor.dir/vnd.csipsimple";
    /**
     * Base item content type for csipsimple objects.
     */
    public static final String BASE_ITEM_TYPE = "vnd.android.cursor.item/vnd.csipsimple";

    // Content Provider - call logs
    /**
     * Table name for call logs.
     */
    public static final String CALLLOGS_TABLE_NAME = "calllogs";
    /**
     * Content type for call logs provider.
     */
    public static final String CALLLOG_CONTENT_TYPE = BASE_DIR_TYPE + ".calllog";
    /**
     * Item type for call logs provider.
     */
    public static final String CALLLOG_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + ".calllog";
    /**
     * Uri for call log content provider.
     */
    public static final Uri CALLLOG_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + AUTHORITY + "/"
            + CALLLOGS_TABLE_NAME);
    /**
     * Base uri for a specific call log. Should be appended with id of the call log.
     */
    public static final Uri CALLLOG_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + AUTHORITY + "/"
            + CALLLOGS_TABLE_NAME + "/");
    // -- Extra fields for call logs
    /**
     * The account used for this call
     */
    public static final String CALLLOG_PROFILE_ID_FIELD = "account_id";
    /**
     * The final latest status code for this call.
     */
    public static final String CALLLOG_STATUS_CODE_FIELD = "status_code";
    /**
     * The final latest status text for this call.
     */
    public static final String CALLLOG_STATUS_TEXT_FIELD = "status_text";

    // Content Provider - filter
    /**
     * Table name for filters/rewriting rules.
     */
    public static final String FILTERS_TABLE_NAME = "outgoing_filters";
    /**
     * Content type for filter provider.
     */
    public static final String FILTER_CONTENT_TYPE = BASE_DIR_TYPE + ".filter";
    /**
     * Item type for filter provider.
     */
    public static final String FILTER_CONTENT_ITEM_TYPE = BASE_ITEM_TYPE + ".filter";
    /**
     * Uri for filters provider.
     */
    public static final Uri FILTER_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + AUTHORITY + "/"
            + FILTERS_TABLE_NAME);
    /**
     * Base uri for a specific filter. Should be appended with filter id.
     */
    public static final Uri FILTER_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + AUTHORITY + "/"
            + FILTERS_TABLE_NAME + "/");

    // EXTRAS
    /**
     * Extra key to contains infos about a sip call.
     */
    public static final String EXTRA_CALL_INFO = "call_info";
    
    // Constants
    /**
     * Constant for success return
     */
    public static final int SUCCESS = 0;
    /**
     * Constant for network errors return
     */
    public static final int ERROR_CURRENT_NETWORK = 10;

    /**
     * Possible presence status.
     */
    public enum PresenceStatus {
        /**
         * Unknown status
         */
        UNKNOWN,
        /**
         * Online status
         */
        ONLINE,
        /**
         * Offline status
         */
        OFFLINE,
        /**
         * Busy status
         */
        BUSY,
        /**
         * Away status
         */
        AWAY,
    }

    /**
     * Current api version number.<br/>
     * Major version x 1000 + minor version. <br/>
     * Major version are backward compatible.
     */
    public static final int CURRENT_API = 2000;

    /**
     * Ensure capability of the remote sip service to reply our requests <br/>
     * 
     * @param service the bound service to check
     * @return true if we can safely use the API
     */
    public static boolean isApiCompatible(ISipService service) {
        if (service != null) {
            try {
                int version = service.getVersion();
                return (Math.floor(version / 1000) == Math.floor(CURRENT_API % 1000));
            } catch (RemoteException e) {
                // We consider this is a bad api version that does not have
                // versionning at all
                return false;
            }
        }

        return false;
    }
}
