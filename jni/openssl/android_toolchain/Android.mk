LOCAL_PATH := $(call my-dir)/../sources/crypto/

arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM

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
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/asn1 \
	$(LOCAL_PATH)/evp \
	$(LOCAL_PATH)/../ \
	$(LOCAL_PATH)/../include


local_c_flags := -DNO_WINDOWS_BRAINDEATH

include $(CLEAR_VARS)
LOCAL_MODULE := crypto_ec_static


include $(LOCAL_PATH)/../android-config.mk
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += $(local_c_flags)
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_LDLIBS += -lz
ifeq ($(TARGET_ARCH),arm)
	LOCAL_SRC_FILES += $(arm_src_files)
	LOCAL_CFLAGS += $(arm_cflags)
else
	LOCAL_SRC_FILES += $(non_arm_src_files)
endif

include $(BUILD_STATIC_LIBRARY)
include $(CLEAR_VARS)
