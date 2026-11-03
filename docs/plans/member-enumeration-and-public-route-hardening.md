# 회원 아이디 수집 차단과 공개 경로 축소

- Phase: 보안 감사 후속 1 (계정 잠금 악용 경로 축소)
- 브랜치: `refactor/member-enumeration-and-public-route-hardening`
- worktree: `.worktrees/member-enumeration-and-public-route-hardening`
- 근거: 2026-11-03 보안 감사 `demp:member-login:account-keyed-lockout-denies-owner` (needs_validation)

## 문제와 보장할 결과

비로그인 사용자가 `GET /api/member/{memberId}`에 1부터 차례로 ID를 넣으면 모든 회원(관리자 포함)의 로그인 아이디를 얻는다. 이 목록은 계정별 로그인 제한(5회 실패 시 15분 차단)을 남의 계정에 걸어 로그인을 막는 공격의 첫 단계다. 프런트는 이 API를 호출하지 않는다.

```text
비로그인 ─ GET /api/member/1..N ─► 로그인 아이디 목록 ─► 아이디마다 틀린 비밀번호 5회 ─► 15분 차단 반복
          ▲ 이번 변경으로 차단
```

1. 비로그인 사용자는 회원 ID로 로그인 아이디를 조회할 수 없다. 응답 본문에 해당 아이디가 없다.
2. `/api/member/**` 중 비로그인 허용은 `POST login`, `POST save`, `GET validUsername`뿐이다. 새로 추가되는 회원 경로는 기본적으로 인증을 요구한다.
3. 기존 로그인·가입·아이디 확인·본인 정보 경로의 동작은 바뀌지 않는다.

## 범위 밖과 남은 위험

- 계정별 차단이 올바른 비밀번호도 막는 구조 자체(클라이언트·IP 차원의 제한)는 운영 프록시의 실제 IP 전달 설정을 확인해야 하므로 별도 작업이다. 작성자 이름으로 노출되는 username은 프런트 표시 변경이 함께 필요해 별도 작업으로 남긴다. 따라서 이 변경은 공격 비용을 높이지만 계정 잠금 악용을 완전히 해소하지 않는다.

## 객체 책임

`SecurityConfiguration`이 경로별 인증 정책을, `MemberController`가 HTTP 변환을 맡는다. 미사용 조회 핸들러와 그 전용 Service 메서드를 제거하고 새 객체는 만들지 않는다.

## 검증 기록

- Red: `./gradlew test --tests 'com.inhatc.demp.config.ApiSecurityTest'` → 27건 중 새 2건 실패. 비로그인 `GET /api/member/{id}`가 expected 401 / actual **200**(아이디 노출), 미등록 회원 경로가 expected 401 / actual 400(`/api/member/**` 전체 공개 때문에 `{memberId}`에 매칭).
- Green: `SecurityConfiguration`의 `/api/member/**` permitAll을 `POST login·save`, `GET validUsername`으로 좁히고 `MemberController#getMember`와 전용 `MemberService#findById`를 삭제. 삭제 경로를 빌려 쓰던 오류 계약 테스트는 `validUsername`으로 옮기고, "미등록 API는 JSON 404" 계약은 인증된 요청으로 유지(`ExControllerTest`, `verify_local_flow.py`). REST Docs `member-get` 절과 `spec.md` 행을 정리.
- 전체 검증에서 `verify_local_flow.py`가 비로그인 미등록 경로의 401을 잡아냈고(의도된 계약 변경), 비로그인 401 + 인증 404로 두 계약을 명시했다.
- Refactor: `HttpMethod` 전체 경로 표기를 import로 정리(동작 변경 없음).
- 검증: `JAVA_HOME=corretto-25.0.5` `bash scripts/verify.sh` exit 0 — Java 460건, 필수 suite 68, 실패·오류·스킵 0, 실제 HTTP 회원 계약 23요청·로컬 흐름·공고 계약 통과. 프런트는 이 API를 호출하지 않아 변경 없음(`dempfrontend/src/api/members.ts` 확인).
- 이름·책임 검토: `MemberController`는 로그인·가입·아이디 확인 HTTP 변환만 남음. `MemberService`의 미사용 `findById`를 제거해 공개 계약이 실제 호출부와 일치. 새 객체 없음.
