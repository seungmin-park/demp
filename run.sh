#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
jar_path=${DEMP_JAR_PATH:-"$project_root/build/libs/demp-0.0.1-SNAPSHOT.jar"}
port=${PORT:-8080}

if [[ ! $port =~ ^[0-9]{1,5}$ ]] || (( 10#$port < 1 || 10#$port > 65535 )); then
  printf 'PORT must be a number between 1 and 65535\n' >&2
  exit 1
fi
if [[ ! -f $jar_path ]]; then
  printf 'JAR not found: %s\n' "$jar_path" >&2
  exit 1
fi

# JAVA_OPTS supports space-separated options; JAVA_TOOL_OPTIONS is also
# read by the JVM itself. Never evaluate option values as shell commands.
java_command=(java "-Dserver.port=$port" "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}")
if [[ ${JAVA_OPTS:-} =~ [^[:space:]] ]]; then
  read -r -a jvm_options <<< "${JAVA_OPTS//$'\n'/ }"
  java_command+=("${jvm_options[@]}")
fi
exec "${java_command[@]}" -jar "$jar_path" "$@"
