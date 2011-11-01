LOCAL_PATH := /
CALL_PATH := $(call my-dir)

# TODO : amr build with _AMR_ folder
G729_PATH := $(CALL_PATH)/../sources/
PJ_G729_PATH := $(CALL_PATH)/../pj_sources/

ifeq ($(MY_USE_G729),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_g729_codec

# pj
PJ_DIR = $(CALL_PATH)/../../pjsip/sources/
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include/ \
	$(PJ_DIR)/pjlib-util/include/ \
	$(PJ_DIR)/pjnath/include/ \
	$(PJ_DIR)/pjmedia/include/
# silk
LOCAL_C_INCLUDES += $(G729_PATH)/include
LOCAL_SRC_FILES += $(wildcard $(G729_PATH)/src/*.c)
# self
LOCAL_C_INCLUDES += $(PJ_G729_PATH)
LOCAL_SRC_FILES += $(PJ_G729_PATH)/g729.c



LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)

endif