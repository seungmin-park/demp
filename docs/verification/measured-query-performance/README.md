# T102–T103 · 최초 리팩터링 이전과 대용량 조회 비교

2026-09-28. 브랜치 `refactor/measured-query-performance`, worktree `.worktrees/measured-query-performance/{backend,baseline}`.

## 결론

- **공고 페이지의 전체 엔티티 로딩을 제거했다.** 1만 건에서 같은 20개 응답을 만들 때 10,001개(인증 회원 포함)에서 21개로 줄었다.
- **질문 전체 배열을 페이지 응답으로 바꿨다.** 1만 개 반환에서 20개로 줄었다. 같은 출력 계약의 속도 비교가 아니다.
- **답변·질문 상세의 N+1을 제거했다.** 현행 HTTP 경로의 SQL은 답변50개 53→3회, 질문 상세7→3회다.
- **모든 지연시간이 개선되지는 않았다.** N+1 수정 후 1만 건 환경의 답변 p50은 3.140→3.666ms, 상세는 1.313→2.908ms로 증가했다. SQL 감소를 속도 향상이라고 바꾸어 쓰지 않는다.
- **과거 10만 건 반복은 2GiB에서도 실패했다.** 부분 표본으로 p95나 배수를 계산하지 않았다. 현재 최종 코드는 같은 규모의 6개 경로·3개 JVM 측정을 완료했다.

![측정 결과](comparison.png)

## 주요 수치

각 행은 독립 JVM 3개 × 워밍업 후 표본15개(총45개). SQL·엔티티·바이트는 해당 그룹에서 일정했다.

| 시나리오·규모 | 최초 버전 p50 / p95 ms | 현재 최종 p50 / p95 ms | SQL 최초→최종 | 엔티티 최초→최종 |
|---|---:|---:|---:|---:|
| 공고 첫20개 / 1만 | 189.147 / 597.310 | 3.809 / 5.337 | 3→3 | 10,001→21 |
| 질문 목록 / 1만 | 9.811 / 25.253 | 1.494 / 2.933 | 2→2 | 1→1 |
| 답변50개 / 1만 | 1.627 / 4.129 | 3.666 / 5.486 | 52→3 | 101→101 |
| 질문 상세 / 1만 | 0.798 / 1.651 | 2.908 / 4.349 | 6→3 | 7→7 |
| 공고 첫20개 / 10만 | 반복 실패, 통계 없음 | 2.625 / 3.414 | —→3 | —→21 |

질문 목록의 응답 바이트는 **646,789→1,338**(1만 건)이다. 과거는 질문1만개 전체, 현재는20개+Slice metadata다. 공고 응답은 새로운 필드 때문에 **3,486→8,966바이트**로 커졌다. 변경된 응답 계약을 숨기지 않는다.

공고1만개 첫 페이지의 순차 RPS는 3.48→241.45다. 이는 표본 수÷총 처리 시간이며, 동시 부하의 최대 처리량이 아니다. 과거 공고 p50의 JVM별 범위가 177.62–497.23ms로 커서 단일 배수만으로 홍보하지 않는다. 전체 p50/p95/RPS는 [표](results.md), JVM별 중앙값 범위와 바이트는 [CSV](summary.csv)/[JSON](summary.json)에 있다.

### 이번 N+1 수정만의 전후

| 1만 건 환경 | 수정 전 current | 수정 후 current-optimized |
|---|---:|---:|
| 답변 SQL | 53 | 3 |
| 답변 p50 / p95 ms | 3.140 / 3.841 | 3.666 / 5.486 |
| 상세 SQL | 7 | 3 |
| 상세 p50 / p95 ms | 1.313 / 1.743 | 2.908 / 4.349 |

응답 건수·바이트·로딩 대상은 동일하다. 로컬 H2는 DB 네트워크 비용이 없고 join 처리·JIT·순서·열 상태 영향을 받는다. 위 원인 중 어느 하나가 지연 증가를 만들었다고 분리 입증하지는 않았다. 이번 추가 변경의 입증된 효과는 **데이터 수에 비례하던 SQL 왕복 제거**다. 실제 MySQL 부하에서 지연 개선 여부를 별도로 검증해야 한다.

## 비교군·재현 환경

| 비교군 | 코드 | 완료한 규모 |
|---|---|---|
| baseline | `a8b057b`(제품 코드 `cb6e0b3`과 동일), 두 컴파일 오류 경계 복원 | 1천·1만 |
| current | `2f4d749`, 반응 저장까지 완료 | 1천·1만·10만 |
| current-control | current runtime/schema/DTO + 과거 조회 방식의 근사 대조군 | 1천·1만 |
| current-optimized | current + [조회 변경](assets/optimized-production.patch) | 1천·1만·10만 |

baseline Java11.0.24/Boot2.5.10/Hibernate5/H2 1.4.200, current Java25.0.4.1/Boot4.1.1/Hibernate7/H2 2.5.252. Apple M2(Mac14,2), 16GiB, macOS27 arm64. JVM heap 2GiB 고정, ActiveProcessorCount4. [환경 JSON](environment.json).

최종 `formal/`에 **56개 그룹·2,520개 표본**을 보존했다. 그룹당 JVM3개, 워밍업5회, 측정15회. DB 생성/seed/JSON 검증/로그 기록은 시간 측정 밖이다. 인증·Spring MVC·JPA·JSON 직렬화는 측정 안이며 외부 네트워크/브라우저/실제 MySQL/S3는 포함하지 않는다. 같은 머신에서 비교군별 순차 실행했고 그룹 내 시나리오 순서는 결정적 shuffle을 사용했다.

데이터는 공고N·질문N, 각 기술/태그2개, 회원1천명, 서로 다른 작성자의 답변50개다. 동일 seed를 사용하지만 과거는 정렬 계약이 없어 최신 공고와 동일 ID 집합을 반환하지 않는다. current-control은 서비스 경로·정렬·이미지 URL도 다르므로 알고리즘 하나만 바꾼 실험이나 Java 업그레이드 효과를 측정한 실험으로 부르지 않는다.

[재현 명령](../../../benchmarks/README.md) · [완료 조합](comparison-matrix.json) · [원시 체크섬](raw-sha256.json) · [이전 하네스 해시](before-manifest.json) · [이후 하네스 해시](after-manifest.json).

## 실패와 제외

- 원본은 compileQuerydsl에서4개 타입 오류. `735e768`의 InitDb와 AnnouncementController만 기계적으로 복원했다. 쿼리/서비스는 그대로다. [원본 로그](assets/original-build.log), [복원 patch](assets/baseline-restore.patch).
- 초기1GiB 진단 실패·캡처 설정 변경으로 중단한 시도·Gradle worker 중첩 시도는 제외했다.
- 캡처를 끈2GiB baseline도 10만 건 깊은 페이지 반복에서 `Java heap space`, worker exit3/Gradle exit1로 중단했다. [실제 로그](raw-2g/baseline-round-1.log). H2·테스트 계층도 같은 heap에 있으므로 운영 메모리 상한으로 일반화하지 않는다.
- 실패 후 비교 범위를 고정했다. baseline10만은 실패 기록, control10만은 미실행, current 두 버전은10만 완료. 실패 부분 표본을 성공 통계에 섞지 않았다.
- 별도 필터 검증의 최초 시도는 과거 DTO에 없는 announcementType을 읽어 NPE가 났다. 테스트 하네스 오류로 분류하고, 원본 DB 행의 유형도 확인하도록 수정했다. 제품 기능 Red나 제품 오류로 보고하지 않는다.

자세한 제외 근거는 [measurement-notes](measurement-notes.md). p95는45개 표본의 nearest-rank 추정값이며 운영 꼬리 지연 보장이 아니다. 성능 개선만 선별한 보고서가 아니라 모든 측정 경로와 실패를 공개한다.

## TDD와 책임 검토

1. **Red:** `./gradlew test --tests 'com.inhatc.demp.service.CommunityQueryBudgetTest'`에서 질문 상세8회·답변4개6회가 상한3회를 초과해2개 assertion 실패. [실제 XML](assets/query-budget-red.xml).
2. **Green:** 답변 조회에 member graph, 질문 상세 전용 `findDetailById`에 member·questionHashtags.hashtag graph를 적용했다. 두 테스트 통과. [로그](assets/query-budget-green.log).
3. **Refactor:** 조회 DTO에 필요한 연관 데이터의 fetch 책임을 Repository에 유지했다. `findDetailById`는 상세 조회 목적을 드러내며 수정·삭제·반응 잠금용 조회는 기존 경로를 유지한다. 질문은 단일 컬렉션 fetch이며 페이지 조회에 적용하지 않는다.
4. **전체 검증:** 백엔드316개 실패/오류/스킵0, REST Docs·bootJar 종료0. [로그](assets/backend-all.log).
5. **하네스 검증:** 누락 비교군을 받아들이던 집계기의 실제 assertion Red 후 전체 조합 검사를 추가해 Green. `benchmarks/test_summary_gate.py`로 재현 가능. 계약 모드는 EMP/JAVA 조건, ID 중복 없음, 첫/깊은 페이지의 분리를 검증했다. 세 비교군 모두 통과: [과거](assets/contract-baseline.log) / [현재](assets/contract-current.log) / [대조군](assets/contract-control.log).
6. **독립 검토:** production 변경에서 추가 결함 없음. 집계 누락, 대조군 과장 설명, 정확성 검증, 재현 문서 불일치를 수정했다. 정적 리뷰와 실제 실행 결과를 구분한다.

프런트의 공개 API·DB 저장 형식은 변경하지 않았다. 런타임 성능 실험용 `/bench/**`는 별도 source set에만 존재하고 일반 애플리케이션 JAR에는 포함하지 않는다.
