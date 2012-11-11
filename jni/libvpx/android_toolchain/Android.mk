LOCAL_PATH := $(call my-dir)
TOOLCHAIN_PATH := $(LOCAL_PATH)
VPX_SRC_PATH := $(TOOLCHAIN_PATH)/../sources
VPX_ALL_BUILDS_PATH := $(TOOLCHAIN_PATH)/../build

## The build process
include $(CLEAR_VARS)
LOCAL_PATH := $(VPX_SRC_PATH)
CONFIG_DIR := $(VPX_ALL_BUILDS_PATH)/$(TARGET_ARCH_ABI)
LIBVPX_PATH := $(VPX_SRC_PATH)
_local_mk := $(strip $(wildcard $(CONFIG_DIR)/config.mk))
_local_patched := $(strip $(wildcard $(TOOLCHAIN_PATH)/../.patched_sources))
ifdef _local_mk
ifdef _local_patched
include $(CONFIG_DIR)/config.mk
DIST_DIR := $(CONFIG_DIR)/out
include $(VPX_SRC_PATH)/build/make/Android.mk
endif
endif


## pjsip vpx
include $(CLEAR_VARS)


LOCAL_MODULE := pj_vpx
PJ_PATH := $(TOOLCHAIN_PATH)/../../pjsip/sources

LOCAL_C_INCLUDES := $(TOOLCHAIN_PATH)/../pj_sources \
	$(VPX_SRC_PATH) \
	$(PJ_PATH)/pjmedia/include \
	$(PJ_PATH)/pjlib/include \
	$(PJ_PATH)/pjlib-util/include \
	$(PJ_PATH)/pjsip/include \
	$(PJ_PATH)/pjnath/include 


# Pj implementation for renderer
LOCAL_SRC_FILES += ../pj_sources/pj_vpx.c

 
# Add ffmpeg to flags for pj part build
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) \
	-DPJMEDIA_HAS_VPX_CODEC=1
	
	
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_LDLIBS += -llog
LOCAL_STATIC_LIBRARIES += libvpx libgcc cpufeatures


include $(BUILD_SHARED_LIBRARY)