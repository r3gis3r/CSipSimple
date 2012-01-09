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

import com.csipsimple.utils.Compatibility;

import org.webrtc.videoengine.VideoCaptureAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid;
import org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.AndroidVideoCaptureDevice;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class CameraUtilsWrapper {
    private static CameraUtilsWrapper instance;

    public static CameraUtilsWrapper getInstance() {
        if (instance == null) {
            String className = "org.webrtc.videoengine.camera.CameraUtils";
            if (Compatibility.isCompatible(9)) {
                className += "9";
            } else if (Compatibility.isCompatible(8)) {
                className += "8";
            } else {
                className += "5";
            }
            try {
                Class<? extends CameraUtilsWrapper> wrappedClass = Class.forName(className)
                        .asSubclass(CameraUtilsWrapper.class);
                instance = wrappedClass.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        return instance;
    }

    protected CameraUtilsWrapper() {
        // By default nothing to do in constructor
    }

    /**
     * Init the camera list
     * @param deviceInfoAndroid the VideoCaptureDeviceInfoAndroid instance
     * @param listToPopulate List to be populated with detected devices
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public abstract void Init(VideoCaptureDeviceInfoAndroid deviceInfoAndroid,
            List<AndroidVideoCaptureDevice> listToPopulate) throws SecurityException,
            IllegalArgumentException, NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException;

    /**
     * Open camera at given index. Not that for old device index may be useless parameter
     * @param index the index of the camera device to open
     * @return the camera object
     */
    public abstract Camera openCamera(int index);
    
    /**
     * Set the callback for video camera. depending on android version it may use the VideoCaptureAndroid as preview call back or use some buffer callback
     * @param captureAndroid the VideoCaptureAndroid instance
     * @param numCaptureBuffers number of capture buffers avail
     * @param bufSize buffer size
     * @param camera camera to use
     */
    public abstract void setCallback(VideoCaptureAndroid captureAndroid, int numCaptureBuffers,
            int bufSize, Camera camera);
    
    /**
     * Unset the callback for video camera
     * @param camera the camera to unset callback from
     */
    public abstract void unsetCallback(Camera camera);

    /**
     * Add buffer to callback buffer if necessary
     * @param camera the camera
     * @param data the buffer byte array to fill with datas
     */
    public abstract void addCallbackBuffer(Camera camera, byte[] data);

    /**
     * Set the camera orientation for display
     * @param camera the camera
     * @param resultRotation the rotation to use
     */
    public abstract void setDisplayOrientation(Camera camera, int resultRotation);
}
