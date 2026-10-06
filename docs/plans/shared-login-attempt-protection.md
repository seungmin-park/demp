# 공유 DB 로그인 제한

2026-10-06 사용자 승인: 계정별 15분 창에서 5회 실패하면 5번째 응답부터 15분 제한. 성공은 실패를 초기화한다. backend/frontend의 `refactor/shared-login-attempt-protection`, `.worktrees/shared-login-attempt-protection/{backend,frontend}`에서 미커밋으로 진행한다. backend 기준 f10d9d4, frontend 기준 7f63e06에 이전 CI 무결성 미커밋 변경 7개를 그대로 이어 두었다.

## 요구사항과 책임

```text
POST /api/member/login → MemberService의 독립 쓰기 트랜잭션
  → MemberRepository의 회원 행 잠금
  → Member: 실패 창·횟수·제한 만료 / 성공 초기화
  → commit → MemberInfo 토큰 또는 ApiException(401)/LoginRateLimitException(429)
  → ExController: 기존 오류 JSON + Retry-After(올림한 초)
  → members API: HTTP 오류 해석 → LoginForm: 입력 보존·대기 안내
```

Member가 자기 로그인 상태의 불변식을 소유한다. 저장소는 DB 잠금, 서비스는 인증·commit 순서, Controller Advice는 HTTP 변환을 맡는다. 실패 예외를 쓰기 트랜잭션 안에서 던지면 횟수가 rollback되어 제한이 작동하지 않을 수 있으므로 인증 결과를 먼저 commit한다. 서버 메모리 카운터는 인스턴스 간 공유되지 않아 사용하지 않는다. 시간은 기존 단일 Clock 빈의 Instant를 사용한다. 빈의 이름 `recruitmentClock`은 기존 호출 계약을 유지하며 Instant에는 표시 시간대가 영향을 주지 않는다.

## 순서와 관찰 근거

1. 기존 공개 서비스 API로 5회 실패 테스트 작성: expected 429 / actual 401, 실제 Red.
2. 시간 창 만료·차단 만료·성공 초기화·계정 분리·null 비밀번호·동시 8건·HTTP 헤더·두 독립 컨텍스트를 구현 전에 실행: 11건 중 기대 assertion 실패 10건, 없는 회원의 generic 401은 기존 동작으로 통과.
3. UI 단위 Red: Retry-After 125를 받았으나 2분 5초 안내 없음. 현재 cmux workspace:3 helper surface:11의 headed development/production 두 브라우저도 같은 assertion으로 실제 실패, exit 1.
4. 최소 구현 후 대상 서비스·기존 로그인·기존 스키마 테스트와 UI 단위 통과. 전체/수동 SQL/실제 브라우저 결과는 최종 검증 기록에 연결한다.

Gradle 캐시 잠금 sandbox 거절은 기능 Red로 세지 않았고 같은 명령을 권한 요청 후 실행했다. 최초 npm 설치의 기본 Node25 선택은 Node24.21.0으로 다시 설치했다. 시간 경계는 주입 Clock으로 검증하며 실제 15분 sleep으로 대체하지 않는다.

## 배포 전 DB 절차

자동 migration은 추가하지 않는다. 운영 쓰기를 중지하고 백업·복구 가능 여부와 기존 member 컬럼을 대조한 뒤 [수동 SQL](../../src/main/resources/db/manual/member-login-protection.sql)을 한 번 적용한다. 기존 회원 count는 0, 두 시각은 NULL로 시작한다. MySQL DDL은 자동 commit될 수 있으므로 애플리케이션 트랜잭션으로 원복된다고 가정하지 않는다. 새 JAR은 ddl-auto validate이므로 SQL 누락 시 기동에 실패해야 한다. 이전 JAR로 되돌릴 때 이 추가 컬럼은 우선 유지한다. 컬럼 제거는 백업·코드 호환성 확인 후 별도 작업이다.

Cloud SQL/Cloud Run은 아직 실제 생성·검증하지 않았다. 서버 간 시계 동기화가 필요하며 로그인 실패가 DB에 보존되므로 인스턴스 재시작으로 제한을 초기화하지 않는다. 기존 username 확인 API, 신규 비밀번호 정책, IP/전역 트래픽 제한은 이번 승인된 계정별 제한과 별도 남은 작업이다. 공격자가 다른 사람 계정을 고의로 제한할 수 있는 계정 잠금의 특성은 후속 전체 보안 설계에서 고려한다.

## 이름·호출부·공개 계약 리뷰

`recordLoginFailure`, `expireLoginRestriction`, `resetLoginFailures`는 상태 변경을 이름에 드러내고 `loginRetryAfterSeconds`는 조회만 한다. `authenticateUnderLock`는 잠금 상태의 인증 유스케이스를 조정하고 `LoginAttempt`는 commit 전에 HTTP 예외로 바꾸지 않는 내부 결과다. 단순 위임 서비스/추측성 인터페이스를 만들지 않았다. MemberInfo/가입 DTO/JWT/로그인 라우트와 기존 BCrypt 긴 비밀번호 로그인은 유지한다. 새 DB 컬럼은 수동 SQL·legacy validate로, 새 429는 실제 MVC 바인딩·보안 필터·CORS 헤더로 검증한다. Vue는 표현·입력 상태, TS members 모듈은 HTTP 오류 경계로 평가한다. 필요한 작은 공개 함수만 추가하며 Vue에 transport를 넣지 않는다.

## 전체 검사와 시간대 회귀

최초 전체 344건 중 LocalSeedTest 1건은 새 count 컬럼에 DB 기본값이 없어 기존 seed SQL이 NULL을 넣으려다 실패했다. 같은 컬럼의 Hibernate DDL 기본값도 0으로 맞춰 관련 검증과 전체 검증을 통과시켰다. 일반 검증은 Java344/필수57 suite, Python21, 프런트 단위223/개발·배포 headed162, 모두 실패·스킵 0이었다.

리뷰의 정적 시간대 지적을 실제 MySQL 두 JVM으로 확장했다. JDBC connectionTimeZone=UTC는 같은 조건으로 두고 각각 풀 연결 init SQL로 session time_zone을 +00:00과 +09:00으로 설정했다. TIMESTAMP(6)에서 +09:00 JVM이 제한을 저장했는데 UTC JVM의 정상 비밀번호 요청이 expected 429 / actual 200으로 통과하는 실제 Red를 얻었다. [MySQL 공식 시간대 설명](https://dev.mysql.com/doc/connectors/en/connector-j-time-instants.html)을 확인했고, 기존 날짜에 영향을 주는 전역 설정 대신 새 로그인 두 시각 컬럼만 DATETIME(6)으로 변경했다. Hibernate Instant의 UTC Calendar 입출력과 세션 변환 없는 저장을 한 계약으로 검증한다. ORM/드라이버를 바꾸면 이 실제 MySQL 왕복도 다시 확인해야 한다.

최종 결과는 build/verification/runtime.json, frontend .verification/summary.json, 아래 [검증 기록](../verification/shared-login-attempt-protection/README.md)을 따른다. 앞선 통과를 수정 후 결과로 대신 사용하지 않는다. 실제 GCP/운영 데이터에는 SQL을 적용하지 않았다.
