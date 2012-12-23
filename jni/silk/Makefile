# This makefile aim is only for now to address source checkout and patch
patches_files := $(wildcard patches/*.diff)
silk_version := v1.0.9
silk_remote_folder := SILK_SDK_SRC_$(silk_version)
silk_repo := https://developer.skype.com/silk/$(silk_remote_folder).zip

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done


sources : sources/SILK_SDK_SRC_ARM_$(silk_version)
	# source checkout done
	
sources/SILK_SDK_SRC_ARM_$(silk_version) :
	@wget $(silk_repo); \
	unzip -d $< sources -q $(silk_remote_folder).zip; \
	rm $(silk_remote_folder).zip

update : sources/SILK_SDK_SRC_ARM_$(silk_version)
	# managed by sources
	
patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files)
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;