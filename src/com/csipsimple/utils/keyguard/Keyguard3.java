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

package com.csipsimple.utils.keyguard;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;

@SuppressWarnings("deprecation")
public class Keyguard3 extends KeyguardWrapper {
    
    private Context context;
    
    // Keygard for incoming call
    private boolean manageKeyguard = false;
    private KeyguardManager keyguardManager;
    private KeyguardManager.KeyguardLock keyguardLock;
    

    @Override
    public void initActivity(Activity activity) {
        context = activity;
        keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("com.csipsimple.inCallKeyguard");
    }
    
    @Override
    public void lock() {
        if (manageKeyguard) {
            keyguardLock.reenableKeyguard();
        }
    }

    @Override
    public void unlock() {
        manageKeyguard = true;
        keyguardLock.disableKeyguard();
    }

    
}
