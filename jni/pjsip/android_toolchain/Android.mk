LOCAL_PATH := $(call my-dir)


# Add target arm version
ifeq ($(TARGET_ARCH_ABI),armeabi)
MY_PJSIP_FLAGS := $(BASE_PJSIP_FLAGS) -DPJ_HAS_FLOATING_POINT=0
else
MY_PJSIP_FLAGS := $(BASE_PJSIP_FLAGS) -DPJ_HAS_FLOATING_POINT=1
endif

# Build all sub dirs
include $(call all-subdir-makefiles)
include $(CLEAR_VARS)

