# Phase 8 실행 계획

Spec: tasks.md Phase 8, docs/design/redesigned-discovery.md, 사용자 ZIP 및 작성기/아이콘 교체 지시.

## Global Constraints
앱 이름 DEMP 유지. ZIP 색상·타이포·공통 구조를 기존의 검증된 API/인증/스크롤 위에 이식. 각 작업 실제 assertion Red→Green과 전체 unit/typecheck/lint/build, task 체크·commit. 로컬 사용자 흐름은 현재 cmux workspace 보조 terminal+내장browser. 기존 임시 테스트 데이터와 운영 데이터 구분. 서버 전체 검색, nullable 값, 응답 역전, HTML 정화 계약 유지.

## Task 70: 공통 디자인과 작성기
ZIP route/style inventory와 누락 상태를 기록한다. 공통 CSS 토큰/버튼/입력/상태/헤더, 자체 SVG 아이콘/회원 아바타, 모든 기존 화면 템플릿을 이식한다. 질문·답변·공고에 공통 MarkdownEditor를 적용한다. source↔HTML 변환은 별도 TS 모듈, 출력은 SafeHtml; 서버의 제목/표 등 안전한 서식 whitelist와 동일하게 맞춘다. 기존 HTML(밑줄 포함)의 변환 왕복을 단위 검증하고 실제 수정 UI는 T81에서 연결한다. toolbar cursor·preview·XSS·실패 재시도·중복 제출을 먼저 검사한다. 기존 Summernote/CDN과 미사용 Bootstrap 경계를 제거한다.
Expected: component API regression, Markdown rendering/roundtrip/toolbars/security RED→GREEN; B sanitizer+전체 tests; F unit/typecheck/lint/build; cmux desktop/mobile actual writing and submission; checkbox/commit.
Consumes: Phase7 API/types/auth/scroll. Produces: 공통 디자인 토큰·아이콘·안전한 MarkdownEditor.

## Task 71: 채용/교육 검색
공식 사례 비교표와 실제 DB 필드를 대조한다. 채용/교육 모드 분리, 직무/기술/경력/급여/모집상태와 교육 고유 정보·조건을 실제 저장/조회 계약으로 설계한다. 풍부한 카드/상세 요약과 지원 링크. 필터 칩/초기화/mobile panel, URL 저장/복원/뒤로가기, 서버 전체 검색.
Expected: B 전체 조건/페이지/정렬 tests; F URL/경합/빈결과/retry/scroll tests; 타입/REST Docs 계약 일치; 전체 gates; checkbox/commit.
Consumes: 공통 디자인, 기존 전체조회. Produces: 검증되는 탐색 flow.

## Task 72: 누락 상태와 인수
404/403/로딩/빈결과/서버오류 재시도, 접근성/반응형을 점검한다. 기존 E2E fixture를 새로운 편집기/화면에 맞추고 흐름 assertion 유지. URL restore/search/mobile regression 추가. 현재 cmux 실제 API 연결로 주요 페이지 desktop/mobile 캡처와 작성/필터/스크롤 검증.
Expected: 양쪽 전체 tests, typecheck/lint/build/REST Docs, 자동 E2E+실제cmux 사용자흐름, 하나의 전체 branch review. 체크/commit.

## Review Focus
클라이언트·서버 HTML whitelist와 roundtrip, markdown raw HTML/XSS, 기존 API/로그인/쓰기 회귀, 필터 URL 다중값과 잘못된 값, 전체 DB 검색·중복 페이지·늦은 응답, nullable 레거시 교육정보, 실제 지원 URL 검증, 모바일 overflow/focus/labels.
