# This makefile aim is only for now to address source patch
patches_files := $(wildcard patches/*.diff)

NDK_CINCLUDES_ARMEABI_V7A := $(filter-out -c -o, $(filter -%,$(shell ndk-build -n -C ~/android/android-ndk-r8/samples/hello-jni NDK_LOG=1 APP_ABI=armeabi-v7a | grep gcc)))
NDK_GCC_ARMEABI_V7A := $(filter %gcc, $(shell ndk-build -n -C ~/android/android-ndk-r8/samples/hello-jni NDK_LOG=1 APP_ABI=armeabi-v7a | grep gcc))

AECM_NEON_DIR := sources/modules/audio_processing/aecm
AECM_NEON_GEN := $(AECM_NEON_DIR)/aecm_core_neon_offsets.h
AECM_NEON_INTERMEDIATE := $(AECM_NEON_GEN:%.h=%.S)
AECM_NEON_SRC := $(AECM_NEON_GEN:%.h=%.c)
AECM_NEON_INCLUDES := $(AECM_NEON_DIR)/include \
    $(AECM_NEON_DIR)/../utility \
    $(AECM_NEON_DIR)/../../.. \
    $(AECM_NEON_DIR)/../../../common_audio/signal_processing/include \
    $(AECM_NEON_DIR)/../../../system_wrappers/interface


all : 
	# For now we do nothing to build - it's made by android_toolchain

preprocess : $(AECM_NEON_GEN)
	# Pre process generation done

$(AECM_NEON_GEN) : sources/build/generate_asm_header.py $(AECM_NEON_INTERMEDIATE)
	python $^ $@ offset_aecm_

$(AECM_NEON_INTERMEDIATE) : $(AECM_NEON_SRC)
	$(NDK_GCC_ARMEABI_V7A) $(NDK_CINCLUDES_ARMEABI_V7A) $(patsubst %,-I%,$(AECM_NEON_INCLUDES)) -S -o $@ $^

patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files)
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;