
#######
# GSM #
#######

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/gsm

include $(CLEAR_VARS)
LOCAL_MODULE    := gsm

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../pjlib/include $(LOCAL_PATH)/inc \
			$(LOCAL_PATH)/../build/gsm

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/add.c $(PJLIB_SRC_DIR)/code.c $(PJLIB_SRC_DIR)/decode.c \
                	$(PJLIB_SRC_DIR)/gsm_create.c $(PJLIB_SRC_DIR)/gsm_decode.c $(PJLIB_SRC_DIR)/gsm_destroy.c \
                	$(PJLIB_SRC_DIR)/gsm_encode.c $(PJLIB_SRC_DIR)/gsm_explode.c $(PJLIB_SRC_DIR)/gsm_implode.c \
                	$(PJLIB_SRC_DIR)/gsm_option.c $(PJLIB_SRC_DIR)/long_term.c \
                	$(PJLIB_SRC_DIR)/lpc.c $(PJLIB_SRC_DIR)/preprocess.c $(PJLIB_SRC_DIR)/rpe.c $(PJLIB_SRC_DIR)/short_term.c \
                	$(PJLIB_SRC_DIR)/table.c

include $(BUILD_STATIC_LIBRARY)

