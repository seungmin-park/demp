# 답변 커서 조회 인덱스 리허설과 판단

2026-10-07. 답변 보호 Phase의 기존 `refactor/bounded-answer-creation-and-read` 브랜치에서 진행했다. worktree는 `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/bounded-answer-creation-and-read/backend`, 기반 HEAD는 `bb6c6cfc1c831d183687b1d2b7d2279db9538408`이다. 아래 성능 리허설은 애플리케이션 조회 SQL·Java 코드·기존 수동 SQL을 변경하지 않고 실행했다. 이후 사용자가 도입을 승인하여 Java의 스키마 생성 매핑과 기존 스키마 검증을 보완했다. 도입 결과 보고 후 GitHub 전달도 승인받았다. 로컬 근거를 실제 merge·배포 완료로 표현하지 않는다.

## 도입 구현

`Answer`의 `@Table(indexes = @Index(...))`에 `idx_answer_question_cursor(question_id, answer_id)`를 선언했다. Hibernate로 새 스키마를 만들 때 인덱스가 생성되며, 기존 운영 스키마에는 검토된 수동 SQL을 적용한다. `ddl-auto=validate`의 자동 생성·인덱스 검증 기능을 추가한 것은 아니다.

새 H2 스키마의 실제 인덱스 컬럼 순서는 `AnswerRepositoryTest`, 기존 레거시 스키마에 수동 SQL을 적용한 뒤의 인덱스·데이터·ID 연속성은 `LegacySchemaCompatibilityTest`가 검사한다. 먼저 두 경로에서 실제 메타데이터가 빈 목록인 assertion 실패를 확인했고, 매핑과 legacy SQL 적용 목록을 보완하여 대상 6건과 전체 백엔드 376건·59 suite가 통과했다. 조회 SQL·응답 계약·기존 수동 SQL은 유지했다. 후속 실행은 [도입 결과](index-adoption-result.json)에, 도입 전의 성능 비교는 아래와 [index-result.json](index-result.json)에 별도로 기록한다.

## 판단

**DEMP의 답변 조회용 인덱스 도입은 필요하다. 배포 DDL은 `(question_id, answer_id)` 복합 인덱스로 확정한다.** 기준 [레거시 스키마](../../../src/test/resources/legacy/hibernate5-schema.sql)의 answer 테이블에는 `PRIMARY KEY(answer_id)`만 있고 question-leading 인덱스가 없다. 이 상태를 실제 JAR로 검증했을 때 대규모 스캔이 발생했다. 따라서 이번 답변 변경에 [answer-cursor-index.sql](../../../src/main/resources/db/manual/answer-cursor-index.sql)을 포함하고, 기준 스키마에서 배포할 때 적용한다.

단독 인덱스도 조회를 개선한다는 관측은 유효하다. 프로젝트의 DDL에서는 질문 선택·커서 범위를 이름과 컬럼 목록으로 명시하는 복합 인덱스를 채택한다. 단독보다 항상 빠르다는 주장이 아니라, PK만 존재하는 배포 기준에 필요한 질문별 경로를 제공하기 위한 결정이다.

아래의 기존 인덱스 확인은 도입 결정을 미루는 조건이 아니라 배포 대상에서 중복 DDL을 피하는 절차다. 실제 운영 DB를 확인했거나 이미 적용했다고 주장하지 않는다.

1. **기존의 사용 가능한 question-leading 인덱스를 우선 재사용한다.** InnoDB에서 `answer_id`가 PK이고 전체 `question_id`를 첫 컬럼으로 갖는 visible 인덱스가 있으면 실제 첫/다음/끝 페이지 계획을 확인한다. 이번 단독·외래 키 자동 인덱스는 두 분포의 모든 페이지에서 대규모 스캔을 없앴다.
2. **적절한 인덱스가 없으면 기존 [answer-cursor-index.sql](../../../src/main/resources/db/manual/answer-cursor-index.sql)의 `(question_id, answer_id)` 인덱스를 백업 후 한 번 적용한다.** 리허설에서는 기존 단독 인덱스를 대체하며 비교했지만, 운영 인덱스 삭제 권한이나 자동 실행 절차를 만들지 않았다.
3. **기존 단독 인덱스 옆에 복합 인덱스를 무조건 추가하지 않는다.** 일부 작은 커서 범위에서 단독/자동 인덱스는 최대 21행 정렬을 선택했지만 답변 전체를 정렬하지 않았다. 복합 인덱스의 작은 시간 차이가 모든 조회에서 우세하지 않았고 인덱스 둘 다 유지할 근거가 되지 않는다.

이는 로컬 MySQL 8.4.11의 현재 SQL과 합성 데이터에 대한 판단이다. 아직 없는 Google Cloud 운영 DB의 인덱스·통계·실제 부하를 확인한 결과로 표현하지 않는다.

## 왜 동작하는가

```text
InnoDB PK = answer_id
    │
    ├─ 단독 secondary(question_id)
    │     내부 키에 answer_id가 붙음
    │     → 질문 선택 → 커서 아래 범위 → 역순 답변 조회
    │
    └─ 명시적 secondary(question_id, answer_id)
          같은 질문·커서 경로를 명시
```

InnoDB가 보조 인덱스 뒤에 PK를 붙이고 optimizer가 그 확장 키를 범위·정렬에 사용할 수 있다는 것은 [MySQL 공식 문서](https://dev.mysql.com/doc/refman/8.4/en/index-extensions.html)의 계약이다. 외래 키 컬럼을 선두로 갖는 인덱스가 없으면 MySQL이 자동 생성한다는 점도 [공식 FK 문서](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html)에서 확인했다. 리허설에서 외래 키를 추가한 뒤 실제 `SHOW CREATE TABLE`에 `KEY fk_rehearsal_answer_question (question_id)`가 생겼다.

인덱스가 존재한다고 모든 계획이 동일해지지는 않는다. [ORDER BY 최적화 문서](https://dev.mysql.com/doc/refman/8.4/en/order-by-optimization.html)와 함께 실제 선택된 range/backward scan·index merge·sort·작성자 PK lookup을 확인했다. 힌트나 `FORCE INDEX`로 유리한 계획을 강제하지 않았다.

## 실행 환경과 검증 범위

현재 cmux 호출은 workspace:3/caller surface:6, 보조 pane:8/runner surface:14이다. 사용자 터미널에는 입력하지 않았다. 기존 앱 surface:12·15·16과 브라우저 surface:13을 유지했다. 새 리허설 JVM만 실행 종료 후 종료했다. DB와 보고서는 남겼다.

- 기존 소유 label `release-20261005`의 loopback 전용 Docker MySQL 8.4.11/InnoDB, 고정 image digest `6ea90827b1100f8f2ae306a539f86d2c264a26ed435a2a9f75551dd5c3aeb242`.
- 검증을 통과한 현재 JAR SHA `9336c0c4ffc484e57201d4fba77c28b436d4ba10b8d2cf46673f08ac53081e01`.
- 새 격리 DB 두 개: `demp_release_20261007_001822`, `demp_release_20261007_002004`. 각각 답변 200,000건·합성 작성자 1,000명·질문 1,002개.
- clustered 분포는 희소 질문 41건·많은 질문 20,000건의 ID가 각각 연속한다. interleaved 분포는 여러 질문의 ID가 섞이고 희소 질문 41건·많은 질문 19,996건이다.
- 분포마다 PK만 존재 → question_id 단독 추가 → 명시적 복합 → 외래 키 자동 인덱스를 순차 비교했다. 원본 candidate 이름 `none`은 question-leading 보조 인덱스가 없다는 뜻이며, **모든 후보에서 answer_id의 PRIMARY KEY 인덱스는 유지했다.** 각 단계의 데이터 CHECKSUM·행수·ID 합계를 확인했다. 질문별 보조 인덱스가 둘 다 있는 상태로 비교하지 않았다.
- 각 후보의 첫/다음/마지막·많은 질문의 중간/끝·빈 질문 등 8개 경로를 현재 JAR의 HTTP로 실행했다. 경로당 warm-up 2회·측정 5회, 합계 페이지 요청 448회·측정 SQL 320개·명시적 assertion 82개, 두 명령 모두 종료 0.
- 기존 `performance_schema.events_statements_history`에서 **실제로 실행된 SQL**·Rows_examined·Rows_sent·Sort_rows·SQL TIMER_WAIT를 읽고 같은 SQL로 EXPLAIN JSON/ANALYZE를 실행했다. 시스템 전역 설정을 바꾸거나 캐시를 비우지 않았다. API 시간과 SQL 시간을 별도로 기록했다.
- 정확한 문자열 답변 ID·내림차순 전체 페이지 내용·20건·hasNext·커서·NONE 반응을 매 요청 검증했다. DTO 쿼리의 SQL 반환은 최대 21행이다.

실제 첫 페이지 SQL 예:

```sql
select a1_0.answer_id,m1_0.username,a1_0.content,a1_0.recommend,a1_0.dislike
from answer a1_0
join member m1_0 on m1_0.member_id=a1_0.member_id
where a1_0.question_id=3000
  and (null is null or a1_0.answer_id<null)
order by a1_0.answer_id desc limit 21
```

다음 페이지도 실제 Hibernate SQL의 `(before is null or answer_id < before)` 형태를 그대로 측정했다. 수작업으로 더 유리한 WHERE 조건으로 바꾼 결과가 아니다.

## 읽은 행 비교

아래는 답변 DTO **조회 SQL**의 `Rows_examined`다. 작성자 join·중간 처리도 포함하며 API 전체 쿼리나 답변 엔티티 수가 아니다. 일반적인 42는 답변 21행과 작성자 PK lookup 21회이며, 일부 63은 index merge와 21행 정렬을 포함한다. 각각의 원본 계획을 [index-result.json](index-result.json)에 보존했다.

### clustered

| 조회 | PK만 존재 | question_id 단독 | 명시적 복합 | 외래 키 자동 |
|---|---:|---:|---:|---:|
| sparse-first | 200001 | 42 | 42 | 42 |
| sparse-next | 42 | 42 | 42 | 42 |
| sparse-last | 10 | 10 | 10 | 10 |
| hot-first | 180001 | 42 | 42 | 42 |
| hot-middle | 42 | 42 | 42 | 42 |
| hot-near-end | 42 | 63 | 42 | 63 |
| hot-last | 51 | 10 | 10 | 15 |
| empty | 200000 | 0 | 0 | 0 |

### interleaved

| 조회 | PK만 존재 | question_id 단독 | 명시적 복합 | 외래 키 자동 |
|---|---:|---:|---:|---:|
| sparse-first | 97605 | 42 | 42 | 42 |
| sparse-next | 102438 | 63 | 42 | 63 |
| sparse-last | 29266 | 10 | 10 | 15 |
| hot-first | 222 | 42 | 42 | 42 |
| hot-middle | 231 | 42 | 42 | 42 |
| hot-near-end | 231 | 63 | 42 | 63 |
| hot-last | 64 | 10 | 10 | 15 |
| empty | 200000 | 0 | 0 | 0 |

PK만 존재하는 clustered 희소 첫 페이지의 SQL 중앙값은 42.217ms, question_id 단독 인덱스를 추가하면 0.147ms였다. interleaved 희소 첫 페이지는 21.443ms → 0.172ms였다. 그러나 질문별 보조 인덱스 후보끼리의 0.1~0.2ms대 차이로 운영 SLA나 개선 배율을 주장하지 않는다. Docker·warm cache·고정 후보 순서·합성 본문·단일 요청 조건이며 동시 부하·운영 본문 분포를 검증한 결과가 아니다. 원본에는 각 5회 값·범위도 기록했다.

단독·복합 인덱스는 각각 353페이지, FK 생성 인덱스는 481페이지를 사용했다(페이지당 16KiB). 생성 경로·통계·페이지 배치가 다르므로 키 이름만으로 차이가 생겼다고 해석하지 않는다. 두 인덱스를 같이 유지하는 쓰기 비용을 측정한 결과도 아니다.

## 배포 시 실행할 판별 순서

```sql
SELECT VERSION(), @@default_storage_engine;
SHOW CREATE TABLE answer;
SHOW INDEX FROM answer;
```

1. 실제 엔진이 InnoDB이고 PK가 answer_id인지, 인덱스가 visible인지, 전체 question_id가 첫 컬럼인지 확인한다. 같은 목적의 기존 복합/FK 자동 인덱스도 포함한다.
2. 실제 데이터와 위 DTO SQL로 첫 페이지·중간 커서·끝·빈 질문의 EXPLAIN/가능한 EXPLAIN ANALYZE를 확인한다. LIMIT 반환 수만 보지 말고 scan rows·sort 범위·실제 인덱스 사용을 읽는다.
3. 적절한 경로가 있으면 추가하지 않는다. 없으면 백업·DDL 실행 시간과 잠금 가능 범위를 확인한 후 기존 수동 복합 SQL을 적용하고 같은 계획·페이지 내용으로 검증한다.
4. 인덱스를 삭제하거나 교체해야 한다면 FK가 의존하는 인덱스인지 별도로 확인한다. 이 리허설은 운영 삭제·교체를 수행하거나 승인한 것이 아니다. `ddl-auto=validate`가 인덱스 누락을 검출한다고 가정하지 않는다.

현재 리허설은 본문 길이·생성 제한값·새 quota table·조회 힌트를 추가하지 않았고 운영 DB·GCP 인스턴스를 만들거나 변경하지 않았다.

## 재현과 원본 근거

같은 worktree에서 기존 소유 로컬 MySQL이 실행 중이고 합성 credential 파일이 있을 때 보조 cmux 터미널에서 순차 실행한다:

```sh
python3 docs/verification/bounded-answer-creation-and-read/index_rehearsal.py
python3 docs/verification/bounded-answer-creation-and-read/index_rehearsal.py --interleaved
```

이 두 줄을 하나의 cmux send로 동시에 입력하지 않는다. 각 명령의 종료를 확인한 뒤 다음을 실행한다. 원본: `/private/tmp/demp-bounded-answers-20261006/index-rehearsal.log`, `index-interleaved-rehearsal.log`와 각 명령 JSON, `index-rehearsal/20261007-001822/result.json`, `index-rehearsal/20261007-002004/result.json`, 각 격리 JVM의 `index-api.log`. 첫 결과는 분포 선택 옵션을 넣기 전의 script SHA, 두 번째는 최종 script SHA를 기록한다. 원본을 덮어쓰지 않았다.
