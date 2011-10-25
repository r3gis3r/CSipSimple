LOCAL_PATH := $(call my-dir)
THIRD_PARTY_PATH := $(LOCAL_PATH)

# Third parties
include $(THIRD_PARTY_PATH)/resample/Android.mk

##Secure third parties

#TLS
#ifeq ($(MY_USE_TLS),1)
#include $(TOP_LOCAL_PATH)/third_party/openssl/Android.mk
#include $(TOP_LOCAL_PATH)/third_party/build/zrtp4pj/Android.mk		
#endif

#SRTP
include $(THIRD_PARTY_PATH)/srtp/Android.mk


##Media third parties
ifeq ($(MY_USE_ILBC),1)
	include $(THIRD_PARTY_PATH)/ilbc/Android.mk
endif
ifeq ($(MY_USE_GSM),1)
	include $(THIRD_PARTY_PATH)/gsm/Android.mk
endif
ifeq ($(MY_USE_SPEEX),1)
	include $(THIRD_PARTY_PATH)/speex/Android.mk
endif
#ifeq ($(MY_USE_G729),1)
#	include $(TOP_LOCAL_PATH)/third_party/build/g729/Android.mk
#endif
#ifeq ($(MY_USE_SILK),1)
#	include $(TOP_LOCAL_PATH)/third_party/build/silk/Android.mk
#endif
#ifeq ($(MY_USE_CODEC2),1)
#	include $(TOP_LOCAL_PATH)/third_party/build/codec2/Android.mk
#endif

ifeq ($(MY_USE_G7221),1)
	include $(THIRD_PARTY_PATH)/g7221/Android.mk
endif


