> 2026-09-21 변경: 아래는 최초 구현의 검증 기록이다. 사용자 요청으로 Flyway와 migration은 제거했으며 README도 원복했다. 현재 H2 설정·검증은 [리뷰 수정 기록](review-2026-09-21.md)을 따른다.

# T10 데이터 수명 검증

- Phase 1, 브랜치 `refactor/data-preservation-and-security`
- worktree: `.worktrees/data-preservation-and-security/backend`
- 변경은 미커밋 상태로 유지한다.

## Red

Java 11에서 실행:

```sh
./gradlew test --tests 'com.inhatc.demp.config.DatabaseLifecycleTest' --console=plain
```

기존 `ddl-auto=create`와 `InitDb`로 3개 테스트가 의도한 assertion 때문에 실패했다.

- 같은 격리 H2 DB로 두 번째 컨텍스트를 시작한 뒤 `retained-member` 조회 결과가 `Optional.empty`였다.
- 기본 프로필과 test 프로필에서 회원 수가 기대값 0 대신 2였다.
- 최초 시도는 병렬 작업에서 Flyway 의존성이 먼저 추가돼 migration 파일 부재로 실패했다. 이것은 Red로 세지 않았다. 기존 동작 재현 시에만 테스트 인자로 Flyway를 비활성화했고, Green에서는 이 임시 인자를 제거했다.
- Red 콘솔 기록: `/private/tmp/t10-red.log`. 병렬 Gradle 실행으로 XML이 덮어써져 Red XML은 보관되지 않았다. 이후 Gradle 실행을 직렬화한다.

## Green 구현

`InitDb`를 삭제하고 기본 프로필 선택을 호출자에게 돌려주었다. 기본 SQL 초기화는 `never`, Hibernate는 `validate`다. Flyway V1이 빈 MySQL 스키마를 만들며 기존 데이터베이스에는 자동 baseline하지 않는다. local은 전용 파일 H2와 MySQL57Dialect를 사용하고 SQL seed만 선택한다.

```text
Flyway: 스키마 버전과 생성 책임
    ↓
local/data.sql: local 화면 예제만 삽입 (음수 ID, NOT EXISTS)
    ↓
Hibernate validate: 엔티티와 실제 스키마 정합성 검증
```

회원·질문 서비스 테스트에서 자동 seed 기대값을 제거했다. 각 테스트 본문은 필요한 객체를 직접 생성한다. local 자동 테스트도 SQL 실행을 끄고 프로필 계약만 확인한다. local seed 실제 실행은 별도 애플리케이션 실행으로 검증한다.

초기 Green에서 DatabaseLifecycleTest 3개와 QuestionServiceTest 9개를 실행해 성공했다. 콘솔 기록은 `/private/tmp/t10-green.log`다. 이후 local 계약 테스트와 테스트 설정 경로 검증을 추가했으므로 최종 결과는 아래에서 갱신한다.

## 검증 범위와 제한

H2 MySQL 모드에서는 MySQL용 table generator와 엔티티 `validate`가 함께 통과함을 확인한다. 실제 MySQL 서버의 migration 성공으로 표현하지 않는다. 운영 DB와 S3에 연결하지 않는다. 기존 DB의 백업·복원·스키마 대조·명시적 baseline 및 중복 회원명 처리 절차는 README에 기록한다.


## 최종 대상 검증과 실제 local 두 번 실행

`./gradlew test --tests 'com.inhatc.demp.config.DatabaseLifecycleTest' --tests 'com.inhatc.demp.config.LocalDataInitializationTest' bootJar --console=plain`은 4 tests, 실패·오류·skip 0으로 통과했다. 콘솔 `/private/tmp/t10-final-targeted.log`, XML `/private/tmp/t10-final-lifecycle.xml`, `/private/tmp/t10-final-local.xml`을 별도 보관했다.

빌드한 실행 jar를 복사한 뒤 자동 테스트와 별도로 `/private/tmp/verify-demp-local.py /private/tmp/demp-t10-local.jar`를 실행했다. 작업 디렉터리 `/private/tmp/demp-local-verification-mjksmafo`의 기본 local 파일 DB를 사용해 애플리케이션을 두 번 실제 기동하고 정상 종료했다. JVM의 EC2 metadata 조회는 비활성화했다.

| 확인 항목 | 첫 기동 | 두 번째 기동 |
| --- | --- | --- |
| HTTP 가입으로 생성한 회원 ID | 1 | 같은 ID와 이름 재조회 성공 |
| 회원 | 2 (예제 1 + 가입 1) | 2 |
| 권한 행 | 2 | 2 |
| 공고·언어·질문·태그·질문태그·답변 | 각각 1 | 각각 1 |

예제 SQL을 실제로 로드하면서 데이터 유지, seed 중복 방지, 양수 생성 ID와 음수 seed ID의 비충돌을 확인했다. Flyway migration이 먼저 실행된 뒤 local SQL과 Hibernate validate가 정상 완료됐다. 초기 별도 점검 스크립트의 Flyway 이력 테이블 대소문자 조회 오류는 애플리케이션 오류와 구분하고, 해당 조회를 제거·행 개수 assertion을 추가한 새 격리 DB 실행이 exit 0으로 완료됐다. 최종 콘솔은 `/private/tmp/t10-local-execution.log`, 각 시작 로그와 DB 조회는 위 작업 디렉터리에 보관했다.

최종 Phase 1 전체 테스트는 부모 작업의 회원·보안 변경을 통합한 뒤 별도로 실행한다. 여기의 성공은 T10 대상 4개와 초기 질문 서비스 9개, 실제 local 두 번 기동 범위다.

## V2 및 회원·보안 통합 후 실제 local 재검증

최신 `bootJar`를 `/private/tmp/demp-t10-local-integrated.jar`로 복사해 새로운 격리 작업 디렉터리 `/private/tmp/demp-local-verification-ei0fd2dj`에서 두 번 기동했다. 두 번 모두 `local-member` / `password`의 HTTP 로그인이 성공하고 JWT가 반환됐다(토큰 값은 기록하지 않음). HTTP 가입으로 생성한 양수 ID 1의 회원은 재시작 뒤 유지됐고, 가입·조회 응답에 password 필드가 없었다. 회원 2, 권한 2, 나머지 여섯 예제 테이블 각각 1행으로 두 번의 개수가 같았다.

별도 JDBC 검사에서 예제 계정과 HTTP 가입 계정의 저장값은 평문이 아니며 BCrypt `matches("password", hash)`가 모두 참이었다. Flyway 이력에서 V1과 V2가 모두 성공했다. 임시 DB의 트랜잭션 안에서 중복 username 삽입은 SQLState `23505`, NULL username 삽입은 `23502`로 거부됐고 각 시도 뒤 rollback했다. 최초 이력 점검 스크립트가 version이 NULL인 Flyway 스키마 생성 표시 행까지 migration 개수에 포함한 오류를 수정해, version이 있는 이력만 확인한 최종 점검은 exit 0으로 완료됐다.

통합 실행 콘솔: `/private/tmp/t10-local-integrated-execution.log`. 부팅·행 개수·제약 검사 결과는 위 작업 디렉터리의 `boot-1.log`, `boot-2.log`, `database-1.txt`, `database-2.txt`, `integrated-constraints.txt`다. 이 추가 검증은 Gradle을 다시 실행하지 않고 부모가 전체 검증한 최신 jar로 수행했다.
