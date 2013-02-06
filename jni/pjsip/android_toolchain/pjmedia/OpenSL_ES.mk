TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_PATH := $(TOOLCHAIN_PATH)/../../sources/pjmedia


# OpenSL-ES implementation

include $(CLEAR_VARS)

LOCAL_MODULE := pj_opensl_dev

PJ_ANDROID_SRC_DIR := ../../android_sources/pjmedia/src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/../pjlib-util/include \
	$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/include $(LOCAL_PATH)/..

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-audiodev


LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-audiodev/opensl_dev.c


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DPJMEDIA_AUDIO_DEV_HAS_OPENSL=1
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_LDLIBS += -lOpenSLES

LOCAL_STATIC_LIBRARIES += libgcc

include $(BUILD_SHARED_LIBRARY)