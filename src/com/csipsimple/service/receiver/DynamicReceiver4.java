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

package com.csipsimple.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DynamicReceiver4 extends BroadcastReceiver {

    private static final String THIS_FILE = "DynamicReceiver";
    

    // Comes from android.net.vpn.VpnManager.java
    // Action for broadcasting a connectivity state.
    public static final String ACTION_VPN_CONNECTIVITY = "vpn.connectivity";
    /** Key to the connectivity state of a connectivity broadcast event. */
    public static final String BROADCAST_CONNECTION_STATE = "connection_state";
    
    private SipService service;
    private PreferencesProviderWrapper prefsWrapper;
    
    
    // Store current state
    private boolean lastKnownVpnState = false;
    private String mNetworkType;
    private boolean mConnected = false;
    private String mLocalIp;
    
    private boolean hasStartedWifi = false;
    
    /**
     * Check if the intent received is a sticky broadcast one 
     * A compat way
     * @param it intent received
     * @return true if it's an initial sticky broadcast
     */
    public boolean compatIsInitialStickyBroadcast(Intent it) {
        if(ConnectivityManager.CONNECTIVITY_ACTION.equals(it.getAction())) {
            if(!hasStartedWifi) {
                hasStartedWifi = true;
                return true;
            }
        }
        return false;
    }
    
    public DynamicReceiver4(SipService aService) {
        service = aService;
        prefsWrapper = new PreferencesProviderWrapper(service);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(!compatIsInitialStickyBroadcast(intent)) {
            // Run the handler in SipServiceExecutor to be protected by wake lock
            service.getExecutor().execute(new SipRunnable()  {
                public void doRun() throws SameThreadException {
                    onReceiveInternal(context, intent);
                }
            });
        }
    }

    /**
     * Internal receiver that will run on sip executor thread
     * @param context Application context
     * @param intent Intent received
     * @throws SameThreadException
     */
    private void onReceiveInternal(Context context, Intent intent) throws SameThreadException {
        String action = intent.getAction();
        Log.d(THIS_FILE, "Internal receive " + action);
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Bundle b = intent.getExtras();
            if (b != null) {
                final NetworkInfo info = (NetworkInfo) b.get(ConnectivityManager.EXTRA_NETWORK_INFO);
                onConnectivityChanged(info, false);
            }
        } else if (action.equals(SipManager.ACTION_SIP_ACCOUNT_CHANGED)) {
            final long accountId = intent.getLongExtra(SipProfile.FIELD_ID, -1);
            // Should that be threaded?
            if (accountId != SipProfile.INVALID_ID) {
                final SipProfile account = service.getAccount(accountId);
                if (account != null) {
                    Log.d(THIS_FILE, "Enqueue set account registration");
                    service.setAccountRegistration(account, account.active ? 1 : 0, true);
                }
            }
        } else if (action.equals(SipManager.ACTION_SIP_CAN_BE_STOPPED)) {
            service.cleanStop();
        } else if (action.equals(SipManager.ACTION_SIP_REQUEST_RESTART)){
            service.restartSipStack();
        } else if(action.equals(ACTION_VPN_CONNECTIVITY)) {
            String connection_state = intent.getSerializableExtra(BROADCAST_CONNECTION_STATE).toString();
            boolean currentVpnState = connection_state.equalsIgnoreCase("CONNECTED");
            if(lastKnownVpnState != currentVpnState) {
                service.restartSipStack();
                lastKnownVpnState = currentVpnState;
            }
        }
    }
    
    /**
     * Try to determine local IP of the phone
     * @return the ip address of the phone.
     */
    private String determineLocalIp() {
        try {
            DatagramSocket s = new DatagramSocket();
            s.connect(InetAddress.getByName("192.168.1.1"), 80);
            return s.getLocalAddress().getHostAddress();
        } catch (IOException e) {
            Log.d(THIS_FILE, "determineLocalIp()", e);
            // dont do anything; there should be a connectivity change going
            return null;
        }
    }

    
    /**
     * Treat the fact that the connectivity has changed
     * @param info Network info
     * @param outgoingOnly start only if for outgoing 
     * @throws SameThreadException
     */
    private void onConnectivityChanged(NetworkInfo info, boolean outgoingOnly) throws SameThreadException {
        // We only care about the default network, and getActiveNetworkInfo()
        // is the only way to distinguish them. However, as broadcasts are
        // delivered asynchronously, we might miss DISCONNECTED events from
        // getActiveNetworkInfo(), which is critical to our SIP stack. To
        // solve this, if it is a DISCONNECTED event to our current network,
        // respect it. Otherwise get a new one from getActiveNetworkInfo().
        if (info == null || info.isConnected() ||
                !info.getTypeName().equals(mNetworkType)) {
            ConnectivityManager cm = (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
            info = cm.getActiveNetworkInfo();
        }

        // As a DISCONNECTED event everything that should be considered as such
        boolean isValid = prefsWrapper.isValidConnectionForOutgoing();
        if(!outgoingOnly) {
            isValid |= prefsWrapper.isValidConnectionForIncoming();
        }
        boolean connected = (info != null && info.isConnected() && isValid);
        
        String networkType = connected ? info.getTypeName() : "null";

        // Ignore the event if the current active network is not changed.
        if (connected == mConnected && networkType.equals(mNetworkType)) {
            return;
        }
        
        Log.d(THIS_FILE, "onConnectivityChanged(): " + mNetworkType +
                    " -> " + networkType);
        

        // Now process the event
        if (mConnected) {
            mLocalIp = null;
        }

        mConnected = connected;
        mNetworkType = networkType;

        if (connected) {
            mLocalIp = determineLocalIp();
            service.restartSipStack();
        } else {
            if(service.stopSipStack()) {
                service.stopSelf();
            }
        }
    }
}
