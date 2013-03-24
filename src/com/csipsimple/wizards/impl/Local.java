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

package com.csipsimple.wizards.impl;

import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

public class Local extends BaseImplementation {
	protected static final String THIS_FILE = "Local W";
	private static final String TRANSPORT_LIST_KEY = "transport_list";
	
	private EditTextPreference accountDisplayName;
    private ListPreference transportPref;
    
	private void bindFields() {
		accountDisplayName = (EditTextPreference) findPreference(SipProfile.FIELD_DISPLAY_NAME);
		hidePreference(null, "caller_id");
		hidePreference(null, "server");
        hidePreference(null, "auth_id");
		hidePreference(null, "username");
		hidePreference(null, "password");
		hidePreference(null, "use_tcp");
		hidePreference(null, "proxy");
	}

	public void fillLayout(final SipProfile account) {
		bindFields();
		
		accountDisplayName.setText(account.display_name);
		
        //Get wizard specific row
        TextView tv = (TextView) parent.findViewById(R.id.custom_wizard_text);
        tv.setText(getLocalIpAddresses());
        tv.setTextSize(10.0f);
        tv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        ((LinearLayout) parent.findViewById(R.id.custom_wizard_row)).setVisibility(View.VISIBLE);
        

        boolean recycle = true;
        transportPref = (ListPreference) findPreference(TRANSPORT_LIST_KEY);
        if (transportPref == null) {
            transportPref = new ListPreference(parent);
            transportPref.setKey(TRANSPORT_LIST_KEY);
            recycle = false;
        } else {
            Log.d(THIS_FILE, "Recycle existing list pref");
        }

        transportPref.setEntries(R.array.transport_choices);
        transportPref.setEntryValues(R.array.transport_values);
        transportPref.setDialogTitle(R.string.transport);
        transportPref.setTitle(R.string.transport);
        transportPref.setSummary(R.string.transport_desc);
        transportPref.setDefaultValue(Integer.toString(SipProfile.TRANSPORT_UDP));

        if (!recycle) {
            addPreference(transportPref);
        }
        transportPref.setValue(Integer.toString(account.transport));
	}

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
	}
	
	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
		 * 
		 */
		private static final long serialVersionUID = 3055562364235868653L;

	{
		put("display_name", R.string.w_common_display_name_desc);
	}};

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if(res != null) {
			return parent.getString( res );
		}
		return "";
	}

	public boolean canSave() {
		boolean isValid = true;
		
		isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));

		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		account.display_name = accountDisplayName.getText();
		account.reg_uri = "";
		account.acc_id = "";
		account.transport = getIntValue(transportPref, SipProfile.TRANSPORT_UDP);
		return account;
	}

    private static int getIntValue(ListPreference pref, int defaultValue) {
        try {
            return Integer.parseInt(pref.getValue());
        }catch(NumberFormatException e) {
            Log.e(THIS_FILE, "List item is not a number");
        }
        return defaultValue;
    }
    
	@Override
	public int getBasePreferenceResource() {
		return R.xml.w_advanced_preferences;
	}
	
	@Override
	public boolean needRestart() {
		return true;
	}

	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		int transport = getIntValue(transportPref, SipProfile.TRANSPORT_UDP);
		if(transport == SipProfile.TRANSPORT_UDP) {
		    prefs.setPreferenceStringValue(SipConfigManager.UDP_TRANSPORT_PORT, "5060");
		}else if(transport == SipProfile.TRANSPORT_TCP) {
		    prefs.setPreferenceStringValue(SipConfigManager.TCP_TRANSPORT_PORT, "5060");
		}else if(transport == SipProfile.TRANSPORT_TLS) {
            prefs.setPreferenceStringValue(SipConfigManager.TLS_TRANSPORT_PORT, "5061");
        }
		
	}
	
    public String getLocalIpAddresses() {
        ArrayList<String> addresses = new ArrayList<String>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        addresses.add(inetAddress.getHostAddress().toString());
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(THIS_FILE, "Impossible to get ip address", ex);
        }
        return TextUtils.join("\n", addresses);
    }
    
    
}
