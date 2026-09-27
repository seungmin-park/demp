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

실행 브랜치: `refactor/async-state-and-client-integration`. 백엔드 worktree: `demp/.worktrees/async-state-and-client-integration/backend`; 프런트 worktree: `dempfrontend/.worktrees/async-state-and-client-integration/frontend`. Phase 3의 B `27f6a42`, F `2180693`에서 분기했다.

### T40 · 재시도·검색·페이지 상태 — R12 / S10

수정 F: `src/components/question/QuestionWrite.vue`, `QuestionSearch.vue`, `QuestionList.vue`, `src/components/announcement/AnnouncementList.vue`. 생성 F 테스트: `tests/unit/QuestionWrite.spec.js`, `QuestionSearch.spec.js`, `AnnouncementList.spec.js`.

- [x] 최초 제출 실패→재제출의 두 요청이 동일 태그 배열을 가지는 테스트를 작성한다. 전역 temp를 제거하고 `tags.map(tag => tag.value)`로 매번 별도 payload를 만든다.
- [x] 제목 검색 후 내용 검색 시 title query가 비워지는 테스트를 작성하고 선택하지 않은 조건을 제거한다.
- [x] 첫 요청보다 두 번째 검색 응답이 먼저 도착하도록 Promise를 제어해 화면이 최신 검색에 머무는지 검사한다. 실제 재현된 새 요청 누락과 기존 코드의 오래된 응답 반영 경로를 요청 세대 ID로 해결한다.
- [x] 더보기 연속 클릭 중 요청 1개, 실패 시 error+재시도 버튼, 성공 빈 결과만 last 상태인 사례를 각각 Red→Green으로 진행한다.
- [x] emitter 핸들러를 named function으로 두고 unmounted에서 해제한다. 재마운트 후 이벤트가 한 번만 처리되는지 확인한다.
- [x] 전체 테스트/lint/build 후 상태와 렌더링 책임을 정리한다.

2026-09-26: Red `npm test -- --runInBand QuestionWrite QuestionSearch`에서 재제출 태그 `['JAVA', undefined]`와 이전 제목 잔존을 확인했다. `QuestionList AnnouncementList` 대상 테스트에서는 검색 중 새 요청 누락(1회/기대 2회), 이벤트 구독 잔존(2개/기대 1개), 더보기 중복 요청(3회/기대 2회), 실패가 마지막 상태로 오인되는 결과를 확인했다. Green은 제출마다 새 payload·태그 배열 생성, 선택한 검색 조건만 route 전달, 요청 세대별 최신 응답 반영, 공고 loading/error/last 분리, named emitter handler 해제다. Refactor에서는 검색의 중복 상태 필드를 없애고 공고 조건을 복사해 요청 중 변경을 막았으며, 질문 편집기 입력을 선언된 폼 필드에 연결했다. 프런트 `npm test -- --runInBand --silent` 10 suite·34 test 통과, `npm run lint -- --no-fix` 통과, `npm run build` 성공(기존 번들 크기 권고 경고). 변경 Vue의 역할은 입력/표시와 요청 상태 조정이며 API 요청 형식은 기존 모듈에 유지했다. 호출부의 query·emitter·HTTP 경로 계약은 컴포넌트 테스트로 검증했다. 독립 TypeScript 객체는 현재 없다. 프런트 커밋 `20ffba5`.

### T41 · API client·인증 만료·운영 경로 — R14 / S11

생성 F: `src/api/client.js`, `tests/unit/apiClient.spec.js`, `tests/unit/router.spec.js`, `.env.example`. 수정 F: Login store, router, 각 Axios 사용 컴포넌트, vue.config.js, server.js, README.md. 수정 B: SecurityConfiguration CORS 설정. 생성 B 테스트: `config/CorsPolicyTest.java`.

새 계약: `createApiClient({baseURL, getToken, onUnauthorized})` → Axios instance. X-AUTH-TOKEN을 요청 경계에서 추가하고 401이면 onUnauthorized를 호출한다. API endpoint 함수는 이 client만 사용한다.

- [x] 만료 토큰 요청의 401 뒤 store의 token/username이 모두 비워지고 login redirect가 원래 경로를 보존하는 테스트를 작성한다.
- [x] 중앙 client interceptor와 라우트 인증 meta/guard를 도입한다. 비어 있지 않은 토큰만으로 유효 인증을 확정하지 않는다.
- [x] vue.config.js를 하나의 export로 합치고 개발 API target을 환경 설정으로 분리한다.
- [x] 운영은 명시적 API baseURL 또는 앞단 /api reverse proxy 중 배포 방식에 맞는 설정을 문서화한다. Express 단독 /api GET은 HTML fallback이 아니라 JSON 404를 반환하도록 테스트 후 처리한다.
- [x] CORS 허용 origin/거절 origin preflight 테스트를 작성하고 환경별 허용목록을 적용한다.
- [x] 모듈별 Axios 중복을 제거하고 전체 검증한다.

2026-09-26: Red `npm test -- --runInBand apiClient`에서 요청 토큰 헤더가 두 번 모두 `undefined`, `router` 대상에서는 보호 경로가 `true`, `server` 대상에서는 없는 `/api` GET이 200 HTML이었다. B `./gradlew test --tests 'com.inhatc.demp.config.CorsPolicyTest'`는 미허용 Origin 거절 assertion이 실패했다. Green은 Axios instance 요청/401 interceptor, Vuex logout과 원래 경로 redirect, route meta/guard, 환경변수 기반 개발 proxy·운영 API 경로, JSON 404, CORS 허용목록이다. Refactor에서 모든 컴포넌트의 직접 Axios 호출을 API 모듈로 옮기고 중복 토큰 인자를 없앴다. API 모듈은 경로·요청 변환, view는 입력·이동, client는 인증 헤더/실패 공통 처리만 맡는다. 공개 경로와 요청 payload는 기존 컴포넌트 테스트 및 실제 Axios adapter 테스트로 확인했다. B `clean test asciidoctor bootJar` 30 suite·189 test·실패 0, F 13 suite·38 test·실패 0, lint·build 성공(번들 크기 권고 경고). 프런트 커밋 `a3b4ff0`. 독립 TypeScript 객체는 아직 없다.

### T42 · 추천 표기와 미완성 반응 안내 — R13 / S12

수정 F: `src/components/question/QuestionMenu.vue`, `QuestionList.vue`, `QuestionDetail.vue`, `QuestionAnswer.vue`. 생성/수정 F 테스트: `tests/unit/QuestionDetail.spec.js`, `QuestionMenu.spec.js`, `QuestionAnswer.spec.js`.

- [x] 서버 `{recommend:3}` fixture가 3으로 표시되고 추천 메뉴가 orderBy=recommend를 전달하는 테스트를 작성한다.
- [x] recomend 오타를 통일하고 로컬 카운터 증가를 제거한다.
- [x] 반응 버튼이 disabled이며 저장되지 않는 기능임을 사용자에게 표시하는 테스트를 작성·통과시킨다. 실제 투표 API는 추가하지 않는다.
- [x] 목록·상세·답변을 모두 검증하고 전체 테스트/lint/build를 실행한다.

2026-09-26: Red `npm test -- --runInBand QuestionMenu QuestionDetail QuestionAnswer QuestionList`에서 `{recommend:3}`의 추천 표시가 비고 추천 메뉴가 `recomend`를 전달하는 assertion 실패를 확인했다. 메뉴 테스트의 초기 stub 오류를 고친 뒤 실제 정렬값 실패를 다시 확인했다. Green은 네 화면의 필드·정렬 이름을 `recommend`로 통일하고 질문·답변 반응 버튼에 `disabled`와 “반응 저장 기능 준비 중” 안내를 추가했다. Refactor에서는 저장 없이 숫자만 늘리던 상세 화면 메서드 두 개를 제거했다. 화면은 서버 값을 표시하며 반응 저장 책임을 가장하지 않는다. API·JSON 필드와 라우트 query는 컴포넌트 테스트로 검증했고, 독립 TypeScript 객체는 현재 없다. F 전체 16 suite·42 test, lint·build 성공(기존 번들 크기 권고 경고). 프런트 커밋 `07020c0`.

## Phase 5 · 도메인 언어와 책임 전수 검토 — R16 / S13

실행 브랜치: `refactor/domain-language-and-responsibility`. 백엔드 worktree: `demp/.worktrees/domain-language-and-responsibility/backend`(Phase 4 B `93ca588` 기준); 프런트 worktree: `dempfrontend/.worktrees/domain-language-and-responsibility/frontend`(Phase 4 F `07020c0` 기준). 두 브랜치는 생성 직후 Phase 4 완료 커밋까지 fast-forward했다. 완료 작업은 2026-09-26 사용자 요청에 따라 커밋한다.

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

- [x] 관계 연결·교체, 값 객체 불변식, 응답 매핑의 현재 공개 동작을 Domain/Repository/Controller 테스트로 먼저 고정한다. `QuestionHashtags`, `settingMember`, `getBuild`, `announcemnet`의 변경이 JPA 필드명·QueryDSL 생성형·JSON 키를 바꾸는지 확인한다.
- [x] 서로 연동되는 관계 변경은 소유 엔티티의 하나의 연산으로 모으고, 태그 탐색·저장은 도메인 엔티티로 옮기지 않는다. 도메인에서 상태 변경과 양방향 관계를 보장하는 테스트를 통과시킨다. 새 동작이 필요하면 그 사례만 별도 Red→Green으로 진행한다.
- [x] 이름을 의미에 맞게 바꾸고 호출부를 함께 정리한다. 외부 계약 변경이 불필요하면 명시적 JPA/JSON 매핑으로 호환성을 유지한다. 대상 및 `./gradlew test`를 실행한다.

2026-09-26: Red `./gradlew test --tests 'com.inhatc.demp.domain.QuestionTest' --tests 'com.inhatc.demp.domain.AnswerTest'`에서 작성자·질문 재지정 세 사례 모두 이전 객체 목록의 낡은 참조 때문에 실패했다. Green은 관계를 재지정할 때 이전 목록에서 제거하고 새 목록에 한 번만 추가하는 엔티티 연산이다. Refactor에서 질문의 `addHashtag`가 양방향 태그 연결을 한 번에 수행하게 하고 관계 setter를 package 범위로 제한했다. `settingMember/settingQuestion`은 `assignMember/assignQuestion`, `getBuild`는 `from`, `QuestionHashtags`는 `questionHashtags`, `announcemnet` 패키지는 `announcement`로 이름과 호출부를 바꿨다. 태그 이름 탐색·저장은 서비스/저장소에 유지했다. 관계의 JPA `mappedBy` 필드와 join column은 동일하고, DTO 필드·JSON 키는 변경하지 않았다. QueryDSL 재생성, Repository/Controller/REST Docs 및 값 객체 테스트를 포함한 `clean test asciidoctor bootJar`는 31 suite·192 test·실패 0으로 통과했다. 운영 DB 스키마 검증은 T49/T50에서 별도 환경 대상으로 남긴다.

### T47 · 서비스·저장소 메서드의 효과와 클래스 소속 — T32 이후

수정 후보 B: `service/QuestionService.java`, `AnnouncementService.java`, `MemberService.java`, `FileService.java`, `repository/question/QuestionQueryRepository.java`, `repository/announcement/AnnouncementQueryRepository.java`, `controller/MemberController.java` 및 호출부/관련 테스트. 실제 수정 목록은 T45 판정표로 확정한다.

- [x] `join`, `setHashtags`, `save`, `pageTest/pagingTest`, `validationDuplicateUsername`, `QuestionSort`, `getFullPath`의 호출부·반환값·부작용을 추적하고 유지/이름 변경/메서드 추출/클래스 추출 중 하나를 근거와 함께 결정한다.
- [x] 질문 생성/태그 관리, 공고 입력 변환/저장·보상, 파일 검증/전송이 각각 독립된 변경 이유를 가진 경우에만 협력 클래스를 추출한다. 새 클래스의 입력·출력·소유 규칙·트랜잭션/외부 효과를 판정표에 명시한다. 서비스는 유스케이스의 순서와 트랜잭션을 조정할 수 있으므로 단계 수만으로 분리하지 않는다.
- [x] 기존 결과·상태 코드·트랜잭션 commit·파일 삭제 보상 테스트를 먼저 실행해 기준선을 확보한다. 책임 이동 뒤 대상 테스트와 `./gradlew test`를 다시 실행하고, 동작 오류가 새로 발견되면 별도 실패 테스트를 먼저 작성한다.

2026-09-26: 기준선은 T46 완료의 `clean test asciidoctor bootJar` 192개 통과다. T47은 공개 동작을 바꾸지 않는 이름·책임 정리여서 새 기능 Red를 주장하지 않는다. `join`은 회원 등록/질문 생성/준비된 공고 엔티티 저장으로 구분했고, 요청 DTO 공고 저장은 `createAnnouncement`, 답변 저장·목록 반환은 `createAnswerAndList`로 명명했다. `validationDuplicateUsername`은 실제 반환값인 `isUsernameAvailable`, 운영 `pagingTest/getAnnounceScroll`은 `findAnnouncementPage/findAnnouncementSlice`로 바꿨다. T31의 `setHashtags`와 T32의 `QuestionSort`는 이미 제거되었음을 호출부에서 확인했다. 사용처가 없는 서비스 페이지 전달·전체 조회와 `getFullPath`를 제거했다. `HashtagResolver`는 `List<String> → List<Hashtag>` 정규화·조회·저장을 맡고 질문 서비스의 트랜잭션 안에서 호출된다. `ImageValidator`는 `MultipartFile → 검증된 확장자`의 무상태 파일 정책을 맡고 S3 전송은 `FileService`에 남긴다. 공고 DTO 변환·업로드·DB commit·실패 보상은 하나의 생성 유스케이스에 속하므로 `AnnouncementService`에 유지했다. Controller 테스트 이름을 HTTP 전달 assertion에 맞추고 답변 서비스 사례 세 개를 `AnswerServiceTest`로 옮겼다. 이 이름 변경은 HTTP route, JSON, DB 컬럼을 바꾸지 않는다. 파일 형식·업로드 보상·서비스 대상 테스트 통과, Refactor 후 전체 `./gradlew test` 31 suite·192 test·실패 0, 직전 `clean test asciidoctor bootJar` 성공.

### T48 · 프런트 도메인 이름과 상태·API 경계 — T40~T42 이후

수정 후보 F: `src/data/positon.js`, `src/router/index.js`, `src/components/announcement/AnnouncementList.vue`, `src/api/*`, `src/store/*`, `src/fontAwesomeIcon.js`, 미사용 예제 컴포넌트 및 관련 테스트. 실제 수정 목록은 T45 판정표로 확정한다.

- [x] 모든 Vue/JavaScript 파일에서 컴포넌트명·props·emits·상태 필드·함수명·라우트명을 화면 도메인과 실제 효과에 대조한다. `positon`, `Test*`, `loadDataFromServer` 후보의 외부 참조를 확인한다.
- [x] 비동기 요청·상태 전이·렌더링이 한 메서드에 섞인 경우 API 호출은 API 모듈, 공유 상태 전이는 store, 표현은 컴포넌트에 배치할지 결정한다. 단일 화면의 지역 상태만 필요한 경우 불필요한 store 추출을 하지 않는다.
- [x] 사용자에게 보이는 결과와 라우트/이벤트 계약을 컴포넌트 테스트로 고정한 뒤 이름·책임을 바꾼다. F 전체 `npm test -- --runInBand`, `npm run lint -- --no-fix`, `npm run build`를 실행한다.

2026-09-26: 기존 동작의 Green 기준으로 공고 항목 클릭 `/detail/71`, 태그 입력 `addHashtags`, `/login`·`/account` 공개 경로 테스트를 추가하고 대상 9개를 먼저 통과시켰다. 기능 동작을 바꾸지 않는 이름·소속 정리이므로 별도 Red를 주장하지 않는다. `positon`→`positions`, `Test*` 라우트 이름과 `loadDataFromServer`·`printCondition`·`AccountMethod`·`DetailAnnounce` 등 내부 이름을 효과에 맞게 바꿨다. `Hashtags`의 동기 입력 로직에서 불필요한 `async/await`를 제거했다. Vue 2 방식이며 import되지 않는 Font Awesome 설정, 예제 `/hello` 컴포넌트와 주석 처리된 공고 footer는 사용처 검사 후 제거했다. T40/T41에서 API 호출은 모듈에 모였고 목록 상태는 두 화면의 지역 상태라 store 추출을 하지 않았다. props/event 이름과 제품 URL은 유지했고, 라우트·event·화면 결과는 컴포넌트 테스트로 확인했다. 독립 TypeScript 객체는 현재 없다. F 17 suite·45 test, lint·build 성공(기존 번들 크기 권고 경고). 프런트 커밋 `fd26a50`.

### T49 · 감사 누락·공개 계약 재검증

수정 B: `docs/verification/domain-language-and-responsibility/inventory.md`, `tasks.md`. 필요한 경우 B/F의 테스트·API 문서.

- [x] T45 파일 목록을 현재 양쪽 추적 대상 파일과 재대조하고 모든 판정·후속 조치·유지 이유를 닫는다. 새로 생긴 파일도 이름·효과·소속 책임을 검사한다.
- [x] REST Docs/JSON 키, DB 매핑, 라우트, 파일 저장 경로, 검색·페이지 결과를 변경 전 계약과 대조한다. 의도한 변경은 양쪽 테스트와 문서에 반영한다.
- [x] B `./gradlew test`와 `./gradlew asciidoctor`, F 전체 테스트/lint/build를 실행하고 실제 명령·결과를 기록한다. 정적 위험과 실행 재현 결과를 구분한다.

2026-09-26: [현재 파일별 161행과 N01~N15 종료 판정](docs/verification/domain-language-and-responsibility/inventory.md)을 T45 목록 및 양쪽 실제 파일에 재대조해 누락·초과 0개를 확인했다(B Java 102, F Vue/JS 53, 빌드·실행 6). Red `./gradlew test --tests 'com.inhatc.demp.service.AnnouncementImageUrlTest'`는 CDN 설정 주소가 빠진 URL assertion 실패였다. Green은 `AnnouncementImageUrl`이 설정 접두부와 저장 키를 연결하게 했다. Refactor에서 Repository는 공고 엔티티 Slice만 반환하고 Service가 상세·목록·스크롤 DTO의 `image`를 조립하게 했다. 중복된 허용 전체 `WebConfig`를 제거하고 미사용 DTO 두 개를 삭제했다. `CustomUserDetailService`의 Spring 고정 메서드명은 유지하고 매개변수를 회원 ID 문자열로 고쳤다. 작성 요청의 `username`은 기존 wire 계약 때문에 유지하며 인증 주체로 사용하지 않는다. 설정을 바꾼 서비스 테스트와 REST Docs 세 응답의 이미지 URL assertion, CORS 허용/거절, Repository 페이지 테스트가 통과했다. 상세 fixture 누락으로 난 NPE는 테스트 데이터 준비 오류로 바로잡았으며 기능 Red로 계산하지 않는다. B `clean test asciidoctor bootJar` 32 suite·194 test·실패 0, 생성 HTML에 세 공고 응답의 기존 URL 확인. F 17 suite·45 test, lint·build 성공(기존 번들 크기 권고 경고). `spec.md`에 초기 관찰과 현재 계약의 차이를 명시했다. 운영 DB 실물 스키마와 브라우저 E2E는 실행하지 않았고 T50에서 확인한다.

## Phase 6 · 통합 검증과 인수

작업 브랜치(양쪽 저장소): `refactor/integration-ci-and-user-flows`. worktree: B `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/integration-ci-and-user-flows/backend`, F `/Users/seungmin/Desktop/repo/archive/dempfrontend/.worktrees/integration-ci-and-user-flows/frontend`. Phase 5 완료 커밋 B `3c0ac02`, F `fd26a50`에서 시작한다.

### T50 · 전체 사용자 흐름과 CI — S01~S13

생성 B/F: `.github/workflows/ci.yml`. 생성 F: `tests/e2e/community.spec.js`, `playwright.config.js`. 수정 B/F: README.md. 외부 시스템은 격리된 테스트 DB/파일 저장 대역을 사용한다.

- [x] 양쪽 CI에서 lockfile 설치, B `./gradlew test`, F 테스트/lint/build를 실행한다. 실패 단계에서 pipeline이 종료되는지 확인한다.
- [x] B REST Docs 테스트도 전체 test에 포함하고 `./gradlew asciidoctor`로 API HTML을 생성한다. 문서 테스트 실패·누락 snippet·내부 정보가 포함된 예제가 있으면 인수하지 않는다.
- [x] 회원가입→로그인→공고 필터→상세→질문 작성→답변→별도 재조회 흐름의 E2E를 작성한다.
- [x] 다른 회원의 수정 거절, 만료 로그인, HTML 콘텐츠, 검색 응답 역전, 새로고침 후 상태를 추가한다.
- [x] 테스트 fixture를 독립적으로 생성·정리하고 생산 데이터/운영 S3 접근이 없는지 확인한다.
- [x] 아래 명령 결과를 날짜·실제 건수와 함께 기록한다. 실행하지 않은 명령을 통과로 적지 않는다.

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

- [x] 변경된 API fixture와 spec.md를 대조하고 양쪽 동시 배포/rollback 순서를 README에 기록한다. GitHub push·배포는 이 문서 작성 요청의 실행 범위에 포함하지 않는다.

2026-09-26: [T50 검증 기록](docs/verification/integration-ci-and-user-flows/README.md)에 실제 Red·Green·Refactor, API fixture 대조, 정적 CI 판정과 실행 결과를 기록했다. B `clean test` 33 suite·195 test·실패 0, `asciidoctor` HTML·누락 참조·내부 키 검사 통과. 별도 임시 H2 Spring API 점검 14개 요청으로 공고·가입·로그인·질문·답변 재조회 및 403/401을 확인했다. F `npm ci` 성공, 17 suite·45 test, lint·build, Chromium E2E 4개 통과. 실제 GitHub Actions 실행과 브라우저→실제 Spring 단일 연결 실행은 하지 않았으며 각각 workflow 구성 및 분리된 브라우저/API 검증으로 범위를 명시했다. 프런트 커밋 `4cded55`. Phase 7 버전 갱신과 TypeScript 체크리스트는 T50 이후 작업으로 유지한다.

## Phase 7 · 런타임·프레임워크 갱신과 TypeScript 도입 (T50 완료 후)

실행 브랜치: `refactor/runtime-framework-and-typescript-upgrade`. 백엔드·프런트엔드에 각각 같은 이름의 worktree를 만들고, T50의 통과 기준 커밋에서 시작한다. 아래 항목은 후속 작업 체크리스트이며 버전과 호환성은 시작 시 공식 지원표를 확인해 결정한다.

### T60 · 기준선과 업그레이드 순서

- [x] 현재 Java/JDK, Gradle, Spring Boot 및 의존성, Node/npm, Vue/CLI, Jest, 브라우저 지원 범위와 배포 환경을 기록한다.
- [x] 공식 호환성·지원 종료 정보에 맞춰 목표 버전을 정하고, 중간 버전 경유가 필요한 변경 순서를 기록한다.
- [x] T50 전체 검증과 핵심 사용자 흐름을 기준선으로 재실행하고 결과를 남긴다.

2026-09-27: [T60 기준선·호환성 기록](docs/verification/runtime-framework-and-typescript-upgrade/README.md). 별도 worktree, asdf Zulu25/Node24, B 195·F45·headed E2E4 및 실제 API14 통과.

### T61 · Java·Spring 업그레이드

- [x] Java, Gradle, Spring Boot를 호환되는 순서로 올리고 빌드·테스트 실패를 각각 원인별로 해결한다.
- [x] Security/JPA/validation/REST Docs 설정과 인증, 트랜잭션, 페이지네이션, 파일 업로드 계약을 회귀 테스트로 검증한다.
- [x] `clean test asciidoctor bootJar`와 운영 프로필 시작·종료를 확인하고 배포 JDK 설정을 갱신한다.

2026-09-27: [T61 기록](docs/verification/runtime-framework-and-typescript-upgrade/README.md). Zulu25/Gradle9.8/Boot4.1.1 전환, 기존 sequence/enum 보존 Red→Green, H2 upstream CHECK 오류 수정. 전체 34 suite·197 test, REST Docs·bootJar 성공. 현재 cmux에서 실제 API14·브라우저 가입/로그인 확인. 운영 설정 재시작은 격리 H2로 검증했으며 운영 MySQL은 미접속.

### T62 · Node·Vue 업그레이드

- [x] 지원되는 Node/npm 버전과 Vue 생태계(Vue, Router, Vuex 또는 대체 상태 관리, CLI 또는 빌드 도구, Jest 또는 대체 러너)의 목표 조합을 결정한다.
- [x] lockfile을 갱신하고 `npm ci`, 컴포넌트 테스트, lint, build, E2E를 순서대로 실행한다.
- [x] 인증 만료, 검색·페이지 응답 역전, 에디터·태그 입력, 공고 더보기의 실제 브라우저 흐름을 확인한다.
- [x] 사용자 추가 요청: 여러 페이지 분량의 격리 데이터로 인피니티 스크롤을 실제 cmux에서 확인한다. 다음 페이지 1회 요청·중복 방지·필터 변경 시 초기화·응답 역전·마지막 페이지·오류 재시도를 자동 E2E에도 고정한다.

2026-09-27: Node24.21/Vue3.5.43/Router5.3.1/Vite8.3.1/Vitest5.0.2, npm ci333개. 기존45개와 스크롤 Red→Green3개로 unit48개, lint/build 통과. 자동 E2E7개 exit0. 현재 cmux 내장 브라우저와 실제 Spring/H2에서 8→16→24→28개, 교육 필터8→10개 확인. 실제 에디터·태그 입력/저장/재조회 및 인증 만료 이동 확인. [자세한 기록](docs/verification/runtime-framework-and-typescript-upgrade/README.md).

### T63 · TypeScript 단계적 도입

- [x] `tsconfig`, 타입 검사 명령, Vue SFC 타입 지원, CI 필수 검사를 마련하고 작은 API 모듈 하나로 실패→통과를 확인한다.
- [x] API 요청/응답 DTO, 인증 상태, 라우트, 비동기 목록 상태를 경계부터 타입화한다. `any` 사용 이유와 남은 JS 범위를 기록한다.
- [x] Vue 컴포넌트를 기능 단위로 옮기며 props/emits와 nullable 응답을 검증한다. 각 단위마다 테스트·타입 검사·lint·build를 실행한다.
- [x] 양쪽 계약 fixture, 전체 E2E, 배포·롤백 절차와 README를 갱신하고 최종 전체 검증 결과를 기록한다.

## Phase 8 · 제공 디자인 적용과 채용·부트캠프 탐색 개선

사용자 제공 `/Users/seungmin/Downloads/dempfrontend-redesigned.zip`을 디자인 기준으로 삼는다. **앱 이름은 DEMP로 유지**한다(화면·로고·문서 제목·메타데이터 포함). Phase 7의 완료 지점에서 양쪽 저장소의 `refactor/redesigned-discovery-and-bootcamp-filters` 브랜치와 별도 worktree로 시작한다. ZIP의 구형 인증·API·상태 코드는 가져오지 않고 Phase 7 코드에 디자인을 이식한다. 참고 사이트는 [원티드](https://www.wanted.co.kr/wdlist/518), [점핏](https://jumpit.saramin.co.kr/positions), [부트텐트](https://boottent.com/camps)이며 콘텐츠나 상표를 복제하지 않는다.

2026-09-27 시작: 양쪽 브랜치 `refactor/redesigned-discovery-and-bootcamp-filters`, worktree `.worktrees/redesigned-discovery-and-bootcamp-filters/{backend,frontend}`. 기준 B3da14a5/F ac5b7fd. [실행 계획](docs/verification/redesigned-discovery/plan.md).

### T70 · 디자인 기준·공통 화면·Markdown 작성기

- [x] 공고 목록·상세의 기술 배열을 `Java, Spring`처럼 쉼표로 구분해 표시하고 따옴표·괄호를 제거한다. ISO 날짜의 T/초를 숨기고 날짜·시간과 빈값을 공통 표시 함수로 정리해 검증한다.
- [x] 추천·비추천·조회수·회원 이모지를 통일된 SVG 아이콘·아바타로 교체하고 숫자 배치, 접근 가능한 이름, 선택/비활성/포커스 상태 검증.

- [x] ZIP과 현재 라우트 전수 대조, 부족한 페이지·상태 목록과 디자인 계획을 기록한다.
- [x] ZIP의 색상·타이포·여백·카드·헤더를 공통 토큰으로 정리하고 DEMP 이름을 보존한다.
- [x] 로그인·가입·공고·질문/답변·작성 화면에 동일한 디자인을 적용한다. 기존 공개 계약과 보안 동작을 검증한다.
- [x] 사용자 추가 요청: 글쓰기 화면과 낡은 에디터를 교체한다. 공통 Markdown 작성기(제목/굵게/목록/링크/코드 도구, 작성·미리보기, 데스크톱 분할 보기, 모바일 입력/저장)를 질문·답변·공고에 적용한다. 기존 HTML 표시와 HTML↔Markdown 변환/XSS 방어를 단위 검증한다. 기존 HTML의 실제 편집·저장 UI 검증은 T81 관리자 수정 화면에서 수행한다.
- [x] 모바일·키보드 포커스·명도 대비·reduced-motion을 확인하고 전체 unit/typecheck/lint/build 후 체크·커밋한다.

2026-09-27: [T70 실행·검증 기록](docs/verification/redesigned-discovery/t70.md). B205/F75, 타입·lint·build, 자동 E2E7 및 현재 cmux 실제 작성/저장/재조회 통과.

### T71 · 공고·부트캠프 필터

- [x] 원티드·점핏·사람인·잡코리아와 네이버·카카오·토스·당근의 자체 채용 페이지 등 다수 사례를 조사한다. 필터·공고 카드·상세 구조·지원 동선·모바일·모집 상태별 장점과 DEMP 반영/제외 이유를 비교표에 남긴다. 부트캠프는 부트텐트를 별도 참고한다. DEMP가 저장하는 필드와 대조해 조건별 지원 범위를 확정한다.
- [x] 채용과 교육의 필터를 각각 설계한다. 선택 조건 칩·개별 해제·전체 초기화·결과 안내·모바일 필터를 구현한다.
- [x] URL에 필터를 저장해 새로고침·뒤로 가기·링크 공유 때 동일한 결과가 나오게 한다.
- [x] API에서 지원하지 않는 필터가 필요하면 저장·작성·조회 계약을 먼저 추가한다. 일부 로드된 카드만 거르거나 없는 정보를 추측하지 않는다.
- [x] 필터 조합·페이지 초기화·응답 역전·오류 재시도·빈 결과를 Red→Green으로 검증하고 양쪽 전체 검증 후 체크·커밋한다.

2026-09-27: [T71 검증 기록](docs/verification/redesigned-discovery/t71.md). B211/F80, 타입·lint·build·REST Docs 및 cmux pane의 E2E8 통과.

### T72 · 누락 화면과 최종 사용자 흐름

- [x] 404·로딩·빈 결과·권한/서버 오류·재시도 화면을 디자인에 맞춰 완성한다.
- [x] 기존 headed E2E를 유지하고 필터 복원·채용/교육 전환·모바일 사용자 흐름을 추가한다.
- [x] 현재 cmux pane에서 실제 API 연결과 브라우저 클릭/입력으로 주요 흐름을 확인한다. 화면별 데스크톱·모바일 캡처를 점검한다.
- [x] 양쪽 전체 테스트·typecheck·lint·build·REST Docs와 최종 리뷰를 완료하고 체크·커밋한다.

## Phase 9 · DEMP 관리자 페이지와 서버 권한

2026-09-27 사용자가 관리자 페이지 부재를 지적해 범위에 추가했다. 양쪽 `refactor/admin-console-and-authorization` 브랜치·worktree를 Phase 8 완료 지점에서 만든다. 앱 이름은 DEMP이며 ZIP 디자인 토큰을 공유한다. 기본 범위는 운영 현황, 공고·부트캠프 CRUD, 질문·답변 조회·관리다.

### T80 · 관리자 접근 계약

- [x] 기존 역할·로그인·가입 계약을 대조하고 `ROLE_ADMIN`과 관리자 확인 API, `/admin` 라우트의 인증/권한 경계를 설계한다.
- [x] 일반 회원이 관리자 역할을 요청하거나 토큰/localStorage를 변조해 승격할 수 없게 한다. 관리자 역할은 서버 저장값으로 검증한다.
- [x] 관리자 계정 준비 절차를 문서화한다. 공개 가입을 통한 관리자 생성이나 운영용 기본 비밀번호를 두지 않는다.
- [x] 비로그인 401·일반 회원 403·관리자 성공·권한 회수 후 거절 테스트를 Red→Green으로 통과시키고 체크·커밋한다.

2026-09-27: [T80 기록](docs/verification/admin-console/t80.md). 별도 관리자 worktree, B214/F101 전체 검증 통과.

### T81 · 관리자 운영 화면과 API

- [x] 실제 DB 집계 기반 현황, 공고/부트캠프 목록·검색·등록·수정·삭제, 질문/답변 목록·상세·관리 화면을 구현한다.
- [x] 기존 회원의 자기 글 수정·삭제 권한과 별개로 관리자 관리 유스케이스를 명시한다. 파일 정리·연관 데이터·트랜잭션 계약을 검증한다.
- [x] 삭제 확인, 저장 중 중복 제출 방지, 필드 오류, 빈 목록, 서버 오류/재시도를 제공한다.
- [x] 관리자 API REST Docs·DTO·프런트 타입을 함께 갱신하고 양쪽 전체 검증 후 체크·커밋한다.

2026-09-27: [T81 기록](docs/verification/admin-console/t81.md). B229/F106 전체 검증과 REST Docs 통과.

### T82 · 관리자 인수 검증

- [x] 직접 URL 접근·권한 만료·다른 역할·일반 사용자 회귀를 headed E2E로 확인한다.
- [x] 현재 cmux 브라우저에서 격리된 관리자 fixture로 등록→수정→별도 재조회→삭제 흐름을 보여준다. 운영 데이터에는 접근하지 않는다.
- [x] 데스크톱·모바일 관리자 화면, 전체 unit/typecheck/lint/build/REST Docs/E2E와 최종 리뷰를 통과한 후 체크·커밋한다.

2026-09-27: [T82 기록](docs/verification/admin-console/t82.md). 실제 cmux CRUD·HTML/이미지 보존, B231/F124·headed E2E16 통과.

### T83 · 공고 모집 대상 표시

- [x] 이미지 기능 유지. 카드·상세·관련 공고에 모집 구분 배지와 경력 연차를 공통 기준으로 표시한다.
- [x] 교육, 경력 무관(0~상한 없음), 신입·경력(0~N), 경력(N~M/상한 없음), 정보 누락을 구분하고 교육에 경력 문구를 붙이지 않는다.
- [x] 관련 공고 API에 모집 유형·경력 범위를 추가하고 REST Docs·타입·표현 테스트를 갱신한다.
- [x] 단위/전체 검증 및 현재 cmux에서 모바일·데스크톱 표시를 확인한 후 체크·커밋한다.

2026-09-27: [T83 기록](docs/verification/admin-console/t83.md). B231/F114, headed E2E15 통과.

### T84 · 상황별 빈 결과 안내

- [x] 전체·채용·부트캠프/교육과정의 미등록, 필터 결과 없음, 검색어 결과 없음을 구분한다.
- [x] 로딩·실패·다음 페이지 실패·목록 끝을 구분하고 기존 결과를 보존한다.
- [x] 검색·필터 해제로 현재 종류 탭을 유지하며 목록 복구, 등록된 종류가 없으면 전체 공고 이동을 제공한다.
- [x] 실제 Red→Green, 전체 프런트 검증, 현재 cmux 빈 교육 검색/복구, headed E2E 통과 후 체크·커밋한다.

2026-09-27: [T84 기록](docs/verification/admin-console/t84.md). F124·typecheck/lint/build, headed E2E16 통과.

### T85 · 국내 서비스 등록 운영 조사와 DEMP 점검

- [x] 잡코리아·사람인·인디스워크·부트텐트의 공식 안내로 작성 주체/작성 도구/검수/수정·삭제 흐름을 비교한다.
- [x] 공개 확인 사실과 비공개 편집기·자동수집에 대한 미확인 사항을 구분한다.
- [x] 기존 일반 공고 등록 API와 관리자 등록, 도메인/DTO/폼을 대조하고 우선순위를 기록한다.
- [x] [조사·점검 보고서](docs/research/recruitment-publication-workflows.md)에 출처·코드 근거·후속 체크리스트를 남긴다.

### T85A · 직행 등록·수집 운영 추가 조사

- [x] 직행 공식 앱 소개와 실제 목록·상세에서 외부 공고 집계·출처·요약/원문 표시를 확인한다.
- [x] cmux에서 기업 서비스·회원가입 화면을 확인하고 기업 직접 등록과 광고 신청을 구분한다.
- [x] 공개 사실과 미확인 수집 기술·편집기·검수 절차를 나눠 [비교 보고서](docs/research/recruitment-publication-workflows.md)에 추가한다.

2026-09-27: 기업 무료 등록·기업 정보 관리 안내, 사업자등록증/업무용 이메일 인증 입력, 별도 광고 폼 확인. 계정 생성·신청 제출 없음. 문서와 출처 정합성 및 `git diff --check` 검증. 코드 변경이 없어 애플리케이션 테스트 재실행 없음.

#### 조사 후속 항목 · T92–T96 반영 결과

- [x] 초기 게시 주체를 운영자 전용 또는 승인된 기관 담당자+검수로 결정하고 일반 등록 API의 권한을 일치시킨다.
- [x] 게시 상태(초안/검수/공개/비공개)와 모집 상태를 분리한다. 원문 URL·지원 URL·작성자·기관·확인일·변경 이력을 설계한다.
- [x] 채용/교육 전용 입력 항목·본문 템플릿을 분리하고 교육 기간·기수·수업 방식·지원금 등을 구조화한다. (T88/T90 본문·일정·수업 방식, T94 기수·지원금 완료.)
- [x] 신입/경력/무관/혼합 모집 대상을 명시적으로 저장하고 연차의 상한 없음과 구별한다.
- [x] 제목 전역 중복 거절을 기관/원문/기수 기준으로 재설계한다.
- [x] 이미지 기능은 유지하면서 기관 로고 재사용·선택 업로드·기본 이미지와 헤더 크롭 정책을 결정한다.
- **조건부 미도입:** 기관 직접 제출은 현재 범위에서 도입하지 않는다. 도입 시 소속 승인·기관 권한·반려/재제출·게시본 유지 규칙을 별도 구현한다.
- [x] 외부 공고 도입 시 등록 경로·원문 플랫폼/식별값·확인일, 중복·변경·마감 처리, 운영자 수정 보존·정보 누락 검토·오류 제보를 설계한다. 광고 노출 관리는 일반 게시와 구분한다.
- [x] 확정한 동작마다 TDD 및 현재 cmux 등록→검수→게시→수정/종료 검증을 수행한다.

근거: [운영 정책](docs/plans/curated-publication-policy.md), [T96 QA](docs/verification/publication-workflow/t96-qa.md).

### T86 · 원문 지원 링크와 선택 대표 이미지

- [x] 원문 공고 URL을 등록하고 지원하기가 해당 주소를 새 탭으로 여는 계약을 검증한다.
- [x] 대표 이미지 없이 등록·목록·상세·관련 공고 조회가 가능하고 기존 이미지는 유지한다.
- [x] 테스트 Red→Green, 전체 검증, 체크·커밋.

### T87 · 공고 본문 이미지 저장

- [x] 본문 이미지를 공고 저장 요청과 함께 업로드하고 안전한 HTML·이미지 참조를 저장한다.
- [x] 수정 시 유지/삭제, 공고 삭제 시 정리, 실패 시 업로드 보상을 검증한다.
- [x] 외부 HTML의 이미지를 자동 수집하지 않고 기존 본문 서식을 보존한다.
- [x] API 문서·테스트·검증 기록 후 체크·커밋.

검증 기록: [T87](docs/verification/admin-console/t87.md).

### T88 · 서식 편집과 원문 이동 인수

- [x] 채용/교육 섹션, 텍스트 붙여넣기 정돈, 이미지 파일·붙여넣기·드래그, 모바일 미리보기를 제공한다.
- [x] 실패 시 입력/첨부 보존, 중복 제출 방지, 수정 재조회 보존을 검증한다.
- [x] 현재 cmux terminal12/browser13에서 등록→수정→상세→지원하기 원문 이동을 확인한다.
- [x] 전체 unit/typecheck/lint/build/REST Docs/headed E2E와 리뷰 후 체크·커밋.

검증 기록: [T88](docs/verification/admin-console/t88.md).

작업 위치: Phase 9 후속, `refactor/admin-console-and-authorization`, `.worktrees/admin-console-and-authorization/{backend,frontend}`. 사용자 승인: 텍스트 기본 본문+선택 이미지+원문 링크, 지원하기는 원문으로 이동. 기존 `accessUrl`을 원문 URL로 사용해 저장 계약을 유지한다. 자동 수집·기업 제출/검수 시스템은 별도 후속 항목이다.

## Phase 10 · 선택 연봉과 교육과정 탐색

작업 위치: `refactor/compensation-and-education-filters`, `.worktrees/compensation-and-education-filters/{backend,frontend}`. 기준 B `db4ecfc`, F `352ae74`. 사용자는 연봉 필터 숨김/상세 표시를 선택했고 부트텐트 참고 상세 교육 필터 구현을 요청했다.

### T89 · 연봉 선택 정보와 금액 미확인 구분

- [x] 연봉 필터·칩·URL/API 조건 제거, 상세에서 미공개/협의/공개 연봉 범위 표시.
- [x] 양쪽 등록 폼에 선택 연봉과 교육비 미확인/무료/유료 구분, null 금액 보존.
- [x] 기존 양수 금액 호환, 잘못된 범위 거절, DB·REST Docs·전체 테스트 후 커밋.

검증 기록: [T89](docs/verification/education-discovery/t89.md).

### T90 · 교육 조건 저장과 상세 검색

- [x] 부트텐트 상세 필터 조사 근거와 DEMP 적용 기준 기록.
- [x] 수업 방식·지역·참여 시간·지원 유형·선발 방식·학습 수준·교육 일정 등록/재조회.
- [x] 해당 조건과 시작일·기간 검색을 DB 쿼리/페이지/카운트에 일관되게 적용, 미확인 항목은 필터에 임의 포함하지 않음.
- [x] 반응형 필터·URL 복원·개별 해제·초기화·구체적인 빈 결과·상세/카드 표시.
- [x] 현재 cmux에서 실제 등록→검색→수정 흐름, headed E2E·전체 검증·리뷰 후 커밋.

검증 기록: [T90](docs/verification/education-discovery/t90.md).

### T91 · QA 스킬 기반 실제 사용자 흐름 점검

- [x] 현재 cmux에서 연봉/교육 등록·수정, 필터·URL 복원, 빈 결과, 오류 안내, 반응형 화면을 탐색 검증한다.
- [x] 로그인·권한·무한 스크롤 등 기존 자동 E2E를 headed로 실행하고 실제 서버 검증과 구분한다.
- [x] 발견 결함은 재현·Red→Green으로 수정하고 QA 보고서·완료 체크·커밋을 남긴다.

작업 위치는 Phase 10 후속의 기존 `refactor/compensation-and-education-filters` worktree다. QA 스킬의 원격 Firecrawl 수집은 로컬 주소에 직접 접근할 수 없어 사용자 지정 cmux 브라우저로 대체한다.

## Phase 11 · 운영자 중심 외부 공고 게시 마무리

브랜치: `refactor/curated-publication-workflow`, worktree: `.worktrees/curated-publication-workflow/{backend,frontend}`. Java zulu-25.36.205 / Node24.21.0 고정, 현재 cmux workspace:2 surface12/13 재사용. [실행 계획](docs/plans/curated-publication-workflow.md).

### T92 · 등록 권한 통일
- [x] 기존 공고 등록 API도 ADMIN으로 제한하고 프런트 진입을 관리자 등록으로 통합한다.
- [x] 회원/관리자/비로그인 경계 Red→Green, 전체 검증 후 체크·커밋.

검증: [T92](docs/verification/publication-workflow/t92.md).

### T93 · 게시 상태와 출처·감사 기록
- [x] 신규 공고는 초안, 운영 검토/공개/비공개 구분, 공개 조회는 게시 허용 공고만 반환.
- [x] 원문/별도 지원 URL·출처 식별·확인일·작성자/수정자·변경 이력 저장과 관리자 UI.
- [x] 기존 공개 데이터 보존, 관리자 조회와 공개 조회 분리, 전체 검증 후 체크·커밋.

검증: [T93](docs/verification/publication-workflow/t93.md).

### T94 · 모집 대상·교육 기수·지원금과 중복 기준
- [x] 명시적 신입/경력/무관/혼합 저장, 기존 데이터의 불명확한 신입 추정 금지.
- [x] 교육 기수·지원금 금액/조건 입력·조회, 연봉/교육비와 혼용 방지.
- [x] 제목 중복을 폐지하고 원문 URL·기관·기수 기준 중복 검증, 동시성 보장.
- [x] 기존 호출부·스키마·검색 계약 검증 후 체크·커밋.

검증: [T94](docs/verification/publication-workflow/t94.md).

### T95 · 운영 확인·마감·오류 제보와 이미지 정책
- [x] 운영자 원문 확인/수정/마감 처리, 사용자 오류 제보→관리자 확인·처리.
- [x] 이미지 선택 업로드·기본 이미지·크롭과 기관 로고 재사용 정책 확정/문서화.
- [x] 자동 수집/광고/기관 직접 제출의 도입 여부를 명시하고 현재 운영 흐름과 구분.
- [x] 실패·권한·미확인 정보·운영자 수정 보존 검증 후 체크·커밋.

검증: [T95](docs/verification/publication-workflow/t95.md), [운영 정책](docs/plans/curated-publication-policy.md).

### T96 · 최종 인수와 통합
- [x] T91 QA를 현재 cmux에서 완료하고 독립 리뷰 중요 결함을 수정한다.
- [x] 남은 체크리스트를 구현 완료/조건부 미도입/사용자 보류로 정확히 정리한다.
- [x] 최종 전체 검증 후 브랜치를 보존하고 기존 요청에 따라 main 병합·push한다. 2026-09-27 B merge `e255364`, F main `d26ab49`; 두 원격 main 및 `refactor/curated-publication-workflow` push 완료.

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

현재 상태(2026-09-28): Phase 0~12 및 마지막 보류 항목 T97 생성자 제한·builder 통일 완료. QA·전체 검증·main 병합·push 완료. 당시 미체크 항목 없음. 이후 사용자 요청한 Phase 13~15는 아래 별도 추적. 작업 브랜치/worktree 유지. 상세 검증과 기능별 의도적 범위는 각 Phase 기록 참조.

### 2026-09-21 Phase 1 리뷰 반영

작업 위치는 사용자 리뷰를 위해 전환한 원본 `/Users/seungmin/Desktop/repo/archive/demp`, 브랜치는 `refactor/data-preservation-and-security`다. 이전 worktree는 보관본이며 현재 리뷰 수정의 기준은 원본이다. README 변경을 원복하고 Flyway/migration을 제거했다. 회원 가입·로그인 응답 조립은 서비스로 이동하고, 질문 상세의 도달 불가능한 null 분기와 완료된 임시 회원 TODO를 제거했다. 답변 배열 응답의 wrapper 변경은 기존 계획에 없으며 현재 계약을 유지한다. 상세 검증은 [리뷰 수정 기록](docs/verification/data-preservation-and-security/review-2026-09-21.md)을 참조한다.

- [x] T97: 생성자 제한·builder 통일. 2026-09-28 사용자 재개 지시로 Phase 12에서 수행한다. 기준선 271개 통과 후 도메인 생성 경로를 통일했다. JPA 기본 생성자 및 Spring/Jackson 바인딩 요구를 보존한다. 기존 private Announcement 생성자는 유지한다.

리뷰 수정 최종 검증: H2 백엔드 144개 테스트와 clean test asciidoctor bootJar 통과. REST Docs 91개 참조 누락 없음. 프런트는 이번 리뷰에서 변경하지 않았다.

## Phase 12 — 도메인 생성 경로 통일 (2026-09-28)

계획: [controlled-domain-construction](docs/plans/controlled-domain-construction.md)
브랜치: `refactor/controlled-domain-construction`, worktree: `.worktrees/controlled-domain-construction/backend`.

- [x] T98: 전체 테스트·빌드·현재 cmux의 생성 흐름 QA·최종 리뷰 후 main 병합·push. 브랜치와 worktree 보존.

T97 결과: 공개 생성자/JPA 접근 계약 Red 28건 중 17건 assertion 실패 → private 생성자 builder와 protected JPA 기본 생성자로 Green. 전체 299건(기존 271+계약 28) 실패·스킵 0, REST Docs·bootJar 통과. 모든 호출부를 builder로 이전했고 DTO 바인딩 생성자와 Question.addHashtag의 양방향 관계 조립은 유지했다. 별도 리뷰 중요 결함 없음, 제안된 테스트 줄바꿈 개선 반영 후 299건 재통과. [상세 검증](docs/verification/controlled-domain-construction/t97-t98.md).

T98 결과: 현재 cmux workspace:2의 기존 보조 pane에서 회원 가입→로그인→질문→답변→새로고침 검증 완료. 실제 HTTP 공고·교육·제보·게시 이력·중복/권한/빈 검색 assertion 종료 0. T97 `f545e10`, main 병합 `31a0ebf` 및 원격 push 완료. 병합된 main 전체 299건 실패·스킵 0 재확인. 프런트 변경 없음(직전 QA 기록 유지). 브랜치/worktree와 QA pane 보존.

## Phase 13 — 질문·답변 반응 저장

브랜치 `refactor/persistent-content-reactions`, worktree `.worktrees/persistent-content-reactions/{backend,frontend}`.

- [x] T99: 회원별 추천·비추천·취소 저장, 재시도 멱등성, 동시성, 삭제 수명주기, 조회·REST Docs·수동 SQL 검증.
- [ ] T100: 질문·답변의 내 반응 표시, 저장 중 연타 차단, 실패·재시도·라우트 변경 보호.
- [ ] T101: 전체 검증·현재 cmux 실제 QA·fresh review·커밋·main 병합/push.

## Phase 14 — 최초 리팩터링 이전과 성능 비교

- [ ] T102: 최초 리팩터링 직전 기준 커밋과 실행 가능한 최소 복원, 동일 가상 데이터/환경/시나리오 정의.
- [ ] T103: 데이터 규모별 응답시간·처리량·SQL 수 반복 측정, 병목 개선과 재측정, 원시 결과·한계·재현 절차 기록. 개선/악화 모두 공개.

## Phase 15 — 포트폴리오 문서 정리

- [ ] T104: 양쪽 README 전면 개편: 현재 화면·제품 흐름·설계·실행·테스트·개선 수치·한계·관련 저장소. 과거 정보와 현재 상태 분리.
- [ ] T105: 문서 링크/명령 검증, 최종 QA·리뷰·브랜치 보존·main 병합/push·전체 체크.

T99: B314건·REST Docs·bootJar 통과. API 404 Red와 본문 수정 경쟁 조건 Red 재현 후 Green. [상세 기록](docs/verification/persistent-content-reactions/t99-t101.md).
