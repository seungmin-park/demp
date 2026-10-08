# 공고·질문 후속 작업 검증 — 2026-10-08

[작업 계획](../../plans/announcement-catalog-and-discovery.md), [기계 판독 요약](evidence/summary.json), [운영 질문 확인](production-question-views.md)을 함께 읽는다. 실제 화면·상태·HTTP 결과와 정적 리뷰를 구분한다. 앱 변경은 미커밋이며 새 공고 기능은 운영에 배포하지 않았다.

## 현재 후보와 결과

| 범위 | 실제 실행 결과 | 근거 |
|---|---|---|
| backend 현재 main 기반 전체 검증 | 417 tests, 필수 61 suites, 실패·오류·skip 0; 문서 패키징·served docs·H2 반응 commit/switch/cancel 통과 | [runtime](evidence/backend-runtime.json), 원시 `/private/tmp/demp-discovery-current-main.log` |
| frontend 최종 전체 검사 | 53 suites / 320 tests; typecheck·agent contracts·lint·build exit 0 | [test](evidence/demp-discovery-ux-full-test.log), [types](evidence/demp-discovery-ux-full-typecheck.log), [contracts](evidence/demp-discovery-ux-full-agent-contracts.log), [lint](evidence/demp-discovery-ux-full-lint.log), [build](evidence/demp-discovery-ux-full-build.log) |
| MySQL 실제 JAR 리허설 | 158 checks 통과; 저장/검색/조회수/3정렬/재시작/누락 schema 거절 | [result](evidence/mysql-result.json), [runner](mysql_rehearsal.py) |
| CI용 Playwright fixture | 206 cases / 4 files **발견만** 확인 | [discovery](evidence/demp-discovery-ux-e2e-discovery.log) |

로컬 Chrome for Testing 제외 지시를 유지했다. CI용 Playwright 실행을 cmux 수동 자동화의 통과로 대신하지 않는다. 아래 실제 사용자 흐름은 cmux의 클릭·입력·이동으로 별도 실행한 것이다.

## 보이는 현재 cmux 사용자 흐름

호출 env와 `cmux identify --json`으로 workspace `workspace:1000020005` / caller `surface:1000020016`을 확인했다. 같은 workspace의 기존 helper pane `pane:1000020010`을 재사용했다. preview terminal `0017`, backend terminal `0018`, local browser `0019`, 검증 terminal `0020`, 운영 browser `0022`를 유지했다. 터미널 명령에는 workspace·surface를 명시했다. 브라우저 명령은 surface에 묶었다.

실제 대상은 frontend `http://127.0.0.1:51019`, backend `http://127.0.0.1:18084`다. 현재 worktree 산출물을 사용했고 DB는 격리된 H2다.

| 흐름 | 결과 | 원시 기록 |
|---|---|---|
| 등록·5개 폼 그룹·확장 스택·날짜 선택·6개 폭 | 11 assertions 통과 후 검증 스크립트 JS 문자열 오류로 중단; 실제 공고 생성됨 | [초기 기록](evidence/demp-discovery-visible-ui.json) |
| 동일 공고 이어서 상세 1→2·순수 편집·신규 직무/스택 commit·실제 DB 검색/정렬/URL·더보기 | 18 assertions, exit 0 | [이어 검증](evidence/demp-discovery-visible-resume.json) |
| 질문 실제 페이지 20→2→20 | 3 assertions, exit 0 | [pagination](evidence/demp-discovery-pagination-ui.json) |
| 제보·관리자 처리: 빈 입력/초점/기본 말풍선 제외·6개 폭·실제 접수/처리 commit | 17 assertions, exit 0 | [feedback](evidence/demp-discovery-feedback-ui.json) |
| 최종 기술 선택: 8분야/66항목·6폭·검색 중 요약 해제·실제 저장/재편집·필수 누락/초점/회복·저장값 보존 | 15 assertions, exit 0 | [technology](evidence/demp-discovery-technology-ui.json) |
| 최종 달력: 위/아래 재배치·키보드·Esc 초점·분 유지·500px 높이 제한·모바일 | 8 assertions, exit 0 | [calendar](evidence/demp-discovery-calendar-ui.json) |

초기 스크립트 실패를 삭제하거나 전체 실행 성공으로 바꾸지 않았다. 달력 첫 스크립트는 `scroll --dy`에 소수를 보내 실행 오류가 났다. 정수로 고쳐 재실행했다. 기술 첫 스크립트의 기본 Tab은 macOS WebKit에서 버튼을 건너뛰었다. 모든 요약 버튼의 tabindex=0/disabled=false를 확인하고, 실제 macOS Option+Tab으로 접근을 검증했다. [기술 첫 기록](evidence/demp-discovery-technology-ui-first.json)과 [달력 첫 기록](evidence/demp-discovery-calendar-ui-first.json)을 보존한다.

## 화면

- 기술 선택: [desktop](evidence/demp-discovery-technology-1440.png), [mobile](evidence/demp-discovery-technology-320.png), [오류 회복](evidence/demp-discovery-technology-recovery.png).
- 달력: [위 배치](evidence/demp-discovery-calendar-above.png), [짧은 창](evidence/demp-discovery-calendar-short.png), [mobile](evidence/demp-discovery-calendar-mobile.png).
- 제보: [desktop](evidence/demp-discovery-report-1440.png), [mobile](evidence/demp-discovery-report-320.png).
- 관리자 처리: [desktop](evidence/demp-discovery-resolution-1440.png), [mobile](evidence/demp-discovery-resolution-320.png).
- 정렬 목록: [desktop](evidence/demp-discovery-list-1440.png), [mobile](evidence/demp-discovery-list-320.png).

## MySQL 경계와 배포 준비

`mysql_rehearsal.py`는 기존 고용 형태 리허설의 ownership·loopback·자격 증명 guard를 재사용한다. 자기 소유의 `demp_release_*` fixture DB만 생성하고 다른 DB를 삭제하지 않는다. 새 schema SQL과 실제 packaged JAR로 검증한다. 실행 후 owned container의 기존 stopped 상태를 복원했고 다른 작업 container는 건드리지 않았다.

기술 66개와 직무 31개 전체를 HTTP·독립 SQL·조회·검색에서 검증했다. legacy `React` 등 문자열과 VARCHAR 컬럼, 원문·회사·금액을 보존했다. 동시 방문 16회가 정확히 1..16으로 저장되고 편집·401·404·잘못된 조건은 조회수를 바꾸지 않는다. 페이지 전체를 비교해 3정렬의 중복/누락/동률/마감 조건을 검증했다. 재시작 후 값이 유지되고 hits 미적용 DB의 startup은 거절됐다.

처음 리허설의 2099년 fixture는 기존 MySQL TIMESTAMP 범위 밖이어서 DataTruncation으로 실패했다. 앱을 바꾸거나 assertion을 제거하지 않고 검증용 미래 날짜를 2030년으로 수정한 뒤 통과했다. 이는 기존 컬럼의 날짜 범위이며 신규 enum/조회수 결함으로 보고하지 않는다.

JAR SHA256 `e9cefca9025cd48150a6fa719f9fe2f82899b2b55716ca1a9d9bbf72942c3cbd`, schema SQL SHA256 `a8ffc8f8c1b2ceaae91c1840658c8fd78414ef9c84d303e10a8c76e1548e0923`. 후보 배포 전에 [announcement-view-count.sql](../../../src/main/resources/db/manual/announcement-view-count.sql)을 대상 DB에 한 번 적용해야 한다. 로컬 fixture 검증을 운영 DB 검증으로 취급하지 않는다. 자격 증명 파일과 개인 Notion 기록은 근거 디렉터리에 복사하지 않았다.

## 실패·리뷰 기록

기술 선택 추가 Red 3건과 짧은 달력 Red 2건은 [Red](evidence/demp-discovery-ux-red.log), [Green](evidence/demp-discovery-ux-green.log)에 있다. 최초 Green은 비동기 달력 초점 완료 이전을 읽은 테스트 1건이 실패해 assertion을 유지하고 flushPromises를 추가했다. 첫 타입 검사도 선택적 error prop에 undefined를 넘긴 실제 strict 오류가 있었고 부모가 빈 문자열을 전달하도록 수정했다.

필수 기술 누락의 오류 위치 안내는 [실제 Red](evidence/demp-discovery-recovery-red.log) 후 [Green](evidence/demp-discovery-recovery-green.log)이다. 저장500 후 검색·선택·제목·본문 보존과 같은 값 재시도 검증은 처음부터 통과했으며 기존 보장 확인으로 기록한다. 성공 패턴만으로 기능을 완료했다고 판단하지 않는다.

독립 읽기 전용 리뷰에서 짧은 창의 달력 P2 한 건을 발견했다. 실제 남은 공간 제한과 regression으로 수정했고 재리뷰에서 추가 substantive finding이 없었다. 리뷰는 정적 검토이며 리뷰어가 테스트나 GUI를 재실행한 결과라고 보고하지 않는다.
