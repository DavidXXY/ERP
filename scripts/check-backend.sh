#!/usr/bin/env bash
set -u

echo "Backend environment check"
echo

missing=0
API_PORT="${API_PORT:-8080}"

check_command() {
  local name="$1"
  local hint="$2"
  if command -v "$name" >/dev/null 2>&1; then
    echo "OK   $name: $(command -v "$name")"
  else
    echo "MISS $name: $hint"
    missing=1
  fi
}

check_port() {
  local port="$1"
  local name="$2"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "OK   $name: listening on $port"
  else
    echo "MISS $name: not listening on $port"
    missing=1
  fi
}

find_local_java() {
  find ".dev/tools" -path "*/Contents/Home/bin/java" -type f 2>/dev/null | head -n 1
}

find_local_maven() {
  find ".dev/tools" -path "*/bin/mvn" -type f 2>/dev/null | head -n 1
}

local_java="$(find_local_java)"
local_maven="$(find_local_maven)"

if [[ -n "$local_java" ]]; then
  echo "OK   java: $local_java"
elif command -v java >/dev/null 2>&1 && java -version >/dev/null 2>&1; then
  echo "OK   java: $(java -version 2>&1 | head -n 1)"
else
  echo "MISS java: install Java 17 or run npm run tools:install"
  missing=1
fi

if [[ -n "$local_maven" ]]; then
  echo "OK   mvn: $local_maven"
elif command -v mvn >/dev/null 2>&1 && mvn -version >/dev/null 2>&1; then
  echo "OK   mvn: $(mvn -version 2>&1 | head -n 1)"
else
  echo "MISS mvn: install Maven or run npm run tools:install"
  missing=1
fi

if command -v docker >/dev/null 2>&1; then
  echo "OK   docker: $(command -v docker)"
else
  echo "WARN docker: not installed; PostgreSQL docker mode unavailable"
fi

echo
if lsof -nP -iTCP:5432 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "OK   PostgreSQL: listening on 5432"
else
  echo "MISS PostgreSQL: not listening on 5432"
  missing=1
fi
check_port "$API_PORT" "Spring Boot API"

echo
if curl -fsS "http://localhost:${API_PORT}/actuator/health" >/dev/null 2>&1; then
  echo "OK   API health: http://localhost:${API_PORT}/actuator/health"
else
  echo "MISS API health: backend is not reachable"
  missing=1
fi

echo
if [[ "$missing" -eq 0 ]]; then
  echo "Backend environment looks ready."
else
  echo "Backend is not ready yet."
  echo "PostgreSQL startup:"
  echo "  1. npm run tools:install"
  echo "  2. npm run infra:up"
  echo "  3. npm run api:dev"
  echo "  4. npm run admin:dev"
fi

exit "$missing"
