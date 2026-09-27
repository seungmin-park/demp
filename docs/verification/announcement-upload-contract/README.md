# Phase 2 공고 API·업로드 계약 검증

## 작업 위치와 범위

- 브랜치: `refactor/announcement-upload-contract`
- Backend: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/announcement-upload-contract/backend`
- Frontend: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/announcement-upload-contract/frontend`
- 범위: `tasks.md` Phase 2의 T20, T21
- 명시적으로 보호된 Backend 루트 README는 변경하지 않았다. Frontend README diff는 Phase 0/1 선행 상태에서 복사된 것이며 Phase 2에서 편집하지 않았다. 구현과 체크만 수행했으며 commit하지 않았다.

## 책임과 실행 흐름

```text
Vue 화면
  └─ announcements API 모듈
       ├─ 화면 모델 → 평면 multipart 요청
       └─ 서버 상세 응답 → 화면 모델
                │
                ▼
AnnouncementController ── 바인딩·Bean Validation(400)
                │
                ▼
AnnouncementService ── 중복 확인 ──► FileStorage.save
                │                         │
                │                         └─ 확장자·MIME·시그니처·5 MiB 검증
                ▼
        별도 DB transaction
                │
       ┌────────┴────────┐
       │ commit 성공     │ save/commit 실패
       ▼                 ▼
     완료          FileStorage.delete(key)
                         │
                         └─ 삭제도 실패하면 원래 DB 예외에 suppressed로 보존하고 key 기록
```

Controller는 HTTP 형식과 400 응답을 담당하고, Service는 업로드와 DB commit 순서를 조정한다. `Career`와 `RecruitPeriod`는 HTTP 밖에서 생성돼도 잘못된 상태를 거절한다. `FileStorage`는 업무 흐름이 S3 구현 세부사항을 직접 알지 않게 만든 경계다.

## T20 Red → Green → Refactor

Red에서는 기존 중첩 DTO와 소문자 enum을 전제로 한 구현 때문에 백엔드 대상 10건 중 7건, 프런트 상세·작성 테스트 2건이 실패했다. 실패 내용은 평면 상세 필드 부재, multipart enum/필드 불일치, 음수·역전 경력 및 날짜 허용이었다.

Green에서는 생성 요청을 평면 필드로 받고 Service가 `Company`, `Career`, `Description`, `RecruitPeriod`로 조립하도록 바꿨다. 상세 응답은 값 객체 내부 구조를 노출하지 않고 명시적 필드로 반환한다. 프런트는 `src/api/announcements.js`에서 요청 FormData와 응답 모델을 변환한다.

Refactor에서는 HTTP 경계에 경력 범위와 모집 기간의 교차 필드 검증을 추가했다. 새 Controller 테스트는 변경 전 두 사례 모두 200을 받아 실패했고, `@AssertTrue` 검증 뒤 서비스 호출 없이 400을 반환했다. 도메인 불변식은 이중 방어를 위해 유지했다.

## T21 Red → Green → Refactor

Red 대상 13건 중 11건이 실패했다. 업로드가 중복 검사보다 먼저 실행됐고 DB 실패 보상과 파일 유형 검증이 없었기 때문이다.

Green에서는 검증 가능한 도메인 값을 먼저 만들고 중복을 조회한 다음 업로드한다. 업로드 뒤 DB 저장은 `TransactionTemplate`의 commit까지 `try` 경계에 포함한다. 저장 또는 commit이 실패하면 저장 키를 삭제한다.

첫 테스트 구조는 Spring Data 인터페이스 프록시를 `@SpyBean`으로 두고 `callRealMethod()`를 사용해 실패했다. 호출할 구체 메서드가 없었고 스텁도 다른 테스트를 오염했다. 실제 H2 정상 흐름과 중복 검증은 통합 테스트에 남기고, save/commit 실패 주입은 repository와 transaction manager를 명시적으로 mock하는 단위 테스트로 분리했다.

파일 테스트는 최대 크기를 5 MiB로 명시한다. 이 설정이 없으면 단위 테스트 객체의 기본값 0 때문에 모든 비어 있지 않은 파일이 용량 초과로만 실패해 MIME·시그니처 테스트가 거짓 양성이 될 수 있다. 잘못된 파일에서는 S3 mock 호출이 없고, 올바른 JPEG/PNG에서만 `putObject`가 한 번 호출되는지 확인한다.

## 검증 명령과 결과

```sh
JAVA_HOME=/Users/seungmin/.asdf/installs/java/zulu-11.74.15/zulu-11.jdk/Contents/Home \
  ./gradlew clean test asciidoctor bootJar --console=plain

PATH=/Users/seungmin/.asdf/installs/nodejs/18.18.2/bin:$PATH npm test -- --runInBand
PATH=/Users/seungmin/.asdf/installs/nodejs/18.18.2/bin:$PATH npm run lint
PATH=/Users/seungmin/.asdf/installs/nodejs/18.18.2/bin:$PATH npm run build
```

- Backend: 168 tests, 실패·오류·skip 0; AsciiDoc와 bootJar 성공
- Frontend: 6 suites / 24 tests 성공, lint 오류 없음, production build 성공(hash `96b5112091518e25`)
- Build 경고: 오래된 Browserslist 데이터와 368 KiB app entrypoint 크기 경고가 남는다.
- Node 25.5.0 실행은 Babel 7.17.8과 설치된 syntax plugin의 런타임 호환 오류로 테스트 발견 전에 실패했다. 프로젝트 기준인 Node 18.18.2에서는 같은 소스와 설치 트리로 통과했다.
- 실제 S3와 MySQL은 실행하지 않았다. 파일 검증·호출 계약은 mock S3, 영속성과 commit은 H2로 검증했다.

## 독립 리뷰 반영과 남은 경계

독립 리뷰에서 Critical은 없었다. UI 파일 입력이 서버보다 넓은 `image/*`를 허용하고 필수가 아니던 문제는 실패 테스트를 추가한 뒤 `required`와 JPEG/PNG accept 목록으로 맞췄다.

제목 사전 조회는 순차 중복 요청에서 불필요한 업로드를 막지만 동시 요청 두 건의 유일성을 보장하지 않는다. 이를 확정하려면 `Announcement.title`이 업무적으로 전역 유일한지 먼저 결정하고 운영 DB에 unique 제약을 배포해야 한다. 사용자가 Flyway를 제거하고 H2만 사용하도록 정한 현재 범위에서 JPA 애너테이션만 추가하면 운영 제약을 만들지 못하므로, 이번 Phase는 동시성 보장을 주장하지 않는다.
