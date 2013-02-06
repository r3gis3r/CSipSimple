
TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_PATH := $(TOOLCHAIN_PATH)/../../sources/pjmedia

# G7221 shared lib

ifeq ($(MY_USE_G7221),1)

include $(CLEAR_VARS)

PJMEDIACODEC_SRC_DIR := src/pjmedia-codec

LOCAL_MODULE := pj_g7221_codec
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/../pjlib-util/include/ \
	$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/include $(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../third_party


LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/g7221.c $(PJMEDIACODEC_SRC_DIR)/g7221_sdp_match.c 
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

LOCAL_STATIC_LIBRARIES += g7221
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_STATIC_LIBRARIES += libgcc

include $(BUILD_SHARED_LIBRARY)
endif