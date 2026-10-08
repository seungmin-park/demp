# 운영 질문 조회수 확인 — 2026-10-08

대상은 https://demp-app.duckdns.org 및 질문 ID 1이다. 사용자가 현재 cmux 운영 브라우저에서 로그인한 상태를 확인한 뒤 실제 상세 진입·새로고침·편집 진입을 수행했다. 편집 입력과 제출은 하지 않았다. 토큰은 브라우저 안에서 요청에만 사용하며 이 기록에 저장하지 않는다.

## 운영 배포 기록

- [backend CI 37745463295](https://github.com/seungmin-park/demp/actions/runs/37745463295): main push, success, head `7981f365dd086745b76993f08b108d46635a044b`.
- [backend CD 37747753344](https://github.com/seungmin-park/demp/actions/runs/37747753344): success, 시작 `2026-10-08 08:07:23 UTC`, helper 결과 `08:11:27 UTC`.
- CD helper가 보고한 상태: `serving`, phase `stable`, composite release `demp-45e696d8c425`, cleanup complete true.
- backend tree `cd73dcbb56ac9d15d5fd8721027508ad2de56f81`.
- backend JAR SHA256 `f07869595acc0a8b01ba81f6f7357b15283d942c8e979b12743cffedef768cf6`.
- artifact archive digest `44811a34c3356d9867f78eefe4ff33fd99a4b0bfc43c9e47db9eca6702f63474`.

위 identity는 성공한 CD의 실행 기록이다. 현재 시점의 VM 디스크를 직접 읽어 재확인한 fingerprint라고 표현하지 않는다. 이 작업에서 새 운영 배포를 실행하지 않았다.

## 실제 브라우저와 독립 HTTP 관찰

caller workspace `workspace:1000020005`, 운영 browser `surface:1000020022`를 사용했다. 다른 workspace나 사용자 터미널을 대상으로 삼지 않았다.

| 동작 | 목록 API 관찰 | 화면·순수 조회 |
|---|---:|---|
| 상세 진입 전 `/api/question?size=2` | 조회 2 | 기준 |
| 실제 질문 제목 클릭 → `/questions/1` | 조회 3 | 상세 화면 3 |
| 상세 새로고침 | 조회 4 | 증가 확인 |
| 인증된 `/api/question/detail/1?recordView=false` | 조회 4 | HTTP 200, hits 4 |
| 실제 편집 링크 → `/questions/1/edit` | 조회 4 | 수정 폼 표시, 저장하지 않음 |

따라서 질문 조회수의 운영 배포 완료 기록과 실제 증가·순수 편집 제외 동작을 확인했다. 운영 DB에 직접 접속해 SQL을 실행하지 않았으며 브라우저와 HTTP 관찰에 근거한다.

이번 작업에서 추가한 **공고** 조회수·정렬·신규 enum과 화면 개선은 미커밋 로컬 후보다. 이 질문 검증을 새 공고 기능의 운영 반영으로 취급하지 않는다.
