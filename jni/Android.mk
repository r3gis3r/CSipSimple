
LOCAL_PATH := $(call my-dir)/
JNI_PATH := $(LOCAL_PATH)

# Build pjsip for android
include $(JNI_PATH)/pjsip/android_toolchain/Android.mk

# Build webrtc
include $(JNI_PATH)/webrtc/android_toolchain/Android.mk

# Build amr-stagefright dynamic loader glue
include $(JNI_PATH)/amr-stagefright/android_toolchain/Android.mk

# Build silk
include $(JNI_PATH)/silk/android_toolchain/Android.mk

# Build g726
include $(JNI_PATH)/g726/android_toolchain/Android.mk

# Build g729
include $(JNI_PATH)/g729/android_toolchain/Android.mk

# Build codec2
include $(JNI_PATH)/codec2/android_toolchain/Android.mk

# Build zrtp
include $(JNI_PATH)/zrtp4pj/android_toolchain/Android.mk

# Build openssl
ifeq ($(MY_USE_TLS),1)
NDK_PROJECT_PATH := $(JNI_PATH)/openssl/sources/
include $(JNI_PATH)/openssl/sources/Android.mk
include $(JNI_PATH)/openssl/android_toolchain/Android.mk
NDK_PROJECT_PATH := $(JNI_PATH)
endif

# Build libyuv
include $(JNI_PATH)/third_party/android_toolchain/libyuv/Android.mk


# Build wrapper
include $(JNI_PATH)/swig-glue/android_toolchain/Android.mk

# Build the lib for csipsimple
include $(JNI_PATH)/csipsimple-wrapper/android_toolchain/Android.mk
