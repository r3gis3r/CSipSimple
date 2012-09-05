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

import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.utils.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DynamicReceiver4 extends BroadcastReceiver {

    private static final String THIS_FILE = "DynamicReceiver";
    

    // Comes from android.net.vpn.VpnManager.java
    // Action for broadcasting a connectivity state.
    public static final String ACTION_VPN_CONNECTIVITY = "vpn.connectivity";
    /** Key to the connectivity state of a connectivity broadcast event. */
    public static final String BROADCAST_CONNECTION_STATE = "connection_state";
    
    private SipService service;
    
    
    // Store current state
    private String mNetworkType;
    private boolean mConnected = false;
    private String mRoutes = "";
    
    private boolean hasStartedWifi = false;


    private Timer pollingTimer;

    
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
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        // Run the handler in SipServiceExecutor to be protected by wake lock
        service.getExecutor().execute(new SipRunnable()  {
            public void doRun() throws SameThreadException {
                onReceiveInternal(context, intent, compatIsInitialStickyBroadcast(intent));
            }
        });
    }
    
    

    /**
     * Internal receiver that will run on sip executor thread
     * @param context Application context
     * @param intent Intent received
     * @throws SameThreadException
     */
    private void onReceiveInternal(Context context, Intent intent, boolean isSticky) throws SameThreadException {
        String action = intent.getAction();
        Log.d(THIS_FILE, "Internal receive " + action);
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            onConnectivityChanged(activeNetwork, isSticky);
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
            onConnectivityChanged(null, isSticky);
        }
    }
    

    private static final String PROC_NET_ROUTE = "/proc/net/route";
    private String dumpRoutes() {
        String routes = "";
        FileReader fr = null;
        try {
            fr = new FileReader(PROC_NET_ROUTE);
            if(fr != null) {
                StringBuffer contentBuf = new StringBuffer();
                BufferedReader buf = new BufferedReader(fr);
                String line;
                while ((line = buf.readLine()) != null) {
                    contentBuf.append(line);
                }
                routes = contentBuf.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e(THIS_FILE, "No route file found routes", e);
        } catch (IOException e) {
            Log.e(THIS_FILE, "Unable to read route file", e);
        }finally {
            try {
                fr.close();
            } catch (IOException e) {
                Log.e(THIS_FILE, "Unable to close route file", e);
            }
        }
        
        return routes;
    }

    
    /**
     * Treat the fact that the connectivity has changed
     * @param info Network info
     * @param incomingOnly start only if for outgoing 
     * @throws SameThreadException
     */
    private void onConnectivityChanged(NetworkInfo info, boolean isSticky) throws SameThreadException {
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

        boolean connected = (info != null && info.isConnected() && service.isConnectivityValid());
        String networkType = connected ? info.getTypeName() : "null";
        String currentRoutes = dumpRoutes();
        String oldRoutes;
        synchronized (mRoutes) {
            oldRoutes = mRoutes;
        }
        
        // Ignore the event if the current active network is not changed.
        if (connected == mConnected && networkType.equals(mNetworkType) && currentRoutes.equals(oldRoutes)) {
            return;
        }
        if(Log.getLogLevel() >= 4) {
            if(!networkType.equals(mNetworkType)) {
                Log.d(THIS_FILE, "onConnectivityChanged(): " + mNetworkType +
                            " -> " + networkType);
            }else {
                Log.d(THIS_FILE, "Route changed : "+ mRoutes+" -> "+currentRoutes);
            }
        }
        // Now process the event
        synchronized (mRoutes) {
            mRoutes = currentRoutes;
        }
        mConnected = connected;
        mNetworkType = networkType;

        if(!isSticky) {
            if (connected) {
                service.restartSipStack();
            } else {
                Log.d(THIS_FILE, "We are not connected, stop");
                if(service.stopSipStack()) {
                    service.stopSelf();
                }
            }
        }
    }
    
    
    
    public void startMonitoring() {
        int pollingIntervalMin = service.getPrefs().getPreferenceIntegerValue(SipConfigManager.NETWORK_ROUTES_POLLING);

        Log.d(THIS_FILE, "Start monitoring of route file ? " + pollingIntervalMin);
        if(pollingIntervalMin > 0) {
            pollingTimer = new Timer("RouteChangeMonitor", true);
            pollingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String currentRoutes = dumpRoutes();
                    String oldRoutes;
                    synchronized (mRoutes) {
                        oldRoutes = mRoutes;
                    }
                    if(!currentRoutes.equalsIgnoreCase(oldRoutes)) {
                        Log.d(THIS_FILE, "Route changed");
                        // Run the handler in SipServiceExecutor to be protected by wake lock
                        service.getExecutor().execute(new SipRunnable()  {
                            public void doRun() throws SameThreadException {
                                onConnectivityChanged(null, false);
                            }
                        });
                    }
                }
            }, new Date(), pollingIntervalMin * 60 * 1000);
        }
    }
    
    public void stopMonitoring() {
        if(pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer.purge();
            pollingTimer = null;
        }
    }
}
