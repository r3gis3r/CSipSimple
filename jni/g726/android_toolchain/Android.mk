LOCAL_PATH := $(call my-dir)



ifeq ($(MY_USE_G726),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_g726_codec

G726_PATH := $(LOCAL_PATH)/../sources
PJ_G726_PATH := $(LOCAL_PATH)/../pj_sources

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# g726
LOCAL_C_INCLUDES += $(G726_PATH)/
G726_FILES := $(wildcard $(G726_PATH)/*.c)
LOCAL_SRC_FILES += $(G726_FILES:$(LOCAL_PATH)/%=%) 
# self
LOCAL_C_INCLUDES += $(PJ_G726_PATH)
LOCAL_SRC_FILES += ../pj_sources/pj_g726.c

LOCAL_SHARED_LIBRARIES += libpjsipjni

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

endif