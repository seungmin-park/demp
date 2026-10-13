# 운영 공고 13개 주입 — 2026-10-13

사용자가 옆 cmux pane의 운영 관리자 로그인을 제공하고 앞서 조사한 13개 공고 등록을 요청했다. 기존 공개 당근 id4를 그대로 유지하고 신규 초안 12개(id8~19)를 생성했다. 등록 후 각각 실제 폼 reload 및 로그인된 브라우저 컨텍스트의 독립 HTTP 재조회로 제목·원문·분야·기술·직무별 조건·급여·참고 URL·null 일정을 확인했다. 미확인 시각/기술은 추정하지 않았고 일정 충돌은 본문에 남겼다. 필수/우대/조직 사용 기술은 구분했다. 공개 전 검토가 필요한 초안이며 기존 당근을 덮어쓰지 않았다.

## 실제 운영 결함과 수정

첫 SK 등록은 HTTP 500, 전체 트랜잭션 rollback으로 저장되지 않았다. MySQL performance_schema에서 errno3819 및 language_chk_1 위반을 확인했다. 운영 CHECK는 HTML/CSS/React/JAVA/JPA/SPRING 6개만 허용했다. Hibernate validate와 새 빈 스키마 기반 MySQL 검증으로는 이 운영 제약의 낡은 허용 집합을 발견하지 못했다.

별도 복원 DB에서 CPP insert를 실행해 CHECK 위반3819 Red를 확인했다. 현재 66개 enum으로 단일 ALTER TABLE의 DROP CHECK+ADD CHECK를 실행했다. 복원 DB에서 전체66개를 rollback transaction으로 삽입 가능하고 INVALID_TECH는3819로 거절됨을 검증했다. 이후 운영에 같은 SQL을 적용했다. 기존 전체 컬럼 값 SHA256은 전후 `7c313be1756d2ed37d603a6950b84218cc776e5967d46e0f8a269069e1b6b164`로 일치했다.

백업 경로 `/srv/deploy/backups/demp-technology-check-20261013T105042Z`, SQL SHA256 `3e30b8f1f3102c0b4b23e889c7d497ff19b015a086b3b6b02944f67bf8d99132`. 실제 DB dump/기존 값 파일은 VM root 전용 경로에 보존하고 저장소에 넣지 않았다. 복원 DB는 정리했다. 이후 12개 신규 저장이 통과했다.

## 지속 검증

Phase/branch/worktree: `refactor/technology-check-compatibility`, `.worktrees/technology-check-compatibility`. Language는 기술 집합 소유자, 수동 SQL은 운영 CHECK 호환, TechnologyCheckMigrationTest는 enum과 배포 SQL 집합의 드리프트 거절을 맡는다. 코드/API/DB 컬럼 이름과 기존 호출 계약은 변경하지 않았다. 단순 테스트 수와 성공한 새 schema 기동을 운영 데이터 호환으로 확대하지 않는다.

공용 전체 verify와 required suite에 검사를 연결했다. 필수 목록에 대한 설명에서 고정 숫자를 제거해 목록 자체가 기준이 되도록 했다. 실제 운영 폼과 재조회 근거는 `/private/tmp/demp-production-postings-20261013`, 사용자 workspace `workspace:1000050001`, 운영 브라우저 `surface:1000050017`. 목록은 같은 pane에 남겼다. 상세 등록 목록은 results.json을 따른다.
