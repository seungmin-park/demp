# 반응 저장의 책임 경계

질문 조회수는 `QuestionService.recordViewAndGetDetail`이 쓰기 트랜잭션을 소유하며 `QuestionRepository.incrementHits`가 DB 원자 증가를 수행한다. 성공 응답의 hits와 커밋 후 조회 값이 일치한다. 기존 `findById`는 순수 조회로 유지한다. Controller는 기본 집계/`recordView=false` 편집 조회를 구분하고 보안 필터는 인증되지 않은 요청이 서비스에 도달하지 못하게 한다. 새로운 컬럼이나 집계용 테이블은 만들지 않는다.

고용 형태는 `Announcement`가 EMP 전용 상태와 교육 전환 시 초기화를 소유한다. `AnnouncementFields`는 평면 입력, 생성/관리자 Service는 저장 트랜잭션, 상세·목록·스크롤 DTO는 반환을 맡는다. 모집 대상과 별도 nullable enum이므로 신입·경력 조합과 충돌하지 않는다. [계약·수동 SQL·검증](../plans/separate-employment-type.md)을 따른다.

```text
Vue 표현 → useContentReaction → API → Controller → ContentReactionService → Repository/DB
                                  인증 ID       잠금·반응 상태·집계의 유일한 기록 책임
```

`ContentReactionController`는 인증 주체와 HTTP 필드를 변환한다. 다른 회원 ID를 요청 본문에서 읽지 않는다. `ContentReactionService`만 `ContentReactionRepository`의 쓰기를 사용하며 같은 트랜잭션에서 반응 행·집계를 갱신한다. 질문/답변 조회 서비스는 이 저장소를 읽어 `myReaction`을 조립할 수 있다. `scripts/check_agent_contracts.py`는 다른 Java 클래스의 직접 `save/delete` 사용을 거절한다. 이 좁은 정적 검사는 리플렉션·간접 위임을 판별하지 않으므로 기능 테스트와 코드 리뷰가 계속 필요하다. 프런트 표현 컴포넌트는 `setReaction`을 직접 호출하지 않고 composable을 거친다.
