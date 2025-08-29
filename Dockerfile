# ------------------------------------------------------------
# Android APK builder (clean) — a .task modellt lokálisan adod
# ------------------------------------------------------------
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Csak ami kell az Android buildhez
RUN apt-get update && apt-get install -y \
  openjdk-17-jdk \
  wget unzip curl ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# ---------- Android SDK ----------
ENV ANDROID_SDK_ROOT=/opt/android-sdk
RUN mkdir -p $ANDROID_SDK_ROOT && \
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip && \
  mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
  unzip -qo /tmp/tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
  mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
  rm /tmp/tools.zip

ENV PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# SDK komponensek
RUN yes | sdkmanager --sdk_root=$ANDROID_SDK_ROOT --licenses && \
  sdkmanager --sdk_root=$ANDROID_SDK_ROOT \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "cmdline-tools;latest"

# ---------- Gradle 8.7 ----------
RUN wget -q https://services.gradle.org/distributions/gradle-8.7-bin.zip -O /tmp/gradle.zip && \
  unzip -qo /tmp/gradle.zip -d /opt && rm /tmp/gradle.zip
ENV PATH="/opt/gradle-8.7/bin:$PATH"

# ---------- Projekt ----------
WORKDIR /app
COPY . .

# Gradle wrapper beállítás
RUN if [ -f gradle/wrapper/gradle-wrapper.properties ]; then \
      sed -i 's#^distributionUrl=.*#distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip#' gradle/wrapper/gradle-wrapper.properties ; \
    fi && \
    if [ -f gradlew ]; then chmod +x gradlew ; else /opt/gradle-8.7/bin/gradle wrapper --gradle-version 8.7 --distribution-type bin --no-daemon ; fi

# ---------- Assets + minimal prepare_model.sh ----------
RUN mkdir -p /app/app/src/main/assets && \
printf '%s\n' \
'#!/usr/bin/env bash' \
'set -euo pipefail' \
'' \
'# Forrás modell:' \
'# 1) Elsődleges: /models alatt egy *.task (közvetlen bind mount)' \
'# 2) Alternatíva: MODEL_URL környezeti változóval egy közvetlen URL-ről (curl)' \
'' \
'MODEL_DIR="${MODEL_DIR:-/models}"' \
'ASSETS_DIR="/app/app/src/main/assets"' \
'OUT_NAME="${MODEL_NAME:-gemma3-270m-it-q8.task}"' \
'MODEL_URL="${MODEL_URL:-}"' \
'' \
'mkdir -p "$ASSETS_DIR"' \
'' \
'if ls "$MODEL_DIR"/*.task >/dev/null 2>&1; then' \
'  SRC_TASK="$(ls "$MODEL_DIR"/*.task | head -n1)"' \
'  echo "📦 Helyi .task talált: $SRC_TASK"' \
'  cp "$SRC_TASK" "$ASSETS_DIR/$OUT_NAME"' \
'  echo "✅ Kész: $ASSETS_DIR/$OUT_NAME"' \
'  exit 0' \
'fi' \
'' \
'if [ -n "$MODEL_URL" ]; then' \
'  echo "🌐 Letöltés URL-ről: $MODEL_URL"' \
'  curl -fL "$MODEL_URL" -o "$ASSETS_DIR/$OUT_NAME"' \
'  echo "✅ Kész: $ASSETS_DIR/$OUT_NAME"' \
'  exit 0' \
'fi' \
'' \
'echo "❌ Nincs modell. Add át bind mounttal: -v /abszolut/ut/a/taskokhoz:/models (vagy állíts be MODEL_URL-t)."' \
'exit 1' \
> /usr/local/bin/prepare_model.sh && chmod +x /usr/local/bin/prepare_model.sh

CMD ["/bin/bash"]

