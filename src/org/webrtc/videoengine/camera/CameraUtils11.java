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

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.io.IOException;

@TargetApi(11)
public class CameraUtils11 extends CameraUtils9 {

    private SurfaceTexture dummySurfaceTexture = null;

    /*
     * (non-Javadoc)
     * @see
     * org.webrtc.videoengine.camera.CameraUtils5#setDummyTexture(android.hardware
     * .Camera)
     */
    @Override
    public void setDummyTexture(Camera camera) {
        try {
            if (dummySurfaceTexture == null) {
                dummySurfaceTexture = new SurfaceTexture(42);
            }
            camera.setPreviewTexture(dummySurfaceTexture);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
