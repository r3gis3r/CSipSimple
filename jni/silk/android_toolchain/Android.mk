LOCAL_PATH := /
CALL_PATH := $(call my-dir)

# TODO : amr build with _AMR_ folder
SILK_PATH := $(CALL_PATH)/../sources/SILK_SDK_SRC_FIX_v1.0.8/
PJ_SILK_PATH := $(CALL_PATH)/../pj_sources/

ifeq ($(MY_USE_SILK),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_silk_codec

# pj
PJ_DIR = $(CALL_PATH)/../../pjsip/sources/
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include/ \
	$(PJ_DIR)/pjlib-util/include/ \
	$(PJ_DIR)/pjnath/include/ \
	$(PJ_DIR)/pjmedia/include/
# silk
LOCAL_C_INCLUDES += $(SILK_PATH)/interface
LOCAL_SRC_FILES += $(wildcard $(SILK_PATH)/src/*.c)
# self
LOCAL_C_INCLUDES += $(PJ_SILK_PATH)
LOCAL_SRC_FILES += $(PJ_SILK_PATH)/silk.c



LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)

endif