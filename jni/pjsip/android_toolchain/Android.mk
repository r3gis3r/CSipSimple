LOCAL_PATH := $(call my-dir)

ifeq ($(USE_FIXED_POINT),1)
MY_PJSIP_FLAGS := $(BASE_PJSIP_FLAGS) -DPJ_HAS_FLOATING_POINT=0
else
MY_PJSIP_FLAGS := $(BASE_PJSIP_FLAGS) -DPJ_HAS_FLOATING_POINT=1
endif

# Build all sub dirs
include $(call all-subdir-makefiles)
include $(CLEAR_VARS)

