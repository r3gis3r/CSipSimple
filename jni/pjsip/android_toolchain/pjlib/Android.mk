#########
# PJLIB #
#########

LOCAL_PATH := $(call my-dir)/../../sources/pjlib

include $(CLEAR_VARS)
LOCAL_MODULE    := pjlib

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
ifeq ($(MY_USE_TLS),1)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../openssl/sources/include
endif

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src/pj
PJ_ANDROID_SRC_DIR := ../../android_sources/pjlib/src

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/addr_resolv_sock.c \
	$(PJLIB_SRC_DIR)/file_access_unistd.c \
	$(PJLIB_SRC_DIR)/file_io_ansi.c \
	$(PJLIB_SRC_DIR)/guid_simple.c \
	$(PJLIB_SRC_DIR)/log.c \
	$(PJLIB_SRC_DIR)/log_writer_stdout.c \
	$(PJLIB_SRC_DIR)/os_info.c \
	$(PJLIB_SRC_DIR)/os_core_unix.c \
	$(PJLIB_SRC_DIR)/os_error_unix.c \
	$(PJLIB_SRC_DIR)/os_time_common.c \
	$(PJLIB_SRC_DIR)/os_time_unix.c \
	$(PJLIB_SRC_DIR)/os_timestamp_common.c \
	$(PJLIB_SRC_DIR)/os_timestamp_posix.c \
	$(PJLIB_SRC_DIR)/pool_policy_malloc.c \
	$(PJLIB_SRC_DIR)/sock_common.c \
	$(PJLIB_SRC_DIR)/sock_qos_common.c \
	$(PJLIB_SRC_DIR)/sock_qos_bsd.c \
	$(PJLIB_SRC_DIR)/ssl_sock_common.c \
	$(PJLIB_SRC_DIR)/ssl_sock_ossl.c \
	$(PJLIB_SRC_DIR)/sock_bsd.c \
	$(PJLIB_SRC_DIR)/sock_select.c \
	$(PJLIB_SRC_DIR)/activesock.c \
	$(PJLIB_SRC_DIR)/array.c \
	$(PJLIB_SRC_DIR)/config.c \
	$(PJLIB_SRC_DIR)/ctype.c \
	$(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/except.c \
	$(PJLIB_SRC_DIR)/fifobuf.c \
	$(PJLIB_SRC_DIR)/guid.c \
	$(PJLIB_SRC_DIR)/hash.c \
	$(PJLIB_SRC_DIR)/ip_helper_generic.c \
	$(PJLIB_SRC_DIR)/list.c \
	$(PJLIB_SRC_DIR)/lock.c \
	$(PJLIB_SRC_DIR)/pool.c \
	$(PJLIB_SRC_DIR)/pool_buf.c \
	$(PJLIB_SRC_DIR)/pool_caching.c \
	$(PJLIB_SRC_DIR)/pool_dbg.c \
	$(PJLIB_SRC_DIR)/rbtree.c \
	$(PJLIB_SRC_DIR)/string.c \
	$(PJLIB_SRC_DIR)/rand.c \
	$(PJLIB_SRC_DIR)/types.c \
	$(PJLIB_SRC_DIR)/ioqueue_select.c

ifeq ($(MY_USE_CSIPSIMPLE),1)
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/timer_android.c
else
LOCAL_SRC_FILES += $(PJLIB_SRC_DIR)/timer.c
endif
include $(BUILD_STATIC_LIBRARY)

