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

import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.Preference;
import android.telephony.TelephonyManager;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.ExtraPlugins;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.ExtraPlugins.DynCodecInfos;
import com.csipsimple.utils.Theme;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PrefsLogic {

    private static final String NWK_TLS_KEY = "tls";
    //private static final String MEDIA_MISC_KEY = "misc";
    private static final String MEDIA_AUDIO_VOLUME_KEY = "audio_volume";
    private static final String MEDIA_AUDIO_QUALITY_KEY = "audio_quality";
    private static final String MEDIA_BAND_TYPE_KEY = "band_types";
    private static final String MEDIA_CODEC_LIST_KEY = "codecs_list";
    private static final String MEDIA_MISC_KEY = "misc";
    private static final String MEDIA_AUDIO_TROUBLESHOOT_KEY = "audio_troubleshooting";

    private static final String NWK_SECURE_TRANSPORT_KEY = "secure_transport";
    private static final String NWK_KEEP_ALIVE_KEY = "keep_alive";
    private static final String NWK_NAT_TRAVERSAL_KEY = "nat_traversal";
    private static final String NWK_TRANSPORT_KEY = "transport";
    private static final String NWK_SIP_PROTOCOL_KEY = "sip_protocol";
    private static final String NWK_PERFS_KEY = "perfs";
    
    

    public final static String EXTRA_PREFERENCE_TYPE = "preference_type";
    public final static int TYPE_MEDIA = 0;
    public final static int TYPE_MEDIA_BAND_TYPE = 1;
    public final static int TYPE_MEDIA_TROUBLESHOOT = 2;
    public final static int TYPE_NETWORK = 20;
    public final static int TYPE_NETWORK_KEEP_ALIVE = 21;
    public final static int TYPE_NETWORK_SECURE = 22;
    public final static int TYPE_NETWORK_SIP_PROTOCOL = 23;
    public final static int TYPE_CALLS = 40;
    public final static int TYPE_UI = 60;
    
    /**
     * Get the xml res for preference screen building.
     * @param t The preference screen type
     * @return the int res for xml
     */
    public static int getXmlResourceForType(int t) {
        switch(t) {
            case TYPE_MEDIA:
                return R.xml.prefs_media;
            case TYPE_MEDIA_BAND_TYPE:
                return R.xml.prefs_media_band_types;
            case TYPE_MEDIA_TROUBLESHOOT:
                return R.xml.prefs_media_troubleshoot;
            case TYPE_NETWORK:
                return R.xml.prefs_network;
            case TYPE_NETWORK_KEEP_ALIVE:
                return R.xml.prefs_network_keep_alive;
            case TYPE_NETWORK_SECURE:
                return R.xml.prefs_network_secure;
            case TYPE_NETWORK_SIP_PROTOCOL:
                return R.xml.prefs_network_sip_protocol;
            case TYPE_CALLS:
                return R.xml.prefs_calls;
            case TYPE_UI:
                return R.xml.prefs_ui;
        }
        return 0;
    }
    
    /**
     * Get the title int resource string for the type of preference.
     * @param t The preference screen type
     * @return the int res for title
     */
    public static int getTitleResourceForType(int t) {
        switch(t) {
            case TYPE_MEDIA:
                return R.string.prefs_media;
            case TYPE_MEDIA_BAND_TYPE:
                return R.string.codecs_band_types;
            case TYPE_MEDIA_TROUBLESHOOT:
                return R.string.audio_troubleshooting;
            case TYPE_NETWORK:
                return R.string.prefs_network;
            case TYPE_NETWORK_KEEP_ALIVE:
                return R.string.keep_alive_interval;
            case TYPE_NETWORK_SECURE:
                return R.string.secure_transport;
            case TYPE_NETWORK_SIP_PROTOCOL:
                return R.string.sip_protocol;
            case TYPE_CALLS:
                return R.string.prefs_calls;
            case TYPE_UI:
                return R.string.prefs_ui;
        }
        return 0;
    }
    
    
    
    public static void afterBuildPrefsForType(Context ctxt, IPreferenceHelper pfh, int t) {
        PreferencesWrapper pfw = new PreferencesWrapper(ctxt);
        
        switch (t) {
            case TYPE_MEDIA: {
                // Disable io queue because it needs one working thread that has been disabled
                pfh.hidePreference(MEDIA_AUDIO_QUALITY_KEY, SipConfigManager.HAS_IO_QUEUE);
                
                // Expert mode
                if(!pfw.isAdvancedUser()) {
                    
                    pfh.hidePreference(MEDIA_AUDIO_QUALITY_KEY, SipConfigManager.SND_MEDIA_QUALITY);
                    pfh.hidePreference(MEDIA_AUDIO_QUALITY_KEY, SipConfigManager.ECHO_CANCELLATION_TAIL);
                    pfh.hidePreference(MEDIA_AUDIO_QUALITY_KEY, SipConfigManager.ECHO_MODE);
                    pfh.hidePreference(MEDIA_AUDIO_QUALITY_KEY, SipConfigManager.SND_PTIME);
                    
                    
                    
                    pfh.hidePreference(MEDIA_AUDIO_VOLUME_KEY, SipConfigManager.SND_MIC_LEVEL);
                    pfh.hidePreference(MEDIA_AUDIO_VOLUME_KEY, SipConfigManager.SND_SPEAKER_LEVEL);
                    
                    pfh.hidePreference(MEDIA_AUDIO_VOLUME_KEY, SipConfigManager.SND_BT_MIC_LEVEL);
                    pfh.hidePreference(MEDIA_AUDIO_VOLUME_KEY, SipConfigManager.SND_BT_SPEAKER_LEVEL);
                    
                    pfh.hidePreference(MEDIA_AUDIO_VOLUME_KEY, SipConfigManager.USE_SOFT_VOLUME);
                    
                    pfh.hidePreference(MEDIA_MISC_KEY, SipConfigManager.AUTO_CONNECT_SPEAKER);
                    pfh.hidePreference(MEDIA_MISC_KEY, SipConfigManager.THREAD_COUNT);
                    
                    pfh.hidePreference(null, MEDIA_BAND_TYPE_KEY);
                    pfh.hidePreference(null, MEDIA_AUDIO_TROUBLESHOOT_KEY);
                }else {
                    // Bind only if not removed
                    pfh.setPreferenceScreenType(MEDIA_AUDIO_TROUBLESHOOT_KEY, TYPE_MEDIA_TROUBLESHOOT);
                    pfh.setPreferenceScreenType(MEDIA_BAND_TYPE_KEY, TYPE_MEDIA_BAND_TYPE);
                }
                
                // Sub activity intent for codecs
                Preference pf = pfh.findPreference(MEDIA_CODEC_LIST_KEY);
                Intent it = new Intent(ctxt, Codecs.class);
                pf.setIntent(it);
                

                break;
            }
            
            case TYPE_MEDIA_TROUBLESHOOT : {

                break;
            }
            case TYPE_NETWORK: {
                TelephonyManager telephonyManager = (TelephonyManager) ctxt.getSystemService(Context.TELEPHONY_SERVICE);
                
                if (telephonyManager.getPhoneType() == 2 /*TelephonyManager.PHONE_TYPE_CDMA*/) {
                    pfh.hidePreference("for_incoming", SipConfigManager.USE_GPRS_IN);
                    pfh.hidePreference("for_outgoing", SipConfigManager.USE_GPRS_OUT);
                    pfh.hidePreference("for_incoming", SipConfigManager.USE_EDGE_IN);
                    pfh.hidePreference("for_outgoing", SipConfigManager.USE_EDGE_OUT);
                }
                

                if(!Compatibility.isCompatible(9)) {
                    pfh.hidePreference(NWK_PERFS_KEY, SipConfigManager.LOCK_WIFI_PERFS);
                }
                
                if(!pfw.isAdvancedUser()) {
                    
                    pfh.hidePreference(NWK_NAT_TRAVERSAL_KEY, SipConfigManager.ENABLE_TURN);
                    pfh.hidePreference(NWK_NAT_TRAVERSAL_KEY, SipConfigManager.TURN_SERVER);
                    pfh.hidePreference(NWK_NAT_TRAVERSAL_KEY, SipConfigManager.TURN_USERNAME);
                    pfh.hidePreference(NWK_NAT_TRAVERSAL_KEY, SipConfigManager.TURN_PASSWORD);
                    
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.ENABLE_TCP);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.ENABLE_UDP);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.DISABLE_TCP_SWITCH);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.TCP_TRANSPORT_PORT);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.UDP_TRANSPORT_PORT);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.RTP_PORT);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.USE_IPV6);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.OVERRIDE_NAMESERVER);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.FORCE_NO_UPDATE);
                    
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.ENABLE_QOS);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.DSCP_VAL);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.USER_AGENT);
                    pfh.hidePreference(NWK_TRANSPORT_KEY, SipConfigManager.NETWORK_ROUTES_POLLING);
                    
                    pfh.hidePreference(NWK_NAT_TRAVERSAL_KEY, SipConfigManager.ENABLE_STUN2);

                    pfh.hidePreference("for_incoming", SipConfigManager.USE_ANYWAY_IN);
                    pfh.hidePreference("for_outgoing", SipConfigManager.USE_ANYWAY_OUT);
                    
                    pfh.hidePreference(null, NWK_SIP_PROTOCOL_KEY);
                    pfh.hidePreference(null, NWK_PERFS_KEY);
                }else {
                    // Bind only if not removed
                    pfh.setPreferenceScreenType(NWK_SIP_PROTOCOL_KEY, TYPE_NETWORK_SIP_PROTOCOL);
                }
                
                // Bind preference screen
                pfh.setPreferenceScreenType(NWK_KEEP_ALIVE_KEY, TYPE_NETWORK_KEEP_ALIVE);
                pfh.setPreferenceScreenType(NWK_SECURE_TRANSPORT_KEY, TYPE_NETWORK_SECURE);
                
                break;
            }
            case TYPE_NETWORK_SECURE:{

                if(!pfw.isAdvancedUser()) {
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.CA_LIST_FILE);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.TLS_VERIFY_CLIENT);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.TLS_VERIFY_SERVER);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.TLS_PASSWORD);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.TLS_METHOD);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.TLS_SERVER_NAME);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.CERT_FILE);
                    pfh.hidePreference(NWK_TLS_KEY, SipConfigManager.PRIVKEY_FILE);
                }
                
                boolean canTls = pfw.getLibCapability(PreferencesProviderWrapper.LIB_CAP_TLS);
                if(!canTls) {
                    pfh.hidePreference(null, NWK_TLS_KEY);
                    pfh.hidePreference("secure_media", SipConfigManager.USE_ZRTP);
                }
                break;
            }
            case TYPE_UI: {

                if(!pfw.isAdvancedUser()) {
                    pfh.hidePreference(null, "advanced_ui");
                    pfh.hidePreference("android_integration", SipConfigManager.GSM_INTEGRATION_TYPE);
                    pfh.hidePreference("android_integration", SipConfigManager.INTEGRATE_TEL_PRIVILEGED);
                    
                }
                
                ListPreference lp = (ListPreference) pfh.findPreference(SipConfigManager.THEME);
                HashMap<String, String> themes = Theme.getAvailableThemes(ctxt);
                
                CharSequence[] entries = new CharSequence[themes.size()];
                CharSequence[] values = new CharSequence[themes.size()];
                int i = 0;
                for( Entry<String, String> theme : themes.entrySet() ) {
                    entries[i] = theme.getKey();
                    values[i] = theme.getValue();
                    i++;
                }
                
                lp.setEntries(entries);
                lp.setEntryValues(values);
                
                break;
            }
            case TYPE_CALLS : {

                if(CustomDistribution.forceNoMultipleCalls()) {
                    pfh.hidePreference(null, SipConfigManager.SUPPORT_MULTIPLE_CALLS);
                }
                if(!CustomDistribution.supportCallRecord()) {
                    pfh.hidePreference(null, SipConfigManager.AUTO_RECORD_CALLS);
                }
                Map<String, DynCodecInfos> videoPlugins = ExtraPlugins.getDynPlugins(ctxt, SipManager.ACTION_GET_VIDEO_PLUGIN);
                if(videoPlugins.size() == 0) {
                    pfh.hidePreference(null, SipConfigManager.USE_VIDEO);
                }
            }
            default:
                break;
        }
        
    }
    

    public static void updateDescriptionForType(Context ctxt, IPreferenceHelper pfh, int t) {
        
        switch (t) {
            case TYPE_MEDIA:
                break;
            case TYPE_NETWORK:
                pfh.setStringFieldSummary(SipConfigManager.STUN_SERVER);
                break;
        }
    }
    
    
    public static boolean onMainActivityOptionsItemSelected(MenuItem item, Context ctxt, PreferencesWrapper prefsWrapper) {
        int id = item.getItemId();
        if (id == R.id.audio_test) {
            ctxt.startActivity(new Intent(ctxt, AudioTester.class));
            return true;
        } else if (id == R.id.reset_settings) {
            prefsWrapper.resetAllDefaultValues();
            return true;
        } else if (id == R.id.expert) {
            prefsWrapper.toogleExpertMode();

            return true;
        }
        return false;
    }

    public static void onMainActivityPrepareOptionMenu(Menu menu, Context ctxt, PreferencesWrapper prefsWrapper) {

        menu.findItem(R.id.expert).setTitle(prefsWrapper.isAdvancedUser()? R.string.normal_preferences: R.string.expert_preferences);
        //menu.findItem(R.id.audio_test).setVisible(prefsWrapper.isAdvancedUser());
    }
}
