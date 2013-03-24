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

package com.csipsimple.ui.favorites;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipManager.PresenceStatus;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.utils.AccountListUtils;
import com.csipsimple.utils.AccountListUtils.AccountStatusDisplay;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class PresenceStatusSpinner extends Spinner implements android.widget.AdapterView.OnItemSelectedListener {

    private static final String THIS_FILE = "PresenceStatusSpinner";

    private long profileId = SipProfile.INVALID_ID;
    
    private boolean hasPresenceRegistration = false;
    private boolean isValid = false;
    

    private PresencesAdapter mAdapter;


    public PresenceStatusSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        

        List<CharSequence> list = new ArrayList<CharSequence>();
        if(!isInEditMode()) {
            String[] fromRes = context.getResources().getStringArray(R.array.presence_status_names);
            for(CharSequence str : fromRes) {
                list.add(str);
            }
        }
        
        mAdapter = new PresencesAdapter(getContext(), list);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(mAdapter);
        updateRegistration();
        
        setOnItemSelectedListener(this);
    }
    
    private class PresencesAdapter extends ArrayAdapter<CharSequence> {

        private LayoutInflater inflater;
        

        public PresencesAdapter(Context context, List<CharSequence> datas) {
            super(context, android.R.layout.simple_spinner_item, datas);
            inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent, false);
        }

        /**
         * Get the custom view for the presence spinner.
         * @param position position of the item
         * @param convertView view to convert
         * @param parent Group to open to
         * @param choiceMode true if it's part of drop down
         * @return the view recycled.
         */
        public View getCustomView(int position, View convertView, ViewGroup parent, boolean choiceMode) {
            View row = inflater.inflate(R.layout.fav_presence_item, parent, false);
            TextView label = (TextView) row.findViewById(R.id.item_status_text);
            ImageView icon = (ImageView) row.findViewById(R.id.item_status_icon);
            //TextView contactName = (TextView) row.findViewById(R.id.contact_name);
            
            // Show / hide
            //contactName.setVisibility(/*dropDownMode ? View.VISIBLE : */View.GONE);
            int padding = choiceMode ? 15 : 5;
            row.setPadding(padding, padding, padding, padding);
            
            // Content binding
            if(hasPresenceRegistration) {
                label.setText(getItem(position));
                icon.setImageResource(position == 0 ? android.R.drawable.presence_online : android.R.drawable.presence_invisible);
                icon.setVisibility(View.VISIBLE);
            }else {
                label.setText(choiceMode ? getItem(position) : getContext().getString(R.string.presence));
                icon.setVisibility(View.GONE);
            }
            return row;
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
        if(profileId != SipProfile.INVALID_ID) {
            if(hasPresenceRegistration && isValid) {
                if(position < PRESENCES_ITEMS_LENGTH) {
                    if(service != null) {
                        try {
                            service.setPresence(getSelectedPresence().ordinal(), "Test", profileId);
                        } catch (RemoteException e) {
                            Log.e(THIS_FILE, "Error while trying to set presence through service", e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Set the account profile that this spinner controls
     * @param accId
     */
    public void setProfileId(long accId) {
        profileId = accId;
    }
    
    private static final int PRESENCES_ITEMS_LENGTH = 2;
    
    private PresenceStatus getSelectedPresence() {
        switch (getSelectedItemPosition()) {
            case 1:
                return PresenceStatus.OFFLINE;
            case 0:
            default:
                return PresenceStatus.ONLINE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapter) {
        // We have nothing to do in this case
    }
    
    

    /**
     * Observer for changes of account registration status
     */
    class AccountStatusContentObserver extends ContentObserver {
        public AccountStatusContentObserver(Handler h) {
            super(h);
        }

        public void onChange(boolean selfChange) {
            Log.d(THIS_FILE, "Accounts status.onChange( " + selfChange + ")");
            updateRegistration();
        }
    }
    

    private static final String[] ACC_PROJECTION = new String[] {
            SipProfile.FIELD_ID,
            SipProfile.FIELD_ACC_ID, // Needed for default domain
            SipProfile.FIELD_REG_URI, // Needed for default domain
            SipProfile.FIELD_PROXY, // Needed for default domain
            SipProfile.FIELD_DEFAULT_URI_SCHEME, // Needed for default scheme
            SipProfile.FIELD_DISPLAY_NAME,
            SipProfile.FIELD_WIZARD,
            SipProfile.FIELD_PUBLISH_ENABLED
    };
    
    /**
     * Update user interface when registration of account has changed
     * This include change selected account if we are in canChangeIfValid mode
     */
    private void updateRegistration() {
        if(profileId < 0) {
            return;
        }
        SipProfile acc = SipProfile.getProfileFromDbId(getContext(), profileId, ACC_PROJECTION);
        isValid = false;
        hasPresenceRegistration = false;
        if(acc != null) {
            AccountStatusDisplay accountStatusDisplay = AccountListUtils
                    .getAccountDisplay(getContext(), acc.id);
            if(accountStatusDisplay.availableForCalls) {
                isValid = true;
            }
            hasPresenceRegistration = (acc.publish_enabled == 1);
        }
        
        setEnabled(isValid);
        setVisibility(hasPresenceRegistration ? View.VISIBLE : View.GONE);
    }
    
    
    // Service connection
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };
    

    private final Handler mHandler = new Handler();
    private AccountStatusContentObserver statusObserver = null;
    
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().bindService(new Intent(getContext(), SipService.class), connection, Context.BIND_AUTO_CREATE);
        if(statusObserver == null) {
            statusObserver = new AccountStatusContentObserver(mHandler);
            getContext().getContentResolver().registerContentObserver(SipProfile.ACCOUNT_STATUS_URI,
                    true, statusObserver);
        }
        updateRegistration();
    };
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            getContext().unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
        if (statusObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(statusObserver);
            statusObserver = null;
        }
        service = null;
    }

    
}
