########
# ZRTP #
########

LOCAL_PATH := $(call my-dir)/../sources/zsrtp

include $(CLEAR_VARS)
LOCAL_MODULE    := zrtp4pj

PJ_SRC_DIR := $(LOCAL_PATH)/../../../pjsip/sources/
OPENSSL_SRC_DIR := $(LOCAL_PATH)/../../../openssl/sources/

# Self includes
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include \
			$(LOCAL_PATH)/zrtp/ \
			$(LOCAL_PATH)/zsrtp/ \
			$(LOCAL_PATH)/zrtp/zrtp/ \
			$(LOCAL_PATH)/zrtp/zrtp/libzrtpcpp \
			$(LOCAL_PATH)/zrtp/srtp/ 

# Pj includes
LOCAL_C_INCLUDES += $(PJ_SRC_DIR)/pjsip/include $(PJ_SRC_DIR)/pjlib-util/include/ \
			$(PJ_ROOT_DIR)/pjlib/include/ $(PJ_SRC_DIR)/pjmedia/include \
			$(PJ_SRC_DIR)/pjnath/include $(PJ_SRC_DIR)/pjlib/include
			
#OpenSSL includes
LOCAL_C_INCLUDES += $(OPENSSL_SRC_DIR)/include 

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DDYNAMIC_TIMER=1

###### From make file

ciphersossl = zrtp/srtp/crypto/openssl/SrtpSymCrypto.o \
    zrtp/srtp/crypto/openssl/hmac.o \
    zrtp/zrtp/crypto/openssl/zrtpDH.o \
    zrtp/zrtp/crypto/openssl/hmac256.o \
    zrtp/zrtp/crypto/openssl/sha256.o \
    zrtp/zrtp/crypto/openssl/hmac384.o \
    zrtp/zrtp/crypto/openssl/sha384.o \
    zrtp/zrtp/crypto/openssl/aesCFB.o

skeinmac = zrtp/cryptcommon/skein.o zrtp/cryptcommon/skein_block.o zrtp/cryptcommon/skeinApi.o \
    zrtp/cryptcommon/macSkein.o

twofish = zrtp/cryptcommon/twofish.o \
	zrtp/cryptcommon/twofish_cfb.o \
	zrtp/zrtp/crypto/twoCFB.o

# Gcrypt support currently not tested
#ciphersgcrypt = crypto/gcrypt/gcryptAesSrtp.o crypto/gcrypt/gcrypthmac.o \
#          crypto/gcrypt/InitializeGcrypt.o
zrtpobj = zrtp/zrtp/ZrtpCallbackWrapper.o \
    zrtp/zrtp/ZIDCacheFile.o \
    zrtp/zrtp/ZIDRecordFile.o \
    zrtp/zrtp/ZRtp.o \
    zrtp/zrtp/ZrtpCrc32.o \
    zrtp/zrtp/ZrtpPacketCommit.o \
    zrtp/zrtp/ZrtpPacketConf2Ack.o \
    zrtp/zrtp/ZrtpPacketConfirm.o \
    zrtp/zrtp/ZrtpPacketDHPart.o \
    zrtp/zrtp/ZrtpPacketGoClear.o \
    zrtp/zrtp/ZrtpPacketClearAck.o \
    zrtp/zrtp/ZrtpPacketHelloAck.o \
    zrtp/zrtp/ZrtpPacketHello.o \
    zrtp/zrtp/ZrtpPacketError.o \
    zrtp/zrtp/ZrtpPacketErrorAck.o \
    zrtp/zrtp/ZrtpPacketPingAck.o \
    zrtp/zrtp/ZrtpPacketPing.o \
    zrtp/zrtp/ZrtpPacketSASrelay.o \
    zrtp/zrtp/ZrtpPacketRelayAck.o \
    zrtp/zrtp/ZrtpStateClass.o \
    zrtp/zrtp/ZrtpTextData.o \
    zrtp/zrtp/ZrtpConfigure.o \
    zrtp/zrtp/ZrtpCWrapper.o \
    zrtp/zrtp/Base32.o
    

srtpobj = srtp/ZsrtpCWrapper.o zrtp/srtp/CryptoContext.o zrtp/srtp/CryptoContextCtrl.o
transportobj = transport_zrtp.o
cryptobj =  $(ciphersossl) $(skeinmac) $(twofish)
# -- END OF ZRTP4PJ makefile

zrtpsrc := $(zrtpobj:%.o=%.cpp)
cryptsrc := $(cryptobj:%.o=%.cpp)
cryptsrc := $(cryptsrc:%skein.cpp=%skein.c)
cryptsrc := $(cryptsrc:%skein_block.cpp=%skein_block.c)
cryptsrc := $(cryptsrc:%skeinApi.cpp=%skeinApi.c)
cryptsrc := $(cryptsrc:%twofish.cpp=%twofish.c)
cryptsrc := $(cryptsrc:%twofish_cfb.cpp=%twofish_cfb.c)
srtpsrc := $(srtpobj:%.o=%.cpp)
transportsrc := $(transportobj:%.o=%.c)

LOCAL_SRC_FILES += $(zrtpsrc) $(cryptsrc) $(srtpsrc) $(transportsrc) zrtp/common/osSpecifics.c

include $(BUILD_STATIC_LIBRARY)
