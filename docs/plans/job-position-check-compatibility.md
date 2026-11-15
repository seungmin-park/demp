# 운영 분야 CHECK 호환

- Phase: 운영 데이터 호환 후속 (기술 CHECK 호환 #16과 같은 결함 유형)
- 브랜치: `refactor/job-position-check-compatibility`
- worktree: `.worktrees/job-position-check-compatibility`
- 근거: 새 분야로 공고를 저장하면 500이 난다는 보고. 운영 `language_chk_1`이 #10 이전 6개 기술만 허용하던 기록([운영 공고 주입](../verification/production-postings-20261013/README.md))

## 문제

운영 스키마는 `Language`가 6개 값이던 시기(#10 이전)에 Hibernate가 enum 컬럼마다 인라인 CHECK를 붙여 만들었다. 같은 시기 `announcement.job_position`도 `@Enumerated(STRING)` + VARCHAR였고 `JobPosition`은 21개였다. #10에서 추가한 10개 분야(DATA_SCIENTIST, DATA_ANALYST, DATABASE_ADMIN, CLOUD_ENGINEER, PLATFORM_ENGINEER, SRE, MLOPS, WEB_PUBLISHER, GAME_ENGINE, AI_RESEARCH)는 이 CHECK에 걸려 MySQL 3819 → 트랜잭션 롤백 → HTTP 500이 된다. `ddl-auto=validate`와 새 빈 스키마 검증은 CHECK의 허용 집합을 보지 않으므로 발견하지 못한다.

```text
관리자 저장(job_position=SRE) → INSERT announcement → 낡은 CHECK(21개) → 3819 → 롤백 → 500
```

같은 시점 이후 값이 늘어난 enum 컬럼을 모두 비교했다. `Language`(이미 #16에서 수정)와 `JobPosition`만 늘었다. `announcement_role.position`은 #13 수동 SQL로 CHECK 없이 만든 VARCHAR이고, #10 이후 생긴 enum 컬럼(`DeadlineMode`, `SalaryPeriod` 등)도 수동 SQL로 CHECK 없이 추가했다. 수동 SQL이 직접 만든 enum CHECK는 `content-reaction.sql`의 `ck_reaction_value` 하나이고 현재 `ReactionType` 3개 값과 같다.

기술 CHECK와 다른 점: `announcement`에는 `announcement_type` CHECK도 있어 Hibernate가 붙인 `announcement_chk_N` 번호가 컬럼 순서에 따라 달라진다. 운영의 실제 번호는 확인하지 않았으므로 이름을 고정한 `DROP CHECK announcement_chk_2`는 다른 제약을 지울 위험이 있다.

## 보장할 결과

1. 수동 SQL을 적용한 DB는 현재 31개 분야를 모두 저장하고, 알 수 없는 분야는 계속 3819로 거절한다.
2. 분야 없는 초안(NULL)과 `announcement_type` CHECK는 그대로다. 기존 행 값은 바뀌지 않는다.
3. 제약 이름을 가정하지 않는다. 분야 CHECK가 몇 번째든, 아예 없든 같은 SQL로 처리하고, 다시 실행해도 안전하다(다음 분야 확장 때 같은 파일을 갱신해 재적용).
4. `JobPosition`이 바뀌었는데 SQL을 갱신하지 않으면 CI가 실패한다.

## 객체 책임

- 분야 집합: `JobPosition` enum (변경 없음).
- 운영 CHECK 호환: `db/manual/announcement-job-position-check.sql`. `information_schema`에서 `announcement`의 CHECK 중 절에 `` `job_position` ``이 있는 것을 찾아 PREPARE로 지운 뒤 `announcement_job_position_chk`를 추가한다. 두 개 이상이면 서브쿼리 오류로 아무것도 바꾸지 않고 멈춘다.
- 드리프트 거절: `JobPositionCheckMigrationTest` (필수 suite).
- 실제 동작: `docs/verification/job-position-check-compatibility/mysql_rehearsal.sh`.
- 코드·API·DB 컬럼 이름과 호출 계약은 바꾸지 않았다. 3819를 500 대신 다른 응답으로 바꾸는 일은 원인 제거 후에는 발생하지 않아야 하므로 이번 범위에 넣지 않았다.

## 운영 적용 (수동, 운영자)

1. 쓰기 중지·백업 후 현재 제약을 확인한다: `SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE FROM information_schema.TABLE_CONSTRAINTS tc JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'announcement';`
2. 별도 복원 DB에서 `mysql <복원 DB> < announcement-job-position-check.sql`을 실행하고, 기존 21개 외 분야(SRE)가 적용 전 3819·적용 후 저장되는지, 알 수 없는 값이 거절되는지, 기존 `job_position` 값 해시가 같은지 확인한다.
3. 같은 SQL을 운영에 적용하고 관리자 화면에서 새 분야 공고를 저장·재조회한다.

## 검증 기록

- 원인 재현 (임시 MySQL 26.7.0, scratchpad 데이터 디렉터리, 소켓 전용): #10 이전 Hibernate DDL 모양의 `announcement`(announcement_type CHECK → job_position CHECK 순)에 `SRE` 저장 → `ERROR 3819: Check constraint 'announcement_chk_2' is violated`. 기존 `BACKEND`·NULL은 저장됨.
- Red: `./gradlew test --tests 'com.inhatc.demp.config.JobPositionCheckMigrationTest'` → 2건 모두 `[운영 분야 CHECK 갱신 SQL이 패키징되어야 한다] Expecting actual not to be null` (SQL 부재로 인한 의도한 assertion 실패).
- Green: `announcement-job-position-check.sql` 추가 → 2건 통과. 첫 실행에서 SQL 주석의 `announcement_chk_N` 글자가 "고정 이름 금지" 검사에 걸려 1건 실패했고, assertion을 바꾸지 않고 주석 표현을 고쳤다.
- 실제 MySQL 리허설: `mysql_rehearsal.sh` ALL PASS — 분야 CHECK가 chk_2인 경우·chk_1인 경우 각각 적용 전 SRE 3819(Red) → 적용 후 31개 허용(롤백)·INVALID 3819·NULL 허용·announcement_type CHECK 유지·기존 값 해시 동일·분야 CHECK 정확히 1개, 재실행 후 같은 결과. 분야 CHECK가 없는 DB는 추가만 됨. 출력은 [검증 기록](../verification/job-position-check-compatibility/README.md).
- Refactor: 구조 변경 없음. 새 테스트는 기존 `TechnologyCheckMigrationTest`와 같은 구성을 따랐고, 두 테스트의 공통화는 SQL 형태(고정 이름 vs 조회)가 달라 하지 않았다.
- 전체 검증: `JAVA_HOME=<Java 25> bash scripts/verify.sh` exit 0 — Java 테스트 480건(기존 478 + 신규 2), 필수 suite 70(`JobPositionCheckMigrationTest` 추가), 실패·오류·스킵 0, 생성 문서·JAR 일치, 회원·공고 실제 HTTP 계약과 반응 저장 흐름 통과(격리된 메모리 H2). 첫 실행은 로그를 `build/`에 두어 `clean`에 지워졌으므로 중단하고 로그 위치만 바꿔 다시 실행했다.
- 확인하지 않은 것: 운영 MySQL의 실제 제약 이름·버전과 운영 적용. 리허설은 MySQL 26.7.0에서 실행했고, SQL이 쓰는 `information_schema.CHECK_CONSTRAINTS`·`PREPARE`·`DO`는 MySQL 8.0.16 이상에 있다(운영이 3819로 CHECK를 강제하므로 8.0.16 이상으로 판단). 운영 복원 DB 리허설은 운영자 경로로 진행해야 한다.
- 이름·책임 검토: 새 제약 이름 `announcement_job_position_chk`는 대상 테이블·컬럼을 드러내며 자동 번호와 겹치지 않는다. 테스트 이름은 기존 `TechnologyCheckMigrationTest`와 대칭이다. 변경한 프로덕션 코드가 없어 호출부·공개 계약 영향은 없다.
