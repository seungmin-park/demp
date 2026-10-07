# 질문 조회수 저장 — 2026-10-07

Phase `question-view-count-persistence`, branch `refactor/question-view-count-persistence`; worktrees `.worktrees/question-view-count-persistence/backend` 및 `frontend`는 별도 저장소다. 기준 backend `13fcb24`, frontend `4f7736b`. 사용자가 본인·새로고침 포함 성공 상세마다 1 증가, 목록/편집/401/404 제외를 승인했다.

## 관찰과 책임

기존 상세 GET·새로고침·별도 API 조회가 모두 0이었다. `QuestionService.findById`에는 증가 경로가 없었다. UI 표시만 고쳐서는 DB와 목록이 바뀌지 않는다.

```text
상세 → viewQuestion → Controller → recordViewAndGetDetail → DB hits=hits+1 → 상세 DTO
편집 → getQuestionDetail(recordView=false) → Controller → findById → 기존 DTO
```

Repository는 원자 증가 SQL, Service는 증가/조회/commit, Controller는 HTTP 조건 변환을 맡는다. 기존 pure findById·JSON·태그/반응·스키마는 유지한다. 효과가 다른 API 이름을 분리하여 편집이 방문으로 기록되지 않게 했다. 새로운 상태 객체/테이블/클라이언트 증가 계산은 없다.

## 실행 증거

- Red: 서비스 `recordsEachSuccessfulView`는 기대 1/실제 0 assertion 실패. 컴파일을 위한 초기 usecase는 기존 pure 조회만 위임했고 집계 구현은 실패 확인 후 추가했다. Controller 기본 호출도 pure 경로여서 집계 usecase 검증 실패.
- Green: DB 원자 증가와 쓰기 트랜잭션, HTTP 기본 true/편집 false. 기존 순수 조회를 공유하므로 DTO/반응 반환이 중복되지 않는다.
- `bash scripts/verify.sh` Java 25: 397 tests, required suites 59, failure/error/skip 0, 실제 commit/16개 동시 요청 반환 1..16·저장16, 보안/HTTP/문서·패키지·served docs 일치.
- 현재 cmux workspace1000000002, runner surface1000000006, server11, browser9. 새 JAR SHA256 `56437d1d318a5e90d38b53730967b9cc7b93a77dbd804674f43a72bcfb6203e4`; 격리 H2 `demp_view_ui_20261007`, backend18081/frontend51017.
- 실제 화면 10 assertions: 목록0 → 진입1 → 추천 후1 → reload2/추천보존 → 편집 제목복원 → 목록2 → fresh pure API2/RECOMMEND → 목록 API2. 첫 실행의 잘못된 영문 seed 기대값은 실제 SQL의 ‘로컬 질문 예제’로 수정하고 새 DB에서 재실행, exit0.
- 독립 리뷰 Critical/Important 0. 선택적 반응 보존 보강은 실제 cmux 추천·reload·fresh API로 확인했다.

로그와 JSON: `/private/tmp/demp-question-view-count-20261007/`. 프런트 291 unit, 타입·경계 실제 위반 검출·lint·build·CD tests5 통과. CI fixture192는 로컬 실행하지 않았으며 cmux가 대체 통과를 증명하지 않는다.

## 전달 순서와 한계

프런트 집계 제외 조건을 먼저 전달한다. 이전 서버는 알 수 없는 조건을 무시하고 순수 조회를 유지하므로 호환된다. 이후 백엔드 집계를 전달한다. 백엔드에는 자동 CD가 없으므로 PR/main CI만으로 운영 조회수 완료라고 하지 않는다. 실제 backend artifact와 current frontend를 연결한 배포 후보·설치 결과를 별도로 확인한다. 최종 PR/CI/merge/CD 식별자는 PR 설명과 외부 progress 기록에서 갱신한다.
