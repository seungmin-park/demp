# 회원 마이페이지와 관리자 진입 분리

2026-10-13 사용자 요청: 최소 회원 정보 확인·수정, 관리자는 별도. 사용자는 로그인 아이디 유지, 닉네임·비밀번호 수정을 선택했다. 기능별 PR·CI·자동 머지 요청을 이어 적용한다.

Phase: `refactor/member-self-service`, `.worktrees/member-self-service/{backend,frontend}`. 기존 미커밋 작업은 보존한다. 백엔드 계정 API, 프런트 마이페이지, 관리자 진입 분리 순으로 전달한다.

```text
회원 화면 → 계정 composable → /api/member/me → 인증 principal
                                           → MemberAccountService → Member/DB
JWT → 저장된 credentialVersion 비교 → 변경 전 토큰 거절
관리자 링크 → 기존 /api/admin/me 검증 → 기존 운영 화면
```

아이디는 읽기 전용, nickname null 허용·변경 시 앞뒤 공백 제거 1~30자. 비밀번호는 현재 비밀번호 확인·8자 이상 UTF-8 72바이트 이하. 실패 5회/15분 잠금은 기존 로그인 제한과 공유한다. 행 잠금으로 중복 비밀번호 변경을 직렬화하고 인증 버전을 다시 비교한다. 실패 횟수는 실패 응답에도 commit된다. 비밀번호 성공 시 버전 증가, 모든 기존 토큰 무효화, 화면 비밀 입력 삭제·재로그인 안내. 닉네임 오류/인증 중단에서는 작성값을 사용자별 sessionStorage에 보존하고 비밀번호는 저장하지 않는다.

Red: 이전 실제 로컬 Spring `:18090`에 새 HTTP 계약 실행 → `/api/member/me` 401 기대/400 실제. 프런트 빈 화면에 공개 동작 테스트 4개 실행 → 모두 assertion 실패. 로그 `/private/tmp/demp-member-self-service/red-http.log`, `red-ui.log`.

첫 전체 검증에서 도메인 HTTP 의존을 구조 검사가 거절했다. InvalidNicknameException을 HTTP 변환과 분리했다. 기존 스키마 검증에 추가 DDL 누락도 재현·수정했다. 이후 432 tests 및 새 실제 HTTP 23 requests 통과. 추가 MVC 문서·필수 suite·최종 UI 검증은 후속 실행으로 기록한다.

책임/이름: 기존 MemberService 가입·로그인과 별도 MemberAccountService는 자기 계정 수정 트랜잭션을 담당한다. Member의 changeNickname/changePassword는 상태를 변경한다. MemberProfile은 비밀·내부 권한 목록을 제외한다. Vue는 표시/이벤트, useMemberAccount는 요청 세대·입력·복구, members API 모듈은 HTTP를 담당한다. 공개 기존 DTO는 유지한다. nickname은 마이페이지 프로필 정보이며 기존 글 작성자 표시 계약은 바꾸지 않는다.

## 최종 로컬 검증

Backend 전체 verify exit 0: 437 tests, 64 required suites, 실패/오류/skip 0. 실제 회원 HTTP 23 requests, 공고 HTTP 55 requests 통과. Frontend 전체 headed verify exit 0: 334 unit, 216 browser flows, typecheck/lint/build 성공. 최초 E2E 4건은 redirect의 URL 인코딩만 가정한 테스트 오류였고 실제 pathname/searchParams 값으로 수정했다.

실제 caller workspace `workspace:1000050001`의 cmux 브라우저에서 local-member 로그인→마이페이지 복귀→닉네임 저장→reload 보존→잘못된 현재 비밀번호 거절→올바른 비밀번호 변경→새 비밀번호 로그인→닉네임 보존 확인. `surface:1000050017`, UI `:5062`→새 JAR/H2 `:18091`. 화면 `/private/tmp/demp-member-self-service/mypage-real.png`. 사용자의 정리 요청에 따라 이전 작업 서버·브라우저는 모두 종료했고 보조 터미널/브라우저 하나씩만 재사용한다.
