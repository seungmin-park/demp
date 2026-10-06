# 로그인 보호 검증 기록

현재 브랜치·승인 범위·객체 책임·Red→Green·수동 SQL 순서는 [계획](../../plans/shared-login-attempt-protection.md)에 있다. [MySQL 실행물](mysql_login_rehearsal.py)은 기존 owned loopback 컨테이너에서 새 합성 DB를 만들어 기존 회원과 BCrypt를 보존하고 실제 두 JVM의 상태 공유·행 잠금·429·재시작·만료·스키마 누락을 검증한다. 운영 DB나 GCP가 아니다.

- backend 전체: `build/verification/runtime.json`, XML, REST Docs와 packaged/served docs 일치, 새 isolated H2 API 흐름. 최종 명령 `JAVA_HOME=.../zulu-25.36.205/Contents/Home bash scripts/verify.sh`, 현재 cmux workspace:3 surface:11에서 실행.
- frontend 전체: `../frontend/.verification/summary.json`은 frontend 저장소의 경로이며 단위·types·contracts/probe·lint·build·headed development/production fixture 결과를 담는다. 실제 Spring DB 흐름은 별도다.
- 원시 로그/JSON/Red: `/private/tmp/demp-login-protection-20261006/`. 자격 증명·덤프 파일은 공유하지 않는다. MySQL의 `timezone-red-rehearsal.log`는 expected429/actual200 재현이고 `rehearsal.log`·`result.json`은 현재 마지막 SQL/JAR 결과다.
- 현재 cmux 실제 브라우저: helper pane:10의 surface:18, 터미널surface:11. 새 local H2 JAR/검증한 프런트 production preview의 프로세스·경로는 `live-servers.json`, 실제 DOM/assertion/API 재조회는 `visible-results.json`에 기록한다. 캡처를 실제 assertion 대신 사용하지 않는다.
- 실행 결과 묶음: `final-results.json`은 현재 소스/빌드 해시와 최종 종료 코드, 실제 assertion을 담는다. 검증 실패·실행 불가를 통과로 세지 않는다.

수동 SQL은 한 번만 적용한다. 새 두 DATETIME(6)은 UTC 값 저장 계약이며 다른 세션 time_zone 두 JVM에서 왕복을 확인한다. 이전 TIMESTAMP 안은 로컬 재현에서 거부했고 운영에 적용하지 않았다. 만일 다른 곳에 이전 SQL을 이미 적용했다면 임의로 MODIFY하여 값을 해석하지 말고 그 DB의 시각 인코딩·기존 제한 상태를 확인해 별도 전환해야 한다.

## 확정한 실행 결과

- 수정 후 backend 최종 전체: Python21, Java344/필수57 suite, failure/error/skip 0, REST Docs·bootJar·fresh H2 API·served docs 일치. `backend-verify-final.exit=0`.
- frontend: 46파일/223 단위 assertion, 개발·배포 headed162, warning/skip/fail 0, types/contracts/probe/lint/build 각 exit0. 현재 소스와 production build가 summary 해시와 일치함도 별도 exit0으로 확인했다. frontend 변경 없이 backend SQL을 재검증했다.
- 실제 MySQL 8.4: 35 assertions, 서로 다른 +00:00/+09:00 풀 세션의 두 JVM, 기존 회원/BCrypt/기본값 보존, distributed 실패 누적, 8 concurrent HTTP → 401 4건/429 4건, DB재조회, 재시작, 만료/성공초기화, missing-column 기동 거부. `rehearsal.exit=0`, result status passed.
- 실제 cmux production preview + fresh local H2: local-member 정상 로그인, 질문 추천/답변 비추천·새로고침, 질문 전환/취소·새로고침 후 answer 유지. visible-limit-member 1~4회 일반 오류, 5회 15분 안내, correct password 제한/입력 보존, 새로고침 후 남은 시간 감소. 새 HTTP 조회는 blocked429, independent account200, question NONE/0/0, answer DISLIKE/0/1 확인. 브라우저 errors/console entries 없음.

화면의 첫 DOM selector는 편집기 aria-pressed까지 세어 timeout과 잘못된 text assertion이 났다. 반응 버튼의 aria-label 범위로 고쳐 모든 개수·pressed 값을 정확히 검증했다. 제품 동작 실패로 바꾸어 보고하지 않는다. helper pane과 browser surface를 유지하며 이 검증용 JAR/preview만 종료한다.

Refactor에서는 불필요한 Member import를 정리했고 추측성 로그인 서비스 인터페이스는 추가하지 않았다. Member의 상태 규칙/Repository의 잠금/서비스의 commit/Advice의 HTTP/API 모듈의 오류 해석/Vue의 표시 책임으로 충분하다. 변경 이름·호출부·DTO/JWT/Bcrypt/라우트 계약의 리뷰와 실제 검증을 완료했다. 시간대 리뷰 Important는 실제 Red→새 컬럼 계약→최종 MySQL Green으로 해결했고 추가 구체적 결함은 없었다.

양쪽 새 작업은 미커밋이다. 기존 CI 보안 수정도 frontend worktree에 포함돼 있다. 실제 GitHub CI/재스캔/보안 finding 종료/클라우드 배포는 수행한 것으로 보고하지 않는다. 계정별 제한 외에 username 열거·신규 password 정책·IP/전역 보호는 후속 검토 대상이다.
