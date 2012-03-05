all : libraries

libraries : libraries-armeabi #libraries-armeabi-v7a

libraries-armeabi : build/ffmpeg/armeabi/lib/libavcodec.a
#libraries-armeabi-v7a : build/ffmpeg/armeabi-v7a/lib/libavcodec.a

build/ffmpeg/armeabi/lib/libavcodec.a : ffmpeg_src build/x264/armeabi/lib/libx264.a
	./build_ffmpeg.sh armeabi

#build/ffmpeg/armeabi-v7a/lib/libavcodec.a : ffmpeg_src build/x264/armeabi-v7a/lib
#	./build_ffmpeg.sh armv7a

build/x264/armeabi/lib/libx264.a : x264_src
	./build_x264.sh armeabi

#build/x264/armeabi-v7a/lib : x264_src
#	./build_x264.sh armv7a


# Source checkout
ffmpeg_src :
	git clone git://git.videolan.org/ffmpeg.git ffmpeg_src; \
	cd ffmpeg_src; \
	git fetch --tags; \
	git checkout origin; \
	git checkout n0.10

x264_src :
	git clone git://git.videolan.org/x264.git x264_src
	# cd x264_src; \
	# git fetch --tags; \
	# git checkout origin; \
	# git checkout 10fe242813531daa61088af158b8b64c6fbe787e


clean :
	rm -r build
	
update :
	# FFmpeg
	cd ffmpeg_src; \
	git fetch --tags; \
	git checkout origin; \
	git checkout n0.10
	# X264
	# cd x264_src; \
	# git checkout origin; \
	# git checkout 10fe242813531daa61088af158b8b64c6fbe787e
