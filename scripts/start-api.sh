#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

find_java_home() {
  local java_bin
  local system_java_home
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]] \
      && "${JAVA_HOME}/bin/java" -version >/dev/null 2>&1; then
    echo "$JAVA_HOME"
    return 0
  fi
  java_bin="$(find "$ROOT_DIR/.dev/tools" -path "*/Contents/Home/bin/java" -type f 2>/dev/null | head -n 1 || true)"
  if [[ -n "$java_bin" ]] && "$java_bin" -version >/dev/null 2>&1; then
    dirname "$(dirname "$java_bin")"
    return 0
  fi
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    /usr/libexec/java_home -v 17
    return 0
  fi
  java_bin="$(command -v java 2>/dev/null || true)"
  if [[ -n "$java_bin" ]] && "$java_bin" -version >/dev/null 2>&1; then
    system_java_home="$("$java_bin" -XshowSettings:properties -version 2>&1 | sed -n 's/^[[:space:]]*java.home = //p' | head -n 1)"
    if [[ -n "$system_java_home" && -d "$system_java_home" ]]; then
      echo "$system_java_home"
      return 0
    fi
  fi
  return 1
}

find_maven() {
  local local_mvn
  if command -v mvn >/dev/null 2>&1 && mvn -version >/dev/null 2>&1; then
    command -v mvn
    return 0
  fi
  local_mvn="$(find "$ROOT_DIR/.dev/tools" -path "*/bin/mvn" -type f 2>/dev/null | head -n 1 || true)"
  if [[ -n "$local_mvn" ]] && "$local_mvn" -version >/dev/null 2>&1; then
    echo "$local_mvn"
    return 0
  fi
  return 1
}

JAVA_HOME_VALUE="$(find_java_home || true)"
MAVEN_BIN="$(find_maven || true)"

if [[ -z "$JAVA_HOME_VALUE" || -z "$MAVEN_BIN" ]]; then
  echo "Java 17 or Maven is not ready. Run: npm run tools:install"
  exit 1
fi

export JAVA_HOME="$JAVA_HOME_VALUE"
export PATH="$JAVA_HOME/bin:$(dirname "$MAVEN_BIN"):$PATH"

cd "$ROOT_DIR/services/api"
exec "$MAVEN_BIN" spring-boot:run
