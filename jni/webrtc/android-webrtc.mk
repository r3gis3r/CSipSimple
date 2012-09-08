# Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.
intermediates := /tmp/$(TARGET_ARCH_ABI)
$(shell mkdir -p $(intermediates))

# These defines will apply to all source files
# Think again before changing it
MY_WEBRTC_COMMON_DEFS := \
    -DWEBRTC_TARGET_PC \
    -DWEBRTC_LINUX \
    -DWEBRTC_THREAD_RR \
    -DWEBRTC_CLOCK_TYPE_REALTIME \
    -DWEBRTC_ANDROID
#    The following macros are used by modules,
#    we might need to re-organize them
#    '-DWEBRTC_ANDROID_OPENSLES' [module audio_device]
#    '-DNETEQ_VOICEENGINE_CODECS' [module audio_coding neteq]
#    '-DWEBRTC_MODULE_UTILITY_VIDEO' [module media_file] [module utility]



ifeq ($(TARGET_ARCH),arm)
MY_WEBRTC_COMMON_DEFS += \
    -DWEBRTC_ARCH_ARM

# Transform NDK vars to Android Build Vars
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
ARCH_ARM_HAVE_ARMV7A := true
MY_WEBRTC_COMMON_DEFS += \
    -DWEBRTC_ARCH_ARM_V7A \
    -DWEBRTC_DETECT_ARM_NEON
else
ARCH_ARM_HAVE_ARMV7A := false
endif


ifneq (,$(filter -DWEBRTC_DETECT_ARM_NEON -DWEBRTC_ARCH_ARM_NEON, \
    $(MY_WEBRTC_COMMON_DEFS)))
WEBRTC_BUILD_NEON_LIBS := true
# TODO(kma): Use MY_ARM_CFLAGS_NEON for Neon libraies in AECM, NS, and iSAC.
MY_ARM_CFLAGS_NEON := \
    -mfpu=neon \
    -mfloat-abi=softfp \
    -flax-vector-conversions
else
WEBRTC_BUILD_NEON_LIBS := false
endif


endif # ifeq ($(TARGET_ARCH),arm)


# Disable not wanted (for now) codecs
# This disable engine internal auto configuration and replace by ower settings
MY_WEBRTC_COMMON_DEFS += \
	-DWEBRTC_ENGINE_CONFIGURATIONS_H_ \
	-DWEBRTC_CODEC_ILBC 

#L16 is useless -DWEBRTC_CODEC_PCM16
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a))
MY_WEBRTC_COMMON_DEFS += -DWEBRTC_CODEC_ISACFX
else
MY_WEBRTC_COMMON_DEFS += -DWEBRTC_CODEC_ISAC
endif

