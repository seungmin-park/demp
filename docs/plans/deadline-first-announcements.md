# 마감 임박순 기본 정렬

2026-10-15. 사용자 추가 요청: 마감일이 얼마 남지 않은 공고를 먼저 표시하고 이 정렬을 기본으로 설정한다. 브랜치 `refactor/deadline-first-announcements`, worktree `.worktrees/deadline-first-announcements/{backend,frontend}`.

## 동작과 경계

기존 DEADLINE 정렬을 재사용한다. 현재 시각 이후 마감이며 수동 마감이 아닌 공고를 가까운 마감부터 정렬하고, 같은 마감이면 ID 최신순이다. 만료·수동 마감·마감 미확인 공고는 뒤로 보낸다. 모집 예정 공고도 유효한 마감이 있으면 기존 알고리즘 기준을 따른다. 새로운 기간 숫자나 모집 상태를 강제하지 않는다.

공개 목록의 HTTP 기본값만 DEADLINE으로 지정한다. 공유 검색 DTO의 LATEST 기본값은 유지해 운영 목록·내부 호출 순서를 바꾸지 않는다. Controller는 HTTP 기본값을 변환하며 실제 정렬은 저장소가 소유한다. 명시적인 LATEST·VIEWS와 잘못된 enum의 400 계약은 유지한다.

화면의 정렬 미선택·잘못된 URL 값은 DEADLINE으로 복원한다. LATEST를 명시적으로 선택하면 URL에 보존하고 API에 전달한다. 기존에는 LATEST를 URL/API에서 생략했으므로 서버 기본값 변경과 함께 이 생략 규칙을 바꾸지 않으면 최신순을 선택해도 마감순으로 조회되는 결함이 생긴다. 날짜 검색·기술·종류 등 기존 조건은 유지한다.

```text
정렬 미선택 → 화면/API 기본 DEADLINE → 저장소의 기존 마감 우선 규칙
명시적 최신순 → URL LATEST 보존 → API LATEST 전달 → ID 최신순
운영 목록 → 공유 DTO 기본 LATEST 유지
```

## 테스트 선행

backend AnnouncementControllerTest의 정렬 생략 assertion이 기존 LATEST 때문에 실패한 뒤 공개 endpoint 기본값만 변경했다. enum 경로를 잘못 쓴 초기 컴파일 오류는 Red가 아니며 바로잡고 실제 assertion 실패를 확인했다. frontend AnnouncementFilters.spec.js는 13개 중 기본값·잘못된 URL 2개가 LATEST→DEADLINE 기대 차이로 실패했다. 구현 후 해당 13개가 통과했다. 기존 Repository 고정 Clock 마감순·동률·null·만료·수동 마감·페이지 계약을 유지하고 Controller 명시 정렬·미지원 400도 함께 검증한다.

브라우저 회귀는 초기 DEADLINE, LATEST 선택·새로고침·기술 조건 변경의 URL/API 상태 유지를 검증한다. 로컬과 CI E2E는 사용자의 최신 요청에 따라 headless로 실행하며 cmux 터미널에 명령과 로그를 표시한다. API fixture 화면 검증과 기존 실제 Spring/H2 통합 검증은 구분한다.

## 전달

기능별 전체 검증 후 PR을 생성하고 보호된 필수 CI와 정확한 head의 native squash auto-merge를 사용한다. 실제 머지 및 해당 merge SHA의 main CI를 확인한다. 기존 기능 PR 결과는 별도 기록이며 신규 변경의 검증으로 바꾸어 보고하지 않는다.

이름·책임: 기존 AnnouncementOrder.DEADLINE, orderBy, filtersFromQuery/filtersToQuery 이름과 DB/JSON 공개 계약을 유지한다. 신규 interface·새 필터 체계나 별도 정렬 객체를 만들지 않는다. 공개 API 기본값은 Controller, 화면 URL 기본값은 라우터 변환, HTTP 전송은 API 모듈, 순서는 저장소의 기존 Clock 기반 규칙이 각각 소유한다.
