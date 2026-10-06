# 답변 단건 저장·커서 조회 검증

2026-10-06부터 2026-10-07까지 두 `refactor/bounded-answer-creation-and-read` worktree의 미커밋 변경을 순차 TDD로 검증했다. 기반 main은 backend `bb6c6cf`, frontend `d419900`이며 앞선 CI 무결성·로그인 보호 PR #4는 각각 merge와 main CI까지 완료됐다. 아래 JSON은 로컬 검증 시점의 상태다. 2026-10-07 인덱스 도입 결과 보고 후 사용자가 commit·PR·merge 후속 전달을 승인했고, 해당 상태는 실제 GitHub 실행을 관찰해 별도로 확인한다.

ID 정밀도 수정 직후 `bash scripts/verify.sh`가 Java 375건·59 suite와 검사기 회귀 21건, 문서 생성/패키징/제공·실제 HTTP 저장/재조회를 통과했다. `npm run verify -- --headed`는 unit 235건·개발/배포 browser 166건, 타입·lint·build·소스/산출물 일치를 통과했다. 당시 MySQL assertion 64건과 새 앱의 실제 cmux assertion 8건도 통과했다. 필수 검증의 실패·오류·skip·브라우저 재시도는 모두 0이다. [최종 결과](final-results.json), [MySQL 결과](mysql-result.json), [cmux 결과](cmux-result.json), [최종 화면](cmux-final.png)을 함께 보관한다. 이후 인덱스 매핑 도입 후의 376건 검증은 아래 별도 절에 기록한다.

## 사용자 동작과 책임

```text
View: 질문 ID·인증 사용자 props
  → Answer component: 표시·입력·클릭
  → useQuestionAnswers: 현재 목록·커서·입력·요청 세대
  → API: 페이지 GET / 저장한 객체 POST
  → Controller: HTTP·커서 파싱·인증 ID
  → AnswerService: 정제·commit·반응 조립
  → Repository: DTO 최대 21행 / 반환할 답변 ID의 반응
```

생성은 부모 컬렉션을 읽지 않는 FK 소유 측의 `Answer.createFor`를 사용하고 저장한 `QuestionAnswer` 1건만 반환한다. 조회는 ID 내림차순 최대 20개와 문자열 커서·hasNext다. sentinel 1개를 포함해 DB 반환 최대 21행이며 전체 건수 조회는 없다. 프런트는 현재 목록에 단건을 앞에, 다음 페이지를 뒤에 합친다. keyed 반응 컴포넌트의 확정 상태와 기존 ID를 보존하며 화면 개수는 ‘표시된 답변’이라고 안내한다.

이름 검토: 기존 `createAnswerAndList`/`findByQuestion` 공개 service를 단건 `createAnswer`/페이지 `findAnswerPage`로 바꿨다. `findAnswerWindow`는 DB window, `loadInitial`/`loadMore`/`submitAnswer`는 공개 사용자 명령을 드러낸다. 기존 assign 관계 이동 helper·소유권·삭제 cascade는 유지한다. Vue는 props/event·렌더링으로 평가하며, TS composable/API는 상태 변경 이유와 HTTP 의존성으로 분리했다. 단순 위임 객체나 추측성 인터페이스는 추가하지 않았다.

## Red → Green

- Task 1: service 배열 반환, MVC JSON 배열, 기존 답변 3개 로딩으로 실제 assertion 3건 실패. 단건 생성·FK 연결 후 대상과 전체 345건 통과.
- Task 2: 페이지/잘못된 커서의 초기 assertion 9건 실패. 이어 경계·정렬·반응 로딩 테스트 10건 실패와 실제 query rows 1,000개·반응 엔티티 25개 로딩을 관찰했다. DTO DB limit 21/반환할 20개 반응 조회 후 367건·59 필수 suite와 공용 runtime 검증 통과.
- Task 3: 페이지 객체를 배열로 처리해 20개 예상에 3개 렌더링·null.answerId 오류가 발생했다. 구현 전 공개 상태 테스트와 개발/배포 headed browser Red도 작성·실행했다. API·props·상태 소유자 연결 후 로그인 만료 안내 1건 실패를 기존 mutationErrorMessage로 수정했다. 전체 unit 234건, 타입·lint 통과.
- Task 4: 새 browser fixture의 selector가 data-testid를 사용해 4건 timeout이 났다. 프로젝트의 data-test로 고친 뒤 동일 assertion을 포함한 전체 개발/배포 headed 166건이 통과했다. 경고·skip·flaky/재시도를 통과로 바꾸지 않았다.
- MySQL: 만료된 임시 credential 경로와 잘못 계산한 41개 fixture의 cursor 기대값은 검증 스크립트 오류였다. 기존 전용 컨테이너의 합성 DB credential만 내부적으로 읽고, `< cursor`의 정확한 20개 기대값으로 고쳤다. 환경/테스트 오류를 제품 Red로 세지 않는다.
- 인덱스: 레거시 schema의 실제 question-leading index 부재 assertion Red를 확인했다. 수동 `answer-cursor-index.sql` 적용 후 격리 MySQL assertion 61건 통과. 기존 인덱스를 먼저 조회하며 운영에는 적용하지 않았다.

Red/Green 명령·종료·원본 로그: `/private/tmp/demp-bounded-answers-20261006/{task1-*,task2-*,task3-*,task4-*}.{log,json}`. Red XML은 task1-red-xml, task2-red-xml, task2-pagination-red-xml, task2-query-budget-red-xml; 브라우저 최초 Red와 selector 실패의 trace/화면은 browser-red, browser-selector-failure에 보존했다. 최종 기록은 이 디렉터리의 결과 JSON을 참조한다.

## 최종 리뷰의 ID 정밀도 보완

새 검토자는 `gpt-6-astra`이며 두 저장소의 tracked diff·untracked 구현/테스트와 실제 근거를 읽었다. 숫자 answerId에서 9007199254740993과 9007199254740992가 같은 JavaScript 값으로 바뀌어 새 병합이 행을 누락하는 현상을 직접 재현했다. 원래 Minor 등급을 유효한 답변 누락의 효과에 따라 Important로 재평가했다.

숫자 wire 타입 때문에 GET/POST 경계 assertion 8건이 실패하는 Red를 관찰했다. 첫 jsonPath value 검사는 비교값을 문자열로 변환해 문제를 놓쳐 `isString`도 요구하도록 바로잡았다. `QuestionAnswer.answerId`를 문자열로 직렬화하고 프런트 wire 타입을 string으로 고정했다. 서버 내부 Long과 반응 경로는 유지하며 페이지·저장·반응에서 두 큰 ID가 별도로 남는 UI assertion, 숫자 DTO를 거부하는 type fixture와 실제 MySQL 큰 ID 경계를 검증했다. 타입 부정 fixture의 정밀도 손실 숫자가 lint에 걸려 안전한 숫자 `1`로 바꿨다. 어떤 숫자 ID도 타입 검사가 거부해야 한다는 assertion은 그대로다. 재리뷰 대신 같은 수정 단계에서 대상과 전체 suite를 다시 실행해 통과했다.

최종 명령·종료·로그: 임시 근거 디렉터리의 `final-id-wire-red`, `final-id-green`, `final-backend-verify`, `final-frontend-verify`, `final-mysql-verify`, `final-seed`, `final-cmux-flow`의 JSON/로그를 참조한다. 실행 시각과 JAR·프런트 SHA는 최종 결과에 기록했다. 후속 인덱스 리허설 스크립트를 포함한 350개 소스·테스트·검증 스크립트의 당시 SHA는 [도입 전 SHA](source-sha256-before-index-adoption.json), 매핑 도입 후 SHA는 [source-sha256.json](source-sha256.json)에 있다. 도입 단계에서 바뀐 소스/테스트는 Answer·AnswerRepositoryTest·LegacySchemaCompatibilityTest 3개이며 나머지 347개는 일치했다.

문서 feature-map의 기존 responsive 표 행 위치는 처음 Minor로 보류했다가 2026-10-07 사용자 후속 질문 이후 원래 기능 표 안으로 옮겼다. 문서 변경만이며 동작 변경은 없다. 운영 DB의 실제 인덱스/실행 계획, 실제 트래픽의 본문/쓰기 용량, 운영 배포·Security finding 종료는 검토하지 않은 범위로 기록하며 이를 수행한 것으로 주장하지 않는다. 모든 판단·비용은 실행 ledger에 남긴다.

## 실제 cmux와 별도 저장 확인

호출은 workspace:3/caller surface:6, 오른쪽 helper pane:8의 runner surface:14와 기존 browser surface:13을 사용했다. 최종 ID 수정 후 서버는 surface:16에서 새로 실행했다. 이전 로그인 서버 surface:12와 첫 답변 서버 surface:15를 종료하지 않았다. 최종 앱은 frontend `http://127.0.0.1:62814`, backend `http://127.0.0.1:62849`, 격리 H2 `demp_answers_ids_20261007`이다. 시작 시 JAR SHA와 검증된 실제 dist SHA를 기록하고 최종 검증의 산출물과 일치함을 확인했다.

실제 브라우저 로그인 후 `/questions/1`에서 20 → 더 보기 26 → 저장 27, 작성 입력 보존·성공 초기화, 새 답변 비추천, 새로고침 후 첫 20건·내 반응 복원을 확인했다. 별도 HTTP 세션이 문자열 새 ID·DISLIKE 1건과 20+7개 전체 ID를 확인했다. API fixture Playwright와 실제 저장 경로는 별도 증거다. H2 직접 SQL을 수동으로 조회했다고 주장하지 않는다.

비선택 WKWebView의 background 자동화는 로딩으로 돌아가 timeout이 났고 API에는 긴 답변이 정상 저장됐다. helper 브라우저를 표시하고 같은 실제 클릭·입력으로 검증하자 8개 assertion이 통과했다. 실패 로그도 유지한다. `cmux_flow.py`는 이 세션의 이미 로그인된 surface:13을 대상으로 한다. 현재 workspace와 브라우저 표시를 확인한 뒤 실행해야 한다. 로컬 서버·pane은 확인할 수 있도록 유지한다.

## 비용 측정과 한계

합성 Markdown 1,200회 반복: 원문 Unicode code point 13,220자/UTF-8 26,446바이트 → 실제 브라우저 HTML 28,828자/42,054바이트 → XHR JSON 요청 43,319바이트 → 서버 정제 HTML 28,828자/42,054바이트. 원문 끝·강조 1,200회·emoji 1,200회를 별도 조회했다. 실제 사용 빈도나 부하 한계의 측정은 아니다. 26회 H2·41회 MySQL 연속 성공 생성도 새로운 10회 차단이 없다는 합성 사례다.

MySQL의 번역한 레거시 fixture+1,000개 다른 질문 행과 큰 ID 경계에서 기존 answer table 전체 1,047행 스캔을 관찰했다. 수동 복합 인덱스 후 해당 질문의 후보 40행 조회로 바뀌었다. 소규모 fixture의 optimizer는 hash join·sort를 여전히 선택했고 결과는 LIMIT 21이었다. ‘내부 스캔도 21행’이나 운영 성능 개선 비율로 주장하지 않는다. 운영은 SHOW INDEX/EXPLAIN·백업 후 적절한 기존 question-leading index 여부를 판단해야 한다. ddl-auto validate은 인덱스를 검증하지 않는다.

긴 단일 LOB·본문 처리 CPU/전송 크기와 대량 생성 비용은 여전히 남는다. 본문 1만 자·15분 10건·제한 테이블은 추가하지 않았다. 이번 계약은 프런트·백엔드 함께 배포해야 한다. Google Cloud 배포·운영 DB 변경·Security finding 종료는 수행하지 않았다.

## 후속 인덱스 판단 리허설

2026-10-07 사용자 요청으로 같은 현재 JAR를 MySQL 8.4.11의 격리 DB 두 개에서 추가 검증했다. 각각 20만 답변·1천 작성자와 질문별 ID가 연속/섞인 두 분포를 만들고 PK만 존재·question_id 단독 추가·명시적 복합·FK 자동을 비교했다. 모든 후보에 answer_id의 PK 인덱스는 존재한다. 실제 HTTP 페이지 448회와 캡처한 실행 SQL 320개, 데이터 보존 등 assertion 82개가 통과했다.

프로젝트 결정은 **답변 조회용 `(question_id, answer_id)` 복합 인덱스 도입**이다. 기준 레거시 answer 스키마에는 PK만 있어 이번 배포 DDL에 기존 수동 SQL을 포함한다. 배포 대상의 기존 question-leading 인덱스 확인은 도입 결정을 미루기 위한 조건이 아니라 중복 DDL을 피하기 위한 절차다. 작은 21행 정렬이나 sub-millisecond 차이만으로 중복 인덱스를 추가하지 않는다. [판단과 배포 시 판별 순서](index-decision.md), [원본 실행 계획·측정](index-result.json)을 참조한다. 애플리케이션·기존 SQL의 동작을 바꾸지 않았으므로 이번 추가 검증을 전체 앱 suite 재실행으로 표현하지 않는다.

## 승인 후 인덱스 도입

2026-10-07 사용자 ‘도입 ㄱㄱ’ 승인 후 같은 브랜치에서 `Answer`에 `@Table/@Index`를 선언했다. 새 Hibernate 스키마의 물리적 인덱스 컬럼 순서를 검사하는 repository 테스트와, 기존 수동 SQL을 포함한 legacy 스키마 호환성 검증을 연결했다. 먼저 6개 대상 테스트 중 2개가 인덱스 부재 때문에 실패했고, 최소 매핑/SQL 목록 수정 후 6개가 통과했다. 객체 책임·이름·컬럼·호출부를 검토했으며 서비스·HTTP 계약이나 수동 SQL의 변경은 필요하지 않았다.

새 JAR로 공용 전체 검증을 다시 실행하여 Java **376건·59 suite**, Python **21건**, 문서·패키지·실제 H2 HTTP가 통과했다. 격리 MySQL `demp_release_20261007_004154`에서는 **64개 assertion**, 실제 `(question_id, answer_id)` 컬럼 순서와 인덱스 사용·전체 답변 스캔 제거를 확인했다. JAR SHA는 `17d409dbd1c92eb82ddbf5e24ee61ebd3b3b0c2008ec31ef48d0fb67ba714d58`이다. [도입 결과](index-adoption-result.json), [도입 후 MySQL 원본](index-adoption-mysql-result.json)을 참조한다. 도입 전 20만 건 성능 측정의 JAR와 원본 결과는 그대로 보존했다.

현재 cmux 호출 workspace:3/caller surface:6을 다시 확인했을 때 이전 helper pane:8은 없었다. 오른쪽 helper pane:10/runner surface:18을 focus false로 만들고 대상/전체/MySQL 명령과 로그를 표시했다. 실행 로그·종료 JSON과 Red/Green XML은 `/private/tmp/demp-bounded-answers-20261006/index-adoption-*`에 있다. 이번 변경은 백엔드 스키마 매핑이므로 프런트와 브라우저 흐름을 다시 실행한 것으로 보고하지 않는다. 운영은 여전히 `ddl-auto=validate`이며 기존 DB에는 수동 DDL과 실제 인덱스 확인이 필요하다. GCP/운영 DB에 적용하거나 commit·PR·merge하지 않았다.
