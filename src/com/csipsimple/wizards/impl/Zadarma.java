/**
 * Copyright (C) 2011 Dmytro Tokar
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

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.os.Message;
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
import com.csipsimple.api.SipProfile;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.MD5;

public class Zadarma extends SimpleImplementation {


	protected static final String THIS_FILE = "ZadarmaW";
	protected static final int DID_SUCCEED = 0;
	protected static final int DID_ERROR = 1;
	
	private static final String webCreationPage = "https://ss.zadarma.com/android/registration/";

	private LinearLayout customWizard;
	private TextView customWizardText;
	private WebView webView;
	private LinearLayout settingsContainer;
	private LinearLayout validationBar;
	
	@Override
	protected String getDomain() {
		return "Zadarma.com";
	}
	
	@Override
	protected String getDefaultName() {
		return "Zadarma";
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
		initWebView();
	}
	



	private Handler creditHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case DID_SUCCEED: {
				//Here we get the credit info, now add a row in the interface
				String response = (String) message.obj;
				try{
					float value = Float.parseFloat(response.trim());
					if(value >= 0) {
						customWizardText.setText("Balance : " + Math.round(value * 100.0)/100.0 + " USD");
						customWizard.setVisibility(View.VISIBLE);
					}
				}catch(NumberFormatException e) {
					Log.e(THIS_FILE, "Impossible to parse result", e);
				}catch (NullPointerException e) {
					Log.e(THIS_FILE, "Null result");
				}
				
				break;
			}
			case DID_ERROR: {
				Exception e = (Exception) message.obj;
				e.printStackTrace();
				break;
			
			}
			}
		}
	};
	private ProgressBar loadingProgressBar;
	
	private void updateAccountInfos(final SipProfile acc) {
		if (acc != null && acc.id != SipProfile.INVALID_ID) {
			customWizard.setVisibility(View.GONE);
			Thread t = new Thread() {

				public void run() {
					try {
						HttpClient httpClient = new DefaultHttpClient();
						
						String requestURL = "https://ss.zadarma.com/android/getbalance/?"
							+ "login=" + acc.username
							+ "&code=" + MD5.MD5Hash(acc.data);
						
						HttpGet httpGet = new HttpGet(requestURL);

						// Create a response handler
						HttpResponse httpResponse = httpClient.execute(httpGet);
						if(httpResponse.getStatusLine().getStatusCode() == 200) {
							InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
							BufferedReader br = new BufferedReader(isr);
							String line = br.readLine();
							creditHandler.sendMessage(creditHandler.obtainMessage(DID_SUCCEED, line));
						}else {
							creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
						}
					} catch (Exception e) {
						creditHandler.sendMessage(creditHandler.obtainMessage(DID_ERROR));
					}
				}
			};
			t.start();
		} else {
			// add a row to link 
			customWizardText.setText("Create account");
			customWizard.setVisibility(View.VISIBLE);
			customWizard.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					settingsContainer.setVisibility(View.GONE);
					validationBar.setVisibility(View.GONE);
					webView.setVisibility(View.VISIBLE);
					webView.loadUrl(webCreationPage);
					webView.requestFocus(View.FOCUS_DOWN);
				}
			});
		}
	}
	
	
	
	@Override
	public boolean needRestart() {
		return true;
	}
	
	public SipProfile buildAccount(SipProfile account) {
		account = super.buildAccount(account);
		return account;
	}
	

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
				Log.d(THIS_FILE, "Progress changed to " + progress);
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
}
