#!/usr/bin/env bash
# Kendi bilgisayarında derle + imzala.
# Gereken: JDK 17+, Android SDK (ANDROID_HOME), gradle 8.7+
set -euo pipefail
cd "$(dirname "$0")"

: "${ANDROID_HOME:?ANDROID_HOME ayarla (Android SDK yolu)}"
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)

gradle :app:assembleRelease --no-daemon

[ -f release.jks ] || keytool -genkeypair -keystore release.jks -alias whatisthis \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass whatisthis123 -keypass whatisthis123 \
  -dname "CN=WHAT IS THIS, O=tdev, C=TR"

mkdir -p dist
"$BT"/zipalign -f -p 4 app/build/outputs/apk/release/app-release-unsigned.apk dist/aligned.apk
"$BT"/apksigner sign --ks release.jks --ks-key-alias whatisthis \
  --ks-pass pass:whatisthis123 --key-pass pass:whatisthis123 \
  --out dist/com.tdev.whatisthis.apk dist/aligned.apk
"$BT"/apksigner verify --verbose dist/com.tdev.whatisthis.apk
rm -f dist/aligned.apk
echo "Hazır: dist/com.tdev.whatisthis.apk"
