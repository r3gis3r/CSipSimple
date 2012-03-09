#!/bin/bash

# set the base path to your Android NDK (or export NDK to environment)


if [[ "x$ANDROID_NDK" == "x" ]]; then
    NDK_BUILD_PATH=$(which ndk-build)
    ANDROID_NDK=$(dirname $NDK_BUILD_PATH)
    echo "No ANDROID_NDK set, using $ANDROID_NDK"
fi

NDK_PLATFORM_VERSION=9
NDK_SYSROOT=$ANDROID_NDK/platforms/android-$NDK_PLATFORM_VERSION/arch-x86
NDK_UNAME=`uname -s | tr '[A-Z]' '[a-z]'`
NDK_TOOLCHAIN_BASE=$ANDROID_NDK/toolchains/x86-4.4.3/prebuilt/$NDK_UNAME-x86
CC="$NDK_TOOLCHAIN_BASE/bin/i686-android-linux-gcc --sysroot=$NDK_SYSROOT"
LD=$NDK_TOOLCHAIN_BASE/bin/i686-android-linux-ld

# i use only a small number of formats - set this to 0 if you want everything.
# changed 0 to the default, so it'll compile shitloads of codecs normally
if [[ "x$minimal_featureset" == "x" ]]; then
minimal_featureset=1
fi

function current_dir {
  echo "$(cd "$(dirname $0)"; pwd)"
}

