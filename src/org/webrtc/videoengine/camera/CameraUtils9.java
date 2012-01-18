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

import android.hardware.Camera;

import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.AndroidVideoCaptureDevice;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.FrontFacingCameraType;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CameraUtils9 extends CameraUtils8 {
    @Override
    public void Init(VideoCaptureDeviceInfoAndroid deviceInfoAndroid,
            List<AndroidVideoCaptureDevice> listToPopulate)
            throws SecurityException,
            IllegalArgumentException, NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {

        Camera camera = null;

        // From Android 2.3 and onwards
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            AndroidVideoCaptureDevice newDevice = deviceInfoAndroid.new AndroidVideoCaptureDevice();

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            newDevice.index = i;
            newDevice.orientation = info.orientation;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                newDevice.deviceUniqueName =
                        "Camera " + i + ", Facing back, Orientation " + info.orientation;
            }
            else {
                newDevice.deviceUniqueName =
                        "Camera " + i + ", Facing front, Orientation " + info.orientation;
                newDevice.frontCameraType = FrontFacingCameraType.Android23;
            }

            camera = Camera.open(i);
            Camera.Parameters parameters = camera.getParameters();
            deviceInfoAndroid.AddDeviceInfo(newDevice, parameters);
            camera.release();
            camera = null;
            listToPopulate.add(newDevice);
        }
    }
    
    @Override
    public Camera openCamera(int index) {
        return Camera.open(index);
    }
}
