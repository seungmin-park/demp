# 운영 분야 CHECK 호환 검증

[작업 기록](../../plans/job-position-check-compatibility.md)의 실행 근거다. 운영 DB에 접속하거나 운영에 적용하지 않았다.

## 실행 환경

- MySQL 26.7.0 (Homebrew), scratchpad의 새 데이터 디렉터리, `--skip-networking` 소켓 전용 임시 인스턴스. 검증 후 종료.
- 명령: `bash docs/verification/job-position-check-compatibility/mysql_rehearsal.sh <socket>`
- 스크립트는 `demp_jobpos_*` 데이터베이스만 만들고 지운다. #10 이전 Hibernate DDL 모양(enum 컬럼마다 인라인 CHECK)을 두 가지 컬럼 순서로 만들고, 분야 CHECK가 없는 DB도 확인한다.

## 결과

```text
-- demp_jobpos_type_first before: announcement_chk_1=0 announcement_chk_2=1 
PASS: demp_jobpos_type_first: Red reproduced, SRE rejected with 3819 before migration
PASS: demp_jobpos_type_first: all 31 current positions accepted (rolled back)
PASS: demp_jobpos_type_first: unknown position still rejected with 3819
PASS: demp_jobpos_type_first: NULL position (draft) still accepted
PASS: demp_jobpos_type_first: announcement_type CHECK preserved
PASS: demp_jobpos_type_first: stored values digest unchanged
PASS: demp_jobpos_type_first: exactly one job_position CHECK (announcement_job_position_chk)
PASS: demp_jobpos_type_first: all 31 current positions accepted (rolled back)
PASS: demp_jobpos_type_first: unknown position still rejected with 3819
PASS: demp_jobpos_type_first: NULL position (draft) still accepted
PASS: demp_jobpos_type_first: announcement_type CHECK preserved
PASS: demp_jobpos_type_first: stored values digest unchanged
PASS: demp_jobpos_type_first: exactly one job_position CHECK (announcement_job_position_chk)
PASS: demp_jobpos_type_first: re-running the migration is safe
-- demp_jobpos_position_first before: announcement_chk_1=1 announcement_chk_2=0 
PASS: demp_jobpos_position_first: Red reproduced, SRE rejected with 3819 before migration
PASS: demp_jobpos_position_first: all 31 current positions accepted (rolled back)
PASS: demp_jobpos_position_first: unknown position still rejected with 3819
PASS: demp_jobpos_position_first: NULL position (draft) still accepted
PASS: demp_jobpos_position_first: announcement_type CHECK preserved
PASS: demp_jobpos_position_first: stored values digest unchanged
PASS: demp_jobpos_position_first: exactly one job_position CHECK (announcement_job_position_chk)
PASS: demp_jobpos_position_first: all 31 current positions accepted (rolled back)
PASS: demp_jobpos_position_first: unknown position still rejected with 3819
PASS: demp_jobpos_position_first: NULL position (draft) still accepted
PASS: demp_jobpos_position_first: announcement_type CHECK preserved
PASS: demp_jobpos_position_first: stored values digest unchanged
PASS: demp_jobpos_position_first: exactly one job_position CHECK (announcement_job_position_chk)
PASS: demp_jobpos_position_first: re-running the migration is safe
PASS: demp_jobpos_without_check: all 31 current positions accepted (rolled back)
PASS: demp_jobpos_without_check: unknown position still rejected with 3819
PASS: demp_jobpos_without_check: NULL position (draft) still accepted
PASS: demp_jobpos_without_check: announcement_type CHECK preserved
PASS: demp_jobpos_without_check: stored values digest unchanged
PASS: demp_jobpos_without_check: exactly one job_position CHECK (announcement_job_position_chk)
ALL PASS (26.7.0)
```

`announcement_chk_1=0 announcement_chk_2=1`은 해당 제약의 절이 `job_position`을 포함하는지(1) 여부다. 두 순서 모두 같은 SQL로 올바른 제약만 교체됐다.

## 범위

- 이 리허설은 운영과 같은 모양의 스키마에서 SQL의 동작을 확인한다. 운영 MySQL의 실제 제약 이름·버전·데이터에서의 결과는 운영 복원 DB 리허설로 따로 확인해야 한다.
- `JobPositionCheckMigrationTest`는 CI에서 enum과 SQL 허용 집합의 일치만 검사하며 MySQL 실행을 대체하지 않는다.
