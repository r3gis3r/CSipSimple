LOCAL_PATH := $(call my-dir)/../sources/

### Local static library for missing EC on 2.2 devices ###
arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM
local_src_files := ec/ec2_mult.c \
	ec/ec2_smpl.c \
	ec/ec_ameth.c \
	ec/ec_asn1.c \
	ec/ec_check.c \
	ec/ec_curve.c \
	ec/ec_cvt.c \
	ec/ec_err.c \
	ec/ec_key.c \
	ec/ec_lib.c \
	ec/ec_mult.c \
	ec/ec_pmeth.c \
	ec/ec_print.c \
	ec/eck_prn.c \
	ec/ecp_mont.c \
	ec/ecp_nist.c \
	ec/ecp_smpl.c \
	ecdh/ech_err.c \
	ecdh/ech_key.c \
	ecdh/ech_lib.c \
	ecdh/ech_ossl.c 
	
	
local_c_includes := \
	$(LOCAL_PATH)/crypto \
	$(LOCAL_PATH)/crypto/asn1 \
	$(LOCAL_PATH)/crypto/evp \
	$(LOCAL_PATH)/ \
	$(LOCAL_PATH)/include


local_c_flags := -DNO_WINDOWS_BRAINDEATH


include $(CLEAR_VARS)
LOCAL_MODULE := crypto_ec_static
include $(LOCAL_PATH)/android-config.mk
LOCAL_SRC_FILES += $(local_src_files:%=crypto/%)
LOCAL_CFLAGS += $(local_c_flags)
LOCAL_C_INCLUDES += $(local_c_includes)
ifeq ($(TARGET_ARCH),arm)
	LOCAL_SRC_FILES += $(arm_src_files:%=crypto/%)
	LOCAL_CFLAGS += $(arm_cflags)
else
	LOCAL_SRC_FILES += $(non_arm_src_files:%=crypto/%)
endif

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
	kssl.c

LOCAL_SRC_FILES += $(ssl_src_files:%=ssl/%)
LOCAL_C_INCLUDES += $(ssl_c_includes)
LOCAL_SHARED_LIBRARIES += libcrypto
include $(BUILD_STATIC_LIBRARY)
