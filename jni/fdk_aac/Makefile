# This makefile aim is only for now to address source checkout and patch
fdk_tag := jb-release
fdk_url := https://android.googlesource.com/platform/external/aac

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	@git clone $(fdk_url) sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(fdk_tag)

update : sources
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(fdk_tag)
