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

package org.webrtc.videoengine.camera;

import android.hardware.Camera;

import org.webrtc.videoengine.VideoCaptureAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.AndroidVideoCaptureDevice;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CameraUtils3 extends CameraUtilsWrapper {

    @Override
    public void Init(VideoCaptureDeviceInfoAndroid deviceInfoAndroid,
            List<AndroidVideoCaptureDevice> listToPopulate) throws SecurityException,
            IllegalArgumentException, NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {
        // TODO Auto-generated method stub

    }

    @Override
    public Camera openCamera(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCallback(VideoCaptureAndroid captureAndroid, int numCaptureBuffers, int bufSize,
            Camera camera) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsetCallback(Camera camera) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addCallbackBuffer(Camera camera, byte[] data) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDisplayOrientation(Camera camera, int resultRotation) {
        // TODO Auto-generated method stub

    }

}
