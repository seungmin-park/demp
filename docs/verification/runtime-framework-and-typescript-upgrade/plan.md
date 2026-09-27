# Phase 7 실행 계획

Spec: ../../../../spec.md 및 ../../../../tasks.md Phase 7. 공통 제약은 루트 AGENTS.md.

## Global Constraints
기존 HTTP/DB 계약 보존, 사용자 데이터 접근 금지, main 유지, 각 T마다 체크·커밋. 새 기능/오류 수정은 실패 assertion을 먼저 관찰한다. 라이브러리 소스 호환 오류는 기능 Red로 보고하지 않는다. 기존 테스트를 먼저 실행하고 그대로 회귀 게이트로 사용한다. E2E는 현재 cmux 보조 pane에서 headed runner와 실제 브라우저를 구분하여 실행한다.

## Task 60: 기준선과 목표 조합
기존 전체 B/F 검증, 공식 지원표와 인프런 목록 검토, asdf local 고정.
Expected: B 195 test, F 45 test, E2E 4 test 통과; 실제 API 흐름 통과. 문서 링크·diff 정합성 확인 후 커밋.
Produces: README 버전표, .tool-versions.

## Task 61: Java/Spring 생태계
Gradle9 wrapper/JDK25, Boot4/Jakarta/SecurityFilterChain/Jackson3, SDK2, JWT0.13, 유지보수되는 Querydsl, OpenAPI로 전환. 의존성 lock 갱신. 기존 테스트 assertion 유지. 직렬화·ID/enum/LOB·날짜·권한·외부 파일 경계를 검증.
Expected: clean test asciidoctor bootJar 종료0, production 설정 임시 DB start/stop 및 현재 cmux 실제 API 통과. 검증 기록·체크·커밋.
Consumes: T60 런타임. Produces: 보존된 API 계약과 JDK25 산출물.

## Task 62: Node/Vue 도구
Node24/Vue stable, Vite/Vitest, 불필요한 legacy 의존성 정리, lock/CI 갱신. 기존 테스트를 동등한 assertion으로 이식.
Expected: npm ci, 전체 unit, lint, build, headed Playwright 및 cmux 실제 사용자 흐름 통과. 기록·체크·커밋.
Consumes: T60 런타임 및 T61 API. Produces: TypeScript 지원 가능한 도구.

## Task 63: TypeScript
strict tsconfig/vue-tsc/CI. API 경계 DTO부터 store/router/비동기 상태, Vue props/emits 순서로 전환. 타입 오류 실제 실패→수정→typecheck 통과. 런타임 행동 회귀 검증.
Expected: 전체 unit/typecheck/lint/build/E2E 통과. API fixture 대조, 배포·rollback 문서, 남은 JS/any 명시. 기록·체크·커밋.
Consumes: T62 도구/T61 API. Produces: 검사되는 프런트 타입 계약.

## Review Focus
저장 데이터 schema/sequence 보존, 기존 JWT 검증 호환성, 400/401/403/404 일관성, nullable DTO, E2E fixture와 실제 API 간 차이, 직접 의존성/CI 런타임 호환성.
