/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
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

public class CameraUtils5 extends CameraUtilsWrapper {

    @Override
    public void Init(VideoCaptureDeviceInfoAndroid deviceInfoAndroid,
            List<AndroidVideoCaptureDevice> listToPopulate) throws SecurityException,
            IllegalArgumentException, NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {

        Camera camera = null;
        // Prior to Android 2.3
        AndroidVideoCaptureDevice newDevice;
        Camera.Parameters parameters;

        newDevice = deviceInfoAndroid.new AndroidVideoCaptureDevice();
        camera = Camera.open();
        parameters = camera.getParameters();
        newDevice.deviceUniqueName = "Camera 1, Facing back";
        newDevice.orientation = 90;
        deviceInfoAndroid.AddDeviceInfo(newDevice, parameters);

        listToPopulate.add(newDevice);
        camera.release();
        camera = null;

        newDevice = deviceInfoAndroid.new AndroidVideoCaptureDevice();
        newDevice.deviceUniqueName = "Camera 2, Facing front";
        parameters = deviceInfoAndroid.SearchOldFrontFacingCameras(newDevice);
        if (parameters != null) {
            deviceInfoAndroid.AddDeviceInfo(newDevice, parameters);
            listToPopulate.add(newDevice);
        }
    }

    @Override
    public void setCallback(VideoCaptureAndroid captureAndroid, int numCaptureBuffers, int bufSize,
            Camera camera) {

        camera.setPreviewCallback(captureAndroid);
    }

    @Override
    public void unsetCallback(Camera camera) {
        camera.setPreviewCallback(null);
    }

    @Override
    public void addCallbackBuffer(Camera camera, byte[] data) {
        // Nothing to do in this case
    }

    @Override
    public void setDisplayOrientation(Camera camera, int resultRotation) {
        // Android 2.1 and previous
        // This rotation unfortunately does not seems to work.
        // http://code.google.com/p/android/issues/detail?id=1193
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(resultRotation);
        camera.setParameters(parameters);
    }

    @Override
    public Camera openCamera(int index) {
        return Camera.open();
    }

}
