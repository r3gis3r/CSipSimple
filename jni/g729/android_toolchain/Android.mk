LOCAL_PATH := $(call my-dir)


ifeq ($(MY_USE_G729),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_g729_codec

G729_PATH := $(LOCAL_PATH)/../sources
PJ_G729_PATH := $(LOCAL_PATH)/../pj_sources


# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# g729
LOCAL_C_INCLUDES += $(G729_PATH)/include
G729_FILES := $(wildcard $(G729_PATH)/src/*.c)
LOCAL_SRC_FILES += $(G729_FILES:$(LOCAL_PATH)/%=%) 
# self
LOCAL_C_INCLUDES += $(PJ_G729_PATH)
LOCAL_SRC_FILES += ../pj_sources/pj_g729.c


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)


LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_STATIC_LIBRARIES += libgcc

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

endif