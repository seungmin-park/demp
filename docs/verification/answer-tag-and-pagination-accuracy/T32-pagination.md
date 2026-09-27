# T32 안정된 페이지 조회 검증

## 위치와 계약

- Backend: `refactor/answer-tag-and-pagination-accuracy`, `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/answer-tag-and-pagination-accuracy/backend`
- Frontend: `refactor/answer-tag-and-pagination-accuracy`, `/Users/seungmin/Desktop/repo/archive/dempfrontend/.worktrees/answer-tag-and-pagination-accuracy/frontend`
- 공고는 기존 Slice JSON을 유지한다. 질문 `GET /api/question?page=0&size=20`은 `{ "content": [...], "last": true, "number": 0 }`을 반환한다. `size`는 1~100, 기본 20이다.

```text
공고 조건 → ID 조회(offset, size+1, id DESC)
              │
              └─ 현재 페이지 ID로 언어 fetch → 원래 ID 순서로 응답 구성

질문 조건 → 제목 AND 본문 AND (태그 OR EXISTS) → 정렬값 DESC, id DESC
                                         └─ offset, size+1 → content/last/number
```

공고 Slice는 전체 개수를 조회하지 않는다. 별도 Page 경로만 count를 실행한다. 컬렉션 fetch join을 페이지 쿼리에서 제거했기 때문에 DB `limit`이 적용된다. 질문은 태그별 join 중복을 만들지 않도록 태그 조건을 EXISTS로 평가한다. Controller는 페이지 파라미터를 검사하고, Service는 조회 유스케이스를 위임하며, Repository가 DB 정렬·페이지를 소유한다. 프런트 API 모듈은 HTTP 파라미터를 만들고 목록 컴포넌트는 페이지 이동과 표시를 맡는다.

## Red → Green → Refactor

- 공고: Hibernate `fail_on_pagination_over_collection_fetch=true` 설정에서 복수 언어 공고의 첫 슬라이스 조회가 `firstResult/maxResults specified with collection fetch`로 실패했다. Page 경로의 기존 경계 테스트 6개 중 5개도 같은 원인으로 실패했다. ID 선조회와 별도 연관 로드로 Green.
- 질문 HTTP: `QuestionControllerTest.returnsQuestionSlice`의 `$.content[0].id`가 기존 배열 응답에서 없어서 실패했다. 페이지 DTO와 `PageRequest`를 연결해 Green.
- 프런트: `QuestionList.spec.js`가 기존 배열 소비 때문에 제목을 렌더링하지 못해 실패했다. API 모듈과 다음/이전 버튼을 추가해 Green. 라우트 전환 테스트에서 반응하지 않은 첫 구성은 비반응성 `$route` mock의 테스트 실수였고 실제 Vue Router 메모리 경로로 고친 뒤 통과했다.
- 공고 SQL의 `limit`과 Slice `count` 부재는 `SqlCaptureInspector`로 실제 생성 SQL을 검사했다. 복수 언어 공고와 같은 정렬값의 질문은 두 페이지 ID·마지막/빈 페이지를 검증했다. 공고 제목·직군·언어와 질문 제목·태그 조건은 페이지 이동에도 유지했다. Axios 배열 쿼리의 `hashtags[]`가 Spring MVC의 태그 목록에 바인딩되는 것도 MockMvc로 확인했다.
- Refactor: Controller 메서드 이름을 반환값에 맞게 `getQuestionSlice`로 정리했고, 호출되지 않는 Service의 전체 목록 메서드를 제거했다. `QuestionSort`를 결과에 맞는 `primaryOrder`로 고치고 불필요한 `OrderByNull` 클래스를 제거했다. `pagingTest`, `pageTest`, `getAnnounceScroll`의 이름·효과는 T47에서 호출부와 함께 정리한다.

## 실행 검증

Backend Java 11: `./gradlew clean test asciidoctor bootJar --console=plain`, Refactor 후 `./gradlew test asciidoctor bootJar --console=plain`. JUnit XML은 29 suite·187 test·실패/오류/건너뜀 0이다. Frontend Node 18.18.2: `npm test -- --runInBand`(8 suite·27 test), `npm run lint -- --no-fix`, `npm run build` 모두 성공했다. REST Docs 생성 응답은 `{"content":[{"id":51,"title":"docs-question","hits":7,"recommend":3}],"last":true,"number":0}`이며 생성 HTML `build/docs/asciidoc/index.html`에서도 확인했다.

프런트의 오래된 요청 응답 역전·재시도와 공고 목록의 오류/마지막 상태 분리는 T40에서 별도 실패 테스트로 다룬다. 현재 페이지 전환 테스트만으로 그 비동기 상황을 보장하지 않는다.
