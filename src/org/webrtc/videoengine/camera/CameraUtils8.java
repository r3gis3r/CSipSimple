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
