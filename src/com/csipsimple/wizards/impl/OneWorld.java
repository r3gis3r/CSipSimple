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

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.models.Filter;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationWebview;
import com.csipsimple.wizards.utils.AccountCreationWebview.OnAccountCreationDoneListener;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class OneWorld extends SimpleImplementation implements OnAccountCreationDoneListener {

    private LinearLayout customWizard;
    private TextView customWizardText;
    private AccountCreationWebview extAccCreator;

    @Override
    protected String getDomain() {
        return "sip.1worldtelecom.mobi";
    }

    @Override
    protected String getDefaultName() {
        return "1WorldTelecom";
    }

    @Override
    public void fillLayout(SipProfile account) {
        super.fillLayout(account);

        // Get wizard specific row
        customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
        customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);

        updateAccountInfos(account);

        extAccCreator = new AccountCreationWebview(parent,
                "http://www.1worldsip.com/webregister.php", this);
    }

    private AccountBalanceHelper accountBalanceHelper = new AccountBalance(this);

    private void updateAccountInfos(final SipProfile acc) {
        if (acc != null && acc.id != SipProfile.INVALID_ID) {
            customWizard.setVisibility(View.VISIBLE);
            customWizard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = "http://www.1worldsip.com";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    parent.startActivity(i);

                }
            });
            accountBalanceHelper.launchRequest(acc);
        } else {
            // add a row to link
            customWizardText.setText(R.string.create_account);
            customWizard.setVisibility(View.VISIBLE);
            customWizard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    extAccCreator.show();
                }
            });
        }
    }

    private static final String THIS_FILE = "1world";

    private static class AccountBalance extends AccountBalanceHelper {
        
        WeakReference<OneWorld> w;
        
        AccountBalance(OneWorld wizard){
            w = new WeakReference<OneWorld>(wizard);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {
            String requestURL = "https://1worldsip.com/c5/balance.php?"
                    + "pin=" + acc.username
                    + "&pwd=" + acc.data ;
            HttpGet req = new HttpGet(requestURL);
            req.addHeader("User-Agent", "SMSSync-Android/1.0)");
            return req;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parseResponseLine(String line) {
            try {
                float value = Float.parseFloat(line.trim());
                if (value >= 0) {
                    return "Bal : " + Math.round(value * 100.0) / 100.0;
                }
            } catch (NumberFormatException e) {
                Log.e(THIS_FILE, "Can't get value for line");
            }
            return null;
        }

        @Override
        public void applyResultError() {
            OneWorld wizard = w.get();
            if(wizard != null) {
                wizard.customWizard.setVisibility(View.GONE);
            }
        }

        @Override
        public void applyResultSuccess(String balanceText) {
            OneWorld wizard = w.get();
            if(wizard != null) {
                wizard.customWizardText.setText(balanceText);
                wizard.customWizard.setVisibility(View.VISIBLE);
            }
        }
        
    };

    @Override
    public void setDefaultParams(PreferencesWrapper prefs) {
        super.setDefaultParams(prefs);
        // Stun
        prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_STUN, true);
        prefs.addStunServer("stun01.1worldtelecom.mobi");
        prefs.addStunServer("stun02.1worldtelecom.mobi");
        
        // User agent -- useful?
        //prefs.setPreferenceStringValue(SipConfigManager.USER_AGENT, "1WorldVoip");

        // Codecs -- Assume they have legal rights to provide g729 to each users
        // As they activate it by default in their forked app.
        // For Narrowband
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_NB, "100");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_NB, "150");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_NB, "200");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_NB, "0"); /*
                                                                                * Disable
                                                                                * by
                                                                                * default
                                                                                */
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_NB, "0");
        prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_NB, "0");

        // For Wideband
        prefs.setCodecPriority("PCMU/8000/1", SipConfigManager.CODEC_WB, "100");
        prefs.setCodecPriority("PCMA/8000/1", SipConfigManager.CODEC_WB, "150");
        prefs.setCodecPriority("speex/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("speex/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("GSM/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G722/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G729/8000/1", SipConfigManager.CODEC_WB, "200");
        prefs.setCodecPriority("iLBC/8000/1", SipConfigManager.CODEC_WB, "0"); /*
                                                                                * Disable
                                                                                * by
                                                                                * default
                                                                                */
        prefs.setCodecPriority("SILK/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/12000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("SILK/24000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("CODEC2/8000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G7221/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("G7221/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("ISAC/16000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("ISAC/32000/1", SipConfigManager.CODEC_WB, "0");
        prefs.setCodecPriority("AMR/8000/1", SipConfigManager.CODEC_WB, "0");
    }

    @Override
    public SipProfile buildAccount(SipProfile account) {
        SipProfile acc = super.buildAccount(account);
        String regUri = "sip:" + getDomain() + ":55061";
        acc.reg_uri = regUri;
        acc.proxies = new String[] {
            regUri
        };
        return acc;
    }

    @Override
    public List<Filter> getDefaultFilters(SipProfile acc) {
        ArrayList<Filter> filters = new ArrayList<Filter>();

        Filter f;

        // Remove unwanted leadings numbers
        String[] removableLeadings = new String[] {
                "+", "001", "011", "0"
        };
        for (String removable : removableLeadings) {
            f = getFilterRemoveLeading(removable);
            f.account = (int) acc.id;
            filters.add(f);
        }

        String countryCodeNbr = null;
        if (Locale.getDefault() != null && !TextUtils.isEmpty(Locale.getDefault().getCountry())) {
            String countryCode = Locale.getDefault().getCountry().toUpperCase();
            if (countryCodeNbrs.containsKey(countryCode)) {
                countryCodeNbr = countryCodeNbrs.get(countryCode);
            }
        }
        if (!TextUtils.isEmpty(countryCodeNbr)) {
            String[] prefixableLeadings = new String[] {
                    "0", "888"
            };
            for (String removable : prefixableLeadings) {
                // Autoreplace country code wherever it's necessary
                f = getFilterRemoveReplaceLeading(removable, countryCodeNbr);
                f.account = (int) acc.id;
                filters.add(f);
            }
        }

        return filters;
    }

    private Filter getFilterRemoveLeading(String prefix) {
        return getFilterRemoveReplaceLeading(prefix, "");
    }

    private Filter getFilterRemoveReplaceLeading(String prefix, String replace) {

        Filter f = new Filter();
        f.action = Filter.ACTION_REPLACE;
        f.matchPattern = "^" + Pattern.quote(prefix) + "(.*)$";
        f.replacePattern = replace + "$1";
        f.matchType = Filter.MATCHER_STARTS;

        return f;
    }

    private final static Map<String, String> countryCodeNbrs;
    static {
        countryCodeNbrs = new HashMap<String, String>();

        countryCodeNbrs.put("AF", "93");
        countryCodeNbrs.put("AL", "355");
        countryCodeNbrs.put("DZ", "213");
        countryCodeNbrs.put("AD", "376");
        countryCodeNbrs.put("AO", "244");
        countryCodeNbrs.put("AQ", "672");
        countryCodeNbrs.put("AR", "54");
        countryCodeNbrs.put("AM", "374");
        countryCodeNbrs.put("AW", "297");
        countryCodeNbrs.put("AU", "61");
        countryCodeNbrs.put("AT", "43");
        countryCodeNbrs.put("AZ", "994");
        countryCodeNbrs.put("BH", "973");
        countryCodeNbrs.put("BD", "880");
        countryCodeNbrs.put("BY", "375");
        countryCodeNbrs.put("BE", "32");
        countryCodeNbrs.put("BZ", "501");
        countryCodeNbrs.put("BJ", "229");
        countryCodeNbrs.put("BT", "975");
        countryCodeNbrs.put("BO", "591");
        countryCodeNbrs.put("BA", "387");
        countryCodeNbrs.put("BW", "267");
        countryCodeNbrs.put("BR", "55");
        countryCodeNbrs.put("BN", "673");
        countryCodeNbrs.put("BG", "359");
        countryCodeNbrs.put("BF", "226");
        countryCodeNbrs.put("MM", "95");
        countryCodeNbrs.put("BI", "257");
        countryCodeNbrs.put("KH", "855");
        countryCodeNbrs.put("CM", "237");
        countryCodeNbrs.put("CA", "1");
        countryCodeNbrs.put("CV", "238");
        countryCodeNbrs.put("CF", "236");
        countryCodeNbrs.put("TD", "235");
        countryCodeNbrs.put("CL", "56");
        countryCodeNbrs.put("CN", "86");
        countryCodeNbrs.put("CX", "61");
        countryCodeNbrs.put("CC", "61");
        countryCodeNbrs.put("CO", "57");
        countryCodeNbrs.put("KM", "269");
        countryCodeNbrs.put("CG", "242");
        countryCodeNbrs.put("CD", "243");
        countryCodeNbrs.put("CK", "682");
        countryCodeNbrs.put("CR", "506");
        countryCodeNbrs.put("HR", "385");
        countryCodeNbrs.put("CU", "53");
        countryCodeNbrs.put("CY", "357");
        countryCodeNbrs.put("CZ", "420");
        countryCodeNbrs.put("DK", "45");
        countryCodeNbrs.put("DJ", "253");
        countryCodeNbrs.put("TL", "670");
        countryCodeNbrs.put("EC", "593");
        countryCodeNbrs.put("EG", "20");
        countryCodeNbrs.put("SV", "503");
        countryCodeNbrs.put("GQ", "240");
        countryCodeNbrs.put("ER", "291");
        countryCodeNbrs.put("EE", "372");
        countryCodeNbrs.put("ET", "251");
        countryCodeNbrs.put("FK", "500");
        countryCodeNbrs.put("FO", "298");
        countryCodeNbrs.put("FJ", "679");
        countryCodeNbrs.put("FI", "358");
        countryCodeNbrs.put("FR", "33");
        countryCodeNbrs.put("PF", "689");
        countryCodeNbrs.put("GA", "241");
        countryCodeNbrs.put("GM", "220");
        countryCodeNbrs.put("GE", "995");
        countryCodeNbrs.put("DE", "49");
        countryCodeNbrs.put("GH", "233");
        countryCodeNbrs.put("GI", "350");
        countryCodeNbrs.put("GR", "30");
        countryCodeNbrs.put("GL", "299");
        countryCodeNbrs.put("GT", "502");
        countryCodeNbrs.put("GN", "224");
        countryCodeNbrs.put("GW", "245");
        countryCodeNbrs.put("GY", "592");
        countryCodeNbrs.put("HT", "509");
        countryCodeNbrs.put("HN", "504");
        countryCodeNbrs.put("HK", "852");
        countryCodeNbrs.put("HU", "36");
        countryCodeNbrs.put("IN", "91");
        countryCodeNbrs.put("ID", "62");
        countryCodeNbrs.put("IR", "98");
        countryCodeNbrs.put("IQ", "964");
        countryCodeNbrs.put("IE", "353");
        countryCodeNbrs.put("IM", "44");
        countryCodeNbrs.put("IL", "972");
        countryCodeNbrs.put("IT", "39");
        countryCodeNbrs.put("CI", "225");
        countryCodeNbrs.put("JP", "81");
        countryCodeNbrs.put("JO", "962");
        countryCodeNbrs.put("KZ", "7");
        countryCodeNbrs.put("KE", "254");
        countryCodeNbrs.put("KI", "686");
        countryCodeNbrs.put("KW", "965");
        countryCodeNbrs.put("KG", "996");
        countryCodeNbrs.put("LA", "856");
        countryCodeNbrs.put("LV", "371");
        countryCodeNbrs.put("LB", "961");
        countryCodeNbrs.put("LS", "266");
        countryCodeNbrs.put("LR", "231");
        countryCodeNbrs.put("LY", "218");
        countryCodeNbrs.put("LI", "423");
        countryCodeNbrs.put("LT", "370");
        countryCodeNbrs.put("LU", "352");
        countryCodeNbrs.put("MO", "853");
        countryCodeNbrs.put("MK", "389");
        countryCodeNbrs.put("MG", "261");
        countryCodeNbrs.put("MW", "265");
        countryCodeNbrs.put("MY", "60");
        countryCodeNbrs.put("MV", "960");
        countryCodeNbrs.put("ML", "223");
        countryCodeNbrs.put("MT", "356");
        countryCodeNbrs.put("MH", "692");
        countryCodeNbrs.put("MR", "222");
        countryCodeNbrs.put("MU", "230");
        countryCodeNbrs.put("YT", "262");
        countryCodeNbrs.put("MX", "52");
        countryCodeNbrs.put("FM", "691");
        countryCodeNbrs.put("MD", "373");
        countryCodeNbrs.put("MC", "377");
        countryCodeNbrs.put("MN", "976");
        countryCodeNbrs.put("ME", "382");
        countryCodeNbrs.put("MA", "212");
        countryCodeNbrs.put("MZ", "258");
        countryCodeNbrs.put("NA", "264");
        countryCodeNbrs.put("NR", "674");
        countryCodeNbrs.put("NP", "977");
        countryCodeNbrs.put("NL", "31");
        countryCodeNbrs.put("AN", "599");
        countryCodeNbrs.put("NC", "687");
        countryCodeNbrs.put("NZ", "64");
        countryCodeNbrs.put("NI", "505");
        countryCodeNbrs.put("NE", "227");
        countryCodeNbrs.put("NG", "234");
        countryCodeNbrs.put("NU", "683");
        countryCodeNbrs.put("KP", "850");
        countryCodeNbrs.put("NO", "47");
        countryCodeNbrs.put("OM", "968");
        countryCodeNbrs.put("PK", "92");
        countryCodeNbrs.put("PW", "680");
        countryCodeNbrs.put("PA", "507");
        countryCodeNbrs.put("PG", "675");
        countryCodeNbrs.put("PY", "595");
        countryCodeNbrs.put("PE", "51");
        countryCodeNbrs.put("PH", "63");
        countryCodeNbrs.put("PN", "870");
        countryCodeNbrs.put("PL", "48");
        countryCodeNbrs.put("PT", "351");
        countryCodeNbrs.put("PR", "1");
        countryCodeNbrs.put("QA", "974");
        countryCodeNbrs.put("RO", "40");
        countryCodeNbrs.put("RU", "7");
        countryCodeNbrs.put("RW", "250");
        countryCodeNbrs.put("BL", "590");
        countryCodeNbrs.put("WS", "685");
        countryCodeNbrs.put("SM", "378");
        countryCodeNbrs.put("ST", "239");
        countryCodeNbrs.put("SA", "966");
        countryCodeNbrs.put("SN", "221");
        countryCodeNbrs.put("RS", "381");
        countryCodeNbrs.put("SC", "248");
        countryCodeNbrs.put("SL", "232");
        countryCodeNbrs.put("SG", "65");
        countryCodeNbrs.put("SK", "421");
        countryCodeNbrs.put("SI", "386");
        countryCodeNbrs.put("SB", "677");
        countryCodeNbrs.put("SO", "252");
        countryCodeNbrs.put("ZA", "27");
        countryCodeNbrs.put("KR", "82");
        countryCodeNbrs.put("ES", "34");
        countryCodeNbrs.put("LK", "94");
        countryCodeNbrs.put("SH", "290");
        countryCodeNbrs.put("PM", "508");
        countryCodeNbrs.put("SD", "249");
        countryCodeNbrs.put("SR", "597");
        countryCodeNbrs.put("SZ", "268");
        countryCodeNbrs.put("SE", "46");
        countryCodeNbrs.put("CH", "41");
        countryCodeNbrs.put("SY", "963");
        countryCodeNbrs.put("TW", "886");
        countryCodeNbrs.put("TJ", "992");
        countryCodeNbrs.put("TZ", "255");
        countryCodeNbrs.put("TH", "66");
        countryCodeNbrs.put("TG", "228");
        countryCodeNbrs.put("TK", "690");
        countryCodeNbrs.put("TO", "676");
        countryCodeNbrs.put("TN", "216");
        countryCodeNbrs.put("TR", "90");
        countryCodeNbrs.put("TM", "993");
        countryCodeNbrs.put("TV", "688");
        countryCodeNbrs.put("AE", "971");
        countryCodeNbrs.put("UG", "256");
        countryCodeNbrs.put("GB", "44");
        countryCodeNbrs.put("UA", "380");
        countryCodeNbrs.put("UY", "598");
        countryCodeNbrs.put("US", "1");
        countryCodeNbrs.put("UZ", "998");
        countryCodeNbrs.put("VU", "678");
        countryCodeNbrs.put("VA", "39");
        countryCodeNbrs.put("VE", "58");
        countryCodeNbrs.put("VN", "84");
        countryCodeNbrs.put("WF", "681");
        countryCodeNbrs.put("YE", "967");
        countryCodeNbrs.put("ZM", "260");
        countryCodeNbrs.put("ZW", "263");
    }

    @Override
    public void onAccountCreationDone(String username, String password) {
        // Actually useless here as they do a very crappy way to go back in the
        // application
        // Probably necessary for iPhone but absolutely useless in android as we
        // can inject
        // A javascript api to the webview so that user experience is better !
        setUsername(username);
        setPassword(password);
    }

    @Override
    public boolean saveAndQuit() {
        if (canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }
}
