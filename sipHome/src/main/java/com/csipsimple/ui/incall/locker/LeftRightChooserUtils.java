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
package com.csipsimple.ui.incall.locker;

import android.content.Context;
import android.content.res.TypedArray;

import java.util.ArrayList;

/**
 * @author r3gis3r
 *
 */
public abstract class LeftRightChooserUtils implements IOnLeftRightChoice {

    public static ArrayList<String> loadTargetsDescriptions(Context ctxt, int resourceId) {
        TypedArray array = ctxt.getResources().obtainTypedArray(resourceId);
        final int count = array.length();
        ArrayList<String> targetContentDescriptions = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            String contentDescription = array.getString(i);
            targetContentDescriptions.add(contentDescription);
        }
        array.recycle();
        return targetContentDescriptions;
    }
}
