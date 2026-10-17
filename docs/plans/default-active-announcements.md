# 기본 공고 목록에서 마감 제외

- Phase: 기본 공고 검색 조건
- Branch: `refactor/default-active-announcements`
- Worktree: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/default-active-announcements/backend`

## 관찰과 설계

공개 목록은 현재 상태 조건이 없으면 마감 공고도 반환한다. 클라이언트가 받은 목록에서 마감 건만 제거하면 `Slice.hasNext`와 `Page.totalElements`가 실제 노출 내용과 달라진다. 검색 저장소가 페이지 제한 전에 `ACTIVE`(모집 중·예정)를 적용하도록 한다. 상태 미지정 요청의 기존 전체 조회 의미는 유지해 다른 API 소비자를 보호한다. 화면 기본값은 프런트엔드가 `ACTIVE`를 명시하며, 사용자는 전체·모집 중·예정·마감으로 바꿀 수 있다.

```text
목록 기본값 ACTIVE → API 검색 조건 → QueryRepository 상태 술어 → 페이지/건수 → 화면
상태 전체 선택    → 상태 술어 없음 ────────────────────────────────┘
```

## 검증

- Red: ACTIVE 조건이 공개 Slice의 첫 페이지와 `hasNext`, Page의 총건수에서 종료 공고를 제외하는 실패를 확인한다.
- Green: 조건을 DB 쿼리에 반영하고 대상 테스트를 실행한다.
- Refactor: 상태 판정의 기존 의미와 경계 시각을 유지하고 전체 `bash scripts/verify.sh`로 확인한다.
- 문서: REST Docs에 `ACTIVE` 의미를 기록한다.

## 실행 기록

- Red: `supportsActiveRecruitmentFilter`는 enum에 `ACTIVE`가 없어 assertion 실패. 이어 `activeRecruitmentFiltersBeforePagination`은 `ACTIVE` 값 조회에서 실패했다.
- Green: `ACTIVE`를 `OPEN ∪ UPCOMING`으로 한 시각에 판정한다. 기존 `OPEN`, `UPCOMING`, `CLOSED` 조건과 미지정 전체 조회는 유지한다.
- Refactor: 모집 중·예정 술어를 각각 메서드로 추출해 세 조건이 같은 규칙을 사용한다. 상태가 시각에 따라 바뀌는 기존 테스트도 `ACTIVE` 결과를 검증한다.
- 대상 `AnnouncementQueryRepositoryTest`, `AnnouncementRestDocsTest` 통과. cmux 보조 터미널의 `bash scripts/verify.sh`는 테스트 460개, 필수 suite 68개, 실패·오류·건너뜀 0개, 실제 H2 API 계약 통과로 종료했다. 단, 기존 API 계약은 아직 `ACTIVE` 파라미터를 별도 HTTP 호출로 검증하지 않는다.

이름·책임: `recruitmentMatches`는 요청된 상태를 QueryDSL 술어로 변환하고, `openMatches`/`upcomingMatches`는 상태별 시간 규칙을 소유한다. Controller 바인딩의 enum 공개 값에 `ACTIVE`가 추가되므로 REST Docs를 갱신했다.
