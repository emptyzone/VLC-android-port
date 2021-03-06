#!/bin/sh

if [ -z "$ANDROID_NDK" -o -z "$ANDROID_ABI" ]; then
    echo "Please set the ANDROID_NDK environment variable with its path.\n"
    echo "ANDROID_ABI should match your ABI: armeabi-v7a, armeabi or ..."
    exit 1
fi

# Must use android-9 here. Any replacement functions needed are in the vlc-android/jni
# folder.
ANDROID_API=android-9

VLC_SOURCEDIR=..

CFLAGS="-g -O2 -mlong-calls -fstrict-aliasing -funsafe-math-optimizations"
LDFLAGS="-Wl,-Bdynamic,-dynamic-linker=/system/bin/linker -Wl,--no-undefined"

if [ -z "$NO_NEON" ]; then
    EXTRA_PARAMS=" --enable-neon"
    LDFLAGS="$LDFLAGS -Wl,--fix-cortex-a8"
else
    EXTRA_PARAMS=" --disable-neon"
fi

CPPFLAGS="-I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/include -I${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/libs/${ANDROID_ABI}/include"
LDFLAGS="$LDFLAGS -L${ANDROID_NDK}/sources/cxx-stl/gnu-libstdc++/libs/${ANDROID_ABI}"

SYSROOT=$ANDROID_NDK/platforms/$ANDROID_API/arch-arm
ANDROID_BIN=$ANDROID_NDK/toolchains/arm-linux-androideabi-4.4.3/prebuilt/*-x86/bin/
CROSS_COMPILE=${ANDROID_BIN}/arm-linux-androideabi-

CPPFLAGS="$CPPFLAGS" \
CFLAGS="$CFLAGS ${VLC_EXTRA_CFLAGS}" \
CXXFLAGS="$CFLAGS" \
LDFLAGS="$LDFLAGS" \
CC="${CROSS_COMPILE}gcc --sysroot=${SYSROOT}" \
CXX="${CROSS_COMPILE}g++ --sysroot=${SYSROOT}" \
NM="${CROSS_COMPILE}nm" \
STRIP="${CROSS_COMPILE}strip" \
RANLIB="${CROSS_COMPILE}ranlib" \
AR="${CROSS_COMPILE}ar" \
sh $VLC_SOURCEDIR/configure --host=arm-linux-androideabi --build=x86_64-unknown-linux $EXTRA_PARAMS \
                --enable-live555 --enable-realrtsp \
                --enable-avformat \
                --enable-swscale \
                --enable-avcodec \
                --enable-opensles \
                --enable-audiotrack \
                --enable-android-surface \
                --enable-mkv \
                --enable-taglib \
                --enable-iomx \
                --disable-vlc --disable-shared \
                --disable-vlm \
                --disable-dbus \
                --disable-lua \
                --disable-vcd \
                --disable-v4l2 \
                --disable-gnomevfs \
                --disable-dvdread \
                --disable-dvdnav \
                --disable-bluray \
                --disable-linsys \
                --disable-decklink \
                --disable-libva \
                --disable-dv1394 \
                --disable-mod \
                --disable-sid \
                --disable-gme \
                --disable-tremor \
                --disable-mad \
                --disable-dca \
                --disable-sdl-image \
                --disable-zvbi \
                --disable-fluidsynth \
                --disable-jack \
                --disable-pulse \
                --disable-alsa \
                --disable-samplerate \
                --disable-sdl \
                --disable-xcb \
                --disable-atmo \
                --disable-qt4 \
                --disable-skins2 \
                --disable-mtp \
                --disable-notify \
                --enable-libass \
                --disable-svg \
                --disable-sqlite \
                --disable-udev \
                --enable-libxml2 \
                --disable-caca \
                --disable-glx \
                --disable-egl \
                --disable-goom \
                --disable-projectm \
                --disable-sout \
                --disable-vorbis \
                --disable-x264 \
                --disable-schroedinger --disable-dirac \
                $*
