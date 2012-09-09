#########
# ILBC  #
#########

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/ilbc

include $(CLEAR_VARS)
LOCAL_MODULE    := ilbc

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../pjlib/include/ $(LOCAL_PATH)

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := 

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/FrameClassify.c $(PJLIB_SRC_DIR)/LPCdecode.c $(PJLIB_SRC_DIR)/LPCencode.c \
		   $(PJLIB_SRC_DIR)/StateConstructW.c $(PJLIB_SRC_DIR)/StateSearchW.c $(PJLIB_SRC_DIR)/anaFilter.c \
		   $(PJLIB_SRC_DIR)/constants.c $(PJLIB_SRC_DIR)/createCB.c $(PJLIB_SRC_DIR)/doCPLC.c \
		   $(PJLIB_SRC_DIR)/enhancer.c $(PJLIB_SRC_DIR)/filter.c $(PJLIB_SRC_DIR)/gainquant.c \
		   $(PJLIB_SRC_DIR)/getCBvec.c $(PJLIB_SRC_DIR)/helpfun.c $(PJLIB_SRC_DIR)/hpInput.c \
		   $(PJLIB_SRC_DIR)/hpOutput.c $(PJLIB_SRC_DIR)/iCBConstruct.c $(PJLIB_SRC_DIR)/iCBSearch.c \
		   $(PJLIB_SRC_DIR)/iLBC_decode.c $(PJLIB_SRC_DIR)/iLBC_encode.c $(PJLIB_SRC_DIR)/lsf.c \
		   $(PJLIB_SRC_DIR)/packing.c $(PJLIB_SRC_DIR)/syntFilter.c
		   
include $(BUILD_STATIC_LIBRARY)