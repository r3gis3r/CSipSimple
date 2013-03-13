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

import android.annotation.TargetApi;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

@TargetApi(Build.VERSION_CODES.FROYO)
public class AccountCreationWebviewHelper8 extends AccountCreationWebviewHelper {

    private class CustomWebViewClient extends WebViewClient {
        private boolean bypassSSLErrors = false;
        private boolean bypassUrlChange = false;

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (bypassSSLErrors) {
                handler.proceed();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (bypassUrlChange) {
                view.loadUrl(url);
                return false;
            }
            return true;
        }

        /**
         * @param bypassUrlChange the bypassUrlChange to set
         */
        public void setBypassUrlChange(boolean bypassUrlChange) {
            this.bypassUrlChange = bypassUrlChange;
        }

        /**
         * @param bypassSSLErrors the bypassSSLErrors to set
         */
        public void setBypassSSLErrors(boolean bypassSSLErrors) {
            this.bypassSSLErrors = bypassSSLErrors;
        }
    }

    private CustomWebViewClient mWvc = null;

    private void initWebViewClientIfNeeded(WebView webView) {
        if (mWvc == null) {
            mWvc = new CustomWebViewClient();
            webView.setWebViewClient(mWvc);
        }
    }

    public void setSSLNoSecure(WebView webView) {
        initWebViewClientIfNeeded(webView);
        mWvc.setBypassSSLErrors(true);
    }

    public void setAllowRedirect(WebView webView) {
        initWebViewClientIfNeeded(webView);
        mWvc.setBypassUrlChange(true);
    }
}
