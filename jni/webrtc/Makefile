# This makefile aim is only for now to address source patch
patches_files := $(wildcard patches/*.diff)

NDK_ROOT := $(shell dirname $(shell which ndk-build))
NDK_CINCLUDES_ARMEABI_V7A := $(filter-out -c -o -MF, $(filter -%,$(shell ndk-build -n -C $(NDK_ROOT)/samples/hello-jni NDK_LOG=1 APP_ABI=armeabi-v7a | grep gcc)))
NDK_GCC_ARMEABI_V7A := $(filter %gcc, $(shell ndk-build -n -C $(NDK_ROOT)/samples/hello-jni NDK_LOG=1 APP_ABI=armeabi-v7a | grep gcc))

MY_ARM_CFLAGS_NEON := \
    -mfpu=neon \
    -mfloat-abi=softfp \
    -flax-vector-conversions

AECM_NEON_DIR := sources/modules/audio_processing/aecm
AECM_NEON_GEN := $(AECM_NEON_DIR)/aecm_core_neon_offsets.h
AECM_NEON_INTERMEDIATE := $(AECM_NEON_GEN:%.h=%.S)
AECM_NEON_SRC := $(AECM_NEON_GEN:%.h=%.c)
AECM_NEON_INCLUDES := $(AECM_NEON_DIR)/include \
    $(AECM_NEON_DIR)/../utility \
    $(AECM_NEON_DIR)/../../.. \
    $(AECM_NEON_DIR)/../../../common_audio/signal_processing/include \
    $(AECM_NEON_DIR)/../../../system_wrappers/interface

NS_NEON_DIR := sources/modules/audio_processing/ns
NS_NEON_GEN := $(NS_NEON_DIR)/nsx_core_neon_offsets.h
NS_NEON_INTERMEDIATE := $(NS_NEON_GEN:%.h=%.S)
NS_NEON_SRC := $(NS_NEON_GEN:%.h=%.c)
NS_NEON_INCLUDES := $(NS_NEON_DIR)/include \
    $(NS_NEON_DIR)/../utility \
    $(NS_NEON_DIR)/../../.. \
    $(NS_NEON_DIR)/../../../common_audio/signal_processing/include \
    $(NS_NEON_DIR)/../../../system_wrappers/interface

all : 
	# For now we do nothing to build - it's made by android_toolchain

preprocess : $(AECM_NEON_GEN) $(NS_NEON_GEN)
	# Pre process generation done

$(AECM_NEON_GEN) : sources/build/generate_asm_header.py $(AECM_NEON_INTERMEDIATE)
	@python $^ $@ offset_aecm_

$(AECM_NEON_INTERMEDIATE) : $(AECM_NEON_SRC)
	$(NDK_GCC_ARMEABI_V7A) $(NDK_CINCLUDES_ARMEABI_V7A) $(patsubst %,-I%,$(AECM_NEON_INCLUDES)) $(MY_ARM_CFLAGS_NEON) -S -o $@ $^
	
	
$(NS_NEON_GEN) : sources/build/generate_asm_header.py $(NS_NEON_INTERMEDIATE)
	@python $^ $@ offset_nsx_

$(NS_NEON_INTERMEDIATE) : $(NS_NEON_SRC)
	$(NDK_GCC_ARMEABI_V7A) $(NDK_CINCLUDES_ARMEABI_V7A) $(patsubst %,-I%,$(NS_NEON_INCLUDES)) $(MY_ARM_CFLAGS_NEON) -S -o $@ $^

patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files)
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;
	
	
clean :
	@rm -f $(NS_NEON_INTERMEDIATE)
	@rm -f $(AECM_NEON_INTERMEDIATE)
	@rm -f $(NS_NEON_GEN)
	@rm -f $(AECM_NEON_GEN)