@AGENTS.md

# Claude Code 전용 메모

공통 규칙·명령·필수 요건은 위에서 import한 `AGENTS.md`가 원본이다. 이 파일에는 Claude Code에만 해당하는 내용만 둔다.

- 세션 시작 훅: `.claude/settings.json`의 `SessionStart`(matcher `startup`, timeout 30초)가 `scripts/setup.sh`를 실행한다. 출력 요약은 컨텍스트로 들어오고, 훅은 실패해도 세션을 막지 않는다(항상 exit 0).
  훅은 `CLAUDE_ENV_FILE`에 `JAVA_HOME`·`ANDROID_HOME`·`PATH`(platform-tools, emulator)를 넣어 이후 Bash 명령에서 `adb`·`./gradlew`를 바로 쓸 수 있게 한다.
- `.mcp.json`: Context7(원격 HTTP). API 키는 선택이며 `CONTEXT7_API_KEY` 환경변수로만 준다. 키가 없어도 동작한다.
- `.claude/settings.json`의 `enabledMcpjsonServers: ["context7"]`는 폴더 신뢰(workspace trust)를 승인한 뒤에 적용된다. 승인을 다시 고르려면 `claude mcp reset-project-choices`.
- 스킬 원본은 `.claude/skills/`에 둔다(Codex용 `.agents/skills/`는 심볼릭 링크). 새 스킬을 추가하면 `.agents/skills/<이름>` 링크도 같이 만든다.
- 개인 설정은 `.claude/settings.local.json`에 둔다(gitignore 대상). 자동 기동을 끄려면 여기 `"env": {"HMG_AUTOSTART": "0"}`를 넣거나 셸에서 `export HMG_AUTOSTART=0`.
