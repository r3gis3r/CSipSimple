LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../sources/Android.mk

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_aac_codec


# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include \

# fdk
AAC_DIR := $(LOCAL_PATH)/../sources
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libAACdec/include \
        $(LOCAL_PATH)/libAACenc/include \
        $(LOCAL_PATH)/libPCMutils/include \
        $(LOCAL_PATH)/libFDK/include \
        $(LOCAL_PATH)/libSYS/include \
        $(LOCAL_PATH)/libMpegTPDec/include \
        $(LOCAL_PATH)/libMpegTPEnc/include \
        $(LOCAL_PATH)/libSBRdec/include \
        $(LOCAL_PATH)/libSBRenc/include

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pj_sources

LOCAL_SRC_FILES := ../pj_sources/pj_aac.c

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DPJMEDIA_HAS_AAC_CODEC=1

LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_STATIC_LIBRARIES += libFraunhoferAAC 
LOCAL_STATIC_LIBRARIES += libgcc

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)
