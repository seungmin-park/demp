# 공고 항목·조회·정렬 — 2026-10-08

Phase `announcement-catalog-and-discovery`, 양쪽 branch `refactor/announcement-catalog-and-discovery`, worktrees `.worktrees/announcement-catalog-and-discovery/{backend,frontend}`. 기준 backend는 기존 `57a57ac`에서 운영 main `7981f36`으로 fast-forward 했으며 frontend는 `269977d`다. 프런트는 앞선 화면 개선 Phase의 미커밋 patch를 의존 변경으로 가져왔다. 원래 worktree도 보존한다. 초기 구현 단계는 미커밋으로 유지했으며, 후속 사용자 승인으로 commit·PR·보호된 merge·운영 반영을 진행한다.

## 요구·책임·실패 조건

1. Notion 기록의 공개 공고와 실제 시장 목록을 비교해 직무 31개·스택 66개로 확장한다. 기존 문자열과 매핑은 보존한다. 조사 출처·마감/수집 한계는 프런트 `docs/announcement-market-research.md`에 기록한다. 개인 지원 기록은 저장소로 복사하지 않는다.
2. 공개 상세 성공 요청마다 공고 조회수를 1 증가한다. 목록·관련 목록·관리자 상세·편집·401·404·비공개 공고는 증가시키지 않는다. `recordView=false`는 순수 상세 계약이다.
3. 최신 등록 / 마감 임박 / 조회 많은 순을 URL과 검색 API에 연결한다. 기본 최신·고정 ID 동률 기준·첫 페이지 복원·기존 필터 보존을 검증한다. 만료·수동 마감·미확인 마감은 마감 임박 정렬에서 후순위다.
4. 저장 스키마와 현재 후보 JAR로 MySQL 리허설, 현재 cmux에서 실제 등록/검색/상세/reload/순수 편집을 확인한다. 운영 검증은 실제 환경 기록과 별도로 취급한다.

```text
공통 기술 항목표 → UI 선택/표시/URL → HTTP enum → VARCHAR 저장/검색
공개 상세 → Service 쓰기 TX → Repository 원자 증가 → 상세/목록 DTO
정렬 선택 → URL → 검색 조건 → DB ID 페이지 선택 → 관계 조회/순서 복원
```

조회수는 read-modify-save 대신 `hits = hits + 1`로 누락을 막는다. 관리자의 행 수정은 조회수를 새 폼 상태로 덮어쓰지 않는다. 정렬은 ID 페이지를 선택하는 기존 2단계 조회에 적용하며 관계 fetch 페이지 제한을 바꾸지 않는다.

## 검증 기록

- 항목 Red: 프런트 URL 신규값 소실/관리자 SRE 복원 누락/그룹 누락 3 assertion 실패. 백엔드 새 검색 enum HTTP 기대200/실제400. 저장 조합 SRE/AI 연구/게임 엔진 3 assertion 실패.
- 항목 Green: enum 추가와 공통 항목표·분류/검색을 연결했다. 서비스 commit 이후 JdbcTemplate의 독립 조회와 상세/검색 DTO에서 집합 전체를 확인한다. 테스트 트랜잭션으로 전체 서비스 호출을 감싸지 않는다.
- 첫 Green에서 기존 테스트의 6개 고정 수와 칩의 위치 의존을 발견해 66개 계약/이름 선택으로 갱신했다. readonly tuple 타입 추론 오류를 수정했다. 서비스 검증의 detached lazy collection 접근은 별도 SQL 조회로 바꾸어 실제 commit 검증을 유지했다.
- CI 브라우저 fixture의 발견을 실행 통과로 보고하지 않는다.

## 추가 UI/UX 요청 — 기술 선택

사용자가 내부 스크롤과 작은 체크박스의 밀집을 지적했다. 기존 primary `#4f46e5`, soft `#eef2ff`, ink `#0f172a`, muted `#475569`, line `#e2e8f0`, surface `#fff`, Pretendard를 유지한다. 기술 선택 제목 15px, 분야 제목 14px, 항목 13px를 사용하고 왼쪽 정렬한다. 8개 분야를 데스크톱 2열·모바일 1열로 모두 열어 두며 카테고리 탭·추가 확인 단계를 만들지 않는다. 작은 내부 스크롤 대신 페이지의 자연스러운 스크롤을 사용한다. 카드 경계는 분류를, 보라색은 선택을 나타내며 장식용 번호·그림자·그라데이션은 추가하지 않는다.

검색 → 선택 요약 → 분야별 선택 순서다. 검색으로 숨겨진 기술도 요약에서 한 번 눌러 해제할 수 있고 검색 초기화 후 선택이 유지된다. TechnologySelector는 검색·표현·props/event만 소유하며 부모는 저장 상태와 API를 소유한다. 사용자 개발 철학의 공통 규칙은 양쪽 AGENTS.md에 기록했다.

추가 Red는 요약/바로 해제/검색 초기화 3건과 짧은 창의 달력 높이 제한 2건이다. `/private/tmp/demp-discovery-ux-red.log`에서 실제 5 assertion 실패를 확인했다. 최소 구현 후 28/29 통과였으며 기존 초점 검증이 비동기 스타일 반영 이전을 읽었다. assertion은 유지하고 flushPromises로 달력 배치·초점 완료를 기다리게 했다. 달력은 실제 남은 위·아래 공간을 비교하고 선택한 공간으로 max-height를 제한한다.

## 조회수·정렬 Red → Green → Refactor

- 조회수 Red: frontend 상세/목록 숫자와 순수 조회 flag/API 부재 4건, backend HTTP hits/기본 증가·순수 flag·잘못된 flag 및 schema 양/음성 검증 실패를 실행했다. 실패는 기존 무효 flag 허용·필드/증가 경로 부재를 보여 주었다.
- Green: 공개 상세 service가 production 쓰기 트랜잭션으로 published 행을 원자 증가한 뒤 조회한다. DTO는 저장한 hits를 반환한다. 목록/관리자/순수 상세는 기존 읽기 경로를 유지한다. `@Modifying(clearAutomatically=true, flushAutomatically=true)`로 bulk update 이후 stale entity 조회를 방지한다.
- 실제 commit 검증: 1→2 및 순수0·draft/missing0, 동시16회 저장1..16, 관리자 수정과 방문 overlap에서 변경 제목과 hits2 보존을 확인했다. form은 hits를 소유하거나 덮어쓰지 않는다. 테스트용 트랜잭션으로 production 트랜잭션 누락을 가리지 않았다.
- 정렬 Red: frontend 선택/URL 계약 2건, backend orderBy 미전달/잘못된 enum 허용 등 6건 assertion 실패. 테스트의 Description import 충돌과 fixture null 기간 오류는 먼저 수정했고 기능 Red로 세지 않았다.
- Green: 검색 DTO가 LATEST/DEADLINE/VIEWS를 소유하고 repository가 ID 페이지 조회에 정렬을 적용한다. 마감 임박은 현재 모집 가능 공고의 마감 asc, 나머지는 후순위다. 동률은 id desc다. frontend는 URL을 원본으로 사용해 변경 시 페이지를 초기화하고 필터는 보존한다.
- Refactor: enum별 정렬 식을 한 경계에 두고 기존 ID 선택 → 관계 fetch → ID 순서 복원 흐름을 유지했다. 관계 fetch에 페이지 제한을 추가해 결과 수를 깨뜨리지 않는다. 문서와 required suites를 함께 갱신했다.
- 첫 전체 backend 검사에서 nested fixed Clock 테스트 설정이 다른 suite의 로그인 시간까지 바꿨다. 해당 repository suite의 @MockitoBean Clock으로 제한했다. 로그인 production 코드를 수정하지 않았고 현재 main 기반 전체 417 tests / 필수61 suites 통과했다.

## 사용자 실패와 회복

| 상황 | 지킬 계약 | 검증 |
|---|---|---|
| 기술 검색 결과 없음 | 기존 선택 유지, 검색 한 번 초기화 | unit 및 실제 cmux |
| 기술을 모두 해제한 저장 | HTTP 전송 금지, 이유/고칠 위치 표시·초점, 선택 즉시 오류 제거 | 실제 Red 후 Green, cmux 독립 조회로 저장값 불변 |
| 관리자 저장500 | 제목/본문/기술/검색 보존, 같은 값 재시도, 중복 전송 차단 | unit; 처음부터 통과한 기존 보장임 |
| 제보·처리 빈 값/초과/서버 오류 | 필드 오류와 초점, 길이 안내, 입력 보존·재시도 | UI Red/Green, 실제 빈 값·접수·처리 cmux |
| 조회401/404/draft/순수 편집 | 조회수 변경 금지 | HTTP·service commit·MySQL |
| 잘못된 직무/스택/정렬 | 잘못된 HTTP 조건 거절, 기존 데이터 보존 | enum 바인딩·MySQL |
| 정렬 동률/만료/수동 마감/미확인/페이지 | 고정 순서와 모든 내용·전체 개수/hasNext | repository·MySQL·실제 더보기 |
| 작은 창/모바일/키보드 | 달력 표시 범위·날짜 접근·Esc 초점·시분 보존 | unit regression 및 실제 cmux |

사용자 철학과 흐름 보존 원칙은 [AGENTS.md](../../AGENTS.md)의 공통 지침을 따른다. 로그인·회원가입은 사용자가 든 일반 원칙의 예시이며 이 Phase에서 인증 기능을 새로 구현한 것으로 보고하지 않는다.

## 객체 책임·이름·공개 계약 검토

- Announcement는 hits 상태와 기존 공개 상태를 가진다. Repository가 원자 증가와 DB 정렬을, Application Service가 공개 방문 쓰기 트랜잭션을 소유한다. Controller는 flag·enum의 HTTP 변환과 응답을 맡는다. `recordViewAndGetDetail`은 쓰기 효과를 이름으로 드러내며 기존 순수 조회 계약은 분리했다.
- TechnologySelector는 검색과 표현, props/event를 맡는다. 저장할 selectedLanguages와 API/저장 오류는 관리자 부모가 소유한다. error 제거 규칙도 부모에 남겼다. 날짜 선택기는 날짜 표현·키보드·팝업 배치를 소유하고 부모의 저장/미수정 초 계약을 변경하지 않는다.
- 기술/직무 공용 TS 자료와 formatter·route parser를 연결했다. Vue 컴포넌트는 props/event·상태/API 경계로, TS 모듈은 자료/공개 계약과 의존성 방향으로 검토했다. 추측성 인터페이스·단순 위임 객체를 만들지 않았다.
- Language/JSON `language`, legacy 문자열 `React`, VARCHAR 저장은 기존 외부 계약이므로 이름만 바꿔 깨뜨리지 않았다. 기술 스택이라는 화면 용어와 공용 Technology 타입을 사용한다. DTO/REST Docs/URL/API/DB 저장·검색으로 호출부와 직렬화를 검증했다.
- 읽기 전용 독립 리뷰에서 작은 창 달력 P2 1건을 발견·수정했다. 마지막 추가 회복 동작까지 재리뷰한 결과 추가 substantive finding이 없었다. 리뷰는 정적 결과이며 실제 테스트/GUI 결과와 구분한다.

## 완료 범위와 증거

[검증 기록](../verification/announcement-catalog-and-discovery/README.md)에 현재 backend 417 tests, frontend 53 suites/320 tests 및 types/contracts/lint/build exit0, MySQL158 checks, 실제 cmux 각 assertion 결과와 실패한 실행까지 남겼다. 기술8분야/66개·직무31개 저장/검색, 공고 원자 조회수, 3정렬 및 전체 요청 UI가 로컬 검증됐다. CI용 Playwright206 cases는 발견만 했으며 실행 통과로 주장하지 않는다.

[운영 질문 조회수](../verification/announcement-catalog-and-discovery/production-question-views.md)는 성공한 CD의 source/JAR identity와 실제 공개 목록2→상세3→reload4, 순수/편집4 유지로 확인했다. 새 공고 기능과 화면은 아직 미커밋·운영 미배포다. 대상 DB에 hits SQL을 먼저 적용해야 한다. 이 작업에서 운영 DB를 수정하거나 새 배포를 수행하지 않았다.

현재 cmux pane과 로컬 서버는 사용자가 결과를 볼 수 있도록 유지했다. 원래 main의 사용자 `docs/interview/`는 건드리지 않았다. 루트 AGENTS.md에는 사용자 철학만 추가해 이후 같은 프로젝트 세션에서도 읽을 수 있게 했다.

## 전달 승인과 배포 순서

2026-10-08 사용자가 남은 commit/PR/CI/merge, 운영 SQL/배포/실제 검증 목록을 확인한 뒤 진행을 승인했다. 기존 보호(필수 DEMP verify/frontend verify, strict 최신 main, 관리자 적용, conversation resolution, native auto/squash)를 실제 API로 다시 확인했다. backend 기준 main7981f36/frontend269977d는 현재도 일치한다. 검증 → 양쪽 signed commit/PR CI → 운영 백업/컬럼 사전 확인/추가 SQL → backend 보호 머지/main CI/CD → frontend 보호 머지/main CI/CD → 실제 운영 확인 순서다. 실패하면 그 단계에서 원인을 수정하며 CI/권한/배포 보호를 우회하지 않는다. 원시 로컬 검증의 미커밋/미배포 상태 값은 실행 시점의 역사 기록이므로 변경하지 않는다.
