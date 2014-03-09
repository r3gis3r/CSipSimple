LOCAL_PATH := $(call my-dir)
version := v1.0.9


ifeq ($(MY_USE_SILK),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_silk_codec
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_ARM_MODE := arm
SILK_PATH := $(LOCAL_PATH)/../sources/SILK_SDK_SRC_ARM_$(version)
else
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_ARM_MODE := arm
SILK_PATH := $(LOCAL_PATH)/../sources/SILK_SDK_SRC_ARM_$(version)
else
ifeq ($(TARGET_ARCH_ABI),mips)
SILK_PATH := $(LOCAL_PATH)/../sources/SILK_SDK_SRC_MIPS_$(version)
else
ifeq ($(TARGET_ARCH_ABI),x86)
SILK_PATH := $(LOCAL_PATH)/../sources/SILK_SDK_SRC_FLP_$(version)
endif
endif
endif
endif


PJ_SILK_PATH := $(LOCAL_PATH)/../pj_sources/

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# silk
LOCAL_C_INCLUDES += $(SILK_PATH)/interface
SILK_FILES := $(wildcard $(SILK_PATH)/src/*.c)
ifeq ($(TARGET_ARCH_ABI),mips)
exclude := %SKP_Silk_warped_autocorrelation_FIX.c
SILK_FILES := $(filter-out $(exclude),$(SILK_FILES))
endif
SILK_FILES_S = $(wildcard $(SILK_PATH)/src/*.S)
LOCAL_SRC_FILES += $(SILK_FILES:$(LOCAL_PATH)/%=%)
LOCAL_SRC_FILES += $(SILK_FILES_S:$(LOCAL_PATH)/%=%)
# self
LOCAL_C_INCLUDES += $(PJ_SILK_PATH)
LOCAL_SRC_FILES += ../../pjsip/sources/pjmedia/src/pjmedia-codec/silk.c

LOCAL_SHARED_LIBRARIES += libpjsipjni

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DPJMEDIA_CODEC_SILK_DEFAULT_QUALITY=3 -DPJMEDIA_CODEC_SILK_DEFAULT_COMPLEXITY=1

LOCAL_STATIC_LIBRARIES += libgcc

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

endif