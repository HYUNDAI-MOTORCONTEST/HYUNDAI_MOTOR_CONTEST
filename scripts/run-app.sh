#!/usr/bin/env bash
# debug 앱을 빌드·설치하고 실행한다.
# 사용법: scripts/run-app.sh [gradle 추가 인자...]
#   예) scripts/run-app.sh -Phmg.backendUrl=http://10.0.2.2:9090
# JDK는 JAVA_HOME(17+) → Android Studio 내장 JBR → java_home 순으로 찾는다(_common.sh find_java_home).
set -euo pipefail
source "$(dirname "$0")/_common.sh"

APP_ID="com.hmgchallenge.app"
ACTIVITY="$APP_ID/.MainActivity"

setup_android_env
require_cmd adb "Android SDK의 Platform-Tools를 설치하세요."

JAVA_HOME="$(find_java_home)" ||
  die "JDK 17 이상을 찾지 못했습니다. JAVA_HOME을 지정하거나 Android Studio를 설치하세요."
export JAVA_HOME

adb get-state >/dev/null 2>&1 || die "연결된 기기가 없습니다. 에뮬레이터를 먼저 실행하세요(scripts/pleos-emu.sh --wait)."

cd "$REPO_ROOT"
./gradlew :frontend:app:installDebug "$@" || die "빌드·설치에 실패했습니다."
adb shell am start -n "$ACTIVITY" || die "앱 실행에 실패했습니다: $ACTIVITY"
echo "실행: $ACTIVITY  (로그: adb logcat -s HmgApp)"
