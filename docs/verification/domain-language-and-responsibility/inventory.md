# DEMP 전체 코드 이름·책임 정적 리뷰 — 2026-09-26

## 기준과 판정 범위

- B: `demp` HEAD `2ecf41ef876d2c2af446031f63c8b02c55ef40e2`, 브랜치 `refactor/announcement-upload-contract`의 현재 미커밋 작업 상태. F: `dempfrontend` HEAD `646e7dab0ac4f0b307f44f2c46fd0a9fe3a37b54`의 현재 미커밋 작업 상태.
- B Java 96개(운영 67, 테스트 29), F Vue/JS 40개(운영 34, 테스트 6)를 파일 목록과 코드 선언·호출부로 전수 대조했다. 별도로 B Gradle 2개, F 실행/도구 JS 4개와 양쪽 설정의 역할을 검토했다. 프런트 `.ts/.tsx`는 0개다. 생성 QueryDSL, Gradle wrapper, 의존성, 이미지, 빌드 산출물은 작성 대상 코드가 아니므로 제외했다.
- 이 리뷰는 **이름, 메서드 효과, 클래스/컴포넌트의 책임, 의존 방향**에 대한 정적 검사다. 파일별 포함 여부는 아래 목록으로 확인한다. 모든 실행 경로의 버그를 재현했다는 뜻은 아니다. 발견마다 `정적` 또는 `실행`을 구분한다.
- Phase 5 예정 브랜치 `refactor/domain-language-and-responsibility`, 예정 worktree B `demp/.worktrees/domain-language-and-responsibility/backend`, F `dempfrontend/.worktrees/domain-language-and-responsibility/frontend`. 이번 검토는 기존 미커밋 코드의 정확한 상태를 보기 위해 현재 작업 디렉터리에서 읽기 검사와 이 문서만 작성했다. 구현·커밋은 하지 않았다.

## 결론

현재 코드의 이름 문제는 단순 오타에 그치지 않는다. `setHashtags`는 설정이라는 이름 아래 태그 탐색·생성·관계 연결을 수행하고, 프런트의 `getDetailAnnounce`는 상세 조회가 아니라 라우트 이동을 한다. 이런 불일치는 변경할 책임의 위치를 흐린다. 아래 항목은 우선순위 순서이며, 기존 계획의 기능 수정(T31~T42)과 중복되는 지적은 해당 작업에 합친다.

```text
Controller / Vue event ── 요청·이동 ──► Service / 화면 상태
                                      │ 유스케이스 순서·트랜잭션
                                      ▼
                              Domain 상태·관계 규칙
                                      │
                                      ▼
                         Repository / 파일 저장·API 경계
```

## 발견 사항

| ID | 우선순위·근거 | 실제 효과와 이름/책임의 차이 | 리팩터링 단위·검증 |
| --- | --- | --- | --- |
| N01 | P1 · 정적 · B `QuestionService.java:92-108`, `Question.java:52-55`, `Hashtag.java:25-28` | `setHashtags`는 기존 관계를 대체하지 않고 모든 태그를 새 객체로 만들고 저장/연결한다. `contains`는 값 동등성이 없어 기존 이름 재사용을 보장하지 않는다. 이름·효과가 다르고 태그 탐색과 관계 불변식의 소유자가 섞였다. | T31에서 이름 기준 태그 탐색/재사용과 관계 교체를 Red→Green. 관계 변경은 `Question`이 소유하고 서비스는 순서를 조정할지 결정. T46~T47에서 이름·클래스 추출 판정. |
| N02 | P1 · 정적 · B `QuestionQueryRepository.java:26-38,41-49`, `AnnouncementQueryRepository.java:44-94` | 무태그 질문도 내부 조인으로 빠진다. `getAnnounceScroll`은 Slice 이름 아래 count 결과·컬렉션 fetch join·메모리 페이지 위험을 포함하고, `pagingTest`는 운영 메서드인데 이름이 테스트다. | T31/T32에서 조회 결과·SQL 제한 테스트를 먼저 실패시킨 뒤 쿼리 수정. 이후 `find...Slice`/`find...Page` 같은 결과형 이름으로 정리. |
| N03 | P1 · 정적 · F `QuestionWrite.vue:114-143`, `AnnouncementList.vue:33-90` | `saveQuestion`이 편집기 읽기·전역 `temp` 태그 변환·HTTP·이동을 함께 수행한다. 재시도할 때 전역 배열이 누적될 수 있다. `loadDataFromServer`는 요청, 페이지 합치기, 종료, 오류를 처리하면서 오류도 `last=true`로 만든다. | T40에서 실패→재시도, 응답 역전, 빈 목록/오류를 구별하는 테스트. 요청 payload는 호출별 순수 변환, 목록 상태는 로딩/오류/마지막을 분리하고 이름을 결과에 맞춘다. |
| N04 | P1 · 정적 · F `QuestionSearch.vue:29-38`, `QuestionList.vue:34-75`, `AnnouncementList.vue:35-41` | `printCondition`은 출력이 아니라 라우트 변경이다. 검색 유형 전환 시 이전 조건을 남기고, 오래된 응답이 최신 결과를 덮을 수 있다. emitter 구독은 해제하지 않는다. | T40에서 라우트 query를 검색 기준으로 만들고 응답 세대·구독 해제를 검증. `submitSearch`처럼 효과를 드러내는 이름 사용. |
| N05 | P2 · 정적 · B `domain/announcemnet/*`, `Question.java:33-55`, `Answer.java:32-45`, `AnnouncementDetailResponse.java:36` | 패키지 `announcemnet`, 필드 `QuestionHashtags`, 동사 `settingMember/settingQuestion`, 팩터리 `getBuild`가 도메인 언어 및 실제 관계/변환 동작을 흐린다. | T46에서 호출부·JPA/QueryDSL 매핑·JSON 계약을 특성화한 뒤 일관된 도메인 용어로 변경. 관계 양쪽을 원자적으로 갱신하는 엔티티 연산인지 판정. |
| N06 | P2 · 정적 · B `AnnouncementService.java:44-95`, `FileService.java:41-69` | `join(Announcement)`과 `save(request)`는 둘 다 공고를 저장하지만 입력·검증·업로드·트랜잭션 경계가 다르다. `save`는 DTO 변환, 정제, 중복 조회, 업로드, DB commit, 보상까지 맡는다. `FileService.getFullPath`는 S3 저장 경로와 별개의 로컬 경로를 만들며 호출부가 없다. | T47에서 두 저장 진입점의 사용처를 확인해 단일 유스케이스 계약으로 정리. 입력 변환·파일 검증/전송·보상 규칙이 독립 변경 이유인지 판단한 후 필요한 클래스만 추출. 보상·commit 테스트 유지. |
| N07 | P2 · 정적 · B `AnnouncementDetailResponse.java:3,41`, `AnnouncementResponse.java:3,28`, `AnnouncementScroll.java:3,26`, `AwsS3Config.java:20` | 세 응답 DTO가 인프라 설정의 하드코딩 S3 URL을 직접 읽는다. 응답 표현의 변경 이유가 S3 설정에 묶인다. `FileStorage` 포트는 저장 명령만 담아 URL 해석의 주인이 없다. | T47에서 파일 URL 변환을 응답 조립 경계로 이동하고 설정 주입값/작은 URL 계약을 선택. 세 DTO의 기존 `image` JSON을 검증. |
| N08 | P2 · 정적 · B `MemberController.java:23,42`, `MemberService.java:30,71`, `QuestionService.java:35`, `AnnouncementService.java:102`, `QuestionQueryRepository.java:71` | `Member`, `validUsername`, `validationDuplicateUsername`, `join`, `pageTest`, `QuestionSort`는 각각 조회·사용 가능 여부·생성·페이지 조회·정렬 선택이라는 효과를 명확히 표현하지 못한다. | T47에서 호출부별 결과 계약을 고정하고 `getMember`, `isUsernameAvailable`, `createQuestion`, `find...Page`, `sortOrder` 등 의미가 드러나는 이름을 최종 결정. Spring 인터페이스 메서드명은 계약상 유지. |
| N09 | P2 · 정적 · B `dto/answer/AnswerForm.java:14`, `dto/question/QuestionForm.java:16`, `MemberSearchCondition.java:8-14`, `ListResponse.java:10-19` | 작성 요청의 `username`은 인증 주체 결정에 쓰이지 않지만 DTO에 남아 있다. `MemberSearchCondition`의 나이 조건은 `Member`에 없는 개념이고, `ListResponse`는 호출부가 없다. | T47에서 입력 계약을 확인한 뒤 사용하지 않는 필드/타입 제거 또는 경계에 맞는 이름으로 정리. 요청 JSON/REST Docs 호환성 검증. |
| N10 | P2 · 정적 · B `AnswerService.java:29-35`, `CustomUserDetailService.java:17-22`, `QuestionServiceTest.java:30-35,99-128,176-184` | `AnswerService.save`는 답변 하나를 저장하고 질문의 **전체 답변 목록**을 다시 조회해 반환한다. `loadUserByUsername`은 Spring의 고정 메서드지만 실제 입력은 회원 ID 문자열이다. `QuestionServiceTest` 안에 답변 서비스 자체의 사례가 섞였다. | T47에서 저장·조회 응답 조립의 소속을 정하고 이름/호출 계약을 맞춘다. 인터페이스 시그니처는 유지하고 매개변수명을 ID 의미로 변경. 답변 서비스 테스트는 해당 테스트 클래스로 이동. |
| N11 | P2 · 정적 · F `Hashtags.vue:65-156`, `AnnouncementList.vue:80-82`, `AnnouncementDetail.vue:85-106`, `AccountForm.vue:102`, `router/index.js:11-16`, `data/positon.js` | `setHashtags`는 태그 설정이 아니라 입력창 열기/포커스, `getDetailAnnounce`(목록)는 상세 조회가 아니라 이동이다. `DetailAnnounce` 필드, `AccountMethod`, `Test*` 라우트명, `positon` 파일명도 의미/철자가 불명확하다. `Hashtags`의 동기 작업에 불필요한 `async/await`가 있다. | T48에서 props/event·라우트 사용처·파일 import를 고정하고 실제 효과를 표현하는 이름으로 변경. Vue 컴포넌트는 SOLID 점수 대신 UI/API/상태 역할로 판정. |
| N12 | P2 · 정적 · F `QuestionDetail.vue:15,64-68`, `QuestionAnswer.vue:12`, `QuestionMenu.vue:17`, `QuestionList.vue:6` | 화면은 서버의 `recommend`를 `recomend`로 읽고 추천 클릭은 로컬 값만 늘린다. 이름 불일치가 표시와 저장 의미를 함께 왜곡한다. | T42에서 동일 fixture로 목록·상세·정렬을 검증하고 `recommend`로 통일. 저장 API 없는 반응 버튼은 비활성화. |
| N13 | P2 · 정적 · B `SecurityConfiguration.java:65-76`, `WebConfig.java:11-14`, F `vue.config.js:1-18`, `server.js:9-13` | CORS 정책의 소유자가 서버 설정 두 곳에 있고, Vue 설정은 두 번째 `module.exports`가 첫 번째를 덮는다. Express catch-all은 `/api`도 화면 HTML로 응답한다. 배포/라우팅 책임이 명확하지 않다. | T41에서 설정 소유자를 하나로 정하고 환경별 API 경로·401·CORS 계약을 테스트. |
| N14 | P2 · 정적 · B `AnswerControllerTest.java:66-88,100-107`, `QuestionServiceTest.java:99-128` | Controller 테스트의 이름은 “답변에 반영”·“저장소에 전달”이라고 하지만 실제로는 mock 서비스에 전달된 값만 검증한다. 사용하지 않는 `Answer`도 만든다. 답변 서비스 사례는 질문 서비스 테스트에 섞여 있다. 테스트 이름과 소속도 검증 경계를 정확히 표현하지 못한다. | T47에서 테스트를 해당 계층/클래스로 옮기고 Controller 테스트는 HTTP 전달, Service 테스트는 commit 후 재조회라는 실제 assertion에 맞게 이름을 수정. |
| N15 | P2 · 정적 · F `fontAwesomeIcon.js:1-11`, `main.js:1-22`, `router/index.js:20-23`, `views/announcement/AnnouncementList.vue:20-25` | `fontAwesomeIcon.js`는 Vue 2 방식의 전역 등록을 담지만 현재 진입점에서 import하지 않는다. `HelloWorld` 라우트와 주석 처리한 `AnnouncementFooter`는 예제/미완성 코드로 남아 실제 제품 역할이 불명확하다. | T48에서 실제 사용처를 확인하고 미사용 예제/진입점은 제거하거나 명확한 역할로 연결. 라우트 접근과 빌드 검증 후 판단. |

## 메서드·클래스 책임 이동 판정

| 현재 위치 | 이번 정적 판정 | 이유와 다음 검증 |
| --- | --- | --- |
| `QuestionService.join/updateQuestion` | 유스케이스 조정은 Service에 유지. `setHashtags/settingQuestionHashtag`의 태그 이름 조회·재사용 규칙은 별도 태그 협력자로, 관계 연결·교체는 `Question`의 연산으로 이동할 후보. | DB 조회와 도메인 관계 변경은 변경 이유가 다르다. T31에서 중복/교체 Red를 만든 뒤 실제 책임을 확정한다. |
| `AnnouncementService.save` | 업로드→DB commit→실패 보상의 순서는 하나의 공고 생성 유스케이스이므로 Service 조정 책임에 유지. 요청→값 객체 변환과 설명 정제가 다른 저장 진입점과 반복될 때만 별도 변환 객체를 추출한다. | 단계가 많다는 이유만으로 트랜잭션/보상 순서를 흩뜨리면 실패 경계가 더 불명확해진다. T47에서 두 진입점의 사용처와 보상 테스트로 결정한다. |
| `FileService.validate/save` | 파일 형식/크기 정책은 저장소 구현과 다른 변경 이유이므로 작은 이미지 검증 객체 추출 후보. S3 전송·삭제는 `FileStorage` 구현에 둔다. | 정책을 바꾸는 테스트와 S3 호출 실패 테스트가 분리되어야 한다. 기존 FileServiceTest와 업로드 보상 테스트로 검증한다. |
| 세 공고 응답 DTO의 URL 조립 | `AwsS3Config` 직접 참조를 응답 조립 경계의 URL 해석 협력으로 이동. | S3 도메인/주소 변경이 DTO 세 개를 동시에 수정하게 만들지 않도록 한다. `image` 응답값은 REST Docs로 고정한다. |
| `QuestionQueryRepository.QuestionSort` | 정렬 선택은 조회 쿼리 책임이므로 Repository 안에 유지하고 이름·기본 정렬을 고친다. | 별도 클래스 추출보다 검색/페이지 계약 테스트가 우선이다. |
| `AnswerService.save` | 답변 생성 후 목록 반환은 현재 API 유스케이스에 속하므로 Service에 유지 가능. 반환까지 드러나는 이름과 응답 조회 시점을 검토한다. | 무조건 명령/조회 클래스를 분리하면 기존 HTTP 결과 계약이 복잡해진다. Controller/Service 테스트의 assertion을 먼저 맞춘다. |
| Vue `QuestionWrite.saveQuestion`, `AnnouncementList.loadDataFromServer` | 컴포넌트는 사용자 이벤트와 표시를 맡는다. 재시도마다 새 payload를 만드는 변환은 순수 함수로, 공유 요청 상태가 필요하면 store/API 모듈로 이동한다. | 단일 화면의 지역 상태만 있으면 store를 만들지 않는다. 실패·재시도·응답 역전 테스트가 분리 판단의 근거다. |

## SOLID와 역할 경계 판정

| 영역 | 판정 |
| --- | --- |
| 백엔드 SRP | `QuestionService`의 태그 탐색/관계 변경, 공고 DTO의 S3 URL 결합, 중복 CORS 설정은 분리/소속 재검토가 필요하다. 유스케이스의 여러 단계 조정 자체는 SRP 위반으로 판정하지 않았다. |
| 백엔드 OCP/LSP/ISP | 현재 상속·다형성 확장 지점은 Spring/JPA 계약이 대부분이다. 구체적인 치환 위반 근거가 없는 객체에 추측성 인터페이스를 추가하지 않는다. `FileStorage`는 실제 외부 저장 경계를 분리한 유효한 작은 계약이다. |
| 백엔드 DIP | `AnnouncementService → FileStorage`는 적절하다. 공고 응답 DTO → `AwsS3Config.BUCKET_URL`과 보안 정책의 중복 소유는 의존 방향/경계 개선 대상이다. |
| TypeScript | 현재 소스에 `.ts/.tsx`가 없어 SOLID 적용 대상 없음. |
| Vue | 컴포넌트는 props/event·렌더링, 로컬 상태, 공유 상태, API 호출의 배치로 검토했다. N03/N04/N11이 주요 경계 문제다. |

## 실행 검증과 한계

- B `JAVA_HOME=...zulu-21... ./gradlew test --console=plain`: `compileJava`에서 Lombok/Java 21의 `JCTree$JCImport.qualid` 오류로 중단. 테스트 본문 미실행, 환경 호환성 실패.
- B `JAVA_HOME=...zulu-11... ./gradlew test --console=plain`: **BUILD SUCCESSFUL**, JUnit XML 기준 27 suite·168 test·실패/오류/건너뜀 0. 이 통과는 N01~N15의 미검증 경로가 옳다는 뜻이 아니다.
- F `npm test -- --runInBand`: exit 127, `vue-cli-service: command not found`. `node_modules`가 없는 현재 환경에서 프런트 테스트는 실행되지 않았다. lint/build도 실행 결과로 주장하지 않는다.
- 2026-09-26 후속 확인: `npm ci`는 Python 3.14의 `distutils` 부재로 `node-sass` 빌드에서 실패했다. `npm ci --ignore-scripts` 후 테스트 24개와 lint는 통과했지만 build는 Sass 컴파일에서 실패했다. `node-sass`를 Dart Sass `1.77.8`로 교체한 후 테스트 24개, lint, build가 모두 통과했다. 이는 정적 판정 당시의 미실행 기록을 대체하지 않고 후속 검증으로 구분한다.
- 정적 발견은 실제 브라우저/DB 부하/운영 S3에서 재현한 장애로 표현하지 않는다. Phase 3~5의 Red→Green과 회귀 테스트에서 각 영향 범위를 확인한다.

## 파일별 검사 목록

아래 목록은 해당 날짜의 대상 파일을 빠짐없이 열거한다. `검토`는 이름·선언·호출부·역할에 대한 정적 판정이며, 실행 테스트 성공을 뜻하지 않는다. `Nxx`는 위 지적에 연결되고, `—`는 이번 관점에서 별도 지적이 없음을 뜻한다.

### B Java (96개)

| 파일 | 상태 | 관련 발견 |
| --- | --- | --- |
| `B/src/main/java/com/inhatc/demp/DempApplication.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/config/SecurityConfiguration.java` | 검토 | N13 |
| `B/src/main/java/com/inhatc/demp/config/SwaggerConfig.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/config/WebConfig.java` | 검토 | N13 |
| `B/src/main/java/com/inhatc/demp/config/aws/AwsS3Config.java` | 검토 | N07 |
| `B/src/main/java/com/inhatc/demp/config/jwt/JwtAuthenticationFilter.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/config/jwt/JwtTokenProvider.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/config/security/MemberPrincipal.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/config/security/SecurityErrorWriter.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/controller/AnnouncementController.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/controller/AnswerController.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/controller/ExController.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/controller/MemberController.java` | 검토 | N08 |
| `B/src/main/java/com/inhatc/demp/controller/QuestionController.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/domain/Answer.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/Hashtag.java` | 검토 | N01 |
| `B/src/main/java/com/inhatc/demp/domain/Member.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/domain/Question.java` | 검토 | N01,N05 |
| `B/src/main/java/com/inhatc/demp/domain/QuestionHashtag.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/Announcement.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/AnnouncementType.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/Career.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/Company.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/Description.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/JobPosition.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/Language.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/RecruitPeriod.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/domain/announcemnet/UploadFile.java` | 검토 | N05 |
| `B/src/main/java/com/inhatc/demp/dto/announcement/AnnouncementCreateRequest.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/announcement/AnnouncementDetailResponse.java` | 검토 | N05,N07 |
| `B/src/main/java/com/inhatc/demp/dto/announcement/AnnouncementResponse.java` | 검토 | N07 |
| `B/src/main/java/com/inhatc/demp/dto/announcement/AnnouncementScroll.java` | 검토 | N07 |
| `B/src/main/java/com/inhatc/demp/dto/announcement/AnnouncementSearchCondition.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/announcement/ListResponse.java` | 검토 | N09 |
| `B/src/main/java/com/inhatc/demp/dto/answer/AnswerForm.java` | 검토 | N09 |
| `B/src/main/java/com/inhatc/demp/dto/answer/UpdateAnswerForm.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/member/MemberDto.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/member/MemberInfo.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/member/MemberLoginForm.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/member/MemberSaveForm.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/member/MemberSearchCondition.java` | 검토 | N09 |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionAnswer.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionDetail.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionForm.java` | 검토 | N09 |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionList.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionSearchCondition.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/dto/question/QuestionUpdateForm.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/error/ApiErrors.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/error/ApiException.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/error/ErrorResult.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/error/ResourceNotFoundException.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/AnswerRepository.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/HashtagRepository.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/MemberRepository.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/announcement/AnnouncementQueryRepository.java` | 검토 | N02,N08 |
| `B/src/main/java/com/inhatc/demp/repository/announcement/AnnouncementRepository.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/question/OrderByNull.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/repository/question/QuestionQueryRepository.java` | 검토 | N02 |
| `B/src/main/java/com/inhatc/demp/repository/question/QuestionRepository.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/service/AnnouncementService.java` | 검토 | N06,N08 |
| `B/src/main/java/com/inhatc/demp/service/AnswerService.java` | 검토 | N10 |
| `B/src/main/java/com/inhatc/demp/service/ContentSanitizer.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/service/CustomUserDetailService.java` | 검토 | N10 |
| `B/src/main/java/com/inhatc/demp/service/FileService.java` | 검토 | N06 |
| `B/src/main/java/com/inhatc/demp/service/FileStorage.java` | 검토 | — |
| `B/src/main/java/com/inhatc/demp/service/MemberService.java` | 검토 | N08 |
| `B/src/main/java/com/inhatc/demp/service/QuestionService.java` | 검토 | N01,N08 |
| `B/src/test/java/com/inhatc/demp/DempApplicationTests.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/config/ApiSecurityTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/config/DatabaseLifecycleTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/config/LocalDataInitializationTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/config/security/MemberPrincipalTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/controller/AnnouncementControllerTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/controller/AnswerControllerTest.java` | 검토 | N14 |
| `B/src/test/java/com/inhatc/demp/controller/ExControllerTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/controller/MemberControllerTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/controller/QuestionControllerTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/docs/AnnouncementRestDocsTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/docs/AnswerRestDocsTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/docs/ApiErrorRestDocsTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/docs/MemberRestDocsTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/docs/QuestionRestDocsTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/domain/announcemnet/AnnouncementValuesTest.java` | 검토 | N05 |
| `B/src/test/java/com/inhatc/demp/repository/AnnouncementQueryRepositoryTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/repository/AnswerRepositoryTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/repository/QuestionRepositoryTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/AnnouncementServiceTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/AnnouncementUploadCompensationTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/AnswerServiceTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/ContentSanitizerTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/CustomUserDetailServiceTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/FileServiceTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/MemberServiceTest.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/service/QuestionServiceTest.java` | 검토 | N10,N14 |
| `B/src/test/java/com/inhatc/demp/support/MemberSecurityContextFactory.java` | 검토 | — |
| `B/src/test/java/com/inhatc/demp/support/WithMember.java` | 검토 | — |

### F Vue/JS (40개)

| 파일 | 상태 | 관련 발견 |
| --- | --- | --- |
| `F/src/App.vue` | 검토 | — |
| `F/src/components/AccountForm.vue` | 검토 | N11 |
| `F/src/components/Hashtags.vue` | 검토 | N11 |
| `F/src/components/HelloWorld.vue` | 검토 | N15 |
| `F/src/components/LoginForm.vue` | 검토 | — |
| `F/src/components/announcement/AnnouncementDetail.vue` | 검토 | N11 |
| `F/src/components/announcement/AnnouncementFooter.vue` | 검토 | N15 |
| `F/src/components/announcement/AnnouncementHeader.vue` | 검토 | — |
| `F/src/components/announcement/AnnouncementList.vue` | 검토 | N03,N04,N11 |
| `F/src/components/announcement/AnnouncementScroll.vue` | 검토 | — |
| `F/src/components/announcement/AnnouncementWrite.vue` | 검토 | — |
| `F/src/components/common/SafeHtml.vue` | 검토 | — |
| `F/src/components/layout/Header.vue` | 검토 | — |
| `F/src/components/question/QuestionAnswer.vue` | 검토 | N12 |
| `F/src/components/question/QuestionControl.vue` | 검토 | — |
| `F/src/components/question/QuestionDetail.vue` | 검토 | N12 |
| `F/src/components/question/QuestionList.vue` | 검토 | N04,N12 |
| `F/src/components/question/QuestionMenu.vue` | 검토 | N12 |
| `F/src/components/question/QuestionReturn.vue` | 검토 | — |
| `F/src/components/question/QuestionSearch.vue` | 검토 | N04 |
| `F/src/components/question/QuestionWrite.vue` | 검토 | N03 |
| `F/src/views/announcement/AnnouncementDetail.vue` | 검토 | — |
| `F/src/views/announcement/AnnouncementList.vue` | 검토 | — |
| `F/src/views/announcement/AnnouncementWrite.vue` | 검토 | — |
| `F/src/views/question/QuestionDetail.vue` | 검토 | — |
| `F/src/views/question/QuestionList.vue` | 검토 | — |
| `F/src/views/question/QuestionWrite.vue` | 검토 | — |
| `F/src/api/announcements.js` | 검토 | — |
| `F/src/data/positon.js` | 검토 | N11 |
| `F/src/fontAwesomeIcon.js` | 검토 | N15 |
| `F/src/main.js` | 검토 | — |
| `F/src/router/index.js` | 검토 | N11 |
| `F/src/store/index.js` | 검토 | — |
| `F/src/store/modules/Login.js` | 검토 | — |
| `F/tests/unit/AnnouncementDetail.spec.js` | 검토 | — |
| `F/tests/unit/AnnouncementWrite.spec.js` | 검토 | — |
| `F/tests/unit/HtmlRendering.spec.js` | 검토 | — |
| `F/tests/unit/LoginForm.spec.js` | 검토 | — |
| `F/tests/unit/SafeHtml.spec.js` | 검토 | — |
| `F/tests/unit/test-environment.spec.js` | 검토 | — |

### 빌드·실행 스크립트 (6개)

| 파일 | 상태 | 관련 발견 |
| --- | --- | --- |
| `B/build.gradle` | 검토 | — |
| `B/settings.gradle` | 검토 | — |
| `F/server.js` | 검토 | N13 |
| `F/vue.config.js` | 검토 | N13 |
| `F/babel.config.js` | 검토 | — |
| `F/jest.config.js` | 검토 | — |
