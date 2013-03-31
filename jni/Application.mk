JNI_DIR := $(call my-dir)

# Create a Local.mk file if you need to customize values below

APP_OPTIM        := release
APP_ABI := armeabi armeabi-v7a x86 mips

MY_USE_CSIPSIMPLE := 1

MY_USE_G729 := 1
MY_USE_ILBC := 0
MY_USE_G722 := 1
MY_USE_G7221 := 1
MY_USE_SPEEX := 1
MY_USE_GSM := 1
MY_USE_SILK := 1
MY_USE_CODEC2 := 1
MY_USE_WEBRTC := 1
MY_USE_AMR := 1
MY_USE_G726 := 1
MY_USE_OPUS := 1

MY_USE_VIDEO := 1

MY_USE_TLS := 1
MY_USE_ZRTP := 1
MY_USE_STATIC_SSL := 0

#############################################################
# Do not change behind this line the are flags for pj build #
# Only build pjsipjni and ignore openssl                    #
# Include for local development
_local_mk := $(strip $(wildcard $(JNI_DIR)/Local.mk))
ifdef _local_mk
include $(JNI_DIR)/Local.mk
$(call __ndk_info,Uses local settings)
else
$(call __ndk_info,No local settings... build all in release mode !)
endif

APP_MODULES := libpjsipjni pj_opensl_dev

SWIG ?= swig2.0
PYTHON ?= python

# Modules for extra codecs are 
# pj_g7221_codec pj_codec2_codec pj_g726_codec pj_opus_codec
#
# Modules for video is
# pj_video_android
#
# Module for screen capture is
# pj_screen_capture_android

APP_PLATFORM := android-14
APP_STL := stlport_shared

BASE_PJSIP_FLAGS := -DPJ_ANDROID=1 -DUSE_CSIPSIMPLE=$(MY_USE_CSIPSIMPLE)
# about codecs
BASE_PJSIP_FLAGS += -DPJMEDIA_HAS_G729_CODEC=$(MY_USE_G729) -DPJMEDIA_HAS_G726_CODEC=$(MY_USE_G726) \
	-DPJMEDIA_HAS_ILBC_CODEC=$(MY_USE_ILBC) -DPJMEDIA_HAS_G722_CODEC=$(MY_USE_G722) \
	-DPJMEDIA_HAS_SPEEX_CODEC=$(MY_USE_SPEEX) -DPJMEDIA_HAS_GSM_CODEC=$(MY_USE_GSM) \
	-DPJMEDIA_HAS_SILK_CODEC=$(MY_USE_SILK) -DPJMEDIA_HAS_CODEC2_CODEC=$(MY_USE_CODEC2) \
	-DPJMEDIA_HAS_G7221_CODEC=$(MY_USE_G7221) -DPJMEDIA_HAS_WEBRTC_CODEC=$(MY_USE_WEBRTC) \
	-DPJMEDIA_HAS_OPENCORE_AMRNB_CODEC=$(MY_USE_AMR) -DPJMEDIA_HAS_OPENCORE_AMRWB_CODEC=$(MY_USE_AMR) \
	-DPJMEDIA_HAS_OPUS_CODEC=$(MY_USE_OPUS)

# media
BASE_PJSIP_FLAGS += -DPJMEDIA_HAS_WEBRTC_AEC=$(MY_USE_WEBRTC) \
	-DPJMEDIA_HAS_VIDEO=$(MY_USE_VIDEO) \
	-DPJMEDIA_VIDEO_DEV_HAS_CBAR_SRC=0


# TLS ZRTP
BASE_PJSIP_FLAGS += -DPJ_HAS_SSL_SOCK=$(MY_USE_TLS) -DPJMEDIA_HAS_ZRTP=$(MY_USE_ZRTP)

# Force some settings for compatibility with some buggy sip providers (Pflingo)
BASE_PJSIP_FLAGS += -DPJSUA_SDP_SESS_HAS_CONN=1 

