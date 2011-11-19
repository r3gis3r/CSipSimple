

all : libraries
	# Dispatch to external projects
	./dispatch_shared_libs.sh

libraries : ext-sources ext-lib swig-glue
	# Build main libraries using android ndk
	ndk-build -j4

ext-lib :
	# Build ffmpeg using make
	cd jni/ffmpeg; $(MAKE) $(MFLAGS)

ext-sources : jni/silk/sources jni/zrtp4pj/sources jni/openssl/sources
	# External sources fetched out from external repos/zip

swig-glue : 
	cd jni/swig-glue; $(MAKE) $(MFLAGS)

## External resources from repos/zip ##
jni/silk/sources :
	cd jni/silk; \
	wget http://developer.skype.com/silk/SILK_SDK_SRC_v1.0.8.zip; \
	unzip -q SILK_SDK_SRC_v1.0.8.zip; \
	mv SILK_SDK_SRC_v1.0.8 sources; \
	rm SILK_SDK_SRC_v1.0.8.zip

jni/zrtp4pj/sources :
	cd jni/zrtp4pj; \
	git clone git://github.com/r3gis3r/ZRTP4PJ.git sources

jni/openssl/sources :
	cd jni/openssl; \
	git clone git://github.com/guardianproject/openssl-android.git sources

clean :
	ndk-build clean