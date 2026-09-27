# Curated publication implementation plan

> Execution: superpowers:executing-plans, inline; progress and acceptance in tasks.md T92–T96. User explicitly requested completion by existing task/check/commit workflow.

Goal: 운영자가 외부 공고를 확인하고 정리하여 공개·관리하는 DEMP의 남은 운영 흐름을 완성한다.
Architecture: HTTP 권한 → 운영 application service → 공고의 게시/모집/출처 상태 → 조회 경계. 게시 상태와 모집 날짜를 분리한다. Vue 입력/표시, API, URL 상태 경계를 유지한다.
Spec: tasks.md 조사 후속 목록과 사용자 외부 모아보기 결정. 기업 직접 제출/자동 크롤러/광고는 현재 도입하지 않으며 해당 조건부 항목은 구현 완료로 표기하지 않는다. 기존 명시적 builder 보류 유지.

## Global constraints
- DEMP 이름 유지. 사용자 제공 디자인·현재 Vue/TS, Java/Node 버전과 lockfile 유지.
- H2/local FileStorage로 검증, MySQL additive 수동 SQL 제공. 기존 레코드 보존. 운영 DB/S3 사용 없음.
- 각 동작은 실제 Red → 최소 Green → 책임 정리 → 전체 검증/커밋.
- cmux workspace:2. 기존 terminal12/browser13 종료를 확인하여 T96에서 terminal34/browser35 재생성, 크기 테스트 후 viewport reset. 다른 세션/사용자 입력 터미널 건드리지 않음.

## Tasks and shared contracts
1. T92: SecurityConfiguration POST /api/announce/add ADMIN; legacy /addAnnounce redirect /admin/announcements/new. Controller/docs fixtures 권한 갱신. 일반회원403/관리자200/익명401 실제 필터 테스트가 Red이고 Green 후 전체 B/F 검증.
2. T93: PublicationStatus DRAFT/REVIEW/PUBLISHED/HIDDEN, 신규 DRAFT, 기존 생성자/마이그레이션 데이터 PUBLISHED. Public list/detail/related에는 PUBLISHED만; admin은 전체. Actor는 인증 principal에서 전달. Source 원문(existing accessUrl), 선택 지원 URL, 출처명·식별값, 확인일, 저장/상태 변경 감사 기록. 초안/비공개 유출 테스트 Red, 상태 저장/재조회 Green. 관리자 폼/목록에 상태와 출처/이력 연결.
3. T94: RecruitmentAudience NEW/EXPERIENCED/ANY/MIXED 선택 명시; legacy null fallback 표시만. cohort/stipendAmount/stipendNote 교육 전용. 중복 키는 정규화 원문+기관+기수, unique key와 충돌409. 같은 제목 다른 기관/기수 허용, 같은 원문 재등록 충돌, 기존 충돌 데이터는 수동 점검 대상이고 자동 삭제 없음.
4. T95: 모집 종료 override는 게시 비공개와 다름. 공고 확인 메타정보와 별도 오류 제보 저장/관리자 처리 API/UI. 자동 외부 갱신 없음으로 운영자 수정이 덮이지 않음. 이미지 현행 파일 선택/기본 DEMP/로고 contain 정책 확정; 공유 파일 참조는 수명주기 관리 없는 재사용 금지.
5. T96/T91: 실제 서버 + headed mock E2E를 구분. 초안→검토→공개→수정→종료/비공개, 외부 URL·제보 처리, 회원 권한, 모바일/날짜/금액오류/URL복원/스크롤. 최종 fresh reviewer, 필수 전체 테스트. 브랜치 보존 후 main 통합/push (기존 사용자 승인).

## Review focus
- 공개 조회 모든 경로에서 비공개 자료가 새지 않는가.
- 인증 요청 본문이 작성자를 사칭하거나 상태/마감 규칙을 우회할 수 없는가.
- 동시 중복 생성과 실패 시 업로드 보상, 삭제 시 자식 자료 정리가 일관되는가.
- 기존 공고의 조회/이미지/연봉이 스키마 변경 후 유지되는가.
- 교육/채용 전환·미확인·경력 무관과 상한 없음이 섞이지 않는가.

공유 계약: T92 관리자 HTTP 경계를 T93–T95가 소비한다. T93 게시 상태는 T94 중복 판정과 T95 제보의 조회 허용에 적용한다. T94 교육 기수는 중복 키의 일부다. T95는 원문 확인·수정이 자동 작업에 의해 덮이지 않는 수동 운영으로 한정한다.
