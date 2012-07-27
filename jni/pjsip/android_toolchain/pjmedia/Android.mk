###########
# PJMEDIA #
###########

LOCAL_PATH := $(call my-dir)/../../sources/pjmedia/
include $(CLEAR_VARS)

LOCAL_MODULE    := pjmedia

LOCAL_C_INCLUDES := $(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
	$(LOCAL_PATH)../pjnath/include/ $(LOCAL_PATH)include/ $(LOCAL_PATH)../ \
	$(LOCAL_PATH)../third_party/srtp/include $(LOCAL_PATH)../third_party/srtp/include \
	$(LOCAL_PATH)../third_party/srtp/crypto/include $(LOCAL_PATH)../third_party/build/srtp/ \
	$(LOCAL_PATH)../third_party/build/speex/  $(LOCAL_PATH)../third_party/speex/include \
	$(LOCAL_PATH)../third_party/


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src/pjmedia
PJMEDIADEV_SRC_DIR := src/pjmedia-audiodev
PJMEDIADEV_VIDEO_SRC_DIR := src/pjmedia-videodev
PJMEDIACODEC_SRC_DIR := src/pjmedia-codec

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/alaw_ulaw.c $(PJLIB_SRC_DIR)/alaw_ulaw_table.c \
	$(PJLIB_SRC_DIR)/bidirectional.c $(PJLIB_SRC_DIR)/format.c \
	$(PJLIB_SRC_DIR)/clock_thread.c $(PJLIB_SRC_DIR)/codec.c \
	$(PJLIB_SRC_DIR)/conference.c $(PJLIB_SRC_DIR)/conf_switch.c $(PJLIB_SRC_DIR)/delaybuf.c $(PJLIB_SRC_DIR)/echo_common.c \
	$(PJLIB_SRC_DIR)/echo_speex.c $(PJLIB_SRC_DIR)/echo_port.c $(PJLIB_SRC_DIR)/echo_suppress.c $(PJLIB_SRC_DIR)/endpoint.c $(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/g711.c $(PJLIB_SRC_DIR)/jbuf.c $(PJLIB_SRC_DIR)/master_port.c \
	$(PJLIB_SRC_DIR)/mem_capture.c $(PJLIB_SRC_DIR)/mem_player.c \
	$(PJLIB_SRC_DIR)/null_port.c $(PJLIB_SRC_DIR)/plc_common.c $(PJLIB_SRC_DIR)/port.c $(PJLIB_SRC_DIR)/splitcomb.c \
	$(PJLIB_SRC_DIR)/resample_resample.c $(PJLIB_SRC_DIR)/resample_libsamplerate.c \
	$(PJLIB_SRC_DIR)/resample_port.c $(PJLIB_SRC_DIR)/rtcp.c $(PJLIB_SRC_DIR)/rtcp_xr.c $(PJLIB_SRC_DIR)/rtp.c \
	$(PJLIB_SRC_DIR)/sdp.c $(PJLIB_SRC_DIR)/sdp_cmp.c $(PJLIB_SRC_DIR)/sdp_neg.c \
	$(PJLIB_SRC_DIR)/session.c $(PJLIB_SRC_DIR)/silencedet.c \
	$(PJLIB_SRC_DIR)/sound_port.c $(PJLIB_SRC_DIR)/stereo_port.c \
	$(PJLIB_SRC_DIR)/stream_common.c $(PJLIB_SRC_DIR)/stream_info.c \
	$(PJLIB_SRC_DIR)/stream.c $(PJLIB_SRC_DIR)/tonegen.c $(PJLIB_SRC_DIR)/transport_adapter_sample.c \
	$(PJLIB_SRC_DIR)/transport_ice.c $(PJLIB_SRC_DIR)/transport_loop.c \
	$(PJLIB_SRC_DIR)/transport_srtp.c $(PJLIB_SRC_DIR)/transport_udp.c \
	$(PJLIB_SRC_DIR)/wav_player.c $(PJLIB_SRC_DIR)/wav_playlist.c $(PJLIB_SRC_DIR)/wav_writer.c $(PJLIB_SRC_DIR)/wave.c \
	$(PJLIB_SRC_DIR)/wsola.c \
	$(PJLIB_SRC_DIR)/vid_port.c $(PJLIB_SRC_DIR)/vid_codec.c \
	$(PJLIB_SRC_DIR)/vid_stream.c $(PJLIB_SRC_DIR)/vid_stream_info.c $(PJLIB_SRC_DIR)/vid_tee.c \
	$(PJLIB_SRC_DIR)/converter.c $(PJLIB_SRC_DIR)/event.c \
	$(PJMEDIADEV_SRC_DIR)/audiodev.c $(PJMEDIADEV_SRC_DIR)/audiotest.c $(PJMEDIADEV_SRC_DIR)/errno.c \
	$(PJMEDIADEV_VIDEO_SRC_DIR)/videodev.c $(PJMEDIADEV_VIDEO_SRC_DIR)/colorbar_dev.c $(PJMEDIADEV_VIDEO_SRC_DIR)/errno.c \
	$(PJMEDIACODEC_SRC_DIR)/amr_sdp_match.c

# If not csipsimple, load default audio codecs loader from pjmedia
ifneq ($(MY_USE_CSIPSIMPLE),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/audio_codecs.c
endif

ifeq ($(MY_USE_G722),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/g722.c $(PJMEDIACODEC_SRC_DIR)/g722/g722_enc.c $(PJMEDIACODEC_SRC_DIR)/g722/g722_dec.c
endif
ifeq ($(MY_USE_SPEEX),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/speex_codec.c 
endif
ifeq ($(MY_USE_ILBC),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/ilbc.c 
endif
ifeq ($(MY_USE_GSM),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/gsm.c 
endif

ifeq ($(MY_USE_WEBRTC),1)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../webrtc/sources/
	
	#AEC
	LOCAL_SRC_FILES += $(PJLIB_SRC_DIR)/echo_webrtc_aec.c 
endif

PJ_ANDROID_SRC_DIR := ../../android_sources/pjmedia/src/

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-audiodev/ \
	$(LOCAL_PATH)/../../../swig-glue/
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-audiodev/android_jni_dev.cpp


include $(BUILD_STATIC_LIBRARY)


# G7221 shared lib

ifeq ($(MY_USE_G7221),1)

include $(CLEAR_VARS)

LOCAL_MODULE := pj_g7221_codec
LOCAL_C_INCLUDES := $(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
	$(LOCAL_PATH)../pjnath/include/ $(LOCAL_PATH)include/ $(LOCAL_PATH)../ \
	$(LOCAL_PATH)../third_party/


LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/g7221.c $(PJMEDIACODEC_SRC_DIR)/g7221_sdp_match.c 
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

LOCAL_STATIC_LIBRARIES += g7221
LOCAL_SHARED_LIBRARIES += libpjsipjni

include $(BUILD_SHARED_LIBRARY)
endif

# OpenSL-ES implementation

include $(CLEAR_VARS)

LOCAL_MODULE := pj_opensl_dev


LOCAL_C_INCLUDES := $(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
	$(LOCAL_PATH)../pjnath/include/ $(LOCAL_PATH)include/ $(LOCAL_PATH)../ 

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-audiodev/


LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-audiodev/opensl_dev.c


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DPJMEDIA_AUDIO_DEV_HAS_OPENSL=1
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_LDLIBS += -lOpenSLES

ifeq ($(TARGET_ARCH_ABI),mips)
	LOCAL_STATIC_LIBRARIES += libgcc
endif

include $(BUILD_SHARED_LIBRARY)


ifeq ($(MY_USE_VIDEO),1)
# Video capture/render implementation
include $(CLEAR_VARS)

LOCAL_MODULE := pj_video_android


LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../webrtc/sources/modules/video_render/main/interface/ \
	$(LOCAL_PATH)/../../../webrtc/sources/modules/video_capture/main/interface/ \
	$(LOCAL_PATH)/../../../webrtc/sources/modules/interface/ \
	$(LOCAL_PATH)/../../../webrtc/sources/system_wrappers/interface/ \
	$(LOCAL_PATH)/../../../webrtc/sources/modules/ \
	$(LOCAL_PATH)/../../../webrtc/sources/ \
	$(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
	$(LOCAL_PATH)../pjsip/include/ \
	$(LOCAL_PATH)../pjnath/include/ $(LOCAL_PATH)include/ $(LOCAL_PATH)../ 

# We depends on csipsimple at this point because we need service to be stored somewhere
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-videodev/ \
	$(LOCAL_PATH)/../../../swig-glue/ \
	$(LOCAL_PATH)/../../../csipsimple-wrapper/include/

# Ffmpeg codec depend on ffmpeg
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../ffmpeg/ffmpeg_src/

# Pj implementation for renderer
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-videodev/webrtc_android_render_dev.cpp
# Pj implementation for capture
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-videodev/webrtc_android_capture_dev.cpp

# Ffmpeg codec
LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/ffmpeg_vid_codecs.c \
	$(PJLIB_SRC_DIR)/converter_libswscale.c \
	$(PJLIB_SRC_DIR)/ffmpeg_util.c \
	$(PJMEDIACODEC_SRC_DIR)/h263_packetizer.c \
	$(PJMEDIACODEC_SRC_DIR)/h264_packetizer.c \
	$(PJLIB_SRC_DIR)/vid_codec_util.c



# For render and capture
LOCAL_STATIC_LIBRARIES += libwebrtc_video_render libwebrtc_video_capture

# Common webrtc utility
LOCAL_STATIC_LIBRARIES += libwebrtc_yuv libyuv libwebrtc_apm_utility \
	libwebrtc_system_wrappers libwebrtc_spl


# Ffmpeg codec
BASE_FFMPEG_BUILD_DIR :=  $(LOCAL_PATH)/../../../ffmpeg/build/ffmpeg/$(TARGET_ARCH_ABI)/lib/
LOCAL_LDLIBS += $(BASE_FFMPEG_BUILD_DIR)/libavcodec.a \
		$(BASE_FFMPEG_BUILD_DIR)/libavformat.a \
		$(BASE_FFMPEG_BUILD_DIR)/libswscale.a \
		$(BASE_FFMPEG_BUILD_DIR)/libavutil.a

# Add X264	
BASE_X264_BUILD_DIR :=  $(LOCAL_PATH)/../../../ffmpeg/build/x264/$(TARGET_ARCH_ABI)/lib/
LOCAL_LDLIBS += $(BASE_X264_BUILD_DIR)/libx264.a
 
# Add ffmpeg to flags for pj part build
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DWEBRTC_ANDROID \
	-DPJMEDIA_HAS_FFMPEG=1 \
	-DPJMEDIA_HAS_FFMPEG_CODEC=1 \
	-DPJMEDIA_HAS_FFMPEG_CODEC_H264=1
	
	
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_LDLIBS += -lGLESv2 -llog

include $(BUILD_SHARED_LIBRARY)

## The screen capture backend

include $(CLEAR_VARS)

LOCAL_MODULE := pj_screen_capture_android


LOCAL_C_INCLUDES := $(LOCAL_PATH)../pjlib/include/ $(LOCAL_PATH)../pjlib-util/include/ \
	$(LOCAL_PATH)../pjsip/include/ \
	$(LOCAL_PATH)../pjnath/include/ $(LOCAL_PATH)include/ $(LOCAL_PATH)../ 

# We depends on csipsimple at this point because we need service to be stored somewhere
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-videodev/

# Pj implementation for capture
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-videodev/android_screen_capture_dev.c

LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

LOCAL_SHARED_LIBRARIES += libpjsipjni

include $(BUILD_SHARED_LIBRARY)

endif
