# 관심 공고 실행 기록 — plan.md Phase 17

- 실행 방식: 현재 세션에서 직접 구현. commit/PR/merge/배포 없음.
- backend 기준 73ee647, frontend 기준 3cd16e2, 양쪽 refactor/saved-announcements.
- 기존 원본의 plan.md/tasks.md 변경을 backend worktree로 복사했다. 원본 변경 보존.
- Ruling: 삭제된 공고의 저장 항목은 유지한다. Bookmark는 announcementId만 보관하며 공고 FK를 두지 않는다. 삭제/비공개 목록 응답은 내용 없는 unavailable 항목이다. 회원은 FK와 DB cascade로 수명주기를 소유한다.
- Ruling: 저장/해제는 PUT /api/member/me/bookmarks/{announcementId} {saved:boolean}, 상태 일괄 조회는 GET /status?announcementIds=..., 내 목록은 GET /api/member/me/bookmarks?page=&size= (최대 20). 회원 행 잠금으로 동일 회원의 동시 저장을 직렬화하고 UNIQUE를 둔다. 공개 조회 계약은 변경하지 않는다.
- Ruling: 인증 복귀 의도는 내부 redirect URL의 saveAnnouncement 값에 한 번만 담는다. 로그인 완료 화면에서 의도를 소비하며 계정 변경 뒤 늦은 응답을 무시한다. 임의 외부 redirect는 거절한다.
- Ruling: T116은 사용자 승인한 추가 요구로 같은 Phase에서 별도 테스트/구현 단위로 실행한다. 채용 시 마감/상시는 가상 기한이 없고 수동 종료 우선, 기존 null 값은 UNKNOWN으로 해석한다.
- 기준선 최초 실행은 sandbox의 Gradle/Vite 캐시 쓰기 거절로 시작 실패. 승인된 재실행 진행 중. 기능 Red가 아니다.

- T109~T116: 구현/최종 로컬 검증 완료. 사용자 최종 headless 지시 적용. 결과와 실패/수정/리뷰/남은 배포 경계는 README.md 및 JSON 참조.
- Ruling: 최대 마감+채용 시 조기 종료는 FIXED 최대 일시, 본문 조건, 수동 종료로 표현한다. UNTIL_FILLED 날짜 충돌을 조용히 버리지 않는다.
- 최종 backend458/frontend363/headless236/실제 Spring H2 headless5 묶음 통과, commit·배포 없음.
