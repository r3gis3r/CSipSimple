pjsip_patches := $(wildcard jni/pjsip/patches/*.diff)
webrtc_patches := $(wildcard jni/webrtc/patches/*.diff)

openssl_tag := 1a3c5799337b90ddc56376ace7284a9e7f8cc988
zrtp4pj_tag := 10fe242813531daa61088af158b8b64c6fbe787e
opus_tag := v0.9.10

all : libraries
	# Dispatch to external projects
	@(./dispatch_shared_libs.sh)

libraries : ext-sources swig-glue
	# Build main libraries using android ndk
	@(ndk-build -j6)

ffmpeg-lib :
	# Build ffmpeg using make
	@($(MAKE) -C jni/ffmpeg $(MFLAGS))

ext-sources : jni/silk/sources jni/opus/sources jni/zrtp4pj/sources jni/openssl/sources jni/pjsip/.patched_sources jni/webrtc/.patched_sources
	# External sources fetched out from external repos/zip

swig-glue : 
	@($(MAKE) -C jni/swig-glue $(MFLAGS))


clean :
	# NDK clean
	@(ndk-build clean)
	# FFmpeg clean
	$(MAKE) -C jni/ffmpeg clean
	$(MAKE) -C jni/swig-glue clean

CodecPackLibs :
	@(ndk-build -j6 APP_MODULES="pj_g7221_codec pj_codec2_codec pj_g726_codec")
	@(./dispatch_shared_libs.sh)
	
VideoLibs : ffmpeg-lib
	@(ndk-build -j6 APP_MODULES="pj_video_android")
	@(./dispatch_shared_libs.sh)

ScreenSharingLibs :
	@(ndk-build -j6 APP_MODULES="pj_screen_capture_android")
	@(./dispatch_shared_libs.sh)

## External resources from repos/zip ##
jni/silk/sources :
	@cd jni/silk; \
	wget http://developer.skype.com/silk/SILK_SDK_SRC_v1.0.8.zip; \
	unzip -q SILK_SDK_SRC_v1.0.8.zip; \
	mv SILK_SDK_SRC_v1.0.8 sources; \
	rm SILK_SDK_SRC_v1.0.8.zip

jni/zrtp4pj/sources :
	@cd jni/zrtp4pj; \
	git clone git://github.com/r3gis3r/ZRTP4PJ.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(zrtp4pj_tag)

jni/openssl/sources :
	@cd jni/openssl; \
	git clone git://github.com/guardianproject/openssl-android.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)
	
jni/opus/sources :
	@cd jni/opus; \
	git clone https://git.xiph.org/opus.git sources; \
	cd sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(opus_tag)
	

## Patches against remote projects
jni/pjsip/.patched_sources : $(pjsip_patches)
	@cd jni/pjsip && \
	quilt push -a && \
	touch .patched_sources

jni/webrtc/.patched_sources : $(webrtc_patches)
	@cd jni/webrtc && \
	quilt push -a && \
	touch .patched_sources
	 

update :
	# Quilt removal
	@if [ -f jni/pjsip/.patched_sources ]; then cd jni/pjsip && quilt pop -af; rm .patched_sources; cd -; fi;
	@if [ -f jni/webrtc/.patched_sources ]; then cd jni/webrtc && quilt pop -af; rm .patched_sources; cd -; fi;
	# Svn update
	@svn update --accept theirs-conflict
	# Update ZRTP4pj
	@cd jni/zrtp4pj/sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(zrtp4pj_tag)
	# Update OpenSSL
	@cd jni/openssl/sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(openssl_tag)
	# Update libopus
	@cd jni/opus/sources; \
	git fetch --tags; \
	git checkout origin; \
	git checkout $(opus_tag)
	# Update ffmpeg
	$(MAKE) $(MFLAGS) -C jni/ffmpeg update
	