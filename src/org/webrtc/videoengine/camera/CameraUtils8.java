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

//@TargetApi(8)
public class CameraUtils8 extends CameraUtils5 {

    private boolean ownsBuffers = true;

    @Override
    public void setCallback(VideoCaptureAndroid captureAndroid, int numCaptureBuffers,
            int bufSize,
            Camera camera) {
        // According to Doc addCallbackBuffer belongs to API level 8.
        // But it seems like it works on Android 2.1 as well.
        // At least SE X10 and Milestone
        byte[] buffer = null;
        for (int i = 0; i < numCaptureBuffers; i++) {
            buffer = new byte[bufSize];
            camera.addCallbackBuffer(buffer);
        }

        camera.setPreviewCallbackWithBuffer(captureAndroid);

        ownsBuffers = true;
    }

    @Override
    public void unsetCallback(Camera camera) {
        camera.setPreviewCallbackWithBuffer(null);
    }

    @Override
    public void addCallbackBuffer(Camera camera, byte[] data) {
        if (ownsBuffers) {
            // Give the video buffer to the camera service again.
            camera.addCallbackBuffer(data);
        }
    }

    @Override
    public void setDisplayOrientation(Camera camera, int resultRotation) {
        camera.setDisplayOrientation(resultRotation);
    }
}
