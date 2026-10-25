# docs/guides — 차량 앱 가이드 요약 색인

> **이 폴더의 요약은 참고용이며, 원문이 최종 기준이다.** 요약과 원문이 다르면 원문을 따르고 요약을 고친다.
> 모든 요약은 팀이 직접 쓴 것이다. Pleos 문서 원문은 복사하지 않는다(Pleos Playground 이용약관 제10조②, 제14조⑤⑪ — 회사 게시물의 무단 이용, 서비스 데이터 추출·저장·복제, 자동화된 대량 접속 금지).

## 문서 목록

| 파일 | 내용 | 주요 원문 |
| --- | --- | --- |
| [pleos-ui.md](pleos-ui.md) | Pleos Connect UI 디자인 가이드: 화면 구조, window·방향, 폼팩터, 색·글자·아이콘·버튼, Compose 실무 수치 | https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro |
| [pleos-dev.md](pleos-dev.md) | 기획·개발 가이드: 주행 규제(CarUxRestrictions), 주의 분산, 앱 구조·화면 모델, Vehicle SDK·권한, 에뮬레이터 주의점 | https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro , https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro |
| [google-cars.md](google-cars.md) | Google Design for Driving, Android for Cars, 차량 앱 품질 기준, AAOS 주의 분산 가이드라인(보조 근거) | https://developer.android.com/design/ui/cars , https://source.android.com/docs/automotive/driver_distraction/guidelines |

## 우선순위 (충돌하면 위가 이긴다)

1. Pleos Connect UI 디자인 가이드 — https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
2. Pleos 차량 앱 기획·개발 가이드, Pleos SDK 레퍼런스 — https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
3. Google Design for Driving / Android for Cars — https://developer.android.com/design/ui/cars , https://developer.android.com/training/cars
4. AAOS 운전자 주의 분산 가이드라인 — https://source.android.com/docs/automotive/driver_distraction/guidelines
5. 일반 Android / Jetpack Compose 문서 — https://developer.android.com/develop/ui/compose

Pleos Connect 앱은 Car App Library 템플릿 앱이 아니라 일반 Activity 앱이다. 구글 템플릿 제약은 그대로 적용하지 않는다(`google-cars.md` 0장).

## 그 밖의 원문 링크

- 공모전 개발 가이드: https://appdevchallenge.auto-hmg.io/guide/development
- 공모전 제출 안내: https://appdevchallenge.auto-hmg.io/guide/submission
- 에뮬레이터 설치: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- Virtual Console: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator
- 폼팩터 테스트: https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide
- Vehicle SDK: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro
- 권한: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission
- 이용약관: https://pleos.ai/playground/external/use-policy?partnerId=672505ad-2e37-4500-8162-000bcfc05b5c&locale=ko-KR

## MCP로 찾기 (Context7, `.mcp.json`)

| 대상 | 라이브러리 ID | 비고 |
| --- | --- | --- |
| Pleos 문서 | `/websites/document_pleos_ai_en` | 영어판만, 비공식·부분 색인(SDK 코드 위주. 디자인·기획 가이드는 빠져 있을 가능성이 높음) |
| AAOS (source.android.com) | `/websites/source_android_automotive` | 주의 분산, CarUxRestrictions, VHAL 등 |
| Car App Library | `/websites/developer_android_training_cars_apps` | 템플릿 앱용, 참고만 |

사용 예:
```
mcp__context7__query-docs  libraryId="/websites/document_pleos_ai_en"  query="Vehicle SDK EvBattery state of charge listener"
mcp__context7__query-docs  libraryId="/websites/source_android_automotive"  query="CarUxRestrictionsManager onUxRestrictionsChanged"
mcp__context7__resolve-library-id  libraryName="Jetpack Compose"  query="BoxWithConstraints adaptive layout"
```
결과에 나오는 `Source:` URL은 원문에서 다시 확인한다.

## 요약을 고칠 때

- 원문 URL과 확인 날짜를 같이 고친다.
- 원문에 수치가 없으면 "[문서에 없음]"으로 표시하고 팀 권장값과 그 근거를 따로 적는다.
- 필요한 페이지만 읽는다. sitemap 순회·대량 다운로드 금지.
