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

package com.csipsimple.ui.outgoingcall;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.UriUtils;

public class OutgoingCallChooser extends SherlockFragmentActivity {


    private static final String THIS_FILE = "OutgoingCallChooser";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resetInternals();
        
        // Sanity check
        if (TextUtils.isEmpty(getPhoneNumber())) {
            Log.e(THIS_FILE, "No number detected for : " + getIntent().getAction());
            finish();
            return;
        }
        setContentView(R.layout.outgoing_call_view);
        connectService();
    }
    
    private String phoneNumber = null;
    private boolean ignoreRewritingRules = false;
    private Long accountToCallTo = null;
    
    
    private final static String SCHEME_CSIP = SipManager.PROTOCOL_CSIP;
    
    /**
     * Get the phone number that raised this activity.
     * @return The phone number we are trying to call with this activity
     */
    public String getPhoneNumber() {
        if(phoneNumber == null) {
            Intent it = getIntent();
            // Use utility function to extract number
            phoneNumber = UriUtils.extractNumberFromIntent(it, this);
            
            // Additional check to know if a csip uri (so that no rewriting rules applies)
            if (phoneNumber != null) {
                String action = it.getAction();
                Uri data = it.getData();
                if(!Intent.ACTION_SENDTO.equalsIgnoreCase(action) && data != null) {
                    String scheme = data.getScheme();
                    if(scheme != null) {
                        scheme = scheme.toLowerCase();
                    }
                    if(SCHEME_CSIP.equals(scheme)) {
                        ignoreRewritingRules = true;
                    }
                }
            }
            // Still null ... well make it empty.
            if(phoneNumber == null) {
                phoneNumber = "";
            }
            return phoneNumber;
        }
        
        return phoneNumber;
    }
    
    /**
     * Should we ignore rewriting rules
     * @return True if rewriting rules are not taken into account for this outgoing call.
     */
    public boolean shouldIgnoreRewritingRules() {
        // Ignore rewriting rule is get once phone number is retrieved
        getPhoneNumber();
        
        return ignoreRewritingRules;
    }
    
    /**
     * Get the account to force use for outgoing.
     * @return The account id to use for outgoing. {@link SipProfile#INVALID_ID} if no account should be used.
     */
    public long getAccountToCallTo() {
        if(accountToCallTo == null) {
            accountToCallTo = getIntent().getLongExtra(SipProfile.FIELD_ACC_ID, SipProfile.INVALID_ID);
        }
        return accountToCallTo;
    }
    
    /* Service connection */
    /**
     * Connect to sip service by flagging itself as the component to consider as outgoing activity
     */
    private void connectService() {
        PreferencesProviderWrapper prefsWrapper = new PreferencesProviderWrapper(this);
        Intent sipService = new Intent(SipManager.INTENT_SIP_SERVICE);
        if (prefsWrapper.isValidConnectionForOutgoing()) {
            sipService.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, getComponentName());
            startService(sipService);
        }
        bindService(sipService, connection, Context.BIND_AUTO_CREATE);
    }
    /**
     * Get connected sip service.
     * @return connected sip service from the activity if already connected. Null else.
     */
    public ISipService getConnectedService() {
        return service;
    }
    
    private ISipService service = null;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName component, IBinder binder) {
            service = ISipService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };
    
    @TargetApi(5)
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0
                && !Compatibility.isCompatible(5)) {
            onBackPressed();

        }
        return super.onKeyDown(keyCode, event);
    }

    public void onBackPressed() {
        finishServiceIfNeeded(false);
    }
    
    /**
     * Finish the activity and send unregistration to service as outgoing activity.
     * @param defer If true the activity will ask sip service to remain active until end of next call (because it will initiate a call).
     * If false, ask sip service to consider outgoing mode as not anymore valid right now. Usually cause call will be managed another way than a sip way.
     */
    public void finishServiceIfNeeded(boolean defer) {
        Intent intent = new Intent(defer ? SipManager.ACTION_DEFER_OUTGOING_UNREGISTER : SipManager.ACTION_OUTGOING_UNREGISTER);
        intent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, getComponentName());
        sendBroadcast(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetInternals();
        try {
            unbindService(connection);
        } catch (Exception e) {
        }
    }
    
    private void resetInternals() {
        phoneNumber = null;
        accountToCallTo = null;
        ignoreRewritingRules = false;
    }
}
