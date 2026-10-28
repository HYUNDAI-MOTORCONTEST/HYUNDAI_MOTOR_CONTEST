# Pleos 플랫폼 정책 요약 (설계·개발·테스트 가이드: 오디오, CCP, IME, 성능, 보안)

> 직접 쓴 요약이다. 원문을 옮기지 않았고, 최종 기준은 각 항목의 원문 URL이다.
> 확인일: 2026-10-28 (한국어 페이지 기준)
> 표기: **[문서]** 원문에 있는 내용, **[문서에 없음]** 다른 근거로 정한 팀 권장(근거를 같이 적음), **[팀 확인]** 레포·AAR·에뮬레이터에서 직접 확인한 것.
> 주행 규제(CarUxRestrictions)·주의 분산·카테고리·화면 모델은 [pleos-dev.md](pleos-dev.md), SDK 사용법은 [pleos-sdk.md](pleos-sdk.md)에 있다. 이 파일은 그 밖의 설계·개발·테스트 가이드를 다룬다.

원문 약칭 (모두 `https://document.pleos.ai/docs/pleos-only` 아래)

| 약칭 | 페이지 |
| --- | --- |
| PLAN | `/vehicle-app-planning-guide/intro` (앱 설계 가이드 목차) |
| HW | `/vehicle-app-planning-guide/platform-api-architecture` (하드웨어 접근 정책) |
| SDKPOL | `/vehicle-app-planning-guide/pleos-connect-sdk-api` (SDK API 정책) |
| COMPAT | `/vehicle-app-planning-guide/api-compatibility-availability` (API 호환성 및 가용성) |
| SEC | `/vehicle-app-planning-guide/security-privacy` (보안·개인정보) |
| PERF | `/vehicle-app-planning-guide/performance-resource-mgmt` (성능·리소스) |
| DEVI | `/vehicle-app-developer-guide/intro` (앱 개발 가이드 목차) |
| AF | `/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development` |
| SUSP | `/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus` |
| PRE | `/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide` (TTS 선행 묵음) |
| CCP | `/vehicle-app-developer-guide/ccp-rotary-controller-development` |
| IME | `/vehicle-app-developer-guide/ime-window-resize-handling-guide` |
| MUSIC | `/vehicle-app-developer-guide/music-audio-app-developement-guide` (URL 철자 원문 그대로) |
| NAV | `/vehicle-app-developer-guide/navigation-app-development-guide` |
| FAQ | `/vehicle-app-developer-guide/vehicle-app-faq` (앱 실행 환경·플랫폼 리소스) |
| ADB | `/vehicle-app-developer-guide/adb-command-reference-guide` |
| VC | `/app-test-guide/virtual-console-guide` |

---

## 0. 우리 앱에 바로 영향이 있는 규칙

| 규칙 | 근거 | 우리 앱에서 할 일 |
| --- | --- | --- |
| 최상위 화면에서 Back(CCP Back 하드키)이면 앱을 끝내지 말고 숨긴다(`moveTaskToBack(true)`). 안 하면 심사 반려 가능 | CCP | 9장. 루트 화면의 Back 처리 추가 |
| CCP를 지원하지 않으면 매니페스트 `<application>`에 미지원 meta-data를 선언하고, 필수 조작은 터치만으로 끝나야 한다 | CCP | 9장. 지원 여부를 팀이 정한다 |
| 오디오를 내면 재생 전에 포커스를 요청하고, 포커스 변경 이벤트(LOSS·LOSS_TRANSIENT·CAN_DUCK)를 반드시 처리한다 | AF, PERF | 6장. 음성 안내 재생기에 포커스 처리 |
| 음성 안내 앞에 150~200 ms 묵음을 넣는다(데이터 앞에 패딩 우선) | PRE | 8장 |
| Suspend(시동 OFF 후 저전력) 진입 시 재생 중단·포커스 반납, Resume 때 자동 재생 금지 | SUSP | 7장 |
| 주행 중 CPU 5% 미만(내비 아닌 앱), 정차 중 25% 이하, RAM 400 MB 이하 | PERF | 5장. 폴링·애니메이션 줄이기 |
| 텍스트 입력이 없는 window는 IME가 떠도 밀려 올라가면 안 된다 | IME | 10장. `imePadding`은 입력 화면에만 |
| 하드웨어 직접 접근 금지, 모바일 전용 API(전화·SMS·일부 센서·주소록 등)는 없거나 제한될 수 있다 | HW | 2장 |
| 주행 중 네트워크 끊김 대비: 캐시, 재시도·타임아웃, 오프라인 안내 UI | HW | 2장. 백엔드 없이도 핵심 기능 동작 |
| 개인정보 최소 수집, 로그 마스킹, 토큰은 Keystore, 계정 전환 쉽게 | SEC | 4장 |
| 표준 데이터(속도·기어 등)는 AAOS Car API, Pleos SDK는 특화 기능만 | SDKPOL, FAQ | 3장. 속도는 `PERF_VEHICLE_SPEED` |
| `APP_MAPS` 카테고리, `appCategory="audio"`는 선언하지 않는다(카테고리와 기능 불일치는 즉시 반려) | NAV, MUSIC | 11·12장 |

## 1. 설계·개발 가이드 구성 [문서] — PLAN, DEVI

- 설계 가이드 9개 페이지: 주행 안전 설계, 지역별 주행 규제, 앱 카테고리, 앱 구조·동작 모델(이상 `pleos-dev.md`), 하드웨어 접근, SDK API 정책, API 호환성, 보안·개인정보, 성능·리소스(이 파일).
- 개발 가이드는 "반드시 지켜야 할 정책과 구현 방법"이라고 소개한다. 오디오 포커스, Suspend 오디오, 주행 규제(`pleos-dev.md`), CCP, Maps/Navigation, Music/Audio, TTS 선행 묵음, IME resize, FAQ 2개.
- 상당수 항목이 "Pleos App Market 심사 기준"으로 적혀 있다. 공모전 심사와 같은 기준인지는 문서에 없다. [문서에 없음] 우리는 App Market 심사를 통과할 수준으로 맞춘다고 가정한다.

## 2. 하드웨어 접근·네트워크 [문서] — HW

- 앱은 차량 하드웨어(물리 제어·센서)에 직접 접근하지 않는다. 차량 상태는 Pleos SDK 같은 플랫폼 API로만 읽고 제어한다.
- 모바일 앱에서 쓰던 API가 차량에서는 없거나 제한될 수 있다. 예: 전화, SMS, 자이로·근접 같은 일부 센서, 캘린더, 주소록. 필수가 아니면 빼고, 쓸 때는 지원 여부를 확인한 뒤 동작하는 선택 기능으로 만든다.
- 주행 중 셀룰러·Wi-Fi가 끊기거나 느려질 수 있다. 권장: 최근 콘텐츠 로컬 캐시, 재시도와 적절한 타임아웃, 오프라인 상태를 알리는 UI.

우리 앱 적용 [문서에 없음 — 위 원칙에서 정한 팀 규칙]
- 핵심 기능은 백엔드 없이 앱 안 데이터만으로 동작하게 한다. 서버는 동기화·부가 기능용.
- 지금 `MainViewModel`의 health 호출은 타임아웃 3초에 재시도 버튼이 있다. 실제 기능에도 같은 방식(짧은 타임아웃 + 수동 재시도 + "오프라인" 표시)을 쓴다.
- 전화·SMS·연락처 연동 기능은 기획에 넣지 않는다.

## 3. SDK API 정책과 호환성 [문서] — SDKPOL, COMPAT

- AAOS 표준에 같은 기능이 있으면 표준 API를 쓰고, Pleos SDK는 같은 기능을 따로 만들지 않는다. SDK 전용 API는 AAOS로 안 되는 영역(제조사 고유 제어·보안 인증 등)에만 있다.
- 선택 기준: 속도·기어·외기 온도·연료 같은 표준 데이터는 AAOS Car API, 제조사 특화 기능은 Pleos SDK.
- SDK API가 나중에 AAOS 표준으로 들어가도 기존 SDK API는 최소 2개 메이저 버전 동안 유지된다.
- 차량 모델별 호환성:

| SDK | 호환 |
| --- | --- |
| Vehicle | 모델마다 다름. 도메인별 `checkXxxCapabilities`로 확인 |
| NaviHelper | 내비게이션이 탑재된 차량에서만 전체 호환 |
| TextToSpeech, SpeechToText | 전체 호환 |
| FusedLocation | 모델마다 다름. `isSupported()`로 확인 |

- AAOS Vehicle Property도 모델·프로퍼티마다 지원이 다르다. 모델명으로 분기하지 말고 `VehiclePropertyIds`·`CarPropertyManager`로 실제 지원·접근 가능 여부를 확인한다. SDK에 없는 특수 프로퍼티가 필요하면 구현 전에 기술 지원팀 검토를 받는다.

우리 앱 적용
- [팀 확인] Vehicle SDK 2.3.2의 Gear·EvBatteryLevel API는 deprecated이고 "Google 프로퍼티를 쓰라"고 되어 있다(`@Deprecated` 메시지). 위 AAOS Alignment 정책과 같은 흐름이다. 새 코드는 deprecated API를 쓰지 않는다.
- 내비 탑재 여부를 앱이 확인하는 방법은 공개되지 않았다(FAQ, 13장). NaviHelper 기능은 초기화·요청 실패를 "내비 없음"으로 보고 숨기거나 대체한다.

## 4. 보안·개인정보 [문서] — SEC

- 차량은 여러 사람이 쓰는 기기다. 소유자·운전자가 바뀌면 쉽게 로그아웃·계정 전환할 수 있어야 한다.
- 소셜 로그인·OTT 같은 토큰은 Keystore 같은 안전한 저장소에 둔다.
- 위치·음성·계정 정보를 요구할 때는 왜 필요한지 분명히 알리고, 동의 흐름을 짧고 쉽게 만든다.
- 필요 없는 개인정보(PII)는 저장하지 않는다. 로그에 개인정보가 들어가면 마스킹한다.
- 차량 밖으로 나가는 텔레메트리·분석 이벤트: 차량 ID·사용자 ID 익명화/가명화 정책을 정하고, 운전 중 행동 데이터를 마케팅에 쓰지 않는다.
- App Market 등록용 상세 보안 기준은 별도 "앱 보안 가이드"(`/app-registration-guide/app-security-guide`)에 있다. 이번에는 읽지 않았다(16장).
- NAV(12장)에는 내비 앱 기준으로 위치 데이터 암호화, HTTPS, 개인정보 처리방침 제공, 불필요한 위치 저장 금지가 있다.

우리 앱 적용 [문서에 없음 — SEC·NAV와 공모전 익명성 규칙(`docs/contest/rules.md` 6장)에서 정한 팀 규칙]
- 민감한 개인정보(이름·생년월일·사진 등)는 받지 않거나 최소한(별명 수준)만 받는다. 기기 밖으로 보내지 않는다.
- `Log`에 위치 좌표·주소·음성 인식 원문을 그대로 남기지 않는다. 필요하면 앞자리만 남기거나 길이만 기록한다.
- release 빌드의 백엔드 주소는 HTTPS만 쓴다(현재 `build.gradle.kts`의 release 값은 자리표시자). debug만 평문 HTTP 허용.
- Playground 클라이언트 ID·시크릿, API 키는 레포에 넣지 않는다(`pleos-sdk.md` 7장).
- 첫 실행 시 음성(마이크)·위치를 쓰는 이유를 한 화면으로 설명한다. 주행 중에는 이 동의 화면을 띄우지 않는다(NO_SETUP).
- 계정 기능을 만들지 않으면 계정 전환 요건은 해당 없음. 만들면 프로필 전환 버튼을 설정 첫 화면에 둔다.

## 5. 성능·리소스 [문서] — PERF

| 항목 | 기준 |
| --- | --- |
| CPU (내비 앱, 주행 중) | 15% 이하 |
| CPU (내비 아닌 앱, 주행 중) | 5% 미만 |
| CPU (모든 앱, 정차 중) | 25% 이하 |
| RAM | 400 MB 이하 |

- Cold start에서 스플래시에 오래 머물지 않게 최소한만 초기화하고 나머지는 지연 로딩한다.
- 백그라운드 긴 작업, 불필요한 타이머·폴링을 줄인다. 리스트는 재활용·캐시로 메모리를 아낀다.
- 오디오 앱은 Pleos 오디오 포커스 정책(6장)을 따르고 포커스 획득·상실 처리를 반드시 구현한다.

우리 앱 적용 [문서에 없음 — 측정 방법은 Android 표준 도구]
- 주행 중 5% 미만이 우리 기준이다. 주행 중 화면은 정적 텍스트 위주로, 애니메이션·주기 갱신(초 단위 타이머 UI 등)을 끈다. 남은 시간 표시는 분 단위로 갱신한다.
- `Vehicle`·`NaviHelper`는 콜백 구독을 쓰고 폴링하지 않는다.
- 측정: `adb shell top -n 1 | grep com.hmgchallenge.app`, `adb shell dumpsys meminfo com.hmgchallenge.app`(TOTAL PSS가 400 MB보다 충분히 아래인지). 에뮬레이터 수치는 실차와 다를 수 있으니 여유를 크게 둔다.
- `MainActivity` 시작 시 무거운 초기화(DB 마이그레이션, 네트워크)는 첫 프레임 뒤로 미룬다.

## 6. 오디오 포커스 [문서] — AF

AAOS 기본 개념
- 새 요청의 CarAudioContext와 현재 보유자의 context 조합으로 결과가 정해진다. R(거부: 새 요청 실패), E(배타: 새 요청이 얻고 기존 보유자는 잃음), C(동시: 둘 다 유지, 보통 duck).
- C로 공존하려면 새 요청이 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`을 써야 한다. 기존 보유자가 duck 대신 pause하면 C가 깨질 수 있다.
- 보유자가 여럿이면 각 보유자와의 결과 중 가장 보수적인 것(R > E > C)을 적용한다.
- AudioAttributes usage → context 매핑: MUSIC(`MEDIA`, `GAME`, `UNKNOWN`), NAVIGATION(`ASSISTANCE_NAVIGATION_GUIDANCE`), VOICE_COMMAND(`ASSISTANT`, `VOICE_COMMAND`), CALL_RING, CALL, ALARM, NOTIFICATION(`NOTIFICATION*`), SYSTEM_SOUND(`ASSISTANCE_SONIFICATION` 등), EMERGENCY, SAFETY, VEHICLE_STATUS, ANNOUNCEMENT. 마지막 4개(시스템 usage)는 Android 11 이상에서만 있다.

Pleos 정책
- Pleos Connect Interaction Matrix v1.0(2026-01-28, 원문에 이미지로 있음)을 따른다. 행 = 현재 보유자, 열 = 새 요청.
- 앱이 할 일:
  - 재생 전마다 `AudioAttributes` + `AudioFocusRequest`로 포커스를 요청한다.
  - 포커스 변경 이벤트 처리는 AAOS에서는 선택이지만 Pleos에서는 필수다. 권장 대응: GAIN → 정상 재생, LOSS → 즉시 정지, LOSS_TRANSIENT → 일시정지 후 복귀 가능, LOSS_TRANSIENT_CAN_DUCK → 볼륨 감쇄.
  - duck 대상은 감쇄 후, 다른 스트림이 끝나면 원래 볼륨으로 돌린다.
  - Gleo(VOICE_COMMAND)가 호출돼 `AUDIOFOCUS_LOSS_TRANSIENT`를 받으면 즉시 재생을 멈추고 필요하면 포커스를 반납한다.
  - 매트릭스는 하드코딩하지 말고 설정 파일로 두는 것을 권장한다(이 항목은 포커스를 중재하는 쪽 구현에 해당).
  - 디버그 로그 권장 항목: 요청 context·gain·client id, 현재 보유자, 보유자별 결과, 최종 결과, duck 적용·해제.
- 원문 검증 체크리스트 예: MUSIC 중 NAVIGATION 요청 → C, CALL 중 MUSIC 요청 → R, MUSIC 중 CALL → E, EMERGENCY 중 어떤 요청이든 → R.
- [문서] 매트릭스 이미지에서 우리에게 필요한 칸만 옮긴다(2026-10-28 확인). 새 요청이 NOTIFICATION일 때: 현재 MUSIC·NAVIGATION·CALL·ALARM·NOTIFICATION이면 C, VOICE_COMMAND·CALL_RING·EMERGENCY면 R. 새 요청이 NAVIGATION일 때는 CALL_RING도 C라는 점만 다르다.

우리 앱 적용 [문서에 없음 — AF와 AAOS 오디오 문서에서 정한 팀 규칙]
- 우리 앱 음성 안내는 내비 길안내가 아니므로 usage `USAGE_NOTIFICATION`(또는 `USAGE_NOTIFICATION_EVENT`) + `CONTENT_TYPE_SPEECH`, gain `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`으로 요청한다. 위 매트릭스대로 음악·내비와는 C로 공존(음악 duck)하고, Gleo 대화·벨소리·긴급음 중에는 거부된다. 거부되면 안내를 버리지 말고 화면 표시로 대체하거나 짧게 늦춰 한 번만 다시 시도한다.
  - `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`는 쓰지 않는다. 내비 경로 안내가 아닌 소리에 내비 context를 쓰는 것은 NAV의 단일 경로 정책 취지와 맞지 않는다고 판단했다.
  - `USAGE_ANNOUNCEMENT` 같은 시스템 usage는 서드파티가 쓸 수 없는 것으로 본다(AAOS에서 시스템 권한 필요).
- 안내가 끝나면(완료·중지·오류 모두) 즉시 `abandonAudioFocusRequest`를 부른다. LOSS·LOSS_TRANSIENT를 받으면 바로 멈춘다(짧은 안내라 duck 대신 정지·재시도 안 함이 단순하다).
- Pleos TTS SDK로 말하게 할 때 포커스를 SDK(Speech Service)가 잡는지 앱이 잡아야 하는지는 문서에 없다. [팀 확인] TTS 3.3.1 AAR에는 `setAudioAttributes(AudioAttributes)` 공개 메서드가 있고 앱 쪽 코드에서는 포커스 요청을 찾지 못했다. 쓰게 되면 에뮬레이터에서 음악 재생 중 안내를 내 보며 `adb shell dumpsys audio`의 focus 스택으로 확인한다(`pleos-sdk.md` 3장).

포커스 처리 스케치 (팀 작성)
```kotlin
class GuidanceAudio(private val am: AudioManager) {
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var onLost: () -> Unit = {}
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onLost()
        }
        .build()

    /** true면 재생해도 된다. 재생이 끝나면 반드시 release()를 부른다. */
    fun acquire(onLost: () -> Unit): Boolean {
        this.onLost = onLost
        return am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    fun release() { am.abandonAudioFocusRequest(request) }
}
```

## 7. Suspend 모드 [문서] — SUSP

- Suspend = 시동(IGN) OFF 뒤 완전 종료하지 않고 저전력으로 들어간 상태. 화면·UI·오디오·일반 서비스는 멈추고, Garage mode 같은 일부 시스템 작업만 제한적으로 돈다. 앱은 "언제든 멈췄다가 다시 시작될 수 있다"고 보고 만든다.
- 진입 시 순서: 재생 즉시 중단 → 포커스 반납 → 재생 상태(재생 여부·위치) 저장 → 플레이어 등 오디오 리소스 해제. 이 사이 새 재생·포커스 요청 금지.
- 예외(통화·긴급 통신, 안전 경고음, 시스템 필수 음성 안내)는 시스템 정책으로만 최소 허용된다. 서드파티 앱 안내는 해당하지 않는다고 보는 것이 맞다.
- Resume 시 이전 오디오 상태를 그대로 복원하지 않는다. 포커스는 자동 복구되지 않으며, 앱이 상황을 다시 판단해 필요할 때만 다시 요청한다. 자동 재생 여부는 OEM·사용자 정책.
- 금지: Suspend 중 포커스 유지, 백그라운드 계속 재생, Resume을 위해 포커스를 미리 잡아 두기.
- Suspend 진입·복귀를 앱이 어떤 이벤트로 감지하는지는 원문 예제에 없다(예제는 `onSuspendEntering()`처럼 이름만 둔 메서드). [문서에 없음] AAOS `CarPowerManager`는 시스템 권한이 필요할 수 있어, 우리는 Activity `onStop`에서 안내 재생을 멈추고 포커스를 반납하는 것으로 대신한다(화면이 꺼지면 `onStop`이 온다). 확인 필요(16장).

우리 앱 적용
- 음성 안내는 짧고 일회성이다. 대기 중인 안내 큐는 `onStop`에서 비우고, Resume 뒤에 지난 안내를 몰아서 재생하지 않는다.
- 안내 예약은 앱 프로세스 안 타이머가 아니라 "다음에 화면이 켜졌을 때 다시 계산"하는 방식으로 만든다(프로세스가 죽어도 상태 복원).

## 8. TTS·음성 안내 선행 묵음 [문서] — PRE

- 적용 대상: TTS 안내, 녹음 음원 안내 멘트, 내비·알림·상태 안내 등 모든 음성 출력.
- 재생 시작 전에 묵음 150~200 ms를 넣는다. 150 ms 미만은 첫 음절이 잘릴 수 있고, 200 ms 초과는 지연이 느껴질 수 있어 예외적으로만.
- 넣는 위치 우선순위: ① PCM/WAV 데이터 앞에 무음 샘플 패딩 ② TTS 결과 생성 뒤 후처리로 무음 추가 ③ 플레이어 재생 지연. 지연만으로 처리하는 방식은 스케줄링 편차가 커서 권장하지 않는다.
- 한 발화로 이어지는 연속 음성에는 묵음을 또 넣지 않는다. 스트림이 끊기거나 새 세션이면 다시 넣는다.
- 묵음은 클리핑 완화 수단일 뿐이다. 출력 경로 초기화, 포커스·세션 확보, 플레이어 준비 상태는 따로 확인해야 한다.

우리 앱 적용 [문서에 없음 — PRE 수치를 우리 재생 경로에 맞춘 팀 규칙]
- 녹음(또는 미리 합성한) 안내 음원을 앱에 넣어 쓰는 경우: 음원 파일 자체에 180 ms 무음을 붙여 둔다(빌드 전 처리). 런타임 처리 비용이 없다.
- `AudioTrack`으로 PCM을 직접 낼 때: 16 kHz·16-bit·mono면 180 ms = 2,880 샘플 = 5,760 byte의 0을 앞에 쓴다.
- Pleos TTS SDK(`speak()`)를 쓰는 경우: 합성 PCM을 앱이 받지 못하므로 ①②를 할 수 없다. SDK·Speech Service가 묵음을 넣는지는 문서에 없다. 확인 필요(16장).
- 순서: 포커스 획득 → 플레이어 준비 완료 → (묵음 포함) 재생. 포커스를 얻기 전에 재생 지연 타이머를 돌리지 않는다.

## 9. CCP(로터리·물리 버튼)와 Back [문서] — CCP

- CCP = 차량 물리 입력(로터리 컨트롤러, 다이얼, 버튼). CCP Back 하드키는 이전 단계로 가거나, 더 갈 곳이 없으면 앱·시스템 Back을 수행한다.
- 앱 분류별 CCP 요건:

| 분류 | 요건 |
| --- | --- |
| User-installed (App Market에서 사용자가 설치) | CCP 지원은 선택. 미지원이어도 등록·배포 제한 없음. 단 필수 조작을 터치만으로 끝낼 수 있어야 함 |
| Pre-installed (출고 시 탑재) | CCP 필수(계약 조건). 정적 정보 표시형, WebView 기반, 게임 등은 계약서에 예외 명시 |
| System | 문서에 별도 요건 없음 |

- CCP를 지원하지 않는 앱은 `<application>` 아래 meta-data `com.hmg.rotary.ROTARY_SUPPORTED` = `false`로 미지원을 선언한다.
- **Back 정책(필수)**: 앱 안에서 더 돌아갈 화면이 없는 최상위에서 Back이 오면 앱을 종료하지 말고 숨긴다.
  - Back이 Android 표준 Back 이벤트로 오는지 먼저 확인하고, `OnBackInvokedDispatcher` 또는 기존 Back API로 받는다.
  - 팝업·하위 화면이면 기존대로 닫기·이전 화면, 최상위에서만 `moveTaskToBack(true)`.
  - `finish()`·`finishAffinity()`로 끝내지 않는다. 숨긴 뒤에도 프로세스는 죽을 수 있으니 상태는 따로 저장·복원한다.
  - 숨김이 구현되지 않았거나 앱이 종료되면 심사 반려 대상이 될 수 있다.
- Pre-installed CCP 심사 예시 항목: 모든 주요 UI에서 포커스 이동, 회전 시 리스트 스크롤, 클릭 확인, 뒤로·취소, 터치 전용 영역 없음, 운전 중 차단 준수.
- CCP UX 가이드라인은 Figma 파일 다운로드로만 제공된다(읽지 않음).

우리 앱 적용 [문서에 없음 — CCP 정책에서 정한 팀 권장, 결정은 코디네이터]
- 우리는 User-installed에 해당한다. 로터리 입력을 에뮬레이터에서 재현할 방법이 문서에 없고(user 빌드라 `cmd car_service inject-rotary` 류도 막힌 것으로 보임), 검증 못 한 지원을 주장하는 것보다 미지원 선언 + 터치 완결이 안전하다. 권장: `ROTARY_SUPPORTED=false` 선언.
  - 그래도 Compose 기본 포커스 순서가 자연스럽게 되도록 큰 버튼은 `clickable`(자동으로 focusable)로 두고, 포커스를 가로채는 커스텀 처리를 하지 않는다.
- Back 처리(필수): `MainActivity.onCreate`에서 Activity 단위 콜백으로 숨긴다(구현됨).
  ```kotlin
  onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
  ```
  - 하위 화면·팝업은 각자 Compose `BackHandler`로 닫기·이전 화면을 처리한다. 나중에 등록된 핸들러가 먼저 받으므로 최상위 콜백과 겹치지 않는다. 최상위용 `BackHandler`를 따로 더하지 않는다.
  - Android 12+는 루트 런처 Activity의 Back에서 기본적으로 task를 뒤로 보내지만(AOSP 동작), 예측형 Back·플랫폼 커스텀에 기대지 말고 명시적으로 처리한다.
  - 확인: `adb shell input keyevent KEYCODE_BACK` 후 `adb shell dumpsys activity activities | grep -i hmgchallenge`에서 Activity가 destroyed가 아닌지, 다시 열었을 때 상태가 유지되는지.

## 10. IME 표시와 window resize [문서] — IME

- IME가 뜨면 view를 IME insets만큼 줄이는 것은 Pleos 정책이 아니라 AOSP 기본 동작이다.
- Pleos 사양: EditText/TextField가 **있는** window는 IME에 밀려 올라가는 것이 정상. 텍스트 입력이 **없는** window는 IME가 떠도 움직이면 안 된다(앱이 고쳐야 함).
- 고치는 방법: Dialog·오버레이 window의 fit insets 대상을 `statusBars | navigationBars`로만 한정해 IME insets 영향을 받지 않게 한다. 시스템 바 insets만 따로 반영할 때는 `View.OnApplyWindowInsetsListener`에서 처리한다. Activity는 Google의 "입력기 표시 처리" 가이드를 기준으로 따로 검토한다.

우리 앱 적용 [문서에 없음 — Compose 구현 방식]
- Pleos는 landscape에서 window 2개라 다른 앱의 IME가 떠 있는 상황이 생긴다. 우리 화면 대부분은 입력이 없으므로 움직이면 안 된다.
- `Modifier.imePadding()`·`WindowInsets.ime`는 TextField가 있는 화면(주차 중 설정·검색)에만 쓴다. 공통 Scaffold에 `safeDrawing`처럼 IME를 포함한 insets를 일괄 적용하지 않는다(`systemBars`만).
- Compose `Dialog`를 쓸 때 입력이 없으면 dialog window의 `attributes.fitInsetsTypes`를 `statusBars() or navigationBars()`로 설정한다(`DialogWindowProvider`로 window 접근). 입력 있는 dialog는 기본값 유지.
- 확인: 다른 window에서 키보드를 띄우거나 `adb shell input text`로 입력하며 우리 화면이 밀리지 않는지 본다.

## 11. Music/Audio 앱 가이드 [문서] — MUSIC (우리 앱 해당 여부 판단용)

- Music/Audio 카테고리: 음악 스트리밍·재생, 팟캐스트, 오디오북, 라디오처럼 오디오 재생이 주 기능인 앱. 영상 위주 앱은 제외. 음악을 보조 기능으로만 주는 앱이 이 카테고리로 등록하면 즉시 반려.
- 요건 요약: `MediaBrowserService` + `MediaSession` 필수(없으면 배포 제외 가능), 하위 카테고리(음악·팟캐스트·오디오북·라디오) 명확히, 매니페스트 `android:appCategory="audio"`, 스티어링 휠 키로 기본 재생 제어(미디어 센터 연동 또는 MBS 구현), 연결·프로필 변경 시 자동 재생 금지.
- 포커스·서비스: `AUDIOFOCUS_LOSS`(게임 실행 등)면 정지 + 포커스 반납 + 세션 비활성. 서비스는 `onCreate`에서 `startService`로 Started 상태, 재생 시 `startForeground`, `onTaskRemoved`에서 정지 → `stopSelf` → super 순서. 세 손가락 동작 시 `onStop()`이 올 수 있다.
- Gleo AI: Music/Audio 앱은 곡 검색·재생 + 핵심 기능 3~5개 음성 기능을 Gleo Interface(App Schema 등록) 또는 Media3 `MediaLibraryService` 표준 구현으로 연동할 수 있다(후자는 "향후 업데이트 준비 중").

우리 앱 적용 [문서에 없음 — 팀 판단]
- 우리 앱은 오디오 재생이 주 기능이 아니므로 Music/Audio 카테고리가 아니다. `appCategory="audio"`, MBS, `MediaSession`을 만들지 않는다. 자장가·동요 재생 같은 기능을 넣으면 "보조 기능 음악"에 해당해 카테고리 문제가 생길 수 있으니 넣지 않는다(넣는다면 시스템 미디어 앱으로 넘기는 링크 정도).
- 이 페이지의 포커스 매핑(LOSS → 정지·반납, TRANSIENT → 일시정지)은 6장 음성 안내에도 그대로 쓴다.

## 12. Maps/Navigation 앱 가이드 [문서] — NAV (우리 앱 해당 여부 판단용)

- 기본 원칙: 운전자 안전 최우선, 단일 경로·단일 내비, 주행 중 조작 최소화, 카테고리 기반 기능 제한, 플랫폼·AAOS 표준 API와 UX 가이드 준수.
- 내비 앱은 런처 intent-filter에 `android.intent.category.APP_MAPS`를 선언하고 App Market 등록 카테고리와 맞춘다(내비컨테이너 사용 조건). 선언과 실제 기능이 다르면 즉시 반려, 심사 중 기능이 바뀌면 재심사.
- 단일 경로 정책: AAOS에서 Active Route는 내비 앱 하나만. Active Route일 때만 음성·TBT 안내, Inactive(지도 탐색)·Background에서는 안내 금지. 폰 프로젝션과도 배타. `CarAppFocusManager.requestAppFocus`·`OnAppFocusChangedListener`로 경로 소유권을 다룬다.
- 경로 전환은 사용자의 명시적 선택이나 기존 내비 오류·종료 때만. 전환을 알리고 동의 없이 자동 전환하지 않는다.
- 주행 중 금지: 3 depth 넘는 메뉴, 긴 리스트 스크롤, 영상·애니메이션·광고. 허용: 음성 입력, 단일 터치 주요 기능, 즐겨찾기 목적지 선택.
- UX 심사: 터치 영역 최소 크기, 주행 핵심 정보 상시 노출, 주행 중 전체 화면 팝업 금지.
- 권한: 위치는 필수, 마이크는 음성 입력 때만, 불필요한 권한은 반려. 공식 API만, 비공식 센서·API 금지.
- 성능: 실행·전환 지연 최소, 장시간 안정, crash·freeze는 반려.
- 즉시 반려: 주행 중 광고, 영상·게임 요소, 복수 내비 동시 경로 안내, 카테고리 위반, OEM/AAOS 정책 위반.

우리 앱 적용 [문서에 없음 — 팀 판단]
- 우리 앱은 내비 앱이 아니다. `APP_MAPS`를 선언하지 않고, 자체 경로 안내·TBT·지도 표시를 하지 않는다. 경유지 추가는 NaviHelper로 시스템 내비(Active Route 소유자)에 요청만 한다. 그러면 단일 경로 정책을 어기지 않는다.
- 경유지를 바꿀 때는 "이 장소를 경유지로 추가할까요?"처럼 사용자가 한 번 눌러 확인하게 한다(동의 없는 자동 변경 금지 취지).
- 주행 중 전체 화면 팝업 금지, 3 depth 이하, 광고 없음은 내비 앱이 아니어도 그대로 지킨다(DD·`pleos-ui.md`와 같은 방향).

## 13. FAQ — 실행 환경·플랫폼 리소스 [문서] — FAQ

- 플랫폼 구분: `Build.DEVICE`(`ro.product.device`)가 실차에서 `CONNECT_S`, `CONNECT_L`, `CONNECT_W`. **에뮬레이터에서는 유효하지 않으므로** 플랫폼 구분에 쓸 수 없다.
- 지역 코드: 프로퍼티 `product.sw.target_region` — 한국 `KOR`, 유럽 `EUR`, 북미 `USA`·`CAN`. 지역별 UX 정책을 이 값으로 적용하라고 되어 있다.
- 내비 탑재 여부 확인 방법은 공개하지 않는다(제휴 문의 안내).
- 클러스터와 같은 속도: 클러스터는 AAOS `PERF_VEHICLE_SPEED`를 쓴다. 같은 값을 쓰려면 권한 선언 후 `CarPropertyManager`로 조회·구독한다. GPS로 계산한 속도는 클러스터와 다를 수 있다.
- 배터리 충전 상태: Vehicle SDK `getEvBatteryStateOfCharge`.
- 미디어 코덱: 실차(Connect-S/L, Connect-W)와 에뮬레이터 지원 코덱이 다르다. 에뮬레이터 결과를 실차에 그대로 적용하지 않는다. 에뮬레이터 오디오 디코더: AMR-NB/WB, FLAC, G.711, AAC, MP3, Opus, PCM, Vorbis. 두 실차 계열 모두 AAC·MP3(S/L)·Opus·FLAC·PCM을 지원한다.

우리 앱 적용
- [문서에 없음] 지역 분기: DR(`pleos-dev.md` 2.2)은 "플랫폼이 지역별 규제를 `ActiveRestrictions`로 내려주니 앱은 받은 값만 반영"이라 하고, FAQ는 "앱이 `target_region`으로 지역 UX 정책을 적용"하라고 한다. 우리는 주행 규제는 플래그만 보고(지역 판단 없음), 지역 값은 언어·단위 같은 표시용으로만 쓴다. `target_region`을 읽는 방법(시스템 프로퍼티 접근 가능 여부)은 문서에 없어 확인 필요.
- 속도가 필요하면 Vehicle SDK가 아니라 `CarPropertyManager` + `PERF_VEHICLE_SPEED`(권한 `android.car.permission.CAR_SPEED`, [팀 확인] 에뮬레이터에서 dangerous라 런타임 허용 필요). 주행/정차 판단은 속도 대신 `CarUxRestrictions`로 충분하므로 지금은 쓰지 않는다.
- 안내 음원은 AAC(m4a)나 Opus(ogg)로 넣는다. 에뮬레이터·실차 모두 지원 목록에 있다.

## 14. ADB 명령 [문서] — ADB

- 설치: macOS Homebrew `android-platform-tools`, Windows choco/winget, Ubuntu apt. 우리 레포는 `scripts/_common.sh`가 SDK의 platform-tools를 PATH에 넣는다.
- 기본: `adb devices`, 화면 캡처 `adb exec-out screencap -p > 파일`, 녹화 `adb exec-out screenrecord --output-format=h264 -`(Ctrl+C로 종료) 후 ffmpeg로 mp4 재포장.
- 설치: `adb install -d -r <apk>`(실패하면 `/data/local/tmp`에 push 후 `pm install`). 실행: `monkey -p <패키지> -c android.intent.category.LAUNCHER 1`(GNB에서 누른 것과 비슷) 또는 `am start -n`. 종료 `am force-stop`, 삭제 `uninstall` + `pm clear`.
- 텍스트 입력: `adb shell input text "<문자열>"`.
- 기어 변경은 Virtual Console로(15장).
- **주행 규제 켜기/끄기**: `adb shell cmd hmg_car_service --service drivingstate driving_regulation on|off`. 원문은 "모든 차종 공통"이라고 한다.
- 유틸리티 모드 전환 명령(차속 0·주차 브레이크·P 상태에서 `/dev/vcs_simulator_rx`에 바이트 쓰기)도 있다. 실차·개발 기기용으로 보이며 우리는 쓰지 않는다.
- CCS 연결 명령(`hmg_is_ccs_account_linked` 등)은 CCS 차량 전용이고 Devbox·에뮬레이터에서는 쓰지 말라고 되어 있다.

우리 앱 적용
- 화면 캡처는 레포 규칙대로 `scripts/screenshot.sh "$TMPDIR/..."`를 쓴다(스크린샷을 git에 넣지 않음).
- 시연 영상 녹화에 위 screenrecord + ffmpeg 방식을 쓸 수 있다. 영상에 계정·개인정보가 보이지 않게 한다.
- `hmg_car_service ... driving_regulation`은 에뮬레이터(user 빌드)에서 되는지 아직 시험하지 않았다(2026-10-28 에뮬레이터 미실행). 된다면 Virtual Console보다 간단한 주행 제한 테스트 방법이 된다. 확인 필요(16장). 지금 확인된 방법은 `pleos-dev.md` 6장(Virtual Console 기어 D + 속도).

## 15. Virtual Console 카드 [문서] — VC

- Emulation Controls에서 도메인을 고르거나 카드 이름을 검색하고, 영역(좌석·도어 등)을 고른 뒤 버튼·목록·슬라이더로 값을 바꾼다. 결과는 에뮬레이터와 앱에서 확인한다. 접속 방법은 `pleos-dev.md` 6장, `scripts/pleos-console.sh`.

| 도메인 | 카드 (우리 앱 관련은 굵게) |
| --- | --- |
| Vehicle | Notification 1(차량 알림 표시·숨김) |
| Motion | **Gear Selection(P/R/N/D)**, Brake Pedal(0~100%), Odometer, Steering Angle(±540°), Steering Wheel Heat |
| ADAS | Cruise Control State, Cruise Target Speed |
| Convenience | ETCS(하이패스) 전원·시나리오, **Door Position(앞뒤 좌우)**, Door Lock, Trunk/Frunk, 실내등, **Reading Lights(앞뒤 좌우)**, 앰비언트, 외부등·상향등·비상등·제동등, 방향지시등, **Window Position(앞뒤 좌우)**, Window Lock, **Seat Occupancy(운전석·동승석·뒷좌석 좌우)**, 와이퍼, 글로브 박스 |
| Safety | Telltales(주차 브레이크·안전벨트·ABS·TPMS 등 경고등) |
| Energy | Remaining Range, Battery Energy, **Battery SOC(0~100%)**, Battery Capacity, 충전 전류·목표 충전량·충전 스위치·충전 포트 |
| Climate | 공조 전원, 에어컨, Auto, Dual, 내·외기, 풍량(0~8), 풍향, 성에 제거, **Target Temperature(운전석·동승석, 16~30°C)**, 시트 온도, 공기청정 |

- 원문 카드 목록에는 차속(가속 페달) 카드가 없다. [팀 관찰] 실제 Virtual Console 화면에서는 기어 D + 가속으로 속도를 올릴 수 있었다(`pleos-dev.md` 6장). 문서와 화면이 다르면 화면 기준으로 테스트하고 이 표를 고친다.
- 뒷좌석 착석(Seat Occupancy)·뒷문·뒷창·독서등은 바꿀 수 있다. 뒷좌석 공조 구역과 현재 실내 온도는 카드에 없다. 뒷좌석·뒷문 상태를 쓰는 시나리오는 이 카드로 시연할 수 있다. 단 해당 Vehicle 도메인 권한(`CAR_SEATS`, `CAR_DOORS` 등)을 선언해야 하므로 쓰기 전에 권한 결정을 먼저 한다(`AGENTS.md` 필수 요건).

## 16. 확인 못 한 것·열린 질문

- 읽지 않은 링크 페이지: 앱 보안 가이드(`/app-registration-guide/app-security-guide`), Gleo Interface 구현 가이드(`/app-integration-guide/guide/core-concepts`), CCP UX 가이드라인(Figma 파일). 제출 전에 앱 보안 가이드는 읽어 4장을 보완한다.
- Suspend 진입·복귀를 서드파티 앱이 감지하는 공식 방법(SUSP 예제에 없음).
- Pleos TTS SDK가 오디오 포커스와 선행 묵음을 서비스 쪽에서 처리하는지(AF·PRE·TTS 문서 모두에 없음).
- `hmg_car_service ... driving_regulation on|off`가 에뮬레이터 user 빌드에서 동작하는지.
- `product.sw.target_region`을 서드파티 앱이 읽을 수 있는지, 에뮬레이터 값이 무엇인지.
- App Market 심사 기준(CCP Back, 카테고리, 성능)이 공모전 심사에도 그대로 쓰이는지. 설명회(2026-10-28) 질문 목록에 넣는다(`docs/contest/rules.md` 8장).
