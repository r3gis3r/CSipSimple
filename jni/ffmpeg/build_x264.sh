#!/bin/bash
pushd `dirname $0`
TARGET_ARCH_ABI=$1
DEST=`pwd`/build/x264
ANDROID_API_LEVEL=9
minimal_featureset=1

NDK_ROOT=$(dirname $(which ndk-build))
ndk_gcc_res=$(ndk-build -n -C $NDK_ROOT/samples/hello-jni NDK_LOG=1 APP_ABI=$TARGET_ARCH_ABI APP_PLATFORM=android-$ANDROID_API_LEVEL | grep gcc)
NDK_CROSS_PREFIX=$(echo $ndk_gcc_res | awk '{ print $1 }' | sed 's/gcc//')
NDK_SYSROOT=$(echo $ndk_gcc_res | sed 's/^.*-I\([^ ]*\)\/usr\/include .*$/\1/')

FLAGS="--cross-prefix=$NDK_CROSS_PREFIX"

case "$TARGET_ARCH_ABI" in
	x86)
		FLAGS="$FLAGS --enable-pic --disable-asm "
		;;
	mips)
		FLAGS="$FLAGS --enable-pic --disable-asm "
		;;
	*)
		FLAGS="$FLAGS --enable-pic --host=arm-linux "
		;;
esac

pushd x264_src

FLAGS="$FLAGS --enable-static --disable-cli"
FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"

case "$TARGET_ARCH_ABI" in
	neon)
		EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		# Runtime choosing neon vs non-neon requires
		# renamed files
		ABI="armeabi-v7a-neon"
		;;
	armeabi-v7a)
		EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp"
		EXTRA_LDFLAGS=""
		ABI="armeabi-v7a"
		;;
	x86)
		EXTRA_CFLAGS=""
		EXTRA_LDFLAGS=""
		ABI="x86"
		;;
	mips)
		EXTRA_CFLAGS=""
		EXTRA_LDFLAGS=""
		ABI="mips"
		;;
	*)
		EXTRA_CFLAGS=""
		EXTRA_LDFLAGS=""
		ABI="armeabi"
		;;
esac
DEST="$DEST/$ABI"
FLAGS="$FLAGS --prefix=$DEST"

echo "Build in $FLAGS --extra-cflags=\"$EXTRA_CFLAGS\" --extra-ldflags=\"$EXTRA_LDFLAGS\""
mkdir -p $DEST
./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" | tee $DEST/configuration.txt
make clean
make -j4 || exit 1
make install || exit 1

popd; popd
