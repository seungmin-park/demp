# 기능 지도

질문 조회수: 상세 진입/새로고침 → `recordViewAndGetDetail` → 원자 증가·commit → 응답/목록 재조회. 본인 조회 포함, 편집 `recordView=false`·목록·인증 실패·404 제외. 서비스 실제 commit/16건 동시 요청, MVC/REST Docs·보안 필터와 현재 cmux/Spring 사용자 흐름으로 검증한다.

공용 CI 경로와 검증 책임은 [CI·보호된 전달](ci-and-delivery.md)을 따른다. `bash scripts/verify.sh`는 기존 소유권 검사, 실제 전체 테스트와 구조 검사, 필수 XML, 생성 문서·JAR·인증 HTTP 일치, 방금 만든 JAR의 Spring/H2 저장·재조회 경로를 연결한다. 프런트 실제 브라우저 경로는 별도로 검증한다.

| 기능 | 사용자 경로 | 저장 소유자 | 검증 |
|---|---|---|---|
| 질문·답변 반응 | 로그인 → 질문 상세 → 추천/비추천 → 새로고침 | `ContentReactionService` + DB | 아래 흐름, `./gradlew test --tests '*ContentReaction*'` 및 실제 cmux 브라우저 |
| 답변 단건 생성·커서 조회 | 질문 상세 → 첫 20건 → 더 보기 → 작성·저장 | `AnswerService` + `AnswerRepository`; 반응은 기존 서비스 | `AnswerReadBudgetTest`·`AnswerPaginationTest`·MVC/REST Docs + 실제 Spring/MySQL 리허설 |
| 고용 형태 | 관리자 등록/수정 → 신입·경력과 별도 선택 → 공개 목록/상세/관련 조회 | `Announcement` EMP 상태 규칙, Service 트랜잭션, DB nullable 값 | 입력 MVC·commit 재조회·REST Docs·legacy schema·실제 MySQL, [기록](../plans/separate-employment-type.md) |

## 답변 생성·조회

`POST /api/answer/save`는 인증된 작성자의 새 답변 객체 1건을 반환한다. `Answer.createFor`는 FK 소유 측만 연결하므로 기존 부모의 답변 컬렉션을 읽지 않는다. 질문 삭제의 기존 cascade는 유지한다.

`GET /api/answer/{questionId}?before=<Long 정수 문자열>`은 ID 내림차순 `content` 최대 20개, `nextCursor` 문자열 또는 null, `hasNext`를 반환한다. Repository DTO projection은 DB에서 최대 21행을 읽고, 서비스는 반환하는 최대 20개 ID에 대해서만 내 반응을 조립한다. 없는 질문은 404, 빈 값·비정수·Long 초과 커서는 400이다. 전체 건수 조회는 하지 않는다.

새 스키마는 `Answer`의 JPA 매핑으로 `idx_answer_question_cursor(question_id, answer_id)`를 생성한다. 기존 스키마는 적절한 question-leading 인덱스가 없는 경우 [수동 SQL](../../src/main/resources/db/manual/answer-cursor-index.sql)을 적용한다. `AnswerRepositoryTest`는 새 스키마의 실제 인덱스 컬럼 순서를, `LegacySchemaCompatibilityTest`는 수동 DDL 후 인덱스와 기존 데이터·ID 연속성을 검증한다. 운영의 `ddl-auto=validate`는 인덱스를 만들거나 누락을 검출하지 않으므로 [배포 확인 순서](../verification/bounded-answer-creation-and-read/index-decision.md)를 따른다.

1,000개 fixture의 실제 query row count·엔티티/컬렉션 로딩과 0·20·21·40·41건, 삭제된 커서·다른 질문·음수·Long 경계를 검증한다. 본문·생성 제한은 추가하지 않으며 긴 단일 본문·대량 생성 비용은 잔여 위험이다. 작업별 실제 근거는 [답변 검증 기록](../verification/bounded-answer-creation-and-read/README.md)에 남긴다.

## 질문·답변 반응 저장

- 목적: 로그인한 회원이 질문/답변의 추천·비추천을 선택, 전환, 취소하고 새로고침해도 결과를 확인한다.
- 전제: local profile Spring/H2 서버, 프런트 dev server, 일반 회원 로그인. 공개 local fixture는 `local-member / password`다.
- 진입: `/questions/-1` 로컬 예제 → `추천`/`비추천` 버튼(접근성 `aria-label`, `aria-pressed`) → `PUT /api/question/{id}/reaction` 또는 `/api/answer/{id}/reaction` → 페이지 새로고침.
- 관찰: 선택 버튼 `aria-pressed=true`, 집계 1회 증감. 같은 요청은 멱등. `NONE` 취소 후 서버 재조회에서도 해제 상태.
- 저장: `ContentReactionController`는 인증 주체의 ID를 전달한다. `ContentReactionService`가 대상 행을 잠그고 이전/다음 상태 차이를 계산해 반응 행과 집계를 한 트랜잭션에 저장한다. Repository는 저장 경계. 프런트 `useContentReaction`은 미확정 응답·화면 이동 시 UI 상태를 다룬다.
- 실패: 미인증 401, 존재하지 않는 대상 404, 저장 실패 시 확정된 수치 보존/재시도, 화면 이동 뒤 늦은 응답 폐기.
- 검증: `.agents/skills/verify-demp/SKILL.md`의 정확한 명령과 cmux 표면 확인 후 실행. unit/MVC 테스트는 HTTP 계약과 DB 저장을 검증하고, 실제 브라우저는 별도 범위다. 과거 실제 실행 근거 `docs/verification/persistent-content-reactions/t99-t101.md`; Phase16 재실행 결과는 `docs/verification/agent-verification-and-official-docs/README.md`에 기록.

## 계정별 로그인 제한

`/login` → 기존 계정의 15분 내 실패 5회 → 429·Retry-After → 입력 보존·대기 안내. 최종 제한 상태는 Member DB, 인증/commit은 MemberService, 표시 상태는 LoginForm이 소유한다. 정상 로그인·시간 경계·DB 재조회·동시 요청·두 독립 컨텍스트·HTTP/CORS·단위/개발·배포 브라우저와 실제 Spring 화면을 함께 검증한다.

## 공고 등록·모집 상태

관리자 등록/수정 → `AdminOperationsController` HTTP validation → `AnnouncementService`/`AdminAnnouncementService` 트랜잭션 → `Announcement` 공개 불변식·`RecruitPeriod` 날짜 순서 → DB 저장 → 독립 HTTP 재조회. `AnnouncementSourceKey`와 DB UNIQUE는 URL/원문 식별 중복을 막는다. `Description`은 급여 단위, `RecruitmentRole`은 직무 조건을 소유한다.

모집 상태는 DB 상태 전환이 아니라 `AnnouncementQueryRepository`의 한국 시각 Clock 조회 판정이다. 화면 시간 갱신과 목록 재조회는 프런트 책임이다. 미확인 초안, 불완전 공개 거절, 중복 409 후 보존, 월급의 연봉 검색 제외를 `AnnouncementDraftRecoveryTest`·Repository/MVC/REST Docs와 **공용 verify의 실제 HTTP/H2** `scripts/verify_announcement_contract.py`가 검증한다. 상세/목록 응답 계약과 권한·헤더·삭제 후 404도 여기서 검사한다. required-suite 삭제와 응답 필드 삭제/null 드리프트는 각각 XML 검사와 계약 validator probe가 거절한다.

실제 cmux 등록·시간 경과는 [검증 스킬](../../.agents/skills/verify-demp/SKILL.md)의 공고 경로를 따르며, [작업 기록](../plans/real-posting-registration.md)에 코드·API·화면 검증 범위를 구분한다.

## 내 계정 관리

`GET/PATCH /api/member/me`와 `PUT /api/member/me/password`는 인증 principal의 ID만 사용한다. `MemberAccountService`가 트랜잭션과 현재 비밀번호 확인·공유 실패 제한을 조정하고 `Member`가 닉네임/비밀번호 버전을 소유한다. JWT 필터는 저장된 credentialVersion과 토큰 버전이 다르면 401로 거절한다. 기존 버전 없는 JWT는 0으로 해석하여 기존 회원을 보존한다.

`MemberAccountServiceTest`는 실제 commit/재조회·실패 보존·잠금·토큰 무효화를, `MemberAccountRestDocsTest`는 MVC/문서를, 공용 verify의 `member-contract.json`은 실제 HTTP 계약을 검증한다. 운영은 `db/manual/member-self-service.sql`의 nullable nickname/default-zero credential_version이 먼저 필요하다.

## 운영 기술 목록 제약 호환

기술 값은 Language enum이 소유한다. MySQL에 예전 6개만 허용하는 language_chk_1 CHECK가 있으면 새 기술 저장이 500으로 실패한다. `db/manual/announcement-technologies-check.sql`은 현재 enum의 허용 집합으로 CHECK를 교체하며 기존 값·FK·중복 제약을 유지한다. `TechnologyCheckMigrationTest`는 SQL에 enum 누락/추가가 생기면 CI에서 거절한다. 실제 MySQL 문법 및 CHECK 동작은 운영 백업의 별도 복원 DB에서 기존 CPP 거절→전체 66개 허용/미지 값 거절로 확인했다. 정적 집합 검사는 실제 MySQL 실행을 대체하지 않는다.

- 2026-10-15: [운영 목록의 게시 상태 필터](../plans/admin-announcement-status.md). 화면의 상태 선택은 URL과 API로 전달하고 저장소에서 페이지 분할 전에 필터링한다. 공개 조회의 PUBLISHED 경계는 유지한다. 검색은 첫 페이지로 돌아가며 새로고침 조건을 복원한다.

- 2026-10-15: [기술 미기재 공고 저장](../plans/optional-announcement-technologies.md). 공고에 기술이 없으면 빈 선택을 유지한다. 프런트 검증·DTO·도메인의 기술 필수 조건만 제거한다. 분야·본문·출처·모집 일정 조건과 권한 검증은 유지한다. 공채 분류나 추정 기술을 추가하지 않는다.

- [마감 임박순 기본 정렬](../plans/deadline-first-announcements.md): 공개 HTTP 정렬 기본은 DEADLINE, 운영 검색 기본은 LATEST를 유지한다.
