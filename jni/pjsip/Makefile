# This makefile aim is only for now to address source patch
patches_files := $(wildcard patches/*.diff)

all :
	# For now we do nothing to build - it's made by android_toolchain
	
patch : .patched_sources
	@# Patch done

.patched_sources : $(patches_files)
	@quilt push -a && \
	touch .patched_sources

unpatch :
	@if [ -f .patched_sources ]; then quilt pop -af; rm .patched_sources; fi;