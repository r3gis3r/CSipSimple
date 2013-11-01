
########
# SRTP #
########

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/srtp

include $(CLEAR_VARS)
LOCAL_MODULE    := srtp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../pjlib/include \
			$(LOCAL_PATH)/crypto/include \
			$(LOCAL_PATH)/include \
			$(LOCAL_PATH)/../build/srtp

			

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := .

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/crypto/cipher/cipher.c $(PJLIB_SRC_DIR)/crypto/cipher/null_cipher.c      \
		$(PJLIB_SRC_DIR)/crypto/cipher/aes.c $(PJLIB_SRC_DIR)/crypto/cipher/aes_icm.c             \
		$(PJLIB_SRC_DIR)/crypto/cipher/aes_cbc.c \
		$(PJLIB_SRC_DIR)/crypto/hash/null_auth.c $(PJLIB_SRC_DIR)/crypto/hash/sha1.c \
        $(PJLIB_SRC_DIR)/crypto/hash/hmac.c $(PJLIB_SRC_DIR)/crypto/hash/auth.c \
		$(PJLIB_SRC_DIR)/crypto/replay/rdb.c $(PJLIB_SRC_DIR)/crypto/replay/rdbx.c               \
		$(PJLIB_SRC_DIR)/crypto/replay/ut_sim.c \
		$(PJLIB_SRC_DIR)/crypto/math/datatypes.c $(PJLIB_SRC_DIR)/crypto/math/stat.c \
		$(PJLIB_SRC_DIR)/crypto/rng/rand_source.c $(PJLIB_SRC_DIR)/crypto/rng/prng.c \
		$(PJLIB_SRC_DIR)/crypto/rng/ctr_prng.c \
		$(PJLIB_SRC_DIR)/pjlib/srtp_err.c \
		$(PJLIB_SRC_DIR)/crypto/kernel/crypto_kernel.c  $(PJLIB_SRC_DIR)/crypto/kernel/alloc.c   \
		$(PJLIB_SRC_DIR)/crypto/kernel/key.c \
		$(PJLIB_SRC_DIR)/srtp/srtp.c 


include $(BUILD_STATIC_LIBRARY)
