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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.api.SipUri.ParsedSipUriInfos;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class SipProfile implements Parcelable {
    private static final String THIS_FILE = "SipProfile";

    // Constants
    /**
     * Constant for an invalid account id.
     */
    public final static long INVALID_ID = -1;

    // Transport choices
    /**
     * Automatically choose transport.<br/>
     * By default it uses UDP, if packet is higher than UDP limit it will switch
     * to TCP.<br/>
     * Take care with that , because not all sip servers support udp/tcp
     * correclty.
     */
    public final static int TRANSPORT_AUTO = 0;
    /**
     * Force UDP transport.
     */
    public final static int TRANSPORT_UDP = 1;
    /**
     * Force TCP transport.
     */
    public final static int TRANSPORT_TCP = 2;
    /**
     * Force TLS transport.
     */
    public final static int TRANSPORT_TLS = 3;

    // Stack choices
    /**
     * Use pjsip as backend.<br/>
     * For now it's the only one supported
     */
    public static final int PJSIP_STACK = 0;
    /**
     * @deprecated Use google google android 2.3 backend.<br/>
     *             This is not supported for now.
     */
    public static final int GOOGLE_STACK = 1;

    // Password type choices
    /**
     * Plain password mode.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a8b1e563c814bdf8012f0bdf966d0ad9d"
     * >Pjsip documentation</a>
     * 
     * @see #datatype
     */
    public static final int CRED_DATA_PLAIN_PASSWD = 0;
    /**
     * Digest mode.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a8b1e563c814bdf8012f0bdf966d0ad9d"
     * >Pjsip documentation</a>
     * 
     * @see #datatype
     */
    public static final int CRED_DATA_DIGEST = 1;
    /**
     * @deprecated This mode is not supported by csipsimple for now.<br/>
     *             <a target="_blank" href=
     *             "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a8b1e563c814bdf8012f0bdf966d0ad9d"
     *             >Pjsip documentation</a>
     * @see #datatype
     */
    public static final int CRED_CRED_DATA_EXT_AKA = 2;

    // Scheme credentials choices
    /**
     * Digest scheme for credentials.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#ae31c9ec1c99fb1ffa20be5954ee995a7"
     * >Pjsip documentation</a>
     * 
     * @see #scheme
     */
    public static final String CRED_SCHEME_DIGEST = "Digest";
    /**
     * PGP scheme for credentials.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#ae31c9ec1c99fb1ffa20be5954ee995a7"
     * >Pjsip documentation</a>
     * 
     * @see #scheme
     */
    public static final String CRED_SCHEME_PGP = "PGP";

    /**
     * Separator for proxy field once stored in database.<br/>
     * It's the pipe char.
     * 
     * @see #FIELD_PROXY
     */
    public static final String PROXIES_SEPARATOR = "|";

    // Content Provider - account
    /**
     * Table name of content provider for accounts storage
     */
    public final static String ACCOUNTS_TABLE_NAME = "accounts";
    /**
     * Content type for account / sip profile
     */
    public final static String ACCOUNT_CONTENT_TYPE = SipManager.BASE_DIR_TYPE + ".account";
    /**
     * Item type for account / sip profile
     */
    public final static String ACCOUNT_CONTENT_ITEM_TYPE = SipManager.BASE_ITEM_TYPE + ".account";
    /**
     * Uri of accounts / sip profiles
     */
    public final static Uri ACCOUNT_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + ACCOUNTS_TABLE_NAME);
    /**
     * Base uri for the account / sip profile. <br/>
     * To append with {@link #FIELD_ID}
     * 
     * @see ContentUris#appendId(android.net.Uri.Builder, long)
     */
    public final static Uri ACCOUNT_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + ACCOUNTS_TABLE_NAME + "/");

    // Content Provider - account status
    /**
     * Virutal table name for sip profile adding/registration table.<br/>
     * An application should use it in read only mode.
     */
    public final static String ACCOUNTS_STATUS_TABLE_NAME = "accounts_status";
    /**
     * Content type for sip profile adding/registration state
     */
    public final static String ACCOUNT_STATUS_CONTENT_TYPE = SipManager.BASE_DIR_TYPE
            + ".account_status";
    /**
     * Content type for sip profile adding/registration state item
     */
    public final static String ACCOUNT_STATUS_CONTENT_ITEM_TYPE = SipManager.BASE_ITEM_TYPE
            + ".account_status";
    /**
     * Uri for the sip profile adding/registration state.
     */
    public final static Uri ACCOUNT_STATUS_URI = Uri.parse(ContentResolver.SCHEME_CONTENT + "://"
            + SipManager.AUTHORITY + "/" + ACCOUNTS_STATUS_TABLE_NAME);
    /**
     * Base uri for the sip profile adding/registration state. <br/>
     * To append with {@link #FIELD_ID}
     * 
     * @see ContentUris#appendId(android.net.Uri.Builder, long)
     */
    public final static Uri ACCOUNT_STATUS_ID_URI_BASE = Uri.parse(ContentResolver.SCHEME_CONTENT
            + "://"
            + SipManager.AUTHORITY + "/" + ACCOUNTS_STATUS_TABLE_NAME + "/");

    // Fields for table accounts
    /**
     * Primary key identifier of the account in the database.
     * 
     * @see Long
     */
    public static final String FIELD_ID = "id";
    /**
     * Activation state of the account.<br/>
     * If false this account will be ignored by the sip stack.
     * 
     * @see Boolean
     */
    public static final String FIELD_ACTIVE = "active";
    /**
     * The wizard associated to this account.<br/>
     * Used for icon and edit layout view.
     * 
     * @see String
     */
    public static final String FIELD_WIZARD = "wizard";
    /**
     * The display name of the account. <br/>
     * This is used in the application interface to show the label representing
     * the account.
     * 
     * @see String
     */
    public static final String FIELD_DISPLAY_NAME = "display_name";
    /**
     * The priority of the account.<br/>
     * This is used in the interface when presenting list of accounts.<br/>
     * This can also be used to choose the default account. <br/>
     * Higher means highest priority.
     * 
     * @see Integer
     */
    public static final String FIELD_PRIORITY = "priority";
    /**
     * The full SIP URL for the account. <br/>
     * The value can take name address or URL format, and will look something
     * like "sip:account@serviceprovider".<br/>
     * This field is mandatory.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#ab290b04e8150ed9627335a67e6127b7c"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_ACC_ID = "acc_id";
    /**
     * This is the URL to be put in the request URI for the registration, and
     * will look something like "sip:serviceprovider".<br/>
     * This field should be specified if registration is desired. If the value
     * is empty, no account registration will be performed.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a08473de6401e966d23f34d3a9a05bdd0"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_REG_URI = "reg_uri";
    /**
     * Subscribe to message waiting indication events (RFC 3842).<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a0158ae24d72872a31a0b33c33450a7ab"
     * >Pjsip documentation</a>
     * 
     * @see Boolean
     */
    public static final String FIELD_MWI_ENABLED = "mwi_enabled";
    /**
     * If this flag is set, the presence information of this account will be
     * PUBLISH-ed to the server where the account belongs.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a0d4128f44963deffda4ea9c15183a787"
     * >Pjsip documentation</a>
     * 1 for true, 0 for false
     * 
     * @see Integer
     */
    public static final String FIELD_PUBLISH_ENABLED = "publish_enabled";
    /**
     * Optional interval for registration, in seconds. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a2c097b9ae855783bfbb00056055dd96c"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_REG_TIMEOUT = "reg_timeout";
    /**
     * Specify the number of seconds to refresh the client registration before
     * the registration expires.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a52a35fdf8c17263b2a27d2b17111c040"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_REG_DELAY_BEFORE_REFRESH = "reg_dbr";
    /**
     * Set the interval for periodic keep-alive transmission for this account. <br/>
     * If this value is zero, keep-alive will be disabled for this account.<br/>
     * The keep-alive transmission will be sent to the registrar's address,
     * after successful registration.<br/>
     * Note that in csipsimple this value is not applied anymore in flavor to
     * {@link SipConfigManager#KEEP_ALIVE_INTERVAL_MOBILE} and
     * {@link SipConfigManager#KEEP_ALIVE_INTERVAL_WIFI} <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a98722b6464d16b5a76aec81f2d2a0694"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_KA_INTERVAL = "ka_interval";
    /**
     * Optional PIDF tuple ID for outgoing PUBLISH and NOTIFY. <br/>
     * If this value is not specified, a random string will be used. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#aa603989566022840b4671f0171b6cba1"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_PIDF_TUPLE_ID = "pidf_tuple_id";
    /**
     * Optional URI to be put as Contact for this account.<br/>
     * It is recommended that this field is left empty, so that the value will
     * be calculated automatically based on the transport address.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a5dfdfba40038e33af95819fbe2b896f9"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_FORCE_CONTACT = "force_contact";

    /**
     * This option is used to update the transport address and the Contact
     * header of REGISTER request.<br/>
     * When this option is enabled, the library will keep track of the public IP
     * address from the response of REGISTER request. <br/>
     * Once it detects that the address has changed, it will unregister current
     * Contact, update the Contact with transport address learned from Via
     * header, and register a new Contact to the registrar.<br/>
     * This will also update the public name of UDP transport if STUN is
     * configured.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a22961bb72ea75f7ca7008464f081ca06"
     * >Pjsip documentation</a>
     * 
     * @see Boolean
     */
    public static final String FIELD_ALLOW_CONTACT_REWRITE = "allow_contact_rewrite";
    /**
     * Specify how Contact update will be done with the registration, if
     * allow_contact_rewrite is enabled.<br/>
     * <ul>
     * <li>If set to 1, the Contact update will be done by sending
     * unregistration to the currently registered Contact, while simultaneously
     * sending new registration (with different Call-ID) for the updated
     * Contact.</li>
     * <li>If set to 2, the Contact update will be done in a single, current
     * registration session, by removing the current binding (by setting its
     * Contact's expires parameter to zero) and adding a new Contact binding,
     * all done in a single request.</li>
     * </ul>
     * Value 1 is the legacy behavior.<br/>
     * Value 2 is the default behavior.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a73b69a3a8d225147ce386e310e588285"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_CONTACT_REWRITE_METHOD = "contact_rewrite_method";

    /**
     * Additional parameters that will be appended in the Contact header for
     * this account.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#abef88254f9ef2a490503df6d3b297e54"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_CONTACT_PARAMS = "contact_params";
    /**
     * Additional URI parameters that will be appended in the Contact URI for
     * this account.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#aced70341308928ae951525093bf47562"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_CONTACT_URI_PARAMS = "contact_uri_params";
    /**
     * Transport to use for this account.<br/>
     * 
     * @see #TRANSPORT_AUTO
     * @see #TRANSPORT_UDP
     * @see #TRANSPORT_TCP
     * @see #TRANSPORT_TLS
     */
    public static final String FIELD_TRANSPORT = "transport";
    /**
     * Way the application should use SRTP. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a34b00edb1851924a99efd8fedab917ba"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_USE_SRTP = "use_srtp";
    /**
     * Way the application should use SRTP. <br/>
     * -1 means use default global value of {@link SipConfigManager#USE_ZRTP} <br/>
     * 0 means disabled for this account <br/>
     * 1 means enabled for this account
     *  
     * @see Integer
     */
    public static final String FIELD_USE_ZRTP = "use_zrtp";

    /**
     * Optional URI of the proxies to be visited for all outgoing requests that
     * are using this account (REGISTER, INVITE, etc).<br/>
     * If multiple separate it by {@link #PROXIES_SEPARATOR}. <br/>
     * Warning, for now api doesn't allow multiple credentials so if you have
     * one credential per proxy may not work.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a93ad0699020c17ddad5eb98dea69f699"
     * >Pjsip documentation</a>
     * 
     * @see String
     * @see #PROXIES_SEPARATOR
     */
    public static final String FIELD_PROXY = "proxy";
    /**
     * Specify how the registration uses the outbound and account proxy
     * settings. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#ad932bbb3c2c256f801c775319e645717"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_REG_USE_PROXY = "reg_use_proxy";

    // For now, assume unique credential
    /**
     * Realm to filter on for credentials.<br/>
     * Put star "*" char if you want it to match all requests.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a96eee6bdc2b0e7e3b7eea9b4e1c15674"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_REALM = "realm";
    /**
     * Scheme (e.g. "digest").<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#ae31c9ec1c99fb1ffa20be5954ee995a7"
     * >Pjsip documentation</a>
     * 
     * @see String
     * @see #CRED_SCHEME_DIGEST
     * @see #CRED_SCHEME_PGP
     */
    public static final String FIELD_SCHEME = "scheme";
    /**
     * Credential username.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a3e1f72a171886985c6dfcd57d4bc4f17"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_USERNAME = "username";
    /**
     * Type of the data for credentials.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#a8b1e563c814bdf8012f0bdf966d0ad9d"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     * @see #CRED_DATA_PLAIN_PASSWD
     * @see #CRED_DATA_DIGEST
     * @see #CRED_CRED_DATA_EXT_AKA
     */
    public static final String FIELD_DATATYPE = "datatype";
    /**
     * The data, which can be a plaintext password or a hashed digest.<br/>
     * This is available on in read only for third party application for obvious
     * security reason.<br/>
     * If you update the content provider without passing this parameter it will
     * not override it. <br/>
     * If in a third party app you want to store the password to allow user to
     * see it, you have to manage this by your own. <br/>
     * However, it's highly recommanded to not store it by your own, and keep it
     * stored only in csipsimple.<br/>
     * It available for write/overwrite. <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsip__cred__info.htm#ab3947a7800c51d28a1b25f4fdaea78bd"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_DATA = "data";

    // Android stuff
    /**
     * The backend sip stack to use for this account.<br/>
     * For now only pjsip backend is supported.
     * 
     * @see Integer
     * @see #PJSIP_STACK
     * @see #GOOGLE_STACK
     */
    public static final String FIELD_SIP_STACK = "sip_stack";
    /**
     * Sip contact to call if user want to consult his voice mail.<br/>
     * 
     * @see String
     */
    public static final String FIELD_VOICE_MAIL_NBR = "vm_nbr";
    /**
     * Associated contact group for buddy list of this account.<br/>
     * Users of this group will be considered as part of the buddy list of this
     * account and will automatically try to subscribe presence if activated.<br/>
     * Warning : not implemented for now.
     * 
     * @see String
     */
    public static final String FIELD_ANDROID_GROUP = "android_group";

    // Sip outbound
    /**
     * Control the use of SIP outbound feature. <br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a306e4641988606f1ef0993e398ff98e7"
     * >Pjsip documentation</a>
     * 
     * @see Integer
     */
    public static final String FIELD_USE_RFC5626 = "use_rfc5626";
    /**
     * Specify SIP outbound (RFC 5626) instance ID to be used by this
     * application.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#ae025bf4538d1f9f9506b45015a46a8f6"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_RFC5626_INSTANCE_ID = "rfc5626_instance_id";
    /**
     * Specify SIP outbound (RFC 5626) registration ID.<br/>
     * <a target="_blank" href=
     * "http://www.pjsip.org/pjsip/docs/html/structpjsua__acc__config.htm#a71376e1f32e35401fc6c2c3bcb2087d8"
     * >Pjsip documentation</a>
     * 
     * @see String
     */
    public static final String FIELD_RFC5626_REG_ID = "rfc5626_reg_id";

    // Video config
    /**
     * Auto show video of the remote party.<br/>
     * TODO : complete when pjsip-2.x stable documentation out
     */
    public static final String FIELD_VID_IN_AUTO_SHOW = "vid_in_auto_show";
    /**
     * Auto transmit video of our party.<br/>
     * TODO : complete when pjsip-2.x stable documentation out
     */
    public static final String FIELD_VID_OUT_AUTO_TRANSMIT = "vid_out_auto_transmit";

    // RTP config
    /**
     * Begin RTP port for the media of this account.<br/>
     * By default it will use {@link SipConfigManager#RTP_PORT}
     * 
     * @see Integer
     */
    public static final String FIELD_RTP_PORT = "rtp_port";
    /**
     * Public address to announce in SDP as self media address.<br/>
     * Only use if you have static and known public ip on your device regarding
     * the sip server. <br/>
     * May be helpful in VPN configurations.
     */
    public static final String FIELD_RTP_PUBLIC_ADDR = "rtp_public_addr";
    /**
     * Address to bound from client to enforce on interface to be used. <br/>
     * By default the application bind all addresses. (0.0.0.0).<br/>
     * This is only useful if you want to avoid one interface to be bound, but
     * is useless to get audio path correctly working use
     * {@link #FIELD_RTP_PUBLIC_ADDR}
     */
    public static final String FIELD_RTP_BOUND_ADDR = "rtp_bound_addr";
    /**
     * Should the QoS be enabled on this account.<br/>
     * By default it will use {@link SipConfigManager#ENABLE_QOS}.<br/>
     * Default value is -1 to use global setting. 0 means disabled, 1 means
     * enabled.<br/>
     * 
     * @see Integer
     * @see SipConfigManager#ENABLE_QOS
     */
    public static final String FIELD_RTP_ENABLE_QOS = "rtp_enable_qos";
    /**
     * The value of DSCP.<br/>
     * 
     * @see Integer
     * @see SipConfigManager#DSCP_VAL
     */
    public static final String FIELD_RTP_QOS_DSCP = "rtp_qos_dscp";

    /**
     * Should the application try to clean registration of all sip clients if no
     * registration found.<br/>
     * This is useful if the sip server manage limited serveral concurrent
     * registrations.<br/>
     * Since in this case the registrations may leak in case of failing
     * unregisters, this option will unregister all contacts previously
     * registred.
     * 
     * @see Boolean
     */
    public static final String FIELD_TRY_CLEAN_REGISTERS = "try_clean_reg";

    /**
     * Simple project to use if you want to list accounts with basic infos on it
     * only.
     * 
     * @see #FIELD_ACC_ID
     * @see #FIELD_ACTIVE
     * @see #FIELD_WIZARD
     * @see #FIELD_DISPLAY_NAME
     * @see #FIELD_WIZARD
     * @see #FIELD_PRIORITY
     * @see #FIELD_REG_URI
     */
    public static final String[] LISTABLE_PROJECTION = new String[] {
            SipProfile.FIELD_ID,
            SipProfile.FIELD_ACC_ID,
            SipProfile.FIELD_ACTIVE,
            SipProfile.FIELD_DISPLAY_NAME,
            SipProfile.FIELD_WIZARD,
            SipProfile.FIELD_PRIORITY,
            SipProfile.FIELD_REG_URI
    };

    // Properties
    /**
     * Primary key for serialization of the object.
     */
    public int primaryKey = -1;
    /**
     * @see #FIELD_ID
     */
    public long id = INVALID_ID;
    /**
     * @see #FIELD_DISPLAY_NAME
     */
    public String display_name = "";
    /**
     * @see #FIELD_WIZARD
     */
    public String wizard = "EXPERT";
    /**
     * @see #FIELD_TRANSPORT
     */
    public Integer transport = 0;
    /**
     * @see #FIELD_ACTIVE
     */
    public boolean active = true;
    /**
     * @see #FIELD_PRIORITY
     */
    public int priority = 100;
    /**
     * @see #FIELD_ACC_ID
     */
    public String acc_id = null;

    /**
     * @see #FIELD_REG_URI
     */
    public String reg_uri = null;
    /**
     * @see #FIELD_PUBLISH_ENABLED
     */
    public int publish_enabled = 0;
    /**
     * @see #FIELD_REG_TIMEOUT
     */
    public int reg_timeout = 900;
    /**
     * @see #FIELD_KA_INTERVAL
     */
    public int ka_interval = 0;
    /**
     * @see #FIELD_PIDF_TUPLE_ID
     */
    public String pidf_tuple_id = null;
    /**
     * @see #FIELD_FORCE_CONTACT
     */
    public String force_contact = null;
    /**
     * @see #FIELD_ALLOW_CONTACT_REWRITE
     */
    public boolean allow_contact_rewrite = true;
    /**
     * @see #FIELD_CONTACT_REWRITE_METHOD
     */
    public int contact_rewrite_method = 2;
    /**
     * Exploded array of proxies
     * 
     * @see #FIELD_PROXY
     */
    public String[] proxies = null;
    /**
     * @see #FIELD_REALM
     */
    public String realm = null;
    /**
     * @see #FIELD_USERNAME
     */
    public String username = null;
    /**
     * @see #FIELD_SCHEME
     */
    public String scheme = null;
    /**
     * @see #FIELD_DATATYPE
     */
    public int datatype = 0;
    /**
     * @see #FIELD_DATA
     */
    public String data = null;
    /**
     * @see #FIELD_USE_SRTP
     */
    public int use_srtp = -1;
    /**
     * @see #FIELD_USE_ZRTP
     */
    public int use_zrtp = -1;
    /**
     * @see #FIELD_REG_USE_PROXY
     */
    public int reg_use_proxy = 3;
    /**
     * @see #FIELD_SIP_STACK
     */
    public int sip_stack = PJSIP_STACK;
    /**
     * @see #FIELD_VOICE_MAIL_NBR
     */
    public String vm_nbr = null;
    /**
     * @see #FIELD_REG_DELAY_BEFORE_REFRESH
     */
    public int reg_delay_before_refresh = -1;
    /**
     * @see #FIELD_TRY_CLEAN_REGISTERS
     */
    public int try_clean_registers = 0;
    /**
     * Chache holder icon for the account wizard representation.<br/>
     * This will not not be filled by default. You have to get it from wizard
     * infos.
     */
    public Bitmap icon = null;
    /**
     * @see #FIELD_USE_RFC5626
     */
    public boolean use_rfc5626 = true;
    /**
     * @see #FIELD_RFC5626_INSTANCE_ID
     */
    public String rfc5626_instance_id = "";
    /**
     * @see #FIELD_RFC5626_REG_ID
     */
    public String rfc5626_reg_id = "";
    /**
     * @see #FIELD_VID_IN_AUTO_SHOW
     */
    public int vid_in_auto_show = -1;
    /**
     * @see #FIELD_VID_OUT_AUTO_TRANSMIT
     */
    public int vid_out_auto_transmit = -1;
    /**
     * @see #FIELD_RTP_PORT
     */
    public int rtp_port = -1;
    /**
     * @see #FIELD_RTP_PUBLIC_ADDR
     */
    public String rtp_public_addr = "";
    /**
     * @see #FIELD_RTP_BOUND_ADDR
     */
    public String rtp_bound_addr = "";
    /**
     * @see #FIELD_RTP_ENABLE_QOS
     */
    public int rtp_enable_qos = -1;
    /**
     * @see #FIELD_RTP_QOS_DSCP
     */
    public int rtp_qos_dscp = -1;
    /**
     * @see #FIELD_ANDROID_GROUP
     */
    public String android_group = "";

    public SipProfile() {
        display_name = "";
        wizard = "EXPERT";
        active = true;
    }

    /**
     * Construct a sip profile wrapper from a cursor retrieved with a
     * {@link ContentProvider} query on {@link #ACCOUNTS_TABLE_NAME}.
     * 
     * @param c the cursor to unpack
     */
    public SipProfile(Cursor c) {
        super();
        createFromDb(c);
    }

    /**
     * Construct from parcelable <br/>
     * Only used by {@link #CREATOR}
     * 
     * @param in parcelable to build from
     */
    private SipProfile(Parcel in) {
        primaryKey = in.readInt();
        id = in.readInt();
        display_name = in.readString();
        wizard = in.readString();
        transport = in.readInt();
        active = (in.readInt() != 0) ? true : false;
        priority = in.readInt();
        acc_id = getReadParcelableString(in.readString());
        reg_uri = getReadParcelableString(in.readString());
        publish_enabled = in.readInt();
        reg_timeout = in.readInt();
        ka_interval = in.readInt();
        pidf_tuple_id = getReadParcelableString(in.readString());
        force_contact = getReadParcelableString(in.readString());
        proxies = TextUtils.split(getReadParcelableString(in.readString()),
                Pattern.quote(PROXIES_SEPARATOR));
        realm = getReadParcelableString(in.readString());
        username = getReadParcelableString(in.readString());
        datatype = in.readInt();
        data = getReadParcelableString(in.readString());
        use_srtp = in.readInt();
        allow_contact_rewrite = (in.readInt() != 0);
        contact_rewrite_method = in.readInt();
        sip_stack = in.readInt();
        reg_use_proxy = in.readInt();
        use_zrtp = in.readInt();
        vm_nbr = getReadParcelableString(in.readString());
        reg_delay_before_refresh = in.readInt();
        icon = (Bitmap) in.readParcelable(Bitmap.class.getClassLoader());
        try_clean_registers = in.readInt();
        use_rfc5626 = (in.readInt() != 0);
        rfc5626_instance_id = getReadParcelableString(in.readString());
        rfc5626_reg_id = getReadParcelableString(in.readString());
        vid_in_auto_show = in.readInt();
        vid_out_auto_transmit = in.readInt();
        rtp_port = in.readInt();
        rtp_public_addr = getReadParcelableString(in.readString());
        rtp_bound_addr = getReadParcelableString(in.readString());
        rtp_enable_qos = in.readInt();
        rtp_qos_dscp = in.readInt();
        android_group = getReadParcelableString(in.readString());
    }

    /**
     * Parcelable creator. So that it can be passed as an argument of the aidl
     * interface
     */
    public static final Parcelable.Creator<SipProfile> CREATOR = new Parcelable.Creator<SipProfile>() {
        public SipProfile createFromParcel(Parcel in) {
            return new SipProfile(in);
        }

        public SipProfile[] newArray(int size) {
            return new SipProfile[size];
        }
    };

    /**
     * @see Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @see Parcelable#writeToParcel(Parcel, int)
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(primaryKey);
        dest.writeInt((int) id);
        dest.writeString(display_name);
        dest.writeString(wizard);
        dest.writeInt(transport);
        dest.writeInt(active ? 1 : 0);
        dest.writeInt(priority);
        dest.writeString(getWriteParcelableString(acc_id));
        dest.writeString(getWriteParcelableString(reg_uri));
        dest.writeInt(publish_enabled);
        dest.writeInt(reg_timeout);
        dest.writeInt(ka_interval);
        dest.writeString(getWriteParcelableString(pidf_tuple_id));
        dest.writeString(getWriteParcelableString(force_contact));
        if (proxies != null) {
            dest.writeString(getWriteParcelableString(TextUtils.join(PROXIES_SEPARATOR, proxies)));
        } else {
            dest.writeString("");
        }
        dest.writeString(getWriteParcelableString(realm));
        dest.writeString(getWriteParcelableString(username));
        dest.writeInt(datatype);
        dest.writeString(getWriteParcelableString(data));
        dest.writeInt(use_srtp);
        dest.writeInt(allow_contact_rewrite ? 1 : 0);
        dest.writeInt(contact_rewrite_method);
        dest.writeInt(sip_stack);
        dest.writeInt(reg_use_proxy);
        dest.writeInt(use_zrtp);
        dest.writeString(getWriteParcelableString(vm_nbr));
        dest.writeInt(reg_delay_before_refresh);
        dest.writeParcelable((Parcelable) icon, flags);
        dest.writeInt(try_clean_registers);
        dest.writeInt(use_rfc5626 ? 1 : 0);
        dest.writeString(getWriteParcelableString(rfc5626_instance_id));
        dest.writeString(getWriteParcelableString(rfc5626_reg_id));
        dest.writeInt(vid_in_auto_show);
        dest.writeInt(vid_out_auto_transmit);
        dest.writeInt(rtp_port);
        dest.writeString(getWriteParcelableString(rtp_public_addr));
        dest.writeString(getWriteParcelableString(rtp_bound_addr));
        dest.writeInt(rtp_enable_qos);
        dest.writeInt(rtp_qos_dscp);
        dest.writeString(getWriteParcelableString(android_group));
    }

    // Yes yes that's not clean but well as for now not problem with that.
    // and we send null.
    private String getWriteParcelableString(String str) {
        return (str == null) ? "null" : str;
    }

    private String getReadParcelableString(String str) {
        return str.equalsIgnoreCase("null") ? null : str;
    }

    /**
     * Create account wrapper with cursor datas.
     * 
     * @param c cursor on the database
     */
    private final void createFromDb(Cursor c) {
        ContentValues args = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, args);
        createFromContentValue(args);
    }

    /**
     * Create account wrapper with content values pairs.
     * 
     * @param args the content value to unpack.
     */
    private final void createFromContentValue(ContentValues args) {
        Integer tmp_i;
        String tmp_s;

        // Application specific settings
        tmp_i = args.getAsInteger(FIELD_ID);
        if (tmp_i != null) {
            id = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_DISPLAY_NAME);
        if (tmp_s != null) {
            display_name = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_WIZARD);
        if (tmp_s != null) {
            wizard = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_TRANSPORT);
        if (tmp_i != null) {
            transport = tmp_i;
        }

        tmp_i = args.getAsInteger(FIELD_ACTIVE);
        if (tmp_i != null) {
            active = (tmp_i != 0);
        } else {
            active = true;
        }
        tmp_s = args.getAsString(FIELD_ANDROID_GROUP);
        if (tmp_s != null) {
            android_group = tmp_s;
        }

        // General account settings
        tmp_i = args.getAsInteger(FIELD_PRIORITY);
        if (tmp_i != null) {
            priority = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_ACC_ID);
        if (tmp_s != null) {
            acc_id = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_REG_URI);
        if (tmp_s != null) {
            reg_uri = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_PUBLISH_ENABLED);
        if (tmp_i != null) {
            publish_enabled = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_REG_TIMEOUT);
        if (tmp_i != null && tmp_i >= 0) {
            reg_timeout = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_REG_DELAY_BEFORE_REFRESH);
        if (tmp_i != null && tmp_i >= 0) {
            reg_delay_before_refresh = tmp_i;
        }

        tmp_i = args.getAsInteger(FIELD_KA_INTERVAL);
        if (tmp_i != null && tmp_i >= 0) {
            ka_interval = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_PIDF_TUPLE_ID);
        if (tmp_s != null) {
            pidf_tuple_id = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_FORCE_CONTACT);
        if (tmp_s != null) {
            force_contact = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_ALLOW_CONTACT_REWRITE);
        if (tmp_i != null) {
            allow_contact_rewrite = (tmp_i == 1);
        }
        tmp_i = args.getAsInteger(FIELD_CONTACT_REWRITE_METHOD);
        if (tmp_i != null) {
            contact_rewrite_method = tmp_i;
        }

        tmp_i = args.getAsInteger(FIELD_USE_SRTP);
        if (tmp_i != null && tmp_i >= 0) {
            use_srtp = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_USE_ZRTP);
        if (tmp_i != null && tmp_i >= 0) {
            use_zrtp = tmp_i;
        }

        // Proxy
        tmp_s = args.getAsString(FIELD_PROXY);
        if (tmp_s != null) {
            proxies = TextUtils.split(tmp_s, Pattern.quote(PROXIES_SEPARATOR));
        }
        tmp_i = args.getAsInteger(FIELD_REG_USE_PROXY);
        if (tmp_i != null && tmp_i >= 0) {
            reg_use_proxy = tmp_i;
        }

        // Auth
        tmp_s = args.getAsString(FIELD_REALM);
        if (tmp_s != null) {
            realm = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_SCHEME);
        if (tmp_s != null) {
            scheme = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_USERNAME);
        if (tmp_s != null) {
            username = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_DATATYPE);
        if (tmp_i != null) {
            datatype = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_DATA);
        if (tmp_s != null) {
            data = tmp_s;
        }

        tmp_i = args.getAsInteger(FIELD_SIP_STACK);
        if (tmp_i != null && tmp_i >= 0) {
            sip_stack = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_VOICE_MAIL_NBR);
        if (tmp_s != null) {
            vm_nbr = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_TRY_CLEAN_REGISTERS);
        if (tmp_i != null && tmp_i >= 0) {
            try_clean_registers = tmp_i;
        }

        // RFC 5626
        tmp_i = args.getAsInteger(FIELD_USE_RFC5626);
        if (tmp_i != null && tmp_i >= 0) {
            use_rfc5626 = (tmp_i != 0);
        }
        tmp_s = args.getAsString(FIELD_RFC5626_INSTANCE_ID);
        if (tmp_s != null) {
            rfc5626_instance_id = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_RFC5626_REG_ID);
        if (tmp_s != null) {
            rfc5626_reg_id = tmp_s;
        }

        // Video
        tmp_i = args.getAsInteger(FIELD_VID_IN_AUTO_SHOW);
        if (tmp_i != null && tmp_i >= 0) {
            vid_in_auto_show = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_VID_OUT_AUTO_TRANSMIT);
        if (tmp_i != null && tmp_i >= 0) {
            vid_out_auto_transmit = tmp_i;
        }

        // RTP cfg
        tmp_i = args.getAsInteger(FIELD_RTP_PORT);
        if (tmp_i != null && tmp_i >= 0) {
            rtp_port = tmp_i;
        }
        tmp_s = args.getAsString(FIELD_RTP_PUBLIC_ADDR);
        if (tmp_s != null) {
            rtp_public_addr = tmp_s;
        }
        tmp_s = args.getAsString(FIELD_RTP_BOUND_ADDR);
        if (tmp_s != null) {
            rtp_bound_addr = tmp_s;
        }
        tmp_i = args.getAsInteger(FIELD_RTP_ENABLE_QOS);
        if (tmp_i != null && tmp_i >= 0) {
            rtp_enable_qos = tmp_i;
        }
        tmp_i = args.getAsInteger(FIELD_RTP_QOS_DSCP);
        if (tmp_i != null && tmp_i >= 0) {
            rtp_qos_dscp = tmp_i;
        }

    }

    /**
     * Transform pjsua_acc_config into ContentValues that can be insert into
     * database. <br/>
     * Take care that if your SipProfile is incomplete this content value may
     * also be uncomplete and lead to override unwanted values of the existing
     * database. <br/>
     * So if updating, take care on what you actually want to update instead of
     * using this utility method.
     * 
     * @return Complete content values from the current wrapper around sip
     *         profile.
     */
    public ContentValues getDbContentValues() {
        ContentValues args = new ContentValues();

        if (id != INVALID_ID) {
            args.put(FIELD_ID, id);
        }
        // TODO : ensure of non nullity of some params

        args.put(FIELD_ACTIVE, active ? 1 : 0);
        args.put(FIELD_WIZARD, wizard);
        args.put(FIELD_DISPLAY_NAME, display_name);
        args.put(FIELD_TRANSPORT, transport);

        args.put(FIELD_PRIORITY, priority);
        args.put(FIELD_ACC_ID, acc_id);
        args.put(FIELD_REG_URI, reg_uri);

        // MWI not yet in JNI

        args.put(FIELD_PUBLISH_ENABLED, publish_enabled);
        args.put(FIELD_REG_TIMEOUT, reg_timeout);
        args.put(FIELD_KA_INTERVAL, ka_interval);
        args.put(FIELD_PIDF_TUPLE_ID, pidf_tuple_id);
        args.put(FIELD_FORCE_CONTACT, force_contact);
        args.put(FIELD_ALLOW_CONTACT_REWRITE, allow_contact_rewrite ? 1 : 0);
        args.put(FIELD_CONTACT_REWRITE_METHOD, contact_rewrite_method);
        args.put(FIELD_USE_SRTP, use_srtp);
        args.put(FIELD_USE_ZRTP, use_zrtp);

        // CONTACT_PARAM and CONTACT_PARAM_URI not yet in JNI

        if (proxies != null) {
            args.put(FIELD_PROXY, TextUtils.join(PROXIES_SEPARATOR, proxies));
        } else {
            args.put(FIELD_PROXY, "");
        }
        args.put(FIELD_REG_USE_PROXY, reg_use_proxy);

        // Assume we have an unique credential
        args.put(FIELD_REALM, realm);
        args.put(FIELD_SCHEME, scheme);
        args.put(FIELD_USERNAME, username);
        args.put(FIELD_DATATYPE, datatype);
        if (!TextUtils.isEmpty(data)) {
            args.put(FIELD_DATA, data);
        }

        args.put(FIELD_SIP_STACK, sip_stack);
        args.put(FIELD_VOICE_MAIL_NBR, vm_nbr);
        args.put(FIELD_REG_DELAY_BEFORE_REFRESH, reg_delay_before_refresh);
        args.put(FIELD_TRY_CLEAN_REGISTERS, try_clean_registers);
        
        
        args.put(FIELD_RTP_BOUND_ADDR, rtp_bound_addr);
        args.put(FIELD_RTP_ENABLE_QOS, rtp_enable_qos);
        args.put(FIELD_RTP_PORT, rtp_port);
        args.put(FIELD_RTP_PUBLIC_ADDR, rtp_public_addr);
        args.put(FIELD_RTP_QOS_DSCP, rtp_qos_dscp);
        
        args.put(FIELD_VID_IN_AUTO_SHOW, vid_in_auto_show);
        args.put(FIELD_VID_OUT_AUTO_TRANSMIT, vid_out_auto_transmit);
        
        args.put(FIELD_RFC5626_INSTANCE_ID, rfc5626_instance_id);
        args.put(FIELD_RFC5626_REG_ID, rfc5626_reg_id);
        args.put(FIELD_USE_RFC5626, use_rfc5626 ? 1 : 0);

        args.put(FIELD_ANDROID_GROUP, android_group);

        return args;
    }

    /**
     * Get the default domain for this account
     * 
     * @return the default domain for this account
     */
    public String getDefaultDomain() {
        ParsedSipUriInfos parsedInfo = null;
        if (!TextUtils.isEmpty(reg_uri)) {
            parsedInfo = SipUri.parseSipUri(reg_uri);
        } else if (proxies != null && proxies.length > 0) {
            parsedInfo = SipUri.parseSipUri(proxies[0]);
        }

        if (parsedInfo == null) {
            return null;
        }

        if (parsedInfo.domain != null) {
            String dom = parsedInfo.domain;
            if (parsedInfo.port != 5060) {
                dom += ":" + Integer.toString(parsedInfo.port);
            }
            return dom;
        } else {
            Log.d(THIS_FILE, "Domain not found for this account");
        }
        return null;
    }

    // Android API

    /**
     * Gets the flag of 'Auto Registration'
     * 
     * @return true if auto register this account
     */
    public boolean getAutoRegistration() {
        return true;
    }

    /**
     * Gets the display name of the user.
     * 
     * @return the caller id for this account
     */
    public String getDisplayName() {
        if (acc_id != null) {
            ParsedSipContactInfos parsed = SipUri.parseSipContact(acc_id);
            if (parsed.displayName != null) {
                return parsed.displayName;
            }
        }
        return "";
    }

    /**
     * Gets the password.
     * 
     * @return the password of the sip profile Using this from an external
     *         application will always be empty
     */
    public String getPassword() {
        return data;
    }

    /**
     * Gets the (user-defined) name of the profile.
     * 
     * @return the display name for this profile
     */
    public String getProfileName() {
        return display_name;
    }

    /**
     * Gets the network address of the server outbound proxy.
     * 
     * @return the first proxy server if any else empty string
     */
    public String getProxyAddress() {
        if (proxies != null && proxies.length > 0) {
            return proxies[0];
        }
        return "";
    }

    /**
     * Gets the SIP domain when acc_id is username@domain.
     * 
     * @return the sip domain for this account
     */
    public String getSipDomain() {
        ParsedSipContactInfos parsed = SipUri.parseSipContact(acc_id);
        if (parsed.domain != null) {
            return parsed.domain;
        }
        return "";
    }

    /**
     * Gets the SIP URI string of this profile.
     */
    public String getUriString() {
        return acc_id;
    }

    /**
     * Gets the username when acc_id is username@domain. WARNING : this is
     * different from username of SipProfile which is the authentication name
     * cause of pjsip naming
     * 
     * @return the username of the account sip id. <br/>
     *         Example if acc_id is "Display Name" <sip:user@domain.com>, it
     *         will return user.
     */
    public String getSipUserName() {
        ParsedSipContactInfos parsed = SipUri.parseSipContact(acc_id);
        if (parsed.userName != null) {
            return parsed.userName;
        }
        return "";
    }

    // Helpers static factory
    /**
     * Helper method to retrieve a SipProfile object from its account database
     * id.<br/>
     * You have to specify the projection you want to use for to retrieve infos.<br/>
     * As consequence the wrapper SipProfile object you'll get may be
     * incomplete. So take care if you try to reinject it by updating to not
     * override existing values of the database that you don't get here.
     * 
     * @param ctxt Your application context. Mainly useful to get the content provider for the request.
     * @param accountId The sip profile {@link #FIELD_ID} you want to retrieve.
     * @param projection The list of fields you want to retrieve. Must be in FIELD_* of this class.<br/>
     * Reducing your requested fields to minimum will improve speed of the request.
     * @return A wrapper SipProfile object on the request you done. If not found an invalid account with an {@link #id} equals to {@link #INVALID_ID}
     */
    public static SipProfile getProfileFromDbId(Context ctxt, long accountId, String[] projection) {
        SipProfile account = new SipProfile();
        if (accountId != INVALID_ID) {
            Cursor c = ctxt.getContentResolver().query(
                    ContentUris.withAppendedId(ACCOUNT_ID_URI_BASE, accountId),
                    projection, null, null, null);

            if (c != null) {
                try {
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        account = new SipProfile(c);
                    }
                } catch (Exception e) {
                    Log.e(THIS_FILE, "Something went wrong while retrieving the account", e);
                } finally {
                    c.close();
                }
            }
        }
        return account;
    }

    /**
     * Get the list of sip profiles available.
     * @param ctxt Your application context. Mainly useful to get the content provider for the request.
     * @param onlyActive Pass it to true if you are only interested in active accounts.
     * @return The list of SipProfiles containings only fields of {@link #LISTABLE_PROJECTION} filled.
     * @see #LISTABLE_PROJECTION
     */
    public static ArrayList<SipProfile> getAllProfiles(Context ctxt, boolean onlyActive) {
        return getAllProfiles(ctxt, onlyActive, LISTABLE_PROJECTION);
    }
    
    /**
     * Get the list of sip profiles available.
     * @param ctxt Your application context. Mainly useful to get the content provider for the request.
     * @param onlyActive Pass it to true if you are only interested in active accounts.
     * @param projection The projection to use for cursor
     * @return The list of SipProfiles
     */
    public static ArrayList<SipProfile> getAllProfiles(Context ctxt, boolean onlyActive, String[] projection) {
        ArrayList<SipProfile> result = new ArrayList<SipProfile>();

        String selection = null;
        String[] selectionArgs = null;
        if (onlyActive) {
            selection = SipProfile.FIELD_ACTIVE + "=?";
            selectionArgs = new String[] {
                    "1"
            };
        }
        Cursor c = ctxt.getContentResolver().query(ACCOUNT_URI, projection, selection, selectionArgs, null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        result.add(new SipProfile(c));
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }

        return result;
    }
}
