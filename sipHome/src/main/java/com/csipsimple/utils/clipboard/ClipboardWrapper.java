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

/**
 * 
 */
package com.csipsimple.utils.clipboard;

import android.content.Context;

import com.csipsimple.utils.Compatibility;

public abstract class ClipboardWrapper {

    private static ClipboardWrapper instance;
    
    public static ClipboardWrapper getInstance(Context context) {
        if(instance == null) {
            if(Compatibility.isCompatible(11)) {
                instance = new com.csipsimple.utils.clipboard.Clipboard11();
            }else {
                instance = new com.csipsimple.utils.clipboard.Clipboard1();
            }
            if(instance != null) {
                instance.setContext(context);
            }
        }
        
        return instance;
    }
    
    protected ClipboardWrapper() {}
    
    protected abstract void setContext(Context context);
    
    public abstract void setText(String description, String text);
}
