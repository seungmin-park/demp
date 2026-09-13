# Phase 0 리뷰 수정

2026-09-14. 원본 저장소의 `refactor/build-and-test-foundation`에서 미커밋 변경으로 유지한다.

## 관찰한 실패와 변경

- `./gradlew clean test asciidoctor bootJar --console=plain`: 61개 중 4개 실패. [초기 실패](baseline-failures.json)는 InitDb 데이터가 존재하는데 전체 DB가 비었다고 가정한 assertion과 초기 회원까지 삭제한 cleanup의 FK 오류다. 기능 Red가 아니라 기존 테스트의 잘못된 전제다.
- InitDb는 그대로 실행하고, Service 테스트는 자신이 만든 회원·질문·답변·태그만 삭제한다. 초기 태그와 회원을 포함한 정확한 결과를 검증하고, 실패 요청은 기존 엔티티 ID 목록이 유지되는지 검증한다. InitDb를 삭제하는 T10에서 seed 기대값도 함께 변경한다.
- Controller와 REST Docs는 `@WebMvcTest`에서 실제 MVC·보안 설정을 사용한다. `@ContextConfiguration`으로 Controller·예외 처리기·MVC·보안 설정을 지정하여 애플리케이션의 JPA auditing 설정을 가져오지 않는다. Service와 직접 의존하는 Repository, JWT 공급자는 `@MockBean`으로 교체한다. REST Docs는 `@AutoConfigureRestDocs`를 사용한다.
- 모든 테스트 메서드에 한글 `@DisplayName`을 추가했다. `DempApplicationTests`는 이 애너테이션과 import만 추가하고 기존 전체 컨텍스트 기동 테스트를 유지했다.
- 개발 확인용 로그와 불필요한 Slf4j 선언을 제거했다. 로그 전용 테스트는 제거 전에 4건의 실제 출력 실패를 재현했지만, 사용자의 삭제 요청에 따라 최종 코드에 남기지 않았다. 기존 Controller·문서 테스트로 HTTP 동작을 검증한다.
- 단순 생성자 호출 람다를 생성자 메서드 참조로 바꿨다. Repository 테스트의 `@Import(AnnouncementQueryRepository.class)`는 JPA slice에 직접 작성한 조회 클래스를 등록하기 위해 유지했다.
- AsciiDoc은 표준 `include::`와 각 소스 파일 기준 snippet 경로를 사용한다. Gradle HTML 생성 및 79개 snippet 경로를 검증했다. IntelliJ의 실제 미리보기 화면은 직접 확인하지 않았다.

## 검증

Java 11에서 `./gradlew clean test asciidoctor bootJar --console=plain` 성공. 마지막 생성자 참조 변경 후 `./gradlew test asciidoctor bootJar --console=plain`으로 재검증한다. 최종 수치와 미해결 문서 참조는 [결과](final-results.json)에 기록한다. 기존 unchecked/varargs 및 Gradle deprecation 경고는 남아 있다. commit은 수행하지 않는다.
