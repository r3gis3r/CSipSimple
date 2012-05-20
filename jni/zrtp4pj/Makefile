# This makefile aim is only for now to address source checkout and patch
zrtp4pj_tag := 1.1.0
#10fe242813531daa61088af158b8b64c6fbe787e

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	@git clone git://github.com/r3gis3r/ZRTP4PJ.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(zrtp4pj_tag); \
	cd zsrtp; \
	sh ./getzrtp.sh

update : sources
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(zrtp4pj_tag); \
	cd zsrtp; \
	sh ./getzrtp.sh