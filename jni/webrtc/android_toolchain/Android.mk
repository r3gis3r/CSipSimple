LOCAL_PATH := $(call my-dir)
WEBRTC_PATH := $(LOCAL_PATH)/../sources/

ifeq ($(MY_USE_WEBRTC),1)


### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_webrtc_codec

#CODECS
# webrtc
LOCAL_C_INCLUDES += $(WEBRTC_PATH)/ $(WEBRTC_PATH)/modules/interface/ $(WEBRTC_PATH)/modules/audio_coding/main/interface/
# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources/
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include/ \
	$(PJ_DIR)/pjlib-util/include/ \
	$(PJ_DIR)/pjnath/include/ \
	$(PJ_DIR)/pjmedia/include/
#self
LOCAL_C_INCLUDES += ../pj_sources/


LOCAL_SRC_FILES := ../pj_sources/webrtc_codec.cpp ../pj_sources/webrtc_coder.cpp 

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)


### Commons ###
	include $(WEBRTC_PATH)/system_wrappers/source/Android.mk
	include $(WEBRTC_PATH)/modules/audio_processing/utility/Android.mk
	include $(WEBRTC_PATH)/common_audio/signal_processing/Android.mk
	include $(WEBRTC_PATH)/common_audio/vad/Android.mk
	include $(WEBRTC_PATH)/common_audio/resampler/Android.mk
	
### AEC ###
#ifeq ($(TARGET_ARCH_ABI),armeabi)
# AEC fixed 
	include $(WEBRTC_PATH)/modules/audio_processing/aecm/Android.mk
#else
# AEC floating
#	include $(WEBRTC_PATH)/modules/audio_processing/aec/Android.mk
#endif

### CODECS ###
include $(WEBRTC_PATH)/modules/audio_coding/main/source/Android.mk
include $(WEBRTC_PATH)/modules/audio_coding/neteq/Android.mk
include $(WEBRTC_PATH)/modules/audio_coding/codecs/cng/Android.mk
include $(WEBRTC_PATH)/modules/audio_coding/codecs/g711/Android.mk
include $(WEBRTC_PATH)/modules/audio_coding/codecs/ilbc/Android.mk
#include $(TOP_LOCAL_PATH)/modules/audio_coding/codecs/PCM16B/main/source/Android.mk
ifeq ($(TARGET_ARCH_ABI),armeabi)
#Fix codecs
	include $(WEBRTC_PATH)/modules/audio_coding/codecs/iSAC/fix/source/Android.mk
else
#Floating codecs
	include $(WEBRTC_PATH)/modules/audio_coding/codecs/iSAC/main/source/Android.mk
endif

### NOISE SUPPR ###
include $(WEBRTC_PATH)/modules/audio_processing/ns/Android.mk




# WARN ABOUT DUPLICATE CODEC !
ifeq ($(MY_USE_ILBC),1)
$(warning MY_USE_ILBC and MY_USE_WEBRTC will both produce iLBC codec)
endif

endif