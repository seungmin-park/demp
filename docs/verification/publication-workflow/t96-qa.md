# T96 · DEMP 최종 QA와 독립 리뷰

## 범위와 현재 환경
- Phase11 `refactor/curated-publication-workflow`, `.worktrees/curated-publication-workflow/{backend,frontend}`.
- asdf Zulu25.36.205/Node24.21.0, 기존 Spring/Vue/TS/lockfile 유지. H2 메모리와 로컬 파일만 사용.
- 호출 workspace:2/surface:3 확인. 과거 terminal12/browser13은 종료되어 있었으므로 오른쪽 pane:13에 terminal34/browser35를 새로 생성했다. 다른 workspace 조작 없음.
- cmux 실제 브라우저와 API mock이 있는 별도 headed Chromium의 범위를 구분했다. 최종 viewport는 native로 복원했고 불필요한 새 탭은 남기지 않았다.

## 실제 브라우저 확인
1. local-admin 로그인, 교육 공고 초안 등록. 기수3기/지원금30만원/교육비 미확인/혼합·서울·파트타임 입력 후 저장.
2. 종료일을 시작일보다 앞서 입력하면 교육 종료일 오류를 표시하고, 수정 후 저장 가능.
3. 검토 상태 저장→공개 상세404→공개 상태 저장→상세 노출. 기술은 Java, 시간은 T 없는 형식.
4. 제보 접수→관리자 제보 목록→처리 내용을 기록→미처리 목록에서 제거.
5. 수동 마감→모집 종료 및 원문 확인 표시, 비공개 전환→공개 상세404.
6. 새 연봉6000/상한4000 입력 시 ‘연봉 상한은 최소 금액 이상이어야 합니다.’ 안내.
7. 모바일390×844에서 검색어 입력, URL q 저장·reload 복원, 교육 전용 빈 결과 안내, 해제 시 교육 탭 유지. 실제 스크롤로 카드8→11 추가 로딩, 가로폭390/문서373으로 넘침 없음.
8. 최종 서버에서 이력 보기로 local-admin·공개·저장 시각 확인. 기본 이미지 및 교육 정보 화면을 직접 확인했다.

증거: [교육 상세](assets/education-publication.png), [모바일 빈 교육 검색](assets/education-empty-mobile.png).

## 독립 리뷰 및 수정
Critical 0, Important 4를 받았고 모두 수정했다. 별도 재리뷰 대신 실제 실패→수정→전체 회귀를 수행했다.
- 빈 multipart publicationStatus가 null로 바인딩되어 새 엔티티 PUBLISHED 기본값을 남김 → 생성 시 null은 DRAFT.
- @URL에서 허용된 한글 도메인이 URI.getHost null로 실패하고 기존 행 비교까지 막음 → IDN 정규화, 잘못된 기존 URL은 비교 제외. 신규 잘못된 URL은400. 별도 포트도 보존한다.
- NEW의 Career(0,0)이 경력3년 검색에 포함됨 → 명시적 NEW 제외, ANY 및 기존 범위 유지.
- 명시적 EXPERIENCED/MIXED가 연차 표시를 지움 → 기존 연차 형식을 유지.
Red: `/tmp/demp-t96-review-red.log` 서비스4개 실패, `/tmp/demp-t96-audience-red.log` 표시3개 실패, `/tmp/demp-t96-port-red.log` 국제화 별도 포트1개 실패.
이름/책임: 출처 정규화는 AnnouncementSourceKey, 신규 기본값은 생성 유스케이스, 검색 대상은 QueryRepository, 연차 표시는 presentation 모듈에 유지했다. API 이름·경로·기존 null 데이터 fallback을 보존했다.

## 최종 검증 결과
- B 전체271개 + bootJar 성공: `/tmp/demp-t96-back-final.log`.
- F 전체151개 + typecheck/lint/build 성공: `/tmp/demp-t96-{unit,types,lint,build}-final.log`.
- 현재 cmux 터미널에서 headed E2E19개 성공: `/tmp/demp-t96-e2e-reviewed.log`, 종료코드0. 기존 권한/스크롤/편집/필터와 새 게시 수명주기를 포함한다.
- 같은 터미널의 실제 HTTP 검증 종료0: `/tmp/demp-t96-http.log`. 빈 상태 비공개, IDN/punycode 중복409, 경력 검색, 회원403, 제보 처리, 교육 금액/기수/인증 이력, 빈 검색200을 검증했다.
- 초기 새 E2E는 관련 카드까지 고르는 locator 때문에 실패했고 상세 카드로 범위를 수정했다. HTTP 스크립트 초기401은 Bearer를 쓴 도구 오류였고 실제 프로젝트 X-AUTH-TOKEN으로 재검증했다.
- 실행 중 JAR을 재빌드하던 이전 프로세스에서 검색500을 관찰했다. 최종 JAR 사본으로 재시작 후 동일 요청200·빈 목록과 UI 안내를 확인했다. 실행 파일을 /tmp의 고정 사본으로 분리했다. 이를 애플리케이션 검색 성공으로 덮어 기록하지 않았다.
- 미해결 Critical/Important 및 리뷰 Minor 없음. 정량 성능/운영 MySQL 배포는 이번 로컬 QA 범위가 아니다.

## 결정·한계
- 운영자 수동 큐레이션. 기관 직접 제출/자동 수집/광고/공유 로고 저장은 미도입이며 구현 완료로 체크하지 않는다.
- 이력은 작성·수정자/시각/제목/상태/원문 기록이며 본문 복원 기능은 제공하지 않는다. 확인 표시는 운영자가 직접 수행한다.
- 기존 nullable 중복 키 행은 보존하며 비교하므로 운영 쓰기에 추가 조회 비용이 있다. 검토 없는 자동 backfill/삭제를 수행하지 않는다.
- 생성자 제한·builder 통일은 이전 사용자 보류 결정을 유지한다.
- 적용 SQL: `src/main/resources/db/manual/announcement-publication.sql`. 실제 운영 DB에는 실행하지 않았다.

## 다시 검증할 때
workflow: firecrawl-qa / url: http://127.0.0.1:5050 / focus: forms, permissions, navigation, responsive.
호스팅 Firecrawl는 로컬 주소에 접근할 수 없어 요청한 cmux에서 실제 조작과 증거 수집을 수행했다. 서버 실행 `/tmp/demp-t96-visible-flow.sh`, 실제 API `/tmp/demp-t96-http.py`(새 로컬 seed DB), 브라우저 자동 `npx playwright test --headed --workers=1`.

## main 통합 결과
- 백엔드 main merge e255364, 프런트 main merge 99fe419 + 환경 설정 d26ab49. 두 원격 main과 refactor/curated-publication-workflow push 완료. 기존 브랜치/worktree 보존.
- main에서도 B271 `/tmp/demp-main-tests.log`, F151 `/tmp/demp-front-main-tests.log` 및 typecheck/lint/build 성공.
- 프런트 main의 .worktrees 아래 과거 checkout을 ESLint가 검사해 1489 errors/24 warnings가 발생했다. 해당 파일들을 수정하지 않고 eslint global ignore 및 .gitignore에 .worktrees/를 추가했다. 원래 root에서 lint/build 재실행 성공, 격리 브랜치에서도 전체151/typecheck/lint/build 성공. 기존 worktree 디렉터리는 삭제하지 않았다.
- 최종 실행 JAR 사본으로 서버를 재시작하고 실제 HTTP 전체 assertion을 다시 통과했다. cmux browser35는 /detail/5, native614×833이며 terminal34에 FINAL_HTTP_EXIT=0 및 E2E 로그가 남는다.
- 생성자/builder 사용자 보류 이외의 현재 실행 대상 미완료 체크 없음. 기관 직접 제출은 별도 조건부 미도입으로 표기했다.
