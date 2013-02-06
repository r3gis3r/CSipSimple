TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_PATH := $(TOOLCHAIN_PATH)/../../sources/pjmedia

ifeq ($(MY_USE_VIDEO),1)
## The screen capture backend

include $(CLEAR_VARS)

TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_MODULE := pj_screen_capture_android

PJ_ANDROID_SRC_DIR := ../../android_sources/pjmedia/src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/../pjlib-util/include \
	$(LOCAL_PATH)/../pjsip/include \
	$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/include $(LOCAL_PATH)/.. 

# We depends on csipsimple at this point because we need service to be stored somewhere
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-videodev

# Pj implementation for capture
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-videodev/android_screen_capture_dev.c

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
LOCAL_STATIC_LIBRARIES += libgcc

LOCAL_SHARED_LIBRARIES += libpjsipjni

include $(BUILD_SHARED_LIBRARY)

endif
