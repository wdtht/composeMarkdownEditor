#!/usr/bin/env bash
set -euo pipefail

current_major() {
  java -version 2>&1 | awk -F '[".]' '/version/ {print $2; exit}'
}

pick_compatible_java() {
  for j in \
    "$HOME/.local/share/mise/installs/java/21.0.2" \
    "$HOME/.local/share/mise/installs/java/17.0.2" \
    "/usr/lib/jvm/java-21-openjdk-amd64" \
    "/usr/lib/jvm/java-17-openjdk-amd64"
  do
    if [[ -x "$j/bin/java" ]]; then
      export JAVA_HOME="$j"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  done
  return 1
}

if [[ -n "${JAVA_HOME:-}" ]]; then
  major="$(current_major || echo 999)"
  if [[ "$major" -ge 24 ]]; then
    pick_compatible_java || true
  fi
else
  pick_compatible_java || true
fi

echo "Using JAVA_HOME=${JAVA_HOME:-<unset>}"
java -version

gradle --no-daemon test "$@"
