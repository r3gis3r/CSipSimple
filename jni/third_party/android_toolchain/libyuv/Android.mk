LOCAL_PATH := $(call my-dir)/../../libyuv

include $(CLEAR_VARS)


LOCAL_MODULE    := libyuv
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

LOCAL_CPP_EXTENSION := .cc


LOCAL_C_INCLUDES += $(LOCAL_PATH)/include

LOCAL_SRC_FILES := \
    source/compare.cc \
    source/convert.cc \
    source/convert_from.cc \
    source/convert_from_argb.cc \
    source/convert_argb.cc \
    source/cpu_id.cc \
    source/format_conversion.cc \
    source/planar_functions.cc \
    source/rotate.cc \
    source/rotate_argb.cc \
    source/row_common.cc \
    source/row_posix.cc \
    source/scale.cc \
    source/scale_argb.cc \
    source/video_common.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DLIBYUV_NEON
    LOCAL_SRC_FILES += \
        source/compare_neon.cc.neon \
        source/rotate_neon.cc.neon \
        source/row_neon.cc.neon \
        source/scale_neon.cc.neon
endif
#LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_STATIC_LIBRARY)

#$(call import-module,android/cpufeatures)