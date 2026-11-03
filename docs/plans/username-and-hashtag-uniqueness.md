# 로그인 아이디와 해시태그 이름의 DB 유일성

- Phase: 보안 감사 후속 2
- 브랜치: `refactor/username-and-hashtag-uniqueness`
- worktree: `.worktrees/username-and-hashtag-uniqueness`
- 근거: 2026-11-03 보안 감사 `MemberService.registerMember:username-precheck-without-db-unique`, `demp:HashtagResolver.resolve:hashtag.tag_name-unique-absent-in-validated-legacy-schema` (둘 다 needs_validation)

## 문제

`Member.username`과 `Hashtag.tagName`은 JPA 매핑에만 `unique = true`가 있다. 운영은 `ddl-auto: validate`라 이 제약을 만들지도 검사하지도 않고, 운영 스키마 모델(`legacy/hibernate5-schema.sql` + `db/manual`)에도 UNIQUE가 없다. 코드는 "먼저 조회 → 없으면 저장"이라 동시 요청이 둘 다 저장되면 이후 단건 조회(`Optional`)가 예외를 던진다.

```text
요청 A ─ findByTagName("x") = 없음 ─┐
요청 B ─ findByTagName("x") = 없음 ─┼─► 둘 다 INSERT ─► 같은 이름 2행 ─► 이후 그 태그를 쓰는 모든 질문 작성 500
                                    └─ (가입도 같은 구조: 같은 아이디 2계정 ─► 둘 다 로그인 불가)
```

## 보장할 결과

1. 수동 스키마 변경을 적용한 기존 DB는 같은 로그인 아이디, 같은 태그 이름의 두 번째 행을 거절한다.
2. 기존 DB에 이미 같은 이름의 태그가 여러 행 있으면, 변경 스크립트가 질문 연결을 가장 작은 ID로 옮기고 나머지를 지운 뒤 제약을 건다. 연결된 질문은 잃지 않는다. 같은 아이디의 회원이 이미 여럿이면 계정을 임의로 합치지 않고 제약 추가가 실패해 운영자가 확인하게 한다.
3. 두 회원이 동시에 같은 새 태그로 질문을 써도 두 질문 모두 저장되고 태그는 한 행이다. 제약 위반으로 사용자에게 500을 돌려주지 않는다.
4. 동시 가입은 기존 `DataIntegrityViolationException → 409` 처리가 DB 제약을 통해 실제로 동작한다.

## 객체 책임

- 스키마 불변식: `db/manual` SQL (기존 수동 적용 절차 유지, 자동 마이그레이션 도입 안 함).
- 태그 경합 회복: 트랜잭션 경계를 소유한 `QuestionService`가 질문 작성·수정 유스케이스를 한 번 재시도한다. 첫 시도의 스냅샷에서는 다른 트랜잭션이 커밋한 태그가 보이지 않을 수 있어(MySQL REPEATABLE READ) 같은 트랜잭션 안의 재조회로는 회복할 수 없기 때문이다. `HashtagResolver`의 이름 정규화·조회 책임은 그대로 둔다.

## 운영 적용 (수동)

쓰기 중지·백업 후 `SHOW INDEX FROM member` / `SHOW INDEX FROM hashtag`로 같은 컬럼의 유일 인덱스가 이미 있는지 먼저 확인한다. 중복 조회(`SELECT username, COUNT(*) FROM member GROUP BY username HAVING COUNT(*) > 1`)가 0이 아니면 member 스크립트는 적용하지 말고 계정을 개별 확인한다. 이 저장소의 테스트는 H2 MySQL 모드 결과이며 운영 MySQL 적용의 증거가 아니다.

## 검증 기록

- Red 1 (`LegacySchemaCompatibilityTest`): 레거시 스키마 + 중복 태그 fixture + 기존 수동 SQL 위에서 `legacy-duplicate` 태그 행이 expected `[1100]` / actual `[1100, 1101]`. 새 SQL 파일을 빈 파일·주석 파일로 두었을 때의 `'script' must not be null or empty`는 환경 오류라 Red로 세지 않았고, `SELECT 1;` 자리표시로 바꿔 의도한 assertion 실패를 확인했다.
- Red 2 (`HashtagCreationRaceTest`): 두 트랜잭션이 모두 태그 부재를 본 뒤 저장하도록 spy로 조회 시점을 맞추자 결과가 expected `[saved, saved]` / actual `[DataIntegrityViolationException, saved]`.
- Green: `db/manual/member-username-unique.sql`(회원은 합치지 않고 UNIQUE만), `db/manual/hashtag-tag-name-unique.sql`(관계를 최소 ID로 이관 → 같은 쌍 하나만 유지 → 비대표 행 삭제 → UNIQUE). `QuestionService.createQuestion/updateQuestion`을 `NOT_SUPPORTED` + `TransactionTemplate`로 바꿔 `DataIntegrityViolationException`이면 새 트랜잭션으로 한 번 재시도.
- Refactor: `LegacySchemaCompatibilityTest`에 세 번 복제된 스크립트 목록을 운영 적용 순서 상수와 `schemaLocations` 도우미로 정리(변환 전후 문자열 동일함을 확인).
- 검증: 최신 main(`dd1e37e`) rebase 후 `bash scripts/verify.sh` exit 0 — Java 462건, 필수 suite 69(`HashtagCreationRaceTest` 추가), 실패·오류·스킵 0, 실제 HTTP 흐름 통과.
- 확인하지 않은 것: 실제 MySQL에서의 SQL 실행(collation, derived table 처리)과 운영 DB 적용. `docs/verification/deployment-runtime-and-data-verification/mysql_rehearsal.py` 리허설은 Docker가 꺼져 있어 실행하지 않았다. 운영 적용은 위 "운영 적용" 절차대로 별도로 진행한다.
- 이름·책임 검토: 재시도는 트랜잭션 경계 소유자인 `QuestionService`에 두고 이름(`writeRetryingConcurrentTagCreation`)에 범위를 드러냈다. `HashtagResolver`는 정규화·조회만 맡는다. 가입은 기존 `registerMember`의 409 변환이 DB 제약으로 실제 동작하게 되었고 코드 변경은 없다.
