#!/bin/bash
pushd `dirname $0`
VERSION=$1
DEST=`pwd`/build/x264

case "$VERSION" in
	x86)
		. settings_x86.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/i686-android-linux- --enable-pic --disable-asm "
		;;
	mips)
		. settings_mips.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/mipsel-linux-android- --enable-pic --disable-asm "
		;;
	*)
		. settings.sh
		FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/arm-linux-androideabi- --enable-pic --host=arm-linux "
		;;
esac

pushd x264_src

FLAGS="$FLAGS --enable-static --disable-cli"
FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"

case "$VERSION" in
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
