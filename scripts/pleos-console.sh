#!/usr/bin/env bash
# Pleos Virtual Console(차량 상태 시뮬레이터)을 연다.
# 에뮬레이터 8088 포트를 호스트로 포워딩하고 브라우저로 http://127.0.0.1:8088/ 을 연다.
# 사용법: scripts/pleos-console.sh   (PLEOS_CONSOLE_PORT로 포트 변경, 기본 8088)
set -euo pipefail
source "$(dirname "$0")/_common.sh"

PORT="${PLEOS_CONSOLE_PORT:-8088}"
URL="http://127.0.0.1:${PORT}/"

setup_android_env
require_cmd adb "Android SDK의 Platform-Tools를 설치하세요."

echo "에뮬레이터 연결을 기다립니다..."
adb wait-for-device
adb forward "tcp:${PORT}" "tcp:8088" >/dev/null ||
  die "포트 포워딩(tcp:${PORT} → tcp:8088)에 실패했습니다."
echo "포워딩 완료: $URL"

if [[ "$(uname -s)" == "Darwin" ]] && command -v open >/dev/null 2>&1; then
  open "$URL"
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$URL" >/dev/null 2>&1 &
else
  echo "브라우저를 자동으로 열 수 없습니다. 직접 여세요: $URL"
fi
