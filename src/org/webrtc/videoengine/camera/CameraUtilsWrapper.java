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
            if (Compatibility.isCompatible(9)) {
                instance = new org.webrtc.videoengine.camera.CameraUtils9();
            } else if (Compatibility.isCompatible(8)) {
                instance = new org.webrtc.videoengine.camera.CameraUtils8();
            } else if (Compatibility.isCompatible(5)){
                instance = new org.webrtc.videoengine.camera.CameraUtils5();
            }else {
                instance = new org.webrtc.videoengine.camera.CameraUtils3();
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
