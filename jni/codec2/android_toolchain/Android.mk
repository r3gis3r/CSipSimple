LOCAL_PATH := $(call my-dir)


ifeq ($(MY_USE_CODEC2),1)

### Glue for pjsip codec ###
include $(CLEAR_VARS)
LOCAL_MODULE := pj_codec2_codec

CODEC2_PATH := ../sources
PJ_CODEC2_PATH := ../pj_sources
CODEC2_GEN_PATH := ../generated

# pj
PJ_DIR = $(LOCAL_PATH)/../../pjsip/sources
LOCAL_C_INCLUDES += $(PJ_DIR)/pjlib/include \
	$(PJ_DIR)/pjlib-util/include \
	$(PJ_DIR)/pjnath/include \
	$(PJ_DIR)/pjmedia/include
# codec2
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(CODEC2_PATH)
LOCAL_SRC_FILES += $(CODEC2_PATH)/dump.c \
	$(CODEC2_PATH)/lpc.c \
	$(CODEC2_PATH)/nlp.c \
	$(CODEC2_PATH)/postfilter.c \
	$(CODEC2_PATH)/sine.c \
	$(CODEC2_PATH)/codec2.c \
	$(CODEC2_PATH)/fft.c \
	$(CODEC2_PATH)/kiss_fft.c \
	$(CODEC2_PATH)/interp.c \
	$(CODEC2_PATH)/lsp.c \
	$(CODEC2_PATH)/phase.c \
	$(CODEC2_PATH)/quantise.c \
	$(CODEC2_PATH)/pack.c \
	$(CODEC2_GEN_PATH)/codebook.c \
	$(CODEC2_GEN_PATH)/codebookd.c \
	$(CODEC2_GEN_PATH)/codebookdvq.c
# self
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(PJ_CODEC2_PATH)
LOCAL_SRC_FILES += $(PJ_CODEC2_PATH)/pj_codec2.c



LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
LOCAL_SHARED_LIBRARIES += libpjsipjni

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)

endif