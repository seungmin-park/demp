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
| TypeScript | 없음 | 호환되는 최신 stable + vue-tsc | 설치 시 peer 범위와 실제 typecheck로 확인 |

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

T60 기준선 완료. 문서·환경 설정 변경이며 기능 Red/Green으로 주장하지 않는다. T61~T63은 미완료다.

- 실제 cmux 가입 POST에서 초기 403: Origin `127.0.0.1:5050`과 기본 CORS `localhost:5050` 불일치. 실행 환경 `APP_CORS_ALLOWED_ORIGINS=http://127.0.0.1:5050` 설정 후 가입→로그인 화면 확인.
- asdf Java 구형 plugin의 Zulu 압축 구조 대응: 설치 루트에서 `Contents/Home`의 각 자식으로 symlink를 연결하고 `asdf reshim java zulu-25.36.205`; `asdf exec java -version`에서 25.0.4.1 LTS 확인. 전역 선택 버전 불변.

## T61 Java·Spring 전환 검증

### 실패 원인 → 변경

1. javax 제거, Security adapter 제거, REST Docs API 이동은 소스 호환성 오류였다. 이를 기능 Red로 계산하지 않았다. Jakarta·SecurityFilterChain·Jackson 3·새 test slice/MockitoBean·query/form/multipart 문서 API로 옮겼다. Boot 4에서 제거된 `@MockBean`은 Spring Framework `@MockitoBean`으로 대체하되 MockMvc의 Spring MVC 구성을 유지한다.
2. **실제 Red**: `./gradlew test --tests '*LegacySchemaCompatibilityTest' --write-locks`에서 `announcement_seq`가 없는 기존 Hibernate 5 schema에 컨텍스트 실패 assertion. 공유 `hibernate_sequence`를 명시하고 enum을 VARCHAR로 고정해 schema 변경 없이 Green. fixture는 기존 main의 Hibernate 5 DDL이다.
3. **실제 Red**: 공고 Repository 39개가 H2 2.4.240의 `CHECK constraint invalid / database has been closed`로 실패. [H2 #4342](https://github.com/h2database/h2database/issues/4342) 및 [#4302 수정 릴리스](https://github.com/h2database/h2database/releases)를 확인해 최신 2.5.252로 고정했다. 체크 제약을 유지한 채 대상 40개 통과.
4. Hibernate 7/H2의 페이지 SQL은 ANSI `fetch first`를 사용한다. SQL 형태 assertion만 해당 문법으로 이식했고 내용·정렬·hasNext assertion은 유지했다. 제거된 MySQL57Dialect를 local/test H2Dialect로 교체했다.
5. 이전 JJWT와 같은 raw UTF-8 HS256 키·subject/roles/iat/exp 계약을 독립 JCA 서명으로 검증했다. 신규 테스트의 첫 404는 잘못 입력한 테스트 URL이 원인이므로 기능 Red가 아니다. 실제 `/detail/{id}` 경로에서 인증 성공을 확인했다.
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
