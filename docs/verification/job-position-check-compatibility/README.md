# 운영 분야 CHECK 호환 검증

[작업 기록](../../plans/job-position-check-compatibility.md)의 실행 근거다. 아래 "실행 환경"과 "결과"는 임시 MySQL 리허설이고, 운영 적용 결과는 [운영 적용 결과](#운영-적용-결과-2026-11-23)에 따로 기록한다.

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

## 운영 적용 결과 (2026-11-23)

운영자 경로로 수행한 적용 작업의 보고를 옮긴 기록이다. 이 문서를 갱신한 작업에서 운영 DB에 다시 접속해 재확인하지는 않았다.

- 제약: 기존 `announcement_chk_5`가 `announcement_job_position_chk`로 교체됐고 다른 CHECK 제약은 그대로다.
- 동작: 앱과 같은 utf8mb4 접속에서 31개 값이 모두 저장됐고 잘못된 값은 3819로 거절됐다. 검증 쓰기는 모두 롤백해 공고 242건 중 새 분야로 바뀐 행은 없다.
- 데이터: 적용 전후 전체 테이블 체크섬이 같다.
- 서비스: DEMP 백엔드가 약 35초 중단됐고, 이후 상태 UP이며 재시작 후 로그에 오류가 없다. 같은 VM의 다른 서비스는 중지하지 않았다.
- 백업: DEMP를 멈춘 상태에서 적용 직전에 뜬 덤프를 VM에 보관했다.
- 리허설과 달랐던 점: 적용 접속의 문자셋이 latin1이어서 제약의 값 표기가 `_utf8mb4`가 아니라 `_latin1`로 저장됐다. `_utf8mb4` 표기를 세던 적용 스크립트의 사후 검사가 0을 내어 FAILED로 끝났지만, 제약에는 31개 값이 모두 있다. 값이 모두 ASCII이고 utf8mb4 접속의 동작 검증을 통과해 재적용하지 않았다.

## 범위

- 임시 MySQL 리허설은 운영과 같은 모양의 스키마에서 SQL의 동작을 확인한다.
- 관리자 화면에서 새 분야 공고를 실제로 저장·재조회하는 흐름은 아직 확인하지 않았다.
- `JobPositionCheckMigrationTest`는 CI에서 enum과 SQL 허용 집합의 일치만 검사하며 MySQL 실행을 대체하지 않는다. 기술·분야 외 enum 컬럼의 값 변경은 이 테스트들이 잡지 못한다.
