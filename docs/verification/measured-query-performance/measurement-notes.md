# 초기 진단 기록

1GiB 최초 시도는 baseline 100k 공고의 엔티티 hydration 중 OutOfMemoryError로 실패했다. 실패 시 MockMvc가 누적 요청/응답을 출력한 로그 15MB는 gzip 보존했다. 하네스의 실패용 응답 캡처도 메모리에 영향을 줄 수 있어 이를 순수 제품의 운영 메모리 한계 증거로 사용하지 않는다.

첫 2GiB 시도는 캡처를 제외하기 위해 중단했고 diagnostic-2g에 남긴다. **공식 raw-2g는 모든 비교군에서 MockMvcPrint.NONE, 동일 2GiB heap**이다. 최초 두 진단의 지연 표본은 공식 통계에서 제외한다. JVM heap에는 H2 데이터도 포함된다.

중단된 Gradle client와 별개로 worker가 남아 두 JVM이 겹친 시도도 diagnostic-contended로 분리했다. 두 worker 종료를 확인하고, 각 측정 시작 전 다른 Gradle Test Executor가 있으면 거절하는 검사 추가. 해당 표본은 모두 제외.

## 2GiB 결과와 비교 범위 변경

raw-2g/baseline-round-1.log의 첫 공식 시도도 10만 건 깊은 페이지 반복 중 Java heap space/worker exit 3(Gradle exit 1)으로 종료했다. 첫 페이지와 필터 일부 표본만 완료되어 **raw-2g 전체를 최종 지연 통계에서 제외**한다. 응답 캡처를 꺼도 발생했으나 H2·테스트 계층을 포함한 실패이므로 운영 DB 용량 한계로 해석하지 않는다.

이 실패를 확인한 뒤 최종 범위를 다음과 같이 사전 고정했다. 각 완료 그룹은 3개 독립 JVM × 워밍업 5회 + 표본 15회다.
- baseline: 1천·1만 건. 10만 건은 실패 기록만 제공하며 p50/p95와 배수 비교 없음.
- current 및 current-optimized: 1천·1만·10만 건.
- current-control: 1천·1만 건의 근사 전략 비교. 10만 건 대조군은 실행하지 않음.

최종 formal/은 비교군별로 순차 실행한다. 과거의 회전 순서 계획과 달라졌으며 순서·열 상태에 따른 영향을 완전히 제거한 실험이 아니다. 실패한 표본을 성공 표본에 혼합하거나 누락된 조합을 성공으로 표시하지 않는다.
