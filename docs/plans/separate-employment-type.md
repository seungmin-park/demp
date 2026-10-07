# 채용 고용 형태 저장과 검증

2026-10-07 승인 설계. Phase `question-tag-and-account-ui`, branch `refactor/question-tag-and-account-ui`, base `7ad8fab`. 프런트의 동일 Phase 계획에서 순차 작업 및 PR/CI/머지 상태를 추적한다. 고용 형태를 모집 대상(신입·경력)과 별도로 관리한다.

## 계약과 책임

`employmentType`은 `REGULAR`, `CONTRACT`, `CONVERSION_INTERNSHIP`, `EXPERIENTIAL_INTERNSHIP` 또는 null이다. 기존·불명확한 공고는 null이며 정규직으로 추정하지 않는다. 관리자 등록/수정 및 기존 평면 multipart 입력을 지원한다. 잘못된 enum은 서비스 호출 전에 400이다. 관리자 수정은 전체 편집 폼 계약을 유지하며 생략된 고용 형태는 미확인으로 저장한다. 교육 공고는 고용 형태와 모집 대상을 비운다.

```text
관리자 폼 → AnnouncementFields → 생성/관리자 Service → Announcement → DB
                                트랜잭션 조정       EMP 상태 규칙
DB → 상세·목록·스크롤 DTO → 프런트 공통 표시
```

`Announcement.changeEmploymentType`이 공고 종류에 따른 상태 규칙을 소유한다. Service는 기존 트랜잭션 흐름에서 이를 호출하고 DTO는 값을 평면 필드로 반환한다. `employment_type`은 nullable VARCHAR로 매핑한다. 기존 경력·게시 상태·이미지·출처 계약은 그대로 사용한다.

## Red → Green → Refactor

- HTTP 입력: 4개 enum·생략·잘못된 값의 6개 의도한 실패 → `EmploymentType`/입력 필드로 통과.
- 저장·관리자 수정·세 응답·legacy 컬럼: 7개 의도한 실패 → domain 필드/규칙·서비스 호출·DTO·수동 SQL로 통과.
- 중간 Green에서 미확인 공고의 상세 조회가 비어 있었다. 테스트 요청의 기본 DRAFT는 공개 조회 대상이 아니므로 fixture를 PUBLISHED로 바로잡았다. 운영의 게시 상태 규칙은 바꾸지 않았다.
- Green 후 저장 테스트는 존재하지 않던 필드의 BeanWrapper 검사에서 타입이 있는 공개 getter 검증으로 정리했다.
- `JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-25.36.205`에서 `bash scripts/verify.sh`: 전체 390건, 실패/오류/스킵 0. 생성·패키지·인증 HTTP 문서 일치 및 실제 Spring/H2 저장·반응 전환/취소 통과. 등록 문서의 고용 형태 request part 보강 후 최종 전체 verify도 390건과 문서/실제 API 흐름 통과, exit 0 (`final-backend-verify.json`).

## 기존 DB 적용

새 JAR 전에 [수동 SQL](../../src/main/resources/db/manual/announcement-employment-type.sql)을 한 번 적용한다. 자동 migration을 추가하지 않는다. 기존 스키마·백업/복구를 확인하고 컬럼 존재 여부를 먼저 대조한다. MySQL DDL이 자동 commit될 수 있으므로 애플리케이션 rollback으로 되돌린다고 가정하지 않는다. 이전 JAR로 되돌릴 때 추가 컬럼은 유지할 수 있다. 운영 DB 적용은 아직 하지 않았다.

H2 legacy schema에서 기존 제목·회사·금액과 null 및 validate를 확인했다. 실제 MySQL은 [리허설 도구](../verification/separate-employment-type/mysql_rehearsal.py)로 확인했다. 기존 owned container의 loopback 연결, 새 `demp_release_*` DB만 쓰며 기존 DB는 지우지 않았다. 도구가 시작한 container만 종료한다.

실행: `python3 docs/verification/separate-employment-type/mysql_rehearsal.py --evidence /private/tmp/demp-question-ui-20261007/mysql-valid`. exit 0, 25개 assertion: 기존 데이터/null 보존, 네 값 등록·DB commit·상세/목록/스크롤, 관리자 변경/미확인/교육 전환, invalid 400·저장값 보존, 수동 SQL 누락 시 validate 기동 거부. 첫 실행의 스크롤 401은 검증 요청에 필요한 토큰을 누락한 오류였으며 인증 헤더를 제공해 재실행했다.

리허설 JAR SHA256 `f38cc4c1ddd604405470ebbde2e6c8d522dc5d32f219582510a23ea0aa5d338e`, SQL SHA256 `da959b893edaf302ffb26daecb2a038f92dae94cb09ccdce7f554f98a94e2320`. 원본 명령·로그·JSON은 `/private/tmp/demp-question-ui-20261007/`에 있다. 이 JAR의 실제 cmux에서 신입+전환형 인턴을 저장/재조회하고 공개 목록→상세 표시를 확인했다. 최종 리뷰·PR·CI·머지까지 진행 중이며 운영 SQL·배포는 미실행이다.
