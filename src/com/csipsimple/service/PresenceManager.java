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

package com.csipsimple.service;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService.SameThreadException;
import com.csipsimple.service.SipService.SipRunnable;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.contacts.ContactsWrapper;

import java.util.ArrayList;
import java.util.List;

public class PresenceManager {
    private static final String THIS_FILE = "PresenceManager";

    private static final String[] ACC_PROJECTION = new String[] {
            SipProfile.FIELD_ID,
            SipProfile.FIELD_DISPLAY_NAME,
            SipProfile.FIELD_WIZARD
    };
    

    private SipService service;

    private final Handler mHandler = new Handler();
    private ArrayList<SipProfile> addedAccounts = new ArrayList<SipProfile>();

    private AccountStatusContentObserver statusObserver;

    public synchronized void startMonitoring(SipService srv) {
        service = srv;

        statusObserver = new AccountStatusContentObserver(mHandler);
        service.getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI,
                true, statusObserver);
    }

    public synchronized void stopMonitoring() {
        if (statusObserver != null) {
            service.getContentResolver().unregisterContentObserver(statusObserver);
            statusObserver = null;
        }
        service = null;
    }
    
    
    /**
     * Get buddies sip uris associated with a sip profile
     * @param acc the profile to search in
     * @return a list of sip uris
     */
    private synchronized List<String> getBuddiesForAccount(SipProfile acc){
        if(service != null) {
            return ContactsWrapper.getInstance().getCSipPhonesByGroup(service,
                    acc.display_name);
        }else {
            return new ArrayList<String>();
        }
    }

    /**
     * Add buddies for a given account
     * @param acc
     */
    private synchronized void addBuddiesForAccount(SipProfile acc) {
        // Get buddies uris for this account
        final List<String> toAdd = getBuddiesForAccount(acc);

        if (toAdd.size() > 0 && service != null) {
            service.getExecutor().execute(new SipRunnable() {

                @Override
                protected void doRun() throws SameThreadException {

                    for (String csipUri : toAdd) {
                        service.addBuddy("sip:" + csipUri);
                    }
                }
            });
        }
        addedAccounts.add(acc);
    }
    
    
    /**
     * Delete buddies for a given account
     * @param acc
     */
    private synchronized void deleteBuddiesForAccount(SipProfile acc) {
        // Get buddies uris for this account
        final List<String> toDel = getBuddiesForAccount(acc);

        if (toDel.size() > 0 && service != null) {
            for (String csipUri : toDel) {
                ContactsWrapper.getInstance().updateCSipPresence(service, csipUri, SipManager.PresenceStatus.UNKNOWN, "");
            }
            
            service.getExecutor().execute(new SipRunnable() {

                @Override
                protected void doRun() throws SameThreadException {
                    if(service != null) {
                        for (String csipUri : toDel) {
                            service.removeBuddy("sip:" + csipUri);
                        }
                    }
                }
            });
        }
        // Find the correct account to remove
        int toRemoveIndex = -1;
        for(int idx = 0; idx < addedAccounts.size(); idx++) {
            SipProfile existingAcc = addedAccounts.get(idx);
            if(existingAcc.id == acc.id) {
                toRemoveIndex = idx;
                break;
            }
        }
        
        if(toRemoveIndex >= 0) {
            addedAccounts.remove(toRemoveIndex);
        }

    }

    /**
     * Update internal state of registered account
     * Push buddies for registered account
     * Remove buddies for offline accounts
     */
    private synchronized void updateRegistrations() {
        if(service == null) {
            // Nothing to do at this point
            return;
        }
        Cursor c = service.getContentResolver().query(SipProfile.ACCOUNT_URI, ACC_PROJECTION,
                SipProfile.FIELD_ACTIVE + "=?", new String[] {
                    "1"
                }, null);

        ArrayList<SipProfile> accToAdd = new ArrayList<SipProfile>();
        ArrayList<SipProfile> accToRemove = new ArrayList<SipProfile>();
        ArrayList<Long> alreadyAddedAcc = new ArrayList<Long>();
        for (SipProfile addedAcc : addedAccounts) {
            alreadyAddedAcc.add(addedAcc.id);
        }
        // Decide which accounts should be removed, added, left unchanged
        if (c != null && c.getCount() > 0) {
            try {
                if (c.moveToFirst()) {
                    do {
                        final SipProfile acc = new SipProfile(c);

                        AccountStatusDisplay accountStatusDisplay = AccountListUtils
                                .getAccountDisplay(service, acc.id);
                        if (accountStatusDisplay.availableForCalls) {
                            if (!alreadyAddedAcc.contains(acc.id)) {
                                accToAdd.add(acc);
                            }
                        } else {
                            if (alreadyAddedAcc.contains(acc.id)) {
                                accToRemove.add(acc);
                            }
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Error on looping over sip profiles", e);
            } finally {
                c.close();
            }
        }

        for(SipProfile acc : accToRemove) {
            deleteBuddiesForAccount(acc);
        }

        for(SipProfile acc : accToAdd) {
            addBuddiesForAccount(acc);
        }
    }

    /**
     * Observer for changes of account registration status
     */
    class AccountStatusContentObserver extends ContentObserver {

        public AccountStatusContentObserver(Handler h) {
            super(h);
        }

        public void onChange(boolean selfChange) {
            updateRegistrations();
        }
    }
    
    /**
     * Forward status change for a buddy to manager
     * @param buddyUri buddy uri 
     * @param monitorPres whether the status is currently monitored
     * @param presStatus the status 
     * @param statusText the text representing this status
     */
    public void changeBuddyState(String buddyUri, int monitorPres, SipManager.PresenceStatus presStatus, String statusText) {
        if(service != null) {
            ContactsWrapper.getInstance().updateCSipPresence(service, buddyUri.replace("sip:", ""), presStatus, statusText);
        }
        
    }

}
