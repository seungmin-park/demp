# 전체 백엔드 테스트 리뷰와 개선

2026-09-14. 기존 백엔드 테스트 14개 클래스를 모두 검토했다. 새로운 Phase를 시작하지 않고 기존 `refactor/build-and-test-foundation`의 미커밋 변경을 개선했다. 프로덕션 동작은 이번 작업에서 변경하지 않았다.

## Inflearn 근거

- 박우빈, [한 눈에 들어오는 Test Fixture 구성하기](https://www.inflearn.com/courses/lecture?courseId=329295&unitId=152676): 테스트에 중요한 값은 드러내고 공통 데이터에 대한 결합을 줄인다.
- 박우빈, [ParameterizedTest](https://www.inflearn.com/courses/lecture?courseId=329295&unitId=152678): 같은 동작을 여러 입력으로 검증할 때 파라미터 테스트를 사용한다.
- 박우빈, [Presentation Layer 테스트 (2)](https://www.inflearn.com/courses/lecture?courseId=329295&unitId=150673): Spring MVC, 하위 서비스 mock, ObjectMapper 직렬화 및 HTTP 계약 검증을 사용한다.

아래 판단은 해당 원칙을 현재 코드에 적용한 리뷰이며 강사가 이 저장소를 직접 리뷰한 결과는 아니다.

## 클래스별 관찰과 조치

| 테스트 | 관찰 | 조치 |
| --- | --- | --- |
| AnnouncementQueryRepositoryTest | 고정 20개 데이터, 일부 제목만 확인, 페이지 크기와 실제 원소 수 혼동, hasNext 누락 | 검색별 2~3개 데이터와 정확한 제목 집합, null·공백·미일치·급여 경계, 페이지·슬라이스 6가지 경계 및 중복·검색 조건 적용 검증 |
| AnswerRepositoryTest | 정상 조회만 있고 Service 테스트에 Repository 삭제 검증이 섞임 | 조회 ID 확인, 빈 결과 추가, 답변 단독 삭제와 다른 답변 보존 검증을 이동 |
| QuestionRepositoryTest | 정렬 없는 조회의 순서 가정, 필드별 assertion으로 동일 객체의 값 관계가 불명확 | 제목·조회수·추천수·작성자를 tuple로 묶어 순서와 무관하게 정확히 검증 |
| MemberServiceTest | 저장 존재만 확인, 이름 공백 경계 없음 | 별도 조회로 저장 필드와 권한 검증, null·빈 문자열·공백 이름 검증 |
| QuestionServiceTest | 없는 회원·없는 질문을 한 요청에서 동시에 사용, 준비 과정이 join에 의존, 저장소 삭제가 잘못 분류됨 | 실패 전제 분리, 조회·수정·삭제 fixture를 Repository로 직접 준비, 저장소 삭제 테스트 이동, 테스트 전용 회원 이름 분리 |
| AnnouncementControllerTest | 404만 검증 | 검색어와 페이지 번호·크기가 실제 서비스 인자로 전달되는지 추가 검증 |
| AnswerControllerTest | 정상 변경만 검증 | 빈 답변 목록과 필수 ID 누락 시 하위 계층 미호출 검증 |
| MemberControllerTest | 회원 조회만 검증 | 중복 확인 true/false 응답 및 username 전달 검증 |
| QuestionControllerTest | 등록 요청 전달과 바인딩 거절 사례 부족 | ObjectMapper로 등록 DTO 전달 검증, 잘못된 숫자·범위 초과·누락 ID의 400 및 서비스 미호출 검증 |
| AnnouncementRestDocsTest | 구조 문서화는 있으나 실제 목록값과 검색 전달값 검증 부족 | 응답 ID·제목·크기·last 확인, 검색 DTO와 Pageable이 일치하는 경우만 mock 응답 제공 |
| AnswerRestDocsTest | 응답 구조 외 값 검증 부족, 수정·삭제 요청이 무시돼도 통과 가능 | 응답값·빈 응답 확인, 등록 요청 DTO 일치, 수정 내용·삭제 ID 전달 확인 |
| MemberRestDocsTest | 응답 구조 외 값 검증 부족 | 회원 ID·이름·JWT·중복 확인 boolean 확인, 로그인 DTO 일치 검증 |
| QuestionRestDocsTest | 응답 구조 외 값과 요청 전달 검증 부족 | 조회값·태그·등록 응답 확인, 검색 DTO 일치와 등록·수정·삭제 전달 계약 검증 |
| DempApplicationTests | 전체 애플리케이션 기동 확인 역할에 맞음 | 원래 기동 테스트 유지. InitDb 제외나 mock 추가 없음 |

공통 정리: 사용하지 않는 JwtTokenProvider 필드는 클래스 단위 `@MockBean(JwtTokenProvider.class)`으로 바꿨다. 명시적 import를 정렬하고 중복·불필요 import를 제거했다. `@Test` 또는 `@ParameterizedTest` 다음에 한글 `@DisplayName`을 둔다.

## 실행 검증과 실패 구분

이 작업은 테스트 리팩터링과 기존 동작의 검증 보강이다. 새로운 기능 Red라고 보고하지 않는다. 공고 Repository 보강 테스트는 기존 구현에서 통과했다.

- 공고 테스트 단독: `./gradlew test --tests 'com.inhatc.demp.repository.AnnouncementQueryRepositoryTest' --console=plain` 성공.
- Service 변경 중 중복 변수 선언은 컴파일 오류였다. 수정 후 실행했다.
- 회원 저장 assertion에서 Hibernate 권한 컬렉션을 일반 List와 객체 비교해 실패했다. 출력값은 같지만 컬렉션의 동등성 의미가 달랐다. 회원 필드와 권한 원소를 각각 검증하도록 바로잡았다. 프로덕션 오류가 아니다.
- MVC·문서 보강 후 전체 테스트 성공.
- 최종 `./gradlew clean test asciidoctor bootJar --console=plain`: **14 suites, 95 tests, 실패·오류·skip 0**. Java 11 사용.
- 기존 테스트의 varargs 컴파일 경고는 타입이 있는 메서드 참조와 List 기대값으로 해소했다. 기존 프로덕션 unchecked 및 Gradle deprecation 경고는 남아 있다.

## 남아 있는 프로덕션 계약

- 공고 쿼리에 정렬 계약이 없다. 테스트는 삽입 순서를 기대하지 않는다. 정렬과 안정적인 페이지 간 원소 순서 보장은 T32에서 구현·검증한다.
- 공고 multipart 등록 검증 오류, 오류 HTTP 상태 불일치, 정상 상세 직렬화 문제는 T14/T20 범위다. 기존 실패 문서 테스트를 성공으로 위장하지 않았다.
- 답변 수정은 Controller의 메모리 변경만 검증한다. 실제 저장 commit 보장은 T12/T30이다.
- 질문 태그는 현재 추가 동작을 검증한다. 교체·중복 엔티티 정책은 T31이다.
- InitDb는 유지한다. 초기 회원·태그를 포함하는 기대값은 T10에서 삭제와 함께 변경한다.

새로운 Domain·보안·파일 저장 계층 전체의 기능 커버리지를 완성했다는 의미는 아니다. 이번 범위는 기존 백엔드 테스트 전체의 리뷰와 개선이다. 커밋하지 않았다.
