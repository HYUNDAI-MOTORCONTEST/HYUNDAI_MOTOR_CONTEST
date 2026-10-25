#!/usr/bin/env bash
# 개발환경 점검·세팅 (멱등, macOS·Linux). Claude Code·Codex의 SessionStart 훅이 기본 모드로 실행한다.
#
# 사용법: scripts/setup.sh [--check | --install | --create-avd] [-h]
#   (기본)        빠른 점검(수 초) + local.properties 생성 + 에뮬레이터·백엔드 백그라운드 자동 기동.
#                 아무것도 설치하지 않고, 빠진 것은 설치 명령만 안내한다.
#   --check       점검만 한다(파일 생성·자동 기동 없음).
#   --install     터미널(TTY)에서 대화형 설치: 빠진 SDK 패키지를 sdkmanager로 설치(라이선스는 직접 읽고 동의),
#                 AVD 생성, Gradle 배포판 준비. Pleos 시스템 이미지는 설치하지 않는다(안내만).
#   --create-avd  Pleos 시스템 이미지가 있으면 AVD만 만든다.
#
# 환경변수
#   HMG_AUTOSTART=0          자동 기동을 모두 끈다. 하나만 끄려면 HMG_AUTOSTART_EMU=0 / HMG_AUTOSTART_BACKEND=0
#   PLEOS_AVD                AVD 이름 (기본 Pleos_Connect_v2.1.0)
#   HMG_BACKEND_PORT         백엔드 포트 (기본 8081)
#   HMG_DEV_DIR              로그·PID 폴더 (기본 <레포>/.dev, git 제외)
#
# 종료 코드는 항상 0이다(훅이 세션을 막지 않게). 결과는 ✅/⚠️/❌ 요약으로 본다.
set -uo pipefail
source "$(dirname "$0")/_common.sh"

START_TS="$(perl -MTime::HiRes=time -e 'printf "%.3f", time' 2>/dev/null || date +%s)"

MODE="default"
for arg in "$@"; do
  case "$arg" in
    --check) MODE="check" ;;
    --install) MODE="install" ;;
    --create-avd) MODE="create-avd" ;;
    -h | --help) sed -n '2,19p' "$0"; exit 0 ;;
    *) echo "알 수 없는 인자: $arg (scripts/setup.sh --help)"; exit 0 ;;
  esac
done

AVD="${PLEOS_AVD:-Pleos_Connect_v2.1.0}"
PLEOS_DEVICE="Pleos_Connect_v2_0"
PLEOS_API="34"
PLEOS_TAG="pleos_car_emulator"
PORT="${HMG_BACKEND_PORT:-8081}"
DEV_DIR="${HMG_DEV_DIR:-$REPO_ROOT/.dev}"
PLEOS_DOC_URL="https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator"
INTERACTIVE=0
[[ -t 0 && -t 1 ]] && INTERACTIVE=1

case "$(uname -m)" in
  arm64 | aarch64) ABI="arm64-v8a"; PLEOS_SITE="https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml"; PLEOS_SIZE="약 4.7GB" ;;
  *) ABI="x86_64"; PLEOS_SITE="https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml"; PLEOS_SIZE="약 4.1GB" ;;
esac
PLEOS_PKG="system-images;android-${PLEOS_API};${PLEOS_TAG};${ABI}"

# ---- 결과 모음 ----
LINES=()
TODOS=()
N_WARN=0
N_FAIL=0
ok() { LINES+=("✅ $*"); }
warn() { LINES+=("⚠️ $*"); N_WARN=$((N_WARN + 1)); }
fail() { LINES+=("❌ $*"); N_FAIL=$((N_FAIL + 1)); }
todo() { TODOS+=("$*"); }

# 명령을 최대 N초만 실행한다(macOS에 timeout이 없어서 perl alarm 사용).
run_limited() {
  local secs="$1"; shift
  perl -e 'alarm shift; exec @ARGV or exit 127' "$secs" "$@"
}

# 새 세션(setsid)으로 완전히 분리해 띄운다. 훅이 이 프로세스를 기다리지 않게 stdin/stdout/stderr를 모두 돌린다.
SPAWN_PID=""
spawn_detached() {
  local log="$1"; shift
  if command -v perl >/dev/null 2>&1; then
    nohup perl -MPOSIX -e 'POSIX::setsid(); exec @ARGV or die "exec 실패: $!\n"' -- "$@" </dev/null >>"$log" 2>&1 &
  elif command -v setsid >/dev/null 2>&1; then
    nohup setsid "$@" </dev/null >>"$log" 2>&1 &
  else
    nohup "$@" </dev/null >>"$log" 2>&1 &
  fi
  SPAWN_PID=$!
  disown "$SPAWN_PID" 2>/dev/null || true
}

port_open() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# ini 파일의 key=value를 바꾸거나 추가한다.
set_ini() {
  local file="$1" key="$2" value="$3" tmp
  tmp="$(mktemp "${file}.XXXXXX")" || return 1
  awk -v k="$key" -v v="$value" -F= '
    $1 == k { print k "=" v; done = 1; next }
    { print }
    END { if (!done) print k "=" v }
  ' "$file" >"$tmp" && mv "$tmp" "$file"
}

ini_get() {
  sed -n "s/^$2=//p" "$1" 2>/dev/null | head -1
}

# ---- 1. JDK ----
JAVA_FOUND=""
if JAVA_FOUND="$(find_java_home)"; then
  export JAVA_HOME="$JAVA_FOUND"
  ok "JDK $(java_major "$JAVA_FOUND") — $JAVA_FOUND"
else
  JAVA_FOUND=""
  fail "JDK 17 이상 없음"
  todo "JDK: Android Studio를 설치하면 내장 JBR을 자동으로 씁니다. 다른 JDK는 JAVA_HOME=<JDK 17+ 경로>로 지정하세요."
fi

# ---- 2. Android SDK ----
SDK=""
if SDK="$(find_android_home)"; then
  export ANDROID_HOME="$SDK"
  ok "Android SDK — $SDK"
else
  SDK=""
  fail "Android SDK 없음 (ANDROID_HOME, ~/Library/Android/sdk, ~/Android/Sdk 모두 없음)"
  todo "Android Studio 설치 후 첫 실행 마법사(Standard)로 SDK를 받으세요. 다른 위치면 ANDROID_HOME을 지정하세요."
fi

# ---- 3. local.properties ----
LP="$REPO_ROOT/local.properties"
if [[ -z "$SDK" ]]; then
  warn "local.properties — SDK 경로를 몰라 건너뜀"
elif [[ -f "$LP" ]]; then
  lp_sdk="$(sed -n 's/^sdk\.dir[[:space:]]*=[[:space:]]*//p' "$LP" | head -1)"
  if [[ -z "$lp_sdk" ]]; then
    warn "local.properties에 sdk.dir 없음 — 'sdk.dir=$SDK' 줄을 추가하세요"
  elif [[ -d "$lp_sdk" ]]; then
    ok "local.properties — sdk.dir=$lp_sdk"
  else
    warn "local.properties의 sdk.dir 경로가 없음($lp_sdk) — 파일을 지우고 다시 실행하면 새로 만듭니다"
  fi
elif [[ "$MODE" == "check" ]]; then
  warn "local.properties 없음 — scripts/setup.sh(기본 모드)가 sdk.dir=$SDK 로 만듭니다"
else
  if printf '# scripts/setup.sh가 만든 파일 (git 제외, PC마다 다름)\nsdk.dir=%s\n' "$SDK" >"$LP"; then
    ok "local.properties 생성 — sdk.dir=$SDK"
  else
    fail "local.properties 생성 실패 ($LP)"
  fi
fi

# ---- 4. 필수 SDK 패키지 ----
COMPILE_SDK="$(sed -n 's/^android-compileSdk[[:space:]]*=[[:space:]]*"\([0-9.]*\)".*/\1/p' "$REPO_ROOT/gradle/libs.versions.toml" | head -1)"
COMPILE_SDK="${COMPILE_SDK:-37}"
# 패키지 이름: API 37부터는 minor 포함(platforms;android-37.0), 36 이하는 platforms;android-36
if [[ "$COMPILE_SDK" == *.* ]]; then
  PLATFORM_PKG="platforms;android-$COMPILE_SDK"
elif (( COMPILE_SDK >= 37 )); then
  PLATFORM_PKG="platforms;android-$COMPILE_SDK.0"
else
  PLATFORM_PKG="platforms;android-$COMPILE_SDK"
fi
BUILD_TOOLS_PKG="build-tools;${COMPILE_SDK%%.*}.0.0"

SDKMANAGER=""
AVDMANAGER=""
MISSING_PKGS=()
if [[ -n "$SDK" ]]; then
  for d in "$SDK/cmdline-tools/latest" "$SDK"/cmdline-tools/*; do
    if [[ -x "$d/bin/sdkmanager" ]]; then
      SDKMANAGER="$d/bin/sdkmanager"
      AVDMANAGER="$d/bin/avdmanager"
      break
    fi
  done

  present=()
  [[ -x "$SDK/platform-tools/adb" ]] && present+=("platform-tools") || MISSING_PKGS+=("platform-tools")
  [[ -x "$SDK/emulator/emulator" ]] && present+=("emulator") || MISSING_PKGS+=("emulator")
  plat_dir="$SDK/platforms/${PLATFORM_PKG#platforms;}"
  if [[ -d "$plat_dir" || -d "$SDK/platforms/android-${COMPILE_SDK%%.*}" ]]; then
    present+=("$PLATFORM_PKG")
  else
    MISSING_PKGS+=("$PLATFORM_PKG")
  fi
  bt_found=""
  for d in "$SDK"/build-tools/*; do
    [[ -d "$d" ]] && bt_found="$(basename "$d")"
  done
  if [[ -n "$bt_found" ]]; then
    present+=("build-tools;$bt_found")
  else
    MISSING_PKGS+=("$BUILD_TOOLS_PKG")
  fi

  if [[ "$MODE" == "install" && ${#MISSING_PKGS[@]} -gt 0 && -n "$SDKMANAGER" && "$INTERACTIVE" -eq 1 && -n "$JAVA_FOUND" ]]; then
    echo "SDK 패키지를 설치합니다: ${MISSING_PKGS[*]}"
    echo "라이선스 동의 질문이 나오면 내용을 읽고 직접 답하세요."
    if "$SDKMANAGER" --install "${MISSING_PKGS[@]}"; then
      MISSING_PKGS=()
      present+=("(방금 설치)")
    fi
  fi

  if [[ ${#MISSING_PKGS[@]} -eq 0 ]]; then
    ok "SDK 패키지 — ${present[*]} (compileSdk $COMPILE_SDK)"
  else
    fail "SDK 패키지 없음: ${MISSING_PKGS[*]} (compileSdk $COMPILE_SDK)"
    todo "SDK 패키지 설치(터미널에서, 라이선스는 직접 동의): scripts/setup.sh --install  또는  sdkmanager $(printf '"%s" ' "${MISSING_PKGS[@]}")"
  fi
  if [[ -z "$SDKMANAGER" ]]; then
    warn "Android SDK Command-line Tools 없음 — --install·AVD 생성에 필요합니다"
    todo "Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Tools에서 'Android SDK Command-line Tools (latest)'를 설치하세요."
  fi
fi

# ---- 5. Pleos 시스템 이미지 (자동 설치·라이선스 동의 금지: 안내만) ----
PLEOS_OK=0
if [[ -n "$SDK" ]]; then
  img_dir="$SDK/system-images/android-${PLEOS_API}/${PLEOS_TAG}/${ABI}"
  if [[ -f "$img_dir/package.xml" || -f "$img_dir/system.img" ]]; then
    PLEOS_OK=1
    img_name="$(sed -n 's/.*<display-name>\([^<]*\)<.*/\1/p' "$img_dir/package.xml" 2>/dev/null | head -1)"
    ok "Pleos 시스템 이미지 — $PLEOS_PKG${img_name:+ ($img_name)}"
  else
    fail "Pleos 시스템 이미지 없음 — $PLEOS_PKG"
    todo "Pleos 이미지(사람이 직접, 1회): Android Studio > SDK Manager > SDK Update Sites에 + 'Pleos Connect System Image' / $PLEOS_SITE 추가 → SDK Platforms에서 'Show Package Details' → Android 34의 'Pleos Connect v2.1.0 APIs' 체크 → 42dot 라이선스(pleos-connect-sdk-license)를 읽고 직접 동의 → 다운로드 $PLEOS_SIZE. 절차: $PLEOS_DOC_URL"
  fi
fi

# ---- 6. AVD ----
AVD_HOME="${ANDROID_AVD_HOME:-${ANDROID_USER_HOME:-$HOME/.android}/avd}"
AVD_OK=0
create_avd() {
  local log="$1" avd_dir="$AVD_HOME/$AVD.avd" cfg
  mkdir -p "$AVD_HOME"
  if ! echo no | ANDROID_AVD_HOME="$AVD_HOME" "$AVDMANAGER" create avd -n "$AVD" -k "$PLEOS_PKG" -d "$PLEOS_DEVICE" -p "$avd_dir" >"$log" 2>&1; then
    return 1
  fi
  cfg="$avd_dir/config.ini"
  [[ -f "$cfg" ]] || return 1
  set_ini "$cfg" hw.ramSize 4096 &&
    set_ini "$cfg" fastboot.forceColdBoot yes &&
    set_ini "$cfg" fastboot.forceFastBoot no &&
    set_ini "$cfg" hw.device.name "$PLEOS_DEVICE" &&
    set_ini "$cfg" hw.device.manufacturer 42dot &&
    set_ini "$cfg" hw.gpu.enabled yes &&
    set_ini "$cfg" avd.ini.displayname "Pleos Connect v2.1.0"
}
if [[ -f "$AVD_HOME/$AVD.ini" ]]; then
  AVD_OK=1
  cfg="$(sed -n 's/^path=//p' "$AVD_HOME/$AVD.ini" | head -1)"
  cfg="${cfg:-$AVD_HOME/$AVD.avd}/config.ini"
  ram="$(ini_get "$cfg" hw.ramSize)"
  cold="$(ini_get "$cfg" fastboot.forceColdBoot)"
  dev="$(ini_get "$cfg" hw.device.name)"
  note="RAM ${ram:-?}MB, forceColdBoot=${cold:-?}, device=${dev:-?}"
  if [[ "$cold" == "yes" && "${ram%M}" == "4096" ]]; then
    ok "AVD '$AVD' — $note"
  else
    warn "AVD '$AVD' — $note (권장: RAM 4096, forceColdBoot=yes. pleos-emu.sh는 항상 Cold boot로 띄움)"
  fi
elif [[ "$PLEOS_OK" -eq 1 ]]; then
  if [[ "$MODE" == "install" || "$MODE" == "create-avd" ]] && [[ -n "$AVDMANAGER" && -n "$JAVA_FOUND" ]]; then
    mkdir -p "$DEV_DIR"
    if create_avd "$DEV_DIR/avdmanager.log"; then
      AVD_OK=1
      ok "AVD '$AVD' 생성 — RAM 4096MB, Cold boot, device=$PLEOS_DEVICE"
    else
      fail "AVD '$AVD' 생성 실패 — 로그: $DEV_DIR/avdmanager.log"
    fi
  else
    warn "AVD '$AVD' 없음 (이미지는 있음)"
    todo "AVD 만들기: scripts/setup.sh --create-avd  (또는 Android Studio Device Manager > Automotive > Pleos Connect v2.1.0, RAM 4GB, Default boot: Cold)"
  fi
else
  warn "AVD '$AVD' 없음 — Pleos 이미지를 먼저 설치하세요"
fi

# ---- 7. Gradle wrapper ----
if [[ -x "$REPO_ROOT/gradlew" && -f "$REPO_ROOT/gradle/wrapper/gradle-wrapper.jar" ]]; then
  dist_url="$(sed -n 's/^distributionUrl=//p' "$REPO_ROOT/gradle/wrapper/gradle-wrapper.properties" | head -1)"
  dist_name="$(basename "${dist_url//\\/}" .zip)"
  dist_dir="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/$dist_name"
  if [[ "$MODE" == "install" && ! -d "$dist_dir" && -n "$JAVA_FOUND" ]]; then
    echo "Gradle 배포판($dist_name)을 내려받습니다..."
    (cd "$REPO_ROOT" && ./gradlew --version >/dev/null) || true
  fi
  if [[ -d "$dist_dir" ]]; then
    ok "Gradle wrapper — $dist_name (배포판 캐시 있음)"
  else
    warn "Gradle wrapper — $dist_name 배포판 미캐시(첫 빌드 때 자동 다운로드, 네트워크 필요)"
  fi
elif [[ -f "$REPO_ROOT/gradlew" ]]; then
  warn "gradlew 실행 권한 없음 — chmod +x gradlew"
else
  fail "gradlew 없음"
fi

# ---- 8. Context7 MCP ----
mcp_claude="없음"
if grep -q '"context7"' "$REPO_ROOT/.mcp.json" 2>/dev/null; then
  mcp_claude=".mcp.json"
  grep -q '"enabledMcpjsonServers"' "$REPO_ROOT/.claude/settings.json" 2>/dev/null && mcp_claude=".mcp.json(자동 승인: 폴더 신뢰 후)"
fi
mcp_codex="없음"
grep -q '^\[mcp_servers\.context7\]' "$REPO_ROOT/.codex/config.toml" 2>/dev/null && mcp_codex=".codex/config.toml"
codex_note=""
if command -v codex >/dev/null 2>&1; then
  codex_cfg="${CODEX_HOME:-$HOME/.codex}/config.toml"
  trusted="$(awk -v hdr="[projects.\"$REPO_ROOT\"]" '
    index($0, hdr) == 1 { inside = 1; next }
    /^\[/ { inside = 0 }
    inside && $0 ~ /trust_level *= *"trusted"/ { print "yes" }
  ' "$codex_cfg" 2>/dev/null)"
  if [[ "$trusted" == "yes" ]]; then
    codex_note=", Codex 프로젝트 신뢰됨"
  else
    codex_note=", Codex 프로젝트 미신뢰"
  fi
else
  codex_note=", codex CLI 없음"
fi
key_note="키 없음(무료 한도)"
[[ -n "${CONTEXT7_API_KEY:-}" ]] && key_note="CONTEXT7_API_KEY 설정됨"
if [[ "$mcp_claude" != "없음" && "$mcp_codex" != "없음" ]]; then
  if [[ "$codex_note" == ", Codex 프로젝트 미신뢰" ]]; then
    warn "Context7 MCP — Claude: $mcp_claude / Codex: $mcp_codex$codex_note, $key_note"
    todo "Codex: 이 폴더에서 codex를 실행해 프로젝트를 신뢰(trust)하고 /hooks에서 SessionStart 훅을 검토·신뢰하세요. 그래야 .codex/config.toml의 MCP·훅이 로드됩니다."
  else
    ok "Context7 MCP — Claude: $mcp_claude / Codex: $mcp_codex$codex_note, $key_note"
  fi
else
  warn "Context7 MCP 설정 누락 — Claude: $mcp_claude / Codex: $mcp_codex"
fi

# ---- 9. 자동 기동 (기본·--install 모드) ----
AUTOSTART=1
autostart_why=""
if [[ "$MODE" == "check" || "$MODE" == "create-avd" ]]; then
  AUTOSTART=0
elif [[ "${HMG_AUTOSTART:-1}" == "0" ]]; then
  AUTOSTART=0; autostart_why="HMG_AUTOSTART=0"
elif [[ -n "${CI:-}" ]]; then
  AUTOSTART=0; autostart_why="CI 환경"
fi

LOCK="$DEV_DIR/autostart.lock"
if [[ "$AUTOSTART" -eq 1 ]]; then
  mkdir -p "$DEV_DIR"
  # 동시에 열린 두 세션이 함께 띄우지 않게 잠금(60초 지나면 낡은 잠금으로 보고 지움)
  if [[ -d "$LOCK" ]]; then
    age="$(perl -e 'print time - (stat shift)[9]' "$LOCK" 2>/dev/null || echo 999)"
    (( age > 60 )) && rmdir "$LOCK" 2>/dev/null
  fi
  if ! mkdir "$LOCK" 2>/dev/null; then
    AUTOSTART=0; autostart_why="다른 세션이 기동 중"
  fi
fi

# 에뮬레이터
emu_running=0
emu_serials=""
if [[ -n "$SDK" ]]; then
  if pgrep -f -- "-avd $AVD( |$)" >/dev/null 2>&1 || pgrep -f -- "qemu-system.*-avd " >/dev/null 2>&1; then
    emu_running=1
  fi
  if [[ -x "$SDK/platform-tools/adb" ]]; then
    emu_serials="$(run_limited 3 "$SDK/platform-tools/adb" devices 2>/dev/null </dev/null | awk '/^emulator-/{printf "%s%s(%s)", sep, $1, $2; sep=", "}')"
    [[ -n "$emu_serials" ]] && emu_running=1
  fi
fi
if [[ "$emu_running" -eq 1 ]]; then
  ok "에뮬레이터 실행 중${emu_serials:+ — $emu_serials} (새로 띄우지 않음)"
elif [[ "$AUTOSTART" -eq 0 ]]; then
  if [[ "$MODE" == "check" ]]; then
    warn "에뮬레이터 꺼짐 — 띄우려면 scripts/pleos-emu.sh --wait"
  else
    warn "에뮬레이터 꺼짐 — 자동 기동 생략(${autostart_why:-점검 모드})"
  fi
elif [[ "${HMG_AUTOSTART_EMU:-1}" == "0" ]]; then
  warn "에뮬레이터 꺼짐 — 자동 기동 생략(HMG_AUTOSTART_EMU=0)"
elif [[ "$AVD_OK" -ne 1 || ! -x "${SDK:-/nonexistent}/emulator/emulator" ]]; then
  warn "에뮬레이터 자동 기동 생략 — AVD·emulator 준비 안 됨"
elif [[ "$(uname -s)" != "Darwin" && -z "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then
  warn "에뮬레이터 자동 기동 생략 — 화면(DISPLAY) 없는 세션"
else
  : >"$DEV_DIR/pleos-emu-launch.log"
  PLEOS_EMU_LOG="$DEV_DIR/pleos-emu.log" PLEOS_AVD="$AVD" spawn_detached "$DEV_DIR/pleos-emu-launch.log" bash "$REPO_ROOT/scripts/pleos-emu.sh"
  ok "에뮬레이터 '$AVD' 기동 시작(Cold boot, 부팅 1~3분) — 로그: $DEV_DIR/pleos-emu.log"
fi

# 백엔드
BACKEND_PID_FILE="$DEV_DIR/backend.pid"
backend_pid=""
[[ -f "$BACKEND_PID_FILE" ]] && backend_pid="$(cat "$BACKEND_PID_FILE" 2>/dev/null)"
if port_open "$PORT"; then
  health="$(curl -s -m 2 "http://127.0.0.1:$PORT/api/health" 2>/dev/null)"
  if [[ "$health" == *'"status":"UP"'* ]]; then
    ok "백엔드 실행 중 — http://localhost:$PORT/api/health UP (새로 띄우지 않음)"
  else
    warn "포트 $PORT 사용 중이지만 /api/health 응답 없음 — 다른 프로세스일 수 있음(lsof -nP -iTCP:$PORT -sTCP:LISTEN)"
  fi
elif [[ -n "$backend_pid" ]] && kill -0 "$backend_pid" 2>/dev/null; then
  ok "백엔드 기동 중(PID $backend_pid, 아직 포트 $PORT 대기) — 로그: $DEV_DIR/backend.log"
elif [[ "$AUTOSTART" -eq 0 ]]; then
  if [[ "$MODE" == "check" ]]; then
    warn "백엔드 꺼짐 — 띄우려면 ./gradlew :backend:bootRun"
  else
    warn "백엔드 꺼짐 — 자동 기동 생략(${autostart_why:-점검 모드})"
  fi
elif [[ "${HMG_AUTOSTART_BACKEND:-1}" == "0" ]]; then
  warn "백엔드 꺼짐 — 자동 기동 생략(HMG_AUTOSTART_BACKEND=0)"
elif [[ -z "$JAVA_FOUND" || ! -x "$REPO_ROOT/gradlew" ]]; then
  warn "백엔드 자동 기동 생략 — JDK·gradlew 준비 안 됨"
else
  bootrun_args=(:backend:bootRun)
  [[ "$PORT" != "8081" ]] && bootrun_args+=("--args=--server.port=$PORT")
  {
    echo "=== $(date '+%Y-%m-%d %H:%M:%S') scripts/setup.sh: ./gradlew ${bootrun_args[*]}"
  } >>"$DEV_DIR/backend.log"
  (cd "$REPO_ROOT" && spawn_detached "$DEV_DIR/backend.log" ./gradlew "${bootrun_args[@]}" && echo "$SPAWN_PID" >"$BACKEND_PID_FILE")
  ok "백엔드 기동 시작(./gradlew :backend:bootRun, 포트 $PORT, 30초~1분) — 로그: $DEV_DIR/backend.log, 중지: scripts/dev-down.sh"
fi

[[ -d "$LOCK" ]] && rmdir "$LOCK" 2>/dev/null

# ---- 에이전트 셸 환경 (Claude Code SessionStart 전용) ----
if [[ -n "${CLAUDE_ENV_FILE:-}" ]]; then
  {
    [[ -n "$JAVA_FOUND" ]] && printf 'export JAVA_HOME=%q\n' "$JAVA_FOUND"
    if [[ -n "$SDK" ]]; then
      printf 'export ANDROID_HOME=%q\n' "$SDK"
      printf 'export PATH=%q:%q:"$PATH"\n' "$SDK/platform-tools" "$SDK/emulator"
    fi
  } >>"$CLAUDE_ENV_FILE" 2>/dev/null
fi

# ---- 요약 출력 ----
END_TS="$(perl -MTime::HiRes=time -e 'printf "%.3f", time' 2>/dev/null || date +%s)"
ELAPSED="$(awk -v a="$START_TS" -v b="$END_TS" 'BEGIN { printf "%.1f", b - a }')"
mode_label="기본"
case "$MODE" in
  check) mode_label="--check" ;;
  install) mode_label="--install" ;;
  create-avd) mode_label="--create-avd" ;;
esac
SUMMARY="$(
  echo "[개발환경 점검] scripts/setup.sh ($mode_label 모드, ${ELAPSED}초) — ❌ ${N_FAIL}개, ⚠️ ${N_WARN}개"
  printf '%s\n' "${LINES[@]}"
  if [[ ${#TODOS[@]} -gt 0 ]]; then
    echo "사람이 할 일 (설치·라이선스 동의는 에이전트가 대신하지 않음):"
    i=1
    for t in "${TODOS[@]}"; do
      echo "  $i. $t"
      i=$((i + 1))
    done
  fi
  if [[ "$MODE" == "install" && "$INTERACTIVE" -eq 0 ]]; then
    echo "참고: --install은 터미널(TTY)에서 직접 실행해야 설치를 진행합니다. 이번 실행은 점검만 했습니다."
  fi
)"
printf '%s\n' "$SUMMARY"
# 마지막 결과를 남긴다(점검 모드는 파일을 만들지 않음)
if [[ "$MODE" != "check" ]]; then
  mkdir -p "$DEV_DIR" 2>/dev/null && printf '%s\n' "$SUMMARY" >"$DEV_DIR/setup-last.txt" 2>/dev/null
fi

exit 0
