########
# ZRTP #
########

LOCAL_PATH := $(call my-dir)/../sources/zsrtp

include $(CLEAR_VARS)
LOCAL_MODULE    := zrtp4pj

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include \
			$(LOCAL_PATH)/include/crypto/ \
			$(LOCAL_PATH)/include/ \
			$(LOCAL_PATH)/../../../pjlib/include \
		    $(LOCAL_PATH)/../../../pjlib-util/include \
		    $(LOCAL_PATH)/../../../pjmedia/include  \
		    $(LOCAL_PATH)/../../openssl/include 

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := 

# ciphersossl
LOCAL_SRC_FILES := crypto/openssl/AesSrtp.cpp \
	crypto/openssl/hmac.cpp \
    crypto/openssl/ZrtpDH.cpp \
    crypto/openssl/hmac256.cpp \
    crypto/openssl/sha256.cpp \
    crypto/openssl/hmac384.cpp \
    crypto/openssl/sha384.cpp \
    crypto/openssl/AesCFB.cpp

#skeinmac 
LOCAL_SRC_FILES += crypto/skein.c crypto/skein_block.c crypto/skeinApi.c \
	crypto/macSkein.cpp

#twofish 
LOCAL_SRC_FILES += crypto/twofish.c \
	crypto/twofish_cfb.c \
	crypto/TwoCFB.cpp

# zrtpobj
LOCAL_SRC_FILES += zrtp/ZrtpCallbackWrapper.cpp \
    zrtp/ZIDFile.cpp \
    zrtp/ZIDRecord.cpp \
    zrtp/ZRtp.cpp \
	zrtp/ZrtpCrc32.cpp \
	zrtp/ZrtpPacketCommit.cpp \
	zrtp/ZrtpPacketConf2Ack.cpp \
	zrtp/ZrtpPacketConfirm.cpp \
	zrtp/ZrtpPacketDHPart.cpp \
	zrtp/ZrtpPacketGoClear.cpp \
	zrtp/ZrtpPacketClearAck.cpp \
	zrtp/ZrtpPacketHelloAck.cpp \
	zrtp/ZrtpPacketHello.cpp \
	zrtp/ZrtpPacketError.cpp \
	zrtp/ZrtpPacketErrorAck.cpp \
	zrtp/ZrtpPacketPingAck.cpp \
	zrtp/ZrtpPacketPing.cpp \
	zrtp/ZrtpStateClass.cpp \
	zrtp/ZrtpTextData.cpp \
	zrtp/ZrtpConfigure.cpp \
	zrtp/ZrtpCWrapper.cpp \
	zrtp/Base32.cpp


#srtpobj 
LOCAL_SRC_FILES += srtp/ZsrtpCWrapper.cpp srtp/CryptoContext.cpp

#transportobj 
LOCAL_SRC_FILES += transport_zrtp.c



include $(BUILD_STATIC_LIBRARY)
