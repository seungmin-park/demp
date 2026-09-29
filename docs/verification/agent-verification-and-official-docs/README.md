# Phase 16 · 에이전트 검증 경로와 공식 문서 동기화

2026-09-29. Backend와 frontend 모두 `refactor/agent-verification-and-official-docs` 브랜치에서 작업했다. Worktree는 각각 `.worktrees/agent-verification-and-official-docs/backend`, `.worktrees/agent-verification-and-official-docs/frontend`이다. 시작 기준 HEAD는 backend `220daa1`, frontend `78f6b95`였다.

작업별 커밋: T106 backend `ec3ac11`, frontend `ce221f3`; T107 backend `5f89ac7`, frontend `f96f838`. T108의 검증 기록과 완료 체크는 별도 커밋으로 남긴다.

## 요구사항 → 근거 → 변경

| 요구사항 | 관찰한 근거 | 변경 및 검증 |
| --- | --- | --- |
| 설치된 기술 버전에 맞는 공식 문서 | B `.tool-versions`, `build.gradle`, wrapper; F `.tool-versions`, `package.json`, `package-lock.json` | 양쪽 `docs/engineering/official-docs.{json,md}`에 정확한 버전·공식 URL·적용 범위·확인일을 기록. `check_agent_contracts.py`와 `check-agent-contracts.mjs`를 CI에 연결하고 임시 버전 `0.0.0` 불일치가 종료 1인지 확인. 문서 내용은 외부에서 변하므로 검사기는 로컬 버전 일치만 보장한다. |
| 작업할 기능·검증 명령을 찾을 수 있어야 함 | 반응 저장은 B Controller/Service/DB, F composable/API/화면으로 나뉨 | 양쪽 기능 지도·아키텍처 경계·로컬 `verify-*` 스킬과 `AGENTS.md`/README 진입점을 추가. |
| 책임 우회는 실제로 거절되어야 함 | Repository 직접 쓰기는 서비스의 잠금·집계 트랜잭션을 건너뜀. 컴포넌트의 API 직접 호출은 저장 중 상태 처리를 건너뜀 | B의 서비스 외 직접 `ContentReactionRepository.save/delete`, F의 컴포넌트 직접 `setReaction` import를 CI가 거절. 임시 위반 파일로 각각 종료 1을 확인. |

### 객체 책임과 데이터 흐름

```text
회원 클릭 → Vue 표현 → useContentReaction → reactions API
                                         ↓ HTTP + 인증
              DB/Repository ← ContentReactionService ← Controller
                  영속값          잠금·상태 전환·집계
```

화면은 클릭과 확정 전 표시를 관리하고, 서버 서비스는 회원별 반응과 집계를 하나의 트랜잭션으로 저장한다. 따라서 새로고침 뒤 수치는 서버 응답으로 다시 조립된다. 다른 클래스가 저장소를 직접 쓰면 집계나 멱등 규칙을 빠뜨릴 수 있어 정적 검사가 이를 막는다. 반대로 정적 검사는 리플렉션과 간접 호출을 알 수 없으므로 테스트와 실제 사용자 흐름이 필요하다.

## Red → Green → Refactor

이번 변경은 제품 동작 변경이 아니라 검증 경로와 문서 구축이다. 새로운 가드의 Red는 임시 위반 Java/Vue 파일과 버전 불일치 JSON을 투입했을 때 각 검사기가 종료 1인 것으로 확인했다. Green은 실제 현재 소스와 설치 버전에서 검사기 종료 0이었다. Refactor에서는 이름만 `ContentReactionService.java`인 파일까지 면제하던 조건을 실제 서비스의 상대 경로 하나로 좁혔다. 제품 코드의 기존 반응 동작은 자동 테스트와 실제 브라우저로 확인했다.

## 실행 증거

| 검증 | 명령/관찰 | 결과 |
| --- | --- | --- |
| B 로컬 계약 | `python3 scripts/check_agent_contracts.py`; `--probe-violation`; 임시 `--scan-root`와 `--docs-json` | 정상 종료 0; 임시 직접 저장과 버전 불일치 각각 종료 1 |
| B 전체 | Java 25로 `./gradlew test asciidoctor bootJar --console=plain` | 53 XML suite, **316 tests**, 실패·오류·스킵 0, 명령 종료 0; REST Docs HTML과 bootJar 생성 |
| F 로컬 계약 | `npm ci`; `npm run check:agent-contracts`; `--probe-violation`; 임시 `--scan-root`와 `--docs-json` | 정상 종료 0; 임시 직접 API import와 버전 불일치 각각 종료 1 |
| F 전체 | `npm test`; `npm run typecheck`; `npm run lint -- --no-fix`; `npm run build` | 40 files, **156 tests** 통과; 타입·lint·build 종료 0 |
| F headed E2E | `npm run test:e2e -- --headed` | **20 passed**, 종료 0. Playwright의 API fixture를 쓰는 화면 검증 |
| 실제 브라우저 | 아래 현재 cmux의 local Spring/H2 + Vite 경로 | 로그인→질문 추천/답변 비추천→새로고침, 질문 전환·취소→새로고침 DOM assertion 통과 |

기계 로그는 실행 환경의 `/tmp/demp-agent-{verify,backend,frontend-unit,e2e,npmci,start,spring,vite}.log`에 남았다. 자동 테스트의 최종 출력은 `AUTOMATED_CHECKS_EXIT=0`이었다. F 단위 테스트에는 기존 `QuestionMenu` RouterLink의 extraneous non-props attribute 경고가 보였지만 156 assertion은 통과했다. 이를 기능 결함이 없다는 증명으로 확대하지 않는다.

추가 doctor에서 Gradle 9.8.0, Node 24.21.0과 `npm ls --depth=0`의 프런트 선언 버전 일치를 확인했다. 로컬 기본 `./gradlew --version`은 JVM 21을 표시했으므로 Java 25를 `JAVA_HOME`으로 지정해 다시 실행했고 launcher 25.0.4.1을 확인했다. 새 문서 및 README/AGENTS의 로컬 Markdown 링크 27개는 모두 존재했다. 이전 Phase 문서의 형제 저장소 상대 링크 하나는 worktree 경로에서 끊어져 보이지만 원본 저장소 배치에서는 형제 경로를 가리킨다.

### 현재 cmux에서 보인 실제 사용자 흐름

호출 환경 `CMUX_WORKSPACE_ID=513D2093-E1D4-4C99-AF65-3881B6CC5499`, `CMUX_SURFACE_ID`는 비어 있었다. `cmux identify --json`으로 호출 터미널 `workspace:3/pane:4/surface:4`를 확인하고 오른쪽 보조 pane `workspace:3/pane:9/surface:10`에서 명령·로그를 표시했다. 동일 workspace의 브라우저 `surface:11`을 실제 클릭·입력·새로고침에 사용했다. 실행 후에도 보조 pane을 유지했다.

이 worktree로 만든 bootJar SHA-256은 `6e74a9e5043f7beed3e94e3f71cffc7dad259b35177a7fcb6f707ee7f977a40d`이고 local H2 서버는 `127.0.0.1:18081`, frontend Vite는 `127.0.0.1:5051`이었다. `local-member`로 로그인 후 `/questions/-1`에서 질문 추천과 답변 비추천을 클릭했다. 새로고침 뒤 질문 `추천 1 / aria-pressed=true`, 답변 `비추천 1 / aria-pressed=true`를 확인했다. 질문을 비추천으로 바꿨다가 같은 버튼으로 취소하고 재조회하니 질문 추천·비추천 모두 0/선택 해제, 답변 비추천 1/선택 유지였다. [실제 화면](assets/reactions-after-reload.png)을 저장했다. 이는 Playwright fixture 결과와 별개의 실제 Spring 저장 확인이다.

## 경계와 남은 위험

- 공식 링크 중 Spring Boot, Gradle, Vue 등의 가이드는 최신 문서로 이동할 수 있다. 표에 적힌 버전을 바꿀 때 릴리스 기록과 현재 페이지를 다시 확인해야 한다. CI는 외부 문서의 내용 변경을 감지하지 않는다.
- 저장소 쓰기와 컴포넌트 import 정적 검사는 명시적인 직접 경로만 탐지한다. 실제 권한, 동시성, 화면 상태는 기존 테스트와 실제 흐름으로 검증했다.
- 이번 Phase는 제품 반응 로직을 바꾸지 않았다. 테스트의 통과는 이 worktree 소스와 local/H2 및 테스트 fixture 조건에 한정된다.
