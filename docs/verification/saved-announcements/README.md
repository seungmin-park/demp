# 관심 공고와 수시 모집 마감 방식 — 2026-10-15

## 전달 상태

양쪽 `refactor/saved-announcements`, backend 기준 73ee647/frontend 기준 3cd16e2. 구현은 `.worktrees/saved-announcements/{backend,frontend}`에 미커밋 상태다. 사용자가 요청한 plan.md/tasks.md는 원본과 backend worktree에 함께 반영한다. commit·PR·merge·배포·운영 SQL은 실행하지 않았다.

## 사용자 결과와 책임

목록·상세의 저장/해제 → 로그인·가입 후 검색/선택 복귀 → `/saved` 내 목록 → 상세 재진입·해제. 회원별 서버 저장과 저장순 페이지를 제공한다. 모집이 끝나도 유지하고 비공개·삭제 항목은 내용 없이 이용 불가로 남겨 해제할 수 있다.

```text
BookmarkButton / SavedAnnouncements
   → useAnnouncementBookmarks / useSavedAnnouncements (상태·복구)
   → api/bookmarks (HTTP)
   → AnnouncementBookmarkController (principal·입출력)
   → AnnouncementBookmarkService (잠금·트랜잭션·공개 경계)
   → AnnouncementBookmark / Repository (회원별 UNIQUE·저장)
```

회원 행 잠금은 동일 회원의 중복·동시 저장을 직렬화한다. 원하는 최종 상태를 PUT으로 보내 재전송해도 중복 항목이 생기지 않는다. 공고 FK를 두지 않아 삭제 후 항목을 보존하며, 조회에서 현재 공개 공고만 일괄 조립한다. 회원 FK는 회원 삭제 시 cascade한다. 언어는 일괄 fetch하여 저장 목록에 카드별 추가 조회를 만들지 않는다. 상태 조회는 요청당 최대 100개, 목록은 페이지당 최대 20개다.

RecruitPeriod/DeadlineMode가 FIXED, UNTIL_FILLED, ALWAYS_OPEN, UNKNOWN을 소유한다. FIXED는 정확한 마감 일시가 있어야 공개 가능하다. 채용 시 마감·상시는 가상 날짜 없이 명시적 원문 근거로 선택하며, 시작 전·수동 종료 판정이 우선한다. UNKNOWN은 상시로 추정하지 않고 초안에 보존한다. 기존 NULL 방식은 마감이 있으면 FIXED, 없으면 UNKNOWN으로 읽는다. 최대 기한과 조기 마감 조건이 함께 있으면 정확한 최대 기한을 FIXED로 저장하고 조기 마감 조건은 본문에 보존하며 실제 조기 종료는 수동 마감한다.

## Red → Green → Refactor

- backend `./gradlew test --tests '*BookmarkHttpFlowTest'`: fixture에 빠진 빈 언어 집합을 먼저 수정했다(기능 Red 아님). 이후 실제 HTTP 6개가 API 없음 404/expected 200으로 실패했다. 회원별 저장·일괄 상태·목록 API와 DB 제약을 최소 구현해 통과했다.
- `RecruitmentDeadlineServiceTest`: 4개 중 3 assertion 실패. 날짜 없는 채용 시/상시 공개가 validation에서 거절되고 모순된 채용 시+날짜는 허용됐다. 마감 방식·공개 불변식·모집 중 조회를 함께 바꿨다. 미확인 공개 거절/초안 보존은 기존 보장으로 처음부터 통과했으며 Red로 부르지 않는다.
- frontend `SavedAnnouncementsFeature`: 저장 버튼 부재와 인증/가입 복귀 계약 실패. 가입 테스트의 username watcher·RouterLink public prop fixture 문제는 기능 실패와 구분해 수정했다. `/saved` 경로 테스트는 기존 NotFound 화면을 관찰한 실제 assertion 실패에서 시작했다.
- 상태 소유자 테스트: 첫 페이지 밖의 복귀 대상 미저장, 해제 후 재조회 중 로딩 누락 2개를 실패시켰다. 저장 중 시작된 조회가 완료 뒤 상태를 되돌리는 별도 assertion도 실패시켰다. 대상 포함 일괄 조회, 페이지 재조회 직렬화, 저장 전후 revision 비교로 수정했다. 계정 전환·unmount·저장 전 조회의 늦은 응답 보호는 이미 통과한 보장으로 기록한다.
- 리뷰: 저장 PUT 401에서 선택한 공고가 복귀 주소에 빠지고 성공 전 URL 의도를 지우는 Important 1건. interceptor 전달/실패 후 의도 보존 2개 테스트 실제 실패 후 수정했다. 재로그인 URL에 공고 ID를 보존하고 성공 후만 의도를 소비한다. 읽기 전용 재리뷰에서 추가 Critical/Important 없음. 리뷰 자체는 실행 증거가 아니다.
- 로컬 예제 ID -1: backend 실제 HTTP 저장 400/expected 200, frontend 복귀/401 전달 2개 assertion 실패 후 signed nonzero ID를 허용했다. 존재하지 않는 ID는 404, 0은 400을 유지한다.
- 책임 정리: 저장 HTTP·비동기 상태·Vue 표현을 분리하고 마감 enum/기간 객체가 규칙을 소유한다. `setSaved`, `savedIds`, `list`, `hasConfirmedDeadline`, `formatDeadline`, `safeReturnPath`를 효과에 맞춰 이름 지었다. 공개 공고 조회는 익명 계약을 유지하고 회원별 상태는 별도 인증 API다. Vue는 props/event·상태/API 경계로 평가했고 독립 TS 모듈은 상태 소유·작은 계약을 기준으로 검토했다. 모든 기존 호출부·타입·직렬화·URL·REST Docs를 전체 검증에 포함했다.

## 최종 실행 결과

- Java 25 `bash scripts/verify.sh`: 종료 0. **458 tests**, failures/errors/skipped 0, 필수 suite 68, 구조 검사와 위반 probe, REST Docs·JAR·제공 문서 일치, 실제 Spring/H2 공용 HTTP 계약 통과.
- `npm run verify`: 종료 0, **363 unit assertions**, 개발/배포 **236 headless browser flows**, typecheck/lint/build·구조 검사·위반 probe 통과. [최종 요약](frontend-summary.json).
- 실제 Vue→Spring/H2 headless 경로: [결과](live-results.json), [실행 스크립트](live-browser.mjs), [격리 fixture](local-fixture.sql), [390px 저장 화면](assets/saved-mobile.png). 네트워크 mock 없이 비로그인 저장·실제 로그인·검색 복귀·새로고침·별도 HTTP 저장 재조회·저장순·수시 모집·수동 종료·비공개 정보 차단·해제, 모바일 가로 넘침 없음·Vue 경고 없음의 5개 검증 묶음 통과. 5개는 전체 요구사항 개수라는 뜻이 아니다.

## 실행 중 실패와 범위

초기 cache 쓰기는 sandbox가 차단해 승인 후 재실행했다. frontend 기준선 341개 중 VerificationSource 1개는 5초 시간 초과했고 단독 18개 재실행과 이후 전체 검증에서 통과했다. backend 문서 테스트의 `status` 메서드가 matcher를 가린 컴파일 오류, TS exactOptionalPropertyTypes와 lint 제어문자 오류는 기능 Red로 기록하지 않았다.

처음에는 사용자 지침에 따라 cmux caller workspace:1000090001, helper pane:1000090013, surfaces:1000090018/19에서 headed 검증을 실행했다. 이후 사용자가 **headless**를 명시하여 최종 전체·실제 서버 검증은 headless로 전환했다. 이를 실제 cmux 브라우저 클릭 검증으로 표현하지 않는다. 중간 headed는 232/236 통과였고 복귀 URL의 한국어 인코딩 비교를 정정했다. 다음 headless 235/236에서는 가입 화면 전환 전에 이전 로그인 폼에 입력하는 테스트 대기 문제를 발견하여 새 heading 확인·중복 확인 완료 대기로 수정했다. assertion을 삭제하거나 재시도를 허용하지 않았고 최종 236/236이다.

실제 서버 최초 로그인은 검증 포트와 CORS 허용 Origin 불일치로 거절됐다. 검증 전용 서버의 허용 Origin을 15150으로 맞추고 새 in-memory DB로 다시 실행했다. 기존 운영 설정은 변경하지 않았다.

운영 DB와 S3·외부 지원 사이트·알림 발송은 검증하지 않았다. 새 수동 SQL은 H2 MySQL 모드 기존 스키마 validate에서 검증했으며 실제 운영 MySQL 적용 증거는 아니다. 배포 전 별도 백업/복제 DB에서 `announcement-deadline-mode.sql`과 `announcement-bookmarks.sql` 검증이 필요하다. exact API 응답 필드와 필수 테스트 목록에 신규 계약을 반영했다.

원시 로그: `/private/tmp/demp-saved-verify-backend-final.log`, frontend `.verification/{summary,unit,e2e}.json` 및 단계별 `.log`, Red `/private/tmp/demp-saved-{backend-red,frontend-red,races-red,during-save-red,expiry-red,local-namespace-red}.log`, `/private/tmp/demp-deadline-{red,frontend-red}.log`. 테스트 종료 0은 실패한 이전 실행을 지우지 않는다.


## 후속 전달 승인

사용자가 기능 단위 PR·CI·자동 머지를 요청하여 `refactor/member-saved-announcements`에서 전달한다. 앞 절의 미커밋/운영 미적용은 구현 검증 완료 시점의 기록이다. 후속 스키마 백업/복원 및 PR/머지/main CI 결과는 별도 delivery 기록으로 구분한다.
