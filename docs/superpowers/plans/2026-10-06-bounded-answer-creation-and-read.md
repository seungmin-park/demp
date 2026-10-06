# 답변 단건 저장 응답·커서 조회 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. 사용자가 선택한 현재 세션의 순차 실행을 유지한다. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 답변 생성은 저장한 1건을 반환하고, 조회는 ID 커서로 최대 20건씩 제공하면서 입력·반응·기존 데이터를 보존한다.

**Architecture:** AnswerService가 생성 트랜잭션·단건 응답과 제한된 조회 조립을 맡는다. Repository의 DTO projection은 최대 21행, 내 반응 조회는 반환할 20개 ID로 제한한다. 프런트 composable이 목록·커서·입력·요청 세대를 소유하며 컴포넌트는 props/event·렌더링에 집중한다.

**Tech Stack:** Java 25 `zulu-25.36.205`, Spring Boot/JPA·Hibernate·Jackson·JUnit·MockMvc·REST Docs의 현재 프로젝트 버전, Node `24.21.0`, 현재 Vue 3·TypeScript·Vitest·Playwright. 의존성·러너를 새로 도입하지 않는다.

**Spec:** [승인된 수정 명세](../specs/2026-10-06-bounded-answer-creation-and-read-design.md). 사용자 승인: ‘수정 명세 승인 — 순차 TDD 계획 작성’.

## Global Constraints

- 두 브랜치 `refactor/bounded-answer-creation-and-read`, worktree `.worktrees/bounded-answer-creation-and-read/{backend,frontend}`.
- 조회 응답 최대 20개, 다음 존재 확인 포함 DB 조회 최대 21행, ID 내림차순. 커서는 Long 범위의 정수 문자열이며 음수 local fixture도 허용한다.
- 저장은 기존 200과 QuestionAnswer 필드를 유지하며 배열 대신 객체 1건을 반환한다. 기존 인증 ID·정제·소유권·삭제 cascade를 유지한다.
- 본문 10,000자·15분 10건 제한, 생성 기록 테이블·Member 제한 컬럼·회원 잠금은 이번 구현에 추가하지 않는다.
- 실제 assertion Red를 프로덕션 변경보다 먼저 확인한다. 컴파일·환경 오류를 Red로 기록하지 않는다. Service 테스트에 테스트용 트랜잭션을 붙이지 않는다.
- 현재 cmux 호출 workspace·surface 확인 후 보조 pane·브라우저를 재사용하며 E2E 명령·로그·실제 클릭을 표시한다. 기존 서버·다른 workspace를 임의로 정리하지 않는다.
- 현재 CI·로그인 보호의 로컬 검증과 이 계획의 실행 결과를 구분한다. 새 worktree의 빌드·검증 결과를 새로 기록한다.
- 별도 commit 승인 전에는 미커밋 상태로 둔다. PR·머지 권한이 있을 때는 정확한 head의 필수 CI·보호된 squash merge·merge SHA의 main CI까지 확인한다.

## Review Focus

1. 커서가 삭제된 행·다른 질문·음수 fixture·Long 경계여도 질문 범위와 정렬을 유지한다 → Task 2.
2. 새 답변의 FK를 연결할 때 기존 부모 컬렉션을 읽지 않고 질문 삭제 cascade가 유지된다 → Task 1.
3. 저장과 더 보기의 응답 순서가 역전돼도 새 답변·기존 목록·작성 입력을 잃지 않는다 → Task 3.
4. 더 보기 중 바뀐 추천·비추천과 질문 이동 뒤 도착한 응답이 기존/새 화면을 오염하지 않는다 → Task 3·4.
5. 긴 기존 본문·빠른 정상 작성이 이번 변경으로 새롭게 차단되거나 잘리지 않는다 → Task 4. 비용 측정은 제한 정책의 완료 증거와 구분한다.

## 파일과 인터페이스 지도

아래 경로는 각각 backend 또는 frontend Git root 기준이다. 기존 suite는 삭제하지 않는다.

| 저장소·파일 | 책임 |
| --- | --- |
| backend `src/main/java/com/inhatc/demp/domain/Answer.java` | `static Answer createFor(Member member, Question question, String content)`; FK 소유 측만 연결 |
| backend `dto/answer/AnswerPage.java` (동일 Java 패키지 root 아래 신규) | `record AnswerPage(List<QuestionAnswer> content, String nextCursor, boolean hasNext)` |
| backend `dto/question/QuestionAnswer.java` | 기존 필드/생성자 유지, projection용 `(Long answerId, String username, String content, int recommend, int dislike)` 생성자 |
| backend `repository/AnswerRepository.java` | `List<QuestionAnswer> findAnswerWindow(Long questionId, Long before, Pageable pageable)`; 정렬·DB limit |
| backend `repository/ContentReactionRepository.java` | `List<ContentReaction> findByMember_IdAndAnswer_IdIn(Long memberId, Collection<Long> answerIds)` |
| backend `service/AnswerService.java` | `QuestionAnswer createAnswer(Long actorId, AnswerForm form)`, `AnswerPage findAnswerPage(Long questionId, Long actorId, Long before)` |
| backend `controller/AnswerController.java` | 새 반환 형태·커서 바인딩·인증 ID 전달 |
| frontend `src/types/api.ts` | `AnswerPage { content: Answer[]; nextCursor: string | null; hasNext: boolean }` |
| frontend `src/api/answers.ts` | `getAnswers(questionId: EntityId, before?: string): Promise<AxiosResponse<AnswerPage>>`, `createAnswer(form: AnswerForm): Promise<AxiosResponse<Answer>>` |
| frontend `src/composables/useQuestionAnswers.ts` (신규) | 읽기 전용 상태와 `setBody(body: string)`, `loadInitial()`, `loadMore()`, `submitAnswer()`; props 감시·수명/요청 세대 |
| frontend `src/components/question/QuestionAnswer.vue` | `questionId: EntityId`, `username: string` props, 상태 렌더링·입력/클릭 전달 |
| frontend `src/views/question/QuestionDetail.vue` | 현재 route ID·로그인 username을 답변 컴포넌트 props로 전달 |

Java 표의 축약 경로 앞에는 `src/main/java/com/inhatc/demp/`가 붙는다. frontend composable 입력은 반응형 props `{ questionId: EntityId; username: string }`이다. 반환 상태에는 `answers`, `body`, `nextCursor`, `hasNext`, `loading`, `loadingMore`, `saving`, `loadError`, `moreError`, `saveError`를 둔다. 부모 화면에서 route를 해석하고, 답변 컴포넌트의 테스트는 props 변화로 질문 이동을 검증한다.

## Task 0: 소스·실행 기반 확인

**Files:** 기존 `.tool-versions`, 두 AGENTS.md·검증 스킬, 이전 `final-results.json`, 두 검증 manifest. 결과는 `docs/verification/bounded-answer-creation-and-read/README.md`에 기록한다.

- [x] 두 worktree의 branch·HEAD·이전 변경 보존을 확인한다. 기존 main·`docs/interview/`를 수정하지 않는다. 이전 결과를 새 결과로 복사해 통과 처리하지 않는다.
- [x] Java 25/Node 24 경로를 확인하고, 필요할 때 새 프런트 worktree에서 `npm ci`를 실행한다. 파일·네트워크 권한 오류는 제품 실패와 구분해 같은 명령의 정식 escalation으로 해결한다.
- [x] 실행할 cmux 호출 환경을 확인하고 기존 helper terminal·browser를 재사용한다. 실행 경로를 먼저 알린다. 현재 살아 있는 로그인 검증 서버는 다음 빌드의 서버와 구분한다.
- [x] 초기 전체 검증을 두 저장소에서 순차 실행하여 baseline을 기록한다. 기존 실패가 있으면 원인을 분류·해결하고, 테스트 0개나 skip을 통과로 처리하지 않는다.

## Task 1: 생성 단건 반환과 전체 컬렉션 읽기 제거

**Files:** Modify `Answer.java`, `AnswerService.java`, `AnswerController.java`; Test `src/test/java/com/inhatc/demp/service/AnswerServiceTest.java`, `src/test/java/com/inhatc/demp/controller/AnswerControllerTest.java`, `src/test/java/com/inhatc/demp/docs/AnswerRestDocsTest.java`, `src/test/java/com/inhatc/demp/service/QuestionServiceTest.java`; Create `src/test/java/com/inhatc/demp/service/AnswerReadBudgetTest.java`; Modify `scripts/verify_local_flow.py`의 POST 소비 부분.

**Interfaces:** Consumes 기존 `AnswerForm`, 인증 actor ID, ContentSanitizer. Produces `createAnswer`와 `Answer.createFor`의 위 공개 계약. 조회 배열은 Task 2까지 기존 계약이다.

- [x] `returnsOnlyCreatedAnswer`를 기존 메서드로 실행 가능하게 작성한다. `Object result = service.createAnswerAndList(...)`에 `assertThat(result).isInstanceOf(QuestionAnswer.class)`를 사용해 현재 배열 반환 때문에 실패하도록 한다. 기존·새 답변의 별도 DB 재조회도 유지한다.
- [x] MockMvc의 기존 저장 테스트에서 배열 path 대신 `$.content`, `$.username`, `$.myReaction`과 객체 형태를 요구한다. 현재 service mock 반환 타입은 유지하여 컴파일 오류가 아닌 실제 JSON assertion 실패를 얻는다.
- [x] `doesNotLoadExistingAnswersWhenCreating`는 저장된 기존 답변 집합을 준비한 뒤 Hibernate 통계를 초기화하고 실제 service commit을 호출한다. `Answer` 기존 행 로딩·Member.answers/Question.answers 컬렉션 로딩이 0이어야 한다. fixture 준비 SQL과 검증 SQL은 측정 구간에서 제외한다.
- [x] Red 실행: `./gradlew test --tests '*AnswerServiceTest' --tests '*AnswerControllerTest' --tests '*AnswerReadBudgetTest' --console=plain`. JSON 배열/객체 불일치와 기존 답변 읽기 실패를 로그에 보존한다.
- [x] 최소 Green: `createFor`는 FK 소유 측만 연결하고, 서비스는 정제·저장 후 해당 DTO 1건을 조립한다. 메서드 rename과 기존 mock·호출부·REST Docs POST·local-flow POST 소비를 함께 바꾼다. 단건 초기 집계 0·NONE을 검증한다.
- [x] `AnswerServiceTest`의 기존 전체 반환 assertion은 승인된 단건 계약으로 바꾸되, DB에 기존 답변이 남는 assertion·위조 username 무시·권한·sanitizer assertion을 유지한다. 질문 삭제의 기존 service/cascade 테스트도 실행한다.
- [x] 대상과 `./gradlew test --console=plain`을 통과시킨 뒤 이름·책임을 리뷰한다. 이 단계는 백엔드 단건 계약의 검증이며 프런트 전달 완료로 보고하지 않는다. commit하지 않는다.

## Task 2: DB에서 제한하는 커서 페이지와 내 반응

**Files:** Create `src/main/java/com/inhatc/demp/dto/answer/AnswerPage.java`; Modify 위 DTO·두 Repository·Service·Controller; Test 기존 `AnswerRepositoryTest`, `AnswerControllerTest`, `AnswerRestDocsTest`, `CommunityQueryBudgetTest`, `ApiSecurityTest`; Create `src/test/java/com/inhatc/demp/service/AnswerPaginationTest.java`; Modify `scripts/verify_local_flow.py`, `scripts/verify_runtime.py`의 GET 소비.

**Interfaces:** Consumes Task 1의 단건 생성. Produces `findAnswerPage`, `findAnswerWindow`, 한정된 반응 Repository, `AnswerPage`.

- [x] `returnsAtMostTwentyAnswers`는 현재 `findByQuestion` 호출로 답변 21개가 20개 상한을 넘는 실제 assertion Red를 먼저 얻는다. MVC GET에는 `content`, `hasNext`, `nextCursor`를 요구하여 현재 배열 형태의 실패도 기록한다.
- [x] `pagesAllAnswersInDescendingIdOrder`에 0·20·21·40·41건을 준비한다. 첫/다음/마지막의 전체 ID와 순서, 20개 크기, 마지막 null·false, 중복·누락 없음을 검증한다. Repository는 `@DataJpaTest`, 서비스는 실제 commit·자식 우선 `@AfterEach` cleanup을 사용한다.
- [x] `keepsQuestionScopeWhenCursorWasDeleted`, `acceptsNegativeLocalCursor`, `rejectsMalformedAndOverflowCursor`, `readsOnlyPageReactions`를 작성한다. 존재하지 않는 질문 404도 확인한다. 새로운 메서드 부재로 인한 컴파일 실패는 추가 동작의 Red로 주장하지 않고, 먼저 기록한 실행 가능한 Red와 최소 골격을 유지한다.
- [x] Red 실행: `./gradlew test --tests '*AnswerPaginationTest' --tests '*AnswerControllerTest' --tests '*CommunityQueryBudgetTest' --console=plain`.
- [x] 최소 Green: 질문 존재 확인, `PageRequest.of(0, 21)`의 정렬된 DTO projection, 처음 20개에 대한 반응 조립, 21번째로 hasNext 판단. 전체 COUNT·전체 배열·부모 컬렉션 읽기를 추가하지 않는다. 커서는 응답 마지막 ID 문자열이다.
- [x] 공개 전체 목록 service/HTTP 우회는 제거하고 모든 호출부·문서·runtime script를 페이지로 갱신한다. 테스트 fixture를 위한 제한 없는 Repository 조회는 제품의 공개 우회 경로로 사용하지 않는다.
- [x] 1,000개 fixture에서 SQL과 로딩 규모를 확인한다. 쿼리 수만 적고 전량을 읽는 것을 통과시키지 않는다. MySQL의 기존 인덱스·EXPLAIN을 먼저 확인하고 필요할 때만 수동 인덱스 SQL을 추가한다. 인덱스 누락을 ddl-auto validate이 검출한다고 가정하지 않는다.
- [x] 대상·전체 백엔드 테스트 후 `bash scripts/verify.sh`를 실행한다. runtime의 별도 반응 조회·문서/JAR 일치까지 통과해야 한다. 필수 manifest에 새 두 suite를 추가하고 기존 suite를 유지한다.

## Task 3: 페이지·단건을 합치는 프런트 상태와 화면

**Files:** Modify `src/types/api.ts`, `src/api/answers.ts`, `src/components/question/QuestionAnswer.vue`, `src/views/question/QuestionDetail.vue`; Create `src/composables/useQuestionAnswers.ts`, `tests/unit/useQuestionAnswers.spec.js`; Modify `tests/unit/QuestionAnswer.spec.js`, `tests/unit/DetailStates.spec.js`, `tests/unit/HtmlRendering.spec.js`의 답변 fixture/props.

**Interfaces:** Consumes Task 2의 페이지 GET·단건 POST. Produces 위 composable/props 계약. API를 다른 화면 상태 모듈에서 직접 우회하지 않는다.

- [x] 컴포넌트 테스트에 실제 새 계약의 mock 응답을 먼저 준다. `addsOnlyCreatedAnswerWithoutReplacingPreviousAnswers`는 초기 20건 뒤 POST 객체 1건을 받아 21건·입력 비우기·기존 목록 유지·추가 전체 GET 없음·커서 유지를 요구한다. 현재 배열 교체 동작의 실제 실패를 기록한다.
- [x] `loadsNextPageAndKeepsInputAfterFailure`는 더 보기 클릭·오류·같은 before 재시도·마지막 버튼 종료를 요구한다. 기존 입력과 답변을 유지하고 개수는 표시한 개수라고 안내해야 한다.
- [x] Red 실행: `npm test -- tests/unit/QuestionAnswer.spec.js tests/unit/DetailStates.spec.js tests/unit/HtmlRendering.spec.js`.
- [x] 최소 Green으로 API/타입과 화면을 연결한 뒤 상태 책임을 composable로 옮긴다. route/username 조립은 부모 view의 props, 답변 component는 렌더링·이벤트, API는 HTTP에 둔다. 원문 입력은 setBody로 변경한다.
- [x] composable 테스트에서 `keepsCreatedAnswerWhenMoreFinishesLater`·`keepsCreatedAnswerWhenSaveFinishesLater`를 deferred Promise 양방향으로 검증한다. 더 보기·저장의 개별 연타를 막되 서로의 실행은 막지 않는다. 현재 목록을 기준으로 합치고 기존 ID의 확정 상태를 우선 보존한다.
- [x] `discardsOldQuestionResponses`·`discardsResponsesAfterUnmount`는 props 이동/unmount 뒤 이전 저장/조회가 새 입력·목록·오류를 바꾸지 못함을 검증한다. `doesNotResetAnswerReactionWhenMoreArrives`는 버튼 추천/비추천 이후 추가 응답에도 확정된 반응이 유지됨을 확인한다.
- [x] `npm test -- tests/unit/useQuestionAnswers.spec.js tests/unit/QuestionAnswer.spec.js tests/unit/DetailStates.spec.js tests/unit/HtmlRendering.spec.js`, 전체 `npm test`, typecheck·lint를 통과시킨다. 예상하지 않은 Vue 경고·any·타입 오류 억제를 추가하지 않는다. 기존 HTML 안전 렌더링 assertion은 유지한다.

## Task 4: 전체 자동·실제 서버·사용자 흐름 검증

**Files:** Modify `tests/e2e/community.spec.js`, `tests/e2e/responsive-layout.spec.js`의 답변 fixture, `scripts/required-verification.json`, 두 `docs/engineering/feature-map.md`; Create `backend/docs/verification/bounded-answer-creation-and-read/README.md`와 실제 실행 근거. 기존 verify·Playwright 프로젝트를 사용한다.

- [x] API fixture를 새 계약으로 바꾸고 ‘답변 20개 조회 후 더 보기와 단건 저장을 함께 진행한다’·‘답변 더 보기 실패는 목록과 작성 입력을 보존한다’를 작성한다. 기본 개발·배포 두 프로젝트에서 실제 headed Red를 먼저 실행하고 실패 화면/trace/assertion을 보존한다.
- [x] 최소 Green 후 필수 unit/e2e manifest에 새 흐름·assertion을 추가한다. 기존 로그인·반응·반응 취소·권한 흐름과 320px/390px responsive fixture를 새 응답으로 정합화하며 assertion은 유지한다.
- [x] 새 worktree의 현재 코드로 전체 백엔드 `bash scripts/verify.sh`, 프런트 `npm run verify -- --headed`를 순차 실행한다. 실제 비영 테스트 수, 실패/오류/skip 0, source/build 일치, Vue 경고 0, 생성·패키징·제공 문서 일치를 확인한다.
- [x] helper pane에서 새 JAR·새 프런트 build를 격리 H2·loopback으로 실행하고 실제 cmux 브라우저로 로그인 → 첫 20건 → 더 보기 → 작성/저장 → 반응 → 새로고침을 조작한다. 새로 저장한 ID와 반응을 별도 API/DB 조회로 확인한다. fixture E2E로 DB 저장을 증명했다고 표현하지 않는다.
- [x] 합성 긴 본문·연속 작성 사례에서 새 1만 자/10회 차단과 본문 자르기가 생기지 않았음을 확인한다. 사용자 원문 글자 수·렌더링 HTML·직렬화 요청 UTF-8 바이트·정제 바이트의 차이는 측정 결과로 기록한다. 실제 사용자 빈도가 없으면 합성 사례라고 표시한다.
- [x] MySQL 격리 리허설에서 커서 페이지·단건 저장·기존 데이터·삭제 cascade·인덱스/실행 계획을 확인한다. 운영 DB에 적용하지 않는다. H2 결과를 MySQL 확인으로 대체하지 않는다.
- [x] Red 명령·실패 원인, Green 최소 변경, Refactor 책임/이름, 최종 소스·빌드 SHA·로그·DOM assertion·리뷰·잔여 대량 생성/본문 위험을 기록한다. 로컬 확인용 서버와 pane을 유지한다.

## Task 5: 권한이 있는 GitHub 전달과 최종 상태 확인

**Files:** 기존 CI/보호 설정·검증 자료, 두 PR 설명. 구현이 끝난 정확한 변경을 review한다.

- [x] 두 diff·공개 계약·데이터 보존·객체 책임을 최종 리뷰하고, 소스와 최종 검증 근거를 대조한다. 새 변경/통합 수정이 생겼을 때만 영향 범위의 검증을 다시 실행한다.
- [x] 해당 변경의 commit·PR·머지 요청이 있는지 확인한다. 2026-10-07 답변 보호·인덱스 결과와 준비된 두 PR를 제시한 뒤 사용자가 ‘ㄱㄱ’로 후속 전달을 승인했다. 앞선 CI/로그인 전달 승인과 별도의 승인이다.
- [ ] 승인 범위에서 기존 서명 설정을 유지해 commit·push한다. 같은 head의 열린 PR을 갱신하거나 main 대상 PR을 만든다. PR 설명은 최종 사용자 동작과 검증·실제 한계를 기준으로 작성한다.
- [x] 실제 main 보호·ruleset·필수 check의 GitHub Actions app·strict 최신 base·관리자 동일 적용·auto-merge/squash를 재조회한다. 2026-10-07 backend `DEMP verify`, frontend `DEMP frontend verify`, app 15368, strict/enforce_admins true, rulesets [], native auto-merge/squash true를 확인했다. main은 검증 기반 bb6c6cf/d419900과 일치했다.
- [ ] 정확한 PR head SHA의 필수 check completion/conclusion과 실제 XML·browser 결과를 확인한다. missing/skipped/neutral/0개를 실행 통과로 보고하지 않는다. base가 움직이면 정책에 따라 통합·재검증한다.
- [ ] 정확한 head에 native `gh pr merge <PR> --auto --squash --match-head-commit <SHA>`를 적용한다. 보호를 확인할 수 없으면 그 제한을 보고하고 우회하지 않는다. main 직접 push·admin bypass를 사용하지 않는다.
- [ ] MERGED·merge SHA·merge 시각과 그 SHA의 main CI를 확인한다. auto-merge 신청만으로 완료라고 하지 않는다. 읽기 전용 `agent-engineering/scripts/pr_status.py`를 실제 check 이름·head·goal merged로 사용하고 underlying 결과도 확인한다.
- [ ] 최종 보고는 ‘로컬 검증’, ‘PR head CI’, ‘실제 merge’, ‘merge SHA main CI’, ‘배포’의 관찰된 상태를 구분한다. 이 계획에서 운영 배포 완료를 주장하지 않는다.

## 검증 명령과 계획 self-review

각 명령은 해당 Git root에서 Java 25/Node 24 환경으로 실행한다. 로컬 E2E/서버/브라우저 명령은 현재 cmux helper에서 표시한다.

```sh
export JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205/Contents/Home
export PATH=/Users/seungmin/.asdf/installs/nodejs/24.21.0/bin:$JAVA_HOME/bin:$PATH
# backend root
./gradlew test --console=plain
bash scripts/verify.sh
# frontend root
npm test
npm run typecheck
npm run lint
npm run verify -- --headed
```

- [x] 명세의 단건/20개/21행/내 반응/입력 보존/경쟁/레거시/잔여 위험을 Tasks 1–4에 연결했다.
- [x] 다섯 Review Focus를 소유 task의 테스트에 연결했다.
- [x] DTO·service·API·composable 명칭과 타입을 위 Interfaces 지도에 고정했다.
- [x] 실제 실행 전인 체크박스는 모두 미완료다. 이번 계획 작성은 애플리케이션 Red/Green 실행이 아니다.
- [x] 기존 순차 실행 선택을 유지하며, 사용자가 계획을 승인했고 구현을 순차 진행했다. commit/PR/merge는 해당 변경의 승인 범위에서 Task 5까지 관찰한다.
