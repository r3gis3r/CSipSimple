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

import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.wizards.utils.AccountCreationFirstView;
import com.csipsimple.wizards.utils.AccountCreationFirstView.OnAccountCreationFirstViewListener;
import com.csipsimple.wizards.utils.AccountCreationWebview;
import com.csipsimple.wizards.utils.AccountCreationWebview.OnAccountCreationDoneListener;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class Tanstagi extends SimpleImplementation implements OnAccountCreationDoneListener, OnAccountCreationFirstViewListener {
	private static final String webCreationPage = "https://intimi.ca:4242/gork/new.pl";

	//private LinearLayout customWizard;
	//private TextView customWizardText;
    private AccountCreationWebview extAccCreator;

    private HostnameVerifier defaultVerifier;

    private SSLSocketFactory defaultSSLSocketFactory;

    private AccountCreationFirstView firstView;

    private ViewGroup settingsContainer;
    private ViewGroup validationBar;
	
	@Override
	protected String getDomain() {
		return "tanstagi.net";
	}
	
	@Override
	protected String getDefaultName() {
		return "tanstagi";
	}
	
	
	//Customization
	@Override
	public void fillLayout(final SipProfile account) {
		super.fillLayout(account);
		accountUsername.getEditText().setInputType(InputType.TYPE_CLASS_TEXT);

        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);
		
		updateAccountInfos(account);

        extAccCreator = new AccountCreationWebview(parent, webCreationPage, this);
        extAccCreator.setUntrustedCertificate();
	}
	


//	private ProgressBar loadingProgressBar;
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
	
	private void setFirstViewVisibility(boolean visible) {
        if(firstView != null) {
            firstView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        validationBar.setVisibility(visible ? View.GONE : View.VISIBLE);
        settingsContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
	}
	
	private void updateAccountInfos(final SipProfile acc) {
		if (acc != null && acc.id != SipProfile.INVALID_ID) {
		    setFirstViewVisibility(false);
		} else {
		    if(firstView == null) {
		        firstView = new AccountCreationFirstView(parent);
		        ViewGroup globalContainer = (ViewGroup) settingsContainer.getParent();
		        firstView.setOnAccountCreationFirstViewListener(this);
		        globalContainer.addView(firstView);
		    }
		    setFirstViewVisibility(true);
		}
	}
	
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_TLS;
		account.use_zrtp = 1;
		return account;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccountCreationDone(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAccountCreationDone(String username, String password, String extra) {
        onAccountCreationDone(username, password);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveAndQuit() {
        if (canSave()) {
            parent.saveAndFinish();
            return true;
        }
        return false;
    }
	
    @Override
    public void onStart() {
        super.onStart();
        trustEveryone();
    }
    
    public void onStop() {
        super.onStop();
        untrustEveryone();
    }
    
    private void trustEveryone() {
        // TODO : this should actually not be everyone -- but only the one we accept
        try {
            if(defaultVerifier == null) {
                defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
            }
            if(defaultSSLSocketFactory == null) {
                defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
            }
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                        }

                        public void checkServerTrusted(X509Certificate[] chain,
                                String authType) throws CertificateException {
                        }

                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }
    
    private void untrustEveryone() {
        if(defaultVerifier != null) {
            HttpsURLConnection.setDefaultHostnameVerifier(defaultVerifier);
        }
        if(defaultSSLSocketFactory != null) {
             HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSLSocketFactory);
        }
    }

    @Override
    public void onCreateAccountRequested() {
        setFirstViewVisibility(false);
        extAccCreator.show();
    }

    @Override
    public void onEditAccountRequested() {
        setFirstViewVisibility(false);
    }
}
