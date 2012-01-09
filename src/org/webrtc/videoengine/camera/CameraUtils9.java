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
