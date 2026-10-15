# 운영 목록의 게시 상태 필터

2026-10-15. 기능별 PR 전달. 브랜치 `refactor/admin-announcement-status`, worktree `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/admin-announcement-status/backend`.

기존 통합 작업에서 이 기능의 코드·회귀 테스트만 분리한다. 공통 CI·보호 설정·검증 도구는 변경하지 않는다. 기본 main의 다른 기능은 유지한다. 기존 main 및 통합 작업의 미커밋 내용은 건드리지 않는다.

화면의 상태 선택은 URL과 API로 전달하고 저장소에서 페이지 분할 전에 필터링한다. 공개 조회의 PUBLISHED 경계는 유지한다. 검색은 첫 페이지로 돌아가며 새로고침 조건을 복원한다.

검증: Java 25 `bash scripts/verify.sh`를 해당 분리 worktree에서 실행한다. 기존 통합 작업에서는 실패 테스트 선행 후 최소 변경·전체 검증과 cmux 실제 Spring/H2 저장·재조회를 수행했다. 이번 전달에서는 분리된 입력을 다시 검증하고 정확한 PR head CI·native auto-merge·merge SHA의 main CI를 순서대로 확인한다.

이름·책임 검토: 기존 publicationStatus·date/time·CompanyImage 이름과 공개 계약을 유지한다. 필터는 운영 조회에 한정하고, Vue는 props/event·입력 상태·API 책임으로 검토한다. 불필요한 인터페이스와 위임 객체는 추가하지 않는다. 운영 배포 실행과 CI/머지는 별도 상태다.
