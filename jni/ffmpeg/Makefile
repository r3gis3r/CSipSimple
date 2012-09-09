BUILD_DIRECTORY := build
FFMPEG_BUILD_DIRECTORY := $(BUILD_DIRECTORY)/ffmpeg
X264_BUILD_DIRECTORY := $(BUILD_DIRECTORY)/x264
FFMPEG_TEST_LIB := lib/libavcodec.a
X264_TEST_LIB := lib/libx264.a

ffmpeg_version := n0.10.4
x264_version := stable

# Get targets from Application.mk config file
include ../Application.mk
_local_mk := $(strip $(wildcard ../Local.mk))
ifdef _local_mk
include ../Local.mk
endif
TARGETS := $(APP_ABI) 

FFMPEG_LIBS := $(foreach TARGET, $(TARGETS),$(FFMPEG_BUILD_DIRECTORY)/$(TARGET)/$(FFMPEG_TEST_LIB))
X264_LIBS := $(foreach TARGET, $(TARGETS),$(X264_BUILD_DIRECTORY)/$(TARGET)/$(X264_TEST_LIB))

git_update=git fetch --tags; \
git checkout origin; \
git checkout $(1);

all : $(FFMPEG_LIBS) $(X264_LIBS)
	# All lib

$(X264_BUILD_DIRECTORY)/%/$(X264_TEST_LIB) : x264_src/configure
	./build_x264.sh $(subst /$(X264_TEST_LIB),,$(subst $(X264_BUILD_DIRECTORY)/,,$@))


$(FFMPEG_BUILD_DIRECTORY)/%/$(FFMPEG_TEST_LIB) : ffmpeg_src/VERSION $(X264_BUILD_DIRECTORY)/%/$(X264_TEST_LIB)
	./build_ffmpeg.sh $(subst /$(FFMPEG_TEST_LIB),,$(subst $(FFMPEG_BUILD_DIRECTORY)/,,$@))

# Source checkout
ffmpeg_src/VERSION :
	@git clone git://git.videolan.org/ffmpeg.git ffmpeg_src; \
	cd ffmpeg_src; \
	$(call git_update, $(ffmpeg_version))

x264_src/configure :
	@git clone git://git.videolan.org/x264.git x264_src; \
	cd x264_src; \
	$(call git_update, $(x264_version))

patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files) x264_src/configure ffmpeg_src/VERSION
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;

clean :
	@if [ -f ffmpeg_src/config.mak ]; then $(MAKE) -C ffmpeg_src clean; fi;
	@if [ -f x264_src/config.mak ]; then $(MAKE) -C x264_src clean; fi;
	@if [ -d build ]; then rm -rf build; fi;
	
update : ffmpeg_src/VERSION
	# FFmpeg
	@cd ffmpeg_src; \
	$(call git_update, $(ffmpeg_version))
	# X264
	@cd x264_src; \
	$(call git_update, $(x264_version))

