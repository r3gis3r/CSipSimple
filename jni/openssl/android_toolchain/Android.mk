LOCAL_PATH := $(call my-dir)/../sources

include $(LOCAL_PATH)/build-config-64.mk
include $(LOCAL_PATH)/build-config-32.mk


#######################################
# target crypto static library
include $(CLEAR_VARS)
LOCAL_MODULE := crypto_static

include $(LOCAL_PATH)/Crypto-config-target.mk
include $(LOCAL_PATH)/android-config.mk

# Replace cflags with static-specific cflags so we dont build in libdl deps
LOCAL_CFLAGS_32 := $(openssl_cflags_static_32)
LOCAL_CFLAGS_64 := $(openssl_cflags_static_64)

LOCAL_CFLAGS += $(openssl_cflags_static_32)
# From DEPFLAG=
LOCAL_CFLAGS += -DOPENSSL_NO_CMS  -DOPENSSL_NO_SRP
# -DOPENSSL_NO_ENGINE
# Extra
LOCAL_CFLAGS += -DOPENSSL_NO_HW -DZLIB

LOCAL_C_INCLUDES = $(common_c_includes:external/openssl/%=$(LOCAL_PATH)/%)

# Replace with our armcap that does not crash on android < 9
LOCAL_SRC_FILES_arm := $(filter-out crypto/armcap.c,$(LOCAL_SRC_FILES_arm) ../android_sources/armcap.c)

LOCAL_SRC_FILES := $(LOCAL_SRC_FILES_$(TARGET_ARCH))
# Remove disabled modules
 LOCAL_SRC_FILES := $(filter-out crypto/cms/% crypto/srp/%, $(LOCAL_SRC_FILES))
 # crypto/engine/%
 
 # remove transitional var that might be interpr by ndk
LOCAL_SRC_FILES_$(TARGET_ARCH) := 
LOCAL_SRC_FILES_x86_64 :=
LOCAL_SRC_FILES_arm :=
LOCAL_SRC_FILES_x86 :=
LOCAL_SRC_FILES_mips :=

# Add cflags for target (probably unecessary, but duplicate are not pb)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_$(TARGET_ARCH))

include $(BUILD_STATIC_LIBRARY)

## Additional target for ssl static
include $(CLEAR_VARS)
LOCAL_MODULE:= ssl_static

include $(LOCAL_PATH)/android-config.mk

# Replace cflags with static-specific cflags so we dont build in libdl deps
LOCAL_CFLAGS_32 := $(openssl_cflags_static_32)
LOCAL_CFLAGS_64 := $(openssl_cflags_static_64)

LOCAL_CFLAGS += $(openssl_cflags_static_32)
# From DEPFLAG=
LOCAL_CFLAGS += -DOPENSSL_NO_CMS  -DOPENSSL_NO_SRP
#-DOPENSSL_NO_ENGINE
# Extra
LOCAL_CFLAGS += -DOPENSSL_NO_HW -DZLIB

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
