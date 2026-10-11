#!/usr/bin/env bash
set -euo pipefail
adb wait-for-device
adb shell getprop sys.boot_completed
find app/build/outputs/apk/gplay/release -name '*.apk' -print
APK=$(find app/build/outputs/apk/gplay/release -name '*x86_64*release.apk' | head -1)
test -n "$APK"
adb install -r "$APK"
PKG=$(apkanalyzer manifest application-id "$APK")
adb shell monkey -p "$PKG" 1
sleep 20
adb logcat -d -v time > /tmp/syncme-emulator-logcat.txt
if grep -E 'FATAL EXCEPTION|UnsatisfiedLinkError|NoClassDefFoundError' /tmp/syncme-emulator-logcat.txt | grep -v 'com.google.android'; then
  echo 'Potential app crash in emulator logcat'
  exit 1
fi
adb shell pidof "$PKG"
