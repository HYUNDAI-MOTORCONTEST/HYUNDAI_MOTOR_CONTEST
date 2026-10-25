# Google·AAOS 차량 앱 가이드 요약 (보조 근거)

> **우선순위**: 이 문서의 내용이 Pleos 문서(`pleos-ui.md`, `pleos-dev.md`)와 다르면 **Pleos가 이긴다.**
> Google 문서는 Pleos 문서에 수치·규칙이 없을 때만 근거로 쓴다.
>
> 직접 쓴 요약이며 최종 기준은 원문이다. 확인일: 2026-10-25.
> 출처·라이선스: developer.android.com 콘텐츠는 Creative Commons Attribution 2.5(https://developer.android.com/license),
> source.android.com 콘텐츠는 Creative Commons Attribution 4.0(https://source.android.com/license), 코드 샘플은 Apache 2.0.
> 아래는 해당 원문을 요약·변형한 것이다. © Google, 출처 URL은 각 장에 적었다.

## 0. Pleos에 그대로 적용하면 안 되는 것

- Google "Android for Cars"의 운전 중 앱 대부분(내비, POI, IoT, 날씨, 템플릿 메시징 등)은 **Car App Library 템플릿**으로 만든다. Pleos Connect 앱은 템플릿 앱이 아니라 **일반 Activity(Compose) 앱**이다.
- 그래서 다음은 적용하지 않는다: 템플릿 종류·단계 수 제한(5단계, list 템플릿 규칙), 템플릿 전용 컴포넌트·액션 스트립 규칙, "AAOS에서는 주차 중 설정·로그인 외에 자체 Activity로 기능을 제공하지 말 것"(품질 기준 PE-1), Google Play 심사 절차.
- Google 분류로는 Views/Compose 앱이 "parked 카테고리(Video, Games, Browsers)"에 들어가 운전 중 화면이 아예 안 보여야 한다(DD-2). Pleos는 대신 **DO 선언 + CarUxRestrictions** 방식으로 주행 중 동작을 정한다(`pleos-dev.md` 2장). Pleos 규칙을 따른다.

## 1. Design for Driving 원칙
출처: https://developer.android.com/design/ui/cars/guides/foundations/design-principles

- 운전이 최우선. 한눈에 탐색할 수 있는 단순한 UI. 운전 중 쓸 수 없는 기능은 눈에 띄게 구분한다.
- 음성 제어를 넓게 지원한다. 마이크가 열려 있으면 그 상태가 보이게 한다.
- 작업을 쉽게 멈추고 이어 할 수 있어야 한다. 중단돼도 진행 상황을 잃지 않는다.
- 작업은 최대한 짧게: **자주 쓰는 작업은 3번 터치 이내**. 긴 A-Z 목록에는 바로가기를 둔다. 시스템이 운전 중 탐색을 잠시 제한할 수 있다.
- 한눈에 읽기: 운전 중 텍스트는 **1~3줄, 120자 이내**.
- 운전 상태에 맞춰 UI를 바꾼다. 알림은 위치·시점을 골라 띄우고, 운전자가 조작 중일 때는 억제한다.

## 2. UX 요구사항 (템플릿 앱 기준, 참고만)
출처: https://developer.android.com/design/ui/cars/guides/ux-requirements/overview

- 작업 흐름은 짧게(권장 2~3단계). 처음 열 때 필요한 권한을 요청한다. 음성 입력 전 마이크 권한을 받는다.
- 밝은·어두운 배경 모두에 맞게 강조색 2개를 두는 것을 권장한다.
- 로딩 화면처럼 다른 동작이 없는 곳에도 뒤로 가기 같은 빠져나갈 방법을 둔다. 빈 화면보다 쓸모 있는 내용을 먼저 보여 준다.
- 사용자 동작 없이 연속 자동 전환으로 작업을 끝내지 않는다. 자동 전환 전 내용은 8초 이상 보여 준다.

## 3. 차량 앱 품질 기준에서 쓸 만한 수치
출처: https://developer.android.com/docs/quality-guidelines/car-app-quality

| 기준 | 내용 | 적용 범위(원문) | 우리 사용 |
| --- | --- | --- | --- |
| UX-1 | 터치 대상 **64dp 이상** | parked(Video·Games·Browsers) | Pleos에 수치가 없어 최소값으로 채택 |
| UX-2 | 터치 대상 간격·화면 가장자리 여백 **24dp 이상** | parked | 채택 |
| UX-3 | 글자 **24sp 이상** 권장 | parked | 본문 최소값으로 채택 |
| SA-1 | 애니메이션 그래픽·영상 표시 금지(주차 중 운전 관련 캔버스 애니메이션은 예외) | 운전 중 카테고리 | Pleos 원칙과 같음 |
| ST-1 | 자동 스크롤 텍스트 금지 | 운전 중 카테고리 | Pleos 원칙과 같음 |
| AD-1 | 광고주·제품 이름 외 텍스트 광고 금지 | 운전 중 카테고리 | Pleos 알림 원칙과 같음 |
| IU-1 | 이미지 최소화(콘텐츠 배경 정지 이미지 1장, 운전 판단을 돕는 이미지 등만) | 운전 중 카테고리 | 운전 중 화면 설계 참고 |
| DR-1 | 앱 버튼 응답 2초 이내 | 운전 중 카테고리 | **Pleos 0.25초가 우선** |
| DR-2·3 | 앱 실행·콘텐츠 로딩 10초 이내 | 운전 중 카테고리 | 참고 |
| VD-1·TH-1 | 대비 요구 충족, 커스텀 테마는 라이트·다크 모두 지원 | 일부 카테고리 | Pleos 4.5:1·주야간 모드가 기준 |

## 4. AAOS 운전자 주의 분산 가이드라인
출처: https://source.android.com/docs/automotive/driver_distraction/guidelines
관련: https://source.android.com/docs/automotive/driver_distraction/car_uxr , https://source.android.com/docs/automotive/driver_distraction/consume

- 가이드라인을 지킨 Activity는 **Distraction Optimized(DO)** 로 표시해 차량이 움직이는 동안(UX 제한 상태)에도 실행될 수 있다.
- 제한 상태가 되면 DO로 표시된 Activity만 계속 실행되거나 새로 뜰 수 있다. 앱 전체가 DO가 되려면 모든 Activity가 가이드라인을 지켜야 한다.
- 플랫폼은 매니페스트 선언만 확인하고 실제 준수 여부는 검사하지 못한다. 그러니 선언한 화면은 앱이 스스로 제한을 지켜야 한다.
- 주차용·주행용 화면을 나눠 일부 Activity만 DO로 표시해도 된다. 선언 방법(Activity 단위 meta-data):

```xml
<activity android:name=".MainActivity" ...>
    <meta-data android:name="distractionOptimized" android:value="true" />
</activity>
```

## 5. Android for Cars 개요
출처: https://developer.android.com/training/cars

- Android Auto(휴대폰 투영)와 Android Automotive OS(차량 내장, 앱을 차에 직접 설치) 두 플랫폼이 있다. Pleos Connect는 AAOS 기반이다.
- Google은 차량용 앱을 카테고리로 나눈다: 미디어·메시징·통화·내비·POI·IoT·날씨(운전 중 가능, 대부분 템플릿)와 Video·Games·Browsers(주차 중 전용, Views/Compose). 0장대로 Pleos에는 이 분류를 그대로 적용하지 않는다.

## 6. 문서 조회
- Context7: AAOS 문서는 `/websites/source_android_automotive`, Android for Cars App Library는 `/websites/developer_android_training_cars_apps`(템플릿 앱용이라 참고만).
- developer.android.com 단일 페이지는 WebFetch로 원문을 읽는다.
