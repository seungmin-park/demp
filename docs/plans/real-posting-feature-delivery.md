# 공고 개선 기능별 PR 전달

2026-10-13 사용자가 기능별 PR·CI·자동 merge까지 명시적으로 요청했다. 원래 `real-posting-registration` worktree의 미커밋 작업을 보존하고, `.worktrees/feature-delivery/{backend,frontend}`에서 기능별 커밋을 분리했다. 분리된 각 단계에서 공용 전체 verify를 실행했다. 이전 main과 동일한 내용의 squash merge 뒤에는 아직 푸시하지 않은 다음 기능만 최신 main 위로 rebase했다. 공개 브랜치에 force push하지 않았다.

| 기능 | 백엔드 | 프런트 | 분리 단계 로컬 검증 |
| --- | --- | --- | --- |
| 미확인 초안·공개 불변식 | [#11](https://github.com/seungmin-park/demp/pull/11), `refactor/incomplete-announcement-drafts` | [#15](https://github.com/seungmin-park/dempfrontend/pull/15), `refactor/incomplete-announcement-editor` | backend 419 tests, frontend 325 unit/206 headed flows |
| 원문 식별 중복 | [#12](https://github.com/seungmin-park/demp/pull/12), `refactor/announcement-source-identity` | 기존 식별 입력 사용 | backend 423 tests |
| 직무별 조건·급여 단위·참고 자료 | [#13](https://github.com/seungmin-park/demp/pull/13), `refactor/structured-recruitment-facts` | [#16](https://github.com/seungmin-park/dempfrontend/pull/16), 같은 브랜치 이름 | backend 427 tests, frontend 328 unit/212 headed flows |
| 모집 시각 표시·자동 갱신·초 보존 | 기존 Clock 동작의 회귀를 마지막 계약 PR에 포함 | [#14](https://github.com/seungmin-park/dempfrontend/pull/14), `refactor/recruitment-time-transitions` | frontend 324 unit/206 headed flows |
| 실제 HTTP 계약·회귀 보호·근거 | `refactor/announcement-http-contract-verification` | 프로젝트 verify 스킬은 #16에 포함 | backend 428 tests/62 required suites + 실제 HTTP 55 requests |

원격 정책을 조회하여 GitHub Actions(app 15368)의 정확한 `DEMP verify`/`DEMP frontend verify`, strict 최신 main, 관리자 동일 적용, 대화 해결, force push/삭제 금지와 native squash/auto-merge 활성화를 확인했다. 필수 승인 인원은 0명으로 CI와 사람의 승인을 혼동하지 않는다. 각 PR에는 exact-head native auto-merge를 사용하고 MERGED·merge SHA·그 SHA의 main CI를 별도로 확인한다. 최종 전달 시점의 결과는 PR 상태·실행 링크와 후속 보고서로 확인한다.

## 운영 스키마 준비

main 머지의 기존 CD가 활성화돼 있으므로 schema PR 전에 추가 DDL을 준비했다. 운영 MySQL 8.4.11의 날짜 컬럼은 DATETIME이었다. 로컬의 구형 schema 재현 fixture는 TIMESTAMP여서 최초 HTTP fixture의 2099년이 범위를 넘어 실패했다. 현재 검증 계약 날짜는 2037년으로 수정했고, 동일한 55개 요청이 MySQL에서 통과했다. 이것을 운영에서 2099년을 저장하다 발생한 제품 오류로 기록하지 않는다.

로컬 [MySQL 리허설](../verification/real-posting-registration/mysql_rehearsal.py)은 owned container의 새 DB에서 기존 행/금액·null·관계 목록·HTTP cleanup을 확인하고, 새 schema가 없으면 validate 기동이 거절됨을 확인한다. 명령은 Java 25 `JAVA_HOME`으로 `python3 docs/verification/real-posting-registration/mysql_rehearsal.py --evidence /private/tmp/demp-recruitment-mysql`이다. 기존 fixture 컨테이너가 필요하며 production을 대상으로 하지 않는다.

운영 백업은 `/srv/deploy/backups/demp-recruitment-facts-20261013T073317Z`다. single-transaction 백업을 별도 임시 DB에 복원하고 기존 모든 컬럼 값을 대조한 뒤 동일 DDL을 복원 DB에 먼저 적용했다. 이어 운영에 nullable 컬럼 4개와 빈 관계 테이블 2개만 추가했다. 신규 사실은 기존 행에서 null/빈 목록으로 남는다. 기존 컬럼 값 SHA-256은 전후 모두 `17776d01809a083540a15fb470472cb03b01a6fa2197b0d0b38b41f5a59c26e9`이며, 합친 두 SQL의 SHA-256은 `316ca0a853cb03a032188166a6c41289a0dae19f632cfb2ffa30440c197df4e1`이다. 이전 API HTTP 200과 복원 임시 DB 삭제를 확인했다. 덤프·기존 값 파일은 VM의 root 전용 경로에 보존하고 저장소에 복사하지 않았다.

첫 SSH 확인의 gcloud 기본 동작이 공개 키 항목을 자동 추가했으나 인증되지 않았다. 그 한 항목만 제거하고 기존 세 항목을 보존했다. 이후 기존 1Password agent 키와 엄격한 기존 호스트 키 확인·IAP proxy로 접근했다. 새 개인 키나 자격 증명을 발급하지 않았다.

첫 backend CD run `38362148070`은 IAP tunnel 프로세스 종료로 실패했다. 성공한 main CI와 구분하며 다음 CD 및 실제 상태를 별도로 확인한다. 오래된 main 실행을 무조건 재배포하지 않는다.

등록된 시간에 따른 전환, 수동 종료 우선, 실패 시 입력 보존의 상세 근거는 [구현 기록](../verification/real-posting-registration/improvements-20261013.md), 지속 검증의 보완은 [계약 재점검](../verification/real-posting-registration/engineering-contract-review-20261013.md)을 따른다. 해당 문서의 미커밋/운영 미적용 표기는 당시 시점의 기록이며 이번 전달이 후속 작업이다.
