
#APP_OPTIM        := debug
APP_OPTIM        := release

APP_ABI := armeabi armeabi-v7a
#APP_ABI := x86 # WARNING --- for now, if you want to activate x86 build, set APP_OPTIM to "debug"



MY_USE_CSIPSIMPLE := 1

MY_USE_G729 := 1
MY_USE_ILBC := 0
MY_USE_G722 := 1
MY_USE_G7221 := 1
MY_USE_SPEEX := 1
MY_USE_GSM := 1
MY_USE_SILK := 1
MY_USE_CODEC2 := 1
MY_USE_TLS := 1
MY_USE_WEBRTC := 1
MY_USE_AMR := 1
MY_USE_G726 := 1

MY_USE_VIDEO := 1


#############################################################
# Do not change behind this line the are flags for pj build #
# Only build pjsipjni and ignore openssl                    #
APP_MODULES := libpjsipjni pj_opensl_dev
ifeq ($(MY_USE_SILK),1)
APP_MODULES += libpj_silk_codec 
endif
ifeq ($(MY_USE_G7221),1)
APP_MODULES += libpj_g7221_codec
endif
ifeq ($(MY_USE_CODEC2),1)
APP_MODULES += libpj_codec2_codec
endif
ifeq ($(MY_USE_G726),1)
APP_MODULES += libpj_g726_codec
endif
ifeq ($(MY_USE_VIDEO),1)
APP_MODULES += pj_video_android pj_screen_capture_android
endif

APP_PLATFORM := android-9
APP_STL := gnustl_static #stlport_static

BASE_PJSIP_FLAGS := -DPJ_ANDROID=1 -DUSE_CSIPSIMPLE=$(MY_USE_CSIPSIMPLE)
# about codecs
BASE_PJSIP_FLAGS += -DPJMEDIA_HAS_G729_CODEC=$(MY_USE_G729) -DPJMEDIA_HAS_G726_CODEC=$(MY_USE_G726) \
	-DPJMEDIA_HAS_ILBC_CODEC=$(MY_USE_ILBC) -DPJMEDIA_HAS_G722_CODEC=$(MY_USE_G722) \
	-DPJMEDIA_HAS_SPEEX_CODEC=$(MY_USE_SPEEX) -DPJMEDIA_HAS_GSM_CODEC=$(MY_USE_GSM) \
	-DPJMEDIA_HAS_SILK_CODEC=$(MY_USE_SILK) -DPJMEDIA_HAS_CODEC2_CODEC=$(MY_USE_CODEC2) \
	-DPJMEDIA_HAS_G7221_CODEC=$(MY_USE_G7221) -DPJMEDIA_HAS_WEBRTC_CODEC=$(MY_USE_WEBRTC) \
	-DPJMEDIA_HAS_AMR_STAGEFRIGHT_CODEC=$(MY_USE_AMR)

# media
BASE_PJSIP_FLAGS += -DPJMEDIA_HAS_WEBRTC_AEC=$(MY_USE_WEBRTC) \
	-DPJMEDIA_HAS_VIDEO=$(MY_USE_VIDEO) \
	-DPJMEDIA_VIDEO_DEV_HAS_CBAR_SRC=0
	

# TLS ZRTP
BASE_PJSIP_FLAGS += -DPJ_HAS_SSL_SOCK=$(MY_USE_TLS) -DPJMEDIA_HAS_ZRTP=$(MY_USE_TLS)
