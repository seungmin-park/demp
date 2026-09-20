> 2026-09-21 변경: 아래는 최초 구현의 검증 기록이다. 사용자 요청으로 Flyway와 migration은 제거했으며 README도 원복했다. 현재 H2 설정·검증은 [리뷰 수정 기록](review-2026-09-21.md)을 따른다.

# Phase 1 구현 및 검증

2026-09-14 · `refactor/data-preservation-and-security` · 커밋하지 않음.

## 요구사항과 변경

| 작업 | 결과 |
| --- | --- |
| T10 | InitDb 삭제, 기본 validate/Flyway, local 전용 반복 가능한 SQL seed, 기존 DB 도입 절차 |
| T11 | 가입·조회 DTO의 password 제거, 서비스 PasswordEncoder, 입력 400/중복 409/로그인 401, username NOT NULL/UNIQUE, 로그인 console 제거 |
| T11A | JPA Member와 값 복사형 MemberPrincipal 분리 |
| T12 | JWT principal ID로 작성자 결정, 서비스에서 질문·답변 소유권 확인, AnswerService로 저장 책임 이동 |
| T13 | 서버 jsoup 허용목록과 화면 DOMPurify SafeHtml 적용, 질문·답변·공고의 저장 및 렌더링 보호 |
| T14 | MVC·보안 필터의 안전한 400/401/403/404/409/500 응답, JWT 오류 처리, REST Docs 오류 계약 |

```text
JWT → MemberPrincipal(회원 ID·권한의 복사본)
    → Controller(HTTP 변환)
    → Service(소유권·트랜잭션·HTML 정화)
    → Repository → DB

저장된 HTML → SafeHtml → DOMPurify → 화면
```

Member는 인증 프레임워크를 알지 않는다. 요청 username을 다른 회원으로 바꾸어도 작성자는 인증 ID로 결정된다. 소유권 검사는 변경 전에 수행하므로 403 뒤 원본이 유지된다. 서버 정화는 새 저장값을 보호하고 화면 정화는 기존 저장 콘텐츠도 보호한다.

## Red → Green → Refactor 근거

- 기존 password 노출 assertion, 데이터 재시작 손실·자동 seed assertion, 엔티티 인증 객체 반환 assertion의 실패를 확인했다. T10 상세 명령과 결과는 [데이터 수명 검증](T10-database.md)에 기록했다.
- 회원·소유권·인증 계약을 함께 실행한 유효한 Red는 21개 중 19개 실패였다. [실패 로그](../phase1/contracts-red.log). 초기에 Gradle을 겹쳐 실행하며 발생한 결과 파일 충돌은 기능 Red에서 제외하고 단독 재실행했다.
- sanitizer는 공고 저장 2개 및 정화 단위 assertion의 실제 실패 후 적용했다. 프런트 로그인 로그와 세 출력 지점은 4개 assertion 실패 후 수정했다. 프런트의 별도 기록은 `frontend/docs/phase1-frontend-verification.md`에 있다.
- 만료 시간이 없는 서명 JWT는 실제 500을 재현한 뒤 401로 수정했다. [실패 로그](../phase1/expiry-red.log).
- 공통 인증 값 객체, AnswerService, ContentSanitizer/SafeHtml, 공통 오류 변환으로 책임을 모았다. InitDb 제거 후 기존 Service cleanup은 테스트 데이터 전체를 자식→부모 순서로 정리한다.
- 추가 JSON 파싱·서명 변조·정화 정책 검사는 통과 중인 동작의 특성화 검증이다. 새 기능 Red로 계산하지 않는다.

## 최종 실행 결과

- Java 11: `./gradlew test asciidoctor bootJar --console=plain` 성공. **24 suites / 141 tests, 실패·오류·skip 0**. [콘솔](backend-green.log).
- REST Docs snippet 참조 **91개, 누락 0**. 400/401/403/404/409/500의 상태·본문 문서 생성.
- 프런트 Node 18: `npm test -- --runInBand` **4 suites / 21 tests 통과**, `npm run lint -- --no-fix`, `npm run build` 성공.
- 최신 구현 jar의 local 두 번 실제 기동: 예제 로그인 성공, 새 회원 ID 1 유지, 예제 행 개수 유지, V1/V2 성공, BCrypt 일치, 중복/NULL username 거절. [콘솔](local-restart-green.log), [상세](T10-database.md).
- 정적 확인: Domain의 Spring Security 의존 없음, Controller의 Repository 의존 없음, Service 테스트의 @Transactional 없음, `git diff --check` 통과. 프런트 v-html은 SafeHtml 한 곳에만 있다.

## 범위와 한계

MySQL 모드의 격리 H2 및 local 파일 H2로 migration을 실행했다. 실제 MySQL 서버·운영 데이터에 적용하거나 실브라우저 E2E를 실행한 결과는 아니다. 기존 DB 적용 전 README의 백업·스키마 비교·중복/NULL 정리·명시적 baseline 절차가 필요하다.

T30의 답변 update 트랜잭션과 commit 후 재조회 검증은 T12 구현에 필요한 부분이라 선행 적용했다. 후속 Phase에서 현재 동작을 다시 고장 내 Red를 만들지 않으며 남은 검증을 대조한다. 정상 공고 등록/상세 DTO(T20), 태그(T31), 정렬(T32) 등 후속 범위는 완료로 표시하지 않았다.

프런트 worktree에는 원본의 미커밋 Phase 0 실행 기반을 복사해 포함했다. 원본 저장소는 변경하지 않았다. 백엔드와 프런트 변경은 모두 이 Phase worktree에 남겨 직접 diff로 리뷰할 수 있다.
