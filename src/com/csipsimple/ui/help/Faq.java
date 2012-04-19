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

package com.csipsimple.ui.help;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.csipsimple.R;
import com.csipsimple.utils.CustomDistribution;

public class Faq extends SherlockDialogFragment {
	private final static String FAQ_URL = CustomDistribution.getFaqLink();

	public static Faq newInstance() {
        return new Faq();
    }
	
	


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setTitle(R.string.faq)
                .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dismiss();
                        }
                    }
                )
                .setView(getCustomView(getActivity().getLayoutInflater(), null, savedInstanceState))
                .create();
    }

    public View getCustomView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.faq, container, false);
	    WebView webView = (WebView) v.findViewById(R.id.webview);
	    webView.getSettings().setJavaScriptEnabled(true);
	    webView.setWebViewClient(new FaqWebViewClient(v));
	    webView.loadUrl(FAQ_URL);
    	return v;
    }
    
    
	private class FaqWebViewClient extends WebViewClient {
		private View parentView;
		
		public FaqWebViewClient(View v) {
			parentView = v;
		}

		@Override
		public void onPageFinished(final WebView view, String url) {
			super.onPageFinished(view, url);
			LinearLayout indicator = (LinearLayout) parentView.findViewById(R.id.loading_indicator);
			indicator.setVisibility(View.GONE);
			// Googlecode collapse side bar
			//view.loadUrl("javascript:$('wikisidebar').setAttribute('class', 'vt collapse');");
		}
		
	}
}
