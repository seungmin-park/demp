# Phase 0 실행·검증 기록

검증일: 2026-09-14. 브랜치: `refactor/build-and-test-foundation` (백엔드·프런트 각각).

## 구현과 재사용

현재 계획을 기준으로 이전 Phase 0 커밋의 코드를 검토해 재사용했다. 과거 통과 결과를 현재 결과로 대체하지 않고 아래 명령을 새로 실행했다.

- T00 타입 정합성: `735e768`의 InitDb/AnnouncementController/404 테스트.
- T01 테스트 경계: `ed673f1`, 복수 결과 assertion 보존: `fbecfdc`.
- T02 독립 REST Docs: `ade7e8e`/`356f8ed`. 정상 multipart를 가장한 Validator mock은 가져오지 않았다.
- 프런트 runner/config/lock: `0887907`. 현재 Red/Green은 [프런트 기록](../../../../dempfrontend/docs/phase0-frontend-verification.md) 참조.
- 이번 리뷰 보완: 현재 태그 추가 동작과 무시되는 공고 sort를 문서에 명시하고, 실제 multipart 실패를 별도 문서 테스트로 추가했다.

## T00 기준선

Java 11: Zulu 11.74.15. 명령 앞에 `JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-11.74.15/zulu-11.jdk/Contents/Home`를 적용했다.

1. `./gradlew test --console=plain`: compileQuerydsl missing symbol 4건, 테스트 미실행.
2. 기계적 타입/생성자 정합성 복원 후 `./gradlew compileJava compileTestJava test --console=plain`: 컴파일 성공, 테스트 43개 중 42개는 S3_ACCESS_KEY placeholder 누락으로 실패.
3. 테스트 전용 설정과 EC2 metadata 탐색 비활성 후 `./gradlew test --console=plain`: 43/43 통과.
4. 공고 404는 첫 실행부터 통과한 기존 동작 특성화다. 기능 Red로 보고하지 않는다.

샌드박스의 Gradle 캐시 lock 쓰기 거절은 환경 실패로 구분했으며 권한 부여 후 재실행했다.

## T01 Refactor 검증

다음 순서로 테스트 클래스 하나를 전환하고 매번 `./gradlew test --console=plain` 전체를 실행했다. 9회 모두 성공했다.

1. AnswerControllerTest
2. MemberControllerTest
3. QuestionControllerTest
4. AnnouncementQueryRepositoryTest
5. AnswerRepositoryTest
6. QuestionRepositoryTest
7. MemberServiceTest
8. QuestionServiceTest
9. DempApplicationTests

Controller 4개, Repository 3개, Service 2개, DempApplicationTests는 각각 `./gradlew test --tests 'com.inhatc.demp.<package.Class>' --console=plain` 단독 실행도 성공했다.

```text
Controller/REST Docs: @WebMvcTest → 실제 Spring MVC → mock 서비스 → HTTP 계약
Repository: @DataJpaTest → 직접 생성 데이터 → rollback
Service: 실제 서비스 commit → 별도 조회 → @AfterEach cleanup
```

Service의 테스트용 @Transactional과 데이터 생성 @BeforeEach를 제거했다. 테스트별 직접 fixture를 사용하며 InitDb는 전체 컨텍스트 테스트에서 그대로 실행한다. cleanup은 테스트가 생성한 데이터만 삭제한다. seed 관련 기대값은 InitDb를 삭제하는 T10에서 함께 변경한다. AnswerController의 기존 Repository 경계는 mock 처리했다. 서비스로 이동하는 생산 코드 변경은 T12/T30이다.

## T02 Red → Green

명령: `./gradlew test --tests 'com.inhatc.demp.docs.MemberRestDocsTest.documentsMemberGet' --console=plain`.

username descriptor를 누락한 테스트가 실제 `SnippetException: payload not documented`로 실패했다. [실패 근거](restdocs-red.txt)를 보존했다. descriptor 복원 후 `./gradlew test --tests 'com.inhatc.demp.docs.*RestDocsTest' asciidoctor --console=plain` 성공.

추가한 multipart 테스트는 실제 Validator와 ExController를 사용한다. HTTP 200/본문 400, UnexpectedTypeException, 저장 서비스 미호출을 검증한다. 기존 결함 특성화이며 정상 등록 성공이나 기능 Red가 아니다.

최종 전체 검증 명령: `./gradlew clean test asciidoctor bootJar --console=plain`.
집계 및 문서 참조 검사: [최종 결과](final-results.json). 추적 중인 static/docs는 변경하지 않았다.

## 남은 범위와 리뷰

- 정상 공고 등록과 상세 응답 계약: T20. Phase 0에서 성공을 보장하지 않는다.
- 오류 상태·메시지 보호: T14. 현재 실패 응답의 결함을 문서에 명시했다.
- 태그 교체: T31. 현재는 추가 동작이다.
- 공고 Pageable sort 적용: T32. 현재 메타데이터는 실제 조회 순서를 보장하지 않는다.
- Gradle deprecation, 기존 unchecked/varargs 컴파일 경고가 남는다. 프런트 caniuse-lite와 entrypoint 크기 경고가 남는다.
- 독립 정적 리뷰에서 태그/정렬 설명 2건을 수정했고, 실제 multipart 실패 문서 방식도 재검토했다.

현재 산출물은 원본 backend/frontend 저장소의 `refactor/build-and-test-foundation` 브랜치에서 미커밋 상태로 리뷰할 수 있다. 사용자 요청에 따라 구현 커밋을 되돌리고 별도 worktree를 제거했다. 이번 수정도 commit하지 않았다.

최종 백엔드 검증: 14 suites / 95 tests, 실패·오류·skip 0. `clean test asciidoctor bootJar` 성공. 문서 snippet 참조 79개 누락 0, multipart 실패 절의 생성 HTML 포함 확인.
