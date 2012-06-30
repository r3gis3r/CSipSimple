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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class Betamax extends AuthorizationImplementation {

    static String PROVIDER = "provider";

    protected static final String THIS_FILE = "BetamaxW";
    private LinearLayout customWizard;
    private TextView customWizardText;
    ListPreference providerListPref;

    static SortedMap<String, String[]> providers = new TreeMap<String, String[]>() {
        private static final long serialVersionUID = 4984940975243241784L;
        {
            put("FreeCall", new String[] {
                    "sip.voiparound.com", "stun.voiparound.com"
            });
            put("InternetCalls", new String[] {
                    "sip.internetcalls.com", "stun.internetcalls.com"
            });
            put("Low Rate VoIP", new String[] {
                    "sip.lowratevoip.com", "stun.lowratevoip.com"
            });
            put("NetAppel", new String[] {
                    "sip.netappel.fr", "stun.netappel.fr"
            });
            put("Poivy", new String[] {
                    "sip.poivy.com", "stun.poivy.com"
            });
            put("SIP Discount", new String[] {
                    "sip.sipdiscount.com", "stun.sipdiscount.com"
            });
            put("SMS Discount", new String[] {
                    "sip.smsdiscount.com", "stun.smsdiscount.com"
            });
            put("SparVoIP", new String[] {
                    "sip.sparvoip.com", "stun.sparvoip.com"
            });
            put("VoIP Buster", new String[] {
                    "sip.voipbuster.com", "stun.voipbuster.com"
            });
            put("VoIP Buster Pro", new String[] {
                    "sip.voipbusterpro.com", "stun.voipbusterpro.com"
            });
            put("VoIP Cheap", new String[] {
                    "sip.voipcheap.com", "stun.voipcheap.com"
            });
            put("VoIP Discount", new String[] {
                    "sip.voipdiscount.com", "stun.voipdiscount.com"
            });
            put("12VoIP", new String[] {
                    "sip.12voip.com", "stun.12voip.com"
            });
            put("VoIP Stunt", new String[] {
                    "sip.voipstunt.com", "stun.voipstunt.com"
            });
            put("WebCall Direct", new String[] {
                    "sip.webcalldirect.com", "stun.webcalldirect.com"
            });
            put("Just VoIP", new String[] {
                    "sip.justvoip.com", "stun.justvoip.com"
            });
            put("Nonoh", new String[] {
                    "sip.nonoh.net", "stun.nonoh.net"
            });
            put("VoIPWise", new String[] {
                    "sip.voipwise.com", "stun.voipwise.com"
            });
            put("VoIPRaider", new String[] {
                    "sip.voipraider.com", "stun.voipraider.com"
            });
            put("BudgetSIP", new String[] {
                    "sip.budgetsip.com", "stun.budgetsip.com"
            });
            put("InterVoIP", new String[] {
                    "sip.intervoip.com", "stun.intervoip.com"
            });
            put("VoIPHit", new String[] {
                    "sip.voiphit.com", "stun.voiphit.com"
            });
            put("SmartVoIP", new String[] {
                    "sip.smartvoip.com", "stun.smartvoip.com"
            });
            put("ActionVoIP", new String[] {
                    "sip.actionvoip.com", "stun.actionvoip.com"
            });
            put("Jumblo", new String[] {
                    "sip.jumblo.com", "stun.jumblo.com"
            });
            put("Rynga", new String[] {
                    "sip.rynga.com", "stun.rynga.com"
            });
            put("PowerVoIP", new String[] {
                    "sip.powervoip.com", "stun.powervoip.com"
            });
            put("Voice Trading", new String[] {
                    "sip.voicetrading.com", "stun.voicetrading.com"
            });
            put("EasyVoip", new String[] {
                    "sip.easyvoip.com", "stun.easyvoip.com"
            });
            put("VoipBlast", new String[] {
                    "sip.voipblast.com", "stun.voipblast.com"
            });
            put("FreeVoipDeal", new String[] {
                    "sip.freevoipdeal.com", "stun.freevoipdeal.com"
            });
            put("VoipAlot", new String[] {
                    "sip.voipalot.com", ""
            });
            put("CosmoVoip", new String[] {
                    "sip.cosmovoip.com", "stun.cosmovoip.com"
            });
            put("BudgetVoipCall", new String[] {
                    "sip.budgetvoipcall.com", "stun.budgetvoipcall.com"
            });
            put("CheapBuzzer", new String[] {
                    "sip.cheapbuzzer.com", "stun.cheapbuzzer.com"
            });
            put("CallPirates", new String[] {
                    "sip.callpirates.com", "stun.callpirates.com"
            });
            put("CheapVoipCall", new String[] {
                    "sip.cheapvoipcall.com", "stun.cheapvoipcall.com"
            });
            put("DialCheap", new String[] {
                    "sip.dialcheap.com", "stun.dialcheap.com"
            });
            put("DiscountCalling", new String[] {
                    "sip.discountcalling.com", "stun.discountcalling.com"
            });
            put("Frynga", new String[] {
                    "sip.frynga.com", "stun.frynga.com"
            });
            put("GlobalFreeCall", new String[] {
                    "sip.globalfreecall.com", "stun.globalfreecall.com"
            });
            put("HotVoip", new String[] {
                    "sip.hotvoip.com", "stun.hotvoip.com"
            });
            put("MEGAvoip", new String[] {
                    "sip.megavoip.com", "stun.megavoip.com"
            });
            put("PennyConnect", new String[] {
                    "sip.pennyconnect.com", "stun.pennyconnect.com"
            });
            put("Rebvoice", new String[] {
                    "sip.rebvoice.com", "stun.rebvoice.com"
            });
            put("StuntCalls", new String[] {
                    "sip.stuntcalls.com", "stun.stuntcalls.com"
            });
            put("VoipBlazer", new String[] {
                    "sip.voipblazer.com", "stun.voipblazer.com"
            });
            put("VoipCaptain", new String[] {
                    "sip.voipcaptain.com", "stun.voipcaptain.com"
            });
            put("VoipChief", new String[] {
                    "sip.voipchief.com", "stun.voipchief.com"
            });
            put("VoipJumper", new String[] {
                    "sip.voipjumper.com", "stun.voipjumper.com"
            });
            put("VoipMove", new String[] {
                    "sip.voipmove.com", "stun.voipmove.com"
            });
            put("VoipSmash", new String[] {
                    "sip.voipsmash.com", "stun.voipsmash.com"
            });
            put("VoipGain", new String[] {
                    "sip.voipgain.com", "stun.voipgain.com"
            });
            put("VoipZoom", new String[] {
                    "sip.voipzoom.com", "stun.voipzoom.com"
            });
            put("Telbo", new String[] {
                    "sip.telbo.com", "stun.telbo.com"
            });

            /*
             * put("InternetCalls", new String[] {"", ""});
             */
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultName() {
        return "Betamax";
    }

    private static final String PROVIDER_LIST_KEY = "provider_list";

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillLayout(final SipProfile account) {
        super.fillLayout(account);

        accountUsername.setTitle(R.string.w_advanced_caller_id);
        accountUsername.setDialogTitle(R.string.w_advanced_caller_id_desc);

        boolean recycle = true;
        providerListPref = (ListPreference) findPreference(PROVIDER_LIST_KEY);
        if (providerListPref == null) {
            Log.d(THIS_FILE, "Create new list pref");
            providerListPref = new ListPreference(parent);
            providerListPref.setKey(PROVIDER_LIST_KEY);
            recycle = false;
        } else {
            Log.d(THIS_FILE, "Recycle existing list pref");
        }

        CharSequence[] v = new CharSequence[providers.size()];
        int i = 0;
        for (String pv : providers.keySet()) {
            v[i] = pv;
            i++;
        }

        providerListPref.setEntries(v);
        providerListPref.setEntryValues(v);
        providerListPref.setKey(PROVIDER);
        providerListPref.setDialogTitle("Provider");
        providerListPref.setTitle("Provider");
        providerListPref.setSummary("Betamax clone provider");
        providerListPref.setDefaultValue("12VoIP");

        if (!recycle) {
            addPreference(providerListPref);
        }
        hidePreference(null, SERVER);

        String domain = account.getDefaultDomain();
        if (domain != null) {
            for (Entry<String, String[]> entry : providers.entrySet()) {
                String[] val = entry.getValue();
                if (val[0].equalsIgnoreCase(domain)) {
                    Log.d(THIS_FILE, "Set provider list pref value to " + entry.getKey());
                    providerListPref.setValue(entry.getKey());
                    break;
                }
            }
        }
        Log.d(THIS_FILE, providerListPref.getValue());

        // Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);

        updateAccountInfos(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipProfile buildAccount(SipProfile account) {
        account = super.buildAccount(account);
        return account;
    }

    private static HashMap<String, Integer> SUMMARIES = new HashMap<String, Integer>() {
        /**
		 * 
		 */
        private static final long serialVersionUID = -5743705263738203615L;

        {
            put(DISPLAY_NAME, R.string.w_common_display_name_desc);
            put(USER_NAME, R.string.w_advanced_caller_id_desc);
            put(AUTH_NAME, R.string.w_authorization_auth_name_desc);
            put(PASSWORD, R.string.w_common_password_desc);
            put(SERVER, R.string.w_common_server_desc);
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDescriptions() {
        super.updateDescriptions();
        setStringFieldSummary(PROVIDER);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultFieldSummary(String fieldName) {
        Integer res = SUMMARIES.get(fieldName);
        if (fieldName == PROVIDER) {
            if (providerListPref != null) {
                return providerListPref.getValue();
            }
        }
        if (res != null) {
            return parent.getString(res);
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canSave() {
        boolean isValid = true;

        isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
        isValid &= checkField(accountUsername, isEmpty(accountUsername));
        isValid &= checkField(accountAuthorization, isEmpty(accountAuthorization));
        isValid &= checkField(accountPassword, isEmpty(accountPassword));
        return isValid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDomain() {
        String provider = providerListPref.getValue();
        if (provider != null) {
            String[] set = providers.get(provider);
            return set[0];
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean needRestart() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        // Disable ICE and turn on STUN!!!
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        String provider = providerListPref.getValue();
        if (provider != null) {
            String[] set = providers.get(provider);
            if (!TextUtils.isEmpty(set[1])) {
                prefs.addStunServer(set[1]);
            }
        }

        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_ICE, false);
    }

    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.GONE);
            accountBalanceHelper.launchRequest(acc);
        } else {
            // add a row to link
            customWizard.setVisibility(View.GONE);

        }
    }

    private AccountBalanceHelper accountBalanceHelper = new AccountBalanceHelper() {

        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {

            String requestURL = "https://";
            String provider = providerListPref.getValue();
            if (provider != null) {
                String[] set = providers.get(provider);
                requestURL += set[0].replace("sip.", "www.");
                requestURL += "/myaccount/getbalance.php";
                requestURL += "?username=" + acc.username;
                requestURL += "&password=" + acc.data;

                return new HttpGet(requestURL);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parseResponseLine(String line) {
            try {
                float value = Float.parseFloat(line.trim());
                if (value >= 0) {
                    return "Balance : " + Math.round(value * 100.0) / 100.0 + " euros";
                }
            } catch (NumberFormatException e) {
                Log.e(THIS_FILE, "Can't get value for line");
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultError() {
            customWizard.setVisibility(View.GONE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void applyResultSuccess(String balanceText) {
            customWizardText.setText(balanceText);
            customWizard.setVisibility(View.VISIBLE);
        }

    };

}
