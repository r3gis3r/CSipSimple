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

import org.webrtc.videoengine.CaptureCapabilityAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid;

import java.util.ArrayList;
import java.util.List;

public class VideoUtils5 extends VideoUtilsWrapper {
    @Override
    public List<VideoCaptureDeviceInfo> getVideoCaptureDevices(Context ctxt) {
        VideoCaptureDeviceInfoAndroid deviceInfoAndroid = VideoCaptureDeviceInfoAndroid.CreateVideoCaptureDeviceInfoAndroid(0, ctxt);
        List<VideoCaptureDeviceInfo> arr = new ArrayList<VideoCaptureDeviceInfo>();
        if(deviceInfoAndroid == null) {
            return arr;
        }
        int i;
        for(i = 0; i < deviceInfoAndroid.NumberOfDevices(); i++) {
            String deviceName = deviceInfoAndroid.GetDeviceUniqueName(i);
            CaptureCapabilityAndroid[] caps = deviceInfoAndroid.GetCapabilityArray(deviceName);
            VideoCaptureDeviceInfo vcdi = new VideoCaptureDeviceInfo();
            int orientation = deviceInfoAndroid.GetOrientation(deviceName);
            boolean invertWidthHeight = false;
            if(orientation == 90 || orientation == 270) {
                invertWidthHeight = true;
            }
            
            for(CaptureCapabilityAndroid cap : caps) {
                VideoCaptureCapability vcc = new VideoCaptureCapability();
                vcc.height = invertWidthHeight ? cap.width : cap.height;
                vcc.width = invertWidthHeight ? cap.height : cap.width;
                vcc.fps = cap.maxFPS;
                vcdi.capabilities.add(vcc);
            }
            
            CaptureCapabilityAndroid bcap = deviceInfoAndroid.GetBestCapability(deviceName);
            if(bcap != null) {
                vcdi.bestCapability = new VideoCaptureCapability();
                vcdi.bestCapability.width = invertWidthHeight ? bcap.width : bcap.width;
                vcdi.bestCapability.height = invertWidthHeight ? bcap.height : bcap.height;
                vcdi.bestCapability.fps = bcap.maxFPS;
            }
            
            arr.add(vcdi);
        }
        
        return arr;
    }

}
