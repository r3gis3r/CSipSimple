########
# G729 #
########

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/g7221/


include $(CLEAR_VARS)
LOCAL_MODULE 	:= g7221

LOCAL_C_INCLUDES += $(LOCAL_PATH)../../pjlib/include/ $(LOCAL_PATH)common/

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := ./

LOCAL_SRC_FILES :=  $(PJLIB_SRC_DIR)/common/common.c $(PJLIB_SRC_DIR)/common/huff_tab.c $(PJLIB_SRC_DIR)/common/tables.c \
                	$(PJLIB_SRC_DIR)/common/basic_op.c  \
                	$(PJLIB_SRC_DIR)/decode/coef2sam.c $(PJLIB_SRC_DIR)/decode/dct4_s.c $(PJLIB_SRC_DIR)/decode/decoder.c \
                	$(PJLIB_SRC_DIR)/encode/dct4_a.c $(PJLIB_SRC_DIR)/encode/sam2coef.c $(PJLIB_SRC_DIR)/encode/encoder.c


include $(BUILD_STATIC_LIBRARY)