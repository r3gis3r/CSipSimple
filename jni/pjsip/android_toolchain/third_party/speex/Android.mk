#########
# SPEEX #
#########

LOCAL_PATH := $(call my-dir)/../../../sources/third_party/speex/

include $(CLEAR_VARS)
LOCAL_MODULE    := speex
LOCAL_C_INCLUDES := $(LOCAL_PATH)../build/speex $(LOCAL_PATH)include \
		   $(LOCAL_PATH)libspeex $(LOCAL_PATH)../../pjlib/include/
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DHAVE_CONFIG_H=1
PJLIB_SRC_DIR := libspeex

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/bits.c $(PJLIB_SRC_DIR)/cb_search.c $(PJLIB_SRC_DIR)/exc_10_16_table.c  \
			$(PJLIB_SRC_DIR)/exc_10_32_table.c $(PJLIB_SRC_DIR)/exc_20_32_table.c \
			$(PJLIB_SRC_DIR)/exc_5_256_table.c $(PJLIB_SRC_DIR)/exc_5_64_table.c \
			$(PJLIB_SRC_DIR)/exc_8_128_table.c $(PJLIB_SRC_DIR)/fftwrap.c $(PJLIB_SRC_DIR)/filterbank.c \
			$(PJLIB_SRC_DIR)/filters.c $(PJLIB_SRC_DIR)/gain_table.c $(PJLIB_SRC_DIR)/gain_table_lbr.c \
			$(PJLIB_SRC_DIR)/hexc_10_32_table.c $(PJLIB_SRC_DIR)/hexc_table.c \
			$(PJLIB_SRC_DIR)/high_lsp_tables.c \
			$(PJLIB_SRC_DIR)/kiss_fft.c $(PJLIB_SRC_DIR)/kiss_fftr.c $(PJLIB_SRC_DIR)/lpc.c \
			$(PJLIB_SRC_DIR)/lsp.c $(PJLIB_SRC_DIR)/lsp_tables_nb.c $(PJLIB_SRC_DIR)/ltp.c \
			$(PJLIB_SRC_DIR)/mdf.c $(PJLIB_SRC_DIR)/modes.c $(PJLIB_SRC_DIR)/modes_wb.c \
			$(PJLIB_SRC_DIR)/nb_celp.c $(PJLIB_SRC_DIR)/preprocess.c \
			$(PJLIB_SRC_DIR)/quant_lsp.c $(PJLIB_SRC_DIR)/resample.c $(PJLIB_SRC_DIR)/sb_celp.c $(PJLIB_SRC_DIR)/smallft.c \
			$(PJLIB_SRC_DIR)/speex.c $(PJLIB_SRC_DIR)/speex_callbacks.c $(PJLIB_SRC_DIR)/speex_header.c \
			$(PJLIB_SRC_DIR)/stereo.c $(PJLIB_SRC_DIR)/vbr.c $(PJLIB_SRC_DIR)/vq.c $(PJLIB_SRC_DIR)/window.c

include $(BUILD_STATIC_LIBRARY)

