# T31 태그 관계·검색 검증

## 실행 흐름과 책임

```text
QuestionService ── 이름 정규화·조회/재사용 ──► HashtagRepository
       │
       └── replaceHashtags ──► Question ── 양방향 관계 ──► QuestionHashtag / Hashtag

QuestionQueryRepository ── 검색 조건 ──► 태그 없는 질문도 포함하는 LEFT JOIN
```

서비스는 입력 태그의 양끝 공백과 빈 값·중복을 정리하고, 이름으로 기존 `Hashtag`를 찾는다. 대소문자는 보존한다. `Question`은 자신의 이전 관계를 제거하고 새 관계를 맺는다. 따라서 한 질문에서 JAVA를 빼도 다른 질문의 JAVA 연결과 태그 레코드는 남는다. 태그 이름은 DB에서도 unique로 보호한다.

## Red → Green → Refactor

- `QuestionServiceTest.updateQuestion`: `[test-java] → [test-jpa]` 기대값으로 변경한 후 기존 누적 동작 때문에 assertion 실패. `Question.replaceHashtags`로 Green.
- `QuestionServiceTest.reusesHashtagByName`: 두 질문의 JAVA가 두 레코드로 저장되어 assertion 실패. 이름 조회·재사용 및 unique 제약으로 Green.
- `QuestionQueryRepositoryTest.includesQuestionWithoutHashtags`: 조건 없는 검색과 빈 태그 목록 조회에서 무태그 질문이 누락되어 각각 assertion 실패. 두 조회의 내부 조인을 LEFT JOIN으로 바꿔 Green.
- 두 질문의 태그 공유, trim·중복 제거·대소문자 보존, OR 태그 + AND 제목/내용, 결과 중복 없음, 동일 태그 재수정, 도메인 양방향 연결은 통과 상태를 고정한 추가 검증이다. 이를 별도의 Red 사례로 주장하지 않는다.
- 기존 테스트 helper가 같은 태그를 새 엔티티로 중복 저장해 unique 제약에서 실패했다. 서비스의 공개 생성 경로를 사용하도록 바꿨다. 테스트 트랜잭션 없이 분리된 서비스 호출 간 LAZY 연관을 직접 건드리지 않게 된 이유도 여기에 있다.

검증 명령은 Java 11에서 `./gradlew test --tests 'com.inhatc.demp.service.QuestionServiceTest' --tests 'com.inhatc.demp.repository.QuestionQueryRepositoryTest' --console=plain`과 `./gradlew test --console=plain`이다. Refactor 후 전체 JUnit XML은 29 suite·176 test·실패/오류/건너뜀 0이다. 테스트는 H2에서 실행했으며 운영 DB의 중복 데이터나 스키마 변경은 실행하지 않았다.

## 기존 데이터에 unique 제약을 적용하기 전

현재 작업은 자동 마이그레이션을 추가하지 않는다. 기존 DB에 같은 `tag_name`이 있다면 바로 unique 제약을 적용하면 실패한다. 배포 전 쓰기 중지와 백업을 한 뒤 하나의 DB 트랜잭션에서 다음 순서로 처리한다.

1. `SELECT tag_name, COUNT(*) FROM hashtag GROUP BY tag_name HAVING COUNT(*) > 1`로 중복 이름을 확정한다. 대소문자 비교는 운영 DB collation과 계약을 맞춘다.
2. 이름마다 대표 `hashtag_id`(예: 최솟값)를 정하고, 나머지 ID를 참조하는 `question_hashtag.hashtag_id`를 대표 ID로 바꾼다.
3. 이관 뒤 `(question_id, hashtag_id)`가 중복된 관계는 각 쌍에서 한 행만 남긴다. 다른 질문의 관계는 삭제하지 않는다.
4. 참조되지 않는 비대표 `hashtag` 행을 삭제한다. 중복 이름과 잘못된 FK 참조가 0개인지 다시 조회한다.
5. `tag_name`에 NOT NULL/UNIQUE 제약을 적용하고, 두 질문이 같은 태그 ID를 참조하며 한쪽 수정이 다른 쪽을 바꾸지 않는지 격리 DB에서 확인한다.

운영 DB의 스키마·collation은 여기서 실행 검증하지 않았다. 실제 DDL과 이관 SQL은 운영 DB 엔진에 맞춰 별도 리허설을 거쳐야 한다.
