LOCAL_PATH := $(call my-dir)



ifeq ($(MY_USE_SILK),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_silk_codec

SILK_PATH := $(LOCAL_PATH)/../sources/SILK_SDK_SRC_FIX_v1.0.8/
PJ_SILK_PATH := $(LOCAL_PATH)/../pj_sources/

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources/
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include/ \
	$(PJ_DIR)/pjlib-util/include/ \
	$(PJ_DIR)/pjnath/include/ \
	$(PJ_DIR)/pjmedia/include/
# silk
LOCAL_C_INCLUDES += $(SILK_PATH)/interface
SILK_FILES := $(wildcard $(SILK_PATH)/src/*.c)
LOCAL_SRC_FILES += $(SILK_FILES:$(LOCAL_PATH)/%=%)
# self
LOCAL_C_INCLUDES += $(PJ_SILK_PATH)
LOCAL_SRC_FILES += ../pj_sources/silk.c

LOCAL_SHARED_LIBRARIES += libpjsipjni

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

endif