# 수시 모집 마감 방식 전달

Phase: recruitment-deadline-modes, branch refactor/recruitment-deadline-modes, worktrees .worktrees/recruitment-deadline-modes/{backend,frontend}. 관심 공고와 독립된 기능 단위다.

일시 지정은 정확한 마감이 필수이고 채용 시 마감·상시는 원문이 명시한 방식으로 보존한다. 미확인을 상시로 추정하지 않는다. RecruitPeriod/DeadlineMode가 마감 규칙, 서비스가 저장, 조회 저장소가 현재 모집 필터, 프런트 presentation이 표시, 관리자 폼이 명시적 입력을 소유한다.

기존 NULL 모드는 날짜가 있으면 FIXED, 없으면 UNKNOWN이다. 새 공개/검토 요청은 시작 일시와 확인된 마감 방식을 요구한다. 비지정 방식과 날짜가 동시에 있으면 버리지 않고 오류로 회복한다. 최대 기한+조기 마감은 FIXED 최대 일시·본문 조기 조건·수동 종료로 표현한다.

백엔드 배포 전 announcement-deadline-mode.sql을 백업/복원 리허설한 운영 스키마에 적용해야 한다. 자동 CD가 활성화되어 있으므로 스키마 준비를 자동 머지의 선행 조건으로 둔다. 프런트는 백엔드 전달 뒤 머지한다. 보호·exact-head auto-merge·main CI를 확인한다.

기존 구현의 실제 Red: 채용 시/상시 공개 validation 거절 2개, 모순 입력 허용 1개. frontend 상태/입력/multipart 4개 실패 뒤 구현. 분리된 전달 브랜치에서도 기존 전체 verify를 headless로 실행한다. 최종 결과는 전달 기록에 추가한다.
