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
package com.csipsimple.ui.help;

import com.csipsimple.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

public class Legal extends Activity {

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.legal_information);
		
		//Set window size
		LayoutParams params = getWindow().getAttributes();
		params.width = LayoutParams.FILL_PARENT;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
		
		//Set title
		((TextView) findViewById(R.id.my_title)).setText(R.string.legal_information);
		((ImageView) findViewById(R.id.my_icon)).setImageResource(android.R.drawable.ic_menu_gallery);
		
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.loadUrl("file:///android_asset/legal.html"); 
	}
}
