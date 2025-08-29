#!/bin/bash

set -e

# Ellenőrizzük, hogy az Android SDK elérhető
if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "ANDROID_SDK_ROOT nincs beállítva!"
    exit 1
fi

# Gradle letöltése és telepítése a konténeren belül
if [ ! -f "/opt/gradle/bin/gradle" ]; then
    echo "🚀 Gradle letöltése..."
    wget -q https://services.gradle.org/distributions/gradle-7.5.1-bin.zip -O /tmp/gradle.zip
    unzip -o -q /tmp/gradle.zip -d /opt
    rm /tmp/gradle.zip
    ln -s /opt/gradle-7.5.1 /opt/gradle
fi

export PATH="/opt/gradle/bin:$PATH"

# Ellenőrizzük, hogy a Gradle elérhető-e
if ! command -v gradle &> /dev/null; then
    echo "❌ Gradle nem található! Kilépés."
    exit 1
fi

# Gradle Wrapper inicializálás (ha nem létezik)
echo "🚀 Gradle Wrapper inicializálása..."
/opt/gradle/bin/gradle wrapper
chmod +x gradlew
./gradlew clean 
echo "Cleaned!"
# Build és APK generálás
./gradlew assembleDebug --no-parallel --stacktrace
echo "-------XXXX--------"
echo $?
find / -name \*.apk
# Kimeneti APK másolása az output mappába
mkdir -p output
cp /app/app/build/outputs/apk/debug/app-debug.apk output/

echo "✅ Build sikeres! Az APK elérhető itt: output/app-debug.apk"