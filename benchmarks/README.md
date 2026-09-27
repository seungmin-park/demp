# 최초 리팩터링 이전 / 현재 조회 성능 비교

이 벤치마크는 **실제 Spring MVC·인증·서비스·JPA·JSON 직렬화를 통과하는 MockMvc 요청**을 측정한다. 네트워크 왕복·브라우저 렌더링·실제 MySQL 서버의 처리량을 뜻하지 않는다. 운영 DB를 받는 옵션은 없으며 `jdbc:h2:mem:benchmark`가 아니면 데이터 생성을 거절한다.

## 비교군

| 이름 | 코드/환경 | 목적 |
|---|---|---|
| baseline | `a8b057b`, Java11 / Boot2.5 / Hibernate5 / H2 1.4 | 최초 개선 계획 당시 제품 코드 |
| current | `2f4d749`, Java25 / Boot4.1 / Hibernate7 / H2 2.5 | 반응 기능까지 완료한 현재 코드 |
| current-control | current와 같은 JVM·DB·DTO + 과거 fetch join/전체 목록 조회 | 과거 조회 전략을 재현한 근사 대조군 |
| current-optimized | 측정 후 발견한 병목을 수정한 코드 | 추가 개선의 전후 비교 |

baseline은 원래 compileQuerydsl 오류 4개로 빌드되지 않는다. `735e768`에서 **InitDb, AnnouncementController 두 파일의 타입·생성자 정합성만** 복원한다. 쿼리·서비스·테스트를 당시 이후 코드로 교체하지 않는다. 복원 diff는 검증 자료에 보존한다. 역사적 커밋을 수정하거나 덮어쓰지 않는다.

## 데이터와 측정 계약

- 공고/질문 각각 1천·1만·10만 건. 각 공고 기술 2개, 각 질문 태그 2개, 회원 1천 명. 상세 질문의 답변 50개는 작성자가 모두 다르다.
- 공고 EMP/EDU 50:50, 전체 공개. 날짜·문자열·ID·분포는 seed 함수로 결정되어 난수나 실행 날짜에 의존하지 않는다.
- 완료 조합별 워밍업 5회 후 15회 측정, 독립 JVM 3라운드. 최종 실험은 baseline → current → current-control → current-optimized 순차 실행이다. 동시에 측정하지 않지만 실행 순서·열 상태의 영향을 완전히 제거하지는 못했다.
- 최종 크기: baseline/current-control은 1천·1만, current/current-optimized는 1천·1만·10만. baseline 10만은 2GiB에서도 반복 중 heap 부족으로 실패했고, control 10만은 실행하지 않았다. 실패한 부분 표본은 완료 통계에서 제외한다.
- heap 2GiB 고정, ActiveProcessorCount=4. DB 생성/데이터 적재는 측정에서 제외한다. 각 HTTP 요청마다 새 persistence context, JVM/DB 캐시는 warm 상태다.
- 개별 표본: 지연(ms), SQL 준비 실행 수, 로딩 엔티티 수, 응답 건수·바이트, Java 버전. 잘못된 HTTP 상태·반환 건수·공고 hasNext는 assertion 실패로 중단한다.
- 공고는 두 버전 모두 20개를 반환하지만 과거는 정렬 계약이 없어 동일 ID 집합을 보장하지 않는다. 현재는 ID 내림차순이다.
- **질문 목록은 계약이 다르다**: 과거는 전체 N개, 현재는 20개 Slice. 이 수치는 같은 결과의 쿼리만 빨라진 비율이 아니라 페이지 단위 응답으로 변경한 효과다.
- current에는 게시 상태·교육 메타데이터와 내 반응 조회가 추가되어 응답 크기/SQL이 늘어난 경우도 있다. current-control은 과거 조회 알고리즘을 현재 모델에 적용한 근사 대조군이며 과거 프로그램 그 자체가 아니다. 서비스 경로·정렬·이미지 URL도 다르므로 조회 알고리즘 하나만 바꾼 실험이나 JVM 업그레이드 효과로 해석하지 않는다.
- 초당 처리 요청은 순차 표본의 총 요청수/측정 시간이다. 최대 동시 처리 용량이나 운영 SLA로 해석하지 않는다. H2 결과를 MySQL 인덱스 효과로 일반화하지 않는다.

## 재현

먼저 `.tool-versions`의 Java25와 별도의 Java11을 준비한다. 아래 `BENCH_JAVA11`, `BENCH_JAVA25`는 각각 JDK home 절대경로다.

```sh
git worktree add --detach .worktrees/benchmark-baseline a8b057b
git worktree add --detach .worktrees/benchmark-current 2f4d749
python3 benchmarks/install_harness.py .worktrees/benchmark-baseline --legacy
python3 benchmarks/install_harness.py .worktrees/benchmark-current
python3 benchmarks/install_harness.py .
# 비교군마다 독립 JVM 3개. 기존 결과 경로 재사용 금지.
python3 benchmarks/run_comparison.py --baseline .worktrees/benchmark-baseline --current .worktrees/benchmark-current --java11 "$BENCH_JAVA11" --java25 "$BENCH_JAVA25" --output build/comparison --labels baseline --sizes 1000,10000
python3 benchmarks/run_comparison.py --baseline .worktrees/benchmark-baseline --current .worktrees/benchmark-current --java11 "$BENCH_JAVA11" --java25 "$BENCH_JAVA25" --output build/comparison --labels current
python3 benchmarks/run_comparison.py --baseline .worktrees/benchmark-baseline --current .worktrees/benchmark-current --java11 "$BENCH_JAVA11" --java25 "$BENCH_JAVA25" --output build/comparison --labels current-control --sizes 1000,10000
python3 benchmarks/run_comparison.py --baseline .worktrees/benchmark-baseline --current . --java11 "$BENCH_JAVA11" --java25 "$BENCH_JAVA25" --output build/comparison --labels current-optimized
python3 benchmarks/summarize.py build/comparison --matrix docs/verification/measured-query-performance/comparison-matrix.json
```

`build/benchmark-src`만 생성하며 일반 애플리케이션/테스트 소스셋에 벤치마크용 controller를 넣지 않는다. 일반 빌드 산출물에는 `/bench/**`가 존재하지 않는다. `benchmark.init.gradle`을 명시한 `queryBenchmark` 작업에서만 사용한다. 이미 결과 파일이 있는 디렉터리에는 덧붙이지 않고 중단한다. 결과 비교는 같은 측정 조건으로 수행한다.

원본 current를 재현하려면 그 커밋의 별도 worktree에 현재 `benchmarks/install_harness.py`로 하네스를 설치하고 `--current`로 지정한다. 과거 시점의 성능 숫자는 최신 코드에서 자동 재현되는 숫자로 오해하지 않는다.

## 증거와 실패 범위

최종 원시 표본은 `docs/verification/measured-query-performance/formal/`이다. `comparison-matrix.json`의 **56개 그룹, 2,520개 표본** 전체가 있어야 집계가 통과한다. 각 그룹의 3개 라운드와 표본 번호도 검사한다. 원시 JSONL의 SHA-256과 설치된 하네스·production patch 해시를 보존한다. `python3 benchmarks/test_summary_gate.py`는 비교군 누락 거부와 완전한 조합 수락을 확인한다.

`-PbenchContract=true`를 전달한 `queryBenchmark`는 시간 측정을 하지 않고 별도 1천 건 데이터에서 EMP와 JAVA 조건을 독립적으로 선택 가능하게 만든다. 반환 ID 중복 없음, 첫 페이지/깊은 페이지 불일치, 필터의 실제 조건 일치를 검사한다. 측정용 seed/루프와 분리했다.

초기 1GiB와 2GiB 시도의 부분 결과·중단 및 동시 실행 진단은 모두 최종 통계에서 제외했다. `raw-2g`의 baseline은 응답 캡처를 꺼도 10만 건 깊은 페이지 반복에서 heap 부족으로 종료했다. H2와 테스트 계층이 같은 heap에 있으므로 운영 MySQL의 메모리 한계를 의미하지 않는다. [실패·변경 기록](../docs/verification/measured-query-performance/measurement-notes.md).

그림 생성은 별도 Python 환경에 matplotlib를 설치하고 집계 명령에 `--plot`을 추가한다. 앱 실행/빌드에는 필요하지 않다. H2 기반 결과의 운영 일반화, Java 업그레이드만의 효과, 최대 동시 처리량, 질문 목록의 동일 응답 속도 배수로 해석하지 않는다.
