LOCAL_PATH := $(call my-dir)/../../sources/pjsip/


#########
# PJSIP #
#########
include $(CLEAR_VARS)
LOCAL_MODULE    := pjsip

LOCAL_C_INCLUDES := $(LOCAL_PATH)include \
		$(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
		$(LOCAL_PATH)../pjnath/include $(LOCAL_PATH)../pjmedia/include


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJSIP_SRC_DIR := src/pjsip
PJSIPUA_SRC_DIR := src/pjsip-ua
PJSIPSIMPLE_SRC_DIR := src/pjsip-simple
PJSUA_SRC_DIR := src/pjsua-lib

LOCAL_SRC_FILES := $(PJSIP_SRC_DIR)/sip_config.c $(PJSIP_SRC_DIR)/sip_multipart.c \
		$(PJSIP_SRC_DIR)/sip_errno.c $(PJSIP_SRC_DIR)/sip_msg.c $(PJSIP_SRC_DIR)/sip_parser.c \
		$(PJSIP_SRC_DIR)/sip_tel_uri.c $(PJSIP_SRC_DIR)/sip_uri.c \
		$(PJSIP_SRC_DIR)/sip_endpoint.c $(PJSIP_SRC_DIR)/sip_util.c $(PJSIP_SRC_DIR)/sip_util_proxy.c \
		$(PJSIP_SRC_DIR)/sip_resolve.c $(PJSIP_SRC_DIR)/sip_transport.c $(PJSIP_SRC_DIR)/sip_transport_loop.c \
		$(PJSIP_SRC_DIR)/sip_transport_udp.c $(PJSIP_SRC_DIR)/sip_transport_tcp.c \
		$(PJSIP_SRC_DIR)/sip_auth_aka.c $(PJSIP_SRC_DIR)/sip_auth_client.c \
		$(PJSIP_SRC_DIR)/sip_auth_msg.c $(PJSIP_SRC_DIR)/sip_auth_parser.c \
		$(PJSIP_SRC_DIR)/sip_auth_server.c \
		$(PJSIP_SRC_DIR)/sip_transaction.c $(PJSIP_SRC_DIR)/sip_util_statefull.c \
		$(PJSIP_SRC_DIR)/sip_dialog.c $(PJSIP_SRC_DIR)/sip_ua_layer.c \
		$(PJSIPUA_SRC_DIR)/sip_inv.c $(PJSIPUA_SRC_DIR)/sip_reg.c \
		$(PJSIPUA_SRC_DIR)/sip_replaces.c $(PJSIPUA_SRC_DIR)/sip_xfer.c \
		$(PJSIPUA_SRC_DIR)/sip_100rel.c $(PJSIPUA_SRC_DIR)/sip_timer.c \
		$(PJSIPSIMPLE_SRC_DIR)/errno.c $(PJSIPSIMPLE_SRC_DIR)/evsub.c \
		$(PJSIPSIMPLE_SRC_DIR)/evsub_msg.c $(PJSIPSIMPLE_SRC_DIR)/iscomposing.c \
		$(PJSIPSIMPLE_SRC_DIR)/mwi.c \
		$(PJSIPSIMPLE_SRC_DIR)/pidf.c $(PJSIPSIMPLE_SRC_DIR)/presence.c \
		$(PJSIPSIMPLE_SRC_DIR)/presence_body.c $(PJSIPSIMPLE_SRC_DIR)/publishc.c \
		$(PJSIPSIMPLE_SRC_DIR)/rpid.c $(PJSIPSIMPLE_SRC_DIR)/xpidf.c \
		$(PJSUA_SRC_DIR)/pjsua_acc.c $(PJSUA_SRC_DIR)/pjsua_call.c $(PJSUA_SRC_DIR)/pjsua_core.c \
		$(PJSUA_SRC_DIR)/pjsua_im.c $(PJSUA_SRC_DIR)/pjsua_media.c $(PJSUA_SRC_DIR)/pjsua_pres.c \
		$(PJSUA_SRC_DIR)/pjsua_dump.c $(PJSUA_SRC_DIR)/pjsua_vid.c 

ifeq ($(MY_USE_TLS),1)
LOCAL_SRC_FILES += $(PJSIP_SRC_DIR)/sip_transport_tls_ossl.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../openssl/sources/include/
endif


include $(BUILD_STATIC_LIBRARY)