
LOCAL_PATH := $(call my-dir)/
JNI_PATH := $(LOCAL_PATH)

# Build pjsip for android
include $(JNI_PATH)/pjsip/android_toolchain/Android.mk

# Build webrtc
include $(JNI_PATH)/webrtc/android_toolchain/Android.mk

# Build wrapper
include $(JNI_PATH)/swig-glue/android_toolchain/Android.mk

# Build the lib for csipsimple
include $(JNI_PATH)/csipsimple-wrapper/android_toolchain/Android.mk
