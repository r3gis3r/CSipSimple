/*
 *  Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.videoengine;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dalvik.system.DexClassLoader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import com.csipsimple.utils.Compatibility;

import org.webrtc.videoengine.camera.CameraUtilsWrapper;

@TargetApi(5)
@SuppressWarnings("deprecation")
public class VideoCaptureDeviceInfoAndroid {

    //Context
    Context context;

    // Set VERBOSE as the default logging level because camera device info
    // is very useful information and doesn't degrade performance normally
    private final static String TAG = "WEBRTC";

    // Private class with info about all available cameras and the capabilities
    public class AndroidVideoCaptureDevice {
        public AndroidVideoCaptureDevice() {
            frontCameraType = FrontFacingCameraType.None;
            index = 0;
        }

        public String deviceUniqueName;
        public CaptureCapabilityAndroid captureCapabilies[];
        public FrontFacingCameraType frontCameraType;

        // Orientation of camera as described in
        // android.hardware.Camera.CameraInfo.Orientation
        public int orientation;
        // Camera index used in Camera.Open on Android 2.3 and onwards
        public int index;
        public CaptureCapabilityAndroid bestCapability;
    }

    public enum FrontFacingCameraType {
        None, // This is not a front facing camera
                GalaxyS, // Galaxy S front facing camera.
                HTCEvo, // HTC Evo front facing camera
                Android23, // Android 2.3 front facing camera.
                }

    String currentDeviceUniqueId;
    int id;
    List<AndroidVideoCaptureDevice> deviceList;

    private CameraUtilsWrapper cameraUtils;

    public static VideoCaptureDeviceInfoAndroid
    CreateVideoCaptureDeviceInfoAndroid(int in_id, Context in_context) {
        Log.d(TAG,
                String.format(Locale.US, "VideoCaptureDeviceInfoAndroid"));

        VideoCaptureDeviceInfoAndroid self =
                new VideoCaptureDeviceInfoAndroid(in_id, in_context);
        if(self != null && self.Init() == 0) {
            return self;
        }
        else {
            Log.d(TAG, "Failed to create VideoCaptureDeviceInfoAndroid.");
        }
        return null;
    }

    private VideoCaptureDeviceInfoAndroid(int in_id,
            Context in_context) {
        id = in_id;
        context = in_context;
        deviceList = new ArrayList<AndroidVideoCaptureDevice>();
        cameraUtils = CameraUtilsWrapper.getInstance();
    }

    private int Init() {
        // Populate the deviceList with available cameras and their capabilities.
        try{
            cameraUtils.Init(this, deviceList);
        } 
        catch (Exception ex) {
            Log.e(TAG, "Failed to init VideoCaptureDeviceInfo ex" +
                    ex.getLocalizedMessage());
            return -1;
        }
        VerifyCapabilities();
        return 0;
    }

    // Adds the capture capabilities of the currently opened device
    public void AddDeviceInfo(AndroidVideoCaptureDevice newDevice,
            Camera.Parameters parameters) {

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
        int maxFPS = 0;
        if(sizes == null) {
            newDevice.captureCapabilies = new CaptureCapabilityAndroid[0];
            return;
        }
        if(frameRates != null) {
        for(Integer frameRate:frameRates) {
            if(frameRate > maxFPS) {
                maxFPS = frameRate;
            }
        }
        }else {
            maxFPS = 15;
        }

        newDevice.captureCapabilies = new CaptureCapabilityAndroid[sizes.size()];
        newDevice.bestCapability = new CaptureCapabilityAndroid();
        int bestBandwidth = 0;
        
        for(int i = 0; i < sizes.size(); ++i) {
            Size s = sizes.get(i);
            newDevice.captureCapabilies[i] = new CaptureCapabilityAndroid();
            newDevice.captureCapabilies[i].height = s.height;
            newDevice.captureCapabilies[i].width = s.width;
            newDevice.captureCapabilies[i].maxFPS = maxFPS;
            Log.v(TAG,
                    "VideoCaptureDeviceInfo " + "maxFPS:" + maxFPS +
                    " width:" + s.width + " height:" + s.height);
            
            // We use h264 primer formula here to estimate bandwidth need
            int currentBandwidth = (int) (s.width * s.height * maxFPS * 0.07);
            int maxBestBandwidth = 1000000;
            // We'd like to find a bandwidth < 1 Mbits
            if (bestBandwidth == 0 ||
                    (currentBandwidth < bestBandwidth && currentBandwidth >= maxBestBandwidth)) {
                newDevice.bestCapability.width = s.width;
                newDevice.bestCapability.height = s.height;
                newDevice.bestCapability.maxFPS = maxFPS;
                bestBandwidth = currentBandwidth;
            } else if (currentBandwidth < maxBestBandwidth) {
                if (s.width > newDevice.bestCapability.width ||
                        s.height > newDevice.bestCapability.height ||
                        bestBandwidth > maxBestBandwidth) {
                    if(s.height != s.width) {
                        newDevice.bestCapability.width = s.width;
                        newDevice.bestCapability.height = s.height;
                        newDevice.bestCapability.maxFPS = maxFPS;
                        bestBandwidth = currentBandwidth;
                    }
                }

            }
        }
        
        Log.d(TAG, "Best capability found " + newDevice.bestCapability.width + " x "
                + newDevice.bestCapability.height);
    }

    // Function that make sure device specific capabilities are
    // in the capability list.
    // Ie Galaxy S supports CIF but does not list CIF as a supported capability.
    // Motorola Droid Camera does not work with frame rate above 15fps.
    // http://code.google.com/p/android/issues/detail?id=5514#c0
    private void VerifyCapabilities() {
        // Nexus S or Galaxy S
        if(android.os.Build.DEVICE.equals("GT-I9000") ||
                android.os.Build.DEVICE.equals("crespo")) {
            CaptureCapabilityAndroid specificCapability =
                    new CaptureCapabilityAndroid();
            specificCapability.width = 352;
            specificCapability.height = 288;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapabilityAndroid();
            specificCapability.width = 176;
            specificCapability.height = 144;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapabilityAndroid();
            specificCapability.width = 320;
            specificCapability.height = 240;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);
        }
        // Motorola Milestone Camera server does not work at 30fps
        // even though it reports that it can
        if(android.os.Build.MANUFACTURER.equals("motorola") &&
                android.os.Build.DEVICE.equals("umts_sholes")) {
            for(AndroidVideoCaptureDevice device:deviceList) {
                for(CaptureCapabilityAndroid capability:device.captureCapabilies) {
                    capability.maxFPS=15;
                }
            }
        }
    }

    private void AddDeviceSpecificCapability(
        CaptureCapabilityAndroid specificCapability) {
        for(AndroidVideoCaptureDevice device:deviceList) {
            boolean foundCapability = false;
            for(CaptureCapabilityAndroid capability:device.captureCapabilies) {
                if(capability.width == specificCapability.width &&
                        capability.height == specificCapability.height) {
                    foundCapability = true;
                    break;
                }
            }
            
            // 3R : galaxy S CIF only on rear
            if (android.os.Build.DEVICE.equals("GT-I9000") &&
                    (device.frontCameraType == FrontFacingCameraType.GalaxyS ||
                    device.frontCameraType == FrontFacingCameraType.Android23)
                    && specificCapability.width == 352 && specificCapability.height == 288) {
                // Simulate found
                foundCapability = true;
            }
            
            if(foundCapability==false) {
                CaptureCapabilityAndroid newCaptureCapabilies[]=
                        new CaptureCapabilityAndroid[device.captureCapabilies.length+1];
                for(int i = 0; i < device.captureCapabilies.length; ++i) {
                    newCaptureCapabilies[i+1] = device.captureCapabilies[i];
                }
                newCaptureCapabilies[0] = specificCapability;
                device.captureCapabilies = newCaptureCapabilies;
            }
        }
    }

    // Returns the number of Capture devices that is supported
    public int NumberOfDevices() {
        return deviceList.size();
    }

    public String GetDeviceUniqueName(int deviceNumber) {
        if(deviceNumber < 0 || deviceNumber >= deviceList.size()) {
            return null;
        }
        return deviceList.get(deviceNumber).deviceUniqueName;
    }

    public CaptureCapabilityAndroid[] GetCapabilityArray (String deviceUniqueId)
    {
        for (AndroidVideoCaptureDevice device: deviceList) {
            if(device.deviceUniqueName.equals(deviceUniqueId)) {
                return (CaptureCapabilityAndroid[]) device.captureCapabilies;
            }
        }
        return null;
    }
    
    public CaptureCapabilityAndroid GetBestCapability (String deviceUniqueId)
    {
        for (AndroidVideoCaptureDevice device: deviceList) {
            if(device.deviceUniqueName.equals(deviceUniqueId)) {
                return (CaptureCapabilityAndroid) device.bestCapability;
            }
        }
        return null;
        
    }

    // Returns the camera orientation as described by
    // android.hardware.Camera.CameraInfo.orientation
    public int GetOrientation(String deviceUniqueId) {
        for (AndroidVideoCaptureDevice device: deviceList) {
            if(device.deviceUniqueName.equals(deviceUniqueId)) {
                return device.orientation;
            }
        }
        return -1;
    }

    // Returns an instance of VideoCaptureAndroid.
    public VideoCaptureAndroid AllocateCamera(int id, long context,
            String deviceUniqueId) {
        try {
            Log.d(TAG, "AllocateCamera " + deviceUniqueId);

            Camera camera = null;
            AndroidVideoCaptureDevice deviceToUse = null;
            for (AndroidVideoCaptureDevice device: deviceList) {
                if(device.deviceUniqueName.equals(deviceUniqueId)) {
                    // Found the wanted camera
                    deviceToUse = device;
                    switch(device.frontCameraType) {
                        case GalaxyS:
                            camera = AllocateGalaxySFrontCamera();
                            break;
                        case HTCEvo:
                            camera = AllocateEVOFrontFacingCamera();
                            break;
                        default:
                            camera = cameraUtils.openCamera(device.index);
                    }
                }
            }

            if(camera == null) {
                return null;
            }
            Log.v(TAG, "AllocateCamera - creating VideoCaptureAndroid");

            return new VideoCaptureAndroid(id, context, camera, deviceToUse);

        }catch (Exception ex) {
            Log.e(TAG, "AllocateCamera Failed to open camera- ex " +
                    ex.getLocalizedMessage());
        }
        return null;
    }

    // Searches for a front facing camera device. This is device specific code.
    public Camera.Parameters
    SearchOldFrontFacingCameras(AndroidVideoCaptureDevice newDevice)
            throws SecurityException, IllegalArgumentException,
            NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {
        // Check the id of the opened camera device
        // Returns null on X10 and 1 on Samsung Galaxy S.
        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        String cameraId = parameters.get("camera-id");
        if(cameraId != null && cameraId.equals("1")) {
            // This might be a Samsung Galaxy S with a front facing camera.
            try {
                parameters.set("camera-id", 2);
                camera.setParameters(parameters);
                parameters = camera.getParameters();
                newDevice.frontCameraType = FrontFacingCameraType.GalaxyS;
                newDevice.orientation = 0;
                camera.release();
                return parameters;
            }
            catch (Exception ex) {
                // Nope - it did not work.
                Log.e(TAG, "Init Failed to open front camera camera - ex " +
                        ex.getLocalizedMessage());
            }
        }
        camera.release();

        // Check for Evo front facing camera
        File file =
                new File("/system/framework/com.htc.hardware.twinCamDevice.jar");
        boolean exists = file.exists();
        if (!exists) {
            file =
                    new File("/system/framework/com.sprint.hardware.twinCamDevice.jar");
            exists = file.exists();
        }
        if(exists) {
            newDevice.frontCameraType = FrontFacingCameraType.HTCEvo;
            newDevice.orientation = 0;
            Camera evCamera = AllocateEVOFrontFacingCamera();
            parameters = evCamera.getParameters();
            evCamera.release();
            return parameters;
        }
        return null;
    }

    // Returns a handle to HTC front facing camera.
    // The caller is responsible to release it on completion.
    private Camera AllocateEVOFrontFacingCamera()
            throws SecurityException, NoSuchMethodException,
            ClassNotFoundException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        String classPath = null;
        File file =
                new File("/system/framework/com.htc.hardware.twinCamDevice.jar");
        classPath = "com.htc.hardware.twinCamDevice.FrontFacingCamera";
        boolean exists = file.exists();
        if (!exists){
            file =
                    new File("/system/framework/com.sprint.hardware.twinCamDevice.jar");
            classPath = "com.sprint.hardware.twinCamDevice.FrontFacingCamera";
            exists = file.exists();
        }
        if(!exists) {
            return null;
        }

        String dexOutputDir = "";
        if(context != null) {
            dexOutputDir = context.getFilesDir().getAbsolutePath();
            File mFilesDir = new File(dexOutputDir, "dexfiles");
            if(!mFilesDir.exists()){
                // Log.e("*WEBRTCN*", "Directory doesn't exists");
                if(!mFilesDir.mkdirs()) {
                    // Log.e("*WEBRTCN*", "Unable to create files directory");
                }
            }
        }

        dexOutputDir += "/dexfiles";

        DexClassLoader loader =
                new DexClassLoader(file.getAbsolutePath(), dexOutputDir,
                        null, ClassLoader.getSystemClassLoader());

        Method method = ((ClassLoader)loader).loadClass(classPath).getDeclaredMethod(
            "getFrontFacingCamera", (Class[]) null);
        Camera camera = (Camera) method.invoke((Object[])null,(Object[]) null);
        return camera;
    }

    // Returns a handle to Galaxy S front camera.
    // The caller is responsible to release it on completion.
    private Camera AllocateGalaxySFrontCamera() {
        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("camera-id",2);
        camera.setParameters(parameters);
        return camera;
    }

}
