#!/bin/bash
pushd `dirname $0`
TARGET_ARCH_ABI=$1
DEST=`pwd`/build/ffmpeg
ANDROID_API_LEVEL=9
minimal_featureset=1

NDK_ROOT=$(dirname $(which ndk-build))
ndk_gcc_res=$(ndk-build -n -C $NDK_ROOT/samples/hello-jni NDK_LOG=1 APP_ABI=$TARGET_ARCH_ABI APP_PLATFORM=android-$ANDROID_API_LEVEL | grep gcc)
NDK_CROSS_PREFIX=$(echo $ndk_gcc_res | awk '{ print $1 }' | sed 's/gcc//')
NDK_SYSROOT=$(echo $ndk_gcc_res | sed 's/^.*-I\([^ ]*\)\/usr\/include .*$/\1/')

FLAGS="--cross-prefix=$NDK_CROSS_PREFIX"

case "$TARGET_ARCH_ABI" in
	x86)
		FLAGS="$FLAGS --enable-pic --target-os=linux --arch=x86 --disable-asm "
		;;
	mips)
		FLAGS="$FLAGS --arch=mips --target-os=linux --disable-asm "
		;;
	*)
		FLAGS="$FLAGS --enable-pic --target-os=linux --arch=arm "
		;;
esac

pushd ffmpeg_src

FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"
FLAGS="$FLAGS --disable-shared --disable-symver"
FLAGS="$FLAGS --disable-everything"
#FLAGS="$FLAGS --enable-small"
FLAGS="$FLAGS --enable-gpl"
FLAGS="$FLAGS --enable-runtime-cpudetect"

# For h263
FLAGS="$FLAGS --enable-decoder=h263 --enable-encoder=h263"

#For x264
FLAGS="$FLAGS --enable-decoder=h264 --enable-encoder=libx264 --enable-parser=h264"
FLAGS="$FLAGS --enable-libx264"

case "$TARGET_ARCH_ABI" in
	neon)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp -mfpu=neon"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -Wl,--fix-cortex-a8"
		# Runtime choosing neon vs non-neon requires
		# renamed files
		ABI="armeabi-v7a-neon"
		;;
	armeabi-v7a)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp"
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I../build/x264/armeabi-v7a/include -I../build/vpx/armeabi-v7a/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L../build/x264/armeabi-v7a/lib -L../build/vpx/armeabi-v7a/lib"
		ABI="armeabi-v7a"
		;;
	x86)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I../build/x264/x86/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L../build/x264/x86/lib"
		ABI="x86"
		;;
	mips)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I../build/x264/mips/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L../build/x264/mips/lib"
		ABI="mips"
		;;
	*)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I../build/x264/armeabi/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L../build/x264/armeabi/lib"
		ABI="armeabi"
		;;
esac
DEST="$DEST/$ABI"
FLAGS="$FLAGS --prefix=$DEST"

mkdir -p $DEST
./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" | tee $DEST/configuration.txt
make clean
make -j4 || exit 1
make install || exit 1

popd; popd
