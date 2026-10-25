#!/usr/bin/env bash
# scripts/setup.sh가 띄운 개발 프로세스를 멈춘다. 기본은 백엔드만, 에뮬레이터는 --emu일 때만.
#
# 사용법: scripts/dev-down.sh [--emu] [--dry-run]
#   --emu       에뮬레이터(PLEOS_AVD, 기본 Pleos_Connect_v2.1.0)도 끈다(adb emu kill)
#   --dry-run   무엇을 멈출지 보여 주기만 한다
#   HMG_BACKEND_PORT  백엔드 포트 (기본 8081)
#   HMG_DEV_DIR       로그·PID 폴더 (기본 <레포>/.dev)
#
# 백엔드 대상: .dev/backend.pid의 프로세스 그룹(./gradlew bootRun) + 포트를 듣는 프로세스 중
# 명령줄에 이 레포의 backend/ 경로가 들어간 것(손으로 띄운 bootRun 포함). 다른 프로그램은 건드리지 않는다.
set -uo pipefail
source "$(dirname "$0")/_common.sh"

EMU=0
DRY=0
for arg in "$@"; do
  case "$arg" in
    --emu) EMU=1 ;;
    --dry-run) DRY=1 ;;
    -h | --help) sed -n '2,12p' "$0"; exit 0 ;;
    *) die "알 수 없는 인자: $arg (scripts/dev-down.sh --help)" ;;
  esac
done

AVD="${PLEOS_AVD:-Pleos_Connect_v2.1.0}"
PORT="${HMG_BACKEND_PORT:-8081}"
DEV_DIR="${HMG_DEV_DIR:-$REPO_ROOT/.dev}"
PID_FILE="$DEV_DIR/backend.pid"

act() {
  if [[ "$DRY" -eq 1 ]]; then
    echo "  (dry-run) $*"
  else
    "$@" 2>/dev/null || true
  fi
}

wait_gone() {
  local pid="$1" i
  for i in 1 2 3 4 5 6 7 8 9 10; do
    kill -0 "$pid" 2>/dev/null || return 0
    sleep 1
  done
  return 1
}

# ---- 백엔드 ----
targets=()
if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE" 2>/dev/null)"
  if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
    targets+=("$pid")
    echo "백엔드 gradlew(PID $pid, setup.sh가 띄움)"
  fi
fi
if command -v lsof >/dev/null 2>&1; then
  for pid in $(lsof -nP -t -iTCP:"$PORT" -sTCP:LISTEN 2>/dev/null); do
    cmd="$(ps -o command= -p "$pid" 2>/dev/null)"
    if [[ "$cmd" == *"$REPO_ROOT/backend/"* ]]; then
      targets+=("$pid")
      echo "백엔드 서버(PID $pid, 포트 $PORT)"
    else
      echo "포트 $PORT의 PID $pid는 이 레포 백엔드가 아니라서 그대로 둡니다: ${cmd:0:80}"
    fi
  done
else
  echo "lsof가 없어 포트 $PORT 리스너는 확인하지 못했습니다."
fi

if [[ ${#targets[@]} -eq 0 ]]; then
  echo "멈출 백엔드가 없습니다."
else
  for pid in "${targets[@]}"; do
    # setup.sh는 setsid로 띄우므로 PGID = PID. 그룹 전체에 TERM을 보낸다.
    pgid="$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ')"
    if [[ -n "$pgid" && "$pgid" == "$pid" ]]; then
      act kill -TERM -- "-$pgid"
    else
      act kill -TERM "$pid"
    fi
  done
  if [[ "$DRY" -eq 0 ]]; then
    for pid in "${targets[@]}"; do
      wait_gone "$pid" || { echo "PID $pid가 10초 안에 끝나지 않아 KILL 합니다."; kill -KILL "$pid" 2>/dev/null || true; }
    done
    rm -f "$PID_FILE"
    echo "백엔드를 멈췄습니다."
  fi
fi

# ---- 에뮬레이터 (--emu) ----
if [[ "$EMU" -eq 1 ]]; then
  ANDROID_HOME="$(find_android_home)" || die "Android SDK를 찾지 못했습니다."
  ADB="$ANDROID_HOME/platform-tools/adb"
  [[ -x "$ADB" ]] || die "adb가 없습니다($ADB)."
  found=0
  for serial in $("$ADB" devices 2>/dev/null | awk '/^emulator-/{print $1}'); do
    name="$("$ADB" -s "$serial" emu avd name 2>/dev/null | head -1 | tr -d '\r')"
    if [[ "$name" == "$AVD" ]]; then
      found=1
      echo "에뮬레이터 $serial ($AVD)"
      act "$ADB" -s "$serial" emu kill
    fi
  done
  [[ "$found" -eq 1 ]] || echo "실행 중인 '$AVD' 에뮬레이터가 없습니다."
fi
