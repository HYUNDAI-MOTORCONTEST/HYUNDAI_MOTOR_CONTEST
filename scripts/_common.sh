#!/usr/bin/env bash
# scripts/*.sh 공용 함수. 직접 실행하지 않고 source 한다.
# - ANDROID_HOME 탐색: 환경변수 → ANDROID_SDK_ROOT → macOS ~/Library/Android/sdk → Linux ~/Android/Sdk
# - adb·emulator를 PATH에 추가
# - JDK 17+ 탐색: JAVA_HOME → Android Studio 내장 JBR → macOS /usr/libexec/java_home → Linux PATH의 java

die() {
  echo "오류: $*" >&2
  exit 1
}

find_android_home() {
  local candidate
  for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [[ -n "$candidate" && -d "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

setup_android_env() {
  ANDROID_HOME="$(find_android_home)" ||
    die "Android SDK를 찾지 못했습니다. ANDROID_HOME을 지정하세요(macOS 기본 ~/Library/Android/sdk, Linux 기본 ~/Android/Sdk)."
  export ANDROID_HOME
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
}

# JDK 홈($1)의 주 버전(예: 25). release 파일을 먼저 읽어 JVM을 띄우지 않는다.
java_major() {
  local home="$1" v=""
  if [[ -f "$home/release" ]]; then
    v="$(sed -n 's/^JAVA_VERSION="\([^"]*\)".*/\1/p' "$home/release" | head -1)"
  fi
  if [[ -z "$v" && -x "$home/bin/java" ]]; then
    v="$("$home/bin/java" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -1)"
  fi
  v="${v#1.}"
  v="${v%%[.+_-]*}"
  [[ "$v" =~ ^[0-9]+$ ]] && echo "$v"
}

# JDK 17 이상 홈 경로를 출력한다. 못 찾으면 1을 반환한다.
find_java_home() {
  local candidate major
  local -a candidates=(
    "${JAVA_HOME:-}"
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    "$HOME/android-studio/jbr"
    "/opt/android-studio/jbr"
    "/usr/local/android-studio/jbr"
    "/snap/android-studio/current/jbr"
  )
  if [[ -x /usr/libexec/java_home ]]; then
    candidates+=("$(/usr/libexec/java_home -v 17+ 2>/dev/null || true)")
  fi
  if [[ "$(uname -s)" != "Darwin" ]] && command -v java >/dev/null 2>&1; then
    candidates+=("$(dirname "$(dirname "$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)")")")
  fi
  for candidate in "${candidates[@]}"; do
    [[ -n "$candidate" && -x "$candidate/bin/java" ]] || continue
    major="$(java_major "$candidate")" || continue
    if (( major >= 17 )); then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' 명령을 찾지 못했습니다. $2"
}

# 레포 루트 (scripts/의 상위)
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT
