# Phase 12: 도메인 생성 경로 통일

- 사용자 재개 지시: 2026-09-28. 기존 보류 항목을 마무리한다.
- 브랜치: `refactor/controlled-domain-construction`
- worktree: `.worktrees/controlled-domain-construction/backend`
- 작업 단위: T97 생성 계약·호출부 통일 → T98 전체 검증·QA·최종 리뷰·병합.

## 범위와 책임

업무 코드 → public builder → private 도메인 생성자 → 기존 검증/초기화
JPA 복원 → protected 기본 생성자
HTTP 입력 → 기존 DTO 바인딩 생성자

엔티티/값 객체의 생성자 입력만 builder에 공개한다. ID, 연관 컬렉션, 파생 교육 일수, 처리 결과는 새 입력으로 노출하지 않는다. QuestionHashtag는 Question.addHashtag가 양방향 관계를 함께 구성하므로 독립 builder를 만들지 않는다. 요청/조회 DTO는 바인딩·projection 계약을 유지한다. 기존 Announcement builder는 유지한다.

## 실행 순서

1. 변경 전 전체 테스트 Green을 기록한다.
2. 도메인의 공개 생성자 금지·JPA protected 기본 생성자 계약을 테스트로 작성하고 실제 assertion Red를 확인한다.
3. private 생성자에 builder를 적용하고 모든 업무/테스트 호출부를 이름 있는 인자로 바꾼다. 검증 규칙과 초기값을 유지한다.
4. 대상/전체 테스트, REST Docs, bootJar를 확인한다.
5. 현재 cmux workspace 보조 pane에서 새 backend와 기존 frontend를 연결해 실제 생성 흐름을 확인한다.
6. fresh reviewer 검토, tasks 체크, commit, main 병합·push. 브랜치/worktree는 보존한다.

프런트 코드·HTTP/JSON·DB 컬럼 변경 없음. Java 도메인 생성 API만 변경되므로 호출부 전체 컴파일과 JPA/서비스/컨트롤러 테스트로 호환성을 검증한다.
