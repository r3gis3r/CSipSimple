#!/bin/bash
pushd `dirname $0`
TARGET_ARCH_ABI=$1
DEST=`pwd`/build/ffmpeg
ANDROID_API_LEVEL=9
minimal_featureset=1
USE_STAGEFRIGHT=0


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
	    FLAGS="$FLAGS --target-os=linux --arch=mips --disable-asm "
	    ;;
        armeabi-v7a)
            FLAGS="$FLAGS --enable-pic --target-os=linux --arch=arm --cpu=armv7-a"
            ;;
	armeabi)
	    FLAGS="$FLAGS --enable-pic --target-os=linux --arch=arm "
	    ;;
esac

pushd ffmpeg_src

ANDROID_SOURCE=./android-source
ANDROID_LIBS=./android-libs

# stagefright only for armeabi-v7a
if [[ "$TARGET_ARCH_ABI" != "armeabi-v7a" ]]; then
USE_STAGEFRIGHT=0
fi

if [ $USE_STAGEFRIGHT -eq 1 ]; then

if [ ! -d "$ANDROID_SOURCE/frameworks/base" ]; then 
    echo "Fetching Android system base headers"
    git clone --depth=1 --branch gingerbread-release git://github.com/CyanogenMod/android_frameworks_base.git $ANDROID_SOURCE/frameworks/base
fi
if [ ! -d "$ANDROID_SOURCE/system/core" ]; then
    echo "Fetching Android system core headers"
    git clone --depth=1 --branch gingerbread-release git://github.com/CyanogenMod/android_system_core.git $ANDROID_SOURCE/system/core
fi
if [ ! -d "$ANDROID_LIBS" ]; then
    # Libraries from any froyo/gingerbread device/emulator should work
    # fine, since the symbols used should be available on most of them.
    echo "Fetching Android libraries for linking"
    if [ ! -f "./update-cm-7.0.3-N1-signed.zip" ]; then
        wget http://download.cyanogenmod.com/get/update-cm-7.0.3-N1-signed.zip -P./
    fi
    unzip ./update-cm-7.0.3-N1-signed.zip system/lib/* -d./
    mv ./system/lib $ANDROID_LIBS
    rmdir ./system
fi

fi


# actual build
FLAGS="$FLAGS --sysroot=$NDK_SYSROOT"
FLAGS="$FLAGS --disable-shared --disable-symver"
FLAGS="$FLAGS --disable-everything"
#FLAGS="$FLAGS --enable-small"
FLAGS="$FLAGS --enable-gpl"
FLAGS="$FLAGS --enable-runtime-cpudetect"

# For h263
FLAGS="$FLAGS --enable-decoder=h263 --enable-encoder=h263"

#For x264
FLAGS="$FLAGS --enable-encoder=libx264 --enable-parser=h264"
FLAGS="$FLAGS --enable-libx264"
if [ $USE_STAGEFRIGHT -eq 1 ]; then
FLAGS="$FLAGS --enable-libstagefright-h264 --enable-decoder=libstagefright_h264"
else
FLAGS="$FLAGS --enable-decoder=h264"
fi

X264_LIBS=../build/x264/$TARGET_ARCH_ABI/lib
X264_INCLUDES=../build/x264/$TARGET_ARCH_ABI/include

ABI=$TARGET_ARCH_ABI
case "$TARGET_ARCH_ABI" in
	neon)
        # --- THIS IS NEVER USED FOR NOW ---
		EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp -mfpu=neon"
		EXTRA_LDFLAGS="$EXTRA_LDFLAGS -Wl,--fix-cortex-a8"
		# Runtime choosing neon vs non-neon requires
		# renamed files
		ABI="armeabi-v7a-neon"
		;;
	armeabi-v7a)
		EXTRA_CFLAGS="$EXTRA_CFLAGS -march=armv7-a -mfloat-abi=softfp"
		;;
esac

DEST="$DEST/$ABI"
FLAGS="$FLAGS --prefix=$DEST"

# CXX
EXTRA_CXXFLAGS="-Wno-multichar -fno-exceptions -fno-rtti"

# X264 libs and includes
EXTRA_CFLAGS="$EXTRA_CFLAGS -DANDROID -D__thumb__ -mthumb -I$X264_INCLUDES"
EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$X264_LIBS -Wl,-rpath-link,$X264_LIBS"

# Stagefright
if [ $USE_STAGEFRIGHT -eq 1 ]; then
    EXTRA_CFLAGS="$EXTRA_CFLAGS -I$ANDROID_SOURCE/frameworks/base/include -I$ANDROID_SOURCE/system/core/include"
    EXTRA_CFLAGS="$EXTRA_CFLAGS -I$ANDROID_SOURCE/frameworks/base/media/libstagefright"
    EXTRA_CFLAGS="$EXTRA_CFLAGS -I$ANDROID_SOURCE/frameworks/base/include/media/stagefright/openmax"
    EXTRA_CFLAGS="$EXTRA_CFLAGS -I$NDK_ROOT/sources/cxx-stl/system/include"
    EXTRA_LDFLAGS="$EXTRA_LDFLAGS -L$ANDROID_LIBS -Wl,-rpath-link,$ANDROID_LIBS"
fi


mkdir -p $DEST
./configure $FLAGS --extra-cflags="$EXTRA_CFLAGS" --extra-ldflags="$EXTRA_LDFLAGS" --extra-cxxflags="$EXTRA_CXXFLAGS" | tee $DEST/configuration.txt
make clean
make -j4 || exit 1
make install || exit 1

popd; popd
