# Pleos Connect UI 요약 (차량 화면 설계·Compose 구현용)

> 직접 쓴 요약이다. 원문을 옮기지 않았고, 최종 기준은 각 항목의 원문 URL이다.
> 확인일: 2026-10-25 (한국어 페이지 기준)
> 표기: **[문서]** 원문에 있는 내용, **[문서에 없음]** 원문에 수치·규칙이 없어 다른 근거로 정한 팀 권장값(근거를 같이 적음).
> 그 밖의 컴포넌트(Icon·Toggle 버튼, Tabs, Slider, Toast, Popup 등)와 그림자·모션·앱 아이콘 요약은 [pleos-components.md](pleos-components.md).

원문 약칭 (모두 `https://document.pleos.ai` 아래)

| 약칭 | 페이지 |
| --- | --- |
| INTRO | `/docs/connect/guide/docs-design/Introduction/intro` (UI Design Guide 첫 페이지) |
| LAYOUT | `/docs/connect/guide/docs-design/getting-started/layout` |
| WINDOW | `/docs/connect/guide/docs-design/getting-started/window` |
| COLOR | `/docs/connect/guide/docs-design/foundations/colors` |
| TYPO | `/docs/connect/guide/docs-design/foundations/typography` |
| ICON | `/docs/connect/guide/docs-design/foundations/contents-creation/iconography` |
| BUTTON | `/docs/connect/guide/docs-design/components/actions/basic-button` |
| FF | `/docs/pleos-only/app-test-guide/form-factor-test-guide` |

디자인 원본 파일(Figma): INTRO 하단의 "Pleos Connect display guide", "Pleos Connect design assets" 링크. 색·글자 크기의 실제 값은 이 파일과 원문 이미지에만 있다.

---

## 1. 기본 원칙 [문서] — INTRO

- **쉽고 안전하게**: 운전 중 시각·인지 부담을 줄인다. 시선을 끄는 요소(동영상, 움직이는 이미지, 텍스트 자동 스크롤)를 운전 중에 쓰지 않는다. 한 손, 단순한 조작으로 끝나야 한다. 양손 조작이나 손발을 크게 움직이게 하는 동작을 요구하지 않는다.
- **일관성**: 아이콘·용어·상호작용 패턴을 통일하고, 기본 탑재 UI와 어긋나지 않게 Pleos 디자인 가이드를 따른다.
- **한눈에 보기**: 운전자는 1~2초 안에 정보를 파악해야 한다. 핵심 정보만 남긴다.
- **응답 속도**: 입력 후 시스템 응답은 **0.25초 이내**. 더 걸리면 처리 중임을 화면에 표시한다.
- **알림**: 운전자에게 필요한 경우에만, 짧게. 알림으로 광고하지 않는다(광고주·제품 이름 외 텍스트 광고 금지).
- **주간/야간 모드**: 조명 환경에 맞게 밝기·대비가 바뀌어도 읽기 쉬워야 한다.
- **음성**: Gleo AI 음성 상호작용을 활용할 수 있는지 검토한다. 음성 대화의 시작·종료·중단은 수동으로도 가능해야 한다.
- **확장성**: 다국어, 여러 차종의 해상도·화면비에 대응한다. OTA 업데이트를 고려한다.

## 2. 화면 구조 [문서] — LAYOUT

| 영역 | 내용 | 앱이 할 일 |
| --- | --- | --- |
| Status bar | 주행 상태, Gleo AI·검색, 시계, 네트워크·마이크 상태, 알림 센터, 프로필 | 가리지 않는다 |
| Driving view | 텔테일, 속도, 차량 상태 등 주행·안전 정보 | 앱이 가리거나 종료할 수 없다 |
| GNB | App library, 즐겨찾는 앱, 공조 컨트롤 | - |
| Application window | 앱 1개 또는 2개 표시 | 앱은 여기에만 그린다 |

- Status bar, GNB의 패널(App library·공조), 키보드 같은 System UI는 앱보다 위 레이어라 앱 화면을 잠시 덮을 수 있다. 중요한 내용을 화면 가장자리에만 두지 않는다.

## 3. Window 모델과 방향 [문서] — WINDOW, LAYOUT

- **portrait** = 앱이 window 1개를 쓴다(앱 2개를 나란히 띄우면 각각 portrait). **landscape** = 앱 하나가 window 2개 폭을 쓴다.
- 가로 전용 게임처럼 한 방향만 되는 앱이 아니면 **portrait와 landscape 레이아웃을 모두** 설계해야 한다.
- Connect-S: 앱 2개면 1:1, 앱 1개면 2:0(기본 1:1). Connect-W: 2:1 또는 1:2(기본 2:1).
- 새 앱은 기존 앱의 방향을 따라 뜬다(landscape 앱 위에서는 landscape, portrait 2개 상태에서는 primary window에 portrait). Connect-W에서는 새 앱이 항상 landscape로 primary window에 뜬다.
- 사용자는 3손가락 스와이프나 window bar로 위치·크기(landscape↔portrait)를 바꾸고 앱을 닫는다. 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남을 수 있다.
- 앱 간 딥링크(App A → App B)는 B가 A 위를 덮고, B가 끝나면 A로 돌아온다.
- 기본 탑재 앱은 landscape·portrait 둘 다 지원한다. 우리 앱도 같은 수준을 목표로 한다.

## 4. 폼팩터 [문서] — LAYOUT, FF

- **Connect-S**(Standard, OS 기본): 17인치, 14.6인치, 12.9인치(예전 이름 Connect-L).
- **Connect-W**(Wide): S보다 가로가 길고 주로 고급 차종. 세로가 늘어나는 확장형이 **Connect-W-Extended**.
- **Connect-WF**(Waterfall): Wide보다 작은 해상도, 핵심 기능만.
- 폼팩터별 해상도(px/dp) 표는 [문서에 없음]. FormFactorConsole(FF 문서)로 가상 디스플레이를 만들면 dp·DPI·px가 표시되니 거기서 확인한다.

## 5. 색 — COLOR, INTRO

- [문서] 중요한 정보는 강한 대비로, 색은 절제해서 쓴다. 아이콘·텍스트·이미지의 **대비율 4.5:1 이상**.
- [문서] 토큰 체계: Basic Color(Grayscale, White/Gray Alpha, 테마와 무관한 Static Light/Dark)와 System Color(Background, Button, Controller, Dropdown, Dim, Driving, Fields, Icon, Informative, Overlay, Regulation, Surface, Switch, Text 등 용도별).
- [문서] 토큰의 hex 값은 웹 본문 텍스트에는 없고 COLOR 페이지의 표 이미지에 있다. 이름 규칙은 `ivi` 접두 + snake_case(`/docs/connect/guide/docs-design/Introduction/design-token`).
- 구현(2026-10-25): COLOR 페이지 표 이미지 25장 전부를 읽어 `frontend/app/.../ui/theme/PleosColors.kt`에 토큰 149개로 옮겼다. 섹션별 주석에 표 이름을 적었다. 이미지·원문은 레포에 두지 않는다.
  - 섹션(토큰 수): Grayscale 11, White Alpha 6, Gray Alpha 6, Static Light 4, Static Dark 4, Background 3, Button 9, Controller 5, Climate 13, Dropdown 2, Dim 6, Driving 5, Fields 2, Gleo AI 6, Icon 8, Informative 4, Keyboard Normal 9, Keyboard Static 13, Media 4, Overlay 1, Phone 4, Regulation 6, Surface 4, Switch 6, Text 8.
  - 이름: 문서 토큰명 그대로. Climate 표는 이름에 섹션 접두가 없어(`button_enabled` 등) `climate_`를, Driving 표의 `gradient_start/end`에는 `driving_`을 붙였다.
  - System 표는 Basic 토큰을 참조하는 구조다(예: `background_primary` = `basic_50`). 코드도 참조로 적었다. 알파는 ARGB.
- 판독 원칙: 표에 적힌 토큰 참조·hex 글자를 먼저 따른다. 견본 색은 PNG 픽셀을 직접 샘플링해 대조했다. 견본과 글자가 다른 곳에는 코드에 `// 불확실:` 주석을 남겼다.
- Background 표 판정: Grayscale과 값이 충돌하는 문제가 아니다. 견본 hex는 Grayscale 값과 정확히 같다. 견본이 행 하나씩 밀려 그려진 것이다(견본 primary 행 = 글자 popup 값, secondary 행 = 글자 primary 값, popup 행 = 글자 secondary 값).
  - 그래서 글자를 채택했다: `background_primary` = basic_50, `background_secondary` = basic_00, `background_popup` = basic_00(주간)/basic_100(야간).
  - 글자 쪽이 야간에 팝업이 배경보다 밝아지는 위계와도 맞는다.
- Alpha 표(예외): 표 글자는 Light/Dark 열이 바뀐 것으로 판단했다. 그래서 `basic_alpha_light_*` = 주간 #FFFFFF n% / 야간 #131417 n%, `basic_alpha_dark_*` = 주간 #131417 n% / 야간 #FFFFFF n%로 옮겼다.
  - 근거 1: Alpha 표 견본 12쌍이 모두 반대 값과 일치한다.
  - 근거 2: 다른 표의 alpha 참조 견본도 다수가 일치한다(46 대 10).
  - 근거 3: Dropdown 표의 어두운 배경 견본이 같은 결론을 보인다.
  - 근거 4: 표 글자대로면 button_basic·overlay가 보이지 않는다.
  - 불확실하므로 Figma design assets로 다시 확인한다.
- 그 밖의 불확실 항목(표 글자를 따랐고 견본이 다른 곳):
  - Text·Icon 표 아래 4행: 이름이 `*_dark_*`로 중복 표기돼 있어 `text_light_*`/`icon_light_*`로 보고 이름을 붙였다.
  - Driving `charging_deep` 야간: 글자 #0082FC, 견본 #00B975.
  - Phone 4행 야간: 견본이 주간 색이다.
  - Surface `surface_basic` 야간: 견본이 basic_00이다.
  - Climate `button_selected` 주간: 견본이 basic_200이다.
  - Keyboard 일부: secondary_pressed 야간, static_background·icon_light·icon_dark·text의 견본이 다르다.
  - Button `button_switch_pressed` 야간.
  - Media의 8자리 hex는 표기가 섞여 있다. primary는 RRGGBBAA, gradient는 AARRGGBB로 읽어야 견본과 맞는다.
  - Driving 토큰명 `gearshitft_background`은 문서 표기 그대로 두었다.
- M3 매핑(`ui/theme/Theme.kt`):
  - 문서 토큰으로 잇는 것: background = `background_primary`, surface = `surface_basic`, surfaceVariant·surfaceContainer(카드) = `surface_high`, onBackground·onSurface = `text_dark_primary`, onSurfaceVariant = `text_dark_secondary`, primary = `button_filled_enabled`, error = `informative_negative`.
  - 팀 판단: 문서에 대응 역할이 없는 반전 배너(basic_700/basic_00), onPrimary, outline, onError.
  - filled 버튼 disabled: 바탕은 문서 토큰 `button_filled_disabled`, 라벨은 팀 판단 `text_dark_tertiary`(카드 위 대비 주간 4.98, 야간 5.60).
  - 역할별 대비는 `PleosColors.kt`의 `PleosRoles` 표에 있다. 모든 글자 역할이 4.5:1 이상이다.
- [팀 계산] Informative 색은 카드(surface_high) 위에서 4.5:1에 못 미치는 경우가 있다(주간 active 2.03, positive 4.25, negative 3.07 / 야간 positive 3.44, negative 3.85). 글자색으로 쓰지 않고, 글자가 같은 뜻을 전하는 보조 표시에만 쓴다. 역할별 대비표는 `PleosColors.kt`의 `PleosRoles` 주석.

## 6. 글자 — TYPO, INTRO

- [문서] 중립적이고 시인성이 고른 서체. 시스템 UI 폰트는 **Asta Sans**(한국어·영어, Google Fonts). 브랜드 폰트도 개성이 강한 것은 피한다.
- [문서] 위계: Headline / Title / Body / Label. 위계별 크기는 일관되게, 보조 텍스트는 위계를 구분한다.
- [문서] 위계별 크기(표 이미지): Headline 60/56/48, Title 40/36/32, Body 30/28/26, Label 24/20(Large/Medium/Small 순). **단위 표기가 없다.** 굵기는 Headline ExtraBold·Bold, Title ExtraBold·Bold·Regular, Body Strong·Normal, Label Normal.
- [문서] 글자 높이: **주요 정보(속도, 길 안내, 경고 등) 5 mm 이상, 보조 텍스트 3 mm 이상**.
- [문서] 최소 글자 크기는 12sp 이상(Android 접근성 기준을 인용), 실제 픽셀 높이 18px 이상, 줄 간격은 대략 1.5배 이상.
- [문서에 없음] 단위, line-height·letter-spacing 수치, Body "Strong"의 실제 굵기, mm → sp 환산식.
- 구현(2026-10-25, `ui/theme/Type.kt`): 위 크기를 **sp**로 두고(팀 판단), 줄 높이는 "1.5배 이상" 규칙대로 크기 × 1.5, Strong은 SemiBold(600). 글꼴은 Asta Sans 가변 폰트(google/fonts 배포본 v1.000, wght 300~800)를 `res/font/asta_sans_variable.ttf`로 내장하고 `FontVariation.weight`로 400/500/600/700/800을 만든다(API 26+, minSdk 28 문제없음). 라이선스 전문 `licenses/AstaSans-OFL.txt`.
- [팀 관찰] Pleos 에뮬레이터 v2.1.0에는 시스템 글꼴로 Asta Sans(OTF)가 이미 들어 있다. 그래도 실제 차량·다른 이미지에서 같은 글꼴을 보장하려고 앱에 내장한다. 그래서 캡처만으로는 내장 글꼴 적용 여부를 구분하기 어렵다(굵기 축 값을 바꿔 그려지는지로 확인했다).

## 7. 텍스트 양 [문서] — INTRO

- 영어 메시지 1개: 120자 또는 24단어 이하. 일본어: 30자 이하(영문 기준).
- 한 줄 80자(영문 기준) 이하, 문장·단락은 **3줄 이하**.
- 긴 글 대신 단순한 아이콘 + 짧은 문장.
- [문서에 없음] 한국어 글자 수 기준. → 팀 권장: 한 메시지 40자 안팎, 3줄 이하(영어 120자 기준을 한글 폭에 맞춰 줄인 값이며 공식 수치 아님).

## 8. 아이콘·피드백 [문서] — ICON, INTRO

- 기본 그리드 **48 × 48dp**. Filled / Outlined 두 스타일. 한 벌을 만들고 크기만 조절한다.
- 국제적으로 통하는 아이콘을 쓰고 **텍스트 라벨을 함께** 둔다.
- 터치 시 햅틱이나 음성 피드백으로 조작을 확인시킨다.

## 9. 버튼 [문서] — BUTTON

- 구성: Root(컨테이너), Label, 앞·뒤 아이콘(선택).
- 상태: Pressed, Disabled(조작 불가), Loading(로더 표시 + 조작 불가).
- [문서에 없음] 버튼·터치 영역 최소 크기와 간격. 기획 가이드도 "UI 디자인 가이드 기준을 따르라"고만 하고 수치는 없다.

---

## 10. Compose 구현 실무 규칙

| 항목 | Pleos 문서 | 우리 규칙 | 근거 |
| --- | --- | --- | --- |
| 터치 영역 | 수치 없음("큰 터치 영역") | **최소 64dp × 64dp**, 터치 대상 간격·화면 가장자리 여백 **24dp 이상**. 주요 버튼은 72dp 이상 | [문서에 없음] Google 차량 앱 품질 기준 UX-1·UX-2(64dp, 24dp). 원래 parked 카테고리 기준이지만 공개된 차량용 수치 중 가장 구체적이다 |
| 본문 글자 | 12sp 이상, 18px 이상. 위계 Body 26~30, Label 20~24(단위 없음) | **본문·버튼 24sp 이상**: Body(26~30sp)·Label Medium(24sp) 사용. Label Small(20sp)은 보조 표기에만 | Pleos 위계 크기 + Google 품질 기준 UX-3(24sp). `PleosTypography` 토큰만 쓰고 `fontSize` 하드코딩 금지 |
| 주요 정보 글자 | 높이 5 mm 이상 | **32sp 이상**(속도·배터리·경고 같은 핵심 수치는 40sp 이상) | [문서에 없음] 추정: dp가 물리 크기를 따른다고 보면 1dp ≈ 0.16 mm라 5 mm ≈ 31dp. 실제 화면 density는 FormFactorConsole의 dp·DPI로 다시 계산한다 |
| 보조 글자 | 높이 3 mm 이상 | **24sp 이상** | 위와 같은 환산(3 mm ≈ 19dp)보다 Google 24sp 기준이 커서 큰 쪽을 쓴다 |
| 대비 | 4.5:1 이상 | 텍스트·아이콘 모두 4.5:1 이상, 주간·야간 두 테마 모두 계산 | [문서] INTRO |
| 주간/야간 | 자동 조정 | `VehicleTheme`이 `isSystemInDarkTheme()`로 `PleosColors.Light/Dark`를 고른다. `configChanges`에 uiMode가 있어도 재생성 없이 바로 바뀐다(에뮬레이터 `cmd uimode night yes/no`로 확인, 2026-10-25) | [문서에 없음] 주야간 신호 전달 방식은 Pleos 문서에 없다. Android 표준 `uiMode` night 기준으로 가정 |
| 방향 | portrait·landscape 둘 다 | `screenOrientation` 지정 금지. `BoxWithConstraints`나 window 크기로 1단(portrait)·2단(landscape) 배치. portrait = 핵심 기능, landscape = 부가 정보 추가 | [문서] LAYOUT, 기획 가이드 화면 모델 |
| 텍스트 길이 | 3줄, 80자/줄, 120자 | `maxLines = 3`(제목은 1~2), `overflow = TextOverflow.Ellipsis`. 문자열은 `strings.xml`(다국어 대비) | [문서] INTRO |
| 응답 | 0.25초 | 클릭 즉시 pressed·loading 상태 표시. IPC·네트워크는 코루틴으로, 메인 스레드 차단 금지 | [문서] INTRO, BUTTON(Loading 상태) |
| 운전 중 움직임 | 동영상·애니메이션·자동 스크롤 금지 | `CarUxRestrictions`가 활성일 때 애니메이션·자동 스크롤·영상 끔. 주차 중에도 장식용 반복 애니메이션은 쓰지 않음 | [문서] INTRO, `pleos-dev.md` 2장 |
| 아이콘 | 48dp 그리드, 라벨 병기 | `Icon` 48dp(최소 36dp), 옆이나 아래에 텍스트 라벨 | [문서] ICON |
| 피드백 | 햅틱·음성 | 주요 버튼에 `LocalHapticFeedback` 적용 검토 | [문서] INTRO |
| 화면 깊이 | 수치 없음 | 자주 쓰는 기능은 첫 화면에, 작업은 3번 터치 이내 | [문서에 없음] Google Design for Driving 원칙(3번 이하). `google-cars.md` |

### 검증 체크
- portrait(window 1개)·landscape(window 2개) 두 상태 모두 `adb exec-out screencap -p`로 캡처해 잘림·겹침·글자 크기를 본다.
- 가능하면 FormFactorConsole로 12.9인치 portrait, 14.6인치 landscape 등 다른 폼팩터도 확인한다(FF 문서: Go 1.22+, 8095·8096 포트, Host Display Bridge 필요).
