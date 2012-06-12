MODULE_PATH := $(call my-dir)/../

# Hack for mips
# Add a static target for libgcc
include $(CLEAR_VARS)
LOCAL_PATH := /
LOCAL_MODULE    := libgcc 
LOCAL_SRC_FILES := $(TARGET_LIBGCC)
include $(PREBUILT_STATIC_LIBRARY)



##################
# CSipSimple lib #
##################
include $(CLEAR_VARS)
LOCAL_PATH := $(MODULE_PATH)

LOCAL_MODULE := pjsipjni

PJ_ROOT_DIR := $(LOCAL_PATH)/../pjsip/sources/
PJ_ANDROID_ROOT_DIR := $(LOCAL_PATH)/../pjsip/android_sources/

#Include PJ interfaces
LOCAL_C_INCLUDES += $(PJ_ROOT_DIR)/pjsip/include $(PJ_ROOT_DIR)/pjlib-util/include/ \
			$(PJ_ROOT_DIR)/pjlib/include/ $(PJ_ROOT_DIR)/pjmedia/include \
			$(PJ_ROOT_DIR)/pjnath/include $(PJ_ROOT_DIR)/pjlib/include
#Include PJ android interfaces
LOCAL_C_INCLUDES += $(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-audiodev
LOCAL_C_INCLUDES += $(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-videodev

# Include WebRTC 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../webrtc/pj_sources/

# Include g729 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../g729/pj_sources/

# Include ZRTP interface 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../zrtp4pj/sources/zsrtp/include/ $(LOCAL_PATH)/../zrtp4pj/sources/zsrtp/zrtp/src/ 

# Include swig wrapper headers
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../swig-glue/

# Include self headers
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

JNI_SRC_DIR := src/

LOCAL_SRC_FILES := $(JNI_SRC_DIR)/pjsua_jni_addons.c $(JNI_SRC_DIR)/q850_reason_parser.c \
	$(JNI_SRC_DIR)/zrtp_android.c $(JNI_SRC_DIR)/ringback_tone.c \
	$(JNI_SRC_DIR)/android_logger.c $(JNI_SRC_DIR)/audio_codecs.c \
	$(JNI_SRC_DIR)/csipsimple_codecs_utils.c \
	$(JNI_SRC_DIR)/call_recorder.c 

# NDK fixer
LOCAL_SRC_FILES +=$(JNI_SRC_DIR)/ndk_stl_fixer.cpp


LOCAL_LDLIBS := -llog -ldl

ifeq ($(MY_USE_AMR),1)
	LOCAL_STATIC_LIBRARIES += pj_amr_stagefright_codec
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../amr-stagefright/pj_sources/
endif


LOCAL_STATIC_LIBRARIES += swig-glue pjsip pjmedia swig-glue pjnath pjlib-util pjlib resample srtp 


ifeq ($(MY_USE_ILBC),1)
	LOCAL_STATIC_LIBRARIES += ilbc
endif
ifeq ($(MY_USE_GSM),1)
	LOCAL_STATIC_LIBRARIES += gsm
endif
ifeq ($(MY_USE_SPEEX),1)
	LOCAL_STATIC_LIBRARIES += speex
endif
ifeq ($(MY_USE_G729),1)
	LOCAL_STATIC_LIBRARIES += pj_g729_codec
endif

ifeq ($(MY_USE_ZRTP),1)
	LOCAL_STATIC_LIBRARIES += zrtp4pj
endif

ifeq ($(MY_USE_TLS),1)
ifeq ($(MY_USE_STATIC_SSL),1)
# This is to do builds with full openssl built in.
# Not use unless you know what you do and create a ssl_static target in openssl lib
	LOCAL_STATIC_LIBRARIES += ssl_static crypto_static
	LOCAL_LDLIBS += -lz
else
# Normal mainstream users mode
	LOCAL_STATIC_LIBRARIES += crypto_ec_static
	LOCAL_SHARED_LIBRARIES += libssl libcrypto
endif
endif


ifeq ($(MY_USE_WEBRTC),1)

	
# Codecs wrap
	LOCAL_STATIC_LIBRARIES +=  pj_webrtc_codec libwebrtc_audio_coding libwebrtc_cng libwebrtc_vad libwebrtc_neteq libwebrtc_resampler
	
#Codecs implementations 
	LOCAL_STATIC_LIBRARIES += libwebrtc_ilbc libwebrtc_g711 
	#libwebrtc_pcm16b
	#libwebrtc_g722 

# AEC
ifeq ($(USE_FIXED_POINT),1)
	LOCAL_STATIC_LIBRARIES += libwebrtc_isacfix libwebrtc_aecm
else
	LOCAL_STATIC_LIBRARIES += libwebrtc_isac libwebrtc_aec
endif

#NS
	LOCAL_STATIC_LIBRARIES += libwebrtc_ns
	
#Common
	LOCAL_STATIC_LIBRARIES += libwebrtc_apm_utility libwebrtc_system_wrappers libwebrtc_spl 

endif

ifeq ($(TARGET_ARCH_ABI),mips)
	LOCAL_STATIC_LIBRARIES += libgcc
endif

include $(BUILD_SHARED_LIBRARY)

