#! /bin/sh

OLD_NAME=org.videolan.vlc
NEW_NAME=org.videolan.vlc.$1

OLD_PATH=$(echo $OLD_NAME |sed 's/\./\//g')
NEW_PATH=$(echo $NEW_NAME |sed 's/\./\//g')

mv vlc-android/src/${OLD_PATH} vlc-android/src/tmp
mkdir -p vlc-android/src/${OLD_PATH}
mv vlc-android/src/tmp vlc-android/src/${NEW_PATH}

find vlc-android \( -name "*.xml" -o -name "*.java" -o -name "*.cfg" -o -name "*.aidl" \) -print0 | xargs -0 sed -ri "s/${OLD_NAME}/${NEW_NAME}/g"
sed -ri "s,${OLD_PATH},${NEW_PATH},g" Makefile

