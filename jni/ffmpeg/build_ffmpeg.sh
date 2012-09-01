#!/bin/bash
pushd `dirname $0`
VERSION=$1
DEST=`pwd`/build/ffmpeg

case "$VERSION" in
	x86)
		. settings_x86.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/i686-linux-android- --enable-pic --target-os=linux --arch=x86 --disable-asm "
		;;
	mips)
		. settings_mips.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/mipsel-linux-android- --enable-pic --target-os=linux --arch=mips --disable-asm "
		;;
	*)
		. settings.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/arm-linux-androideabi- --enable-pic --target-os=linux --arch=arm "
		;;
esac

pushd ffmpeg_src

FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"
FLAGS="$FLAGS --disable-shared --disable-symver"
FLAGS="$FLAGS --disable-everything"
FLAGS="$FLAGS --enable-small"
FLAGS="$FLAGS --enable-gpl"
FLAGS="$FLAGS --enable-decoder=h263 --enable-encoder=h263"
FALGS="$FLAGS --enable-runtime-cpudetect"

#For x264
FLAGS="$FLAGS --enable-decoder=h264 --enable-encoder=h264 --enable-parser=h264"
FLAGS="$FLAGS --enable-encoder=libx264 --enable-libx264"

case "$VERSION" in
	neon)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp -mfpu=neon"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -Wl,--fix-cortex-a8"
		# Runtime choosing neon vs non-neon requires
		# renamed files
		ABI="armeabi-v7a"
		;;
	armeabi-v7a)
		#EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp"
		EXTRA_CFLAGS="$EXTRA_CFLAGS -I../build/x264/armeabi-v7a/include"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L../build/x264/armeabi-v7a/lib"
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
