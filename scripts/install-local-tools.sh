#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOWNLOAD_DIR="$ROOT_DIR/.dev/downloads"
TOOLS_DIR="$ROOT_DIR/.dev/tools"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.19%2B10/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.19_10.tar.gz"
MAVEN_VERSION="3.9.16"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

mkdir -p "$DOWNLOAD_DIR" "$TOOLS_DIR"

if ! compgen -G "$TOOLS_DIR/jdk-17*/Contents/Home/bin/java" >/dev/null; then
  echo "Downloading JDK 17..."
  curl -L --fail --retry 10 --connect-timeout 20 --max-time 1800 -C - \
    -o "$DOWNLOAD_DIR/jdk17.tar.gz" "$JDK_URL"
  tar -xzf "$DOWNLOAD_DIR/jdk17.tar.gz" -C "$TOOLS_DIR"
else
  echo "JDK 17 already exists in .dev/tools."
fi

if ! compgen -G "$TOOLS_DIR/apache-maven-*/bin/mvn" >/dev/null; then
  echo "Downloading Maven ${MAVEN_VERSION}..."
  curl -L --fail --retry 10 --connect-timeout 20 --max-time 180 -C - \
    -o "$DOWNLOAD_DIR/apache-maven-${MAVEN_VERSION}-bin.tar.gz" "$MAVEN_URL"
  tar -xzf "$DOWNLOAD_DIR/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -C "$TOOLS_DIR"
else
  echo "Maven already exists in .dev/tools."
fi

echo "Local tools are ready."
