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

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.csipsimple.api.SipProfile;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class AccountBalanceHelper extends Handler {

    protected static final int DID_SUCCEED = 0;
    protected static final int DID_ERROR = 1;
    
    
    public void launchRequest(final SipProfile acc) {
        Thread t = new Thread() {

            public void run() {
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpRequestBase req = getRequest(acc);
                    if(req == null) {
                        return;
                    }
                    // Create a response handler
                    HttpResponse httpResponse = httpClient.execute(req);
                    if(httpResponse.getStatusLine().getStatusCode() == 200) {
                        InputStreamReader isr = new InputStreamReader(httpResponse.getEntity().getContent());
                        BufferedReader br = new BufferedReader(isr);

                        String line = null;
                        while( (line = br.readLine() ) != null ) {
                            String res = parseResponseLine(line);
                            if(!TextUtils.isEmpty(res)) {
                                AccountBalanceHelper.this.sendMessage(AccountBalanceHelper.this.obtainMessage(DID_SUCCEED, res));
                                break;
                            }
                        }
                        
                    }else {
                        AccountBalanceHelper.this.sendMessage(AccountBalanceHelper.this.obtainMessage(DID_ERROR));
                    }
                } catch (Exception e) {
                    AccountBalanceHelper.this.sendMessage(AccountBalanceHelper.this.obtainMessage(DID_ERROR));
                }
            }
        };
        t.start();
    }
    
    public void handleMessage(Message message) {
        switch (message.what) {
        case DID_SUCCEED: {
            //Here we get the credit info, now add a row in the interface
            String response = (String) message.obj;
            applyResultSuccess(response);
            break;
        }
        case DID_ERROR: {
            applyResultError();
            break;
        }
        }
    }
    
    /**
     * Build account balance request
     * @param acc the sip profile to build request for
     * @return
     */
    public abstract HttpRequestBase getRequest(SipProfile acc) throws IOException;
    /**
     * Search account result in the line.
     * @param line The line to parse
     * @return The account balance text if any parsed in this line. Else return empty or null chain to get next line
     */
    public abstract String parseResponseLine(String line);
    
    /**
     * Apply the error result of balance check
     * This is done in user interface thread so ui can be safely updated here
     */
    public abstract void applyResultError();

    /**
     * Apply the content result of balance check
     * This is done in user interface thread so ui can be safely updated here
     */
    public abstract void applyResultSuccess(String balanceText);
}
