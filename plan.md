# DEMP 개선 계획 및 코드 리뷰

> **For agentic workers:** 실행 시 superpowers:executing-plans를 사용하여 tasks.md의 작은 작업 단위로 진행한다. 이 문서는 실행 완료 보고가 아니다.

**Goal:** GitHub 기준 DEMP의 실행 기반을 복원하고 인증·데이터·API·화면 정확성을 확보한다.

**개발 규칙:** TDD, 계층별 테스트, 객체 책임, 검증 보고 기준은 [AGENTS.md](AGENTS.md)를 따른다.

**Tech Stack:** Java 11/Spring Boot 2.5.10/JPA/QueryDSL, Vue 3/Vuex 4/Axios/Vue CLI 5.

**Spec:** [spec.md](spec.md). 실행 체크리스트: [tasks.md](tasks.md). B/F 경로와 기준 SHA는 spec.md와 동일하다.

## 이번 계획의 범위

- 이번 산출물은 분석과 계획이다.
- 공개 목록·회원 전용 상세와 기존 API 경로를 유지한다. 공고 DTO 변경은 양쪽을 함께 배포한다.
- 전체 프레임워크 업그레이드와 신규 크롤러·채팅·투표 기능은 범위 밖이다.

## 리뷰 결과

P0는 실행/데이터 보호 선행 장애, P1은 보안·핵심 동작 오류, P2는 안정성·성능 문제다. 실행 재현은 R01만 확보했고 나머지는 코드 경로에 기반한 정적 발견이다. 운영 장애를 실제 발생시켰다고 주장하지 않는다.

### R01 · P0 · 원격 main 자체의 컴파일 실패

근거: B `controller/AnnouncementController.java:3,46,61`, `InitDb.java:20` (Java 경로 앞에는 `src/main/java/com/inhatc/demp/`를 붙인다). `./gradlew test`가 네 가지 missing symbol로 실패했다. Domain과 DTO가 변경됐지만 호출부가 남아 있다. import만 고쳐도 InitDb의 이전 생성 방식과 쿼리 필드 등 후속 오류가 나타날 수 있으므로 최초 네 건만 해결하고 완료 선언하면 안 된다.

개선: 최신 공고 값 객체와 DTO에 호출부·fixture를 맞추고 clean compilation을 확보한 뒤 HTTP 계약 테스트를 시작한다. TDD 예외 합의가 필요한 실행 기반 복원은 tasks.md Phase 0의 별도 게이트로 둔다.

### R02 · P0 · 일반 설정의 스키마 재생성과 무조건 seed

근거: B `src/main/resources/application.yml:12`의 `ddl-auto: create`, `InitDb.java:34`의 @PostConstruct 및 initMember/initAnnouncement 호출. 실제 운영 DB에 연결해 정상 기동하면 데이터 보존이 위협받는다. 현재 컴파일 장애가 이 위험을 해결해 주는 것은 아니다.

개선: `InitDb.java`를 삭제하고 `src/main/resources/local/data.sql`을 local 개발환경의 실제 애플리케이션 실행 확인용으로만 사용한다. 기본·운영·자동 테스트에서는 SQL 데이터 초기화를 비활성화하고, 공통 설정의 local 프로필 강제 활성화도 제거한다. local 프로필을 명시적으로 선택한 경우에만 해당 SQL을 실행한다. 자동 테스트 데이터는 각 테스트 본문에서 직접 생성하며 data.sql을 fixture로 재사용하지 않는다. 기본 validate와 버전별 migration으로 스키마를 관리하고, local SQL은 스키마 준비 이후 실행한다. 검증은 격리 DB에서 하며 실제 운영 재시작으로 확인하지 않는다. 실행 항목: T10.

### R03 · P1 · 회원 응답과 브라우저 로그에 비밀번호 노출

근거: B `MemberController.java:29`에서 getPassword를 DTO로 반환, `:46`에서 Member 반환, `SecurityConfiguration.java:45`에서 회원 경로 공개. F `src/components/LoginForm.vue:85`에서 평문 비밀번호 console.log. 해시도 API에 공개할 이유가 없다.

개선: 회원 응답은 id/username만 가진 DTO로 제한하고 비밀번호 로그 제거. 가입 DTO의 암호화 책임은 서비스에 주입한 PasswordEncoder로 이동한다. 직접 가입 요청에 유효성·중복 검사가 없고 Member.username에도 unique가 없으므로 사전 중복 조회만 믿지 말고 DB 제약 및 409 변환을 함께 도입한다.

### R04 · P1 · 인증 사용자와 작성자/소유권이 연결되지 않음

근거: B `QuestionService.java:34,85`는 요청 username을 작성자로 신뢰한다. `:72-80`, `AnswerController.java:42-50`에는 소유권 비교가 없다. A의 토큰으로 B의 username을 보내거나 B의 글 ID를 수정할 수 있는 코드 경로다.

개선: principal ID를 Service에 전달하고 해당 ID로 작성자와 소유권을 검증한다. UI 버튼 숨김은 서버 접근 제어를 대신할 수 없다. 관리자 공고 정책을 새로 발명하지 않고 기존 USER 정책부터 정확하게 만든다.

### R05 · P1 · 저장 HTML이 그대로 DOM으로 들어감

근거: F `src/components/question/QuestionDetail.vue:21`, `QuestionAnswer.vue:8`의 v-html과 B의 가공 없는 content 저장. 허용 태그/속성/URL 검사가 보이지 않는다. 악성 이벤트 속성이 있는 콘텐츠를 저장하고 다른 사용자가 읽으면 저장형 XSS 경로가 된다. Vuex 영속 저장의 토큰도 같은 브라우저 컨텍스트에 있다.

개선: 서버 HTML 허용목록 정제와 프런트 공용 안전 렌더러. 굵게·목록 등 필요한 서식은 유지하고 script, onerror, javascript URL을 제거하는 회귀 사례를 둔다. 수작업 정규식으로 HTML 보안을 구현하지 않는다.

### R06 · P1 · 공고 계약이 양쪽에서 다른 형태

근거: B `dto/announcement/AnnouncementDetailResponse.java:21-28`는 description/recruitPeriod/career 중첩 객체, F `src/components/announcement/AnnouncementDetail.vue:14-60`은 content/payment/startedDate 등 최상위 필드를 읽는다. enum은 EMP/EDU인데 화면 조건은 emp/edu다. F AnnouncementWrite.vue:321-333은 평면 폼을 보내고 서버 생성 DTO는 값 객체를 받는다. Career와 RecruitPeriod에는 공개 getter도 없다. 생성 DTO `:25`의 @NotBlank는 enum에 맞지 않는다.

개선: HTTP DTO는 평면 입력·출력 계약으로 명확히 정의하고 내부 값 객체는 유지한다. 바인딩 DTO를 Domain으로 바로 사용하지 않는다. enum은 EMP/EDU, 직군·언어는 서버 enum 그대로 직렬화한다. 양쪽 fixture를 동일하게 만든다.

### R07 · P1 · 답변 수정의 쓰기 트랜잭션 부재

근거: B `AnswerController.java:43-45`에서 조회한 객체만 변경하고 save나 쓰기 서비스 트랜잭션이 없다. 요청 종료 이후 변경을 commit할 경계가 없다. 테스트 전체에 @Transactional을 씌우면 이 문제가 가려질 수 있다.

개선: AnswerService.update/delete에 트랜잭션과 권한을 모은다. 테스트에 @Transactional을 두지 않고 호출 종료 후 별도 조회한 내용으로 검증한다.

### R08 · P1 · 오류 본문과 실제 HTTP 상태 불일치

근거: B `ExController.java:17-32`는 ErrorResult에 400만 넣고 ResponseEntity/@ResponseStatus를 지정하지 않는다. `QuestionController.java:33`의 null 검사는 서비스가 예외를 던지므로 404를 처리하지 못한다. MemberService.login은 일반 RuntimeException을 던진다.

개선: 400 validation, 401 인증, 403 소유권, 404 없음, 409 중복, 500 내부 실패를 응답 상태와 본문에 동일하게 표현한다. 필터 예외는 Security 진입점에서도 동일 형식으로 변환한다.

보안 점검: `ExController`의 `e.getMessage()` 직접 반환, field error가 없는 validation 처리, 일반 RuntimeException과 업무 예외의 혼용을 점검한다. 클라이언트에는 허용된 오류 코드·안내 문구만 반환하고 SQL·스택 트레이스·클래스명·비밀번호·JWT가 응답이나 로그에 노출되지 않게 한다. 존재하지 않는 계정과 비밀번호 불일치의 로그인 응답은 동일하게 처리한다. MVC 예외와 JWT 필터 예외는 발생 경계가 다르므로 Advice만으로 전체 보안을 처리한다고 가정하지 않는다. 예외 클래스는 실제 구분할 필요가 있는 종류만 두고, 범용 예외 프레임워크를 추가하지 않는다. 실행 항목: T14.

### R09 · P1 · 태그 재사용·교체 실패 및 무태그 질문 누락

근거: B `QuestionService.java:95-103`은 새 Hashtag를 만들어 contains로 비교하지만 Hashtag에 값 동등성 구현이 없다. 수정 시 기존 관계를 지우지도 않는다. `QuestionQueryRepository.java:31-32`는 무조건 inner join하므로 태그 없는 질문을 목록에서 제외한다.

개선: 정규화한 이름으로 기존 태그를 조회/재사용하고 질문이 관계 집합을 교체하도록 한다. 태그 이름·관계의 DB unique 제약을 추가한다. 필터가 없으면 태그 join이 없는 쿼리, 필터가 있으면 exists 조건으로 OR 의미를 유지한다.

### R10 · P2 · 페이지 쿼리의 메모리 처리와 순서 불안정

근거: B `repository/announcement/AnnouncementQueryRepository.java`의 getAnnounceScroll은 collection fetch join+offset/limit, fetchResults를 사용하고 orderBy가 없다. pagingTest에는 메모리 페이징 경고 주석도 있다. 대량 조회·불필요 count 위험이 있으며 페이지의 안정적 순서가 없다. 질문은 전체 fetch다.

개선: ID를 고정 정렬로 size+1 조회한 뒤 연관 값을 조회하는 두 단계 방식으로 공고 Slice를 만든다. 질문에도 상한 있는 페이지 계약을 양쪽 동시 반영한다. SQL 제한과 페이지 경계 중복을 테스트한다.

### R11 · P2 · 업로드 검증 순서와 외부 부작용

근거: B `AnnouncementService.java:36-40`은 중복 검사 전에 S3에 업로드한다. 실패 시 삭제 보상이 없다. FileService는 빈 파일에 null을 반환하지만 공고 응답 DTO는 image를 역참조한다. 확장자/MIME 허용 정책도 없다.

개선: 입력·중복 사전 검사 후 업로드, DB 실패 시 업로드 키 삭제 보상, 실패 추적. S3는 DB rollback에 참여하지 않으므로 @Transactional만으로 파일이 지워지지 않는다. 필수 이미지는 빈 파일을 400으로 거절한다.

### R12 · P2 · 화면 상태가 재시도와 검색 변경을 견디지 못함

근거: F `QuestionWrite.vue:59,139-143`의 모듈 전역 temp가 제출마다 누적되고 입력 객체 형태를 변경한다. `QuestionSearch.vue:30-36`은 검색 종류 변경 시 이전 조건을 남긴다. `AnnouncementList.vue:64-89`에는 동시 요청 차단이 없고 실패를 last=true로 처리한다. emitter 등록에도 해제 경계가 없다.

개선: 매 제출마다 지역 변수로 payload 생성, route query를 검색 상태의 단일 기준으로 사용, 요청 세대 ID 또는 취소로 오래된 응답 무시, loading/error/last 분리, 이벤트 해제. 정상 데이터 없음과 네트워크 실패는 재시도 가능성이 다르다.

### R13 · P2 · 추천 표시와 정렬이 서버와 다름

근거: B QuestionDetail/QuestionList의 recommend, F QuestionMenu.vue:17의 recomend와 QuestionDetail.vue:62-66의 로컬 증가. 새로고침하면 반응이 사라지고 추천 정렬 요청도 서버 분기와 다르다.

개선: recommend로 통일하고 영속화 없는 버튼을 비활성화한다. 투표 API 추가는 별도 기능 결정 후 TDD로 한다.

### R14 · P2 · 인증 만료·API 라우팅·검증 기반 부재

근거: F Login.js:9는 username만 검사한다. axios는 각 컴포넌트가 직접 사용한다. vue.config.js의 두 module.exports 중 뒤 설정이 앞 설정을 덮고 개발 프록시는 고정 IP다. server.js는 /api 프록시 없이 GET 전체에 index.html을 반환하므로 별도 앞단 프록시가 없으면 운영 API 조회가 HTML을 받는다. package.json에는 test 스크립트가 없다. 현재 의존성이 없어 lint/build도 실행하지 못했다. B의 서비스/컨트롤러 테스트는 @SpringBootTest와 테스트 @Transactional에 광범위하게 의존한다.

개선: 실행 버전·lockfile 기반 설치 재현, 프런트 테스트 도입, 계층별 서버 테스트, 중앙 Axios client와 401 처리, 환경별 API 설정, 운영 reverse proxy 계약, CI 검증. 광범위 CORS(`SecurityConfiguration.java:61-64`)는 배포 origin 허용목록으로 좁힌다.

테스트 구조 보완: 테스트 데이터 setup용 `@BeforeEach`를 모두 제거하고 각 테스트 본문에서 필요한 회원·질문·답변·DTO를 직접 생성한다. 공유 필드나 상위 클래스 setup으로 생성을 숨기지 않는다. MockMvc·REST Docs 도구 초기화와 `@AfterEach` cleanup은 데이터 setup과 구분한다. 또한 현재 MemberControllerTest의 REST Docs 생성을 별도 `docs/*RestDocsTest`로 분리하고 회원·공고·질문·답변 API를 함께 문서화한다. Controller 테스트는 동작 검증, REST Docs 테스트는 실행한 HTTP 계약의 문서 생성을 담당한다. 둘 다 유지하며 문서 테스트가 Controller 회귀 테스트를 대체하지 않는다. 실행 항목: T01, T02 및 T50.

### R15 · P2 · Member 엔티티가 Spring Security에 결합

근거: B `domain/Member.java`는 `UserDetails`를 구현하며 GrantedAuthority 변환과 계정 상태 메서드를 포함한다. `service/CustomUserDetailService.java`는 조회한 Member를 그대로 UserDetails로 반환한다. 이 구조에서는 인증 프레임워크의 계약 변경이 회원 도메인에 직접 영향을 준다.

개선: Member의 `implements UserDetails`와 Security 전용 메서드·import를 제거하고 JPA 엔티티와 회원 도메인으로 유지한다. 별도 `config/security/MemberPrincipal`이 UserDetails를 구현하며 memberId·username·password hash·권한의 인증용 값을 가진다. CustomUserDetailService가 Member를 principal로 변환하고, JWT와 Controller는 principal에서 ID를 추출해 Service에 전달한다. principal은 엔티티 참조를 보관하거나 API 응답으로 직렬화하지 않는다. 기존 로그인·JWT·ROLE_USER 동작을 유지하는 테스트로 보호한다. 실행 항목: T11A → T12.

```text
Member (JPA·회원 도메인)
    └── CustomUserDetailService의 변환 ──► MemberPrincipal (UserDetails)
                                              └── 인증 → Controller → actorId → Service
```

## 개선 순서와 설계 이유

```text
Phase 0: 실행·테스트 기반·REST Docs 분리 (R01, R14 일부)
    ↓
Phase 1: local SQL·인증 객체 분리·예외 보안·콘텐츠 보호 (R02~R05, R08, R15)
    ↓
Phase 2: 공고 계약·저장 부작용 (R06, R11)
    ↓
Phase 3: 답변·태그·검색 정확성 (R07, R09, R10)
    ↓
Phase 4: 화면 상태·연결·반응 표시 (R12~R14)
    ↓
Phase 5: 전체 흐름·배포 검증
```

실행할 수 없으면 회귀 테스트를 믿을 수 없고, 인증 주체가 불명확하면 CRUD를 정리해도 권한 버그가 남는다. 계약을 먼저 확정해야 프런트와 서버를 각각 고쳐 놓고 다시 맞추는 일을 줄일 수 있다.

## 완료 조건

spec.md의 S01~S12 및 이번 추가 요구사항 5개를 tasks.md에서 모두 추적하고, [AGENTS.md의 완료 판정](AGENTS.md#완료-판정)에 따라 검증 결과를 기록한다. InitDb 삭제/local 실행 전용 SQL, Member와 UserDetails 분리, 독립 REST Docs 테스트, 테스트별 직접 데이터 생성, 예외·보안 검증을 인수 조건에 포함한다. 이번 리뷰에서는 테스트 실행 전에 막혔으므로 취약 경로의 런타임 재현과 브라우저 E2E는 아직 검증되지 않았다.
