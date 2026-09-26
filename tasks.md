# DEMP 단계별 실행 계획

> **For agentic workers:** superpowers:executing-plans를 사용해 한 작업씩 실행한다. 체크하지 않은 항목은 아직 수행하지 않은 계획이다.

**Goal:** spec.md S01~S13을 테스트와 파일별 검토 기록으로 보장한다.

**Architecture:** 서버는 HTTP → 서비스 트랜잭션/권한 → 도메인/저장소, 화면은 Component → 상태 → API 경계를 따른다.

**Tech Stack:** Java 11, Spring Boot 2.5.10, JPA/QueryDSL, Vue 3, Vuex 4, Vue CLI 5. 프런트 테스트는 기존 Vue CLI와 결합 가능한 Jest/Vue Test Utils 기반을 우선 선택한다.

**Spec:** [spec.md](spec.md). 판단 근거: [plan.md](plan.md).

## 실행 규칙과 경로

B=`demp`, F=`../dempfrontend`가 원본 저장소다. Phase 0은 사용자 요청에 따라 원본 저장소에서 checkout하여 리뷰할 수 있도록 미커밋 변경으로 옮겼고 별도 worktree는 제거했다. 아래 B Java 경로는 `src/main/java/com/inhatc/demp/`, 테스트 경로는 `src/test/java/com/inhatc/demp/` 아래다. F 경로는 저장소 루트 기준이다. 생성/수정 표시를 구분한다. 계획의 메서드명은 새로 도입할 계약이며 이미 존재한다고 가정하지 않는다.

매 작업은 작은 시나리오 하나씩 반복한다:

```text
관찰 가능한 기대 결과 작성
 → 대상 테스트 실행, 기대 assertion 실패 확인
 → 필요한 최소 구현
 → 대상 테스트 + 전체 테스트
 → 책임·이름·중복 정리
 → 전체 테스트 재실행
 → 변경/검증 로그 (커밋은 명시적 요청이 있을 때만)
```

서버 전체 명령은 B에서 `./gradlew test`, 대상은 `./gradlew test --tests 'com.inhatc.demp.<package>.<Class>'`다. 프런트 표준 명령은 Phase 0에서 도입할 F의 `npm test -- --runInBand`다. 각 프런트 작업은 전체 테스트와 `npm run lint -- --no-fix`, `npm run build`를 수행한다. 아래 assertion 예시는 새 테스트에 넣을 핵심이며, setup/fixture는 해당 계층 규칙에 맞춰 구현한다.

테스트 계층별 경계와 한글 `@DisplayName` 규칙은 `AGENTS.md`를 따른다. Controller와 REST Docs는 Spring MVC 컨텍스트를 사용하고 Service 및 직접 의존하는 하위 계층을 mock으로 교체한다.

T10 전에는 전체 컨텍스트 테스트에서 InitDb를 제외하거나 mock으로 교체하지 않는다. 테스트가 생성한 데이터만 cleanup하고 초기 회원·질문·답변·태그를 보존한다. InitDb 삭제 시 seed 관련 기대값도 함께 변경한다.

추가 요구사항: 모든 테스트의 데이터 setup용 `@BeforeEach`를 제거하고 필요한 객체는 각 테스트 본문에서 직접 생성한다. 공통 데이터 필드·상위 setup·자동 data.sql 로딩으로 대체하지 않는다. MockMvc와 REST Docs 실행 도구 초기화만 데이터 생성과 구분해 유지할 수 있고 `@AfterEach` cleanup은 유지한다. API 변경 작업은 Controller 테스트와 별도로 T02의 REST Docs 테스트도 함께 작성·갱신한다.

상시 검토: 모든 T 작업에서 변경 파일과 호출부의 클래스·메서드·필드·매개변수 이름, 이름과 실제 효과, 메서드의 소속 클래스와 추출 필요성을 [AGENTS.md의 이름과 책임의 상시 검토](AGENTS.md#이름과-책임의-상시-검토)에 따라 기록한다. 백엔드와 독립 TypeScript 객체에는 SOLID를, Vue 컴포넌트에는 props/event·렌더링·상태·API 경계를 적용한다. 단순 이름/구조 변경은 기존 공개 동작을 먼저 특성화하고 Green을 유지한다. 동작을 고칠 때는 별도 Red→Green 사이클을 만든다.

## Phase 0 · 실행·검증 기반 복원

### T00 · 컴파일 복원 게이트 — R01 / S01

수정: B `controller/AnnouncementController.java`, `InitDb.java`. 필요 시 컴파일러가 지목한 공고 query/fixture 호출부. 생성: B `controller/AnnouncementControllerTest.java`(테스트 경로).

현재 이미 `./gradlew test`가 compileQuerydsl에서 실패했다. 이는 기능 Red가 아니다. 어떤 새 JUnit 테스트도 실행되지 않는 상황이므로, 구현자는 사용자와 **“기계적 타입/생성자 정합성 복원에 한해 compileJava/compileTestJava를 가장 가까운 검증으로 사용하고, 동작 변경은 그 후 실패 테스트부터 시작한다”**는 예외를 먼저 합의해야 한다. 이번 문서 작성은 그 예외 승인이나 코드 수정이 아니다.

- [x] 실행 환경·실패 로그를 기록하고 위 예외를 합의한다. 합의 전 프로덕션 수정 금지.
- [x] Announcement → domain.announcemnet.Announcement, AnnouncementForm → AnnouncementCreateRequest, AnnouncementDetail → AnnouncementDetailResponse로 참조 정합성을 맞춘다.
- [x] InitDb는 T10에서 삭제할 대상이다. 테스트 실행을 가능하게 하는 데 꼭 필요한 타입 참조만 임시 정합화하고, seed 기능 확장이나 구조 개선은 하지 않는다. 삭제와 local SQL 대체의 동작 검증은 T10에서 수행한다.
- [x] `./gradlew compileJava compileTestJava`를 실행해 후속 컴파일 오류를 모두 확인·해소한다.
- [x] `./gradlew test`로 기존 테스트 상태를 확정한다. 환경 실패·기존 테스트 실패는 각각 원인을 남긴다.
- [x] 다음 계약 테스트를 작성·실행한다. mock 서비스는 Optional.empty를 반환하며, 이번 실행은 처음부터 404로 통과하여 기존 동작 특성화로 기록했다.

```java
mockMvc.perform(get("/api/announce/detail/999"))
    .andExpect(status().isNotFound());
```

- [x] 이미 통과하면 기존 동작 특성화 테스트로 기록한다. 이를 Red라고 부르지 않고 이후 계약 변경의 실패 테스트를 별도로 만든다.
- [x] 동작을 바꾸지 않는 참조 정리만 마무리하고 전체 테스트 결과를 남긴다.

산출 계약: 컴파일 가능한 기존 API와 실패/통과가 명확한 기준선. 다음 Phase로 넘어가려면 테스트 실행 기반이 작동해야 한다.

### T01 · 프런트 테스트 명령과 서버 테스트 경계 — R14 / S01

수정 F: `package.json`, `package-lock.json`, `babel.config.js`. 생성 F: `jest.config.js`, `tests/unit/test-environment.spec.js`. 수정 B 테스트: `controller/MemberControllerTest.java`, `controller/QuestionControllerTest.java`, `controller/AnswerControllerTest.java`, `service/MemberServiceTest.java`, `service/QuestionServiceTest.java`, `repository/AnnouncementQueryRepositoryTest.java`, `repository/QuestionRepositoryTest.java`, `repository/AnswerRepositoryTest.java`.

- [x] F에서 `node --version`, `npm --version`, lockfileVersion과 Vue 해석 버전을 기록하고 `npm ci`로 설치한다. 설치 실패 시 버전/의존성 원인을 해결하고 lockfile만 임의 삭제하지 않는다.
- [x] Vue CLI 5용 unit-jest 플러그인 및 Vue 3용 Vue Test Utils 2, SFC/DOM 변환 설정을 추가한다. `test`는 `vue-cli-service test:unit`로 등록한다. Vue 런타임 의존성이 직접 선언되어 있지 않은 점도 lockfile과 대조해 명시한다.
- [x] 다음 테스트를 먼저 실행하여 실제 `false → true` assertion 실패를 확인한다.

```js
test('테스트 러너가 assertion 실패를 보고한다', () => {
  expect(false).toBe(true);
});
```

- [x] 위 의도적 실패 사례를 `mount({ template: '<button>질문하기</button>' })`의 `wrapper.get('button').text()`가 `질문하기`인지 확인하는 실사용 smoke 테스트로 교체하여 Green을 확인한다. 제품 버그 수정의 Red로 계산하지 않는다.
- [x] B Controller를 Spring MVC 컨텍스트와 mock Service/하위 계층으로 바꾸고, Repository는 @DataJpaTest로 전환한다.
- [x] `rg -n -A 25 '@BeforeEach' src/test/java`로 데이터 생성·저장·공유 fixture 할당을 조사한다. MemberServiceTest와 나머지 테스트 전체의 데이터 setup을 제거하고 각 테스트 본문의 Given에서 필요한 객체를 직접 생성한다. MockMvc 구성만 하는 메서드는 데이터 setup으로 간주하지 않는다.
- [x] 데이터 생성을 공통 필드 초기화나 상위 클래스·숨겨진 fixture helper로 옮기지 않는다. 각 테스트를 단독 실행한 결과와 전체 실행 결과가 같고, 불필요한 다른 테스트 데이터가 없어도 통과하는지 확인한다.
- [x] Service 테스트의 @Transactional을 제거하고 테스트별로 실제 생성한 Repository만 주입해 @AfterEach cleanup한다. Member는 Answer/Question/QuestionHashtag보다 나중에 지운다.
- [x] 각 클래스 전환 전후 전체 테스트를 실행한다. 드러난 생산 코드 오류는 별도 실패 테스트를 먼저 남기고 해당 후속 작업으로 연결한다.
- [x] 양쪽 전체 검증 결과와 설치 명령을 README에 기록한다.

산출 계약: F `npm test -- --runInBand`, B 계층별 실행·격리 기반. Refactor이므로 기존 Green 확인 전 테스트 구조를 일괄 교체하지 않는다.

### T02 · Controller 테스트와 REST Docs 테스트 분리 — R14 / S01

선행: T01. 수정 B 테스트: `controller/MemberControllerTest.java`. 생성 B 테스트: `docs/MemberRestDocsTest.java`, `docs/QuestionRestDocsTest.java`, `docs/AnswerRestDocsTest.java`, `docs/AnnouncementRestDocsTest.java`. 수정 B: `build.gradle`, `src/docs/asciidoc/index.adoc`, `Member-API.adoc`. 생성 B 문서: `src/docs/asciidoc/Question-API.adoc`, `Answer-API.adoc`, `Announcement-API.adoc`.

- [x] 기존 MemberControllerTest의 document 호출을 독립 docs 테스트로 옮기고 Controller의 상태·응답 assertion은 보존한다. docs 테스트도 @WebMvcTest와 @AutoConfigureRestDocs를 사용하며 하위 계층을 mock으로 교체하여 DB에 의존하지 않는다.
- [x] 각 docs 테스트 본문에서 요청 DTO·응답 객체·서비스 stub을 직접 만든다. 문서의 예제 계정과 토큰은 테스트 전용 값만 사용한다.
- [x] 회원·공고·질문·답변의 현재 실행 가능한 요청 헤더/경로/쿼리/본문·응답 필드·상태를 문서화한다. multipart는 실제 검증기에서 발생하는 등록 실패 계약을 문서화했다. 정상 등록·정상 공고 상세의 문서화는 T20에 남아 있으며 성공을 검증한 것으로 간주하지 않는다.
- [x] 응답 필드를 일부러 문서에서 빠뜨린 최소 사례를 실행해 REST Docs의 미문서화 필드 실패를 확인한 뒤 정확한 descriptor로 Green을 확인한다. relaxed 필드 검사나 광범위 ignored로 실패를 숨기지 않는다.
- [x] `./gradlew test --tests 'com.inhatc.demp.docs.*RestDocsTest'`로 snippets 생성을 확인하고 `./gradlew asciidoctor`로 HTML을 생성한다. include 누락·문서 경고도 확인한다.
- [x] T11/T12/T14/T20/T30/T32에서 Controller 회귀 테스트와 docs 테스트를 함께 갱신하도록 실행 규칙과 후속 작업에 연결했다. 실제 후속 API 변경 및 T14 공통 오류 문서화는 각 Phase에서 수행한다.
- [x] 전체 테스트 후 생성된 HTML의 예제와 실제 snippets를 대조한다. 산출물을 만들기 위해 추적 중인 정적 문서를 자동 삭제·덮어쓰는 build.gradle 작업은 출력 디렉터리 기반으로 정리한다.

산출 계약: `build/generated-snippets` → Asciidoctor → `build/docs/asciidoc`의 API 문서. 동작 검증과 문서 검증이 별도 테스트 클래스로 실행된다.

### Phase 0 실행 기록 · 2026-09-14

- 전체 백엔드 테스트 14개 클래스 리뷰 개선 완료: 95개 테스트와 clean test asciidoctor bootJar 통과. 세부 근거는 [전체 테스트 리뷰](docs/verification/review-corrections/full-test-review.md)를 참조한다.

- 브랜치: 양쪽 `refactor/build-and-test-foundation`.
- 현재 Backend 리뷰 경로: `/Users/seungmin/Desktop/repo/archive/demp`.
- 현재 Frontend 리뷰 경로: `/Users/seungmin/Desktop/repo/archive/dempfrontend`.
- T00: 기존에 합의한 기계적 컴파일 복원 예외 적용. 초기 missing symbol 4건 → 타입·생성자 참조만 복원. 404는 처음부터 통과한 특성화이며 Red가 아니다.
- T01: 환경변수 누락으로 43개 중 42개 실패 → 테스트 전용 H2/JWT/S3 설정으로 43개 통과. 이후 테스트 클래스 9개를 하나씩 전환하고 매번 전체 통과. 최종 Controller 4개·Repository 3개·Service 2개·기동 1개는 각각 단독 실행도 통과.
- T01 프런트: Node 18.18.2/npm 9.8.1, lock v2. `false → true` assertion Red 실행 후 버튼 mount Green. 전체 1개, lint, build 성공.
- T02: `MemberRestDocsTest.documentsMemberGet`의 username descriptor를 누락시켜 `SnippetException` Red를 확인하고 복원하여 Green. Controller와 문서 테스트 분리 및 build 출력 경로 정리.
- 범위: 실제 multipart 등록 실패(HTTP 200/본문 400, 서비스 미호출)를 재현·문서화했다. 정상 multipart와 정상 상세는 T20, 오류 상태 정합성은 T14, 태그 교체는 T31, 공고 정렬은 T32에 남는다.
- 상세 명령·결과·재사용 근거: [검증 기록](docs/verification/build-and-test-foundation/README.md).
- 사용자 요청에 따라 구현 커밋을 되돌리고 구현과 체크만 미커밋 상태로 보존했다. 이번 리뷰 수정도 commit하지 않는다.
- 리뷰 수정: InitDb 유지 시 전체 테스트 61개 중 4개 실패를 재현했다. 테스트 전용 데이터만 삭제하고 seed를 고려하도록 조회 기대값을 수정했다. Controller·REST Docs는 Spring MVC로 전환했고, 모든 테스트에 한글 DisplayName을 추가했다. 개발 확인용 로그는 제거하고 생성자 참조를 적용했다. 로그 전용 테스트는 사용자 요청으로 제거했다.
- IntelliJ 미리보기: REST Docs 전용 operation 매크로를 표준 include와 소스 기준 snippet 경로로 교체했다. 최초 미리보기 전에 test로 snippet을 생성한다. 실제 IDE 화면 확인과 Gradle 문서 생성 결과는 구분하여 보고한다.

## Phase 1 · 데이터 보존과 보안 경계

### T10 · InitDb 삭제와 local 실행 전용 data.sql — R02 / S02

삭제 B: `InitDb.java`. 수정 B: `src/main/resources/application.yml`, `src/test/resources/application.yml`, `build.gradle`. 생성 B: `src/main/resources/application-local.yml`, `src/main/resources/local/data.sql`, 테스트 `config/DatabaseLifecycleTest.java`, `config/LocalDataInitializationTest.java`.

- [x] 격리 DB에 회원 한 건을 넣고 기본 프로필 컨텍스트를 재시작한 뒤 회원이 남는 테스트를 작성·실행한다. 현재 create로 데이터가 사라지는 assertion 실패를 확인한다.
- [x] 기본·test 프로필에서 시작 후 예제 회원/공고가 없는 검증을 먼저 실패시킨다. 공통 설정의 `spring.profiles.active: local`을 제거하고 기본 및 테스트 `spring.sql.init.mode: never`를 명시한다.
- [x] InitDb와 관련 빈 참조를 삭제한다. 자동 테스트가 기존 seed에 의존해 실패하면 해당 테스트 본문에 필요한 객체를 직접 생성하고 다시 검증한다.
- [x] local 실제 실행 확인용으로 회원·권한·공고·질문·태그·답변의 최소 예제를 data.sql에 작성한다. FK 순서와 ID/sequence 충돌을 고려하고, 암호는 local 전용 BCrypt 해시를 사용한다. 외부 S3 업로드는 하지 않는다.
- [x] application-local.yml에만 `spring.sql.init.mode: always`, `spring.sql.init.data-locations: classpath:local/data.sql`을 지정한다. local 전용 DB와 Hibernate 스키마 생성 이후 SQL 실행 순서를 정하고 실제 기동으로 검증한다. 공통 루트 data.sql이나 테스트 fixture로 복사하지 않는다.
- [x] 검증 문서에 local 명시 실행 명령과 전용 DB 조건을 기록한다. README는 변경하지 않는다. local 재실행 시 seed 중복·키 충돌이 없도록 SQL을 작성하고 두 번 기동해 확인한다. 데이터 삭제로 중복을 해결하지 않는다.
- [x] 기본 ddl-auto는 validate로 전환한다. 자동 테스트는 data.sql을 로드하지 않으며, LocalDataInitializationTest도 격리 DB에서 프로필/초기화 계약만 검증한다. local 기동 확인은 별도 실행 검증으로 기록한다.
- [x] 2026-09-21 사용자 요청으로 Flyway 의존성·설정·migration을 제거했다. local H2는 update와 지연 SQL 초기화, 기본은 validate를 사용한다. 테스트 스키마는 테스트 준비에서 생성하며 MySQL은 검증하지 않는다.
- [x] 같은 격리 DB에 두 번 기동해 데이터와 스키마가 유지되는 Green을 확인한다.
- [x] InitDb 소스·빈 부재, local 외 SQL 초기화 비활성, 자동 테스트의 직접 fixture 생성을 확인하고 전체 테스트를 재실행한다.

### T11 · 회원 응답·가입·로그인 — R03, R08 / S03, S07

수정 B: `controller/MemberController.java`, `controller/ExController.java`, `service/MemberService.java`, `dto/member/MemberSaveForm.java`, `dto/member/MemberDto.java`, `domain/Member.java`. 수정 B 테스트: `controller/MemberControllerTest.java`, `service/MemberServiceTest.java`. 수정 F: `src/components/LoginForm.vue`. 생성 F: `tests/unit/LoginForm.spec.js`.

- [x] 다음 응답 검사를 가입과 회원 조회 각각에 작성해 password가 존재하여 실패하는지 확인한다.

```java
mockMvc.perform(get("/api/member/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.password").doesNotExist());
```

- [x] MemberDto는 id/username만 포함하도록 만들고 가입도 동일 DTO로 반환한다.
- [x] 빈 username/password는 400, 동일 username 재가입은 409, 비밀번호 불일치는 401인 테스트를 각각 Red→Green으로 진행한다.
- [x] 암호화는 MemberService의 주입 PasswordEncoder로 옮기고 DB username unique를 추가한다. H2의 JPA 매핑으로 NOT NULL/UNIQUE를 생성한다. 기존 외부 DB의 스키마 변경은 이번 범위에 포함하지 않는다.
- [x] F 로그인 제출 테스트에서 console.log spy에 입력 비밀번호가 전달되지 않음을 검증하고 로그를 제거한다. 로그인 성공/실패 안내도 유지되는지 확인한다.
- [x] 전체 검증 후 DTO/서비스 책임을 정리한다.

### T11A · Member와 UserDetails 분리 — R15 / S03, S04

선행: T11, 후속: T12. 수정 B: `domain/Member.java`, `service/CustomUserDetailService.java`, `config/jwt/JwtTokenProvider.java`. 생성 B: `config/security/MemberPrincipal.java`, 테스트 `config/security/MemberPrincipalTest.java`, `service/CustomUserDetailServiceTest.java`. 기존 로그인·JWT 테스트도 갱신한다.

계약: `MemberPrincipal implements UserDetails`, `Long getMemberId()`, `static MemberPrincipal from(Member member)`. memberId/username/password hash/권한 값을 복사하고 엔티티 자체를 보유하지 않는다. Member는 JPA 엔티티로 남고 Spring Security 타입·상태 메서드·GrantedAuthority 변환을 알지 않는다.

- [x] 현재 로그인·JWT 인증의 ID·username·ROLE_USER 동작을 특성화 테스트로 보호한다. 각 테스트 본문에서 Member를 생성한다.
- [x] CustomUserDetailService 반환값이 Member 엔티티가 아닌 인증 객체여야 한다는 assertion을 작성하고 현재 직접 반환 때문에 실패함을 확인한다.
- [x] MemberPrincipal과 변환을 최소 구현하고 인증 ID·암호 비교·권한이 유지되는지 검증한다. 존재하지 않거나 잘못된 토큰 subject의 처리도 T14와 연계한다.
- [x] Green에서 Member의 `implements UserDetails`, Security import와 전용 override를 제거한다. 회원 필드와 JPA 매핑은 유지한다.
- [x] `rg -n 'org.springframework.security|UserDetails|GrantedAuthority' src/main/java/com/inhatc/demp/domain`으로 도메인 결합 제거를 확인하고 전체 테스트를 실행한다. principal을 API 응답으로 반환하지 않는지도 확인한다.

### T12 · principal 기반 작성·소유권·오류 계약 — R04, R08 / S04, S07

수정 B: `controller/QuestionController.java`, `controller/AnswerController.java`, `service/QuestionService.java`, `controller/ExController.java`, `config/SecurityConfiguration.java`, 질문/답변 입력 DTO. 생성 B: `service/AnswerService.java`, `error/ResourceNotFoundException.java`, 테스트 `service/AnswerServiceTest.java`, `config/ApiSecurityTest.java`. 수정 B 테스트: QuestionServiceTest 및 QuestionControllerTest/AnswerControllerTest.

새 서비스 계약: `QuestionService.join(Long actorId, QuestionForm form)`, `updateQuestion(Long actorId, QuestionUpdateForm form)`, `deleteQuestion(Long actorId, Long questionId)`. AnswerService의 `save(Long actorId, AnswerForm form)`, `update(Long actorId, UpdateAnswerForm form)`, `delete(Long actorId, Long answerId)`가 답변 쓰기를 소유한다. principal ID는 T11A의 MemberPrincipal.getMemberId()로 구한다. Member 엔티티를 principal로 캐스팅하지 않는다.

- [x] A principal + B username 입력으로 생성한 질문의 작성자가 A여야 한다는 테스트를 작성·실행한다.
- [x] Controller가 actorId를 전달하고 Service가 actorId로 회원을 조회하도록 최소 변경한다. 요청 username은 호환을 위해 수신해도 신뢰하지 않는다.
- [x] 타인 질문/답변 수정·삭제 각각 403이고 원본이 유지되는 테스트를 Red→Green으로 진행한다.
- [x] 없는 자원 404, 비인증 쓰기 401을 별도 테스트한다. 권한 판정은 객체를 조회한 서비스 경계에서 수행한다.
- [x] ErrorResult와 실제 HTTP 상태를 ResponseEntity로 일치시키고 Security의 AuthenticationEntryPoint/AccessDeniedHandler도 동일 형식을 사용한다.
- [x] Controller에서 Repository 의존성을 제거하고 대상/전체 테스트를 재실행한다.

### T13 · HTML 콘텐츠 보호 — R05 / S05

생성 B: `service/ContentSanitizer.java`, 테스트 `service/ContentSanitizerTest.java`. 수정 B: `build.gradle`, QuestionService/AnswerService/AnnouncementService. 생성 F: `src/components/SafeHtml.vue`, `tests/unit/SafeHtml.spec.js`. 수정 F: 질문 상세/답변 및 공고 상세의 HTML 출력 지점.

- [x] 악성 입력과 허용 서식을 같이 제공하는 테스트를 작성한다.

```js
const raw = '<b>설명</b><img src=x onerror="alert(1)"><a href="javascript:alert(1)">링크</a>';
// SafeHtml에 raw를 전달하고 DOM의 [onerror], a[href^="javascript:"]가 0개인지,
// b의 텍스트는 설명으로 남는지 각각 assertion한다.
```

- [x] 검증된 sanitizer 라이브러리와 명시적 허용목록으로 script/이벤트 속성/위험 URL을 제거한다. 서버 계약은 `String sanitize(String html)`이다.
- [x] 질문·답변·공고 저장 후 응답과 기존 저장 콘텐츠 렌더링 모두 안전한지 각각 Red→Green으로 확인한다.
- [x] 공통 렌더러로 중복을 모으고 전체 테스트를 실행한다. 정규식 필터나 안전성 없는 문자열 교체로 대체하지 않는다.

### T14 · ExController 단순화와 예외·보안 점검 — R08 / S03, S04, S07

선행: T11A,T12. 수정 B: `controller/ExController.java`, `error/ErrorResult.java`, `service/MemberService.java`, `service/CustomUserDetailService.java`, `config/jwt/JwtAuthenticationFilter.java`, `config/jwt/JwtTokenProvider.java`, `config/SecurityConfiguration.java`. 생성 B 테스트: `controller/ExControllerTest.java`; 수정: `config/ApiSecurityTest.java`, `docs/MemberRestDocsTest.java`와 각 API docs 테스트.

오류 계약은 HTTP 상태와 공개 오류 코드/안내 문구를 대응시킨다. 400 입력 오류, 401 인증 실패, 403 권한 없음, 404 자원 없음, 409 중복, 500 내부 오류를 구분한다. 일반 IllegalStateException을 전부 400으로 해석하지 않고, 필요한 업무 예외만 명시적으로 매핑한다.

- [x] `e.getMessage()`에 가짜 SQL/클래스명/비밀번호/토큰을 넣은 예외를 발생시켜 공개 응답에 내부 문자열이 없어야 한다는 회귀 테스트를 실행한다. 내부 500은 고정 안내 문구로 처리한다.
- [x] validation에 field error가 없는 경우, JSON 파싱/타입 오류, 존재하지 않는 자원과 중복 요청의 상태·본문을 각각 테스트한다. 잘못된 validation annotation 등 서버 설정 오류는 클라이언트 400으로 숨기지 않는다.
- [x] 없는 계정과 비밀번호 불일치가 동일한 401 본문을 반환하는지 검증한다. SQL 예외·엔티티·스택 트레이스를 직렬화하지 않는다.
- [x] 누락/만료/변조 JWT, 숫자가 아닌 subject, 삭제된 회원의 유효 서명 JWT가 보호 API에서 500 대신 401로 끝나는지 실제 필터 경계 테스트로 검증한다. 유효 토큰의 권한 부족은 403이다.
- [x] Advice는 MVC 예외, AuthenticationEntryPoint/AccessDeniedHandler는 보안 오류를 처리하게 한다. 모든 Exception을 인증 실패로 치환하지 않고 내부 장애는 500으로 유지한다.
- [x] ExController는 예외→안전한 응답 변환만 맡도록 단순화한다. 로그에는 비밀번호·Authorization/X-AUTH-TOKEN·요청 본문을 남기지 않으며 오류 추적에 필요한 정보만 기록하는지 검사한다.
- [x] 대상/전체 테스트와 REST Docs 오류 응답 생성을 실행하고 상태·문구·노출 필드가 동일한지 확인한다.

Phase 1 최초 검증: 백엔드 141개·프런트 21개 테스트, 문서·jar·프런트 lint/build 및 local 재기동 통과. [구현과 검증 기록](docs/verification/data-preservation-and-security/README.md)을 참조한다.

## Phase 2 · 공고 API와 업로드 계약

작업 브랜치: `refactor/announcement-upload-contract`. Backend worktree: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/announcement-upload-contract/backend`. Frontend worktree: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/announcement-upload-contract/frontend`. 이후 원본 저장소로 옮겨 리뷰했다. 2026-09-26 사용자 요청에 따라 완료 작업을 커밋한다.

### T20 · 공고 DTO 계약 일치 — R06 / S06

수정 B: `dto/announcement/AnnouncementCreateRequest.java`, `AnnouncementDetailResponse.java`, `service/AnnouncementService.java`, `controller/AnnouncementController.java`. 생성 B 테스트: `controller/AnnouncementControllerTest.java`에 사례 추가, `domain/announcemnet/AnnouncementValuesTest.java`. 수정 F: `src/components/announcement/AnnouncementWrite.vue`, `AnnouncementDetail.vue`, `AnnouncementHeader.vue`. 생성 F: `src/api/announcements.js`, `tests/unit/AnnouncementDetail.spec.js`, `tests/unit/AnnouncementWrite.spec.js`.

새 HTTP 계약(내부 Embeddable과 분리):

```json
{
  "title": "백엔드 채용", "company": "DEMP", "type": "EMP",
  "position": "BACKEND", "minCareer": 0, "maxCareer": 3,
  "startedDate": "2026-09-01T00:00:00", "deadLineDate": "2026-09-30T23:59:00",
  "content": "설명", "accessUrl": "https://example.com/jobs/1",
  "payment": 3000, "language": ["JAVA", "SPRING"]
}
```

생성 요청은 위 이름을 가진 multipart 필드와 image 파일이다. company는 문자열이다. 상세 응답은 동일 평면 필드 중 type 대신 announcementType, company 대신 `{ "name": "DEMP" }`를 사용하고 image URL을 추가한다. 이 차이는 API 모듈에서 명시적으로 매핑한다. 최소/최대 경력과 날짜는 응답 DTO 자체에 두어 값 객체 getter 유무에 의존하지 않는다. 금액 단위는 현재 UI의 만원 표기를 유지한다.

- [x] 위 fixture로 상세 화면에 기간·금액·회사·본문이 표시되는 테스트를 작성해 현재 필드 불일치를 확인한다.
- [x] 서버 multipart 바인딩과 응답 JSON 계약 테스트를 작성한다. 잘못된 enum은 400, EMP/EDU 정상 입력은 성공이어야 한다.
- [x] DTO에서 문자열/숫자/날짜를 검증하고 Service에서 Company/Career/Description/RecruitPeriod로 변환한다. enum @NotBlank를 @NotNull로 교체한다.
- [x] 날짜 역전·음수 경력·min>max(max=0 상한 없음 예외)의 순수 도메인 테스트를 각각 Red→Green으로 진행한다.
- [x] F 요청 조립과 응답 사용을 fixture에 맞추고 양쪽 전체 검증을 실행한다.

2026-09-21 완료. Red는 백엔드 대상 10건 중 7건과 프런트 상세·작성 2건의 실패로 계약 불일치를 재현했다. Green에서 평면 DTO·값 객체 변환·명시적 프런트 매핑을 적용했다. Refactor에서 경력/날짜 교차 필드도 HTTP 400으로 막고 도메인 불변식을 유지했다. 최종 검증과 상세 실행 근거는 [Phase 2 검증 기록](docs/verification/announcement-upload-contract/README.md)에 남겼다. 선행 완료된 T20·T21의 백엔드 변경은 2026-09-26에 함께 `4699a51`로 소급 커밋했다.

### T21 · 파일 저장과 실패 보상 — R11 / S06

수정 B: FileService, AnnouncementService. 생성 B: `service/FileStorage.java`, 테스트 `service/AnnouncementServiceTest.java`, `service/FileServiceTest.java`.

새 포트 계약: `UploadFile save(MultipartFile file) throws IOException`, `void delete(String key)`. 기존 FileService가 구현한다. 필수 이미지 입력은 null/빈 파일을 400으로 거절한다. 허용 유형은 JPEG/PNG, 최대 5 MiB를 계획 기본값으로 명시하고 설정으로 관리한다.

- [x] 중복 공고 요청 시 fake storage에 업로드된 파일이 0개인 테스트를 작성하고 실제 실패를 확인한다.
- [x] 검증·중복 조회를 업로드보다 앞에 배치한다.
- [x] 업로드 성공 후 DB 저장 실패를 주입하고 저장 키의 삭제 보상을 assertion한다. DB transaction commit 실패까지 보상 경계에 포함시킨다.
- [x] null/빈 이미지, 크기 초과, 확장자와 MIME 불일치 실패를 각각 검증한다. 파일 내용 시그니처도 확인한다.
- [x] 보상 삭제도 실패할 때 원래 오류를 유지하고 키를 추적 가능하게 기록하는 테스트를 추가한다. 실제 S3 호출은 하지 않는다.
- [x] 외부 저장 인터페이스와 업무 흐름을 정리하고 전체 검증한다.

2026-09-21 완료. Red는 대상 13건 중 11건 실패로 업로드 순서·보상·파일 검증 부재를 재현했다. Green에서 `FileStorage`, commit을 포함한 보상 경계, JPEG/PNG·5 MiB 검증을 구현했다. Refactor에서 잘못된 Spring Data spy를 제거하고 실제 H2 commit 통합 테스트와 실패 주입 단위 테스트를 분리했다. 파일 UI도 필수 JPEG/PNG 선택 계약으로 맞췄다. 제목 사전 조회는 순차 중복 업로드를 막지만 동시 요청의 유일성을 보장하지 않는다. DB unique 제약은 제목의 업무상 유일성 결정과 운영 스키마 변경 절차가 필요하므로 이번 범위에 추가하지 않았다. 백엔드 소급 커밋은 T20과 함께 `4699a51`이다.

## Phase 3 · 저장·관계·조회 정확성

작업 브랜치: `refactor/answer-tag-and-pagination-accuracy`. Backend worktree: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/answer-tag-and-pagination-accuracy/backend`. Frontend worktree: `/Users/seungmin/Desktop/repo/archive/dempfrontend/.worktrees/answer-tag-and-pagination-accuracy/frontend`.

### T30 · 답변 수정 실제 commit — R07 / S07

Phase 1 선행 반영: AnswerService.update의 실제 트랜잭션, commit 뒤 재조회, 타인 403/없는 답변 404를 검증했다. 이 항목 실행 시 현재 통과 동작을 대조하고 남은 검증만 수행한다. 이미 있는 트랜잭션을 제거해 인위적인 Red를 만들지 않는다.

수정 B: T12에서 만든 AnswerService. 테스트: `service/AnswerServiceTest.java`, `controller/AnswerControllerTest.java`.

- [x] 테스트 @Transactional 없이 답변을 만들고 서비스 update 호출 후 Repository에서 다시 조회한다.

```java
answerService.update(actorId, updateForm);
assertThat(answerRepository.findById(answerId).orElseThrow().getContent())
    .isEqualTo("수정된 답변");
```

- [x] T12에서 public update에 이미 적용한 @Transactional과 commit 후 재조회 테스트를 기준선으로 확인한다. 이번에는 구현을 제거해 인위적인 Red를 만들지 않는다. 당시 쓰기 트랜잭션 부재의 실패 로그는 없어 Red 증거로 주장하지 않는다.
- [x] 소유권 실패 시 내용이 바뀌지 않는지, 없는 ID는 404인지 함께 유지한다.
- [x] cleanup은 Answer→Question→Member 순서로 하고 전체 테스트를 재실행한다.

2026-09-26: 기존 `AnswerService.update`의 쓰기 트랜잭션과 `AnswerServiceTest.commitsSanitizedUpdate`가 commit 후 별도 조회에서 통과하는 것을 먼저 확인했다. 타인 수정 시 원본 보존 테스트를 추가했으며 처음부터 통과했으므로 기존 동작 특성화다. 없는 답변은 기존 서비스 예외 테스트, 403과 원본 보존은 기존 `ApiSecurityTest`로 검증한다. 테스트 데이터 cleanup은 Answer→Question→Member 순서다. 이름·책임 검토 결과, `update`는 답변 소유권·정제·상태 변경이라는 하나의 유스케이스에 속하므로 서비스/도메인 경계를 유지했다. 컨트롤러는 HTTP 입력 전달을, 서비스는 권한과 트랜잭션을, Answer는 자신의 내용 변경을 맡는다. 대상 `./gradlew test --tests 'com.inhatc.demp.service.AnswerServiceTest' --tests 'com.inhatc.demp.controller.AnswerControllerTest'` 성공, 전체 `./gradlew test` 27 suite·169 test·실패/오류/건너뜀 0.

### T31 · 태그 교체와 질문 검색 — R09 / S08

수정 B: QuestionService, `domain/Question.java`, `repository/HashtagRepository.java`, `repository/question/QuestionQueryRepository.java`. 생성 B: 테스트 `domain/QuestionTest.java`, `repository/QuestionQueryRepositoryTest.java`. 수정 B 테스트: QuestionServiceTest.

새 계약: `HashtagRepository.findByTagName(String tagName)` → Optional<Hashtag>, `Question.replaceHashtags(List<Hashtag> tags)`는 기존 관계를 제거하고 새 관계를 연결한다. trim 후 빈 태그 제거, 중복 제거, 대소문자 보존을 기본 정책으로 한다.

- [x] `[JAVA] → [SPRING]` 수정 후 JAVA 관계가 남지 않는 테스트를 작성해 실패를 확인한다.
- [x] 관계 교체를 도메인 메서드로 구현하고 다른 질문의 JAVA 관계가 보존되는지 검증한다.
- [x] 동일 이름 태그를 두 질문에 등록해 Hashtag 레코드 1개를 assertion한 뒤 조회/재사용·unique 제약을 구현한다.
- [x] 무태그 질문이 무필터 검색에 포함되는 테스트를 작성한 뒤 조건 없는 inner join을 제거한다.
- [x] 여러 태그 OR 필터와 title/content AND 필터, 중복 질문 부재를 각각 검증한다.
- [x] 향후 유일 제약 적용 전 기존 중복 이름을 대표 ID로 합치고 관계를 이관하는 절차를 작성한다. 전체 테스트를 실행한다.

2026-09-26: Red는 태그 누적, 이름 중복 저장, 두 조회 경로의 무태그 질문 누락에서 각각 assertion 실패를 확인했다. Green은 `Question.replaceHashtags`, `HashtagRepository.findByTagName`, 이름 unique 제약, 두 쿼리의 LEFT JOIN이다. Refactor에서 서비스의 미사용 답변 저장소 의존성과 오래된 테스트 fixture를 정리했다. 관계 공유·trim/중복·대소문자·OR/AND·동일 태그 재수정은 통과 상태를 추가 고정한 사례다. 대상 테스트 성공, Refactor 후 전체 `./gradlew test` 29 suite·176 test·실패/오류/건너뜀 0. 상세 명령, 기존 데이터 이관 순서와 운영 DB 미검증 범위는 [T31 검증 기록](docs/verification/answer-tag-and-pagination-accuracy/T31-tags.md)에 남긴다. `QuestionHashtags` 필드명은 JPA/QueryDSL 참조와 함께 T46에서 변경 여부를 결정한다.

### T32 · 안정된 페이지 조회 — R10 / S09

수정 B: AnnouncementQueryRepository, QuestionQueryRepository, QuestionController, QuestionService. 수정 B 테스트: AnnouncementQueryRepositoryTest, QuestionQueryRepositoryTest. 수정 F: `src/components/question/QuestionList.vue`, `src/api/questions.js`(생성). 생성 F 테스트: `tests/unit/QuestionList.spec.js`.

계약: 공고는 기존 Slice content/last 유지. 질문은 `GET /api/question?page=0&size=20` → `{content:[...], last:boolean, number:0}`로 양쪽 동시 전환한다. size 범위 1~100, 기본 20. 공고 정렬 id DESC, 질문 기본 createdDate DESC/id DESC, hits·recommend 정렬에도 id DESC 동률 기준을 둔다.

- [x] 복수 언어 공고와 동일 정렬값 질문을 page size보다 많이 만들고 두 페이지 ID가 겹치지 않는 테스트를 작성한다.
- [x] 공고 ID만 size+1 조회하고 해당 ID의 연관 데이터를 별도 조회한다. Slice에서 불필요한 count를 제거한다.
- [x] Hibernate 테스트 설정에서 collection fetch pagination을 실패 처리하여 메모리 페이징 재발을 검출한다. 실제 SQL에 limit이 적용되는지도 확인한다.
- [x] 질문의 페이지 결과·경계·빈 마지막 페이지 테스트를 먼저 실패시킨 뒤 B/F 계약을 함께 변경한다.
- [x] 제목·직군·태그 필터가 페이지 변경 후에도 유지되는지 확인하고 전체 검증한다.

2026-09-26: Red는 공고 컬렉션 fetch 페이지의 Hibernate 예외, 질문 HTTP 배열 응답의 `$.content` 부재, 프런트 목록의 새 페이지 객체 미표시였다. Green은 공고 ID 선조회·별도 언어 로드, 질문 조건부 EXISTS와 ID 동률 정렬·Slice DTO, 프런트 API 모듈·페이지 버튼이다. Refactor에서 질문 Controller 이름과 미사용 서비스 전체 목록 메서드, 정렬 메서드·클래스를 정리했다. [T32 검증 기록](docs/verification/answer-tag-and-pagination-accuracy/T32-pagination.md)에 SQL·REST Docs·양쪽 화면 흐름을 기록했다. Backend `clean test asciidoctor bootJar`와 Refactor 후 `test asciidoctor bootJar` 29 suite·187 test·실패/오류/건너뜀 0, Frontend 8 suite·27 test, lint·build 성공. Frontend 커밋 `2180693`.

## Phase 4 · 화면 상태와 실행 연결

### T40 · 재시도·검색·페이지 상태 — R12 / S10

수정 F: `src/components/question/QuestionWrite.vue`, `QuestionSearch.vue`, `QuestionList.vue`, `src/components/announcement/AnnouncementList.vue`. 생성 F 테스트: `tests/unit/QuestionWrite.spec.js`, `QuestionSearch.spec.js`, `AnnouncementList.spec.js`.

- [ ] 최초 제출 실패→재제출의 두 요청이 동일 태그 배열을 가지는 테스트를 작성한다. 전역 temp를 제거하고 `tags.map(tag => tag.value)`로 매번 별도 payload를 만든다.
- [ ] 제목 검색 후 내용 검색 시 title query가 비워지는 테스트를 작성하고 선택하지 않은 조건을 제거한다.
- [ ] 첫 요청보다 두 번째 검색 응답이 먼저 도착하도록 Promise를 제어해 화면이 최신 검색에 머무는지 검사한다. 현재 오래된 응답 덮어쓰기를 확인한 뒤 요청 세대 ID를 적용한다.
- [ ] 더보기 연속 클릭 중 요청 1개, 실패 시 error+재시도 버튼, 성공 빈 결과만 last 상태인 사례를 각각 Red→Green으로 진행한다.
- [ ] emitter 핸들러를 named function으로 두고 unmounted에서 해제한다. 재마운트 후 이벤트가 한 번만 처리되는지 확인한다.
- [ ] 전체 테스트/lint/build 후 상태와 렌더링 책임을 정리한다.

### T41 · API client·인증 만료·운영 경로 — R14 / S11

생성 F: `src/api/client.js`, `tests/unit/apiClient.spec.js`, `tests/unit/router.spec.js`, `.env.example`. 수정 F: Login store, router, 각 Axios 사용 컴포넌트, vue.config.js, server.js, README.md. 수정 B: SecurityConfiguration CORS 설정. 생성 B 테스트: `config/CorsPolicyTest.java`.

새 계약: `createApiClient({baseURL, getToken, onUnauthorized})` → Axios instance. X-AUTH-TOKEN을 요청 경계에서 추가하고 401이면 onUnauthorized를 호출한다. API endpoint 함수는 이 client만 사용한다.

- [ ] 만료 토큰 요청의 401 뒤 store의 token/username이 모두 비워지고 login redirect가 원래 경로를 보존하는 테스트를 작성한다.
- [ ] 중앙 client interceptor와 라우트 인증 meta/guard를 도입한다. 비어 있지 않은 토큰만으로 유효 인증을 확정하지 않는다.
- [ ] vue.config.js를 하나의 export로 합치고 개발 API target을 환경 설정으로 분리한다.
- [ ] 운영은 명시적 API baseURL 또는 앞단 /api reverse proxy 중 배포 방식에 맞는 설정을 문서화한다. Express 단독 /api GET은 HTML fallback이 아니라 JSON 404를 반환하도록 테스트 후 처리한다.
- [ ] CORS 허용 origin/거절 origin preflight 테스트를 작성하고 환경별 허용목록을 적용한다.
- [ ] 모듈별 Axios 중복을 제거하고 전체 검증한다.

### T42 · 추천 표기와 미완성 반응 안내 — R13 / S12

수정 F: `src/components/question/QuestionMenu.vue`, `QuestionList.vue`, `QuestionDetail.vue`, `QuestionAnswer.vue`. 생성/수정 F 테스트: `tests/unit/QuestionDetail.spec.js`, `QuestionMenu.spec.js`, `QuestionAnswer.spec.js`.

- [ ] 서버 `{recommend:3}` fixture가 3으로 표시되고 추천 메뉴가 orderBy=recommend를 전달하는 테스트를 작성한다.
- [ ] recomend 오타를 통일하고 로컬 카운터 증가를 제거한다.
- [ ] 반응 버튼이 disabled이며 저장되지 않는 기능임을 사용자에게 표시하는 테스트를 작성·통과시킨다. 실제 투표 API는 추가하지 않는다.
- [ ] 목록·상세·답변을 모두 검증하고 전체 테스트/lint/build를 실행한다.

## Phase 5 · 도메인 언어와 책임 전수 검토 — R16 / S13

실행 예정 브랜치: `refactor/domain-language-and-responsibility`. 백엔드 worktree: `demp/.worktrees/domain-language-and-responsibility/backend`; 프런트 worktree: `dempfrontend/.worktrees/domain-language-and-responsibility/frontend`. 작업 시작 시 각 저장소의 실제 경로·브랜치·기준 커밋을 기록하고 해당 Phase 전용 worktree에서 수행한다. 현재 브랜치의 미커밋 변경을 암묵적으로 옮기지 않는다. T31·T32·T40~T42와 겹치는 변경은 해당 작업에서 먼저 해결하고 T45에서 재검토한다. 완료 작업은 2026-09-26 사용자 요청에 따라 커밋한다.

판정 단위는 **파일 → 선언된 이름 → 메서드의 실제 효과 → 현재 클래스의 책임 → 함께 옮길 협력자 → 공개 계약**이다. `rg --files`로 B의 `src/main/java`, `src/test/java`와 F의 `src`, `tests`, 실행 스크립트를 목록화한다. 생성 코드·의존성·빌드 산출물은 제외 사유를 적는다. 모든 파일에 `검토 완료/후속 작업/유지`와 근거를 남기고 새 파일도 확인한다. 이전 R01~R15나 아래 후보 몇 개를 전수 검사로 간주하지 않는다.

### T45 · 파일별 전수 인벤토리와 이름·책임 판정표

생성 B: `docs/verification/domain-language-and-responsibility/inventory.md`. 수정 B: `tasks.md`. F는 읽기 범위이며 F의 AGENTS.md와 코드가 허용하는 작업 범위를 시작 시 확인한다.

2026-09-26 현재 미커밋 코드의 선행 정적 리뷰를 [파일별 판정표와 N01~N15](docs/verification/domain-language-and-responsibility/inventory.md)에 기록했다. B Java 96개, F Vue/JS 40개, 빌드·실행 스크립트 6개의 목록과 제외 대상을 대조했다. Java 11의 B `./gradlew test`는 168개 통과했고 F `npm test -- --runInBand`는 의존성 부재로 실행되지 않았다. 실제 T46~T49 구현과 동작 재현은 별도 worktree에서 수행한다.

후속 환경 복원 뒤 F 테스트 24개, lint, build를 실행해 통과했다. 설치 실패 원인과 Sass 교체는 판정표의 후속 검증에 기록했다. T45의 정적 발견을 실행 재현으로 바꾸어 해석하지 않는다.

- [x] 양쪽 저장소의 추적 대상 파일 목록과 개수를 기록하고, 각 파일의 클래스/컴포넌트/모듈 역할과 선언된 이름을 검토한다. 생성 코드와 산출물 제외 목록도 적어 누락을 검증한다.
- [x] 각 메서드에 대해 이름이 반환값·부작용·예외·트랜잭션을 드러내는지, 실제 행위가 한 유스케이스의 조정인지 여러 독립 규칙인지, 현재 클래스가 그 규칙을 소유해야 하는지 판정한다. 필드는 도메인 용어·단위·컬렉션 단복수·HTTP/DB 이름과 대조한다.
- [x] Java 객체와 독립 TypeScript 모듈에 대해 변경 이유(SRP), 확장 지점(OCP), 치환 가능성(LSP), 필요한 계약만 노출하는지(ISP), 외부 경계 의존 방향(DIP)을 **해당되는 관계에서만** 판정한다. 현재 F에는 `.ts/.tsx`가 0개이므로 TypeScript 판정은 대상 없음으로 기록하고, Vue 컴포넌트에는 SOLID 판정을 하지 않는다.
- [x] 각 지적에 `파일:줄`, 호출부, 실제 동작, 제안 이름 또는 책임 이동, 유지/수정 이유, 호환성 위험, 담당 T46~T49를 적는다. 지적이 없는 파일도 `검토 완료·유지`로 남긴다. 파일 목록과 판정 행의 누락이 0개인지 재대조한다.

### T46 · 도메인 용어와 관계 불변식 — T31 이후

수정 후보 B: `domain/Question.java`, `Answer.java`, `QuestionHashtag.java`, `Hashtag.java`, `domain/announcemnet/*`, `dto/announcement/AnnouncementDetailResponse.java` 및 호출부/관련 테스트. 실제 수정 목록은 T45 판정표로 확정한다.

- [ ] 관계 연결·교체, 값 객체 불변식, 응답 매핑의 현재 공개 동작을 Domain/Repository/Controller 테스트로 먼저 고정한다. `QuestionHashtags`, `settingMember`, `getBuild`, `announcemnet`의 변경이 JPA 필드명·QueryDSL 생성형·JSON 키를 바꾸는지 확인한다.
- [ ] 서로 연동되는 관계 변경은 소유 엔티티의 하나의 연산으로 모으고, 태그 탐색·저장은 도메인 엔티티로 옮기지 않는다. 도메인에서 상태 변경과 양방향 관계를 보장하는 테스트를 통과시킨다. 새 동작이 필요하면 그 사례만 별도 Red→Green으로 진행한다.
- [ ] 이름을 의미에 맞게 바꾸고 호출부를 함께 정리한다. 외부 계약 변경이 불필요하면 명시적 JPA/JSON 매핑으로 호환성을 유지한다. 대상 및 `./gradlew test`를 실행한다.

### T47 · 서비스·저장소 메서드의 효과와 클래스 소속 — T32 이후

수정 후보 B: `service/QuestionService.java`, `AnnouncementService.java`, `MemberService.java`, `FileService.java`, `repository/question/QuestionQueryRepository.java`, `repository/announcement/AnnouncementQueryRepository.java`, `controller/MemberController.java` 및 호출부/관련 테스트. 실제 수정 목록은 T45 판정표로 확정한다.

- [ ] `join`, `setHashtags`, `save`, `pageTest/pagingTest`, `validationDuplicateUsername`, `QuestionSort`, `getFullPath`의 호출부·반환값·부작용을 추적하고 유지/이름 변경/메서드 추출/클래스 추출 중 하나를 근거와 함께 결정한다.
- [ ] 질문 생성/태그 관리, 공고 입력 변환/저장·보상, 파일 검증/전송이 각각 독립된 변경 이유를 가진 경우에만 협력 클래스를 추출한다. 새 클래스의 입력·출력·소유 규칙·트랜잭션/외부 효과를 판정표에 명시한다. 서비스는 유스케이스의 순서와 트랜잭션을 조정할 수 있으므로 단계 수만으로 분리하지 않는다.
- [ ] 기존 결과·상태 코드·트랜잭션 commit·파일 삭제 보상 테스트를 먼저 실행해 기준선을 확보한다. 책임 이동 뒤 대상 테스트와 `./gradlew test`를 다시 실행하고, 동작 오류가 새로 발견되면 별도 실패 테스트를 먼저 작성한다.

### T48 · 프런트 도메인 이름과 상태·API 경계 — T40~T42 이후

수정 후보 F: `src/data/positon.js`, `src/router/index.js`, `src/components/announcement/AnnouncementList.vue`, `src/api/*`, `src/store/*`, `src/fontAwesomeIcon.js`, 미사용 예제 컴포넌트 및 관련 테스트. 실제 수정 목록은 T45 판정표로 확정한다.

- [ ] 모든 Vue/JavaScript 파일에서 컴포넌트명·props·emits·상태 필드·함수명·라우트명을 화면 도메인과 실제 효과에 대조한다. `positon`, `Test*`, `loadDataFromServer` 후보의 외부 참조를 확인한다.
- [ ] 비동기 요청·상태 전이·렌더링이 한 메서드에 섞인 경우 API 호출은 API 모듈, 공유 상태 전이는 store, 표현은 컴포넌트에 배치할지 결정한다. 단일 화면의 지역 상태만 필요한 경우 불필요한 store 추출을 하지 않는다.
- [ ] 사용자에게 보이는 결과와 라우트/이벤트 계약을 컴포넌트 테스트로 고정한 뒤 이름·책임을 바꾼다. F 전체 `npm test -- --runInBand`, `npm run lint -- --no-fix`, `npm run build`를 실행한다.

### T49 · 감사 누락·공개 계약 재검증

수정 B: `docs/verification/domain-language-and-responsibility/inventory.md`, `tasks.md`. 필요한 경우 B/F의 테스트·API 문서.

- [ ] T45 파일 목록을 현재 양쪽 추적 대상 파일과 재대조하고 모든 판정·후속 조치·유지 이유를 닫는다. 새로 생긴 파일도 이름·효과·소속 책임을 검사한다.
- [ ] REST Docs/JSON 키, DB 매핑, 라우트, 파일 저장 경로, 검색·페이지 결과를 변경 전 계약과 대조한다. 의도한 변경은 양쪽 테스트와 문서에 반영한다.
- [ ] B `./gradlew test`와 `./gradlew asciidoctor`, F 전체 테스트/lint/build를 실행하고 실제 명령·결과를 기록한다. 정적 위험과 실행 재현 결과를 구분한다.

## Phase 6 · 통합 검증과 인수

### T50 · 전체 사용자 흐름과 CI — S01~S13

생성 B/F: `.github/workflows/ci.yml`. 생성 F: `tests/e2e/community.spec.js`, `playwright.config.js`. 수정 B/F: README.md. 외부 시스템은 격리된 테스트 DB/파일 저장 대역을 사용한다.

- [ ] 양쪽 CI에서 lockfile 설치, B `./gradlew test`, F 테스트/lint/build를 실행한다. 실패 단계에서 pipeline이 종료되는지 확인한다.
- [ ] B REST Docs 테스트도 전체 test에 포함하고 `./gradlew asciidoctor`로 API HTML을 생성한다. 문서 테스트 실패·누락 snippet·내부 정보가 포함된 예제가 있으면 인수하지 않는다.
- [ ] 회원가입→로그인→공고 필터→상세→질문 작성→답변→별도 재조회 흐름의 E2E를 작성한다.
- [ ] 다른 회원의 수정 거절, 만료 로그인, HTML 콘텐츠, 검색 응답 역전, 새로고침 후 상태를 추가한다.
- [ ] 테스트 fixture를 독립적으로 생성·정리하고 생산 데이터/운영 S3 접근이 없는지 확인한다.
- [ ] 아래 명령 결과를 날짜·실제 건수와 함께 기록한다. 실행하지 않은 명령을 통과로 적지 않는다.

```sh
# B
./gradlew clean test
./gradlew asciidoctor
# F
npm ci
npm test -- --runInBand
npm run lint -- --no-fix
npm run build
npx playwright test
```

- [ ] 변경된 API fixture와 spec.md를 대조하고 양쪽 동시 배포/rollback 순서를 README에 기록한다. GitHub push·배포는 이 문서 작성 요청의 실행 범위에 포함하지 않는다.

## 추적표와 작업 종료 기록

| 스펙 | 작업 | 리뷰 |
| --- | --- | --- |
| S01 | T00,T01,T02,T50 | R01,R14 |
| S02 | T10 | R02 |
| S03 | T11,T11A,T14 | R03,R08,R15 |
| S04 | T11A,T12,T14 | R04,R08,R15 |
| S05 | T13 | R05 |
| S06 | T20,T21 | R06,R11 |
| S07 | T11,T12,T14,T30 | R07,R08 |
| S08 | T31 | R09 |
| S09 | T32 | R10 |
| S10 | T40 | R12 |
| S11 | T41 | R14 |
| S12 | T42 | R13 |
| S13 | T45~T49 및 모든 T의 상시 검토 | R16 |

| 추가 요구사항 | 실행 위치 | 인수 기준 |
| --- | --- | --- |
| InitDb 삭제/local data.sql | T10 | Java seed 없음, local 실제 실행에서만 SQL seed, 자동 테스트는 직접 객체 생성 |
| Member에서 UserDetails 제거 | T11A→T12 | 도메인 Security 의존성 없음, 별도 principal로 기존 인증 유지 |
| 별도 REST Docs 테스트 | T02 및 API 변경 작업,T50 | Controller/docs 테스트 분리, snippets와 HTML 생성 |
| 데이터 setup @BeforeEach 제거 | T01 및 모든 후속 테스트 | 각 테스트 본문에서 필요한 객체 직접 생성, 숨은 공유 fixture 없음 |
| ExController·Exception 보안 | T14 | 안전한 상태/본문, 내부 정보 미노출, 필터·MVC 경계 모두 검증 |
| 이름·메서드 효과·클래스 소속 상시 검토 | 모든 T, T45~T49 | 변경 파일·호출부 기록과 양쪽 저장소 파일별 판정 완료 |
| SOLID 적용 범위 | T45~T49 | 백엔드·독립 TypeScript 객체만 판정, Vue는 역할 경계 검토 |

각 작업 완료 시 이 파일의 해당 항목 아래에 실행 날짜, Red 명령/실패 assertion, Green 최소 변경, Refactor 변경 이유, 대상/전체 검증 결과를 추가한다. 커밋을 명시적으로 요청받아 생성한 경우에만 SHA를 적는다. 순수 이름·구조 정리는 기존 Green 계약을 먼저 기록한다. 동작 변경의 Red가 처음부터 통과하면 회귀 재현에 실패한 것이므로 사례를 다시 구성한다. T00의 합의된 컴파일 복원은 이 기능 Red 기록과 분리한다.

현재 상태: Phase 0~3 구현·검증 완료. Phase 4, Phase 5의 T46~T49, Phase 6은 미착수다. T30의 트랜잭션·재조회 검증은 T12에서 선행 적용했고 2026-09-26에 원본 보존 검증을 보강했다. 같은 날 `refactor/announcement-upload-contract` 브랜치의 원본 작업 디렉터리에서 Phase 5의 T45 정적 전수 검토를 선행했다.

### 2026-09-21 Phase 1 리뷰 반영

작업 위치는 사용자 리뷰를 위해 전환한 원본 `/Users/seungmin/Desktop/repo/archive/demp`, 브랜치는 `refactor/data-preservation-and-security`다. 이전 worktree는 보관본이며 현재 리뷰 수정의 기준은 원본이다. README 변경을 원복하고 Flyway/migration을 제거했다. 회원 가입·로그인 응답 조립은 서비스로 이동하고, 질문 상세의 도달 불가능한 null 분기와 완료된 임시 회원 TODO를 제거했다. 답변 배열 응답의 wrapper 변경은 기존 계획에 없으며 현재 계약을 유지한다. 상세 검증은 [리뷰 수정 기록](docs/verification/data-preservation-and-security/review-2026-09-21.md)을 참조한다.

- [ ] 사용자 결정으로 생성자 제한·builder 통일은 후속 리팩터링에 보류한다. 기존 동작 테스트가 통과하는 상태에서 적용 범위를 합의하고 별도로 수행한다. JPA 기본 생성자 및 Spring/Jackson 바인딩 요구를 보존한다. 기존 private Announcement 생성자는 유지한다.

리뷰 수정 최종 검증: H2 백엔드 144개 테스트와 clean test asciidoctor bootJar 통과. REST Docs 91개 참조 누락 없음. 프런트는 이번 리뷰에서 변경하지 않았다.
