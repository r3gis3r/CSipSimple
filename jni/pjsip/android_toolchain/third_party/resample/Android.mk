
############
# RESAMPLE #
############

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/resample

include $(CLEAR_VARS)
LOCAL_MODULE    := resample

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../pjlib/include $(LOCAL_PATH)/include \
		   $(LOCAL_PATH)/../build/resample

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/resamplesubs.c


include $(BUILD_STATIC_LIBRARY)
