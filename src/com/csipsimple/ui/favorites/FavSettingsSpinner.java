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
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;

public class FavSettingsSpinner extends Spinner implements android.widget.AdapterView.OnItemSelectedListener {

    private static final String THIS_FILE = "FavSettingsSpinner";

    private long profileId = SipProfile.INVALID_ID;
    

    private PresencesAdapter mAdapter;

    private boolean hasPresenceRegistration;

    public FavSettingsSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        

        mAdapter = new PresencesAdapter(getContext());
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setAdapter(mAdapter);
        
        setOnItemSelectedListener(this);
    }

    
    private class PresencesAdapter extends ArrayAdapter<CharSequence> {

        private LayoutInflater inflater;

        public PresencesAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item, getListItems());
            inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.fav_presence_item, parent, false);
            }
            TextView label = (TextView) convertView.findViewById(R.id.item_status_text);
            ImageView icon = (ImageView) convertView.findViewById(R.id.item_status_icon);
            

            label.setText(getItem(position));
            icon.setVisibility(View.GONE);
            
            int padding = 5;
            convertView.setPadding(padding, padding, padding, padding);
            
            return convertView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null) {
                convertView = inflater.inflate(R.layout.fav_setting_btn, parent, false);
            }
            return convertView;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
        if(service != null && profileId != SipProfile.INVALID_ID) {
            try {
                service.setPresence(getSelectedPresence().ordinal(), "Test", profileId);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Error while trying to set presence through service", e);
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
    
    
    
    List<CharSequence> getListItems(){
        ArrayList<CharSequence> l = new ArrayList<CharSequence>();
        l.add(getContext().getString(R.string.set_android_group));
        l.add(getContext().getString(R.string.set_sip_data));
        
        return l;
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
    
    
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().bindService(new Intent(getContext(), SipService.class), connection, Context.BIND_AUTO_CREATE);
    };
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            getContext().unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
        service = null;
    }

    
}
