# This makefile aim is only for now to address source checkout and patch
openssl_tag := fa610339baf67da107ba040930352c974b38256f

all :
	# For now we do nothing to build - it's made by android_toolchain
	
init : sources
	# Init done

sources :
	@git clone https://github.com/android/platform_external_openssl.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)

update :
	@cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)