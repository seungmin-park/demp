# 실제 채용 공고 13개 등록 점검

- 점검일: 2026-10-12, Asia/Seoul.
- Phase: 실제 원문에 기반한 관리자 등록 흐름 진단.
- 백엔드 브랜치: `refactor/real-posting-registration`, 기준 `1de07e79555d2029e1f368e83df29eba9dca8726`.
- 프런트 브랜치: `refactor/real-posting-registration`, 기준 `d9a71aae0f174a348973f4360833c61026a7e5f7`.
- worktree: `/Users/seungmin/Desktop/repo/archive/demp/.worktrees/real-posting-registration/{backend,frontend}`.
- 사용자 결정: 최초에는 **13개 점검 결과를 먼저 보고 범위 결정**, 2026-10-13에는 개선과 실제 모집 상태 전환 검증을 승인했다. 커밋 요청 없음.

## 목적과 검증 경로

사용자가 제공한 13개 URL을 각각 직접 확인하고, DEMP 관리자 화면에서 입력·저장·재조회를 시도한다. 기업 지원서 제출은 범위에 포함하지 않는다. 원문에서 확인하지 못한 기술·직무·시작일을 사실인 것처럼 채우지 않는다. 일자만 있는 경우 화면이 요구하는 시각을 입력한 보완 시도는 원문 사실과 구분한다.

```text
원문 확인 → 폼 입력 → 저장 시도 → 오류 시 입력 유지 확인
                                 └→ 저장 성공 → 목록 → 수정 화면 → 새로고침 → 별도 API 재조회
```

작업 지침은 저장소 `AGENTS.md`, 전역 agent-engineering 스킬, 프로젝트 verify-demp 스킬을 따른다. 이 단계는 진단과 문서 작업이므로 제품 동작의 Red/Green/Refactor를 수행했다고 기록하지 않는다.

## 수행 및 결과

- [x] 백엔드 main pull: `57a57ac` → `1de07e7`; 기존 미추적 파일 보존.
- [x] 별도 worktree와 로컬 Spring/H2·Vite 서버 준비.
- [x] 백엔드 기존 테스트 417개, 프런트 기존 테스트 320개 실행.
- [x] 호출 cmux workspace에서 13개 원문 확인·독립 입력·저장 시도.
- [x] 알려진 일자를 보완 입력하여 기술 필수 검증과 날짜 미확인 문제 구분.
- [x] 초안 3건 저장·재조회, 공개 목록 제외 확인.
- [x] 넥슨 다른 직무 분리 등록의 중복 충돌·입력 유지 확인.
- [x] 슈어소프트테크 원본 이미지 크기 제한과 날짜 기본값 확인.
- [x] 목록 이동·만료·본문 미확인 URL 5건 별도 재확인.
- [x] [개별 결과·근거·개선 후보](../verification/real-posting-registration/README.md) 기록.
- [x] 사용자 검토 후 구현 범위 확정.
- [x] 확정한 범위의 실패 조건을 먼저 테스트로 작성하고 실제 Red 확인.
- [x] 최소 구현, 책임 검토, 전체 검증, 같은 cmux 경로 재검증.

## 구현 승인 전 조사 기록

2026-10-13 추가 요청에 따라 다른 채용 서비스·기업 블로그·대학 게시판·공식 PDF에서 당시 공고를 복원했다. [후속 보고서](../verification/real-posting-registration/source-recovery-20261013.md)에 11개 재입력, IBK 자료 보완 후 저장, 대체 출처 중복 생성, 달력 재선택의 초 손실을 기록했다. 이번에도 제품 구현은 진행하지 않았다. 최초 미확인 판정에 근거한 범위를 그대로 확정하지 않고 새 증거로 검토한다.

제품 코드는 변경하지 않았다. 계획·점검 보고서·구조화된 결과만 미커밋 상태로 남긴다. 개선 후보는 초안 완성도와 사실의 미확인 상태, 일정의 정밀도, 한 공고의 여러 직무, 이미지 처리, 출처 식별과 오류 복귀다. 후보를 구현하기 전 공개 정책과 기존 데이터 호환 범위를 확정한다.

구현 시 Vue 폼만 완화하면 서버 DTO와 `RecruitPeriod` 생성 조건에서 다시 실패한다. 게시 상태별 검증과 불완전한 상태의 저장 책임을 먼저 정하고, 폼·HTTP·도메인·재조회가 같은 계약을 따르는지 확인한다. 중복 보호를 끄는 방식으로 다중 직무 문제를 해결하지 않는다.


## 2026-10-13 개선 승인 후 구현

사용자가 개선과 모집 예정→모집 중→마감의 실제 전환 검증을 요청했다. 동일 Phase의 `refactor/real-posting-registration`, 기존 backend/frontend worktree에서 이어간다. 커밋·배포 없이 구현과 검증까지 수행한다.

보장할 결과: (1) 날짜 수정이 초를 손실하지 않음 (2) 확인 중인 분야·기술·일정은 비워 둔 초안 저장·재조회 (3) 공개 요청은 필수 조건 검증 (4) 원문 식별값이 같은 대체 URL의 중복 차단 (5) 모집 상태는 한국 시간의 시작 포함·마감 포함 경계로 검색 및 열어 둔 화면에서 일치 (6) 여러 직무·기술 조건·근무/보상 정보를 사실대로 보존.

현재 검색은 Clock 기반 조회 시 판정이고 스케줄러/DB 상태 갱신은 없다. 화면은 수동 마감 Boolean만 보므로 실제 시간 경과와 지원 안내가 불일치한다. 회귀 테스트로 재현 후 수정한다. 기존 skill의 설계 재승인 흐름은 이미 사용자가 승인한 개선 작업을 다시 묻는 절차로 적용하지 않는다.


### 구현 책임·기존 데이터

- 모집 상태는 저장 enum 전환이 아니다. 서버 `AnnouncementQueryRepository`는 Asia/Seoul Clock으로 요청 시 판단한다. 시작 포함·마감 포함, 마감 직후 CLOSED, 수동 마감 우선이다. 프런트 `recruitmentState`는 같은 경계를 쓰고 `useRecruitmentTime`이 1초 주기로 표시를 갱신한다. 목록은 30초/탭 복귀 시 현재 검색 조건과 로드한 범위를 재조회하며 실패해도 기존 목록을 보존한다.
- `RecruitPeriod`는 확인되지 않은 경계를 null로 보존한다. 초안·숨김과 공개·검토의 완성도 검증을 분리했다. DTO는 HTTP 오류 위치를 제공하고 Domain은 공개 불변식, Controller는 도메인 실패를 HTTP 400으로 변환한다.
- URL 중복 키를 유지하고 별도 회사·교육 기수·원문 식별 키에 UNIQUE를 추가했다. 기존 키 없는 데이터도 조회해 중복을 검사하며, 새 동시 요청은 DB가 최종 거절한다. 제목으로 병합하거나 기존 중복 데이터를 삭제하지 않는다.
- `RecruitmentRole`은 하나의 직무의 근무지와 조건 문장을 소유한다. 필수/우대/조직 사용 기술의 AND/OR 의미를 텍스트로 보존한다. 기존 `language`는 검색용 기술 태그를 유지한다. 세부 직무 분야는 검색에 포함한다.
- `Description.salaryPeriod`는 YEAR/MONTH를 구분하고 기존 null은 YEAR로 읽는다. 월급은 연봉 검색 숫자로 비교하거나 자동 환산하지 않는다. 근무 기간/전환 조건은 날짜를 추정하지 않는 원문 문장이다.
- 원문 URL과 식별값을 유지한 채 `referenceUrls`에 대체 게시처·첨부를 추가한다. HTTP(S)만 허용하고 공개 렌더링에서도 링크를 검사한다.

배포 전 기존 DB에 `announcement-source-identity.sql`, `announcement-recruitment-facts.sql`을 순서대로 적용해야 한다. 운영 DB에는 실행하지 않았다. 기존 연봉·원문·공개 여부는 그대로 유지하고 새 사실은 null/빈 목록이다. 과거 중복은 원문 대조 후 별도 정리가 필요하며 자동 병합하지 않는다. 기존 스키마의 validate 기동과 기존 행 보존을 테스트한다.

### Red와 반복 검증 기록

`/private/tmp/demp-registration-improvements`의 frontend-red/backend-red: 초 표시/초안 required/시간 상태 UI 및 DTO validation 실패. red-2: URL 변경 중복 통과, 목록 갱신 미구현 실패. red-3: 월급/조건/직무/출처 저장 및 표시 누락 실패. search-red: 세부 직무 검색 누락과 월급/연봉 혼합 실패. shared-red: 기존 등록 화면 월급 단위 누락과 상세 초 표시 손실 실패. 기존 서버 Clock 전환 테스트는 처음부터 통과한 보장 확인이다.

첫 전체 실행에서 도메인→HTTP 의존, 숨김 처리의 lazy 접근, 기존 스키마의 신규 준비 누락을 발견하고 수정했다. 첫 프런트 전체 실행에서 수동 마감일 표시 회귀와 select 순서에 결합된 테스트를 수정했다. 새 단위 선택이 추가되어 공개 여부는 해당 label/id로 직접 선택하도록 했다. 테스트 기대값을 약화하지 않고 초 보존/모집 마감 표기의 변경 계약을 반영했다.

### 최종 검증

백엔드 표준 verify는 428 tests / 61 required suites, 프런트 표준 headed verify는 328 unit tests / 212 browser flows로 모두 exit 0이다. 현재 cmux의 실제 Spring/H2·Vite 화면에서 13개 공고의 초안 저장·재조회, 중복 거절·입력 유지, 공개 필수 검증, 초 보존을 확인했다. 별도 합성 공고로 실제 시간의 예정→진행→마감, 목록 자동 진입·이탈과 수동 마감 우선순위를 확인했다. 모바일 신규 직무 선택 높이의 실제 WebKit 실패도 수정 후 재확인했다.

[개선 결과와 실행 근거](../verification/real-posting-registration/improvements-20261013.md)에 객체 책임, 관찰 시각, 실제 실행과 테스트 대역의 차이, 미적용 DB 변경과 남은 한계를 기록했다. 제품 변경은 미커밋이며 운영 반영은 하지 않았다.

사용자 지적 후 [agent-engineering/API 계약 재점검](../verification/real-posting-registration/engineering-contract-review-20261013.md)을 수행했다. 실제 공고 HTTP 55개 요청을 공용 fresh-JAR verify에 연결하고 schema/header/auth/error/CRUD·cleanup을 검증했다. 새 회귀 suite를 필수 목록에 추가하고 해당 XML 부재 및 응답 계약 변형의 실제 거절을 확인했다. 프로젝트 검증 스킬·기능 지도도 수정했다. 재실행은 백엔드 428 tests/62 required suites, 프런트 328 unit/212 headed flows, 모든 exit 0이다. 남은 이미지/date-only 제품 공백을 이번 검증 보완으로 해결했다고 보지 않는다.
