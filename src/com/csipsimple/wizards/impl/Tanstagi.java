/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
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
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.csipsimple.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.PreferencesWrapper;

public class Tanstagi extends SimpleImplementation {


	
	private static final String webCreationPage = "https://create.tanstagi.net/gork/new";

	private LinearLayout customWizard;
	private TextView customWizardText;
	//private WebView webView;
	private LinearLayout settingsContainer;
	private LinearLayout validationBar;
	
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

		//Get wizard specific row
		customWizardText = (TextView) parent.findViewById(R.id.custom_wizard_text);
		customWizard = (LinearLayout) parent.findViewById(R.id.custom_wizard_row);
		
		validationBar = (LinearLayout) parent.findViewById(R.id.validation_bar);
		
		updateAccountInfos(account);
		
		// add webview
		//initWebView();
	}
	


	private ProgressBar loadingProgressBar;
	
	@Override
	public void setDefaultParams(PreferencesWrapper prefs) {
		super.setDefaultParams(prefs);
		prefs.setPreferenceStringValue(SipConfigManager.USE_ZRTP, "2");
		prefs.setPreferenceBooleanValue(SipConfigManager.ENABLE_TLS, true);
	}
	

	@Override
	public boolean needRestart() {
		return true;
	}
	
	private void updateAccountInfos(final SipProfile acc) {
		if (acc != null && acc.id != SipProfile.INVALID_ID) {
			customWizard.setVisibility(View.GONE);
		} else {
			// add a row to link 
			customWizardText.setText(R.string.create_account);
			customWizard.setVisibility(View.VISIBLE);
			customWizard.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					/*
					settingsContainer.setVisibility(View.GONE);
					validationBar.setVisibility(View.GONE);
					webView.setVisibility(View.VISIBLE);
					webView.loadUrl(webCreationPage);
					webView.requestFocus(View.FOCUS_DOWN);
					*/
					parent.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webCreationPage)));
				}
			});
		}
	}
	
	
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		account.transport = SipProfile.TRANSPORT_TLS;
		
		return account;
	}
	
	
	
	
	/*
	private void initWebView() {

		webView = new WebView(parent);
		settingsContainer = (LinearLayout) parent.findViewById(R.id.settings_container);
		LinearLayout globalContainer = (LinearLayout) settingsContainer.getParent();
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lp.weight = 1;
		webView.setVisibility(View.GONE);
		globalContainer.addView(webView, 0, lp);
		
		loadingProgressBar = new ProgressBar(parent, null, android.R.attr.progressBarStyleHorizontal);
		lp = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 30);
		lp.gravity = 1;
		loadingProgressBar.setVisibility(View.GONE);
		loadingProgressBar.setIndeterminate(false);
		loadingProgressBar.setMax(100);
		globalContainer.addView(loadingProgressBar, 0, lp);
		
		
		// Setup webview 
		webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
		
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

	public class JSInterface {
		public void finishAccountCreation(boolean success, String userName, String password) {
			webView.setVisibility(View.GONE);
			settingsContainer.setVisibility(View.VISIBLE);
			validationBar.setVisibility(View.VISIBLE);
			if(success) {
				setUsername(userName);
				setPassword(password);
				parent.updateValidation();
			}
		}
	}
	*/
}
