# Phase 9 실행 계획
Spec: tasks.md Phase 9, 사용자 관리자 페이지 추가 지시. 기준 B3743122 / Fd03323d.
Worktrees: `.worktrees/admin-console-and-authorization/{backend,frontend}`, 양쪽 `refactor/admin-console-and-authorization`.

## Global Constraints
DEMP 디자인·기존 회원/소유자 API 유지. 모든 관리자 API는 서버 DB 역할 검증, 공개 가입은 ROLE_USER만. 실제 Red→Green, task별 체크/commit. 로컬 H2 fixture와 현재 cmux terminal12/browser13 사용, headed fixture E2E와 실제 API 흐름 구분.

## Task 80: 관리자 접근 계약
Security matcher `/api/admin/**` → ROLE_ADMIN; GET me는 DB에서 검증된 principal의 id/username만 제공. AdminLayout은 me 성공 뒤에만 내부 화면을 표시하고 401은 로그인, 403은 권한 안내, 기타 오류는 재시도. 공개 가입의 역할 주입·서명 토큰 역할 위조·DB 권한 회수 검증. 계정 준비는 기존 회원에 DB 역할 부여 절차만 문서화, 기본 운영 비밀번호 없음.
Expected: 보안401/403/200/revocation, UI server authority Red→Green, 전체 gates, 체크/commit.
Consumes: JWT DB 재조회와 기존 로그인. Produces: 관리자 API와 UI의 공통 권한 경계.

## Task 81: 운영 화면과 API
실제 DB 집계 대시보드, 공고/교육 목록·검색·등록·수정·삭제, 질문·답변 검색/내용 조회/수정/삭제. 관리자 공고 create는 기존 파일 보상 서비스 사용, update는 이미지 선택 유지 또는 교체, delete/replacement는 DB commit 후 기존 파일 정리. 공개 소유자 서비스 변경 없이 별도 관리 서비스. HTML→Markdown 편집→HTML 저장 연결. 확인 대화상자·pending·validation/error/retry·모바일 제공. MVC REST Docs+서비스 commit 검증, API 타입/프런트 계약 갱신.
Expected: 실제 별도 조회로 CRUD·연관 삭제·파일 보상, HTML 수정 회귀, 전체 tests/types/lint/build/docs, 체크/commit.
Consumes: T80 권한과 T70 Markdown. Produces: 실제 운영 CRUD.

## Task 82: 최종 인수
역할/직접 URL/만료/일반 회원 회귀 headed E2E. 현재 cmux 실제 H2 관리자 fixture로 create→edit→별도 조회→delete, desktop/mobile 캡처. 전체 gates와 한 번의 전체 branch review, 중요 수정 1회 TDD.
Expected: 테스트/로그/스크린샷 기록, 체크/commit.

## Review Focus
서버 역할 우회, 페이지 크기 제한·검색, HTML 안전성/기존 서식, 수정 시 이미지/연관 데이터 보존, 파일 삭제 실패의 정직한 결과, 중복 제출·응답 역전·권한 회수, 실제 DB 집계.

## 사용자 추가 요구: T83/T84
이미지 기능은 유지한다. T83은 기존 모집 유형/경력 범위를 공통 배지로 보여주고 관련 공고 DTO를 보강한다. T84는 유형·필터·검색어·로딩·실패·목록 끝을 구분해 안내하고 조건 해제로 현재 탭을 유지하며 복구한다. 각 작업의 Red/Green/검증은 t83.md/t84.md에 기록하고 tasks.md에서 완료 처리한다.
