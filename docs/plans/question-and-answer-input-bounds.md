# 질문·답변 입력 한도

- Phase: 보안 감사 후속 3
- 브랜치: `refactor/question-and-answer-input-bounds`
- worktree: `.worktrees/question-and-answer-input-bounds`
- 근거: 2026-11-03 보안 감사 `QuestionForm.hashtags:HashtagResolver.resolve:unbounded-cardinality` (needs_validation), 같은 감사의 "빈 질문·답변 저장, 255자 초과 제목 500" 리뷰 항목

## 문제

`QuestionForm`에는 `@Valid`가 붙어 있지만 검증할 제약이 없고, 질문 수정·답변 작성·답변 수정은 `@Valid`조차 없다. 빈 제목·본문의 글이 저장되고, 255자를 넘는 제목은 DB 오류로 500이 되며, 해시태그 수·본문 길이는 무제한이라 한 요청이 태그 조회·삽입을 개수만큼 반복하고 영구 전역 태그 행을 만든다(비로그인 태그 목록에 전부 노출).

## 보장할 결과

| 입력 | 규칙 | 위반 시 |
| --- | --- | --- |
| 질문 제목 | 필수, 255자 이하(DB 컬럼) | 400, 저장·수정 없음 |
| 질문 본문 | 필수, 50,000자 이하 | 400 |
| 해시태그 | 10개 이하, 각 30자 이하 | 400 |
| 답변 본문 | 필수, 20,000자 이하 | 400 |
| 수정 대상·질문 ID | 필수 | 400 |

경계값(제목 255자, 본문 50,000자, 태그 10개·30자)은 그대로 저장된다. 오류 응답은 기존 `ExController`의 안전한 400 JSON을 따른다.

## 객체 책임

HTTP 입력 한도는 요청 DTO의 Bean Validation과 Controller의 `@Valid`가 맡는다. Service·Domain 규칙(작성자, 정화, 태그 정규화)은 그대로 둔다. 프런트에는 현재 같은 한도가 없으므로 별도 저장소에서 같은 값으로 입력 안내를 맞추는 후속 작업이 필요하다.

## 검증 기록

- Red: `./gradlew test --tests 'com.inhatc.demp.controller.QuestionControllerTest' --tests 'com.inhatc.demp.controller.AnswerControllerTest'` → 15건 실패, 모두 expected 400 / actual **200**(빈 제목·본문, 256자 제목, 50,001자 본문, 태그 11개, 31자 태그 × 작성·수정, 빈·20,001자 답변 × 작성·수정, 질문 ID 없는 답변). 경계값 통과 테스트(`acceptsQuestionAtLimits`)는 처음부터 통과했고 기존 보장 확인으로 기록한다.
- Green: `QuestionForm`·`QuestionUpdateForm`·`AnswerForm`·`UpdateAnswerForm`에 `@NotBlank`·`@Size`·`@NotNull`, 한도 상수 `QuestionLimits`·`AnswerLimits`, 질문 수정·답변 작성·답변 수정에 `@Valid`.
- Refactor: 새 테스트의 전체 패키지 경로 표기를 import로 정리(동작 변경 없음).
- 검증: `bash scripts/verify.sh` exit 0 — Java 476건, 필수 suite 68, 실패·오류·스킵 0, 실제 HTTP 흐름(질문·답변 작성 포함) 통과.
- 이름·책임 검토: 한도는 HTTP 입력 계약이므로 요청 DTO에 두고, 같은 값을 쓰는 곳이 생길 때 흩어지지 않게 상수 클래스로 이름 붙였다. 두 폼의 미사용 `username` 필드는 JSON 계약(프런트가 보낼 수 있음) 때문에 이번에는 유지했다.
- 남은 일: 프런트(`dempfrontend`)에 같은 한도의 입력 안내가 없어 한도 초과 시 사용자에게 400 오류 안내만 보인다. 별도 저장소 후속 작업.
