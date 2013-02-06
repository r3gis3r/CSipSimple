TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_PATH := $(TOOLCHAIN_PATH)/../../sources/pjmedia

ifeq ($(MY_USE_VIDEO),1)
# Video capture/render implementation
include $(CLEAR_VARS)

LOCAL_MODULE := pj_video_android

PJ_ANDROID_SRC_DIR := ../../android_sources/pjmedia/src
PJLIB_SRC_DIR := src/pjmedia
PJMEDIADEV_SRC_DIR := src/pjmedia-audiodev
PJMEDIADEV_VIDEO_SRC_DIR := src/pjmedia-videodev
PJMEDIACODEC_SRC_DIR := src/pjmedia-codec

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../webrtc/sources/modules/video_render/main/interface \
	$(LOCAL_PATH)/../../../webrtc/sources/modules/video_capture/main/interface \
	$(LOCAL_PATH)/../../../webrtc/sources/modules/interface \
	$(LOCAL_PATH)/../../../webrtc/sources/system_wrappers/interface \
	$(LOCAL_PATH)/../../../webrtc/sources/modules \
	$(LOCAL_PATH)/../../../webrtc/sources \
	$(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/../pjlib-util/include \
	$(LOCAL_PATH)/../pjsip/include \
	$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/include $(LOCAL_PATH)/.. 

# We depends on csipsimple at this point because we need service to be stored somewhere
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-videodev \
	$(LOCAL_PATH)/../../../swig-glue \
	$(LOCAL_PATH)/../../../csipsimple-wrapper/include

# Ffmpeg codec depend on ffmpeg
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../ffmpeg/ffmpeg_src

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
BASE_FFMPEG_BUILD_DIR :=  $(LOCAL_PATH)/../../../ffmpeg/build/ffmpeg/$(TARGET_ARCH_ABI)/lib
LOCAL_LDLIBS += $(BASE_FFMPEG_BUILD_DIR)/libavcodec.a \
		$(BASE_FFMPEG_BUILD_DIR)/libavformat.a \
		$(BASE_FFMPEG_BUILD_DIR)/libswscale.a \
		$(BASE_FFMPEG_BUILD_DIR)/libavutil.a

# Add X264	
BASE_X264_BUILD_DIR :=  $(LOCAL_PATH)/../../../ffmpeg/build/x264/$(TARGET_ARCH_ABI)/lib
LOCAL_LDLIBS += $(BASE_X264_BUILD_DIR)/libx264.a
 
# Add ffmpeg to flags for pj part build
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS) -DWEBRTC_ANDROID \
	-DPJMEDIA_HAS_FFMPEG=1 \
	-DPJMEDIA_HAS_FFMPEG_CODEC=1 \
	-DPJMEDIA_HAS_FFMPEG_CODEC_H264=1
	
	
LOCAL_SHARED_LIBRARIES += libpjsipjni
LOCAL_LDLIBS += -lGLESv2 -llog
LOCAL_STATIC_LIBRARIES += libgcc cpufeatures

USE_STAGEFRIGHT_H264:=0
ANDROID_LIBS := ./jni/ffmpeg/ffmpeg_src/android-libs
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
ifeq ($(USE_STAGEFRIGHT_H264),1)
	LOCAL_LDLIBS += -L$(ANDROID_LIBS) -Wl,-rpath-link,$(ANDROID_LIBS) -lstagefright -lutils -lbinder
endif
endif

include $(BUILD_SHARED_LIBRARY)

endif