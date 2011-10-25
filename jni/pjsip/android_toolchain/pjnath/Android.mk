##########
# PJNATH #
##########

LOCAL_PATH := $(call my-dir)/../../sources/pjnath/
include $(CLEAR_VARS)

LOCAL_MODULE    := pjnath

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pjlib/include/ $(LOCAL_PATH)/../pjlib-util/include/ $(LOCAL_PATH)/include/
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := ./src/pjnath

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/ice_session.c \
	$(PJLIB_SRC_DIR)/ice_strans.c \
	$(PJLIB_SRC_DIR)/nat_detect.c \
	$(PJLIB_SRC_DIR)/stun_auth.c \
	$(PJLIB_SRC_DIR)/stun_msg.c \
	$(PJLIB_SRC_DIR)/stun_msg_dump.c \
	$(PJLIB_SRC_DIR)/stun_session.c \
	$(PJLIB_SRC_DIR)/stun_sock.c \
	$(PJLIB_SRC_DIR)/stun_transaction.c \
	$(PJLIB_SRC_DIR)/turn_session.c \
	$(PJLIB_SRC_DIR)/turn_sock.c 


include $(BUILD_STATIC_LIBRARY)

