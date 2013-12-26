# This makefile aim is only for now to address source checkout and patch
opus_tag := v1.1

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	# old uri : https://git.xiph.org/opus.git
	@git clone git://git.opus-codec.org/opus.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(opus_tag)

update : sources
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(opus_tag)
