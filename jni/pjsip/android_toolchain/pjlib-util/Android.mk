##############
# PJLIB-UTIL #
##############

LOCAL_PATH := $(call my-dir)/../../sources/pjlib-util
include $(CLEAR_VARS)

LOCAL_MODULE    := pjlib-util

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/include
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src/pjlib-util

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/base64.c \
	$(PJLIB_SRC_DIR)/crc32.c \
	$(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/dns.c \
	$(PJLIB_SRC_DIR)/dns_dump.c \
	$(PJLIB_SRC_DIR)/dns_server.c \
	$(PJLIB_SRC_DIR)/getopt.c \
	$(PJLIB_SRC_DIR)/hmac_md5.c \
	$(PJLIB_SRC_DIR)/hmac_sha1.c \
	$(PJLIB_SRC_DIR)/md5.c \
	$(PJLIB_SRC_DIR)/pcap.c \
	$(PJLIB_SRC_DIR)/resolver.c \
	$(PJLIB_SRC_DIR)/scanner.c \
	$(PJLIB_SRC_DIR)/sha1.c \
	$(PJLIB_SRC_DIR)/srv_resolver.c \
	$(PJLIB_SRC_DIR)/string.c \
	$(PJLIB_SRC_DIR)/stun_simple.c \
	$(PJLIB_SRC_DIR)/stun_simple_client.c \
	$(PJLIB_SRC_DIR)/xml.c

include $(BUILD_STATIC_LIBRARY)

