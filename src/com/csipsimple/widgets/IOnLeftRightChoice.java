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

package com.csipsimple.widgets;

import android.view.View;

/**
 * Interface definition for a callback to be invoked when a tab is triggered by
 * moving it beyond a target zone.
 */
public interface IOnLeftRightChoice {

    /**
     * The interface was triggered because the user grabbed the left handle and
     * moved it past the target zone.
     */
    int LEFT_HANDLE = 0;

    /**
     * The interface was triggered because the user grabbed the right handle and
     * moved it past the target zone.
     */
    int RIGHT_HANDLE = 1;

    /**
     * Called when the user moves a handle beyond the target zone.
     * 
     * @param v The view that was triggered.
     * @param whichHandle Which "dial handle" the user grabbed, either
     *            {@link #LEFT_HANDLE}, {@link #RIGHT_HANDLE}.
     */
    void onLeftRightChoice(int whichHandle);

    
    public interface IOnLeftRightProvider {
        void setOnLeftRightListener(IOnLeftRightChoice l);
    }
}
