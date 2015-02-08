/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * Copyright (C) 2015 Antonio Eugenio Burriel
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

package com.csipsimple.pjsip.reghandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.SparseIntArray;

import com.csipsimple.pjsip.PjSipService;
import com.csipsimple.utils.Log;

import org.pjsip.pjsua.MobileRegHandlerCallback;
import org.pjsip.pjsua.pj_str_t;
import org.pjsip.pjsua.pjsua;

public class RegHandlerCallback extends MobileRegHandlerCallback {
    private static final String THIS_FILE = "RegHandlerReceiver";
    private static final String REG_URI_PREFIX = "reg_uri_";
    private static final String REG_EXPIRES_PREFIX = "reg_expires_";
    private SharedPreferences prefs_db;
    
    private SparseIntArray accountCleanRegisters = new SparseIntArray();
    private pj_str_t EMPTY_STR = pjsua.pj_str_copy("");
    private Context mCtxt;
    
    public RegHandlerCallback(Context ctxt) {
        mCtxt = ctxt;
        prefs_db = ctxt.getSharedPreferences("reg_handler_db", Context.MODE_PRIVATE);
    }
    
    public void set_account_cleaning_state(int acc_id, int active) {
        accountCleanRegisters.put(acc_id, active);
    }
    
    @Override
    public pj_str_t on_restore_contact(int acc_id) {
        int active = accountCleanRegisters.get(acc_id, 0);
        if(active == 0) {
            return EMPTY_STR;
        }
        long db_acc_id = PjSipService.getAccountIdForPjsipId(mCtxt, acc_id);
        String key_expires = REG_EXPIRES_PREFIX + Long.toString(db_acc_id);
        String key_uri = REG_URI_PREFIX + Long.toString(db_acc_id);
        int expires = prefs_db.getInt(key_expires, 0);
        int now = (int) Math.ceil(System.currentTimeMillis() / 1000.0);
        if(expires >= now) {
            String ret = prefs_db.getString(key_uri, "");
            Log.d(THIS_FILE, "We restore " + ret);
            return pjsua.pj_str_copy(ret);
        }
        return EMPTY_STR;
    }
    
    @Override
    public void on_save_contact(int acc_id, pj_str_t contact, int expires) {
        long db_acc_id = PjSipService.getAccountIdForPjsipId(mCtxt, acc_id);
        String key_expires = REG_EXPIRES_PREFIX + Long.toString(db_acc_id);
        String key_uri = REG_URI_PREFIX + Long.toString(db_acc_id);
        Editor edt = prefs_db.edit();
        edt.putString(key_uri, PjSipService.pjStrToString(contact));
        int now = (int) Math.ceil(System.currentTimeMillis() / 1000.0);
        edt.putInt(key_expires, now + expires);
        // TODO : have this asynchronous
        edt.commit();
    }

}
