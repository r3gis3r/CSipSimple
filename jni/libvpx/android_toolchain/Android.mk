LOCAL_PATH := $(call my-dir)
TOOLCHAIN_PATH := $(LOCAL_PATH)
VPX_SRC_PATH := $(TOOLCHAIN_PATH)/../sources
VPX_ALL_BUILDS_PATH := $(TOOLCHAIN_PATH)/../build

## The build process
include $(CLEAR_VARS)
LOCAL_PATH := $(VPX_SRC_PATH)
CONFIG_DIR := $(VPX_ALL_BUILDS_PATH)/$(TARGET_ARCH_ABI)
LIBVPX_PATH := $(LOCAL_PATH)/../sources
include $(CONFIG_DIR)/config.mk
DIST_DIR := $(CONFIG_DIR)/out
include $(VPX_SRC_PATH)/build/make/Android.mk

