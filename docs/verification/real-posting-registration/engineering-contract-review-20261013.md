# agent-engineering · api-testing 재점검

사용자 지적 이후 앞선 완료 보고와 실제 저장소 상태를 대조했다. 기능 구현과 실제 cmux 검증이 없었던 것은 아니다. 그러나 새 모집 경로의 실제 HTTP 검증은 임시 스크립트에 머물렀고, 공용 CI 검증·프로젝트 verify 스킬·필수 suite 보호로 이어지지 않았다. 전체 테스트 개수만으로 이 공백을 설명하지 않은 완료 보고를 바로잡는다.

## 요구사항 → 소유자 → 실패 검사

| 사용자 보장 | 상태·규칙 소유자 | 지속적으로 실행되는 검사 |
| --- | --- | --- |
| 미확인 초안을 저장하고 공개 전 완성도를 확인 | Announcement 공개 불변식, DTO 입력 validation, Service 트랜잭션 | AnnouncementDraftRecoveryTest + 실제 HTTP의 null 보존·400 후 원본 재조회 |
| 대체 URL 중복이 생기지 않음 | AnnouncementSourceKey, Repository 조회, DB UNIQUE | 동시 요청 suite + 실제 HTTP 409 |
| 월급·직무별 조건·출처가 손실되지 않음 | Description, RecruitmentRole, Announcement 참조 목록 | 실제 multipart 생성/수정·상세 전체 필드·중첩 role 계약 |
| 모집 상태가 등록된 시간에 따라 바뀜 | 서버 Clock 조회 판정, 프런트 시간 표시·목록 갱신 | 기존 Clock 경계·프런트 시간/오류/경합 테스트, 앞선 실제 벽시계 cmux 전환 |
| 재검증 방법과 핵심 테스트가 사라지지 않음 | 프로젝트 verify 스킬, 공용 verify, required-suite manifest | 신규 suite 보고서 제거 시 실패, 실제 응답을 변형한 계약 위반 거절 |

```text
bash scripts/verify.sh
  → 전체 JUnit·필수 XML·REST Docs·새 JAR
  → verify_runtime: 독립 H2·임의 포트·임시 JWT 키·실제 Spring
  → verify_announcement_contract: HTTP 로그인→CRUD→오류·권한→재조회
  → 응답 계약·헤더·최종 상태 assertion
  → 공고 cleanup → 서버 종료·메모리 DB 폐기
```

## 보완한 검증 범위

기존 Python 표준 라이브러리·JUnit/MockMvc/REST Docs를 유지했다. 별도 Playwright API 프로젝트나 새 스키마 의존성을 추가하지 않았다. `.agents/qa-project-context.md`는 없었으며 REST·JWT·응답 계약은 Controller/DTO/SecurityConfiguration/기존 테스트에서 확인했다.

[`verify_announcement_contract.py`](../../../scripts/verify_announcement_contract.py)를 공용 [`verify_runtime.py`](../../../scripts/verify_runtime.py)에 연결했다. 실제 HTTP 성공·오류와 모든 상세/목록 항목의 계약 키·타입, 중첩 직무 필드·실제 값, 페이지 메타데이터를 검증한다. nullable과 빈 배열을 구분하고 `salaryPeriod` 누락, `roles=null`, 직무의 `requiredSkills` 누락을 동일 validator가 거절하는지 검사한다. 이 세 변형은 검사기 검증이며 실제 서버가 잘못 응답했다고 보고하지 않는다.

| HTTP 대상 | 성공 경로 | 오류 경로·상태 보존 |
| --- | --- | --- |
| POST /api/member/login | 실제 로그인·응답 구조 | 잘못된 비밀번호 401 |
| GET /api/admin/announcements | 페이지·검색·빈 다음 페이지 | 미인증/만료 401, 일반 회원 403, 음수 페이지 400 |
| POST /api/admin/announcements | 201·불완전 초안 저장 | 400·401·403·원문 별칭 409 |
| GET /api/admin/announcements/{id} | 전체 응답·독립 재조회 | 401·403·삭제 후 404 |
| PATCH /api/admin/announcements/{id} | 공개·중첩 정보·반복 수정·수동 종료/해제 | 잘못된 공개/출처/직무 400, 401·403·404. 거절 뒤 전체 응답 동일 |
| DELETE /api/admin/announcements/{id} | 삭제·목록 제외 | 401·403, 반복 삭제 404이며 계속 부재 |
| GET /api/announce | 공개 목록·세부 직무 검색·월급의 연봉 검색 제외 | 잘못된 상태 enum 400 |
| GET /api/announce/detail/{id} | 회원의 공개 상세·조회수 없는 순수 재조회 | 비공개/삭제 대상 404의 기존 빈 본문 계약 |
| POST /api/announce/add | 기존 `text/plain`의 `ok` 계약·초안 | 일반 회원 403·잘못된 입력 400 |

JSON은 `application/json`, legacy 생성은 `text/plain`, 공개 상세 404는 빈 본문이라는 기존 계약을 따른다. 모든 요청에서 `Cache-Control`의 `no-store`와 `X-Content-Type-Options: nosniff`를 조건 없이 확인한다. 오류 JSON은 코드·메시지·instance를 전체 비교한다. 로그인으로 받은 JWT를 사용하며, 만료 검증만 해당 독립 서버의 임시 키로 올바르게 서명한 과거 exp를 사용한다. 토큰은 로그에 남기지 않는다.

일반 회원은 실제 가입·로그인으로 만든다. 관리자 fixture 권한은 verifier가 소유한 독립 H2 초기 SQL에만 추가한다. 애플리케이션의 기본 local 계정 권한이나 운영 권한은 바꾸지 않는다. 공고는 finally에서 API로 지우고, 계정과 기존 공용 flow 데이터는 verifier의 finally에서 프로세스·메모리 DB를 폐기한다. 외부 파일 저장소를 사용하는 이미지 요청은 이 텍스트 기반 계약 흐름에 포함하지 않는다.

요청 시간·응답 크기는 측정값만 남긴다. 공고 API의 합의된 성능 예산이 없으므로 임의의 500ms assertion을 추가하지 않았다. refresh API는 존재한다고 가정하지 않는다. 기존 인증/로그인 제한 suite가 더 넓은 토큰·429/Retry-After 계약을 검증한다. MVC slice의 mock Service 증거를 DB 통합 증거로 합산하지 않는다.

## 프로젝트 경로 연결

- 백엔드 [feature map](../../engineering/feature-map.md)과 [verify 스킬](../../../.agents/skills/verify-demp/SKILL.md)에 상태 소유자, 실제 HTTP 명령, 실패 조건, 실제 cmux 경로를 연결했다.
- 프런트 프로젝트 verify 스킬에도 새 입력·시간 전환·WebKit 크기 확인을 추가했다. 이전의 읽기만 하는 스킬 적용에서 검증 경로 유지까지 보완했다.
- `AnnouncementDraftRecoveryTest`를 [필수 suite 목록](../../engineering/required-test-suites.json)에 넣었다. 기존 61개를 제거하거나 대체하지 않았다.
- 이 재점검은 테스트·검증 연결·문서 보완이다. 제품 변경 전 Red를 새로 수행했다고 주장하지 않는다. 최초 55개 HTTP 요청은 현재 구현에서 처음부터 통과한 보장 확인이다. 제품 TDD 기록과 실제 브라우저 전환은 [앞선 구현 기록](improvements-20261013.md)을 따른다.

## 최종 실행 결과

- 동일 caller workspace `workspace:1000050001`와 보조 pane `pane:1000050007`을 재확인했다. 백엔드 실행은 `surface:1000050011`, 프런트 headed 실행은 `surface:1000050014`에서 수행했다. 다른 workspace의 시각적 포커스를 따라가지 않았다.
- Java 25 `bash scripts/verify.sh`: exit 0, JUnit 428 tests, required suites 62, 실패·오류·skip 0. 새 JAR의 실제 공고 HTTP 55개 요청 통과, 생성 공고 잔여 0. API 서버는 owned finally에서 정상 종료 요청을 받아 종료됐다(프로세스 143, verifier 0).
- `npm run verify -- --headed`: exit 0, 단위 328개·개발/production 화면 212개. 212개 결과 각각의 `vue-warnings` attachment가 존재하고 모두 `[]`. 최종 종료 `2026-10-13T07:02:24.633Z`. 생산 빌드 SHA-256 `325f796e79b29299027d20726e23782b414675da3a3fd4fa2ccf4868453c8f7c`로 이전 제품 검증 때와 같다.
- 실제 상세 응답 복사본에서 `salaryPeriod` 삭제, `roles=null`, 첫 role의 `requiredSkills` 삭제를 수행해 validator가 세 경우 모두 거절했다. 원본 HTTP 응답은 통과했다.
- 실제 XML 보고서를 임시 폴더에 복사하면서 `AnnouncementDraftRecoveryTest` 하나만 제외했다. `Missing required test suites: com.inhatc.demp.service.AnnouncementDraftRecoveryTest`로 거절됐고 원본 보고서 428개/62 suites는 통과했다. 실제 보고서를 삭제하거나 제품 코드를 변형하지 않았다.
- 이번 후속 변경은 검증 스크립트·필수 목록·스킬·문서다. 실제 벽시계 브라우저 전환은 앞선 15:33~15:34 실행이며 이번에 새로 수행했다고 합산하지 않는다. 현재 JAR의 실제 HTTP 검증과 fixture headed 검증은 새로 실행했다.

[집계와 소스/JAR 식별](engineering-contract-summary-20261013.json), [실제 HTTP 요청별 상태·시간·크기](../../../.firecrawl/registration-contract-review-20261013/backend/announcement-contract.json), [백엔드 전체 로그](../../../.firecrawl/registration-contract-review-20261013/contract-full-verify.log), [필수 suite 거절 로그](../../../.firecrawl/registration-contract-review-20261013/required-suite-guard.log), [프런트 결과](../../../.firecrawl/registration-contract-review-20261013/frontend/summary.json)를 보존했다. `.firecrawl` 상세 근거는 Git 제외이며 로컬에서 볼 수 있다.

남은 제품 한계는 바뀌지 않았다. date-only 공개 정책, 미기재/조사 중 구분, 대용량 포스터 자동 처리는 구현하지 않았다. 외부 기업 일정 변경은 자동 감지하지 않는다. 운영 DB 변경·커밋·배포·원격 CI 실행도 수행하지 않았다.
