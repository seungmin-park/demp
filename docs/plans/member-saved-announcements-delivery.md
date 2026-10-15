# 관심 공고 기능 전달

Phase member-saved-announcements, branch refactor/member-saved-announcements, worktrees .worktrees/member-saved-announcements/{backend,frontend}. 마감 방식 기능을 먼저 전달한 뒤 회원별 저장/내 목록/인증 복귀를 전달한다. 원래 combined 구현과 검증 기록은 docs/verification/saved-announcements/README.md(backend)에 보존한다.

사용자가 기능 단위 PR·CI·자동 머지까지 승인했다. 최신 main 보호·GitHub Actions 필수 check·정확한 head의 native auto/squash·merge SHA main CI를 확인한다. 운영 DB는 백업/복원 리허설 후 nullable 마감 컬럼과 빈 bookmark 테이블을 먼저 준비하며 기존 데이터는 보존한다. 신규 봇·예약 작업·보호 완화는 없다. 최종 전달 상태는 후속 delivery 기록으로 남긴다.
