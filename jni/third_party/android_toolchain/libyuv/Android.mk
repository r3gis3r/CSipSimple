LOCAL_PATH := $(call my-dir)/../../libyuv

include $(CLEAR_VARS)


LOCAL_MODULE    := libyuv
LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)

LOCAL_CPP_EXTENSION := .cc


LOCAL_C_INCLUDES += $(LOCAL_PATH)/include $(LOCAL_PATH)/source

LOCAL_SRC_FILES := source/compare.cc \
	source/convert.cc \
	source/convert_argb.cc \
	source/convert_from.cc \
	source/cpu_id.cc \
	source/format_conversion.cc \
	source/planar_functions.cc \
	source/rotate_argb.cc \
	source/rotate.cc \
	source/row_common.cc \
	source/scale.cc \
	source/scale_argb.cc \
	source/video_common.cc 
	
	
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_ARM_NEON  := true
	LOCAL_SRC_FILES += source/row_neon.cc.neon source/rotate_neon.cc.neon
else
	LOCAL_SRC_FILES += source/row_posix.cc
endif

#LOCAL_STATIC_LIBRARIES := cpufeatures

include $(BUILD_STATIC_LIBRARY)

#$(call import-module,android/cpufeatures)