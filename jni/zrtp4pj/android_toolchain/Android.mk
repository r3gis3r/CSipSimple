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
			$(LOCAL_PATH)/zrtp/src/ \
			$(LOCAL_PATH)/zrtp/src/libzrtpcpp \
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
    zrtp/src/libzrtpcpp/crypto/openssl/ZrtpDH.o \
    zrtp/src/libzrtpcpp/crypto/openssl/hmac256.o \
    zrtp/src/libzrtpcpp/crypto/openssl/sha256.o \
    zrtp/src/libzrtpcpp/crypto/openssl/hmac384.o \
    zrtp/src/libzrtpcpp/crypto/openssl/sha384.o \
    zrtp/src/libzrtpcpp/crypto/openssl/AesCFB.o

skeinmac = zrtp/srtp/crypto/skein.o zrtp/srtp/crypto/skein_block.o zrtp/srtp/crypto/skeinApi.o \
    zrtp/srtp/crypto/macSkein.o

twofish = zrtp/src/libzrtpcpp/crypto/twofish.o \
	zrtp/src/libzrtpcpp/crypto/twofish_cfb.o \
	zrtp/src/libzrtpcpp/crypto/TwoCFB.o

# Gcrypt support currently not tested
#ciphersgcrypt = crypto/gcrypt/gcryptAesSrtp.o crypto/gcrypt/gcrypthmac.o \
#          crypto/gcrypt/InitializeGcrypt.o

zrtpobj = zrtp/src/ZrtpCallbackWrapper.o \
    zrtp/src/ZIDFile.o \
    zrtp/src/ZIDRecord.o \
    zrtp/src/ZRtp.o \
    zrtp/src/ZrtpCrc32.o \
    zrtp/src/ZrtpPacketCommit.o \
    zrtp/src/ZrtpPacketConf2Ack.o \
    zrtp/src/ZrtpPacketConfirm.o \
    zrtp/src/ZrtpPacketDHPart.o \
    zrtp/src/ZrtpPacketGoClear.o \
    zrtp/src/ZrtpPacketClearAck.o \
    zrtp/src/ZrtpPacketHelloAck.o \
    zrtp/src/ZrtpPacketHello.o \
    zrtp/src/ZrtpPacketError.o \
    zrtp/src/ZrtpPacketErrorAck.o \
    zrtp/src/ZrtpPacketPingAck.o \
    zrtp/src/ZrtpPacketPing.o \
    zrtp/src/ZrtpPacketSASrelay.o \
    zrtp/src/ZrtpPacketRelayAck.o \
    zrtp/src/ZrtpStateClass.o \
    zrtp/src/ZrtpTextData.o \
    zrtp/src/ZrtpConfigure.o \
    zrtp/src/ZrtpCWrapper.o \
    zrtp/src/Base32.o

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

LOCAL_SRC_FILES += $(zrtpsrc) $(cryptsrc) $(srtpsrc) $(transportsrc) 

include $(BUILD_STATIC_LIBRARY)
