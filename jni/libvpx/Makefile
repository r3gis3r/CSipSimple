BUILD_DIRECTORY := build

patches_files := $(wildcard patches/*.diff)
libvpx_tag := v1.1.0
NDK_ROOT := $(shell dirname $(shell which ndk-build))

# Get targets from Application.mk config file
include ../Application.mk
_local_mk := $(strip $(wildcard ../Local.mk))
ifdef _local_mk
include ../Local.mk
endif


TARGETS := $(APP_ABI)

VPX_CONFIGURES := $(foreach TARGET, $(TARGETS),$(BUILD_DIRECTORY)/$(TARGET)/config.mk)

all : 
	# For now we do nothing to build - it's made by android_toolchain
	
init : $(VPX_CONFIGURES)
	# Preconfigure for each done

preprocess : $(VPX_CONFIGURES)
	# Preconfigure for each done

$(BUILD_DIRECTORY)/%/config.mk : sources .patched_sources
	@mkdir -p $(subst /config.mk,,$@)
	cd $(subst /config.mk,,$@); \
	if [ "$(BUILD_DIRECTORY)/armeabi/config.mk" = "$@" ]; then TGT=armv5te-android-gcc; fi; \
	if [ "$(BUILD_DIRECTORY)/armeabi-v7a/config.mk" = "$@" ]; then TGT=armv7-android-gcc; fi; \
	if [ "$(BUILD_DIRECTORY)/x86/config.mk" = "$@" ]; then TGT=x86-android-gcc; fi; \
	if [ "$(BUILD_DIRECTORY)/mips/config.mk" = "$@" ]; then TGT=mips32-android-gcc; fi; \
	BUILD_PFX=jni/libvpx/$(subst config.mk,,$@) ../../sources/configure --target=$$TGT --disable-examples --sdk-path=$(NDK_ROOT)

sources :
	@ git clone https://code.google.com/p/webm.libvpx sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(libvpx_tag);


update : sources
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(libvpx_tag);
	
	
patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files)
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;
	
clean :
	@rm -rf $(BUILD_DIRECTORY)
