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

import com.csipsimple.utils.Compatibility;

public abstract class KeyguardWrapper {

    public static KeyguardWrapper getKeyguardManager(Activity activity) {
        KeyguardWrapper kw;
        if(Compatibility.isCompatible(5)) {
            kw = new Keyguard5();
        } else {
            kw = new Keyguard3();
        }
        kw.initActivity(activity);
        return kw;
    }
    
    public abstract void initActivity(Activity activity);
    public abstract void lock();
    public abstract void unlock();
}
