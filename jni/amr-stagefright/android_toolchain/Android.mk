LOCAL_PATH := $(call my-dir)
PJ_AMR_PATH := $(LOCAL_PATH)/../pj_sources/

ifeq ($(MY_USE_AMR),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_amr_stagefright_codec

#CODECS
#self
LOCAL_C_INCLUDES += ../pj_sources/


LOCAL_SRC_FILES := ../pj_sources/amr_stagefright_dyn_codec.c

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)

endif