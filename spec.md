# DEMP 프로젝트 스펙

분석일: 2026-09-08. 이 문서는 두 저장소를 함께 다룬다. B는 `demp`, F는 형제 디렉터리 `../dempfrontend`를 의미한다. 현재 코드에서 확인한 사실과 앞으로 만족시킬 목표를 구분한다.

## 1. 기준과 분석 범위

| 저장소 | GitHub 기준 브랜치 | 기준 커밋 |
| --- | --- | --- |
| B | seungmin-park/demp, origin/main | cb6e0b35c94d2a69b9094868aaacfb75793152eb |
| F | seungmin-park/dempfrontend, origin/main | 646e7dab0ac4f0b307f44f2c46fd0a9fe3a37b54 |

분석 근거는 양쪽 README, 컨트롤러·서비스·도메인·쿼리·DTO·보안 설정·테스트, 프런트 라우터·스토어·주요 컴포넌트·실행 설정이다. 운영 서버나 실제 DB에는 접속하지 않았다. 아래 API는 소스에 선언된 계약이며, 전체 서비스가 실행된다는 의미는 아니다.

## 2. 무엇을 만드는 프로젝트인가

개발자 취업 준비생이 채용 공고와 교육·부트캠프 정보를 찾아보고, 질문과 답변으로 정보를 교환하는 커뮤니티다. 공고 탐색과 Q&A가 핵심이며 회원 인증이 상세 열람과 쓰기를 제한한다.

```text
방문자 ──► 공고 목록·필터 / 질문 목록·검색
   │
   └── 가입·로그인 ──► 상세 열람 ──► 외부 지원 사이트
                         │
                         └── 질문 작성·답변 작성
```

질문 수정·삭제와 답변 수정·삭제는 서버 API가 있지만 이에 대응하는 완성된 화면 흐름은 확인되지 않았다.

## 3. 구조와 기술

| 영역 | 저장소에 선언된 기술과 책임 |
| --- | --- |
| 서버 | Java 11, Spring Boot 2.5.10, Spring Security, JPA, QueryDSL, Gradle 7.4 |
| 인증 | BCrypt, JWT HS256, X-AUTH-TOKEN 헤더, 토큰 유효시간 1시간 |
| 저장 | H2·MySQL 드라이버, S3 이미지 저장, 환경변수로 연결 설정 |
| API 문서 | Spring REST Docs, Asciidoctor |
| 화면 | Vue 3 방식 createApp, Vue Router 4, Vuex 4, Axios, Vue CLI 5 |
| UI | Bootstrap, vee-validate, Summernote 전역 플러그인, mitt |
| 실행 | 개발 Vue CLI 프록시, 운영용 Express 정적 파일 서버 |

```text
Vue View/Component ── Axios /api ──► Security/JWT ──► Controller
       │                                               │
 Vuex Login + 영속 저장                            Application Service
                                                       │
                                                Domain ↔ Repository ↔ DB
                                                       │
                                                   FileService ↔ S3
```

현재는 컴포넌트에 HTTP·인증·화면 상태가 섞이고 AnswerController가 Repository를 직접 사용한다. 목표 설계는 Controller가 HTTP 변환, Service가 권한·트랜잭션·유스케이스 조정, Domain이 상태 규칙을 맡는 것이다. 파일 저장처럼 실제 외부 부작용이 있는 경계만 작은 인터페이스로 분리한다. 모든 객체에 인터페이스를 만드는 것이 목적은 아니다.

## 4. 데이터 모델

```text
Member 1 ── N Question 1 ── N Answer N ── 1 Member
                 │
                 1 ── N QuestionHashtag N ── 1 Hashtag

Announcement
 ├─ title, announcementType(EMP/EDU), jobPosition
 ├─ Company(name)
 ├─ Career(minCareer, maxCareer)
 ├─ RecruitPeriod(startedDate, deadLineDate)
 ├─ Description(content, accessUrl, payment, languages)
 └─ UploadFile(uploadFileName, saveFileName)
```

Question은 제목·내용·작성자·생성시각·조회/추천/비추천 수를 가진다. Answer는 내용·작성자·질문과 반응 수를 가진다. 태그 관계와 답변은 질문 삭제 시 cascade 제거 대상이다. 사용자 이름과 태그 이름의 DB 유일성은 현재 명시되어 있지 않다. 값 객체가 존재하지만 날짜 역전·음수 경력 등의 불변식은 아직 강제하지 않는다.

## 5. 현재 화면과 API

| 화면 | 목적 |
| --- | --- |
| `/` | 채용·교육 목록과 더보기, 조건 검색 |
| `/detail/:itemId` | 공고 상세, 지원 링크 |
| `/addAnnounce` | 공고 등록 |
| `/account`, `/login` | 가입, 로그인 |
| `/question` | 질문 목록, 제목·내용·태그 검색, 정렬 |
| `/questions/:questionId` | 질문 상세 및 답변 |
| `/questions/new` | 질문 작성 |

| 메서드와 경로 | 입력 / 선언된 출력 | 현재 접근 조건 |
| --- | --- | --- |
| POST `/api/member/save` | 폼 username/password → Member 엔티티 | 공개 |
| POST `/api/member/login` | 폼 username/password → username/jwt | 공개 |
| GET `/api/member/validUsername` | username → 사용 가능 여부 boolean | 공개 |
| GET `/api/member/{memberId}` | ID → id/username/password | 공개, 개선 필요 |
| GET `/api/announce` | announcementType, positions, language, career, payment, title, Pageable → Slice | 공개 |
| GET `/api/announce/scroll` | 전체 공고 → 목록 | USER |
| GET `/api/announce/detail/{id}` | ID → 상세 | USER, 타입 참조 깨짐 |
| POST `/api/announce/add` | multipart → 문자열 ok | USER, 타입 참조 깨짐 |
| GET `/api/question` | title/content/hashtags/orderBy → 목록 | 공개 |
| GET `/api/question/hashtags` | 전체 태그 문자열 목록 | 공개 |
| GET `/api/question/detail/{id}` | ID → 질문 상세 | USER |
| POST `/api/question/add` | JSON title/content/username/hashtags → ok | USER |
| PATCH `/api/question/update` | JSON questionId/title/content/hashtags | USER, 소유권 미검사 |
| DELETE `/api/question/delete` | query questionId | USER, 소유권 미검사 |
| GET `/api/answer/{questionId}` | 질문 ID → 답변 목록 | USER |
| POST `/api/answer/save` | JSON username/questionId/answerContent → 답변 목록 | USER |
| PATCH `/api/answer/update` | JSON answerId/answerContent | USER, 소유권·트랜잭션 개선 필요 |
| DELETE `/api/answer/delete` | query answerId | USER, 소유권 미검사 |

공고 필터의 payment는 현재 `이상` 조건이다. career=0은 필터 생략, maxCareer=0은 상한 없음으로 처리한다. 이것을 신규·무료 필터 의미로 오해하면 안 된다. 질문 태그 조건은 선택 태그 중 하나가 일치하는 OR이며, 제목과 내용은 함께 전달되면 AND다. 정렬의 서버 필드는 `recommend`인데 화면 일부는 `recomend`를 쓴다.

## 6. 개선 후 수용 기준

다음은 현재 보장된 동작이 아니라 plan.md와 tasks.md가 달성할 목표다.

| ID | 목표와 확인 가능한 결과 |
| --- | --- |
| S01 | 깨끗한 checkout에서 서버 테스트, 화면 테스트·lint·build를 재현할 수 있다. 컴파일 오류를 먼저 해소한다. |
| S02 | 일반 실행에서 스키마를 삭제·재생성하거나 예제 데이터를 자동 삽입하지 않는다. 테스트 DB와 운영 설정을 분리한다. |
| S03 | 모든 회원 응답에 password가 없고, 가입은 빈 값·중복을 거부한다. 중복 동시 요청도 DB 제약으로 방어한다. 로그인 실패는 401이다. |
| S04 | 작성자는 인증 principal에서 결정한다. 타인 질문·답변 수정/삭제는 403, 비인증 쓰기는 401이다. |
| S05 | 외부 입력 HTML의 script·이벤트 속성·javascript URL이 실행되지 않는다. 로그인 비밀번호를 로그에 출력하지 않는다. |
| S06 | 공고 요청과 응답 필드·enum을 양쪽에서 동일하게 사용한다. 잘못된 enum/기간/경력/이미지는 400이다. |
| S07 | 답변 수정은 서비스 commit 이후 별도 조회에도 남는다. 없는 질문·답변은 404이며 본문 코드와 HTTP 상태가 일치한다. |
| S08 | 태그 수정은 기존 관계를 교체하고 같은 이름을 재사용한다. 중복 태그가 생기지 않고, 태그 없는 질문도 무필터 목록에 포함된다. |
| S09 | 공고 페이지는 고정된 정렬로 중복 없이 반환되고 DB 페이지 제한을 사용한다. 질문 목록에도 크기 제한을 도입한다. |
| S10 | 로딩·성공·빈 결과·실패를 구분한다. 재시도는 중복 태그/페이지를 만들지 않으며, 늦은 과거 검색 응답은 최신 화면을 덮지 않는다. |
| S11 | 인증 만료 시 저장 상태를 지우고 로그인으로 안내한다. 개발/운영 API 주소를 명시적으로 설정하며 /api에 HTML을 반환하지 않는다. |
| S12 | recommend 표기와 정렬을 통일한다. 서버에 저장하지 않는 반응 버튼은 비활성 표시한다. |
| S13 | 양쪽 저장소의 코드 파일별 이름·메서드 효과·소속 클래스 책임을 빠짐없이 검토하고, 확인된 불일치를 작은 회귀 검증 단위로 고친다. 백엔드와 독립 TypeScript 객체에는 SOLID를 검토하며 Vue 컴포넌트는 역할 경계로 평가한다. |

공개 목록·회원 전용 상세 정책과 기존 URL은 우선 유지한다. 공고 관리자 전용 권한, 실시간 채팅, 크롤러, 실제 투표·중복 투표 방지, 조회수 집계, 비밀번호 재설정은 별도 제품 범위다. 이번 개선 계획에 임의로 신규 기능을 섞지 않는다.

## 7. 검증 기준과 현재 결과

2026-09-08 실행 결과:

- B `./gradlew test`: exit 1. `compileQuerydsl`에서 InitDb의 Language, AnnouncementController의 Announcement/AnnouncementDetail/AnnouncementForm 참조 오류 4건. 테스트 본문은 실행되지 않았다.
- F `npm test`: exit 1. test 스크립트 없음.
- F `npm run build`: exit 127. vue-cli-service 없음.
- F `npm run lint -- --no-fix`: exit 127. vue-cli-service 없음. 소스 변경을 막기 위해 자동 수정 비활성화.
