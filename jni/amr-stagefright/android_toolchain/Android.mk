LOCAL_PATH := $(call my-dir)
PJ_AMR_PATH := $(LOCAL_PATH)/../pj_sources

ifeq ($(MY_USE_AMR),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_amr_stagefright_codec

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# self
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pj_sources $(LOCAL_PATH)/..


LOCAL_SRC_FILES := ../pj_sources/stagefright_amr.c 

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)


### Fake opencore ###
include $(CLEAR_VARS)
LOCAL_MODULE := android_dyn_opencore

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# self
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pj_sources $(LOCAL_PATH)/..


LOCAL_SRC_FILES :=../pj_sources/stagefright_amrnb_dynloader.c \
										../pj_sources/stagefright_amrwb_dynloader.c \
										../pj_sources/cmnMemory.c \
										../pj_sources/homing_amr_wb_dec.c

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)
endif