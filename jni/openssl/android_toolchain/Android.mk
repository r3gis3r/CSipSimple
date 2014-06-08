LOCAL_PATH := $(call my-dir)/../sources

#######################################
# target crypto static library
include $(CLEAR_VARS)

include $(LOCAL_PATH)/Crypto-config-target.mk
include $(LOCAL_PATH)/android-config.mk

# Replace cflags with static-specific cflags so we dont build in libdl deps
LOCAL_CFLAGS_32 := $(openssl_cflags_static_32)
LOCAL_CFLAGS_64 := $(openssl_cflags_static_64)

LOCAL_MODULE := crypto_static
LOCAL_CFLAGS +=  -DOPENSSL_CPUID_OBJ

LOCAL_C_INCLUDES = $(common_c_includes:external/openssl/%=$(LOCAL_PATH)/%)  
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES_$(TARGET_ARCH))
LOCAL_SRC_FILES_$(TARGET_ARCH) := 

include $(BUILD_STATIC_LIBRARY)

## Additional target for ssl static
include $(CLEAR_VARS)
LOCAL_MODULE:= ssl_static
include $(LOCAL_PATH)/android-config.mk

ssl_c_includes := \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/crypto

ssl_src_files:= \
	s2_meth.c \
	s2_srvr.c \
	s2_clnt.c \
	s2_lib.c \
	s2_enc.c \
	s2_pkt.c \
	s3_meth.c \
	s3_srvr.c \
	s3_clnt.c \
	s3_lib.c \
	s3_enc.c \
	s3_pkt.c \
	s3_both.c \
	s3_cbc.c \
	s23_meth.c \
	s23_srvr.c \
	s23_clnt.c \
	s23_lib.c \
	s23_pkt.c \
	t1_meth.c \
	t1_srvr.c \
	t1_clnt.c \
	t1_lib.c \
	t1_enc.c \
	t1_reneg.c \
	d1_srtp.c \
	ssl_lib.c \
	ssl_err2.c \
	ssl_cert.c \
	ssl_sess.c \
	ssl_ciph.c \
	ssl_stat.c \
	ssl_rsa.c \
	ssl_asn1.c \
	ssl_txt.c \
	ssl_algs.c \
	bio_ssl.c \
	ssl_err.c \
	kssl.c \
	tls_srp.c

LOCAL_SRC_FILES += $(ssl_src_files:%=ssl/%)
LOCAL_C_INCLUDES += $(ssl_c_includes)
include $(BUILD_STATIC_LIBRARY)
