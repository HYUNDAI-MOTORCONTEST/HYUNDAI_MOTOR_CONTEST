#!/usr/bin/env bash
# 연결된 기기(에뮬레이터) 화면을 PNG로 캡처한다.
# 사용법: scripts/screenshot.sh [파일]   (기본 ./screenshot.png)
# 캡처 파일은 git에 넣지 않는다(실명·계정 정보가 찍힐 수 있다). 레포 밖 경로나 $TMPDIR을 권장한다.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

OUT="${1:-screenshot.png}"

setup_android_env
require_cmd adb "Android SDK의 Platform-Tools를 설치하세요."
adb get-state >/dev/null 2>&1 || die "연결된 기기가 없습니다. 에뮬레이터를 먼저 실행하세요(scripts/pleos-emu.sh)."

mkdir -p "$(dirname "$OUT")"
adb exec-out screencap -p >"$OUT"
[[ -s "$OUT" ]] || die "캡처 파일이 비어 있습니다: $OUT"
echo "저장: $OUT"
