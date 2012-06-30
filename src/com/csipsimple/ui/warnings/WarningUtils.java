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

package com.csipsimple.ui.warnings;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.PhoneCapabilityTester;
import com.csipsimple.utils.PreferencesProviderWrapper;

import java.util.List;

public class WarningUtils {
    
    
    public static WarningBlockView getViewForWarning(Context ctxt, String warning) {
        
        if(warning.equals(WARNING_NO_STUN)) {
            return new WarningNoStun(ctxt);
        }else if(warning.equals(WARNING_VPN_ICS)) {
            return new WarningVpnIcs(ctxt);
        }else if(warning.equals(WARNING_PRIVILEGED_INTENT)) {
            return new WarningPrivilegedIntent(ctxt);
        }
        /*
        // For now default is to display warning simply
        TextView tv = new TextView(ctxt);
        tv.setText(warning);
        return tv;
        */
        return null;
    }
    
    // Privileged intent
    public static String WARNING_PRIVILEGED_INTENT = "warn_privileged_intent";
    public static boolean shouldWarnPrivilegedIntent(Context ctxt, PreferencesProviderWrapper prefProviderWrapper) {
        if(prefProviderWrapper.getPreferenceBooleanValue(getIgnoreKey(WARNING_PRIVILEGED_INTENT), false)) {
            return false;
        }
        if(prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.INTEGRATE_WITH_DIALER, false)) {
            if(!PhoneCapabilityTester.isPhone(ctxt)) {
                return false;
            }
            List<ResolveInfo> privilegedActs = PhoneCapabilityTester.resolveActivitiesForPriviledgedCall(ctxt);
            if(privilegedActs.size() > 1) {
                // TODO : should check that default activity is actually gsm one if any
                boolean hasDefaultAct = false;
                ResolveInfo defaultAct = PhoneCapabilityTester.resolveActivityForPriviledgedCall(ctxt);
                if(defaultAct != null) {
                    for(ResolveInfo res : privilegedActs) {
                        if(res != null) {
                            if(res.activityInfo.name.equals(defaultAct.activityInfo.name)
                                    && res.activityInfo.packageName.equals(defaultAct.activityInfo.packageName)) {
                                hasDefaultAct = true;
                                break;
                            }
                        }
                    }
                }
                
                if(!hasDefaultAct) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    
    // Stun
    public static String WARNING_NO_STUN = "warn_no_stun";
    public static boolean shouldWarnNoStun(PreferencesProviderWrapper prefProviderWrapper) {
        if(prefProviderWrapper.getPreferenceBooleanValue(getIgnoreKey(WARNING_NO_STUN), false)) {
            return false;
        }
        if(!prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.ENABLE_STUN) 
                && prefProviderWrapper.getPreferenceBooleanValue(SipConfigManager.USE_3G_OUT)) {
            return true;
        }
        return false;
    }
    
    public static String WARNING_3G_TIMEOUT = "warn_3g_timeout";
    
    // Vpn for ICS
    public static String WARNING_VPN_ICS = "warn_vpn_ics";
    public static boolean shouldWarnVpnIcs(PreferencesProviderWrapper prefProviderWrapper) {
        if(prefProviderWrapper.getPreferenceBooleanValue(getIgnoreKey(WARNING_VPN_ICS), false)) {
            return false;
        }
        if(Compatibility.isCompatible(14) && prefProviderWrapper.getPreferenceIntegerValue(SipConfigManager.NETWORK_ROUTES_POLLING) == 0) {
            // services/java/com/android/server/connectivity/Vpn.java
            String[] daemons = new String[] {"racoon", "mtpd"};
            for(String daemon : daemons) {
                String state = prefProviderWrapper.getSystemProp("init.svc." + daemon);
                if("running".equals(state)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static String getIgnoreKey(String warnKey) {
        return "ignore_" + warnKey;
    }
    
    public interface OnWarningChanged {
        void onWarningRemoved(String warnKey);
    }
    
    public static abstract class WarningBlockView extends RelativeLayout implements OnClickListener {
        

        protected OnWarningChanged onWarnChangedListener = null;
        
        public WarningBlockView(Context context) {
            this(context, null);
        }
        public WarningBlockView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }
        public WarningBlockView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(getLayout(), this, true);
            bindView();
        }
        
        /**
         * Get corresponding layout to inflate
         * @return the layout id to inflate.
         */
        protected abstract int getLayout();
        
        /**
         * Get the related warning key
         * @return
         */
        protected abstract String getWarningKey();
        
        /**
         * Bind the view items
         */
        protected void bindView() {
            View ignoreView = findViewById(R.id.warn_ignore);
            if(ignoreView != null) {
                ignoreView.setOnClickListener(this);
            }
        }

        /**
         * @param onWarnChangedListener the onWarnChangedListener to set
         */
        public void setOnWarnChangedListener(OnWarningChanged onWarnChangedListener) {
            this.onWarnChangedListener = onWarnChangedListener;
        }
        

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if(id == R.id.warn_ignore) {
                SipConfigManager.setPreferenceBooleanValue(getContext(), WarningUtils.getIgnoreKey(getWarningKey()), true);
                if(onWarnChangedListener != null) {
                    onWarnChangedListener.onWarningRemoved(getWarningKey());
                }
            }
        }

    }
    
}