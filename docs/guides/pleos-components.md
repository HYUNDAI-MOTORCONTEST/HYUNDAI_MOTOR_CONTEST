# Pleos Connect 컴포넌트·Foundations 요약 (Compose 구현용)

> 직접 쓴 요약이다. 원문을 옮기지 않았고, 최종 기준은 각 항목의 원문 URL이다.
> 확인일: 2026-10-28 (한국어 페이지 기준)
> 표기: **[문서]** 원문에 있는 내용, **[문서에 없음]** 원문에 수치·규칙이 없어 다른 근거로 정한 팀 권장값(근거를 같이 적음).
> 화면 구조·색·글자·아이콘·Basic button·Compose 실무 수치는 [pleos-ui.md](pleos-ui.md)에 있다. 여기서는 그 밖의 디자인 가이드 페이지를 다룬다.

원문 약칭 (모두 `https://document.pleos.ai/docs/connect/guide/docs-design` 아래)

| 약칭 | 페이지 | 약칭 | 페이지 |
| --- | --- | --- | --- |
| DRIVE | `/Introduction/designing-apps-for-driving` | GNB | `/components/gnb` |
| TOKEN | `/Introduction/design-token` | INDI | `/components/indicator` |
| THEME | `/Introduction/theme` | PAGENAV | `/components/page-navigation` |
| DV | `/Experiences/driving-view` | TABS | `/components/segmented-menu` (제목은 Tabs) |
| GLEO | `/Experiences/gleo-ai` | SPIN | `/components/spinner` |
| ANIM | `/foundations/animation` | STATUS | `/components/status-bar` |
| SHADOW | `/foundations/shadow` | TOAST | `/components/toast` |
| INTER | `/foundations/interaction` | WIDGET | `/components/widgets` |
| APPICON | `/foundations/contents-creation/app-icon` | NUMF | `/components/fields/number-field` |
| 3D | `/foundations/contents-creation/3d-contents` (제목은 3D Assets) | TEXTF | `/components/fields/text-field` |
| ICONBTN | `/components/actions/icon-button` | SYSNOTI | `/components/notification/system-notification` |
| TEXTBTN | `/components/actions/text-button` | VEHNOTI | `/components/notification/vehicle-notification` |
| TOGGLE | `/components/actions/toggle-button` | POPUP | `/components/popups/system-popup` |
| KEYPAD | `/components/actions/keypad` | TOASTP | `/components/popups/toast-popup` |
| APPLIB | `/components/app-library` | CHECK | `/components/selections/checkbox` |
| BAR | `/components/bar` | RADIO | `/components/selections/radio` |
| CTRL | `/components/controllers` | SWITCH | `/components/selections/switches` |
| DROP | `/components/dropdown` | | |

공통 사항
- 컴포넌트 페이지는 대부분 **구성 요소(Anatomy)·상태(States)·속성(Properties)만 글로 적고, 크기·간격·색 견본은 이미지에만** 둔다. 이미지에도 치수 표기가 없는 경우가 많다(ICONBTN 스타일 이미지 1장을 열어 확인: 상태 견본만 있고 치수 없음). 실제 치수는 INTRO 하단 "Pleos Connect design assets"(Figma)에서 확인해야 한다.
- 크기 옵션(Large/Medium/Small 등)의 실제 dp 값은 전부 **[문서에 없음]**이다. 우리 앱은 `pleos-ui.md` 10장 규칙(터치 64dp 이상, 주요 버튼 72dp 이상, 간격 24dp)을 따른다.
- "시스템 UI" 표시가 붙은 항목은 OS가 그리는 화면이다. **앱이 만들지 않고, 겹치지 않게 배치만 고려**한다.

---

## 1. 운전용 앱 설계 원칙 [문서] — DRIVE

- 페이지 제목은 "Introduction"이고 내용은 INTRO의 기본 원칙(Easy & Safe, 일관성, 즉시성, 0.25초 응답)과 거의 같다. 요약은 `pleos-ui.md` 1장을 본다.
- 추가된 점: Pleos Connect는 Connect-S·Connect-W 등 폼팩터에 맞춰 디자인 시스템을 갱신했다. Connect-W는 **좌석(운전석·조수석)별로 특화된 UI와 상호작용**을 준다.
- 운전 중 시선 유도 금지(동영상, 움직이는 이미지, 텍스트 자동 스크롤), 한 손·단순 조작, 손발을 크게 움직이게 하는 동작 금지는 INTRO와 같다.

## 2. 디자인 토큰·테마

### 디자인 토큰 [문서] — TOKEN
- 토큰은 색·글꼴·간격·그림자 같은 스타일 값을 코드 단위로 묶은 것이다. 디자인과 코드의 단일 기준으로 쓰고, 주간/야간·브랜드 테마 전환을 쉽게 한다.
- 이름 규칙: 접두 `ivi` + snake_case. 이름만 보고 쓰임새를 알 수 있어야 하고, **약어 대신 전체 단어**를 쓴다(예: `vs`가 아니라 `vehicle_setting`).
- 토큰 구조도와 이름 규칙 예시는 이미지에만 있다.
- 우리 코드: `PleosColors.kt`의 토큰 이름은 문서 토큰명에서 `ivi_` 접두를 뺀 것이다(`pleos-ui.md` 5장). 모션 토큰은 4장 INTER 참고.

### 테마 [문서] — THEME
- 테마는 아이콘·글꼴·색으로 **차량 브랜드 정체성**을 유지하려고 쓴다. 본문은 한 문장이고 나머지(Types, 선택 상태, 샘플)는 이미지다.
- 이미지 1장(Types)을 열어 확인: 42dot / Kia / Genesis 테마가 있고, 같은 컴포넌트(Quick controls, Basic button)가 브랜드마다 선택 상태 색·모서리 모양이 다르다(예: 42dot 초록 표시, Kia 짙은 남색, Genesis 주황 바탕).
- [문서에 없음] 서드파티 앱이 차량 브랜드 테마를 따라야 하는지, 앱이 현재 브랜드 테마를 알아내는 방법. → 팀 판단: COLOR 페이지의 기본 토큰(`PleosColors`) 한 벌만 쓴다. 브랜드 색을 흉내 내지 않는다.

## 3. Experiences (시스템 경험)

### Driving view — 시스템 UI, 앱이 만들지 않음 [문서] — DV
- 운전자에게 차량 상태·주행 정보를 보여 주는 영역이다.
- 구성: ① Indicator(텔테일, 도로 표지판, 에너지, 자율주행·회생제동 표시, 도어 잠금 스위치 등. 일부는 상황에 따라 나타남) ② Driving information(변속단, 속도, 에너지/퍼포먼스 게이지, 주행거리, mini TBT, 드라이빙 모드) ③ View contents(차량 외부 제어, 근접 센서·주변 물체, 차선·노면).
- 앱이 할 일: 이 영역을 가리지 않는다(`pleos-ui.md` 2장). 같은 정보를 앱에서 크게 반복할 필요가 없는지 먼저 본다. Case Variation은 이미지뿐이다.

### Gleo AI — 시스템 UI, 앱이 만들지 않음 [문서] — GLEO
- 개인형 모빌리티 어시스턴트. 대화로 답하거나 기능을 실행하고, 놓치면 안 되는 사항을 먼저 제안(proactive suggestion)한다.
- 대화 상태: Sleep(대기) → Listening_Passive(입력 대기, 2초간 말이 없으면 "어느 좌석에서 듣는 중" 안내) → Listening_Active(발화 중) → Thinking(처리 중) → Asking(되묻기) → Speaking(최종 응답) → Standby(응답 화면이 있으면 5초, 없으면 0초 유지 후 Sleep).
- Chrome(빛 모양 표시)이 상태를 구분하고 Lottie 애니메이션으로 그린다.
- 레이어: OS에서 **GNB 바로 아래 레이어**에 있어 앱 화면을 잠시 가릴 수 있다. 외부 이벤트로 닫힐 수도 있다.
- 구성: Chrome, Bubble(사용자 발화·응답), Panel(Template = list/card, Chips = 선택지 버튼).
- 패널 조작: 리스트 항목·칩 터치 = 기능 실행, 패널을 위로 끌거나 바깥 터치 = 대화 종료, 아래로 끌기 = Gleo AI 앱으로 전환.
- CCP(Center Control Panel, 조그 다이얼): rotate·tilt·push로 포커스를 옮기고 선택한다. Gleo 대화 중에는 팝업과 같은 포커스 정책(tilt 제한, Gleo가 우선 포커스).
- 화면 크기별 배치(Standard 14.6/17, Wide Standard/Extended, Waterfall)는 이미지뿐이다. Wide에서는 말한 좌석 가까이에 뜬다.
- 앱이 할 일: Gleo가 앱을 덮어도 상태가 깨지지 않게 한다(일시 정지·복귀). Gleo chrome·버블 모양을 앱 안에서 흉내 내지 않는다. `gleo_*` 색 토큰은 시스템용이라 앱에서 쓰지 않는다.
- [문서에 없음] 서드파티 앱이 Gleo에 음성 명령을 등록하는 방법(UI 가이드 범위 밖). CCP 포커스를 앱이 어떻게 지원해야 하는지도 이 페이지에는 없다.
- 구현 메모: CCP 대응은 Compose 표준 포커스(`Modifier.focusable()`, 포커스 표시)로 준비해 둘 수 있다. [문서에 없음] 팀 판단, 에뮬레이터에서 CCP 입력 방법은 확인 필요.

## 4. Foundations

### 애니메이션 [문서] — ANIM
- 애니메이션은 **상태를 설명하려는 목적**으로만 쓴다. 로딩 애니메이션 파일 다운로드 링크가 있다.
- 로딩 옵션 2가지: Linear(짧은 일반 로딩), Tension(페이지 로딩처럼 오래 걸릴 때).
- 구현 메모: `CircularProgressIndicator`(색 `icon_dark_secondary` 등)로 시작한다. 공식 Lottie를 쓰려면 Lottie 의존성을 추가해야 하므로 팀 결정 사항이다.
- [문서에 없음] 운전 중 로딩 표시 허용 여부. → 팀 판단: 로딩 표시는 상태 피드백이라 허용한다. 장식·전환 애니메이션은 `LocalUxRestrictions`가 제한 중이면 끈다(INTRO의 "움직이는 이미지 금지"를 따름).

### 그림자 [문서] — SHADOW
- 3단계 elevation. 값은 본문에 텍스트로 있다(단위 표기 없음, 디자인 툴 기준 px로 보임).

| 단계 | 쓰임 | 그림자 1 | 그림자 2 |
| --- | --- | --- | --- |
| Level 1 | 가장 낮은 콘텐츠 | Y 6, Blur 24, 검정 8% | - |
| Level 2 | Level 1보다 높음 | Y 8, Blur 32, 검정 16% | Y 2, Blur 2, 검정 8% |
| Level 3 | 가장 높은 콘텐츠 | Y 20, Blur 40, 검정 24% | Y 2, Blur 4, 검정 20% |

- 모두 X 0, Spread 0.
- 구현 메모: M3 `Modifier.shadow(elevation)`은 offset·blur·색을 따로 못 정한다. Compose UI 1.9+의 `Modifier.dropShadow(shape, Shadow(...))`를 두 번 겹쳐 값을 옮긴다(우리 BOM 2026.09, 정확한 시그니처는 처음 쓸 때 빌드로 확인). [문서에 없음] Figma blur → Compose radius 환산은 같은 값으로 넣고 캡처로 비교한다. 야간 테마 그림자 값은 문서에 없다.
- 어디에 쓰는지(카드·팝업·플로팅 버튼별 단계)도 [문서에 없음]. → 팀 판단: 카드 = Level 1, 플로팅 토글·떠 있는 패널 = Level 2, 팝업 = Level 3.

### 인터랙션 [문서] — INTER
- **Easing**: 기본 곡선 `cubic-bezier(0.16, 1, 0.3, 1)`(이미지에서 확인).
- **Duration 토큰**(이미지에서 확인): `ivi_motion_duration_short1` 100ms, `short2` 150ms, `medium1` 250ms, `medium2` 300ms, `long1` 400ms, `long2` 450ms. Short = 작은 기능성 전환, Medium = 화면 중간 범위 이동, Long = 큰 전환(강조 easing과 함께).
- **제스처**: Pressed(실행/종료), Long pressed(2~3초 누르면 편집 모드, 시스템 앱 기준), Swipe와 Drag는 누른 뒤 이동 속도로 구분(빠르면 Swipe, 느리면 Drag). 정해진 방향이 아니면 취소된다.
- **버튼 인터랙션**: 터치 가능한 요소는 모두 눌렀을 때 **95%로 줄어들고(shrink) 모드별 배경색이 바뀐다**. 손가락이 영역 밖으로 나가면 취소, 영역 안에서 떼면 실행한다.
- **Window 조작**(시스템): window bar를 누르면 수축, 다른 window 폭의 50% 넘게 끌면 자리 이동, 아래로 끌어 높이의 60% 넘게 가서 놓으면 앱 종료(59% 이내면 복귀).
- **스크롤 끝**: 끝에 닿으면 bounce로 알린다. 넘침 허용 범위는 전체 스크롤 영역의 약 2/5.
- 구현 메모:
  - 모션 상수는 `ui/theme`에 `PleosMotion` 같은 객체로 두고 `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)`, `tween(durationMillis = 100…450)`으로 쓴다. [문서에 없음] 파일 이름·위치는 팀 판단.
  - 누름 효과: `interactionSource.collectIsPressedAsState()` → `animateFloatAsState(if (pressed) 0.95f else 1f, tween(100))` → `Modifier.graphicsLayer { scaleX = s; scaleY = s }`. 배경은 `button_*_pressed` 토큰. M3 ripple은 끄거나(`indication = null`) 약하게 한다. [문서에 없음] ripple 처리 방식은 팀 판단.
  - 누름 효과는 조작 확인용 피드백이라 운전 중에도 유지한다. [문서에 없음] 팀 판단.
  - Compose `onLongClick` 기본 시간(약 0.4~0.5초)은 문서의 2~3초와 다르다. 길게 누르기를 유일한 진입 경로로 만들지 않는다.
  - Overscroll bounce: Compose 기본(Android 12+ stretch)과 다르다. [문서에 없음] 커스텀 구현 여부는 팀 결정, 우선 기본값 유지.

### 앱 아이콘 [문서] — APPICON
- GNB·App library·App Market에 쓰이는 대표 이미지.
- 그리드 시스템을 따르고, **Foreground(로고·주요 그래픽)와 Background(배경) 레이어를 나눠** 만든다. 차량 브랜드별 마스킹 스타일이 적용되므로 **Android Adaptive Icon 형식**으로 만든다.
- 그리드·브랜드별 마스크 템플릿은 "Pleos Connect design assets" Figma에 있다.
- 구현 메모: `res/mipmap-anydpi/ic_launcher.xml`(`<adaptive-icon>`)에 foreground·background를 나눠 둔다. 로고를 안전 영역 안에 두어 마스크에 잘리지 않게 한다. 치수는 Android Adaptive Icon 규격(108dp 캔버스, 안전 영역 지름 66dp)을 따른다(Pleos 수치 없음, Android 기준).

### 3D 에셋 [문서] — 3D
- 직관적인 조작을 위해 3D 모델 사용을 권장한다(공조를 실내 모델 위에서 좌석별로 조작, 스티어링 휠 클로즈업, 시트 위치 + 버튼 연동 예시).
- 모델: Lowpoly 차량 데이터(내외장 포함 3ds Max 30MB, FBX 6MB 기준). CAD에서 뽑은 Highpoly는 리토폴로지. 머티리얼·오브젝트를 줄여 draw call을 최소화.
- 포맷 **glTF**, 엔진 **Google Filament**(PBR, 모바일 우선).
- 애니메이션은 glTF 애니메이션 키 또는 회전값으로 제어하고, 파트 이름 규칙(예: `Door_F_L`, `Trunk`, `Charging_Port`, `Seat_F_L_Top`)과 램프 머티리얼 이름·발광값 표가 부록에 있다. 전체 표는 원문 참고.
- 구현 메모: Compose에서는 `AndroidView`로 Filament `SurfaceView`/`TextureView`를 감싼다. 의존성·APK 용량이 크게 늘고 운전 중 움직임 제한도 걸리므로 우리 앱에서는 **쓰지 않는 것을 기본**으로 한다. [문서에 없음] 팀 판단. 차량 3D 원본 데이터의 제공 여부도 문서에 없다.

## 5. Components

### 5.1 Actions

**Icon button** [문서] — ICONBTN
- 누구나 바로 알아보는 아이콘일 때만 쓴다. 구성: Root, Icon.
- 상태: Pressed, Disabled. 속성: Style(Basic·Plain·Circle), Size(Large·Medium·Small).
- 원문 States 표에서 Pressed 설명이 Bar 페이지 문구("Drag 가능한 window일 때")와 같다. 원문 표기 오류로 보인다.
- 구현 메모: `IconButton`이 아니라 `Box` + `clickable`(또는 `Surface(onClick)`)로 만들어 크기를 직접 정한다. Basic = `button_basic_enabled/pressed/disabled` 배경, Plain = 배경 없음, Circle = `CircleShape`. 아이콘색 `icon_dark_primary`, disabled는 `icon_dark_tertiary`. 터치 영역 64dp 이상, 아이콘 48dp(`pleos-ui.md` 8·10장). 아이콘만 있는 버튼에도 `contentDescription`과 가능하면 텍스트 라벨.

**Text button** [문서] — TEXTBTN
- 부가 정보로 가는 링크 용도. 앞·뒤 아이콘을 붙일 수 있다. 구성: Root, Label, Icon(Prefix/Suffix).
- 상태: Pressed, Disabled, Loading(로더 + 조작 불가). Size: Medium·Small.
- 구현 메모: M3 `TextButton` 기반, 글자 `labelMedium` 이상, 색 `text_dark_primary`/`text_dark_secondary`. 최소 높이 64dp는 우리 규칙으로 맞춘다.

**Toggle button** [문서] — TOGGLE
- On/Off 버튼. 여러 옵션의 켜짐 상태를 보여 줄 때 좋다. 구성: Root, Label, Icon(Prefix/Suffix).
- 상태: Toggled(기본 꺼짐), Pressed, Disabled. 속성: Floating(배경에 따라 그림자 또는 색으로 구분), Size(L·M·S).
- 구현 메모: `Surface(selected, onClick)` 또는 `Modifier.toggleable`. 켜짐 배경은 `button_switch_*` 토큰이 맞을 것으로 보이나 문서에 연결 표기가 없다(추정, Figma 확인 필요). Floating이면 SHADOW Level 2. 켜짐을 색만으로 표시하지 말고 아이콘·라벨 변화도 함께.

**Keypad** [문서] — KEYPAD
- 숫자·문자 입력용. 구성: Root, Label, Icon(Prefix/Suffix). 상태: Pressed, Disabled.
- 속성: 키 종류 Primary·Secondary·Special, 입력 방식 Text·Voice. 숫자 패드·쿼티·키보드 예시는 이미지뿐.
- 구현 메모: 문자 입력은 **시스템 키보드(IME)**를 쓴다. 앱이 직접 키보드를 만들지 않는다. 전화번호 같은 숫자 패드가 꼭 필요할 때만 `keyboard_normal_*` 토큰으로 직접 그린다. 운전 중에는 키보드 입력을 막는다(`pleos-dev.md` 2장).

### 5.2 Navigation·구조

**Page navigation (앱 상단 바)** [문서] — PAGENAV
- 앱의 주요 이동 경로를 주는 상단 요소. 구성: Root, App icon, Primary menu(첫 번째 카테고리 드롭다운), Secondary menu(선택), Suffix(앱별 아이콘 메뉴).
- 타입: Main(기본), Depth(하위 페이지, Back 버튼 표시). 속성: Primary, Secondary, Subtitle.
- 원문 Properties 표의 설명 칸이 Types·Button 문구와 겹쳐 뜻이 맞지 않는다(원문 표기 오류로 보임).
- 구현 메모: M3 `TopAppBar`보다 `Row`로 직접 만드는 편이 크기 제어가 쉽다. Depth에서는 왼쪽에 Back 아이콘 버튼. 제목 `titleMedium`, 배경 `background_primary`. 드롭다운 메뉴는 운전 중 조작 부담이 크니 최소화한다(아래 DROP).

**Tabs** [문서] — TABS
- 2개 이상 콘텐츠·기능 옵션을 바꿀 때. 구성: Root, Tab item, Label, Selected indicator, Tab container.
- 상태: Enable, Pressed, Selected. 속성: Type(Box·Line), Fitted(true = 너비에 맞춰 균등, false = 왼쪽 정렬).
- 구현 메모: M3 `PrimaryTabRow`(Fitted)·`PrimaryScrollableTabRow`(왼쪽 정렬) 또는 `SingleChoiceSegmentedButtonRow`(Box형). 배경 `controller_tab_background`. [문서에 없음] 탭 개수 상한. → 팀 판단: 라벨이 잘리지 않는 수(보통 4개 이하), 넘치면 화면을 나눈다.

**Indicator (페이지 표시)** [문서] — INDI
- 현재 값(Present)과 앞뒤 값(Next/Prev)을 보여 주는 페이지 표시. Size: Wide·Narrow.
- 구현 메모: `HorizontalPager`와 함께 점 `Row`로 그린다. 페이지가 여럿이면 화살표 버튼도 둔다(스와이프만으로 이동하게 하지 않음, [문서에 없음] 팀 판단).

**Bar (Window bar·Scroll bar)** [문서] — BAR
- Window bar = window 크기·위치 조정 손잡이(**시스템 UI — 앱이 만들지 않음, 겹치지 않게 배치만 고려**). Scroll bar = 콘텐츠 탐색용.
- 상태: Variant(Light/Dark, 앱이 정한 타입에 따라), Dragging, Disabled(손잡이 숨김), Pressed.
- 구성: Root, Control(손잡이 + 최소 드래그 영역), Handle.
- 구현 메모: 앱 상단 가장자리에 중요한 버튼을 붙이지 않는다(window bar와 겹침). [문서에 없음] window bar 높이. Scroll bar는 Compose 기본 제공이 없어 긴 목록에 직접 그리거나 생략한다. 운전 중에는 긴 목록 자체를 줄인다.

### 5.3 Controllers·선택

**Controllers (Slider·Stepper)** [문서] — CTRL
- 최솟값~최댓값 범위의 값(볼륨, 온도 등)을 슬라이더나 버튼으로 조작한다. 옵션: MinValue, MaxValue, Step, Disabled(최소·최대에 닿으면 true). 상태: Pressed/Dragging.
- Continuous slider: Root, Control, Handle, Track, Icon(Prefix 선택/Suffix), Stepper. Basic과 Floating(밝은 배경·구분 필요할 때). 볼륨 아이콘이 값 구간별로 바뀐다(0%, 1~25%, 26~75%, 76~100%, 100%).
- Centered slider: 가운데를 기준으로 양쪽으로 움직이는 슬라이더(같은 구성, 아이콘 없음).
- Stepper: 현재 값 + 증가(i++)·감소(i--) 버튼.
- 구현 메모: M3 `Slider`(Step은 `steps`로)에 `SliderDefaults.colors`로 `controller_slider_normal/pressed/knob`을 넣는다. Centered slider는 기본 제공이 없어 Track을 직접 그린다. Stepper 배경 `controller_stepper_background`. 끝값에서는 해당 방향 버튼을 Disabled로.
- [문서에 없음] 운전 중 슬라이더 드래그 허용 여부. → 팀 판단: 주행 중에는 **Stepper 버튼을 기본 조작**으로 하고 슬라이더는 값 표시 위주로 둔다(흔들림 속 드래그 정확도, INTRO "단순한 조작").

**Switch** [문서] — SWITCH
- On/Off로 상태를 바꿀 때. 구성: Root, Handle, Track. 상태: Checked, Pressed, Disabled, Invalid.
- 구현 메모: M3 `Switch` + `SwitchDefaults.colors`에 `switch_on/off/disabled`, `switch_knob_on/off/disabled`. 행 전체를 `Modifier.toggleable`로 감싸 64dp 이상 터치 영역을 만든다.

**Checkbox** [문서] — CHECK
- 여러 개를 고를 수 있는 옵션. 구성: Root, Control, Icon. 상태: Checked, Pressed, Disabled.
- 구현 메모: M3 `Checkbox`, 행 전체 `Modifier.toggleable`. [문서에 없음] 체크박스 전용 색 토큰. → 팀 판단: 선택 색 `button_filled_enabled`(= M3 primary), 테두리 `icon_dark_secondary`.

**Radio button** [문서] — RADIO
- 하나만 고르는 옵션. 구성: Root, Control(원문 설명은 Checkbox 문구 그대로). 상태: Value(선택 순번 또는 없음), Pressed, Disabled, Invalid.
- 구현 메모: M3 `RadioButton` + 행 전체 `Modifier.selectable`, 묶음은 `Modifier.selectableGroup()`. 색은 Checkbox와 같은 팀 판단.

**Dropdown** [문서] — DROP
- 메뉴 이동이나 속성 변경. 구성: Root, Label, Prefix(앱 아이콘·아이콘), Suffix(스위치·아이콘), Container. 상태: Opened, Pressed, Disabled. Size: Medium·Small.
- 구현 메모: M3 `ExposedDropdownMenuBox` 또는 `DropdownMenu`, 배경 `dropdown_normal`, 누름 `dropdown_pressed`, 팝업 `background_popup`. **선택지가 적으면 Tabs·Radio로 바꾼다**(vehicle-ui 스킬 "드롭다운 지양", 팀 규칙).

**Spinner (휠 피커)** [문서] — SPIN
- 드래그로 여러 값 중 하나를 고른다. 구성: Root, Selection value, Contents. 타입: Date, Time.
- 구현 메모: M3에는 휠 피커가 없다(`TimePicker`는 다이얼형). 운전 중 드래그 선택은 부담이 커서 **Stepper나 프리셋 버튼으로 대신**한다. [문서에 없음] 팀 판단. 꼭 필요하면 주차 중 화면에서만 `LazyColumn` + snap으로 만든다.

### 5.4 Fields

**Text field** [문서] — TEXTF
- 텍스트·비밀번호·이메일 등 입력. 구성: Root, Field, Cursor, Placeholder, Suffix(선택).
- 상태: Value, Focused, Disabled(포커스 불가), Read only(포커스는 되지만 수정 불가), Invalid. 타입: Text·Tel·Url·Email·Password. Size: Medium·Small. 입력 중 모습은 이미지뿐.
- 구현 메모: M3 `TextField`/`BasicTextField` + `KeyboardOptions(keyboardType = …)`. 배경 `field_background`, 포커스 `field_focused`, Invalid는 `informative_negative` 테두리 + 오류 문구(색만으로 알리지 않음, `pleos-ui.md` 5장 대비 주의). **운전 중에는 입력 비활성화** + "주차 후 이용" 안내.

**Number field** [문서] — NUMF
- 숫자 입력. 구성: Root, Label, Cursor, Field. 상태는 Text field와 같다. Size: Large·Medium.
- 구현 메모: `KeyboardType.Number`. 범위가 정해진 값이면 Stepper(CTRL)를 먼저 고려한다.

### 5.5 알림·팝업

**Toast / Toast popup** [문서] — TOAST, TOASTP
- 앱 운영 중 잠깐 띄우는 알림. 구성: Root, Toast message. 두 페이지가 같은 이미지를 쓴다.
- 수치: 앱 패널 안에서 **좌우·아래 여백 24**(TOASTP는 24dp로 표기), **전체 최대 너비 792, 메시지 글자 최대 너비 712**(단위 표기 없음, dp로 봄).
- [문서에 없음] 표시 시간, 색, 최대 줄 수. → 팀 판단: 표시 3~4초, 1~2줄, 배경 `basic_700`/글자 `basic_00`(M3 inverseSurface 매핑과 같음).
- 구현 메모: Android `Toast`는 모양을 못 바꾸므로 앱 안에 `Box(Modifier.align(BottomCenter).padding(horizontal = 24.dp, bottom = 24.dp).widthIn(max = 792.dp))`로 직접 그린다. 등장·퇴장 애니메이션은 운전 중 끄거나 페이드만.

**System popup (대화상자)** [문서] — POPUP
- 앱 안에서 쓰는 기본 팝업. Body에 서비스 맥락에 맞는 콘텐츠를 넣을 수 있다.
- 구성: Root, Title, Description(선택), Confirm action, Cancel action(선택).
- 속성: Header(Icon·SubTitle·Description), Body(on/off), Size(Large·Medium), Button align(Single·Vertical·Horizontal). 크기 수치는 이미지뿐.
- 구현 메모: M3 `BasicAlertDialog` 또는 `Dialog` + `Surface`(배경 `background_popup`, SHADOW Level 3). 바깥 어둡게(scrim)는 `dim_*` 토큰이 맞을 것으로 보이나 연결 표기는 없다(추정). 버튼 2개면 Horizontal, 라벨이 길면 Vertical. 제목 1줄, 설명 3줄 이하.

**System notification — 시스템 UI, 앱이 만들지 않음** [문서] — SYSNOTI
- OS가 기본 제공하는 알림. 구성: Root, Imagery, Header, Action. 타입: App icon(앱 알림), Profile(전화·문자), Image(내비 등), Action(버튼). Button align: Single·Vertical·Horizontal.
- 앱이 할 일: 모양을 직접 그리지 않는다. 알림이 필요하면 Android 표준 알림 API로 보내고, 짧게·운전자에게 필요한 것만(광고 금지, `pleos-ui.md` 1장).
- [문서에 없음] 표준 `NotificationCompat`이 이 모양으로 바뀌어 보이는지, 채널·중요도 정책. 에뮬레이터에서 확인 필요.

**Vehicle notification — 시스템 UI, 앱이 만들지 않음** [문서] — VEHNOTI
- 주행 관련 알림(Driving notification). 구성: Root, Telltale 아이콘, Title, Description, Guide image. 속성·위치는 이미지뿐.
- 앱이 할 일: 이 알림이 뜨는 위치를 가리지 않는다. 차량 경고를 앱에서 같은 모양으로 흉내 내지 않는다.

### 5.6 시스템 영역

**Status bar — 시스템 UI, 앱이 만들지 않음, 겹치지 않게 배치만 고려** [문서] — STATUS
- 주행·시스템 상태 영역. 우선순위 높은 SP0(고정: 도어 잠금·검색 / 이벤트: 알림 센터·통화 중·개인정보 표시)과 낮은 SP1(고정: 프로필·블루투스·네트워크 / 이벤트: 다운로드·업데이트·무선충전·하이패스·사운드), 시간, PAB(조수석 에어백), More 메뉴.
- SP0과 SP1 사이에 오른쪽 여백 40dp. 자리가 모자라면 SP1 이벤트 아이콘이 More로 옮겨 간다.
- 메뉴 패널(장치 연결·프로필·알림)이 앱 위로 열릴 수 있다.

**GNB — 시스템 UI, 앱이 만들지 않음, 겹치지 않게 배치만 고려** [문서] — GNB
- 운전석 차량 제어, 앱 영역, 조수석 차량 제어로 나뉜다. 차량 설정, 공조 패널, 앞뒤 유리 열선, 편집 슬롯, 고정 앱(내비), App library, 최근 앱 2개 등.
- 편집 모드: 편집 슬롯이나 App library 항목을 길게 누르면 들어간다. 사용자가 **우리 앱을 GNB 슬롯에 둘 수 있으므로** 앱 아이콘이 작게도 잘 보여야 한다(APPICON).
- OSD: 스티어링 휠 버튼으로 미디어·차량·차간거리를 조작하면 뜨는 패널. Connect-S는 GNB 위치, Connect-W는 GNB 오른쪽. **2초간 조작이 없으면 사라진다.**
- 공조 패널·App library 패널이 앱 화면 위를 덮을 수 있다(`pleos-ui.md` 2장).

**App library — 시스템 UI, 앱이 만들지 않음** [문서] — APPLIB
- 설치된 앱과 공조 단축 버튼이 모인 패널. 위쪽 = 차량 제어, 아래쪽 = 앱. Connect-S 기준 한 줄 최대 7개, 넘치면 좌우 이동 표시.
- 편집: 기본 앱·GNB에 있는 앱은 이동 불가 표시, 차량 제어와 앱은 각자 영역 안에서만 이동. 국가 법규에 따라 Frost/Defrost가 GNB에 필수일 수 있다.
- 앱이 할 일: 앱 아이콘과 이름(짧게)만 준비한다.

**Widgets** [문서] — WIDGET
- 구성: Root, Handle(아래로 끌어 숨김), Pagination, Title/Action area, Information area. 속성·인터랙션은 이미지뿐.
- [문서에 없음] 서드파티 앱이 위젯을 제공할 수 있는지, 제공 방식(App Widget인지 별도 API인지). 확인 전까지 우리 앱은 위젯을 만들지 않는다.

---

## 6. 우리 앱에서 쓸 컴포넌트 우선순위

| 우선순위 | 컴포넌트 | 이유 | Compose 기반 |
| --- | --- | --- | --- |
| 1 | Basic button, Icon button, Interaction(95% 누름·모션 토큰) | 모든 화면의 기본 조작. 0.25초 응답 피드백 | `Surface(onClick)` + `graphicsLayer` |
| 1 | Page navigation | portrait·landscape 공통 상단 구조, Depth의 Back | `Row` 직접 구성 |
| 1 | Shadow Level 1 카드 | 정보 카드 구분 | `Modifier.dropShadow` |
| 2 | Tabs | 화면 안 2~4개 보기 전환 | `PrimaryTabRow` |
| 2 | Controllers(Stepper 위주), Switch, Toggle button | 설정값 조작. 주행 중에는 Stepper | `Slider`, `Switch`, `toggleable` |
| 2 | Toast, System popup | 결과 알림·확인 | 직접 `Box`, `BasicAlertDialog` |
| 2 | Indicator | 카드 여러 장을 넘길 때 | `HorizontalPager` + 점 |
| 3 | Text/Number field, Keypad | 운전 중 막힘. 주차 중 설정에만 | `TextField` |
| 3 | Checkbox, Radio, Dropdown, Spinner | 운전 중 부담. Tabs·Stepper로 대체 우선 | M3 기본 |
| - | Status bar, GNB, App library, Driving view, Gleo AI, System/Vehicle notification, Window bar | 시스템 UI. 겹치지 않게 배치만 | - |
| - | 3D, Widgets | 용량·제공 방식 불명 | 쓰지 않음 |

## 7. 확인 못 한 것

- 모든 목록 페이지(35개)를 읽었다. 읽기에 실패한 페이지는 없다.
- 이미지로만 있는 내용은 대부분 열지 않았다: 컴포넌트별 크기(L/M/S 등)의 dp 값, 상태별 색 견본, 팝업 Large/Medium 크기, Toast·Popup 배치 예시, 위젯 속성, Gleo 화면 크기별 배치, 디자인 토큰 구조도. → Figma "Pleos Connect design assets"에서 확인한다.
- 이미지를 연 것(4장): INTER의 easing 곡선·duration 표, THEME의 Types, ICONBTN의 Basic 스타일(치수 표기 없음 확인).
- 컴포넌트 ↔ 색 토큰 연결(Toggle = `button_switch_*`, Popup scrim = `dim_*`, Checkbox·Radio 색)은 문서에 명시가 없다. 위에 "추정"·"팀 판단"으로 표시했다.
- 서드파티 앱의 브랜드 테마 적용 방법, Gleo 음성 명령 연동, CCP 포커스 지원 요건, 위젯 제공 가능 여부, 표준 알림의 표시 모양.
- 원문 표기 오류로 보이는 곳: ICONBTN·TOGGLE·KEYPAD의 Pressed 설명(Bar 문구 재사용), KEYPAD의 Pressed 중복 행, PAGENAV Properties 설명, RADIO 구성 설명(Checkbox 문구). 해석은 위 요약처럼 했다.
