# Phase 7: 런타임·프레임워크·TypeScript 전환

## 작업 환경 (2026-09-27)

- B 기준: main `b30e1c9`, F 기준: main `85a2e04`.
- 양쪽 브랜치: `refactor/runtime-framework-and-typescript-upgrade`.
- worktree: `demp/.worktrees/runtime-framework-and-typescript-upgrade/{backend,frontend}`.
- asdf 0.14.1 사용. 전역 설정은 유지하고 각 `.tool-versions`에서 Zulu/Node를 고정한다.
- 기존 Zulu JDK 배포판을 유지한다. Temurin을 전환 대상으로 사용하지 않는다.

## 버전 결정과 공식 근거

| 구성 | 기존 | 목표 | 호환성 근거 |
| --- | --- | --- | --- |
| Java | Zulu 11 | Zulu 25.36.205 / Java 25.0.4.1 LTS | asdf 제공 macOS ARM64 CA 빌드, [Azul 배포](https://www.azul.com/downloads/?version=java-25-lts) |
| Gradle | 7.4 | 9.8.0 | [Java 25 실행은 9.1 이상](https://docs.gradle.org/current/userguide/compatibility.html) |
| Spring Boot | 2.5.10 | 4.1.1 | [Java 17~26·Gradle 9 지원](https://docs.spring.io/spring-boot/system-requirements.html) |
| Node | 18.18.2 | 24.21.0 LTS | [26은 아직 Current, 24는 LTS](https://nodejs.org/en/about/previous-releases) |
| Vue | 3.2.36 | 3.5.43 stable | [고정 LTS 주기 없음](https://vuejs.org/about/releases), npm latest 실조회 |
| 빌드·단위 테스트 | Vue CLI 5 / Jest 27 | Vite 8.3.1 / Vitest 5.0.2 | npm engines/peers 확인: Node24·Vite8 공통 지원 |
| TypeScript | 없음 | 6.0.3 / vue-tsc 3.3.11 | typescript-eslint 8.70.1 peer <6.1.0; strict typecheck 통과 |

Spring의 마지막 minor 장기 지원은 유료 Enterprise 지원이다. Boot 3.5는 OSS 지원 종료가 공지됐으므로 최신 OSS 안정 4.1.1을 사용한다. 이를 LTS라고 부르지 않는다. [Spring 지원 정책](https://spring.io/support-policy), [3.5 최종 OSS 공지](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).

```text
T60 기존 계약·실행 기준선
  → T61 Gradle/JDK → Boot/Jakarta/Security/JPA/직렬화 → 전체 회귀
  → T62 Node → Vue/Vite/Vitest → 브라우저 계약
  → T63 API DTO → 인증/라우트/비동기 상태 → Vue props/emits → CI 타입 검사
```

중간 2.7/3.x/4.x 마이그레이션 가이드의 변경점을 단계별 적용하되 중간 버전을 배포하지 않는다. API URL·JSON 필드·권한·저장 데이터 계약을 유지한다. Hibernate의 implicit sequence/table/enum 변경과 Jackson 날짜·오류 JSON 변경을 특별히 검사한다. 운영 DB는 연결하지 않고 임시 DB로 검증한다.

## 인프런 참고 범위

접근 가능한 강의 목록 89개 전체를 확인했다(다음 페이지 없음). Java·Spring·JPA/Querydsl·테스트·리팩터링·Vue·TypeScript·배포 관련 강의를 분류하고 직접 관련된 강의 내용을 읽었다. 수강 진도·접속 시각·개인 전체 수강 목록은 커밋하지 않는다.

- 호돌맨, [스프링부트 3.x 버젼 업데이트](https://www.inflearn.com/courses/lecture?courseId=328753&unitId=155900): JDK·Gradle·Jakarta·REST Docs가 함께 움직이는 전환 경계를 참고했다. 강의의 과거 버전과 자동 요약의 부정확한 패키지/H2 옵션은 복사하지 않고 공식 API와 실행 결과로 확인한다.
- 캡틴판교, [스토어 상태 관리에 대한 주의 사항](https://www.inflearn.com/courses/lecture?courseId=326531&unitId=68060): strict 타입 검사를 켜고 API 데이터 계약부터 점진적으로 옮긴다. 페이지 전용 상태는 컴포넌트/props 경계에 두며 전역 인증 상태만 store가 맡는다.
- 김영한의 Spring/JPA/Querydsl 강의, 테스트·가독성 강의 등은 목록·검색으로 관련성을 확인한 범위다. 모든 강의를 완강/본문 검토했다고 주장하지 않는다.

## T60 기준선

- B Java11: `./gradlew clean test asciidoctor bootJar --console=plain` 성공. 33 suites·195 tests, failures/errors/skipped 모두 0.
- F Node18: `npm ci` 1505 packages, 단위 17 suite·45 test 통과, lint·build 종료 0.
- F 최초 unit 실행은 샌드박스의 `listen EPERM`으로 1 test 실패. 권한 있는 재실행에서 전체 통과. 기능 Red가 아니다.
- 현재 cmux `workspace:2`, 호출 `surface:3`, 전용 터미널 `surface:7`, 실제 앱 브라우저 `surface:9` (이전 surface:8 종료).
- `CI=1 npx playwright test --headed --workers=1 --reporter=line`: 4 passed. Playwright는 자체 Chromium과 API fixture를 사용한다.
- 같은 pane에서 Spring Java11 임시 메모리 H2와 Vue proxy 실행. `python3 scripts/verify_local_flow.py`: 실제 API 14 요청, 403/401 및 저장 후 재조회 assertion 통과, exit 0.
- cmux 브라우저는 실제 Spring 연결 화면이다. Playwright fixture 화면과 구분한다. 초기 서버 준비 전 연결 거절은 준비 완료 후 재이동한다.
- 로그: `/tmp/demp-t60-{backend,unit,lint,build,e2e,spring,vue}.log`; B 보고서 `build/reports/tests/test`, `build/docs/asciidoc`.

## 실행 원장

T60 기준선은 문서·환경 설정 변경이며 기능 Red/Green으로 주장하지 않는다. 이후 작업 결과는 아래 T61~T63 기록을 따른다.

- 실제 cmux 가입 POST에서 초기 403: Origin `127.0.0.1:5050`과 기본 CORS `localhost:5050` 불일치. 실행 환경 `APP_CORS_ALLOWED_ORIGINS=http://127.0.0.1:5050` 설정 후 가입→로그인 화면 확인.
- asdf Java 구형 plugin의 Zulu 압축 구조 대응: 설치 루트에서 `Contents/Home`의 각 자식으로 symlink를 연결하고 `asdf reshim java zulu-25.36.205`; `asdf exec java -version`에서 25.0.4.1 LTS 확인. 전역 선택 버전 불변.

## T61 Java·Spring 전환 검증

### 실패 원인 → 변경

1. javax 제거, Security adapter 제거, REST Docs API 이동은 소스 호환성 오류였다. 이를 기능 Red로 계산하지 않았다. Jakarta·SecurityFilterChain·Jackson 3·새 test slice/MockitoBean·query/form/multipart 문서 API로 옮겼다. Boot 4에서 제거된 `@MockBean`은 Spring Framework `@MockitoBean`으로 대체하되 MockMvc의 Spring MVC 구성을 유지한다.
2. **실제 Red**: `./gradlew test --tests '*LegacySchemaCompatibilityTest' --write-locks`에서 `announcement_seq`가 없는 기존 Hibernate 5 schema에 컨텍스트 실패 assertion. 공유 `hibernate_sequence`를 명시하고 enum을 VARCHAR로 고정해 schema 변경 없이 Green. fixture는 기존 main의 Hibernate 5 DDL이다.
3. **실제 Red**: 공고 Repository 39개가 H2 2.4.240의 `CHECK constraint invalid / database has been closed`로 실패. [H2 #4342](https://github.com/h2database/h2database/issues/4342) 및 [#4302 수정 릴리스](https://github.com/h2database/h2database/releases)를 확인해 최신 2.5.252로 고정했다. 체크 제약을 유지한 채 대상 40개 통과.
4. Hibernate 7/H2의 페이지 SQL은 ANSI `fetch first`를 사용한다. SQL 형태 assertion만 해당 문법으로 이식했고 내용·정렬·hasNext assertion은 유지했다. 제거된 MySQL57Dialect를 local/test H2Dialect로 교체했다.
5. 32 UTF-8 바이트 이상의 같은 키에서 이전 JJWT의 raw UTF-8 HS256·subject/roles/iat/exp 계약을 독립 JCA 서명으로 검증했다. 신규 테스트의 첫 404는 잘못 입력한 테스트 URL이 원인이므로 기능 Red가 아니다. 실제 `/detail/{id}` 경로에서 인증 성공을 확인했다.
6. AWS SDK 2의 저장 요청에서 bucket/key/content-type/length/ACL 및 전송 바이트를 검증했다. 실제 S3는 호출하지 않았다.

### 책임·이름 검토

- OpenApiConfig는 OpenAPI 구성만 맡는다(이전 SwaggerConfig 이름 갱신). SecurityConfiguration은 HTTP 보안 경계와 CORS만 구성한다.
- JWT 발급/검증과 FileStorage 구현의 외부 경계를 유지했다. Controller/Service/Repository의 HTTP 계약과 트랜잭션 책임은 바꾸지 않았다.
- 네 엔티티의 ID generator 이름은 각 매핑에서 유일하며 기존 공유 sequence를 가리킨다. enum의 문자열 DB 계약을 명시했다. 외부 JSON 키·URL·파일 저장 키는 보존했다.
- Gradle은 Java toolchain 25, 표준 annotation processor, native BOM, lockfile을 사용한다. 사용되지 않는 thymeleaf 및 구형 Querydsl Gradle plugin/Springfox/AWS1를 제거했다.

### 검증 범위

- Zulu25 `clean test asciidoctor bootJar` 34 suites / **197 tests**, failure/error/skipped 0. lockfile 갱신 후 `--write-locks` 없는 재실행 성공.
- 기본 production 설정을 격리된 H2로 시작→저장→종료→재시작한 테스트에서 데이터 보존, seed 없음 확인. 실제 운영 MySQL에는 연결하지 않았다.
- REST Docs HTML 존재, 미해결 snippet·내부 테스트 키 없음. OpenAPI `/v3/api-docs` 실제 응답에서 `DEMP API` 확인.
- cmux workspace:2 terminal surface:7에서 Java25 JAR + 기존 Node18/Vue 클라이언트 실행. 실제 API 14요청 및 403/401/저장 후 재조회 assertion 성공(exit 0). 브라우저 surface:9에서 회원가입→로그인→공고 목록 확인. 기존 클라이언트가 새 서버와 동작한다.
- 최종 로그 `/tmp/demp-t61-final.log`, 경계 검증 `/tmp/demp-t61-boundaries.log`, 서버 `/tmp/demp-t61-spring.log`, 브라우저 앱 서버 `/tmp/demp-t61-vue.log`.
- Gradle 10 예정 deprecation 및 JVM native access/Mockito attach 경고는 현재 실행 실패가 아니다. Windows wrapper는 생성 파일을 갱신했지만 Windows 실행은 검증하지 않았다.
- 배포/rollback 및 H2 1.4 파일 이관 주의는 루트 README에 기록했다. Java 25 CI 설정은 갱신했으며 원격 Actions 실행은 아직 수행하지 않았다.


## T62 Node/Vue 및 인피니티 스크롤

- Node24.21.0/Vue3.5.43, Router5.3.1, Vite8.3.1/Vitest5.0.2, jsdom30.1.1, ESLint10.11 조합. registry engines/peers 대조 후 설치했다. 최신 TypeScript7은 현재 typescript-eslint peer 범위(<6.1) 밖이므로 T63에서 호환되는 안정 6.0.3을 우선 사용한다.
- 기존 unit45 assertion 보존. Vitest cleanup이 드러낸 불완전 emitter mock(off 없음), 새 Test Utils setData의 File 병합, Vite 동적 import를 기다리는 라우터 테스트 차이를 수정했다. 파일 선택은 실제 input change 이벤트로 보강했다. 기능 Red로 주장하지 않는다.
- **스크롤 Red**: 신규 3개 테스트에서 끝 감지 후 API 호출이 1회에 머물러 expected2 실패, 필터 변경 expected3/actual2, 실패 안내 미표시. IntersectionObserver 연결·중복 ID 제거·요청 세대 유지·observer 해제로 전체 48개 Green.
- headed Playwright7개를 cmux terminal surface:7에서 실행해 통과(exit0). 기존4개와 실제 스크롤3개(18개 카드/페이지0→1→2, 실패페이지1 재시도, 필터 변경 중 늦은 페이지 폐기).
- 최초 E2E fixture `**/api/**`는 Vite `/src/api/auth.js`도 가로챘다. 정확한 origin `/api/**`로 제한해 해결했다. 앱 소스 404로 빈 화면이 된 테스트 인프라 문제이며 기능 Red가 아니다.
- 사용자가 외부 Chromium 창 대신 cmux pane을 요청했다. 이후 실제 UI 조작은 내장 브라우저로 진행하며 러너 자동 assertion 로그와 실제 Spring 연결 검증을 구분한다. 기존 보조 pane이 닫혀 새 terminal surface:12/pane:9 및 browser surface:13/pane:10을 workspace:2에 만들었다. 두 pane을 동시에 표시하고 호출 surface:3 포커스를 유지한다.
- 국내 서비스·기업 채용 페이지 조사: [디자인 판단 기록](../../design/redesigned-discovery.md). 잡코리아 본문은 Firecrawl proxy 오류로 2회 실패하여 구체적 UI를 확인했다고 기록하지 않는다.

T62 실제 cmux 검증(새 surface:13): 로컬 예제 계정 로그인 후 목록 끝 이동으로 **8→16→24→28개**, 각 단계 중복 제목/ID fixture 없음, 마지막 안내 true. API는 fixture route가 아니라 Java25/Spring4 임시 H2에서 반환했다. `scroll --dy`는 이 WKWebView에서 window.scrollY를 바꾸지 않아 공식 `scroll-into-view` 명령으로 목록 끝을 이동했으며 해당 교차 이벤트가 다음 요청을 발생시켰다. 이전 임시 서버의 만료 세션은 로그인으로 이동하고 clear 처리됐다. 내장 브라우저는 네트워크 mock을 지원하지 않으므로 503/응답 지연 자동 assertion은 Playwright 7개 통과 결과로 따로 기록한다.

설치 재현: `npm ci` 333 packages, 최종 unit17 suite/48 test 통과, lint/build exit0. 남은 `vuex-persistedstate` deprecated 경고와 그 하위 `shvl` 경고는 T63의 직접 타입 있는 인증 저장으로 제거한다.

실제 교육 필터는 기존28개를 초기화한 뒤 교육8→10개만 표시하고 마지막에서 멈췄다. cmux 실제 에디터 입력→질문 저장→상세 별도 조회로 본문 보존, Enter 태그 입력과 Vue 태그 보존을 확인했다. 이름·책임 검토: IntersectionObserver는 화면 가시성만 감지, API 모듈은 기존 검색 params와 8개 페이지 계약 유지, 목록 컴포넌트는 페이지/요청 세대/재시도 상태만 조정한다. 서버 전체 검색 계약을 유지하고 중복 ID만 제거한다. Form을 ValidationForm으로 명확히 구분하고 template을 표준 tbody/slot 문법으로 정리했다.

## T63 TypeScript와 인증 저장 경계

- TypeScript6.0.3/vue-tsc3.3.11/typescript-eslint8.70.1을 고정하고 strict `vue-tsc --noEmit`을 CI 필수 단계로 추가했다. 최신 TS7은 현재 lint parser 지원 범위 밖이므로 사용하지 않았다. Vuex4 package exports의 타입 경로 누락은 패키지의 공식 선언 파일에 paths를 연결했다.
- 모든 제품 JS와 Vue script를 TS로 옮겼다. API DTO → store/root state → router/URL → typed mitt 이벤트 → Vue 상태·props/emits·템플릿을 검사한다. 제품 `any`, ts-ignore/nocheck 없음. 기존 tests/config/server.cjs는 JS 유지. 외부 선언 충돌만 skipLibCheck하며 제품 strict는 켜져 있다. Summernote CDN에는 사용하는 code/options 오버로드만 임시 선언했다.
- **런타임 Red 2개**: 기존 저장소가 username:number/token:object를 그대로 인증 상태로 복원했다. 저장 quota 예외는 commit 호출까지 전파됐다(`/tmp/demp-t63-persistence-red.log`). 저장 JSON을 unknown으로 읽어 문자열을 확인하고, 읽기/쓰기 거부 시 메모리 상태를 유지하도록 최소 plugin으로 바꿨다. 기존 vuex 키/모양은 동일. 기존 형식 복원·잘못된 타입·깨진 JSON·로그아웃 저장·저장 거부 5개 Green. deprecated vuex-persistedstate 및 shvl 제거.
- **런타임 Red 1개**: company:null 공고의 관련 목록 렌더링이 TypeError로 중단돼 제목 assertion 실패(`/tmp/demp-t63-null-company-red.log`). nullable 회사 이름을 빈 문자열로 표시해 전체 카드가 유지된다.
- **타입 검사 실패 증거**: API login 반환 JWT에 number를 넣었을 때 TS2322 발생(`/tmp/demp-t63-contract-red.log`). 최종 type contract는 ts-expect-error로 해당 잘못된 값이 계속 거절되는지 검사한다. 이는 런타임 기능 Red와 구분한다. 초기 SFC 미선언 상태/라우트 배열/잘못된 key 등의 컴파일 오류도 타입 이관 오류로 기록한다.
- Refactor/책임: persistAuthentication은 저장 경계만, Login 모듈은 인증 상태만, Axios는 HTTP/401 처리만 담당한다. API 이름은 기존 공개 유스케이스를 보존했다. routeId/queryText는 Router5의 단일·배열 값을 경계에서 정리한다. JSX가 아닌 Vue template에서 this를 제거하고 직무·태그 문자열과 서버 answerId를 key로 사용했다. props/event와 렌더링·상태·API 경계를 기준으로 검토했고 Vue 컴포넌트 자체를 SOLID 점수화하지 않았다. TS 모듈은 서로 좁은 DTO·이벤트 계약만 공유하며 단순 위임 클래스를 추가하지 않았다.
- fixture 대조: MemberDto(id/username), QuestionAnswer(answerId), AnnouncementDetailResponse(payment/nullable fields), Slice(number/last)를 서버 DTO와 대조해 자동 E2E 대역도 보완했다. 실제 서버 API 14개 assertion은 cmux 보조 pane에서 별도로 통과했다.
- 실제 cmux 내장 브라우저: 로그아웃→local-member 로그인→새로고침에도 로그인 유지, 스크롤8→16개(중복 없음), 교육 전환 시 기존 채용 제거 후 교육8개. 토큰 값은 로그에 출력하지 않았다. 러너의 네트워크 mock/503/지연은 내장 브라우저에서 지원하지 않아 Playwright 회귀 결과와 구분한다.
- 문서: 프런트 README에 설치·타입 도구 peer 선택·남은 JS/any·인증 저장 형식·롤백을 기록했다. API/DB 변경 없음. 원격 CI/운영 배포는 실행하지 않았다.

T63 최종 게이트: `npm ci`354개 → unit19 suites/54 tests → typecheck/lint/build exit0. 마지막 Playwright는 **7 passed (4.6s)**, 러너 pipeline exit0 후 서버 유지. 직전 재설치 직후 실행은 5개 navigation/module-load timeout, 2개 pass로 실패했다. 같은 코드·assertion·30초 제한을 유지한 재시작에서 전체 통과했다. CLI 조회까지 약46초 걸렸던 시점과 겹치나 지연의 근본 원인은 확정하지 않는다. 실패 trace는 `/tmp/demp-t63-cold-start-failure/test-results/`, 원본 로그는 같은 디렉터리 e2e.log에 보존했다. 시간 제한 증가·assertion 완화 없음. 최종 로그 `/tmp/demp-t63-final-{unit,typecheck,lint,build}.log`, `/tmp/demp-t63-{ci,e2e,spring,vue}.log`.

## Phase 7 전체 리뷰와 수정

executing-plans 지시에 따라 새 컨텍스트 reviewer가 B b30e1c9..fe0d0ff, F85a2e04..17354f5를 읽었다. Critical 없음, Important3개와 nullable DTO Minor1개. Important를 한 차례 TDD로 수정했다. nullable DTO는 T63의 명시적 API/nullable 타입 요구를 충족하지 못하므로 Important로 재분류해 같은 수정에 포함했다.

- 없는 URL: Spring7 NoResourceFoundException이 기존 catch-all에 잡혀500. 실제 MockMvc `/api/member/missing/path` expected404/actual500 Red → framework missing-resource/handler 예외를 공통404로 변환. 본문 `errorCode/errorMessage/instance`도 검증.
- 신규 비밀번호: ASCII73자·한글25자의 registerMember가 IllegalArgumentException으로 실패하여 ApiException400 기대와 불일치(Red2). 서버는 UTF-8바이트 길이를 입력 경계에서 검사하고 DB 저장 없이400. UI도 초과 입력을 안내하며 전송하지 않음(Red2→Green). 한글24자=72바이트 가입·로그인 통과. 이전 BCrypt의 첫72바이트 해시를 가진73자 비밀번호는 로그인 계속 통과하며 로그인 제한/절단을 추가하지 않았다.
- JWT 최소 키: 짧은 키의 시작 오류가 설정 이름을 안내하지 않는 assertion Red → JWT_SECRET과32 UTF-8바이트 요구만 출력하는 명확한 설정 오류.32바이트 한글/ASCII 혼합 키 발급·검증 Green. 보안 제한을 완화하지 않았으며 동일키 호환성 전제·회전 시 강제 재로그인·롤백 영향을 README에 명시했다.
- nullable 질문제목/질문·답변 본문: null DTO fixture의 TS2322를 확인하고 응답타입과 SafeHtml 입력을 정리했다. 실제 API 값의 타입을 좁게 가정하지 않는다.
- 리뷰 범위 밖 판단: Phase8/9는 승인된 후속 작업. 운영 MySQL/S3·원격 Actions/Windows/전체 브라우저 버전은 실행 증거 범위 밖이며 배포 전에 별도 검증이 필요하다. E2E 지연 근본 원인은 미확정으로 유지한다. 기존 상세 응답 경합·일부 작성 오류 안내는 Phase8 T70/T72에 포함해 다룬다. 기존 긴 BCrypt 로그인은 코드·신규 실제 서비스 테스트로 호환 확인하여 오류로 판단하지 않는다.
- Red로그 `/tmp/demp-phase7-review-{red,jwt-red,password-ui-red,null-types-red}.log`, 대상Green `/tmp/demp-phase7-review-{green,password-ui-green,null-types-green}.log`.

리뷰 수정 최종 게이트: B35 suites/**204 tests** failures/errors/skips0, `clean test asciidoctor bootJar` exit0. F20 suites/**57 tests**, strict typecheck/lint/build exit0. 현재 cmux terminal surface12에서 실제 Spring/H2 **16요청**(새404/긴비밀번호400 포함) assertion 통과, Playwright **7 passed(4.6s)**, 각각exit0. 범위 외 접근 없음. 남겨둔 Minor 없음(타입 계약은 상향하여 수정).
