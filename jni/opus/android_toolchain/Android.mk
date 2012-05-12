LOCAL_PATH := $(call my-dir)

ifeq ($(MY_USE_OPUS),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_opus_codec

PJ_OPUS_PATH := $(LOCAL_PATH)/../pj_sources/
OPUS_PATH := $(LOCAL_PATH)/../sources/

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources/
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include/ \
	$(PJ_DIR)/pjlib-util/include/ \
	$(PJ_DIR)/pjnath/include/ \
	$(PJ_DIR)/pjmedia/include/
# opus
LOCAL_C_INCLUDES += $(OPUS_PATH)/include $(OPUS_PATH)/celt $(OPUS_PATH)/silk

# we need to rebuild silk cause we don't know what are diff required for opus and may change in the future
include $(OPUS_PATH)/silk_sources.mk 
LOCAL_SRC_FILES += $(SILK_SOURCES:%=../sources/%)
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_C_INCLUDES += $(OPUS_PATH)/silk/fixed
LOCAL_SRC_FILES += $(SILK_SOURCES_FIXED:%=../sources/%)
else
LOCAL_C_INCLUDES += $(OPUS_PATH)/silk/float
LOCAL_SRC_FILES += $(SILK_SOURCES_FLOAT:%=../sources/%)
endif
include $(OPUS_PATH)/celt_sources.mk
LOCAL_SRC_FILES += $(CELT_SOURCES:%=../sources/%)
include $(OPUS_PATH)/opus_sources.mk
LOCAL_SRC_FILES += $(OPUS_SOURCES:%=../sources/%)
# self
LOCAL_C_INCLUDES += $(PJ_OPUS_PATH)
LOCAL_SRC_FILES += ../pj_sources/pj_opus.c

LOCAL_SHARED_LIBRARIES += libpjsipjni

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
# Hack to mute restrict not supported by ndk 
LOCAL_CFLAGS += -Drestrict=
ifeq ($(TARGET_ARCH_ABI),armeabi)
LOCAL_CFLAGS += -DFIXED_POINT
endif

ifeq ($(TARGET_ARCH_ABI),mips)
	LOCAL_STATIC_LIBRARIES += libgcc
endif

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

endif