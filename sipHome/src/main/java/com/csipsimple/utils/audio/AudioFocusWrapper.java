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

package com.csipsimple.utils.audio;

import android.media.AudioManager;

import com.csipsimple.service.SipService;
import com.csipsimple.utils.Compatibility;

public abstract class AudioFocusWrapper {
    private static AudioFocusWrapper instance;

    public static AudioFocusWrapper getInstance() {
        if (instance == null) {
            if (Compatibility.isCompatible(17)) {
                instance = new com.csipsimple.utils.audio.AudioFocus17();
            } else if (Compatibility.isCompatible(8)) {
                instance = new com.csipsimple.utils.audio.AudioFocus8();
            } else {
                instance = new com.csipsimple.utils.audio.AudioFocus3();
            }
        }

        return instance;
    }

    protected AudioFocusWrapper() {
    }

    public abstract void init(SipService service, AudioManager manager);

    public abstract void focus(boolean userWantsBluetooth);

    public abstract void unFocus();

    public abstract String getProperty(String property);

}
