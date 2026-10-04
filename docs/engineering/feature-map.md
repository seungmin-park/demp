# 기능 지도

공용 CI 경로와 검증 책임은 [CI·보호된 전달](ci-and-delivery.md)을 따른다. `bash scripts/verify.sh`는 기존 소유권 검사, 실제 전체 테스트와 구조 검사, 필수 XML, 생성 문서·JAR·인증 HTTP 일치, 방금 만든 JAR의 Spring/H2 저장·재조회 경로를 연결한다. 프런트 실제 브라우저 경로는 별도로 검증한다.

| 기능 | 사용자 경로 | 저장 소유자 | 검증 |
|---|---|---|---|
| 질문·답변 반응 | 로그인 → 질문 상세 → 추천/비추천 → 새로고침 | `ContentReactionService` + DB | 아래 흐름, `./gradlew test --tests '*ContentReaction*'` 및 실제 cmux 브라우저 |

## 질문·답변 반응 저장

- 목적: 로그인한 회원이 질문/답변의 추천·비추천을 선택, 전환, 취소하고 새로고침해도 결과를 확인한다.
- 전제: local profile Spring/H2 서버, 프런트 dev server, 일반 회원 로그인. 공개 local fixture는 `local-member / password`다.
- 진입: `/questions/-1` 로컬 예제 → `추천`/`비추천` 버튼(접근성 `aria-label`, `aria-pressed`) → `PUT /api/question/{id}/reaction` 또는 `/api/answer/{id}/reaction` → 페이지 새로고침.
- 관찰: 선택 버튼 `aria-pressed=true`, 집계 1회 증감. 같은 요청은 멱등. `NONE` 취소 후 서버 재조회에서도 해제 상태.
- 저장: `ContentReactionController`는 인증 주체의 ID를 전달한다. `ContentReactionService`가 대상 행을 잠그고 이전/다음 상태 차이를 계산해 반응 행과 집계를 한 트랜잭션에 저장한다. Repository는 저장 경계. 프런트 `useContentReaction`은 미확정 응답·화면 이동 시 UI 상태를 다룬다.
- 실패: 미인증 401, 존재하지 않는 대상 404, 저장 실패 시 확정된 수치 보존/재시도, 화면 이동 뒤 늦은 응답 폐기.
- 검증: `.agents/skills/verify-demp/SKILL.md`의 정확한 명령과 cmux 표면 확인 후 실행. unit/MVC 테스트는 HTTP 계약과 DB 저장을 검증하고, 실제 브라우저는 별도 범위다. 과거 실제 실행 근거 `docs/verification/persistent-content-reactions/t99-t101.md`; Phase16 재실행 결과는 `docs/verification/agent-verification-and-official-docs/README.md`에 기록.
