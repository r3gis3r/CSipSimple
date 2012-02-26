/**
 * Copyright (C) 2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * 
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.videoengine.camera;

//import android.annotation.TargetApi;
import android.hardware.Camera;

import org.webrtc.videoengine.VideoCaptureAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.AndroidVideoCaptureDevice;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

//@TargetApi(5)
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
