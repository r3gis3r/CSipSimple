LOCAL_PATH := $(call my-dir)/../


##################
# CSipSimple lib #
##################
include $(CLEAR_VARS)
LOCAL_MODULE := pjsipjni


PJ_ROOT_DIR := $(LOCAL_PATH)/../pjsip/sources/
PJ_ANDROID_ROOT_DIR := $(LOCAL_PATH)/../pjsip/android_sources/

#Include PJ interfaces
LOCAL_C_INCLUDES += $(PJ_ROOT_DIR)/pjsip/include $(PJ_ROOT_DIR)/pjlib-util/include/ \
			$(PJ_ROOT_DIR)/pjlib/include/ $(PJ_ROOT_DIR)/pjmedia/include \
			$(PJ_ROOT_DIR)/pjnath/include $(PJ_ROOT_DIR)/pjlib/include
#Include PJ android interfaces
LOCAL_C_INCLUDES += $(PJ_ANDROID_ROOT_DIR)/pjmedia/include/pjmedia-audiodev

# Include WebRTC 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../webrtc/pj_sources/

# Include g729 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../g729/pj_sources/

# Include ZRTP interface 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../zrtp4pj/sources/zsrtp/include/ 

# Include self headers
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include/


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

JNI_SRC_DIR := src/

LOCAL_SRC_FILES := $(JNI_SRC_DIR)/pjsua_jni_addons.c $(JNI_SRC_DIR)/zrtp_android.c

# NDK fixer
LOCAL_SRC_FILES +=$(JNI_SRC_DIR)/ndk_stl_fixer.cpp

	
#ifeq ($(MY_ANDROID_DEV),1)
#LOCAL_SRC_FILES += $(JNI_SRC_DIR)/android_jni_dev.cpp
#endif
#ifeq ($(MY_ANDROID_DEV),2)
#LOCAL_SRC_FILES += $(JNI_SRC_DIR)/opensl_dev.cpp
#endif

LOCAL_LDLIBS := -llog

# -- debug build
ifeq ($(APP_OPTIM),debug)
LOCAL_CFLAGS += -g #debug
LOCAL_LDFLAGS += -Wl,-Map,xxx.map #create map fil
endif
# 

ifeq ($(MY_USE_TLS),1)
LOCAL_LDLIBS += -ldl 
endif

ifeq ($(MY_USE_VIDEO),1)
	LOCAL_LDLIBS += -lGLESv1_CM
endif

#LOCAL_LDFLAGS := -Wl,-Map=moblox.map,--cref,--gc-section 

LOCAL_STATIC_LIBRARIES := swig-glue pjsip pjmedia swig-glue pjnath pjlib-util pjlib resample srtp 
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

ifeq ($(MY_USE_CODEC2),1)
	LOCAL_STATIC_LIBRARIES += codec2
endif
ifeq ($(MY_USE_AMR),1)
	LOCAL_STATIC_LIBRARIES += pj_amr_stagefright_codec
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../amr-stagefright/pj_sources/
endif
ifeq ($(MY_USE_TLS),1)
	LOCAL_STATIC_LIBRARIES += zrtp4pj
	LOCAL_STATIC_LIBRARIES += crypto_ec_static
	LOCAL_SHARED_LIBRARIES += libssl libcrypto
endif

ifeq ($(MY_USE_WEBRTC),1)

	
# Codecs wrap
	LOCAL_STATIC_LIBRARIES +=  pj_webrtc_codec libwebrtc_audio_coding libwebrtc_cng libwebrtc_vad libwebrtc_neteq libwebrtc_resampler
	
#Codecs implementations 
	LOCAL_STATIC_LIBRARIES += libwebrtc_ilbc libwebrtc_g711 
	#libwebrtc_pcm16b
	#libwebrtc_g722 

# AEC
ifeq ($(TARGET_ARCH_ABI),armeabi)
	LOCAL_STATIC_LIBRARIES += libwebrtc_isacfix 
else
	LOCAL_STATIC_LIBRARIES += libwebrtc_isac 
endif

LOCAL_STATIC_LIBRARIES += libwebrtc_aecm

#NS
	LOCAL_STATIC_LIBRARIES += libwebrtc_ns
	
#Common
	LOCAL_STATIC_LIBRARIES += libwebrtc_apm_utility libwebrtc_system_wrappers libwebrtc_spl 

endif


include $(BUILD_SHARED_LIBRARY)

