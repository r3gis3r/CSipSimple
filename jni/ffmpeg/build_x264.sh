#!/bin/bash
pushd `dirname $0`
. settings.sh


pushd x264_src
DEST=`pwd`/../build/x264
VERSION=$1


FLAGS="--cross-prefix=$NDK_TOOLCHAIN_BASE/bin/arm-linux-androideabi- --enable-pic --host=arm-linux "
FLAGS="$FLAGS --enable-static --disable-cli"
FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"

case "$VERSION" in
	neon)
		EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon"
		EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
		# Runtime choosing neon vs non-neon requires
		# renamed files
		ABI="armeabi-v7a"
		;;
	armv7a)
		EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp"
		EXTRA_LDFLAGS=""
		ABI="armeabi-v7a"
		;;
	*)
		EXTRA_CFLAGS=""
		EXTRA_LDFLAGS=""
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
