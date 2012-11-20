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

package com.csipsimple.utils.video;

import android.content.Context;
import android.text.TextUtils;

import com.csipsimple.utils.Compatibility;
import com.csipsimple.utils.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class VideoUtilsWrapper {
    private static VideoUtilsWrapper instance;

    private static final String THIS_FILE = "VideoUtilsWrapper";

    public class VideoCaptureDeviceInfo {
        public List<VideoCaptureCapability> capabilities;
        public VideoCaptureCapability bestCapability;

        public VideoCaptureDeviceInfo() {
            capabilities = new ArrayList<VideoCaptureCapability>();
        }
    }

    public static class VideoCaptureCapability {
        public int width;
        public int height;
        public int fps;

        public VideoCaptureCapability() {

        }

        public VideoCaptureCapability(String preferenceValue) {
            if (!TextUtils.isEmpty(preferenceValue)) {
                String[] size_fps = preferenceValue.split("@");
                if (size_fps.length == 2) {
                    String[] width_height = size_fps[0].split("x");
                    if (width_height.length == 2) {
                        try {
                            width = Integer.parseInt(width_height[0]);
                            height = Integer.parseInt(width_height[1]);
                            fps = Integer.parseInt(size_fps[1]);
                        } catch (NumberFormatException e) {
                            Log.e(THIS_FILE, "Cannot parse the preference for video capture cap");
                        }
                    }
                }
            }
        }

        public String toPreferenceValue() {
            return (width + "x" + height + "@" + fps);
        }

        public String toPreferenceDisplay() {
            return (width + " x " + height + " @" + fps + "fps");
        }
    }

    public static VideoUtilsWrapper getInstance() {
        if (instance == null) {
            if (Compatibility.isCompatible(5)) {
                instance = new com.csipsimple.utils.video.VideoUtils5();
            } else {
                instance = new com.csipsimple.utils.video.VideoUtils3();
            }
        }

        return instance;
    }

    protected VideoUtilsWrapper() {
        // By default nothing to do in constructor
    }

    public abstract List<VideoCaptureDeviceInfo> getVideoCaptureDevices(Context ctxt);

}
