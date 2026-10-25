#!/usr/bin/env bash
# Pleos Connect 에뮬레이터를 Cold boot로 백그라운드 실행한다.
# Cold boot 필수: quick boot(스냅샷)로 띄우면 VHAL을 쓸 수 없다(docs/guides/pleos-dev.md 6장).
#
# 사용법: scripts/pleos-emu.sh [--wait] [emulator 추가 인자...]
#   --wait          부팅 완료(sys.boot_completed=1)까지 기다린다
#   PLEOS_AVD       AVD 이름 (기본 Pleos_Connect_v2.1.0)
#   PLEOS_EMU_LOG   로그 파일 (기본 ${TMPDIR:-/tmp}/pleos-emu.log)
#   PLEOS_BOOT_TIMEOUT  --wait 최대 대기 초 (기본 300)
set -euo pipefail
source "$(dirname "$0")/_common.sh"

AVD="${PLEOS_AVD:-Pleos_Connect_v2.1.0}"
TMP_BASE="${TMPDIR:-/tmp}"
LOG="${PLEOS_EMU_LOG:-${TMP_BASE%/}/pleos-emu.log}"
TIMEOUT="${PLEOS_BOOT_TIMEOUT:-300}"

WAIT=0
ARGS=()
for arg in "$@"; do
  case "$arg" in
    --wait) WAIT=1 ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    *) ARGS+=("$arg") ;;
  esac
done

setup_android_env
require_cmd emulator "Android SDK의 Emulator 패키지를 설치하세요."
require_cmd adb "Android SDK의 Platform-Tools를 설치하세요."

emulator -list-avds | grep -qx "$AVD" ||
  die "AVD '$AVD'가 없습니다. 'emulator -list-avds'로 이름을 확인하고 PLEOS_AVD로 지정하세요."

# 이미 실행 중인지: 같은 AVD로 뜬 에뮬레이터 프로세스 또는 adb에 붙은 에뮬레이터
if pgrep -f -- "-avd $AVD( |$)" >/dev/null 2>&1; then
  echo "에뮬레이터 '$AVD'가 이미 실행 중입니다. 새로 띄우지 않습니다."
  adb devices | grep -E '^emulator-' || true
  exit 0
fi
if adb devices | grep -qE '^emulator-[0-9]+[[:space:]]+device'; then
  echo "다른 에뮬레이터가 이미 adb에 연결되어 있습니다. 새로 띄우지 않습니다."
  adb devices | grep -E '^emulator-'
  echo "그래도 띄우려면 기존 에뮬레이터를 끈 뒤 다시 실행하세요."
  exit 0
fi

nohup emulator -avd "$AVD" -no-snapshot-load -no-snapshot-save ${ARGS[@]+"${ARGS[@]}"} >"$LOG" 2>&1 &
PID=$!
disown "$PID" 2>/dev/null || true
echo "에뮬레이터 '$AVD' 실행 (Cold boot, PID $PID)"
echo "로그: $LOG"

if [[ "$WAIT" -eq 1 ]]; then
  echo "부팅 완료를 기다립니다(최대 ${TIMEOUT}초)..."
  start=$(date +%s)
  until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if ! kill -0 "$PID" 2>/dev/null; then
      die "에뮬레이터가 종료되었습니다. 로그를 확인하세요: $LOG"
    fi
    if (( $(date +%s) - start > TIMEOUT )); then
      die "${TIMEOUT}초 안에 부팅이 끝나지 않았습니다. 로그를 확인하세요: $LOG"
    fi
    sleep 3
  done
  echo "부팅 완료 ($(( $(date +%s) - start ))초)"
fi
