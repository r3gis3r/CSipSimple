
LOCAL_PATH := $(call my-dir)/
JNI_PATH := $(LOCAL_PATH)

# Include all submodules declarations
include $(JNI_PATH)/pjsip/android_toolchain/Android.mk
include $(JNI_PATH)/webrtc/android_toolchain/Android.mk
include $(JNI_PATH)/amr-stagefright/android_toolchain/Android.mk
include $(JNI_PATH)/silk/android_toolchain/Android.mk
include $(JNI_PATH)/g726/android_toolchain/Android.mk
include $(JNI_PATH)/g729/android_toolchain/Android.mk
include $(JNI_PATH)/codec2/android_toolchain/Android.mk
include $(JNI_PATH)/opus/android_toolchain/Android.mk
include $(JNI_PATH)/zrtp4pj/android_toolchain/Android.mk

ifeq ($(MY_USE_TLS),1)
NDK_PROJECT_PATH := $(JNI_PATH)/openssl/sources/
include $(JNI_PATH)/openssl/sources/Android.mk
include $(JNI_PATH)/openssl/android_toolchain/Android.mk
NDK_PROJECT_PATH := $(JNI_PATH)
endif

include $(JNI_PATH)/third_party/android_toolchain/libyuv/Android.mk
include $(JNI_PATH)/swig-glue/android_toolchain/Android.mk
include $(JNI_PATH)/csipsimple-wrapper/android_toolchain/Android.mk
