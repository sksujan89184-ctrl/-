#!/bin/bash
# Minimal gradlew fallback: use system gradle if wrapper jar missing
set -e
if [ -x "$(pwd)/gradlew" ] && [ "$0" != "./gradlew" ]; then
  exec "$(pwd)/gradlew" "$@"
fi
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  # Prefer the wrapper if present
  java -jar gradle/wrapper/gradle-wrapper.jar "$@"
else
  if command -v gradle >/dev/null 2>&1; then
    gradle "$@"
  else
    echo "Gradle wrapper not found and 'gradle' is not installed. Please install Gradle or add the Gradle wrapper jar." >&2
    exit 1
  fi
fi
