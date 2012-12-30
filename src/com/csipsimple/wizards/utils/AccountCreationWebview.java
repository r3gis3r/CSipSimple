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

package com.csipsimple.wizards.utils;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.csipsimple.R;
import com.csipsimple.wizards.BasePrefsWizard;

public class AccountCreationWebview {
    
    private final BasePrefsWizard parent;
    private OnAccountCreationDoneListener creationListener;
    private WebView webView;
    private ViewGroup settingsContainer;
    private ViewGroup validationBar;
    private ProgressBar loadingProgressBar;
    private String webCreationPage;
    
    public interface OnAccountCreationDoneListener {
        public void onAccountCreationDone(String username, String password);
        public boolean saveAndQuit();
    }
    
    public AccountCreationWebview(BasePrefsWizard aParent, String url, OnAccountCreationDoneListener l){
        parent = aParent;
        creationListener = l;
        webCreationPage = url;
        
        settingsContainer = (ViewGroup) parent.findViewById(R.id.settings_container);
        validationBar = (ViewGroup) parent.findViewById(R.id.validation_bar);
        
        ViewGroup globalContainer = (ViewGroup) settingsContainer.getParent();
        
        parent.getLayoutInflater().inflate(R.layout.wizard_account_creation_webview, globalContainer);
        
        webView = (WebView) globalContainer.findViewById(R.id.webview);
        loadingProgressBar = (ProgressBar) globalContainer.findViewById(R.id.webview_progress);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setCacheMode(WebSettings.LOAD_NORMAL);
        webSettings.setNeedInitialFocus(true);
        webView.addJavascriptInterface(new JSInterface(), "CSipSimpleWizard");
        
        // Adds Progress bar Support
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if(progress < 100) {
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    loadingProgressBar.setProgress(progress); 
                }else {
                    loadingProgressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    
    public void setUntrustedCertificate() {
        AccountCreationWebviewHelper.getInstance().setSSLNoSecure(webView);
    }

    public class JSInterface {
        /**
         * Allow webview to callback application about the fact account has been fully created.
         * @param success True if succeeded in account creation. 
         * If false is passed, username and password will be ignored, and the app will return the standard account wizard
         * @param userName the username to use for this user account
         * @param password the password to use for this user account
         */
        public void finishAccountCreation(boolean success, String userName, String password) {
            webView.setVisibility(View.GONE);
            settingsContainer.setVisibility(View.VISIBLE);
            validationBar.setVisibility(View.VISIBLE);
            if(success) {
                if(creationListener != null) {
                    creationListener.onAccountCreationDone(userName, password);
                }
                parent.updateValidation();
            }
        }
        
        /**
         * Allow webview to callback application about the fact account has been fully created.
         * In this variant, the account also automatically save without showing to user newly created user account infos
         * @param success True if succeeded in account creation. 
         * If false is passed, username and password will be ignored, and the app will return the standard account wizard
         * @param userName the username to use for this user account
         * @param password the password to use for this user account
         * @return true if the account was saved and quitted.
         */
        public boolean finishAccountCreationAndQuit(boolean success, String userName, String password) {
            webView.setVisibility(View.GONE);
            settingsContainer.setVisibility(View.VISIBLE);
            validationBar.setVisibility(View.VISIBLE);
            if(success) {
                if(creationListener != null) {
                    creationListener.onAccountCreationDone(userName, password);
                    return creationListener.saveAndQuit();
                }
                
            }
            return false;
        }
    }

    public void show() {
        settingsContainer.setVisibility(View.GONE);
        validationBar.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(webCreationPage);
        webView.requestFocus(View.FOCUS_DOWN);
    }
}
