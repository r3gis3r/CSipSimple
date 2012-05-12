# This makefile aim is only for now to address source checkout and patch
openssl_tag := 1a3c5799337b90ddc56376ace7284a9e7f8cc988

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	@git clone git://github.com/guardianproject/openssl-android.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)

update :
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)